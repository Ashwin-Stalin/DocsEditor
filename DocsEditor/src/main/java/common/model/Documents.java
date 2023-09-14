package common.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import common.diff_match_patch;
import common.db.Database;

public class Documents {
	private final Connection connection = Database.getConnection();
	
	public List<Doc> getDocsForUser(int userid, HttpServletResponse response){
		List<Doc> docs = new ArrayList<>();
		docs.addAll(getOwnedDocs(userid, response));
		docs.addAll(getSharedDocs(userid, response));
		return docs;
	}
	
	public Doc getDocForUser(int userid, int docid, HttpServletResponse response) {
		Doc doc = getOwnedDocs(userid, docid, response);
		if(doc == null)
			doc = getSharedDocs(userid, docid, response);
		return doc;
	}
	
	public Versions getDocVersions(int docid, HttpServletResponse response) {
		List<Integer> versions = getVersions(docid, response);
		int currentVersion = getCurrentVersion(docid, response);
		return new Versions(versions, currentVersion);
	}
	
	public String getVersionContent(int docid, int versionToView, HttpServletResponse response) {
		int currentVersionid = getCurrentVersion(docid, response);
		boolean valid = false;
		List<Integer> versions = getVersions(docid, response);
		for(Integer version : versions) {
			if(version == versionToView)
				valid = true;
		}
		if(valid) {
			String content = getContentToView(response, currentVersionid, versionToView, docid);
			return content;
		}
		return null;
	}
	
