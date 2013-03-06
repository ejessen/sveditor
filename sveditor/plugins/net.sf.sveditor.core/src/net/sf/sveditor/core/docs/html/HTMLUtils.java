/****************************************************************************
 * Copyright (c) 2008-2011 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Armond Paiva - initial implementation
 ****************************************************************************/

package net.sf.sveditor.core.docs.html;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.sveditor.core.docs.DocGenConfig;
import net.sf.sveditor.core.docs.DocTopicType;


public class HTMLUtils {
	
	static final String STR_DOCTYPE = 
			"<!DOCTYPE HTML PUBLIC "
				+ "\"-//W3C//DTD HTML 4.0//EN\""
				+ "\"http://www.w3.org/TR/REC-html40/strict.dtd\">" ;
	
	static String genHTMLHeadStart(String relPathToHTML, String title) {
		String result =
			  "	<html><head>"
			+ "	<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
			+ "	<title>" + title + "</title>"
			+ "	<link rel=\"stylesheet\" type=\"text/css\" "
			+ "		href=\"" + relPathToHTML + "/styles/main.css\">"
			+ "	<script language=JavaScript src=\"" + relPathToHTML + "/javascript/main.js\"></script>"
			+ "	<script language=JavaScript src=\"" + relPathToHTML + "/javascript/prettify.js\"></script>"
			+ "	<script language=JavaScript src=\"" + relPathToHTML + "/javascript/searchdata.js\"></script>"
			+ "</head>" ;
		return result ;
		
	}
	
	static String genBodyBegin(String bodyClass) {
		String result =
			  "<body class=\"" + bodyClass + "\" onLoad=\"NDOnLoad();prettyPrint();\">"
			+ "<script language=JavaScript>"
			+ "<!-- if (browserType) {document.write(\"<div class=\" + browserType + \">\");"
			+ "	 if (browserVer)  {document.write(\"<div class=\" + browserVer + \">\"); }}"
			+ "--></script>"
			+ "<!--  Generated by SVEditor -->"
			+ "<!--  http://sveditor.sourceforge.net-->" ;
		return result ;
	}
	
	static String genFooter() {
		String result =
				"<div id=Footer><a href=\"http://sveditor.sourceforge.net\">Generated by SVEditor</a></div>" ;
		return result ;
	}
	
	static String genBodyHTMLEnd() {
		String result =
			  "<script language=JavaScript>"
			+ "<!-- if (browserType) {if (browserVer) {document.write(\"</div>\"); }document.write(\"</div>\");}"
			+ "--></script></body></html>" ;
		return result ;
	}
	
	
	static String genContentBegin() {
		String result =
			  "<div id=Content>" ;
		return result ;
	}
	
	static String genContentEnd() {
		return genDivEnd() ;
	}
	
	static String genCTopicBegin(String topicID) {
		String result =
			  "<div class=CTopic id=" + topicID + ">" ;
		return result ;
	}
	static String genCTopicEnd() {
		return genDivEnd() ;
	}
	
	static String genCTitle(String name) {
		String result =
			  "<h1 class=CTitle><a name=\"" + name + "\"></a>" + name + "</h1>" ;
		return result ;
	}
	
	static String genCBodyBegin() {
		String result =
			  "<div class=CBody>" ;
		return result ;
	}
	static String genCBodyEnd() {
		return genDivEnd() ;
	}
	
	static String genSummaryBegin() {
		String result =
			  "<div class=Summary>" ;
		return result ;
	}
	static String genSummaryEnd() {
		return genDivEnd() ;
	}
	static String genSTitle() {
		String result =
			  "<div class=STitle>Summary</div>" ;
		return result ;
	}
	static String genSBorderBegin() {
		String result =
			  "<div class=SBorder>" ;
		return result ;
	}
	static String genSBorderEnd() {
		return genDivEnd() ;
	}
	
	static String genSTableBegin() {
		String result =
			  "<table border=0 cellspacing=0 cellpadding=0 class=STable>" ;
		return result ;
	}
	static String genSTableEnd() {
		return genTableEnd() ;
	}
	
	static String genDivEnd() {
		String result =
			  "</div>" ;
		return result ;
	}
	static String genTableEnd() {
		String result =
			  "</table>" ;
		return result ;
	}
	
	/**
	 * @param topicName
	 * @return The CSS class for the given topic. Ex. CFunction for topicName=function
	 */
	static String genCSSClassForTopic(String topicName) {
		String c = "C" ;
		if(topicName == null || topicName.length() < 1) {
			// FIXME: report error
			return "CUnknown" ;
		}
		c += capitalize(topicName) ;
		return c ;
	}
	
	static String capitalize(String in) {
		String out = "" ;
		out = out + in.substring(0,1).toUpperCase() ;
		out = out + in.substring(1).toLowerCase() ;
		return out ;
	}
	
	/**
	 * @param topicName
	 * @return The CSS class for the given topic within a summary. Ex. CFunction for topicName=function
	 */
	static String genCSSClassForTopicInSummary(String topicName) {
		String c = "S" ;
		if(topicName == null || topicName.length() < 1) {
			// FIXME: report error
			return "CUnknown" ;
		}
		c += capitalize(topicName) ;
		return c ;
	}
	
