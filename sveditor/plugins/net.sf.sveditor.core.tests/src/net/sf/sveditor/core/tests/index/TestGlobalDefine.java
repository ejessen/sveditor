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


package net.sf.sveditor.core.tests.index;

import java.io.File;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.db.index.SVDBIndexCollection;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.project.SVDBPath;
import net.sf.sveditor.core.db.project.SVDBProjectData;
import net.sf.sveditor.core.db.project.SVDBProjectManager;
import net.sf.sveditor.core.db.project.SVDBSourceCollection;
import net.sf.sveditor.core.db.project.SVProjectFileWrapper;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.tests.CoreReleaseTests;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.TestIndexCacheFactory;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TestGlobalDefine extends TestCase {

	private File					fTmpDir;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		fTmpDir = TestUtils.createTempDir();
		
		File db = new File(fTmpDir, "db");
		assertTrue(db.mkdirs());
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(db));
		SVCorePlugin.getDefault().getProjMgr().init();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.save_state();
		
		if (fTmpDir != null) {
			TestUtils.delete(fTmpDir);
			fTmpDir = null;
		}
	}

	public void testLibIndexGlobalDefine() {
		SVCorePlugin.getDefault().enableDebug(false);
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		CoreReleaseTests.clearErrors();
		
		IProject project_dir = TestUtils.createProject("project");
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);

		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
		
		SVProjectFileWrapper fw = p_data.getProjectFileWrapper();
		fw.getLibraryPaths().add(
				new SVDBPath("${workspace_loc}/project/basic_lib_global_defs/basic_lib_pkg.sv"));
		p_data.setProjectFileWrapper(fw);
	
		try {
			int_testGlobalDefine("testLibIndexGlobalDefine", p_data, null);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
		assertEquals(0, CoreReleaseTests.getErrors().size());
	}

	public void testArgFileIndexGlobalDefine() {
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		CoreReleaseTests.clearErrors();
		
		IProject project_dir = TestUtils.createProject("project");
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);
		
		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
		
		SVProjectFileWrapper fw = p_data.getProjectFileWrapper();
		fw.addArgFilePath("${workspace_loc}/project/basic_lib_global_defs/proj.f");
		p_data.setProjectFileWrapper(fw);
		
		try {
			int_testGlobalDefine("testArgFileIndexGlobalDefine", p_data, null);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
		assertEquals(0, CoreReleaseTests.getErrors().size());
	}

	public void testSourceCollectionIndexGlobalDefine() {
		SVCorePlugin.getDefault().enableDebug(false);
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		CoreReleaseTests.clearErrors();
		
		String pname = "project";
		IProject project_dir = TestUtils.createProject(pname);
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);
		
		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
		SVProjectFileWrapper fw = p_data.getProjectFileWrapper();
		fw.getSourceCollections().add(new SVDBSourceCollection(
				"${workspace_loc}/project/basic_lib_global_defs", true));
		p_data.setProjectFileWrapper(fw);
		
		try {
			int_testGlobalDefine("testSourceCollectionIndexGlobalDefine", p_data, null);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
		
		assertEquals(0, CoreReleaseTests.getErrors().size());
	}

	private void int_testGlobalDefine(
			String 						testname,
			SVDBProjectData				project_data,
			ISVDBIndex					index) {
		LogHandle log = LogFactory.getLogHandle(testname);
		CoreReleaseTests.clearErrors();
		
		// Add the definition that we'll check for
		SVProjectFileWrapper p_wrap = project_data.getProjectFileWrapper().duplicate();
		p_wrap.getGlobalDefines().add(new Tuple<String, String>("ENABLE_CLASS1", ""));
		project_data.setProjectFileWrapper(p_wrap);
		
		p_wrap = project_data.getProjectFileWrapper();
		assertEquals("Check that define not forgotten", 1, p_wrap.getGlobalDefines().size());
		
		SVDBIndexCollection index_mgr = project_data.getProjectIndexMgr();
		ISVDBItemIterator index_it = index_mgr.getItemIterator(new NullProgressMonitor());
		
		ISVDBItemBase class1_it = null;
		while (index_it.hasNext()) {
			ISVDBItemBase it_tmp = index_it.nextItem();
			log.debug("it " + it_tmp.getType() + " " + SVDBItem.getName(it_tmp));
			if (SVDBItem.getName(it_tmp).equals("class1")) {
				class1_it = it_tmp;
			}
		}
		
		assertNotNull("Expect to find \"class1\"", class1_it);
		assertEquals(0, CoreReleaseTests.getErrors().size());
		LogFactory.removeLogHandle(log);
	}
}
