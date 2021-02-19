package net.cdonald.gui.utils;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class RetryError {
	public static boolean retryError(Exception e, String additionalMessage, String title) {
		JTextArea messageArea = new JTextArea();
		messageArea.setLineWrap(true);
		messageArea.setBackground(null);
		JScrollPane scroll = new JScrollPane(messageArea);
		String message = title + "\n" + additionalMessage + "\n" + "Communication error message:\n" + e.getMessage() + "\n\nRetry?";
		messageArea.setText(message);
		DebugLogDialog.append(message);
		return JOptionPane.showConfirmDialog(null, scroll,  "Retry?",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

	}

}
