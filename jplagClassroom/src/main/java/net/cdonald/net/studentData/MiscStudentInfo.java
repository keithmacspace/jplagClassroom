package net.cdonald.net.studentData;

import java.util.ArrayList;
import java.util.List;

import net.cdonald.sourceCode.FileData;

/**
 * 
 * This is the information other than scores, such as first name, last name
 * submit date of their code & whether the code compiles.
 * Unlike scores, where we have to merge the data with the sheet data,
 * we just write this data out.
 *
 */
public class MiscStudentInfo implements Comparable<MiscStudentInfo>{
	private String studentID = null;
	private String lastName = null;
	private String firstName = null;	
	private List<FileData> sourceCode;
	
	public MiscStudentInfo() {
		
	}
	
	public MiscStudentInfo(String studentID, String firstName, String lastName) {
		super();
		
		this.studentID = studentID;
		this.lastName = lastName;
		this.firstName = firstName;
		sourceCode = new ArrayList<FileData>();
	}
	@Override
	public int compareTo(MiscStudentInfo other) {
		if (other.lastName == null || other.firstName == null) {
			if (firstName == null || lastName == null) {
				if (studentID != null && other.studentID != null) {
					return studentID.compareToIgnoreCase(other.studentID);
				}
				return 0;
			}
			return 1;
		}
		if (firstName == null || lastName == null) {
			return -1;
		}
		int lastNameCompare = lastName.compareToIgnoreCase(other.lastName);
		if (lastNameCompare != 0) {
			return lastNameCompare;
		}
		return firstName.compareToIgnoreCase(other.firstName);
	}

	
	@Override
	public String toString() {
		String returnValue =  "MiscStudentInfo [studentID=" + studentID + ", lastName=" + lastName + ", firstName=" + firstName + "]";

		return returnValue;
		
	}

	public String getStudentID() {
		return studentID;
	}

	public void setStudentID(String studentID) {
		this.studentID = studentID;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	


	public List<FileData> getSourceCode() {
		return sourceCode;
	}
	
	public void clearSourceCode() {
		sourceCode.clear();
		
		
	}
	public void addSource(FileData file) {
		boolean found = false;
		for (int i = 0; found == false && i < sourceCode.size(); i++) {
		    FileData fileData = sourceCode.get(i);
			if (fileData.getName().equalsIgnoreCase(file.getName())) {
				fileData.setFileContents(file.getFileContents());
				found = true;
			}
		}
		if (found == false) {
			sourceCode.add(file);
		}
	}
	
	

	
	public String getFullName() {		
		return getFirstName() + " " + getLastName();
	}



	public Boolean isSourceDownloaded() {
		return !sourceCode.isEmpty();
	}
	

	
	




	

}
