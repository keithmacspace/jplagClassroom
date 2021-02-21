package net.cdonald.gui.utils;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;

import net.cdonald.jplagClassroom.utils.MyPreferences;
import net.cdonald.sourceCode.FileData;


public class LoadSource {
	public static List<FileData> loadSource(Component parent) {
		JFileChooser fileChooser = null;

		MyPreferences prefs = MyPreferences.getInstance();
		String currentWorkingDir = prefs.getFileDir();
		if (currentWorkingDir != null) {
			fileChooser = new JFileChooser(currentWorkingDir);
		} else {
			fileChooser = new JFileChooser();
		}
		fileChooser.setMultiSelectionEnabled(true);
		List<FileData> allFiles = Collections.synchronizedList(new ArrayList<FileData>());
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			for (File file : fileChooser.getSelectedFiles()) { 
				Path path = Paths.get(file.getAbsolutePath());
				prefs.setFileDir(path.getParent().toString());

				String fileName = path.getFileName().toString();
				
				try {
					String text = new String(Files.readAllBytes(path));
					FileData fileData = new FileData(fileName, text, null);
					allFiles.add(fileData);
				} catch (IOException e1) {
					DebugLogDialog.appendException(e1);
				}						
			}
		}
		return allFiles;
	}
}
