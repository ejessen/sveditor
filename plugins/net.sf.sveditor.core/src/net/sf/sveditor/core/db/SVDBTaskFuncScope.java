/****************************************************************************
 * Copyright (c) 2008-2010 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;
import net.sf.sveditor.core.db.persistence.ISVDBPersistenceFactory;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceReader;

public class SVDBTaskFuncScope extends SVDBScopeItem implements IFieldItemAttr {
	private List<SVDBTaskFuncParam>			fParams;
	private int								fAttr;
	private SVDBTypeInfo					fRetType;
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			public SVDBItem readSVDBItem(IDBReader reader, SVDBItemType type, 
					SVDBFile file, SVDBScopeItem parent) throws DBFormatException {
				return new SVDBTaskFuncScope(file, parent, type, reader);
			}
		};
		
		SVDBPersistenceReader.registerPersistenceFactory(f,
				SVDBItemType.Function, SVDBItemType.Task);
	}
	
	public SVDBTaskFuncScope(String name, SVDBItemType type) {
		super(name, type);
		fParams = new ArrayList<SVDBTaskFuncParam>();
		fRetType = new SVDBTypeInfo("", 0);
	}

	public SVDBTaskFuncScope(String name, SVDBTypeInfo	ret_type) {
		super(name, SVDBItemType.Function);
		fParams = new ArrayList<SVDBTaskFuncParam>();
		fRetType = ret_type;
	}

	@SuppressWarnings("unchecked")
	public SVDBTaskFuncScope(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(file, parent, type, reader);
		fParams     = (List<SVDBTaskFuncParam>)reader.readItemList(file, this);
		fAttr       = reader.readInt();
		SVDBItemType it = reader.readItemType();
		
		
		if (it != SVDBItemType.TypeInfo) {
			throw new DBFormatException("Bad item type \"" + it + "\": expecting TypeInfo");
		}
		
		fRetType	= new SVDBTypeInfo(file, this, it, reader);
	}
	
	public void dump(IDBWriter writer) {
		super.dump(writer);
		writer.writeItemList(fParams);
		writer.writeInt(fAttr);
		fRetType.dump(writer);
	}
	
	public void setAttr(int attr) {
		fAttr = attr;
	}
	
	public int getAttr() {
		return fAttr;
	}
	
	public void addParam(SVDBTaskFuncParam p) {
		p.setParent(this);
		fParams.add(p);
	}
	
	public List<SVDBTaskFuncParam> getParams() {
		return fParams;
	}
	
	public void setParams(List<SVDBTaskFuncParam> params) {
		fParams = params;
		for (SVDBTaskFuncParam p : params) {
			p.setParent(this);
		}
	}
	
	public SVDBTypeInfo getReturnType() {
		return fRetType;
	}
	
	public void setReturnType(SVDBTypeInfo ret) {
		fRetType = ret;
	}
	public SVDBItem duplicate() {
		SVDBTaskFuncScope ret = new SVDBTaskFuncScope(getName(), getType());
		
		ret.init(this);
		
		return ret;
	}
	
	public void init(SVDBItem other) {
		super.init(other);

		fAttr = ((SVDBTaskFuncScope)other).fAttr;
		fParams.clear();
		for (SVDBTaskFuncParam p : ((SVDBTaskFuncScope)other).fParams) {
			fParams.add((SVDBTaskFuncParam)p.duplicate());
		}
		
		fRetType = ((SVDBTaskFuncScope)other).fRetType;
	}
	
}