	public List<SharedUser> getSharedUsers(int userid, int docid, HttpServletResponse response) {
		List<SharedUser> sharedDetails = new ArrayList<>();
		try {
			PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=? and document.docid=?");
			ps.setInt(1, userid);
			ps.setInt(2, docid);
			ResultSet rs = ps.executeQuery();
	        while(rs.next()) {
				int receivedUserId = rs.getInt("receiverid");
				String receivedUserName = rs.getString("receivedUserName");
				String permission = rs.getString("permission");
				sharedDetails.add(new SharedUser(receivedUserId, receivedUserName, permission));
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return sharedDetails;
	}
	
	public SharedUser getSharedUser(int userid, int docid, int receiverid, HttpServletResponse response) {
		SharedUser sharedDetail = null ;
		try {
			PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=? and document.docid=? and receiverid=?");
			ps.setInt(1, userid);
			ps.setInt(2, docid);
			ps.setInt(3, receiverid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String receivedUserName = rs.getString("receivedUserName");
				String permission = rs.getString("permission");
				sharedDetail = new SharedUser(receiverid, receivedUserName, permission);
		    }
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return sharedDetail;
	}
	
	public boolean createDoc(int userid, String docName, String fileContent, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("insert into document(name, ownerid) values(?,?) returning docid");
			preparedStatement.setString(1, docName);
			preparedStatement.setInt(2, userid);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				int docid = rs.getInt("docid");
				InputStream content = new ByteArrayInputStream(fileContent.getBytes());
	
				preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");
				preparedStatement.setInt(1, docid);
				preparedStatement.setBinaryStream(2, content);
				preparedStatement.setInt(3, userid);
				ResultSet res = preparedStatement.executeQuery();
				if (res.next()) {
					int versionid = res.getInt("versionid");
	
					preparedStatement = connection.prepareStatement("update document set currentversion=? where docid=?");
					preparedStatement.setInt(1, versionid);
					preparedStatement.setInt(2, docid);
					preparedStatement.executeUpdate();
					return true;
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean shareWithUser(String toSendUserName, int docid, String permission, HttpServletResponse response) {
		int toSendUserId = getUserId(toSendUserName, response);
		if(toSendUserId == 0) {
			Respond.sendData(response, 400, "Invalid Username", true);
			return false;
		}
		try {
			PreparedStatement ps = connection.prepareStatement("select * from docshared where docid=? and receiverid=?");
			ps.setInt(1, docid);
			ps.setInt(2, toSendUserId);
			ResultSet resultSet = ps.executeQuery();
			if(resultSet.next())
				Respond.sendData(response, 200, "You Already Shared This Document With This User.", false);
			else {
				ps = connection.prepareStatement("insert into docshared(docid, receiverid, permission) values(?,?,?)");
				ps.setInt(1, docid);
				ps.setInt(2, toSendUserId);
				ps.setString(3, permission);
				if(ps.executeUpdate() == 1)
					return true;
			}
			
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean updateContentOfDoc(int docid, int userid, String newContent, HttpServletResponse response) {
		try {
			int cversionid = getCurrentVersion(docid, response);
			PreparedStatement ps = connection.prepareStatement("select content from versions where versionid=?");
			ps.setInt(1, cversionid);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				InputStream c = res.getBinaryStream("content");
				String oldContent = readInputStreamToString(c, response);

				if(newContent.contentEquals(oldContent))
					Respond.sendData(response, 200, "Nothying to change!", false);
				else
					return makeNewVersion(response, docid, userid, cversionid, newContent, oldContent);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean updatePermission(int docid, int toShareId, String permission, HttpServletResponse response) {
		try {
			PreparedStatement ps = connection.prepareStatement("select userid from users where userid=?");
			ps.setInt(1, toShareId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				ps = connection.prepareStatement("update docshared set permission=? where docid=? and receiverid=?");
				ps.setString(1, permission);
				ps.setInt(2, docid);
				ps.setInt(3, toShareId);
				if(ps.executeUpdate() == 1)
					return true;
			}else
				Respond.sendData(response, 400, "Invalid User ID!", true);
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean updateVersion(int docid, int versionToView, HttpServletResponse response) {
		boolean valid = false;
		List<Integer> versions = getVersions(docid, response);
		for(Integer version : versions) {
			if(version == versionToView)
				valid = true;
		}
		if(valid) {
			int currentVersionid = getCurrentVersion(docid, response);
			if(updateCurrentVersion(docid, currentVersionid, versionToView, response))
				return true;
		} else
			Respond.sendData(response, 404, "Requested versionid not found",true);
		return false;
	}
	
	public boolean deleteDocById(int docid, int userid, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("delete from document where docid=? and ownerid=?");
			preparedStatement.setInt(1, docid);
			preparedStatement.setInt(2, userid);
			if(preparedStatement.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean deleteSharedDocs(int docid, HttpServletResponse response) {
		try {
			PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=?");
			ps.setInt(1, docid);
			if(ps.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean deleteSharedDocForUser(int userIdToDelete, int docid, HttpServletResponse response) {
		try {
			PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=? and receiverid=?");
			ps.setInt(1, docid);
			ps.setInt(2, userIdToDelete);
			if(ps.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public String readInputStreamToString(InputStream inputStream, HttpServletResponse response) {
		StringBuilder sb = new StringBuilder();
		try {
			for (int ch; (ch = inputStream.read()) != -1;)
				sb.append((char) ch);
		} catch(IOException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return sb.toString();
	}
	
	private List<Doc> getOwnedDocs(int userid, HttpServletResponse response) {
		List<Doc> docs = new ArrayList<>();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content from document join versions on document.currentversion=versions.versionid where document.ownerid=?");
			preparedStatement.setInt(1, userid);
			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()) {
				int docid = rs.getInt("docid");
				String docName = rs.getString("name");
				InputStream c = rs.getBinaryStream("content");
				String content = readInputStreamToString(c, response);
				Doc doc = new Doc(docid, docName, content);
				List<Integer> versions = getVersions(docid, response);
				if(versions.size() != 0) {
					int currentVersion = getCurrentVersion(docid, response);
					doc.setVersions(new Versions(versions, currentVersion));
				}
				List<SharedUser> sharedWith = getSharedUsers(userid, docid, response);
				if(sharedWith.size() != 0)
					doc.setSharedWith(sharedWith);
				docs.add(doc);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return docs;
	}
	
	private List<Doc> getSharedDocs(int userid, HttpServletResponse response) {
		List<Doc> docs = new ArrayList<>();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=?");
			preparedStatement.setInt(1, userid);
			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()) {
				int docid = rs.getInt("docid");
				String docName = rs.getString("name");
				InputStream c = rs.getBinaryStream("content");
				String content = readInputStreamToString(c, response);
				String permission = rs.getString("permission");
				Doc document = new Doc(docid, docName, content); 
				document.setPermission(permission);
				Doc doc = new Doc(docid, docName, content);
				List<Integer> versions = getVersions(docid, response);
				if(versions.size() != 0) {
					int currentVersion = getCurrentVersion(docid, response);
					doc.setVersions(new Versions(versions, currentVersion));
				}
				docs.add(doc);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return docs;
	}
	
	private Doc getOwnedDocs(int userid, int docid, HttpServletResponse response) {
		Doc doc = null ;
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select name, content from document join versions on document.currentversion=versions.versionid where document.ownerid=? and document.docid=?");
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, docid);
			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()) {
				String docName = rs.getString("name");
				InputStream c = rs.getBinaryStream("content");
				String content = readInputStreamToString(c, response);
				doc = new Doc(docid, docName, content);
				List<Integer> versions = getVersions(docid, response);
				if(versions.size() != 0) {
					int currentVersion = getCurrentVersion(docid, response);
					doc.setVersions(new Versions(versions, currentVersion));
				}
				List<SharedUser> sharedWith = getSharedUsers(userid, docid, response);
				if(sharedWith.size() != 0)
					doc.setSharedWith(sharedWith);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return doc;
	}
	
	private Doc getSharedDocs(int userid, int docid, HttpServletResponse response) {
		Doc doc = null;
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select name, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=? and document.docid=?");
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, docid);
			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()) {
				String docName = rs.getString("name");
				InputStream c = rs.getBinaryStream("content");
				String content = readInputStreamToString(c, response);
				String permission = rs.getString("permission");
				Doc document = new Doc(docid, docName, content); 
				document.setPermission(permission);
				doc = new Doc(docid, docName, content);
				List<Integer> versions = getVersions(docid, response);
				if(versions.size() != 0) {
					int currentVersion = getCurrentVersion(docid, response);
					doc.setVersions(new Versions(versions, currentVersion));
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return doc;
	}
	
	private List<Integer> getVersions(int docid, HttpServletResponse response) { 
		List<Integer> versions = new ArrayList<>(); 
		try {
			PreparedStatement ps = connection.prepareStatement("select versionid from versions where docid=? order by versionid asc");
			ps.setInt(1, docid);
			ResultSet res = ps.executeQuery();
			while(res.next()) {
				int version = res.getInt("versionid");
				versions.add(version);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return versions;
	}
	
	private int getCurrentVersion(int docid, HttpServletResponse response) { 
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select currentversion from document where docid=?");
			preparedStatement.setInt(1, docid);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next())
				return rs.getInt("currentversion");
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return 0;
	}
	
	private String getContentToView(HttpServletResponse response, int currentVersion, int versionToView, int docid) {
		List<Integer> sourceVersionIds = getVersionIdsToRoot(currentVersion, docid, response);
		List<Integer> destinationVersionIds = getVersionIdsToRoot(versionToView, docid, response);
		
		// Remove all the common version id from both list's back side and add last commonly removed id to sourceVersionid
		int removedVersionId = 0 ;
		while(true) {
			if(sourceVersionIds.size() == 0 || destinationVersionIds.size() == 0) {
				if(removedVersionId!=0)
					sourceVersionIds.add(removedVersionId);
				break;
			}
			if(sourceVersionIds.get(sourceVersionIds.size() - 1).equals(destinationVersionIds.get(destinationVersionIds.size()-1))) {
				removedVersionId = sourceVersionIds.get(sourceVersionIds.size()-1);
				sourceVersionIds.remove(sourceVersionIds.size()-1);
				destinationVersionIds.remove(destinationVersionIds.size()-1);
			}else {
				if(removedVersionId != 0)
					sourceVersionIds.add(removedVersionId);
				break;
			}
		}
		return getContent(response, sourceVersionIds, destinationVersionIds, docid);
	}
	
	private List<Integer> getVersionIdsToRoot(int startVersionId, int docid, HttpServletResponse response){
		// from startVersionid till root of that versionid
		List<Integer> versionIds = new ArrayList<>();
		try {
			int versionToAdd = startVersionId;
			versionIds.add(versionToAdd);
			while(true) {
				PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, docid);
				ResultSet rs = prepareStatement.executeQuery();
				if(rs.next()) {
					int version = rs.getInt("fromversion");
					versionIds.add(version);
					versionToAdd = version;
				}else
					break;
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return versionIds;
	}
	
	private String getContent(HttpServletResponse response, List<Integer> sourceVersionIds, List<Integer> destinationVersionIds, int docid) {
		try {
			List<String> sourceContents = getContentsOfVersions(sourceVersionIds, response);
			List<String> destinationContents = getContentsOfVersions(destinationVersionIds, response);
			
			String currentContent = null;
			String previousContent = null;
			for(String content : sourceContents) {
				if(currentContent == null) {
					currentContent = content;
					continue;
				}
				previousContent = content;
				String patchedText = getPatchedText(previousContent, currentContent);
				currentContent = patchedText;
			}
			
			String afterContent = null;
			for(int i = destinationContents.size()-1; i >= 0; i--){
				afterContent = destinationContents.get(i);
				String patchedText = getPatchedText(afterContent, currentContent);
				currentContent = patchedText;
			}
			return currentContent;
		} catch (Exception e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return null;
	}
	
	private String getPatchedText(String diffContent, String currentContent) {
		diff_match_patch dmp = new diff_match_patch();
		LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(diffContent);
		Object[] results = dmp.patch_apply(patches, currentContent);
		String patchedText = (String) results[0];
		return patchedText;
	}
	
	private List<String> getContentsOfVersions(List<Integer> versions, HttpServletResponse response){
		List<String> contents = new ArrayList<>();
		try {
			for(Integer versionid : versions) {
				PreparedStatement prepareStatement = connection.prepareStatement("select content from versions where versionid=?");
				prepareStatement.setInt(1, versionid);
				ResultSet resultSet = prepareStatement.executeQuery();
				while(resultSet.next()) {
					InputStream c = resultSet.getBinaryStream("content");
					String content = readInputStreamToString(c,response);
					contents.add(content);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return contents;
	}
	
	private boolean makeNewVersion(HttpServletResponse response, int docid, int userid, int versionid, String newContent, String oldContent) {
		try {
			InputStream newfileContent = new ByteArrayInputStream(newContent.getBytes());
			PreparedStatement preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");
			preparedStatement.setInt(1, docid);
			preparedStatement.setBinaryStream(2, newfileContent, newfileContent.available());
			preparedStatement.setInt(3, userid);
			ResultSet rs = preparedStatement.executeQuery();
			
			if (rs.next()) {
				int newversionid = rs.getInt("versionid");
				
				preparedStatement = connection.prepareStatement("update document set currentversion=? where docid=?; insert into versionmapping(docid, fromversion, toversion) values(?,?,?);");
				preparedStatement.setInt(1, newversionid);
				preparedStatement.setInt(2, docid);
				preparedStatement.setInt(3, docid);
				preparedStatement.setInt(4, versionid);
				preparedStatement.setInt(5, newversionid);
				preparedStatement.executeUpdate();
			}else
				return false;
			String patch = getDiffText(newContent, oldContent);
			InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
			preparedStatement = connection.prepareStatement("update versions set content=? where versionid=?");
			preparedStatement.setBinaryStream(1, patchContent);
			preparedStatement.setInt(2, versionid);
			if(preparedStatement.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (IOException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	private String getDiffText(String content, String currentContent) {
		diff_match_patch dmp = new diff_match_patch();
		LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(content, currentContent, false);
		dmp.diff_cleanupSemantic(diffs);
		String patch = dmp.patch_toText(dmp.patch_make(diffs));
		return patch;
	}
	
	private boolean updateCurrentVersion(int docid, int currentVersion, int versionToChange, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from versions where docid=? and versionid=?");
			preparedStatement.setInt(1, docid);
			preparedStatement.setInt(2, versionToChange);
			ResultSet res = preparedStatement.executeQuery();
			if(res.next()) {
				if(changeContent(response, currentVersion, versionToChange, docid))
					return true;
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean checkOwner(int docid, int userid, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from document where ownerid=? and docid=?");
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, docid);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next())
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean canViewDocument(int docid, int userid, HttpServletResponse response) {
		if((checkOwner(docid, userid, response)) | (checkReceived(docid, userid, response)))
			return true;
		return false;
	}
	
	public boolean checkReceived(int docid, int userid, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from docshared where receiverid=? and docid=?");
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, docid);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next())
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean checkReceivedWithPermissionToEdit(int docid, int userid, HttpServletResponse response) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from docshared where receiverid=? and docid=? and permission=?");
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, docid);
			preparedStatement.setString(3, "View_Edit");
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next())
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	private boolean changeContent(HttpServletResponse response, int currentVersion, int versionToView, int docid) {
		List<Integer> sourceVersionIds = getVersionIdsToRoot(currentVersion, docid, response);
		List<Integer> destinationVersionIds = getVersionIdsToRoot(versionToView, docid, response);
		int removedVersionId = 0 ;
		while(true) {
			if(sourceVersionIds.size() == 0 || destinationVersionIds.size() == 0) {
				if(removedVersionId!=0)
					sourceVersionIds.add(removedVersionId);
				break;
			}
			if(sourceVersionIds.get(sourceVersionIds.size() - 1).equals(destinationVersionIds.get(destinationVersionIds.size()-1))) {
				removedVersionId = sourceVersionIds.get(sourceVersionIds.size()-1);
				sourceVersionIds.remove(sourceVersionIds.size()-1);
				destinationVersionIds.remove(destinationVersionIds.size()-1);
			}else {
				if(removedVersionId != 0)
					sourceVersionIds.add(removedVersionId);
				break;
			}
		}
		return updateProcess(docid, sourceVersionIds, destinationVersionIds, currentVersion, response);
	}
	
	private boolean updateProcess(int docid, List<Integer> sourceVersionIds, List<Integer> destinationVersionIds, int currentVersion,HttpServletResponse response) {
		try {
			int cV = currentVersion;
			for(Integer versionid : sourceVersionIds) {
				if(versionid == cV)
					continue;
				cV = updateContent(response, currentVersion, versionid, docid);
				if(cV == -1)
					return false;
			}
			
			for(int i = destinationVersionIds.size()-1; i >= 0; i--){
				cV = updateContent(response, currentVersion, destinationVersionIds.get(i), docid);
				if(cV == -1)
					return false;
	        }
			return true;
		} catch (Exception e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	private int updateContent(HttpServletResponse response, int currentVersion, int versionToChange, int docid) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as contentToChange from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");
			preparedStatement.setInt(1, currentVersion);
			preparedStatement.setInt(2, versionToChange);
			preparedStatement.setInt(3, docid);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next()) {
				
				InputStream c = rs.getBinaryStream("currentContent");
				InputStream p = rs.getBinaryStream("contentToChange");
				String currentContent = readInputStreamToString(c, response);
				String previousContent = readInputStreamToString(p, response);
			
				String patchedText = getPatchedText(previousContent, currentContent);
				InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());
				
				String patch = getDiffText(patchedText, currentContent);
				InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
				
				PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
				pS.setBinaryStream(1, retrievedText);
				pS.setInt(2, versionToChange);
				pS.setBinaryStream(3, patchContent);
				pS.setInt(4, currentVersion);
				pS.setInt(5, versionToChange);
				pS.setInt(6, docid);
				pS.executeUpdate();
				return versionToChange;
			}
			
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return -1;
	}
	
	private int getUserId(String toSendUname, HttpServletResponse response) {
		try {
			PreparedStatement ps = connection.prepareStatement("select userid from users where username=?");
			ps.setString(1, toSendUname);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getInt("userid");
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return 0;
	}
}
