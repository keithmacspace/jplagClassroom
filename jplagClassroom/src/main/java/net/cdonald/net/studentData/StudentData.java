package net.cdonald.net.studentData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.sourceCode.FileData;

/**
 * 
 * This is the class that holds all of the information about the student except
 * their source code.
 *
 */
public class StudentData {

	private Map<String, MiscStudentInfo> miscStudentInfoMap;
	private List<MiscStudentInfo> miscStudentInfoList;


	public StudentData() {
		miscStudentInfoMap = new HashMap<String, MiscStudentInfo>();
		miscStudentInfoList = new ArrayList<MiscStudentInfo>();
	}

	public StudentData(StudentData other) {
		this();
		for (MiscStudentInfo student : other.miscStudentInfoList) {
			addStudent(student.getStudentID(), student.getFirstName(), student.getLastName());
		}
	}

	public void clearInfo() {

		miscStudentInfoMap.clear();
		miscStudentInfoList.clear();
	}


	public void addStudent(String studentID, String firstName, String lastName) {
		if (miscStudentInfoMap.containsKey(studentID) == false) {
			MiscStudentInfo info = new MiscStudentInfo(studentID, firstName, lastName);
			miscStudentInfoMap.put(studentID, info);
			miscStudentInfoList.add(info);
		} else {
			MiscStudentInfo info = miscStudentInfoMap.get(studentID);
			info.setFirstName(firstName);
			info.setLastName(lastName);
			info.setStudentID(studentID);

		}
		Collections.sort(miscStudentInfoList);
	}


	public List<MiscStudentInfo> getMiscStudentInfoList() {
		return miscStudentInfoList;
	}

	public void addFile(String studentID, FileData file) {
		MiscStudentInfo student = miscStudentInfoMap.get(studentID);
		if (student != null) {
			student.addSource(file);
		}
	}

	public MiscStudentInfo getMiscInfo(String studentID) {
		return miscStudentInfoMap.get(studentID);
	}


	@Override
	public String toString() {
		return "StudentData \n" + "MiscInfo " + miscStudentInfoList;
	}

	public boolean hasSource() {

		for (MiscStudentInfo student : miscStudentInfoList) {
			if (student.isSourceDownloaded()) {
				return true;
			}
		}
		return false;
	}

}
