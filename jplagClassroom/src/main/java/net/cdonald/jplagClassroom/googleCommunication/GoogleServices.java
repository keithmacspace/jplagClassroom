package net.cdonald.jplagClassroom.googleCommunication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.Classroom.Builder;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.common.collect.ImmutableList;


public class GoogleServices {



	public static Classroom getClassroomService() {
		if (instance == null) {
			throw new RuntimeException("attempting to use classroom services before initialization");
		}

		return instance.classroomService;
	}

	public static Drive getDriveService() {
		if (instance == null) {
			throw new RuntimeException("attempting to use drive services before initialization");
		}

		return instance.driveService;
	}






	public static GoogleServices initServices(String applicationName, String tokensDirectoryPath, String credentialsFilePath) throws IOException, GeneralSecurityException {
		if (instance == null) {
			new GoogleServices(applicationName, tokensDirectoryPath, credentialsFilePath);
		}
		return instance;
	}

	private GoogleServices(String applicationName, String tokensDirectoryPath, String credentialsFilePath) throws IOException, GeneralSecurityException {
		instance = this;
		this.applicationName = applicationName;
		this.tokensDirectoryPath = tokensDirectoryPath.replace('\\', '/');		
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		startServices();
	}



	private void startServices() throws IOException {
		if (classroomService == null) {
			Credential credentials = getCredentials();			
			if (classroomService == null) {

				Builder classRoom = new Classroom.Builder(httpTransport, JSON_FACTORY, credentials);

				classRoom.setApplicationName(applicationName);

				classroomService = classRoom.build();

			}
			if (driveService == null) {

				driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}	
		}		
	}

	private Credential getCredentials() throws IOException {

		InputStream in = GoogleServices.class.getResourceAsStream(credentialsFilePath);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + credentialsFilePath);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
				.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

		return cred;
	}


	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = new ImmutableList.Builder<String>()
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS)
			.add(ClassroomScopes.CLASSROOM_COURSES_READONLY).add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY)
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY)
			.add(DriveScopes.DRIVE_FILE).build();

	NetHttpTransport httpTransport;
	private String applicationName;
	private String tokensDirectoryPath;
	private static final String credentialsFilePath = "/credentials.json";


	private Classroom classroomService = null;
	private Drive driveService = null;





	private static GoogleServices instance = null;

}
