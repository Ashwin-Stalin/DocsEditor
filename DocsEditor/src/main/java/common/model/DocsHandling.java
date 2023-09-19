package common.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import common.diff_match_patch;
import common.db.Database;

public class DocsHandling {
	private Database db = new Database();
	
	public List<Doc> getDocsForUser(int userid, HttpServletResponse response){
		List<Doc> docs = new ArrayList<>();
		docs.addAll(getOwnedDocs(userid, response));
		docs.addAll(getSharedDocs(userid, response));
		return docs;
	}
	
	public Doc getDocForUser(int userid, String docid, HttpServletResponse response) {
		Doc doc = getOwnedDocs(userid, docid, response);
		if(doc == null)
			doc = getSharedDocs(userid, docid, response);
		return doc;
	}
	
	public Versions getDocVersions(String docid, HttpServletResponse response) {
		List<Integer> versions = getVersions(docid, response);
		int currentVersion = getCurrentVersion(docid, response);
		return new Versions(versions, currentVersion);
	}
	
	public String getVersionContent(String docid, int versionToView, HttpServletResponse response) {
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
	
	public List<SharedUser> getSharedUsers(int userid, String docid, HttpServletResponse response) {
		List<SharedUser> sharedDetails = new ArrayList<>();
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=? and document.docid=?");) {
			int documentId = Integer.parseInt(docid);
			ps.setInt(1, userid);
			ps.setInt(2, documentId);
			try(ResultSet rs = ps.executeQuery();){
		        while(rs.next()) {
					int receivedUserId = rs.getInt("receiverid");
					String receivedUserName = rs.getString("receivedUserName");
					String permission = rs.getString("permission");
					sharedDetails.add(new SharedUser(receivedUserId, receivedUserName, permission));
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return sharedDetails;
	}
	
	public SharedUser getSharedUser(int userid, String docid, int receiverid, HttpServletResponse response) {
		SharedUser sharedDetail = null ;
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=? and document.docid=? and receiverid=?");) {
			int documentId = Integer.parseInt(docid);
			ps.setInt(1, userid);
			ps.setInt(2, documentId);
			ps.setInt(3, receiverid);
			try(ResultSet rs = ps.executeQuery();){
				while (rs.next()) {
					String receivedUserName = rs.getString("receivedUserName");
					String permission = rs.getString("permission");
					sharedDetail = new SharedUser(receiverid, receivedUserName, permission);
			    }
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return sharedDetail;
	}
	
	public boolean createDoc(int userid, String docName, String fileContent, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("insert into document(name, ownerid) values(?,?) returning docid");) {
			preparedStatement.setString(1, docName);
			preparedStatement.setInt(2, userid);
			try(ResultSet rs = preparedStatement.executeQuery();PreparedStatement ps = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");){
				if (rs.next()) {
					int docid = rs.getInt("docid");
					InputStream content = new ByteArrayInputStream(fileContent.getBytes());
					ps.setInt(1, docid);
					ps.setBinaryStream(2, content);
					ps.setInt(3, userid);
					try(ResultSet res = ps.executeQuery();PreparedStatement pS= connection.prepareStatement("update document set currentversion=? where docid=?");){
						if (res.next()) {
							int versionid = res.getInt("versionid");
							pS.setInt(1, versionid);
							pS.setInt(2, docid);
							pS.executeUpdate();
							return true;
						}
					}
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	public boolean shareWithUser(String toSendUserName, String docid, String permission, HttpServletResponse response) {
		int toSendUserId = getUserId(toSendUserName, response);
		if(toSendUserId == 0) {
			Respond.sendData(response, 400, "Invalid Username", true);
			return false;
		}
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select * from docshared where docid=? and receiverid=?");) {
			int documentId = Integer.parseInt(docid);
			ps.setInt(1, documentId);
			ps.setInt(2, toSendUserId);
			try(ResultSet resultSet = ps.executeQuery();PreparedStatement pS = connection.prepareStatement("insert into docshared(docid, receiverid, permission) values(?,?,?)");){
				if(resultSet.next())
					Respond.sendData(response, 200, "You Already Shared This Document With This User.", false);
				else {
					
					pS.setInt(1, documentId);
					pS.setInt(2, toSendUserId);
					pS.setString(3, permission);
					if(pS.executeUpdate() == 1)
						return true;
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean updateContentOfDoc(String docid, int userid, String newContent, HttpServletResponse response) {
		synchronized (docid.intern()) {	
			try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select content from versions where versionid=?");) {
				int cversionid = getCurrentVersion(docid, response);
				ps.setInt(1, cversionid);
				try(ResultSet res = ps.executeQuery();){
					if(res.next()) {
						InputStream c = res.getBinaryStream("content");
						String oldContent = readInputStreamToString(c, response);
		
						if(newContent.contentEquals(oldContent))
							Respond.sendData(response, 200, "Nothying to change!", false);
						else
							return makeNewVersion(response, docid, userid, cversionid, newContent, oldContent);
					}
				}
			
			} catch (SQLException e) {
				Respond.sendData(response, 500, "Internal Server Error", true);
			}
			return false;
		}
	}
	
	public boolean updatePermission(String docid, int toShareId, String permission, HttpServletResponse response) {
		synchronized (docid.intern()){
			try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select userid from users where userid=?");) {
				int documentId = Integer.parseInt(docid);
				ps.setInt(1, toShareId);
				try(ResultSet rs = ps.executeQuery(); PreparedStatement pS = connection.prepareStatement("update docshared set permission=? where docid=? and receiverid=?");){
					if(rs.next()) {
						
						pS.setString(1, permission);
						pS.setInt(2, documentId);
						pS.setInt(3, toShareId);
						if(pS.executeUpdate() == 1)
							return true;
					}else
						Respond.sendData(response, 400, "Invalid User ID!", true);
				}
			} catch (SQLException e) {
				Respond.sendData(response, 500, "Internal Server Error", true);
			} catch (NumberFormatException e) {
				Respond.sendData(response, 400, "Invalid Request Check Value", true);
			}
			return false;
		}
	}
	
	public boolean updateVersion(String docid, int versionToView, HttpServletResponse response) {
		synchronized (docid.intern()) {
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
	}
	
	public boolean deleteDocById(String docid, int userid, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("delete from document where docid=? and ownerid=?");) {
			preparedStatement.setInt(1, Integer.parseInt(docid));
			preparedStatement.setInt(2, userid);
			if(preparedStatement.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean deleteSharedDocs(String docid, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=?");) {
			ps.setInt(1, Integer.parseInt(docid));
			if(ps.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean deleteSharedDocForUser(int userIdToDelete, String docid, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=? and receiverid=?");) {
			ps.setInt(1, Integer.parseInt(docid));
			ps.setInt(2, userIdToDelete);
			if(ps.executeUpdate() == 1)
				return true;
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
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
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content from document join versions on document.currentversion=versions.versionid where document.ownerid=?");) {
			preparedStatement.setInt(1, userid);
			try(ResultSet rs = preparedStatement.executeQuery();){
				while(rs.next()) {
					int docid = rs.getInt("docid");
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					Doc doc = new Doc(docid, docName, content);
					List<Integer> versions = getVersions(String.valueOf(docid), response);
					if(versions.size() != 0) {
						int currentVersion = getCurrentVersion(String.valueOf(docid), response);
						doc.setVersions(new Versions(versions, currentVersion));
					}
					List<SharedUser> sharedWith = getSharedUsers(userid, String.valueOf(docid), response);
					if(sharedWith.size() != 0)
						doc.setSharedWith(sharedWith);
					docs.add(doc);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return docs;
	}
	
	private List<Doc> getSharedDocs(int userid, HttpServletResponse response) {
		List<Doc> docs = new ArrayList<>();
		try(Connection connection = db.getConnection();PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=?");) {
			preparedStatement.setInt(1, userid);
			try(ResultSet rs = preparedStatement.executeQuery();){
				while(rs.next()) {
					int docid = rs.getInt("docid");
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					String permission = rs.getString("permission");
					Doc document = new Doc(docid, docName, content); 
					document.setPermission(permission);
					Doc doc = new Doc(docid, docName, content);
					List<Integer> versions = getVersions(String.valueOf(docid), response);
					if(versions.size() != 0) {
						int currentVersion = getCurrentVersion(String.valueOf(docid), response);
						doc.setVersions(new Versions(versions, currentVersion));
					}
					doc.setSharedWithMe(true);
					docs.add(doc);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return docs;
	}
	
	private Doc getOwnedDocs(int userid, String docid, HttpServletResponse response) {
		Doc doc = null ;
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select name, content from document join versions on document.currentversion=versions.versionid where document.ownerid=? and document.docid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, documentId);
			try(ResultSet rs = preparedStatement.executeQuery();){
				while(rs.next()) {
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					doc = new Doc(documentId, docName, content);
					List<Integer> versions = getVersions(docid, response);
					if(versions.size() != 0) {
						int currentVersion = getCurrentVersion(docid, response);
						doc.setVersions(new Versions(versions, currentVersion));
					}
					List<SharedUser> sharedWith = getSharedUsers(userid, docid, response);
					if(sharedWith.size() != 0)
						doc.setSharedWith(sharedWith);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return doc;
	}
	
	private Doc getSharedDocs(int userid, String docid, HttpServletResponse response) {
		Doc doc = null;
		try(Connection connection = db.getConnection();PreparedStatement preparedStatement = connection.prepareStatement("select name, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=? and document.docid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, documentId);
			try(ResultSet rs = preparedStatement.executeQuery();){
				while(rs.next()) {
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					String permission = rs.getString("permission");
					Doc document = new Doc(documentId, docName, content); 
					document.setPermission(permission);
					doc = new Doc(documentId, docName, content);
					List<Integer> versions = getVersions(docid, response);
					if(versions.size() != 0) {
						int currentVersion = getCurrentVersion(docid, response);
						doc.setVersions(new Versions(versions, currentVersion));
					}
					doc.setSharedWithMe(true);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return doc;
	}
	
	private List<Integer> getVersions(String docid, HttpServletResponse response) { 
		List<Integer> versions = new ArrayList<>(); 
		try(Connection connection = db.getConnection(); PreparedStatement ps = connection.prepareStatement("select versionid from versions where docid=? order by versionid asc");) {
			int documentId = Integer.parseInt(docid);
			ps.setInt(1, documentId);
			try(ResultSet res = ps.executeQuery();){
				while(res.next()) {
					int version = res.getInt("versionid");
					versions.add(version);
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return versions;
	}
	
	private int getCurrentVersion(String docid, HttpServletResponse response) { 
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select currentversion from document where docid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, documentId);
			try(ResultSet rs = preparedStatement.executeQuery();){
				if(rs.next())
					return rs.getInt("currentversion");
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return 0;
	}
	
	private String getContentToView(HttpServletResponse response, int currentVersion, int versionToView, String docid) {
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
		return getContent(response, sourceVersionIds, destinationVersionIds);
	}
	
	private List<Integer> getVersionIdsToRoot(int startVersionId, String docid, HttpServletResponse response){
		// from startVersionid till root of that versionid
		List<Integer> versionIds = new ArrayList<>();
		try(Connection connection = db.getConnection();PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");) {
			int documentId = Integer.parseInt(docid);
			int versionToAdd = startVersionId;
			versionIds.add(versionToAdd);
			while(true) {
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, documentId);
				try(ResultSet rs = prepareStatement.executeQuery();){
					if(rs.next()) {
						int version = rs.getInt("fromversion");
						versionIds.add(version);
						versionToAdd = version;
					}else
						break;
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return versionIds;
	}
	
	private String getContent(HttpServletResponse response, List<Integer> sourceVersionIds, List<Integer> destinationVersionIds) {
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
		try(Connection connection = db.getConnection(); PreparedStatement prepareStatement = connection.prepareStatement("select content from versions where versionid=?");) {
			for(Integer versionid : versions) {
				prepareStatement.setInt(1, versionid);
				try(ResultSet resultSet = prepareStatement.executeQuery();) {
					while(resultSet.next()) {
						InputStream c = resultSet.getBinaryStream("content");
						String content = readInputStreamToString(c,response);
						contents.add(content);
					}
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return contents;
	}
	
	private boolean makeNewVersion(HttpServletResponse response, String docid, int userid, int versionid, String newContent, String oldContent) {
		try(Connection connection = db.getConnection()) {
			try( PreparedStatement preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");) {
				connection.setAutoCommit(false);
				int documentId = Integer.parseInt(docid);
				InputStream newfileContent = new ByteArrayInputStream(newContent.getBytes());
				preparedStatement.setInt(1, documentId);
				preparedStatement.setBinaryStream(2, newfileContent, newfileContent.available());
				preparedStatement.setInt(3, userid);
				try(ResultSet rs = preparedStatement.executeQuery();PreparedStatement ps = connection.prepareStatement("update document set currentversion=? where docid=?; insert into versionmapping(docid, fromversion, toversion) values(?,?,?);");){
					if (rs.next()) {
						int newversionid = rs.getInt("versionid");
						ps.setInt(1, newversionid);
						ps.setInt(2, documentId);
						ps.setInt(3, documentId);
						ps.setInt(4, versionid);
						ps.setInt(5, newversionid);
						ps.executeUpdate();
					}else {
						connection.rollback();
						return false;
					}
				}
				try(PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?");){
					String patch = getDiffText(newContent, oldContent);
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					
					pS.setBinaryStream(1, patchContent);
					pS.setInt(2, versionid);
					if(pS.executeUpdate() == 1) {
						connection.commit();
						return true;
					}else
						connection.rollback();
				}
			} catch (SQLException e) {
				connection.rollback();
				Respond.sendData(response, 500, "Internal Server Error", true);
			} catch (IOException e) {
				connection.rollback();
				Respond.sendData(response, 500, "Internal Server Error", true);
			} catch (NumberFormatException e) {
				connection.rollback();
				Respond.sendData(response, 400, "Invalid Request Check Value", true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
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
	
	private boolean updateCurrentVersion(String docid, int currentVersion, int versionToChange, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from versions where docid=? and versionid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, documentId);
			preparedStatement.setInt(2, versionToChange);
			try(ResultSet res = preparedStatement.executeQuery();){
				if(res.next()) {
					if(changeContent(response, currentVersion, versionToChange, docid))
						return true;
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean checkOwner(String docid, int userid, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from document where ownerid=? and docid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, documentId);
			try(ResultSet rs = preparedStatement.executeQuery();){
				if(rs.next())
					return true;
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean canViewDocument(String docid, int userid, HttpServletResponse response) {
		if((checkOwner(docid, userid, response)) | (checkReceived(docid, userid, response)))
			return true;
		return false;
	}
	
	public boolean checkReceived(String docid, int userid, HttpServletResponse response) {
		try(Connection connection = db.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from docshared where receiverid=? and docid=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, documentId);
			try(ResultSet rs = preparedStatement.executeQuery();){
				if(rs.next())
					return true;
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	public boolean checkReceivedWithPermissionToEdit(String docid, int userid, HttpServletResponse response) {
		try(Connection connection = db.getConnection();PreparedStatement preparedStatement = connection.prepareStatement("select * from docshared where receiverid=? and docid=? and permission=?");) {
			int documentId = Integer.parseInt(docid);
			preparedStatement.setInt(1, userid);
			preparedStatement.setInt(2, documentId);
			preparedStatement.setString(3, "View_Edit");
			try(ResultSet rs = preparedStatement.executeQuery();){
				if(rs.next())
					return true;
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		}
		return false;
	}
	
	private boolean changeContent(HttpServletResponse response, int currentVersion, int versionToView, String docid) {
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
	
	private boolean updateProcess(String docid, List<Integer> sourceVersionIds, List<Integer> destinationVersionIds, int currentVersion,HttpServletResponse response) {
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
		} catch (NumberFormatException e) {
			Respond.sendData(response, 400, "Invalid Request Check Value", true);
		} catch (Exception e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	private int updateContent(HttpServletResponse response, int currentVersion, int versionToChange, String docid) {
		try(Connection connection = db.getConnection()) {	
			connection.setAutoCommit(false);
			try(PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as contentToChange from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");) {
				int documentId = Integer.parseInt(docid);
				preparedStatement.setInt(1, currentVersion);
				preparedStatement.setInt(2, versionToChange);
				preparedStatement.setInt(3, documentId);
				try(ResultSet rs = preparedStatement.executeQuery();PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");){
					if(rs.next()) {
						InputStream c = rs.getBinaryStream("currentContent");
						InputStream p = rs.getBinaryStream("contentToChange");
						String currentContent = readInputStreamToString(c, response);
						String previousContent = readInputStreamToString(p, response);
						String patchedText = getPatchedText(previousContent, currentContent);
						InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());
						String patch = getDiffText(patchedText, currentContent);
						InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
						pS.setBinaryStream(1, retrievedText);
						pS.setInt(2, versionToChange);
						pS.setBinaryStream(3, patchContent);
						pS.setInt(4, currentVersion);
						pS.setInt(5, versionToChange);
						pS.setInt(6, documentId);
						pS.executeUpdate();
						connection.commit();
						return versionToChange;
					}
					connection.rollback();
				}
			} catch (SQLException e) {
				connection.rollback();
				Respond.sendData(response, 500, "Internal Server Error", true);
			} catch (NumberFormatException e) {
				connection.rollback();
				Respond.sendData(response, 400, "Invalid Request Check Value", true);
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return -1;
	}
	
	private int getUserId(String toSendUname, HttpServletResponse response) {
		try(Connection connection = db.getConnection();PreparedStatement ps = connection.prepareStatement("select userid from users where username=?");) {
			ps.setString(1, toSendUname);
			try(ResultSet rs = ps.executeQuery()){
				if(rs.next()) {
					return rs.getInt("userid");
				}
			}
		} catch (SQLException e) {
			Respond.sendData(response, 500, "Internal Server Error", true);
		}
		return 0;
	}
}
