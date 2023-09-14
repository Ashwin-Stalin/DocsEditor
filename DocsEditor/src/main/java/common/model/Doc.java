package common.model;

import java.util.List;

public class Doc {
	int docid;
	String name;
	String content;
	String permission;
	Versions versions;
	List<SharedUser> sharedUsers;
	
	public Doc(int docid, String name, String content){
		this.docid = docid;
		this.name = name;
		this.content = content;
	}
	
	public void setPermission(String permission) {
		this.permission = permission;
	}
	
	public void setVersions(Versions versions) {
		this.versions = versions;
	}
	
	public void setSharedWith(List<SharedUser> sharedUsers) {
		this.sharedUsers = sharedUsers;
	}
}
