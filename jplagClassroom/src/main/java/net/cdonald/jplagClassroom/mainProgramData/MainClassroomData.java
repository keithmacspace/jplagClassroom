package net.cdonald.jplagClassroom.mainProgramData;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.gui.utils.RetryError;
import net.cdonald.jplagClassroom.googleCommunication.AssignmentReader;
import net.cdonald.jplagClassroom.googleCommunication.ClassroomDescriptionsReader;
import net.cdonald.jplagClassroom.googleCommunication.ClassroomInfo;
import net.cdonald.jplagClassroom.googleCommunication.GoogleServices;
import net.cdonald.jplagClassroom.googleCommunication.StudentInfoReader;
import net.cdonald.jplagClassroom.utils.ListenerTracker;
import net.cdonald.jplagClassroom.utils.MyPreferences;
import net.cdonald.net.studentData.StudentData;

public class MainClassroomData extends ListenerTracker<MainClassroomDataListener> {
	public static final String APP_NAME="JPLAG Classroom GUI";
	private Map<String, List<ClassroomInfo>> classToAssignmentMap;
	private Map<String, StudentData> classroomStudentNames;
	private Map<String, List<ClassroomInfo>> classByYear;
	private List<String> classYears;
	private Map<String, StudentData> assignmentToStudentData;



