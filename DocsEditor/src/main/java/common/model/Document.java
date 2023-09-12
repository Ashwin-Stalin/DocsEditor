package common.model;

public class Document {
	int docid;
	String name;
	String content;
	String permission;
	
	public Document(int docid, String name, String content){
		this.docid = docid;
		this.name = name;
		this.content = content;
	}
	
	public void setPermission(String permission) {
		this.permission = permission;
	}
}
