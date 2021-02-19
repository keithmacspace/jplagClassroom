package net.cdonald.sourceCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public class FileData  {

	private String name = "";
	private Date dateSubmitted = null;
	private String fileContents = null;
	public FileData() {
		super();
	}

	public FileData(String name, String fileContents, Date creationTime) {
		this.name = name;
		dateSubmitted = creationTime;
		setFileContents(fileContents);
	}


	public FileData(FileData other) {		
		setFileContents(other.getFileContents());
		dateSubmitted = other.dateSubmitted;
		name = other.name;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getFileContents() {
		return fileContents;
	}
	
	public Date getDateSubmitted() {
		return dateSubmitted;
	}



	public void setFileContents(String fileContents) {
		this.fileContents = fileContents;
	}


}
