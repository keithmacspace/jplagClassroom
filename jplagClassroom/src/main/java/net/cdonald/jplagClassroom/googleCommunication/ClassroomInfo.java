package net.cdonald.jplagClassroom.googleCommunication;

import java.util.Date;

public class ClassroomInfo implements Comparable<ClassroomInfo> {
	public ClassroomInfo(String name, String id, Date date) {
		super();
		this.name = name;
		this.id = id;
		this.date = date;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public Date getDate() {
		return date;
	}
	
	private String name;
	private String id;
	private Date date;
	@Override
	public int compareTo(ClassroomInfo o) {
		if (name == null && o.name == null) {
			return 0;
		}
		if (name == null) {
			return -1;
		}
		if (o.name == null) {
			return 1;
		}
		return this.name.compareTo(o.name);
	}


}
