package net.cdonald.jplagClassroom.componentTests;

import java.io.IOException;
import java.security.GeneralSecurityException;

import net.cdonald.gui.utils.DebugLogDialog;
import net.cdonald.gui.utils.RetryError;
import net.cdonald.jplagClassroom.googleCommunication.GoogleServices;
import net.cdonald.jplagClassroom.utils.MyPreferences;

public class TestServicesInit {
	public static void main(String []args) {
		MyPreferences prefs = MyPreferences.getInstance();
		String tokenDir = prefs.getTokenDir();
		String creds = prefs.getJsonPath();
		boolean done = false;
		while (!done) {
			try {
				GoogleServices.initServices("JPLAG Classroom", tokenDir, creds);
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
}
