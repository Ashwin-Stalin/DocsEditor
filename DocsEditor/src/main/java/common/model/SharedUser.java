package common.model;

public class SharedUser {
	int userId;
	String userName;
	String permission;
	
	public SharedUser(int userId, String userName, String permission) {
		this.userId = userId;
		this.userName = userName;
		this.permission = permission;
	}
}
