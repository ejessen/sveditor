/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/

package net.sf.sveditor.core.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.IFieldItemAttr;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBFieldItem;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBInclude;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBMacroDef;
import net.sf.sveditor.core.db.SVDBMacroDefParam;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.SVDBMarker.MarkerKind;
import net.sf.sveditor.core.db.SVDBMarker.MarkerType;
import net.sf.sveditor.core.db.SVDBPackageDecl;
import net.sf.sveditor.core.db.SVDBScopeItem;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.log.ILogHandle;
import net.sf.sveditor.core.log.ILogLevelListener;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.preproc.ISVPreProcFileMapper;
import net.sf.sveditor.core.preproc.SVPreProcessor;
import net.sf.sveditor.core.scanner.IDefineProvider;
import net.sf.sveditor.core.scanner.IPreProcErrorListener;
import net.sf.sveditor.core.scanner.ISVPreProcScannerObserver;
import net.sf.sveditor.core.scanner.ISVScanner;
import net.sf.sveditor.core.scanner.SVKeywords;
import net.sf.sveditor.core.scanutils.ITextScanner;
import net.sf.sveditor.core.scanutils.ScanLocation;

/**
 * Scanner for SystemVerilog files.
 * 
 * @author ballance
 * 
 *         - Handle structures - Handle enum types - Handle export/import,
 *         "DPI-C", context as function/task qualifiers - type is always <type>
 *         <qualifier>, so no need to handle complex ordering (eg unsigned int)
 *         - handle property as second-level scope - recognize 'import' - handle
 *         class declaration within module - Handle sequence as empty construct
 */
