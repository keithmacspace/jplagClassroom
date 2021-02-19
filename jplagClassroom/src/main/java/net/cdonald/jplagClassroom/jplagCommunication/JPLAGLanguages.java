package net.cdonald.jplagClassroom.jplagCommunication;

public enum JPLAGLanguages {
	java("java19"),
	cpp("c/c++"),
	c_sharp("c#-1.2"),
	python("python3")
	;
	private String jplagName;

	JPLAGLanguages(String jplagName) {
		this.jplagName = jplagName;
	}
	
	public String getJPLAGName() {
		return jplagName;
	}


}
