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


public class SVToken implements ISVKeywords, ISVOperators {

	protected String						fImage;
	protected boolean						fIsString;
	protected OP							fOperator;
	protected boolean						fIsNumber;
	protected boolean						fIsTime;
	protected boolean						fIsIdentifier;
//	protected boolean						fIsKeyword;
	protected KW							fKeyword;
	protected boolean						fIsPath;
//	protected SVDBLocation					fStartLocation;
	protected long							fStartLocation;

	public SVToken duplicate() {
		SVToken ret = new SVToken();
		ret.fImage         = fImage;
		ret.fIsString      = fIsString;
		ret.fOperator      = fOperator;
		ret.fIsNumber      = fIsNumber;
		ret.fIsTime        = fIsTime;
		ret.fIsIdentifier  = fIsIdentifier;
		ret.fKeyword       = fKeyword;
		ret.fIsPath        = fIsPath;
//		ret.fStartLocation = fStartLocation.duplicate();
		ret.fStartLocation = fStartLocation;
		
		return ret;
	}
	
	public boolean isIdentifier() {
		return fIsIdentifier;
	}
	
	public boolean isNumber() {
		return fIsNumber;
	}
	
	public boolean isOperator() {
		return (fOperator != null);
	}
	
	public boolean isOperator(String ... ops) {
		if (fOperator != null) {
			for (String op : ops) {
				if (fOperator.getImg().equals(op)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isPath() {
		return fIsPath;
	}
	
	/**
	 * Return is true when the number is
	 * a time constant
	 * 
	 * @return
	 */
	public boolean isTime() {
		return fIsTime;
	}
	
	public boolean isString() {
		return fIsString;
	}
	
	public boolean isKeyword() {
		return (fKeyword != null);
	}
	
	public String getImage() {
		if (fKeyword != null) {
			return fKeyword.getImg();
		} else if (fOperator != null) {
			return fOperator.getImg();
		} else {
			return fImage;
		}
	}
	
	public long getStartLocation() {
		return fStartLocation;
	}
	
}
