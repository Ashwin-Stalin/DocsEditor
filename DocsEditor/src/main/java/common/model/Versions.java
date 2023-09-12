package common.model;

import java.util.ArrayList;
import java.util.List;

public class Versions {
	List<Integer> versions = new ArrayList<>(); 
	int currentVersion;
	
	public Versions(List<Integer> versions, int currentVersion) {
		this.versions = versions;
		this.currentVersion = currentVersion;
	}
}
