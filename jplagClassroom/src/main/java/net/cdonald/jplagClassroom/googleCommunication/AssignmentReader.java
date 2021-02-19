package net.cdonald.jplagClassroom.googleCommunication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.TimeOfDay;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.File;

import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.gui.utils.RetryError;
import net.cdonald.net.studentData.MiscStudentInfo;
import net.cdonald.net.studentData.StudentData;
import net.cdonald.sourceCode.FileData;
import net.cdonald.utils.SimpleUtils;

public class AssignmentReader {

	public List<ClassroomInfo> getAssignments(ClassroomInfo classroom) {
		DebugLogDialog.startMethod();
		if (classroom == null) {
			return null;
		}
		List<ClassroomInfo> assignments = new ArrayList<ClassroomInfo>();
		boolean done = false;
		while (!done) {
			assignments.clear();
			done = true;
			try {

				int sleepTime = 10;		
				ListCourseWorkResponse courseListResponse = null;
				boolean itWorked = false;
				while(itWorked == false) {
					try {								
						courseListResponse = GoogleServices.getClassroomService().courses().courseWork().list(classroom.getId()).execute();
						itWorked = true;
					}					
					catch(GoogleJsonResponseException e) {
						sleepTime = delayRetry(e, sleepTime); 
					}
				}

				for (CourseWork courseWork : courseListResponse.getCourseWork()) {
					com.google.api.services.classroom.model.Date date = courseWork.getDueDate();
					java.util.Date convertedDueDate = null;
					TimeOfDay dueTime = courseWork.getDueTime();

					if (date != null && dueTime != null) {					
						Integer hours = dueTime.getHours();
						Integer minutes = dueTime.getMinutes();

						Integer month = date.getMonth();
						Integer year = date.getYear();
						Integer day = date.getDay();


						Calendar temp = new GregorianCalendar(year, month - 1, day,
								(hours == null) ? 0 : hours, (minutes == null) ? 0 : minutes);
						long timeInMS = temp.getTimeInMillis();
						long offset = TimeZone.getDefault().getOffset(timeInMS);
						timeInMS += offset;
						temp.setTimeInMillis(timeInMS);
						convertedDueDate = temp.getTime();
					}
					else {
						convertedDueDate = SimpleUtils.createDate(courseWork.getCreationTime());
						if (convertedDueDate == null) {
							convertedDueDate = new java.util.Date();
						}
						if (convertedDueDate != null) {
							// If there is no due date, just add 4 days.
							convertedDueDate.setTime(convertedDueDate.getTime() + (4 * 24 * 3600 * 1000));
						}
					}
					String title = courseWork.getTitle();
					title = title.replaceAll("[^\\x00-\\x7F]", "");
					title = title.strip();
					ClassroomInfo data = new ClassroomInfo(title, courseWork.getId(), convertedDueDate);
					assignments.add(data);

				}
			} catch (Exception e) {
				DebugLogDialog.appendException(e);
				done = !RetryError.retryError(e,"Error Communicating With The Classroom While Attempting to Load Assignments", "Error Communicating With The Classroom While Attempting to Load Assignments");
			}

		}
		DebugLogDialog.endMethod();
		return assignments;		
	}

	/**
	 * This should be called only after we have called getStudents. This can be called multiple times to load various assignments.
	 * It reads the student submissions and stores them in the studentData class. 
	 */
	public void getStudentsSources(ClassroomInfo course, ClassroomInfo assignment, StudentData studentData, JProgressBar progress) {
		DebugLogDialog.startMethod();
		if (course == null || assignment == null || studentData == null) {
			return;
		}
		progress.setString(" Downloading student submissions for " + course.getName());
		progress.setIndeterminate(true);

		boolean done = false;
		while(!done) {
			done = true;
			Classroom classroomService =  GoogleServices.getClassroomService();
			Drive driveService = GoogleServices.getDriveService();

			try {
				ListStudentSubmissionsResponse studentSubmissionResponse = null;
				int sleepTime = 10;
				boolean itWorked = false;
				while(itWorked == false) {
					try {	

						studentSubmissionResponse = classroomService.courses().courseWork()
								.studentSubmissions().list(course.getId(), assignment.getId()).execute();
						itWorked = true;
					}
					catch(IOException e1){
						sleepTime = delayRetry(e1, sleepTime);
					}
				}
				List<StudentSubmission> submissions = studentSubmissionResponse.getStudentSubmissions();
				progress.setIndeterminate(false);
				progress.setValue(0);
				List<MiscStudentInfo> students = studentData.getMiscStudentInfoList();
				progress.setMaximum(students.size());
				Semaphore submissionsRead = new Semaphore(0);

				for (MiscStudentInfo student : students) { 
					readSubmissionThread(submissions, driveService, submissionsRead, student, progress);
				}
				while(submissionsRead.availablePermits() != students.size()) {
					Thread.sleep(100);
				}

			} catch (Exception e) {
				DebugLogDialog.appendException(e);
				done = !RetryError.retryError(e, "Error Communicating With The Classroom While Attempting to Load Student Wor For " + course.getName(), "IO Error");
			}


		}
		DebugLogDialog.endMethod();
	}

