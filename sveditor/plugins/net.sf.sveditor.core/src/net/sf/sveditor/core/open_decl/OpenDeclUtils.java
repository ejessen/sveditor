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


package net.sf.sveditor.core.open_decl;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.index.SVDBDeclCacheItem;
import net.sf.sveditor.core.db.search.SVDBFindDefaultNameMatcher;
import net.sf.sveditor.core.db.utils.SVDBSearchUtils;
import net.sf.sveditor.core.expr_utils.SVContentAssistExprVisitor;
import net.sf.sveditor.core.expr_utils.SVExprContext;
import net.sf.sveditor.core.expr_utils.SVExprScanner;
import net.sf.sveditor.core.expr_utils.SVExprUtilsParser;
import net.sf.sveditor.core.log.ILogLevel;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.parser.SVParseException;
import net.sf.sveditor.core.scanutils.IBIDITextScanner;

import org.eclipse.core.runtime.NullProgressMonitor;

public class OpenDeclUtils {
	
	public static List<Tuple<ISVDBItemBase, SVDBFile>> openDecl_2(
			SVDBFile			file,
			int					line,
			IBIDITextScanner	scanner,
			ISVDBIndexIterator	index_it) {
		Tuple<SVExprContext, ISVDBScopeItem> context_scope = getContextScope(file, line, scanner);
		
		List<OpenDeclResult> result = openDecl(context_scope.first(), context_scope.second(), index_it);
		
		List<Tuple<ISVDBItemBase, SVDBFile>> ret = new ArrayList<Tuple<ISVDBItemBase,SVDBFile>>();
		for (OpenDeclResult r : result) {
			ret.add(new Tuple<ISVDBItemBase, SVDBFile>(r.getItem(), r.getFile()));
		}
		
		return ret;
	}
	
	public static Tuple<SVExprContext, ISVDBScopeItem> getContextScope(
			SVDBFile			file,
			int					line,
			IBIDITextScanner	scanner) {
		LogHandle log = LogFactory.getLogHandle("getContextScope");
		
		SVExprScanner			expr_scanner = new SVExprScanner();
		log.debug(ILogLevel.LEVEL_MID, "getContextScope: " + file.getFilePath() + ":" + line);
		SVExprContext expr_ctxt = expr_scanner.extractExprContext(scanner, true);
		
		log.debug("Expression Context: root=" + expr_ctxt.fRoot +
				" trigger=" + expr_ctxt.fTrigger + " leaf=" + expr_ctxt.fLeaf);
		
		ISVDBScopeItem active_scope = SVDBSearchUtils.findActiveScope(file, line);

		if (active_scope != null) {
			log.debug(ILogLevel.LEVEL_MID, "active_scope:");
			ISVDBChildItem i = active_scope;
			String ind = "";
			while (i != null) {
				log.debug(ILogLevel.LEVEL_MID, 
						ind + SVDBItem.getName(i) + " " + i + " " + i.getParent());
				if (i.getType() == SVDBItemType.File) {
					log.debug(ILogLevel.LEVEL_MID,
							"File: " + (SVDBFile)i + " ; " + file);
				}
				ind += "    ";
				i = i.getParent();
			}
		} else {
			log.debug(ILogLevel.LEVEL_MID, "active_scope: null");
		}

		return new Tuple<SVExprContext, ISVDBScopeItem>(expr_ctxt, active_scope);
	}
	
