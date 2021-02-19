package net.cdonald.jplagClassroom.utils;

import java.util.ArrayList;
import java.util.List;

public class ListenerTracker<T> {
	protected List<T> listeners = new ArrayList<T>();
	
	public List<T> getListeners() {
		return listeners;
	}
	
	public void addListener(T listener) {
		listeners.add(listener);
	}
	
	public void removeListener(T listener) {
		listeners.remove(listener);
	}
 
}