	public MainClassroomData(MainClassroomDataListener mainListener, JProgressBar progressBar) {
		addListener(mainListener);
		classToAssignmentMap = new HashMap<String, List<ClassroomInfo>>();
		classroomStudentNames = new HashMap<String, StudentData>();
		assignmentToStudentData = new HashMap<String, StudentData>();
		classByYear = new HashMap<String, List<ClassroomInfo>>();
		classYears = new ArrayList<String>();
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				try {
					initServices();
					ClassroomDescriptionsReader classDescriptions = new ClassroomDescriptionsReader();
					initClassroomMap(classDescriptions.readClasses());							
					getListeners().forEach((l)->l.initComplete());
				}catch(Exception e) {
					DebugLogDialog.appendException(e);
					e.printStackTrace();
				}
				return null;
			}
		};
		temp.execute();
	}
	public static void initServices() {
		MyPreferences prefs = MyPreferences.getInstance();
		String tokenDir = prefs.getTokenDir();
		String creds = prefs.getJsonPath();
		boolean done = false;
		while (!done) {
			try {
				GoogleServices.initServices(APP_NAME, tokenDir, creds);
				done = true;
			} catch (IOException | GeneralSecurityException e) {
				DebugLogDialog.appendException(e);
				done = !RetryError.retryError(e, "",  "Error Initiating Communicating With Google Classroom");

				if (done == true) {
					throw new RuntimeException("Cannot start google classroom");
				}
			}
		}
	}
	private static Semaphore yearFetchSemaphore = new Semaphore(1);
	private void initClassroomMap(List<ClassroomInfo> courses) {
		boolean release = false;
		try {
			yearFetchSemaphore.acquire();
			release = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (ClassroomInfo classroom : courses) {
			Date creationDate = classroom.getDate();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(creationDate);
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH);
			// Second half of a year like 2020/2021
			if (month >= Calendar.JANUARY && month <= Calendar.JULY) {
				year--;
			}
			String yearVal = year + "/" + (year + 1);
			if (classByYear.get(yearVal) == null) {
				classYears.add(yearVal);
				classByYear.put(yearVal, new ArrayList<ClassroomInfo>());
			}
			classByYear.get(yearVal).add(classroom);
		}
		if (release) {
			yearFetchSemaphore.release();
		}
	}

	public List<String> getClassYears() {
		return classYears;
	}

	public List<ClassroomInfo> getPossibleCourses(String yearVal) {
		return classByYear.get(yearVal);

	}

	public StudentData readSubmissions(ClassroomInfo course, ClassroomInfo assignment, JProgressBar progress) {
		if (assignmentToStudentData.containsKey(assignment.getId()) == false) {
			StudentData studentList = getStudents(course, progress);
			StudentData assignmentStudents = new StudentData(studentList);
			assignmentToStudentData.put(assignment.getId(), assignmentStudents);
			AssignmentReader reader = new AssignmentReader();
			reader.getStudentsSources(course, assignment, assignmentToStudentData.get(assignment.getId()), progress);			
		}
		return assignmentToStudentData.get(assignment.getId());
	}


	public StudentData getStudents(ClassroomInfo course, JProgressBar progress) {
		if (course != null) {
			if (classroomStudentNames.containsKey(course.getId()) == false) {
				progress.setString("Reading students for " + course.getName());
				StudentInfoReader reader = new StudentInfoReader();
				StudentData data = new StudentData();
				reader.getStudents(course, data);
				classroomStudentNames.put(course.getId(), data);			
			}
			return classroomStudentNames.get(course.getId());
		}
		return null;
	}

	public void selectClass(ClassroomInfo currentClass, JProgressBar progress) {
		fillAssignments(currentClass, progress);
		getStudents(currentClass, progress);
	}

	public void fillYearComboBox(JComboBox<String> yearCombo) {
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				yearFetchSemaphore.acquire();
				try {
					List<String> years = getClassYears();
					yearCombo.addItem(null);
					for (String year : years) {
						yearCombo.addItem(year);
					}
				}catch(Exception e) {
					DebugLogDialog.appendException(e);
					e.printStackTrace();
				}
				yearFetchSemaphore.release();
				return null;
			}
		};
		temp.execute();

	}

	private static Semaphore classFetchSemaphore = new Semaphore(1);
	public void fillClassComboBox(String year, JComboBox<ClassroomInfo> classCombo, JProgressBar progress) {
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				yearFetchSemaphore.acquire();
				classFetchSemaphore.acquire();
				progress.setVisible(true);
				progress.setIndeterminate(true);
				progress.setString("fetching courses for " + year);
				try {
					classCombo.removeAllItems();
					classCombo.addItem(null);
					List<ClassroomInfo>courses = getPossibleCourses(year);					
					for (ClassroomInfo info : courses) {
						classCombo.addItem(info);
					}
				}catch(Exception e) {
					DebugLogDialog.appendException(e);
					e.printStackTrace();
				}				
				disableProgress(progress);				
				classFetchSemaphore.release();
				yearFetchSemaphore.release();
				return null;
			}
		};
		temp.execute();		
	}

	public void fillAssignments(ClassroomInfo course, JProgressBar progress) {
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				classFetchSemaphore.acquire();
				try {
					fillAssignmentsSameThread(course, progress);
				}
				catch(Exception e) {

				}
				classFetchSemaphore.release();
				return null;
			}
		};
		temp.execute();	
	}

	private void fillAssignmentsSameThread(ClassroomInfo course, JProgressBar progress) throws InterruptedException {
		if (course != null) {

			if (classToAssignmentMap.containsKey(course.getId()) == false) {
				progress.setVisible(true);
				progress.setString("fetching assignments for " + course.getName());
				progress.setIndeterminate(true);
				try {
					AssignmentReader reader = new AssignmentReader();
					List<ClassroomInfo> assignments =  reader.getAssignments(course);
					classToAssignmentMap.put(course.getId(), assignments);
				}catch(Exception e) {
					DebugLogDialog.appendException(e);
					e.printStackTrace();
				}
				disableProgress(progress);
				
			}

		}
	}
	private void disableProgress(JProgressBar progress) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progress.setVisible(false);
			}
		});
		
	}


	public void fillAssignmentComboBox(ClassroomInfo course, JComboBox<ClassroomInfo> assignmentCombo, JProgressBar progress, boolean sort) {
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				try {
				classFetchSemaphore.acquire();
				fillAssignmentsSameThread(course, progress);

				List<ClassroomInfo> info = classToAssignmentMap.get(course.getId());
				if (sort) {
					info = new ArrayList<ClassroomInfo>(info);
					info.sort(null);
				}
				assignmentCombo.removeAllItems();
				assignmentCombo.addItem(null);
				for (ClassroomInfo assignment : info) {
					assignmentCombo.addItem(assignment);
				}
				}
				catch(Exception e) {
					
				}
				classFetchSemaphore.release();				
				return null;
			}
		};
		temp.execute();	
	}
}
