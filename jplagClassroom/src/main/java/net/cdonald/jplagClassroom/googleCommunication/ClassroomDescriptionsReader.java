package net.cdonald.jplagClassroom.googleCommunication;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.ListCoursesResponse;

import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.gui.utils.RetryError;
import net.cdonald.utils.SimpleUtils;

public class ClassroomDescriptionsReader {
	public List<ClassroomInfo> readClasses()  {
		DebugLogDialog.startMethod();
		List<ClassroomInfo> classes = new ArrayList<ClassroomInfo>();
		Classroom classroomService = GoogleServices.getClassroomService();
		boolean done = false;
		while(!done) {
			try {
				done = true;
				classes.clear();

				ListCoursesResponse response = classroomService.courses().list().execute();
				List<Course> courses = response.getCourses();
				for (Course course : courses) {	
					String creationTime = course.getCreationTime();
					Date creationDate = SimpleUtils.createDate(creationTime);
					ClassroomInfo data = new ClassroomInfo(course.getName(), course.getId(), creationDate);
					classes.add(data);
				}
			} catch (Exception e) {
				DebugLogDialog.appendException(e);
				done = !RetryError.retryError(e, "Error Communicating With The Classroom While Attempting to Load Class List", "Error Communicating With The Classroom While Attempting to Load Class List");
			}
		}
		DebugLogDialog.endMethod();		
		return classes;

	}



}