	public static String extractMacroCall(IBIDITextScanner scanner, boolean has_params) {
		long start = scanner.getPos();
		long end;
	
		int ch = scanner.get_ch();
		
		if (ch != '`') {
			return null; // not a macro call
		}
		
		while ((ch = scanner.get_ch()) != -1 && Character.isWhitespace(ch)) {
			// Skip whitespace
		}
		
		if (ch == -1) {
			return null;
		}
		
		// Read the identifier
		scanner.readIdentifier(ch);
		
		
		if (has_params) {
			while ((ch = scanner.get_ch()) != -1 && Character.isWhitespace(ch)) {
				// Skip whitespace
			}
			
			if (ch == '(') {
				int matchLevel=1, last_ch = -1;
				boolean in_string = false;

				do {
					ch = scanner.get_ch();

					if (!in_string) {
						if (ch == '(') {
							matchLevel++;
						} else if (ch == ')') {
							matchLevel--;
						} else if (ch == '\"' && last_ch != '\\') {
							in_string = true;
						}
					} else if (ch == '\"' && last_ch != '\\') {
						in_string = false;
					}
				} while (ch != -1 && matchLevel > 0);
				
				if (ch == -1) {
					return null;
				}
			} else {
				// Error, since we're missing parameters
				return null;
			}
		}

		end = scanner.getPos();
		
		return scanner.get_str(start, (int)(end-start));
	}

	
	public static List<OpenDeclResult> openDecl(
			SVExprContext		expr_ctxt,
			ISVDBScopeItem		active_scope,
			ISVDBIndexIterator	index_it) {
		LogHandle log = LogFactory.getLogHandle("OpenDeclaration");
		SVDBFile 				inc_file = null;
		
		log.debug("Expression Context: root=" + expr_ctxt.fRoot +
				" trigger=" + expr_ctxt.fTrigger + " leaf=" + expr_ctxt.fLeaf);
		
		List<OpenDeclResult> ret = new ArrayList<OpenDeclResult>();

		// If this is an include lookup, then use a different matching strategy
		if (expr_ctxt.fTrigger != null && expr_ctxt.fTrigger.equals("`")) {
			if (expr_ctxt.fRoot != null && expr_ctxt.fRoot.equals("include")) {
				findMatchingIncludeFiles(log, ret, expr_ctxt, index_it);
			} else if (expr_ctxt.fRoot == null) {
				for (SVDBDeclCacheItem it : index_it.findGlobalScopeDecl(
						new NullProgressMonitor(), expr_ctxt.fLeaf, 
						SVDBFindDefaultNameMatcher.getDefault())) {
					if (it.getType() == SVDBItemType.MacroDef) {
						ret.add(new OpenDeclResult(
								it.getFile(),
								it.getFilePP(),
								it.getSVDBItem()));
					}
				}
			}
		} else { // not a pre-processor expression
			SVExprUtilsParser expr_parser = new SVExprUtilsParser(expr_ctxt, true);
			SVDBExpr expr = null;
			
			try {
				expr = expr_parser.parsers().exprParser().expression();
			} catch (SVParseException e) {
				log.debug("Failed to parse open-declaration expression: " + 
						e.getMessage(), e);
			}

			if (expr != null) {
				SVContentAssistExprVisitor v = new SVContentAssistExprVisitor(
						active_scope, SVDBFindDefaultNameMatcher.getDefault(), index_it);
				ISVDBItemBase item = v.findItem(expr);
				
				
				if (item != null) {
					ret.add(new OpenDeclResult(
							inc_file,
							null,
							item));
				}
			}
		}

		/*
		List<ISVDBItemBase> items = expr_utils.findItems(index_it, active_scope, expr_ctxt, false);
		
		if (items.size() > 0) {
			it = items.get(0);
			
			// Confused here...
			ret.add(new Tuple<ISVDBItemBase, SVDBFile>(it, inc_file));
		}
		 */
		
		log.debug(ILogLevel.LEVEL_MID, "Result:");
		for (OpenDeclResult r : ret) {
			String ind="";
			ISVDBItemBase i = r.getItem();
			while (i != null) {
				log.debug(ILogLevel.LEVEL_MID, ind + SVDBItem.getName(i));
				ind += "    ";
				if (i instanceof ISVDBChildItem) {
					i = ((ISVDBChildItem)i).getParent();
				} else {
					i = null;
				}
			}
		}
		
		return ret;
	}
	
	private static void findMatchingIncludeFiles(
			LogHandle								log,
			List<OpenDeclResult>					ret,
			SVExprContext 							expr_ctxt,
			ISVDBIndexIterator						index_it) {
		boolean debug_en = log.isEnabled();
		
		String target = expr_ctxt.fLeaf;
		String leaf = target;
		int idx=-1;
		
		if (debug_en) {
			log.debug(ILogLevel.LEVEL_MID, "--> findMatchingIncludeFiles: \"" + target + "\"");
		}
		
		if ((idx = leaf.lastIndexOf('/')) != -1) {
			leaf = leaf.substring(idx+1);
		}
		
		// Strip off any relative-path elements
		while (target.startsWith("../")) {
			target = target.substring(3);
		}
		
		if (debug_en) {
			log.debug(ILogLevel.LEVEL_MID, "  leaf=" + leaf + " target=" + target);
		}
		
		for (String filename : index_it.getFileList(new NullProgressMonitor())) {
			if (debug_en) {
				log.debug("  Testing file: " + filename);
			}
			int f_idx = filename.lastIndexOf('/');
			if (f_idx == -1) {
				// only a leaf name in the filename
				if (filename.equals(leaf)) {
					SVDBFile item = new SVDBFile(filename);
					// FIXME:
					if (debug_en) {
						log.debug("    Adding based on the leafname");
					}
					ret.add(new OpenDeclResult(
							item,
							item,
							item));
				}
			} else {
				if (filename.endsWith(target)) {
					if (debug_en) {
						log.debug("    Adding based on the target name");
					}
					SVDBFile item = new SVDBFile(filename);
					// FIXME:
					ret.add(new OpenDeclResult(
							item,
							item,
							item));
				}
			}
		}
		
		if (debug_en) {
			log.debug(ILogLevel.LEVEL_MID, "<-- findMatchingIncludeFiles: \"" + 
					target + "\" " + ret.size() + " found");
		}
	}

}
