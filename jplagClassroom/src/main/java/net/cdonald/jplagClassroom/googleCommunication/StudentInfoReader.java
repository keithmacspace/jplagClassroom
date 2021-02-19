package net.cdonald.jplagClassroom.googleCommunication;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Name;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.UserProfile;

import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.gui.utils.RetryError;
import net.cdonald.net.studentData.StudentData;


public class StudentInfoReader {
	/*
	 * For a given course, fill in the list of students.
	 * Calling this clears all of the data in studentData so that we can fill in  everything.
	 */
	public void getStudents(ClassroomInfo course, StudentData studentData)  {
		DebugLogDialog.startMethod();
		if (course == null || studentData == null) {
			return;
		}

		boolean done = false;
		while(!done) {
			try {

				done = true;
				studentData.clearInfo();				
				boolean itWorked = false;
				int sleepTime = 10;
				ListStudentsResponse studentListResponse = null;
				while(itWorked == false) {
					try {								
						studentListResponse = GoogleServices.getClassroomService().courses().students().list(course.getId()).execute();
						itWorked = true;
					}					
					catch(GoogleJsonResponseException e) {
						sleepTime = AssignmentReader.delayRetry(e, sleepTime); 
					}
				}
				
				for (Student student : studentListResponse.getStudents()) {
					UserProfile studentProfile = student.getProfile();



					Name name = studentProfile.getName();
					String firstName = name.getGivenName();
					String lastName = name.getFamilyName();
					studentData.addStudent(studentProfile.getId(), firstName, lastName);
				}
			} catch (Exception e) {
				DebugLogDialog.appendException(e);
				done = !RetryError.retryError(e, "Error Communicating With The Classroom While Attempting to Load Student List For " + course.getName(), "IO Error");
			}	
		}
		DebugLogDialog.endMethod();
	}




}
