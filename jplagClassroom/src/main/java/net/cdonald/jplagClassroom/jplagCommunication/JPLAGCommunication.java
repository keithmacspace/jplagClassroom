package net.cdonald.jplagClassroom.jplagCommunication;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import jplag.ExitException;
import jplag.Program;
import jplag.options.CommandLineOptions;
import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.jplagClassroom.utils.FileUtils;
import net.cdonald.jplagClassroom.utils.MyPreferences;
import net.cdonald.net.studentData.MiscStudentInfo;
import net.cdonald.net.studentData.StudentData;
import net.cdonald.sourceCode.FileData;
public class JPLAGCommunication {
	private static final String MAIN_COMPARE = "JPLAG_MAIN";
	private static final String OTHER_COMPARE = "JPLAG_OTHERS";
	private static final String OUT_DIR = "JPLAG_OUT";
	private String mainPath;
	private String othersPath;
	private String outPath;
	private boolean hasOthers = false;
	public JPLAGCommunication() {
		mainPath = initPath(MAIN_COMPARE);
		othersPath = initPath(OTHER_COMPARE);
		outPath = initPath(OUT_DIR);

	}
	private String initPath(String subDir) {
		String workingDir = MyPreferences.getInstance().getWorkingDir();
		String assignmentPath = workingDir + File.separator + subDir;

		try {
			FileUtils.removeRecursive(Paths.get(assignmentPath));
		} catch (IOException e1) {
			DebugLogDialog.appendException(e1);
		}
		return assignmentPath;

	}
	public void saveMainData(StudentData studentData) {
		addToSaveArea(mainPath, studentData);
	}
	public void addToCompareData(StudentData otherStudents) {
		hasOthers = true;
		addToSaveArea(othersPath, otherStudents);		
	}

	private void addToSaveArea(String assignmentPath, StudentData students) {
		new File(assignmentPath).mkdir();
		for (MiscStudentInfo student : students.getMiscStudentInfoList()) {
			List<FileData> studentFiles = student.getSourceCode();

			if (studentFiles != null && studentFiles.size() != 0) {
				String studentPath = assignmentPath + File.separator + student.getFullName();

				new File(studentPath).mkdir();
				for (FileData file : studentFiles) {
					String studentFileName = studentPath + File.separator + file.getName();
					try {
						PrintWriter out = new PrintWriter(studentFileName);
						out.print(file.getFileContents());
						out.flush();
						out.close();
					} catch (FileNotFoundException e) {
						DebugLogDialog.appendException(e);
					}
				}
			}

		}		
	}
	public class FinalOutput {
		private boolean success;
		private String finalOutput;
		private String jplagOut;
		private Exception exception;
		public Exception getException() {
			return exception;
		}
		public void setException(Exception exception) {
			this.exception = exception;
		}
		public FinalOutput() {

			super();
			jplagOut = outPath;
			jplagOut += File.separator + "index.html";
			this.finalOutput = outPath;
			finalOutput = jplagOut.replace("\\", "/");
			success = false;
			exception = null;
		}
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public String getFinalOutput() {
			return finalOutput;
		}
		public String getJPLAGOut() {
			return jplagOut;
		}

	}
	public FinalOutput runJPLAG(String language) throws ExitException {
		FinalOutput finalOutput = new FinalOutput();
		try {
			String [] noOthers = {"-vq", "-s",  mainPath, "-r",  outPath,  "-l", language};
			String []args = noOthers;
			String [] others = new String[noOthers.length + 2];
			if (hasOthers) {
				for (int i = 0; i < noOthers.length; i++) {
					others[i] = noOthers[i];
				}
				others[others.length - 2] = "-prior";
				others[others.length - 1] = othersPath;
				args = others;
			}
			String jplagOut = outPath;
			jplagOut += File.separator + "index.html";

			CommandLineOptions options = new CommandLineOptions(args, null);
			Program temp = new Program(options);
			temp.run();




			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				try {
					URI u = new File(jplagOut).toURI();
					Desktop.getDesktop().browse(u);
					finalOutput.setSuccess(true);
				} catch (IOException e) {
					finalOutput.setException(e);

				}
			}
		}catch(Exception e) {
			finalOutput.setException(e);
			e.printStackTrace();
		}


		return finalOutput;
	}

	public void diplayFinalMessage(FinalOutput output) {
		if (output != null) {
			JTextArea message = new JTextArea(3,100);
			if (output.getException() != null) {
				message.setText("Exception while running jplag. Exception message:" + output.getException().getMessage());
			}
			else {
				message.setText("Attempting to open browser with results.\nIf it does not open, manually copy this path into your browser:\n" + output.getFinalOutput());
			}
			message.setWrapStyleWord(true);
			message.setLineWrap(true);
			message.setCaretPosition(0);
			message.setEditable(false);
			JLabel background = new JLabel();
			message.setBackground(background.getBackground());
			JPopupMenu popupSource = new JPopupMenu();
			Action copy = new DefaultEditorKit.CopyAction();
			copy.putValue(Action.NAME, "Copy");
			copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
			popupSource.add(copy);
			message.setComponentPopupMenu(popupSource);
			JOptionPane.showMessageDialog(null, new JScrollPane(message),  "Open Results",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