public class SVParser implements ISVScanner,
		IPreProcErrorListener, ISVDBFileFactory, ISVPreProcScannerObserver,
		ISVParser, ILogLevelListener {
	private ITextScanner fInput;
	private SVLexer fLexer;

	private ScanLocation fStmtLocation;
	private ScanLocation fStartLocation;

	private IDefineProvider fDefineProvider;

	private SVDBFile 					fFile;
	private Stack<SVDBScopeItem> 		fScopeStack;
	private SVParsers 					fSVParsers;
	private int							fParseErrorCount;
	private int							fParseErrorMax;
	private LogHandle					fLog;
	private boolean						fDebugEn;
	private List<SVDBMarker>			fMarkers;
	private boolean						fDisableErrors;
	private ISVPreProcFileMapper		fFileMapper;
	private SVParserConfig				fConfig;
	/**
	 * LanguageLevel controls how source is parsed
	 */
	private SVLanguageLevel				fLanguageLevel;
	
	public SVParser() {
		this(null);
	}

	public SVParser(IDefineProvider dp) {
		// Setup logging
		fLog = LogFactory.getLogHandle(
				"ParserSVDBFileFactory", ILogHandle.LOG_CAT_PARSER);
		fLog.addLogLevelListener(this);
		logLevelChanged(fLog);
		
		setDefineProvider(dp);
		fScopeStack = new Stack<SVDBScopeItem>();

		fParseErrorCount = 0;
		fParseErrorMax = 100;
		
		fConfig = new SVParserConfig();
		
		fLanguageLevel = SVLanguageLevel.SystemVerilog;
	}
	
	public void setConfig(SVParserConfig config) {
		fConfig = config;
	}
	
	public SVParserConfig getConfig() {
		return fConfig;
	}

	public void setDefineProvider(IDefineProvider p) {
		fDefineProvider = p;
	}
	
	public void setFileMapper(ISVPreProcFileMapper mapper) {
		fFileMapper = mapper;
	}

	public ScanLocation getStmtLocation() {
		if (fStmtLocation == null) {
			// TODO: should fix, really
			return getLocation();
		}
		return fStmtLocation;
	}

	public ScanLocation getStartLocation() {
		return fStartLocation;
	}

	public void setStmtLocation(ScanLocation loc) {
		fStmtLocation = loc;
	}

	public void preProcError(String msg, String filename, int lineno) {
		if (fMarkers != null && !fDisableErrors) {
			SVDBMarker marker = new SVDBMarker(
					MarkerType.Error, MarkerKind.UndefinedMacro, msg);
			int file_id = -1;
			if (fFileMapper != null) {
				file_id = fFileMapper.mapFilePathToId(filename, false);
			}
			
			marker.setLocation(SVDBLocation.pack(file_id, lineno, 0));
			fMarkers.add(marker);
		}
	}

	private void top_level_item(ISVDBScopeItem parent) throws SVParseException {
		long start = fLexer.getStartLocation();
		int modifiers = scan_qualifiers(false);

		try {
			if (fLexer.peekOperator(OP.LPAREN_MUL)) {
				fSVParsers.attrParser().parse(parent);
			}
		} catch (SVParseException e) {
			// Ignore the error and allow another parser to deal with
		}
		
		KW kw = fLexer.peekKeywordE();
		
		if (kw != null) {
			switch (kw) {
				case BIND: parsers().modIfcBodyItemParser().parse_bind(parent); break;
				case CONFIG: parsers().configParser().parse_config(parent); break;
				case CLASS: parsers().classParser().parse(parent, modifiers); break;
				case MODULE:
				case MACROMODULE:
				case INTERFACE:
				case PROGRAM:
					// enter module scope
					parsers().modIfcProgParser().parse(parent, modifiers);
					break;
				case PACKAGE: package_decl(parent); break;
				case COVERGROUP: parsers().covergroupParser().parse(parent); break;
				case IMPORT: parsers().impExpParser().parse_import(parent); break;
				case EXPORT: parsers().impExpParser().parse_export(parent); break;
				case TYPEDEF: parsers().dataTypeParser().typedef(parent); break;
				case FUNCTION:
				case TASK: parsers().taskFuncParser().parse(parent, start, modifiers); break;
				case CONSTRAINT: parsers().constraintParser().parse(parent, modifiers); break;
				case PARAMETER:
				case LOCALPARAM: parsers().modIfcBodyItemParser().parse_parameter_decl(parent); break;
				case TIMEPRECISION:
				case TIMEUNIT: parsers().modIfcBodyItemParser().parse_time_units_precision(parent); break;
				
				default:
					// Assume this is a top-level item
					parsers().modIfcBodyItemParser().parse_var_decl_module_inst(parent, modifiers);
//					error("Unhandled keyword: " + fLexer.eatToken());
					break;
			}
		} else if (fLexer.peekId() && fLexer.peek().equals("${file_header}")) { // kw is null
			// Ignore pieces of the template
			fLexer.eatToken();
		} else if (!fLexer.peekOperator()) {
			parsers().modIfcBodyItemParser().parse_var_decl_module_inst(parent, modifiers);
		} else if (fLexer.peekOperator(OP.SEMICOLON)) {
			// null statement
			fLexer.eatToken();
		} else { // kw is null
			// TODO: check for a data declaration
			error("Unknown top-level element \"" + fLexer.peek() + "\"");
		}
	}

	public int scan_qualifiers(boolean param)
			throws EOFException {
		int modifiers = 0;
		Map<String, Integer> qmap = (param) ? fTaskFuncParamQualifiers
				: fFieldQualifers;

		String id;
		while ((id = fLexer.peek()) != null && 
				qmap.containsKey(id) &&
				(fLexer.isKeyword() || id.equals("__sv_builtin"))) {
			if (fDebugEn) {
				debug("item modified by \"" + id + "\"");
			}
			modifiers |= qmap.get(id);

			fLexer.eatToken();
		}

		return modifiers;
	}

	public String scopedIdentifier(boolean allow_keywords) throws SVParseException {
		StringBuilder id = new StringBuilder();

		if (!allow_keywords) {
			id.append(fLexer.readId());
		} else if (fLexer.peekKeyword() || fLexer.peekId()) {
			id.append(fLexer.eatTokenR());
		} else {
			error("scopedIdentifier: starts with " + fLexer.peek());
		}

		while (fLexer.peekOperator(OP.COLON2)) {
			id.append("::");
			fLexer.eatToken();
			if (fLexer.peekKeyword(KW.NEW) ||
					(allow_keywords && fLexer.peekKeyword())) {
				id.append(fLexer.readKeyword());
			} else {
				id.append(fLexer.readId());
			}
		}

		return id.toString();
	}

	public List<SVToken> scopedIdentifier_l(boolean allow_keywords) throws SVParseException {
		List<SVToken> ret = new ArrayList<SVToken>();

		if (!allow_keywords) {
			ret.add(fLexer.readIdTok());
		} else if (fLexer.peekKeyword() || fLexer.peekId()) {
			ret.add(fLexer.consumeToken());
		} else {
			error("scopedIdentifier: starts with " + fLexer.peek());
		}

		while (fLexer.peekOperator(OP.COLON2, OP.DOT)) {
			ret.add(fLexer.consumeToken());
			if (fLexer.peekKeyword(KW.NEW) ||
					(allow_keywords && fLexer.peekKeyword())) {
				ret.add(fLexer.consumeToken());
			} else {
				ret.add(fLexer.readIdTok());
			}
		}

		return ret;
	}

	public List<SVToken> scopedStaticIdentifier_l(boolean allow_keywords) throws SVParseException {
		List<SVToken> ret = new ArrayList<SVToken>();

		if (!allow_keywords) {
			ret.add(fLexer.readIdTok());
		} else if (fLexer.peekKeyword() || fLexer.peekId()) {
			ret.add(fLexer.consumeToken());
		} else {
			error("scopedIdentifier: starts with " + fLexer.peek());
		}
		
		while (fLexer.peekOperator(OP.COLON2)) {
			ret.add(fLexer.consumeToken());
			if (allow_keywords && fLexer.peekKeyword()) {
				ret.add(fLexer.consumeToken());
			} else {
				ret.add(fLexer.readIdTok());
			}
		}

		return ret;
	}



	public String scopedIdentifierList2Str(List<SVToken> scoped_id) {
		StringBuilder sb = new StringBuilder();
		for (SVToken tok : scoped_id) {
			sb.append(tok.getImage());
		}
		return sb.toString();
	}

	private void package_decl(ISVDBScopeItem parent) throws SVParseException {
		if (fLexer.peekOperator(OP.LPAREN_MUL)) {
			fSVParsers.attrParser().parse(parent);
		}
		SVDBPackageDecl pkg = new SVDBPackageDecl();
		pkg.setLocation(fLexer.getStartLocation());
		fLexer.readKeyword(KW.PACKAGE);
		
		fScopeStack.push(pkg);

		try {
			if (fLexer.peekKeyword(KW.STATIC, KW.AUTOMATIC)) {
				fLexer.eatToken();
			}

			String pkg_name = readQualifiedIdentifier();
			pkg.setName(pkg_name);
			fLexer.readOperator(OP.SEMICOLON);

			parent.addChildItem(pkg);

			while (fLexer.peek() != null && !fLexer.peekKeyword(KW.ENDPACKAGE)) {
				top_level_item(pkg);

				if (fLexer.peekKeyword(KW.ENDPACKAGE)) {
					break;
				}
			}

			pkg.setEndLocation(fLexer.getStartLocation());
			fLexer.readKeyword(KW.ENDPACKAGE);
			// Handled named package end-block
			if (fLexer.peekOperator(OP.COLON)) {
				fLexer.eatToken();
				fLexer.readId();
			}
		} finally {
			fScopeStack.pop();
		}
	}

	static private final Map<String, Integer> fFieldQualifers;
	static private final Map<String, Integer> fTaskFuncParamQualifiers;
	static {
		fFieldQualifers = new HashMap<String, Integer>();
		fFieldQualifers.put("local", IFieldItemAttr.FieldAttr_Local);
		fFieldQualifers.put("static", IFieldItemAttr.FieldAttr_Static);
		fFieldQualifers
				.put("protected", IFieldItemAttr.FieldAttr_Protected);
		fFieldQualifers.put("virtual", IFieldItemAttr.FieldAttr_Virtual);
		fFieldQualifers
				.put("automatic", IFieldItemAttr.FieldAttr_Automatic);
		fFieldQualifers.put("rand", IFieldItemAttr.FieldAttr_Rand);
		fFieldQualifers.put("randc", IFieldItemAttr.FieldAttr_Randc);
		fFieldQualifers.put("extern", IFieldItemAttr.FieldAttr_Extern);
		fFieldQualifers.put("const", IFieldItemAttr.FieldAttr_Const);
		fFieldQualifers.put("pure", IFieldItemAttr.FieldAttr_Pure);
		fFieldQualifers.put("context", IFieldItemAttr.FieldAttr_Context);
		fFieldQualifers.put("__sv_builtin",
				IFieldItemAttr.FieldAttr_SvBuiltin);

		fTaskFuncParamQualifiers = new HashMap<String, Integer>();
		fTaskFuncParamQualifiers.put("pure", 0); // TODO
		fTaskFuncParamQualifiers.put("virtual",
				SVDBParamPortDecl.FieldAttr_Virtual);
		fTaskFuncParamQualifiers.put("input",
				SVDBParamPortDecl.Direction_Input);
		fTaskFuncParamQualifiers.put("output",
				SVDBParamPortDecl.Direction_Output);
		fTaskFuncParamQualifiers.put("inout",
				SVDBParamPortDecl.Direction_Inout);
		fTaskFuncParamQualifiers.put("ref", SVDBParamPortDecl.Direction_Ref);
		fTaskFuncParamQualifiers.put("var", SVDBParamPortDecl.Direction_Var);
	}

	public static boolean isFirstLevelScope(String id, int modifiers) {
		return ((id == null) ||
				id.equals("class") ||
				// virtual interface is a valid field
				(id.equals("interface") && (modifiers & SVDBFieldItem.FieldAttr_Virtual) == 0)
				|| id.equals("module"));
	}

	public static boolean isSecondLevelScope(String id) {
		return (id.equals("task") || id.equals("function")
				|| id.equals("always") || id.equals("initial")
				|| id.equals("assign") 
				|| id.equals("endtask") || id.equals("endfunction"));
	}

	/*
	public void setNewStatement() {
		fNewStatement = true;
	}

	public void clrNewStatement() {
		fNewStatement = false;
	}
	 */

	/*
	 * Currently unused private String readLine(int ci) throws EOFException { if
	 * (fInputStack.size() > 0) { return fInputStack.peek().readLine(ci); } else
	 * { return ""; } }
	 */

	private String readQualifiedIdentifier() throws SVParseException {
		if (!fLexer.peekId() && !fLexer.peekKeyword()) {
			return null;
		}
		StringBuffer ret = new StringBuffer();

		ret.append(fLexer.eatTokenR());

		while (fLexer.peekOperator(OP.COLON2)) {
			ret.append(fLexer.eatTokenR());
			ret.append(fLexer.eatTokenR());
		}
		/*
		while (fLexer.peekId() || fLexer.peekOperator(OP.COLON2) || fLexer.peekKeyword()) {
			ret.append(fLexer.eatToken());
		}
		 */

		return ret.toString();
	}

	public ScanLocation getLocation() {
		int line = fInput.getLineno();
		int pos = fInput.getLinepos();
		return new ScanLocation("", line, pos);
	}
	
	public long getLocationL() {
		return SVDBLocation.pack(
				fInput.getFileId(), fInput.getLineno(), fInput.getLinepos());
	}
	
	public void debug(String msg) {
		debug(msg, null);
	}

	public void debug(String msg, Exception e) {
		if (e != null) {
			fLog.debug(msg, e);
		} else {
			fLog.debug(msg);
		}
	}
	
	public ILogHandle getLogHandle() {
		return fLog;
	}
	
	public void logLevelChanged(ILogHandle handle) {
		fDebugEn = fLog.isEnabled();
	}
	
	public String strengths(int max_strengths		// maximum number of strengths that can be here
			) throws SVParseException {
		boolean done = false;
		int num_strengths = 0;
		while (done == false)  {
			if (fLexer.peekKeyword(SVKeywords.fStrength))  {
				// TODO: Read these until done ... don't know what we are going to do with them but anyway
				fLexer.readKeyword(SVKeywords.fStrength);
				num_strengths ++; 
			}
			if (fLexer.peekOperator(OP.RPAREN))  {
				fLexer.readOperator(OP.RPAREN);
				done = true;
			}
			else  {
				// must be separated by comma's
				fLexer.readOperator(OP.COMMA);
			}
		}
		if (max_strengths < num_strengths)  {
			error("[Internal Error] Number of drive strengths '" + num_strengths + "' greater than maximum strengths '" + max_strengths + "' for this gate type");
		}
		return "";
//		return fLexer.endCapture();
	}
	public String delay_n(int max_delays) throws SVParseException {
		fLexer.readOperator(OP.HASH);
		boolean has_min_max_typ = false;	    // is formatted as nnn:nnn:nnn
		boolean done_with_params = false;		// Done with delay params
		int num_delays= 0;						// Number of delay parameters
		
		
		if (fLexer.peekOperator(OP.LPAREN)) {
			fLexer.eatToken();
			while (done_with_params == false)  {
				num_delays ++;
				// Can have up to max_delays delay operators
				// #(Delay_construct) - Normal delay
				// #(Rising_Delay_construct, Falling_Delay Construct) // Rising delay, falling delay
				// #(Rising_Delay_construct, Falling_Delay Construct, Decay_Delay_Construct) // Rising delay, falling delay
				// Where xxx_Delay_Construct is either:
				// <generic_delay>
				// <min_delay>:<typ_delay>:<max_delay>
				parsers().exprParser().expression();			// min or base
				if (fLexer.peekOperator(OP.COLON)) {
					has_min_max_typ = true;	// is formatted as nnn:nnn:nnn
					fLexer.eatToken();
					/* typ */ parsers().exprParser().expression();
	
					fLexer.readOperator(OP.COLON);
					/* max */ parsers().exprParser().expression();
				}
				if (fLexer.peekOperator(OP.RPAREN))  {
					fLexer.readOperator(OP.RPAREN);
					done_with_params = true;
				}
				else if (fLexer.peekOperator(OP.COMMA))  {
					fLexer.readOperator(OP.COMMA);
					
				}
			}
		} else {
			parsers().exprParser().expression();
		}
		
		if (num_delays > max_delays)  {
			error("[Internal Error] Number of delay parameters '" + num_delays + "' greater than maximum delay parameters'" + max_delays + "' for this gate type");
		}
		return "";
//		return fLexer.endCapture();
	}

	public void error(String msg, String filename, int lineno, int linepos) {
		if (fMarkers != null && !fDisableErrors) {
			SVDBMarker marker = new SVDBMarker(
					MarkerType.Error, MarkerKind.ParseError, msg);
			int file_id = -1;
			if (fFileMapper != null) {
				file_id = fFileMapper.mapFilePathToId(filename, false);
			}
			marker.setLocation(SVDBLocation.pack(file_id, lineno, linepos));
			fMarkers.add(marker);
		}
	}
	
	public String getFilename(long loc) {
		if (fFileMapper != null) {
			return fFileMapper.mapFileIdToPath(
					SVDBLocation.unpackFileId(loc));
		} else {
			return "FileId: " + SVDBLocation.unpackFileId(loc);
		}
	}

	@Deprecated
	public SVDBFile parse(
			InputStream 		in, 
			String 				filename, 
			List<SVDBMarker> 	markers) {
		return parse(SVLanguageLevel.SystemVerilog, in, filename, markers);
	}
	
	public SVDBFile parse(
			SVLanguageLevel		language_level,
			InputStream 		in, 
			String 				filename, 
			List<SVDBMarker> 	markers) {
		if (language_level == null) {
			fLanguageLevel = SVLanguageLevel.SystemVerilog;
		} else {
			fLanguageLevel = language_level;
		}
		
		fScopeStack.clear();
		
		fFile = new SVDBFile(filename);
		fScopeStack.clear();
		fScopeStack.push(fFile);

		fMarkers = markers;
		
		if (fMarkers == null) {
			fMarkers = new ArrayList<SVDBMarker>();
		}

		if (fDefineProvider != null) {
			fDefineProvider.addErrorListener(this);
		}

		SVPreProcessor preproc = new SVPreProcessor(
				in, filename, fDefineProvider);
		fInput = preproc.preprocess();
		
		fFile.setLocation(getLocationL());
		
		fLog.debug("File Input: " + filename);
//		fLog.debug(fInput.toString());
		
		fLexer = new SVLexer(fLanguageLevel);
		fLexer.init(this, fInput);
		fSVParsers = new SVParsers();
		fSVParsers.init(this);

		try {
			while (fLexer.peek() != null) {
				top_level_item(fFile);
			}
		} catch (SVParseException e) {
			if (fDebugEn) {
				debug("ParseException: post-process()", e);
			}
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (SVAbortParseException e) {
			// error limit exceeded
		}

		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.File) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}

		if (fDefineProvider != null) {
			fDefineProvider.removeErrorListener(this);
		}
		
		return fFile;
	}

	public SVDBFile parse(
			SVLanguageLevel		language_level,
			ITextScanner		in,
			String				filename,
			List<SVDBMarker>	markers) {
		return parse(language_level, in, filename, null, markers);
	}
	
	public SVDBFile parse(
			SVLanguageLevel		language_level,
			ITextScanner		in,
			String				filename,
			ISVTokenListener	tok_listener,
			List<SVDBMarker>	markers) {
	
		if (language_level == null) {
			fLanguageLevel = SVLanguageLevel.SystemVerilog;
		} else {
			fLanguageLevel = language_level;
		}

		fScopeStack.clear();
		
		fFile = new SVDBFile(filename);
		fScopeStack.clear();
		fScopeStack.push(fFile);

		fMarkers = markers;
		
		if (fMarkers == null) {
			fMarkers = new ArrayList<SVDBMarker>();
		}

		fInput = in;
		fLexer = new SVLexer(fLanguageLevel);
		fLexer.init(this, in);
		
		if (tok_listener != null) {
			fLexer.addTokenListener(tok_listener);
		}
		
		fSVParsers = new SVParsers();
		fSVParsers.init(this);
		
		fFile.setLocation(getLocationL());

		try {
			while (fLexer.peek() != null) {
				top_level_item(fFile);
			}
		} catch (SVParseException e) {
			if (fDebugEn) {
				debug("ParseException: post-process()", e);
			}
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (SVAbortParseException e) {
			// error limit exceeded
		} catch (NullPointerException e) {
			long loc = fLexer.getStartLocation();
			String loc_s = "Unknown";
			if (loc != -1) {
				loc_s = "" + getFilename(loc);
				loc_s += ":" + SVDBLocation.unpackLineno(loc);
			}
			fLog.error("Parser encountered a Null Pointer Exception at " + loc_s, e);
			throw e;
		}

		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.File) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}

		return fFile;
	}
	
	public void init(InputStream in, String name) {
		fScopeStack.clear();
		fFile = new SVDBFile(name);
		fScopeStack.push(fFile);

		if (fDefineProvider != null) {
			fDefineProvider.addErrorListener(this);
		}

		SVPreProcessor preproc = new SVPreProcessor(
				in, name, fDefineProvider);
		fInput = preproc.preprocess();
		
		fLexer = new SVLexer(fLanguageLevel);
		fLexer.init(this, fInput);
		fSVParsers = new SVParsers();
		fSVParsers.init(this);
	}

	// Leftover from pre-processor parser
	public void enter_package(String name) {}
	public void leave_package() {
	}

	public void leave_interface_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.InterfaceDecl) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	public void leave_class_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.ClassDecl) {
//			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	public void leave_task_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.Task) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	public void leave_func_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.Function) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	public void init(ISVScanner scanner) {}

	public void leave_module_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.ModuleDecl) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	public void leave_program_decl() {
		if (fScopeStack.size() > 0
				&& fScopeStack.peek().getType() == SVDBItemType.ProgramDecl) {
			setEndLocation(fScopeStack.peek());
			fScopeStack.pop();
		}
	}

	private void setLocation(ISVDBItemBase item) {
		item.setLocation(getLocationL());
	}

	private void setEndLocation(SVDBScopeItem item) {
		item.setEndLocation(getLocationL());
	}

	public void preproc_define(String key, List<Tuple<String, String>> params, String value) {
		SVDBMacroDef def = new SVDBMacroDef(key, value);

		setLocation(def);
		
		for (Tuple<String, String> p : params) {
			SVDBMacroDefParam mp = new SVDBMacroDefParam(p.first(), p.second());
			def.addParameter(mp);
		}

		if (def.getName() == null || def.getName().equals("")) {
			// TODO: find filename
			System.out.println("    <<UNKNOWN>> " + ":" + 
					SVDBLocation.unpackLineno(def.getLocation()));
		}

		fScopeStack.peek().addItem(def);
	}

	public void preproc_include(String path) {
		SVDBInclude inc = new SVDBInclude(path);

		setLocation(inc);
		fScopeStack.peek().addItem(inc);
	}

	public void enter_preproc_conditional(String type, String conditional) {}
	public void leave_preproc_conditional() {}
	public void comment(String comment, String name) {}

	public boolean error_limit_reached() {
		return (fParseErrorMax > 0 && fParseErrorCount >= fParseErrorMax);
	}

	public SVLexer lexer() {
		return fLexer;
	}

	public void warning(String msg, int lineno) {
		System.out.println("[FIXME] warning \"" + msg + "\" @ " + lineno);
	}

	public void error(SVParseException e) throws SVParseException {
		
		if (!fDisableErrors) {
			fParseErrorCount++;

			error(e.getMessage(), e.getFilename(), e.getLineno(), e.getLinepos());
		
			// Send the full error forward
			String msg = e.getMessage() + " " + 
					e.getFilename() + ":" + e.getLineno();
			if (fDebugEn) {
				fLog.debug("Parse Error: " + msg, e);
			}
		
			if (error_limit_reached()) {
				throw new SVAbortParseException();
			}
		
			throw e;
		}
	}
	
	public void disableErrors(boolean dis) {
		fDisableErrors = dis;
	}
	
	public void error(String msg) throws SVParseException {
		String filename = fFile.getFilePath();
		int fileid = fInput.getFileId();
		int lineno = fInput.getLineno();
		int linepos = fInput.getLinepos();
	
		if (fFileMapper != null) {
			filename = fFileMapper.mapFileIdToPath(fileid);
		}
		
		error(SVParseException.createParseException(msg, filename, lineno, linepos));
	}
	
	public SVParsers parsers() {
		return fSVParsers;
	}

	public void enter_file(String filename) {
		// TODO Auto-generated method stub
		
	}

	public void leave_file() {
		// TODO Auto-generated method stub
		
	}

}
