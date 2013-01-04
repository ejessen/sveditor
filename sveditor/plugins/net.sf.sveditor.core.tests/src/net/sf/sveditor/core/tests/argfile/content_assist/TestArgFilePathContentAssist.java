package net.sf.sveditor.core.tests.argfile.content_assist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.argfile.content_assist.SVArgFileCompletionProposal;
import net.sf.sveditor.core.argfile.parser.ISVArgFileVariableProvider;
import net.sf.sveditor.core.argfile.parser.SVArgFileVariableProviderList;
import net.sf.sveditor.core.db.index.SVDBWSFileSystemProvider;
import net.sf.sveditor.core.scanutils.StringBIDITextScanner;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.TextTagPosUtils;
import net.sf.sveditor.core.tests.argfile.TestArgFileVariableProvider;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TestArgFilePathContentAssist extends SVCoreTestCaseBase {

	public void testFilePathWorkspaceRelative() throws CoreException {
		SVCorePlugin.getDefault().enableDebug(true);
		String doc =
			"${workspace_loc}/" + getName() + "/dir1/f<<MARK>>\n" 
			;
		
		IProject p = TestUtils.createProject(
				getName(), new File(fTmpDir, getName()));
		addProject(p);

		p.getFolder("dir1").create(true, true, new NullProgressMonitor());
		p.getFolder("dir2").create(true, true, new NullProgressMonitor());
		p.getFolder("dir3").create(true, true, new NullProgressMonitor());

		TestUtils.copy("", p.getFile("dir1/file1.sv"));
		TestUtils.copy("", p.getFile("dir1/file2.sv"));
		TestUtils.copy("", p.getFile("dir1/1_file.sv"));
		TestUtils.copy("", p.getFile("dir1/2_file.sv"));

		runTest(doc, "${workspace_loc}/" + getName(), p, null,
				new String[] {
					"${workspace_loc}/" + getName() + "/dir1/file1.sv",
					"${workspace_loc}/" + getName() + "/dir1/file2.sv"});
	}

	public void testFilePathProjectRelative() throws CoreException {
		SVCorePlugin.getDefault().enableDebug(true);
		String doc =
			"dir1/f<<MARK>>\n" 
			;
		
		IProject p = TestUtils.createProject(
				getName(), new File(fTmpDir, getName()));
		addProject(p);

		p.getFolder("dir1").create(true, true, new NullProgressMonitor());
		p.getFolder("dir2").create(true, true, new NullProgressMonitor());
		p.getFolder("dir3").create(true, true, new NullProgressMonitor());

		TestUtils.copy("", p.getFile("dir1/file1.sv"));
		TestUtils.copy("", p.getFile("dir1/file2.sv"));
		TestUtils.copy("", p.getFile("dir1/1_file.sv"));
		TestUtils.copy("", p.getFile("dir1/2_file.sv"));

		runTest(doc, "${workspace_loc}/" + getName(), p, null,
				new String[] {
					"dir1/file1.sv",
					"dir1/file2.sv"});
	}

	public void testFileVarPathProjectRelative() throws CoreException {
		SVCorePlugin.getDefault().enableDebug(true);
		String doc =
			"${DIR}/f<<MARK>>\n" 
			;
		
		IProject p = TestUtils.createProject(
				getName(), new File(fTmpDir, getName()));
		addProject(p);

		p.getFolder("dir1").create(true, true, new NullProgressMonitor());
		p.getFolder("dir2").create(true, true, new NullProgressMonitor());
		p.getFolder("dir3").create(true, true, new NullProgressMonitor());

		TestUtils.copy("", p.getFile("dir1/file1.sv"));
		TestUtils.copy("", p.getFile("dir1/file2.sv"));
		TestUtils.copy("", p.getFile("dir1/1_file.sv"));
		TestUtils.copy("", p.getFile("dir1/2_file.sv"));

		SVArgFileVariableProviderList vp = 
				(SVArgFileVariableProviderList)SVCorePlugin.getVariableProvider(p);
		TestArgFileVariableProvider this_vp = new TestArgFileVariableProvider();
		this_vp.setVar("DIR", "dir1");
		vp.addProvider(this_vp);

		runTest(doc, "${workspace_loc}/" + getName(), p, vp,
				new String[] {
					"${DIR}/file1.sv",
					"${DIR}/file2.sv"});
	}

	public void testIncdirVarPathProjectRelative() throws CoreException {
		SVCorePlugin.getDefault().enableDebug(true);
		String doc =
			"+incdir+${DIR}/f<<MARK>>\n" 
			;
		
		IProject p = TestUtils.createProject(
				getName(), new File(fTmpDir, getName()));
		addProject(p);

		p.getFolder("dir1").create(true, true, new NullProgressMonitor());
		p.getFolder("dir2").create(true, true, new NullProgressMonitor());
		p.getFolder("dir3").create(true, true, new NullProgressMonitor());

		TestUtils.copy("", p.getFile("dir1/file1.sv"));
		TestUtils.copy("", p.getFile("dir1/file2.sv"));
		TestUtils.copy("", p.getFile("dir1/1_file.sv"));
		TestUtils.copy("", p.getFile("dir1/2_file.sv"));
		p.getFolder("dir1/folder1").create(true, true, new NullProgressMonitor());
		p.getFolder("dir1/folder2").create(true, true, new NullProgressMonitor());
		

		TestArgFileVariableProvider this_vp = new TestArgFileVariableProvider();
		this_vp.setVar("DIR", "dir1");

		runTest(doc, "${workspace_loc}/" + getName(), p, this_vp,
				new String[] {
					"${DIR}/folder1",
					"${DIR}/folder2"});
	}

	/*
	public void testFileOptionContentAssist() {
		String doc =
			"${workspace_loc}/foo.sv\n" +
			"-I <<MARK>>\n"
			;
		TextTagPosUtils tt_utils = new TextTagPosUtils(new StringInputStream(doc));
		SVCorePlugin.getDefault().enableDebug(true);
		
		StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());
		
		TestArgFileCompletionProcessor cp = new TestArgFileCompletionProcessor(fLog);
		
		scanner.seek(tt_utils.getPosMap().get("MARK"));

		cp.computeProposals(scanner, -1, -1);
	}

	public void testIncdirPathContentAssist() {
		String doc =
			"${workspace_loc}/foo.sv\n" +
			"+incdir+/tools/include/<<MARK>>\n"
			;
		TextTagPosUtils tt_utils = new TextTagPosUtils(new StringInputStream(doc));
		SVCorePlugin.getDefault().enableDebug(true);
		
		StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());
		
		TestArgFileCompletionProcessor cp = new TestArgFileCompletionProcessor(fLog);
		
		scanner.seek(tt_utils.getPosMap().get("MARK"));

		cp.computeProposals(scanner, -1, -1);
	}
	 */

	private void runTest(
			String 							doc, 
			String							base_location,
			IProject						project,
			ISVArgFileVariableProvider		vp,
			String 							paths[]) {
		TextTagPosUtils tt_utils = new TextTagPosUtils(new StringInputStream(doc));
		
		StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());
		
		if (vp == null) {
			vp = SVCorePlugin.getVariableProvider(project);
		}
		
		TestArgFileCompletionProcessor cp = new TestArgFileCompletionProcessor(fLog);
		cp.init(new SVDBWSFileSystemProvider(), base_location, project, vp);
		
		scanner.seek(tt_utils.getPosMap().get("MARK"));
		
		cp.computeProposals(scanner, -1, -1);
		
		List<SVArgFileCompletionProposal> proposals = new ArrayList<SVArgFileCompletionProposal>();
		proposals.addAll(cp.getProposals());
		
		for (String p : paths) {
			fLog.debug("Expecting : " + p);
		}
		
		for (SVArgFileCompletionProposal p : proposals) {
			fLog.debug("Proposal : " + p.getReplacement());
		}
		
		
		List<String> exp_proposals = new ArrayList<String>();
		for (String p : paths) {
			exp_proposals.add(p);
		}

		/*
		assertEquals("Wrong number of proposals", 
				paths.length, proposals.size());
		 */
	
		for (int i=0; i<exp_proposals.size(); i++) {
			boolean found = false;
			for (int j=0; j<proposals.size(); j++) {
				if (proposals.get(j).getReplacement().equals(exp_proposals.get(i))) {
					found = true;
					proposals.remove(j);
					break;
				}
			}
			
			if (found) {
				exp_proposals.remove(i);
				i--;
			}
		}
		
		StringBuilder missing_proposals = new StringBuilder();
		StringBuilder additional_proposals = new StringBuilder();
		
		for (String p : exp_proposals) {
			missing_proposals.append(p + " ");
		}
		
		for (SVArgFileCompletionProposal p : proposals) {
			additional_proposals.append(p.getReplacement() + " ");
		}
		
		assertEquals("Missing proposals", "", missing_proposals.toString());
		assertEquals("Additional proposals", "", additional_proposals.toString());
	}
}
