package common.model;

public class DocSharedDetail {
	int docid;
	String docname;
	int receivedUserId;
	String receivedUserName;
	String permission;
	
	public DocSharedDetail(int docid, String docname, int receiverUserId, String receiverUserName, String permission) {
		this.docid = docid;
		this.docname = docname;
		this.receivedUserId = receiverUserId;
		this.receivedUserName = receiverUserName;
		this.permission = permission;
	}
}
