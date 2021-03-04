package net.cdonald.jplagClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.cdonald.gui.utils.LoadSource;
import net.cdonald.jplagClassroom.googleCommunication.ClassroomInfo;
import net.cdonald.jplagClassroom.jplagCommunication.JPLAGCommunication;
import net.cdonald.jplagClassroom.jplagCommunication.JPLAGCommunication.FinalOutput;
import net.cdonald.jplagClassroom.jplagCommunication.JPLAGLanguages;
import net.cdonald.jplagClassroom.mainProgramData.MainClassroomData;
import net.cdonald.jplagClassroom.mainProgramData.MainClassroomDataListener;
import net.cdonald.jplagClassroom.utils.MyPreferences;
import net.cdonald.net.studentData.StudentData;
import net.cdonald.sourceCode.FileData;

public class MainPanel extends JPanel implements MainClassroomDataListener {
	private static final long serialVersionUID = 1L;
	private OtherClassesTable compareAssignments;
	private MainClassroomData classroomData;
	private JComboBox<ClassroomInfo> classCombo;
	private JComboBox<ClassroomInfo> assignmentCombo;
	private JComboBox<JPLAGLanguages> languageCombo;
	private List<FileData> baseCode = null;
	private JProgressBar progressBar;
	/**
	 * Create the panel.
	 */
	public MainPanel() {
		progressBar = new JProgressBar();
		classroomData = new MainClassroomData(this, progressBar);
		setLayout(new BorderLayout(0, 0));

		JToolBar mainToolBar = new JToolBar();
		mainToolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		add(mainToolBar, BorderLayout.NORTH);

		JLabel lblNewLabel = new JLabel("Class:");
		mainToolBar.add(lblNewLabel);

		assignmentCombo = new  JComboBox<ClassroomInfo>();		
		classCombo = new JComboBox<ClassroomInfo>();
		classCombo.addItemListener((l)->{
			if (l.getStateChange() == ItemEvent.SELECTED) {
				assignmentCombo.removeAllItems();
				classroomData.fillAssignmentComboBox((ClassroomInfo)classCombo.getSelectedItem(), assignmentCombo, progressBar, false);
			}
		});
		mainToolBar.add(classCombo);

		JLabel lblNewLabel_1 = new JLabel("Assignment");
		mainToolBar.add(lblNewLabel_1);


		mainToolBar.add(assignmentCombo);

		JLabel lblNewLabel_2 = new JLabel("Language");
		mainToolBar.add(lblNewLabel_2);

		languageCombo = new JComboBox<JPLAGLanguages>(JPLAGLanguages.values());
		mainToolBar.add(languageCombo);
		
		JButton baseCodeButton = new JButton("Base Code...");
		baseCodeButton.setToolTipText("Initial source supplied by the teacher");
		mainToolBar.add(baseCodeButton);
		baseCodeButton.addActionListener((l)->{baseCode = LoadSource.loadSource(this);});
		

		JPanel mainStatusBar = new JPanel();

		

		mainStatusBar.setLayout(new BorderLayout());
		add(mainStatusBar, BorderLayout.SOUTH);
		JButton startCompare = new JButton("Start Compare");
		startCompare.addActionListener((l)->{
			performJPLAGCompare();
		});
		mainStatusBar.add(progressBar, BorderLayout.CENTER);
		mainStatusBar.add(startCompare, BorderLayout.EAST);


		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(true);
		progressBar.setString("Loading Classroom Data");



		JPanel otherClassesToCompare = new JPanel();
		otherClassesToCompare.setLayout(new BorderLayout(0, 0));
		otherClassesToCompare.setBorder(BorderFactory.createTitledBorder("Compare With Other Assignments"));
		add(otherClassesToCompare, BorderLayout.CENTER);

		compareAssignments = new OtherClassesTable(classroomData, progressBar);
		JScrollPane scrollPane = new JScrollPane(compareAssignments);
		otherClassesToCompare.add(scrollPane, BorderLayout.CENTER);

	}


	@Override
	public void initComplete() {
		classCombo.addItem(null);
		List<String> years = classroomData.getClassYears();
		if (years.size() > 0) {
			classroomData.fillClassComboBox(years.get(0), classCombo, progressBar);			
		}
		MyPreferences prefs = MyPreferences.getInstance();
		ClassroomInfo currentClass = prefs.getClassroom();
		if (currentClass != null) {
			for (int i = 1; i < classCombo.getItemCount(); i++) {
				ClassroomInfo classItem = classCombo.getItemAt(i);
				if (classItem.getId().equals(currentClass.getId())) {
					classCombo.setSelectedIndex(i);
					break;
				}
			}
		}
	}
	private static Semaphore classFetchSemaphore = new Semaphore(1);
	private void performJPLAGCompare() {
		if (classFetchSemaphore.tryAcquire()) {
			SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					
					try {
						JPLAGCommunication jplagComm = new JPLAGCommunication();	
						ClassroomInfo course = (ClassroomInfo)classCombo.getSelectedItem();
						ClassroomInfo assignment = (ClassroomInfo)assignmentCombo.getSelectedItem();
						JPLAGLanguages language = (JPLAGLanguages)languageCombo.getSelectedItem();
						if (course != null && assignment != null) {
							
							progressBar.setVisible(true);
							progressBar.setIndeterminate(true);
							StudentData mainData = classroomData.readSubmissions(course, assignment, progressBar);
							jplagComm.saveMainData(mainData);
							List<OtherClassesTable.SelectedAssignment> others = compareAssignments.getSelectedAssignments();
							
							for (OtherClassesTable.SelectedAssignment otherAssignment : others) {
								StudentData otherStudents = classroomData.readSubmissions(otherAssignment.getCourse(), otherAssignment.getAssignment(), progressBar); 
								jplagComm.addToCompareData(otherStudents);								
							}
						}
						if (baseCode != null) {
							jplagComm.saveBaseCode(baseCode);
						}
						progressBar.setIndeterminate(true);
						progressBar.setString("Running JPLAG");
						FinalOutput finalMessage = jplagComm.runJPLAG(language.getJPLAGName());
						
						
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								progressBar.setVisible(false);
							}
						});
						jplagComm.diplayFinalMessage(finalMessage);
						
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					classFetchSemaphore.release();				
					return null;
				}
			};
			temp.execute();	
		}

	}


}