	/*
	 * Spawned in the code above. For each student we will create a new thread to potentially download that student's data.
	 * We will spin in the code above until we finish all of these threads.
	 */
	private static void readSubmissionThread(List<StudentSubmission> submissions, Drive driveService, Semaphore submissionsRead, MiscStudentInfo student, JProgressBar  progress) {
		SwingWorker<Void, Void> temp = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {

				try {
					for (StudentSubmission submission : submissions) {

						String studentIDKey = submission.getUserId();
						if (studentIDKey.equals(student.getStudentID())) {

							AssignmentSubmission assignmentSubmission = submission.getAssignmentSubmission();
							if (assignmentSubmission != null && assignmentSubmission.getAttachments() != null) {
								Date submitDate = SimpleUtils.createDate(submission.getUpdateTime());
								for (Attachment attachment : assignmentSubmission.getAttachments()) {
									DriveFile driveFile = attachment.getDriveFile();						
									File file = fileRetryLoop(driveService.files().get(driveFile.getId()));
									String fileContents = getFileContents(driveService, file, driveFile);
									String fileName = driveFile.getTitle();
									FileData studentFile = new FileData(fileName, fileContents, submitDate);
									student.addSource(studentFile);
								}
							}
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				submissionsRead.release();
				progress.setValue(submissionsRead.availablePermits());

				return null;
			}
		};

		temp.execute();	
	}

	/** 
	 * Called from the read thread. This gets the basic file information.
	 * It will spin if we get a rate based error & try again 
	 * (we almost always get rate errors).
	 * 
	 */

	private static File fileRetryLoop(Get request) throws IOException, InterruptedException {
		int sleepTime = 10;		
		File retVal = null;		
		while(true) {
			try {								
				retVal = request.execute();
				return retVal;
			}
			catch(GoogleJsonResponseException e) {
				sleepTime = delayRetry(e, sleepTime); 
			}
		}				
	}

	/** 
	 * Called from the read thread. This downloads the file and translates it into ASCII.
	 * It will spin if we get a rate based error & try again.
	 */
	private static String getFileContents(Drive driveService, File file, DriveFile driveFile) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String type = file.getMimeType();
		String fileContents = null;
		int sleepTime = 10;
		boolean itWorked = false;
		while(itWorked == false) {
			try {
				if (type.contains("text")) {
					driveService.files().get(driveFile.getId()).executeMediaAndDownloadTo(outputStream);
					fileContents = outputStream.toString("US-ASCII");
				}
				else if (type.contains("google-apps.document")) {
					driveService.files().export(driveFile.getId(), "text/plain").executeMediaAndDownloadTo(outputStream);

					fileContents = decodeGoogleDoc(outputStream.toByteArray());
					if (fileContents == null) {
						fileContents = outputStream.toString("US-ASCII");
					}
					fileContents = "// Student uploaded this as a google document, delete any weird characters and\n" +
							"//rename the class to have the same name as the file (without the .java).\n" +
							fileContents;
				} 
				else {
					fileContents = "Student uploaded file in unsupported format, nothing downloaded";								
				}
				itWorked = true;
			}
			catch(IOException e1)
			{
				try {
					sleepTime = delayRetry(e1, sleepTime);
				} catch (IOException | InterruptedException e) {
					fileContents = "Error loading file, check the submission for corruption/an empty file.";
					itWorked = true;
				}
			}
		}
		try {
			outputStream.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return fileContents;
	}

	/**
	 * Called in the many methods. Determines whether we should retry the delay after a wait.
	 * Google has a cap on how many requests we can make per second and it is super easy to go past
	 * that limit. So, if we get an error with the word rate in in (but not the daily rate).
	 * Then we keep retrying with a slightly longer delay (until we reach a second, then we just keep
	 * waiting a second).  
	 */
	public static int delayRetry(IOException e, int sleepTime) throws IOException, InterruptedException {
		
		String message = e.getMessage().toLowerCase();
		if (message.contains("read timed") || (message.contains("rate") && !message.contains("daily"))) {
			Thread.sleep(sleepTime);
			if (sleepTime >= 1000) {
				sleepTime = 1000;
			}
			else {
				sleepTime *= 2;
			}
		}
		else {
			System.err.println("Throwing" + e.getMessage());
			throw e;
		}
		return sleepTime;

	}

	private static String decodeGoogleDoc(byte[] utf8) { 

		CharsetDecoder decoder = Charset.forName("US-ASCII").newDecoder();

		decoder.onMalformedInput(
				java.nio.charset.CodingErrorAction.IGNORE);
		decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

		String str = null;
		try {			
			str = decoder.decode(ByteBuffer.wrap(utf8)).toString();
		} catch (CharacterCodingException e) {
			DebugLogDialog.appendException(e);
		}
		// set decoder back to its default value: REPORT

		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

		return str;
	}

}