	static String genMenu(DocGenConfig cfg, String relPathToHTML, String title, Collection<DocTopicType> docTopicTypes) {
		String res = 
			"<div id=Menu>"
				+ "<div class=MEntry>"
					+ "<div class=MFile id=MSelected>" + title + "</div>"
				+ "</div>"
				+ "<div class=MEntry>"
				+ "<div class=MGroup><a href=\"javascript:ToggleMenu('MGroupContent1')\">Index</a>"
					+ "<div class=MGroupContent id=MGroupContent1>" ; 
//						+ "<div class=MEntry>"
//							+ "<div class=MIndex><a href=\"" + relPathToHTML + "/index/General.html\">Everything</a> </div>"
//						+ "</div>"
		for(DocTopicType docTopicType: docTopicTypes) {
			if(!docTopicType.isIndex()) continue ;
			res +=   	  "<div class=MEntry>"
							+ "<div class=MIndex><a href=\"" 
								+ relPathToHTML 
								+ "/"
								+ getHTMLRelPathForIndexOfTopic(cfg, docTopicType.getPluralName())
								+ "\">"
								+ docTopicType.getPluralNameCapitalized()
								+ "</a></div>"
						+ "</div>" ;
		}
		res += 			  "</div>"
						+ "</div>"
						+ "</div>"
						+ "</div>"
//							+ "<script type=\"text/javascript\">"
//							+ "<!-- var searchPanel = new SearchPanel(\"searchPanel\", \"HTML\", \"" +relPathToHTML + "/search\"); -->"
//							+ "</script><div id=MSearchPanel class=MSearchPanelInactive>"
//								+ "<input type=text id=MSearchField value=Search "
//									+ "onFocus=\"searchPanel.OnSearchFieldFocus(true)\" "
//									+ "onBlur=\"searchPanel.OnSearchFieldFocus(false)\" "
//									+ "onKeyUp=\"searchPanel.OnSearchFieldChange()\"> "
//								+ "<select id=MSearchType onFocus=\"searchPanel.OnSearchTypeFocus(true)\"" 
//											+ "onBlur=\"searchPanel.OnSearchTypeFocus(false)\""
//											+ "onChange=\"searchPanel.OnSearchTypeChange()\">"
//								+ "<option  id=MSearchEverything selected value=\"General\">Everything</option>"
//								+ "<option value=\"Classes\">Classes</option>"
//								+ "<option value=\"Functions\">Functions</option>"
//								+ "<option value=\"Variables\">Variables</option>"
//							+ "</select>"
					+ "</div>"
				+ "</div>"
		+ "<!--Menu-->" ;
		return res ;
	}	
	
	public static File getHTMLFileForIndexOfTopic(DocGenConfig cfg, String topicName) {
		return new File(HTMLUtils.getHTMLDir(cfg),getHTMLRelPathForIndexOfTopic(cfg, topicName).toString()) ;
	}
	
	public static File getHTMLRelPathForIndexOfTopic(DocGenConfig cfg, String topicNamePlural) {
		String topicFileName = 
				topicNamePlural.substring(0, 1).toUpperCase() +
				topicNamePlural.substring(1).toLowerCase() +
				".html";
		File htmlRelIndexFile = new File(new File("index"), topicFileName) ;
		return htmlRelIndexFile ;
	}
	
	public static File getHTMLFileForClass(DocGenConfig cfg, String pkgName, String className) {
		return new File(getPkgClassDir(cfg,pkgName),className + ".html") ;
	}
	
	public static File getHTMLDir(DocGenConfig cfg) {
		return new File(cfg.getOutputDir(), "html") ;
	}	
	
	public static File getFilesDir(DocGenConfig cfg) {
		return new File(getHTMLDir(cfg), "files") ;
	}
	
	public static File getPkgClassDir(DocGenConfig cfg, String pkgName) {
		return new File(getClassesDir(cfg),pkgName) ;
	}

	public static File getClassesDir(DocGenConfig cfg) {
		return  new File(getHTMLDir(cfg),"classes") ;
	}

	public static File getStylesDir(DocGenConfig cfg) {
		return new File(getHTMLDir(cfg), "styles") ;
	}
	public static File getScriptsDir(DocGenConfig cfg) {
		return new File(getHTMLDir(cfg), "scripts") ;
	}

	public static File getHTMLFileForSrcPath(DocGenConfig cfg, String srcPath) {
		return new File(getFilesDir(cfg),srcPath+".html") ;
	}	
	
	//
	//   Replaces <NDMarkup> amp chars with their original symbols.
	//
	public static String restoreAmpChars(String text) {
	
	    text = text.replaceAll("&quot;","\"") ;
	    text = text.replaceAll("&gt;",">") ;
	    text = text.replaceAll("&lt;","<") ;
	    text = text.replaceAll("&amp;","&") ;

	    return text ;
		
    }

	public static String makeRelativeURL(String basePath, String targetPath, boolean baseHasFileName) {
		String ret = "" ; 
		File baseFile = new File(basePath) ;
		File targetFile = new File(targetPath) ;
		if(baseHasFileName) {
			baseFile = baseFile.getParentFile() ;
		}
		
		List<String> baseDirs = new ArrayList<String>(Arrays.asList(baseFile.toString().split("/"))) ;
		List<String> targetDirs = new ArrayList<String>(Arrays.asList(targetFile.toString().split("/"))) ;
		
		
	    // Skip the parts of the path that are the same.
		int idx = 0 ;
		while(idx < baseDirs.size() && idx < targetDirs.size() && baseDirs.get(idx).equals(targetDirs.get(idx)))
	        {
				idx++ ;
	        };
	        
	    baseDirs = baseDirs.subList(idx, baseDirs.size()) ;   
	    targetDirs = targetDirs.subList(idx, targetDirs.size()) ;   
	    

	    // Back out of the base path until it reaches where they were similar.
	    for(idx = 0 ; idx < baseDirs.size() ; idx++) 
	        {
	    	targetDirs.add(0,"..") ;
	        } ;
	        
	    for(idx=0 ; idx< targetDirs.size() ; idx++) {
	    	ret += targetDirs.get(idx) ;
	    	if(targetDirs.size() > 1 && idx < (targetDirs.size()-1)) {
	    		ret += "/" ;
	    	}
	    }
		
		return ret ;
	}
	
	
}
