package common.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import common.diff_match_patch;
import common.db.Database;
import common.diff_match_patch.Diff;
import common.diff_match_patch.Patch;
import common.model.Response;
import common.model.Versions;

public class VersionControl extends HttpServlet {
	private final Connection connection = Database.getConnection();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		try {
			int docid = Integer.parseInt(req.getParameter("docid"));
			if(checkDocExist(docid, resp)) {
				PreparedStatement ps = connection.prepareStatement("select ownerid, receiverid, permission, currentversion from document join docshared on document.docid=docshared.docid where (document.ownerid=? and document.docid=?) or (docshared.receiverid=? and document.docid=?)");
				ps.setInt(1, userid);
				ps.setInt(2, docid);
				ps.setInt(3, userid);
				ps.setInt(4, docid);
				ResultSet rs = ps.executeQuery();
				if(rs.next()) {
					int ownerid = rs.getInt("ownerid");
					int receiverid = rs.getInt("receiverid");
					int currentVersion = rs.getInt("currentversion");
					String permission = rs.getString("permission");
					if((userid == ownerid) | ((receiverid == userid) & permission.contentEquals("All"))) {
						if(req.getParameter("versionid") == null) {
							ps = connection.prepareStatement("select versionid from versions where docid=? order by versionid asc");
							ps.setInt(1, docid);
							ResultSet res = ps.executeQuery();
							List<Integer> versions = new ArrayList<>(); 
							while(res.next()) {
								int version = res.getInt("versionid");
								versions.add(version);
							}
							respond(resp, 200, new Versions(versions, currentVersion));
						}else {
							int versionToView = Integer.parseInt(req.getParameter("versionid"));
							PreparedStatement preparedStatement = connection.prepareStatement("select * from versions where docid=? and versionid=?");
							preparedStatement.setInt(1, docid);
							preparedStatement.setInt(2, versionToView);
							ResultSet res = preparedStatement.executeQuery();
							if(res.next()) {
								String content = getContentToView(resp, currentVersion, versionToView, docid);
								if(content != null)
									respond(resp, 200, content,false);
								else
									respond(resp, 500, "Internal Server Error", true);
							} else
								respond(resp, 400, "Version Id is wrong!", true);
						}
					}else
						respond(resp, 401, "UnAuthorized!", true);
				}else
					respond(resp, 401, "UnAuthorized!", true);
			} else
				respond(resp, 404, "No Such Id Exists!", true);
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		}
		
	}
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		try {
			int docid = Integer.parseInt(req.getParameter("docid"));
			int versionidToChange = Integer.parseInt(req.getParameter("versionid"));
			if(checkDocExist(docid, resp)) {
				PreparedStatement ps = connection.prepareStatement("select ownerid, receiverid, permission, currentversion from document join docshared on document.docid=docshared.docid where (document.ownerid=? and document.docid=?) or (docshared.receiverid=? and document.docid=?)");
				ps.setInt(1, userid);
				ps.setInt(2, docid);
				ps.setInt(3, userid);
				ps.setInt(4, docid);
				ResultSet rs = ps.executeQuery();
				if(rs.next()) {
					int ownerid = rs.getInt("ownerid");
					int receiverid = rs.getInt("receiverid");
					int currentVersion = rs.getInt("currentversion");
					String permission = rs.getString("permission");
					if(currentVersion == versionidToChange)
						respond(resp, 200, "Already a current version!", false);
					if((userid == ownerid) | ((receiverid == userid) & permission.contentEquals("All"))) {
						PreparedStatement preparedStatement = connection.prepareStatement("select * from versions where docid=? and versionid=?");
						preparedStatement.setInt(1, docid);
						preparedStatement.setInt(2, versionidToChange);
						ResultSet res = preparedStatement.executeQuery();
						if(res.next()) {
							if(changeContent(resp, currentVersion, versionidToChange, docid))
								respond(resp, 200, "Updated Successfully!", false);
							else
								respond(resp, 500, "Internal Server Error", true);
						} else
							respond(resp, 400, "Version Id is wrong!", true);
					} else
						respond(resp, 401, "UnAuthorized!", true);
				} else
					respond(resp, 401, "UnAuthorized!", true);
			} else
				respond(resp, 404, "No Such Id Exists!", true);
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "POST Method Not Allowed", true);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "DELETE Method Not Allowed", true);
	}
	
	private boolean checkDocExist(int docid, HttpServletResponse response) {
		try {
			PreparedStatement ps = connection.prepareStatement("select * from document where docid=?");
			ps.setInt(1, docid);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				return true;
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
		}
		return false;
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
			respond(response, 500, "Internal Server Error", true);
		}
		return versionIds;
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
	
	private boolean changeContent(HttpServletResponse response, int currentVersion, int versionToView, int docid) {
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
		return updateContent(docid, sourceVersionIds, destinationVersionIds, currentVersion, response);
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
			respond(response, 500, "Internal Server Error", true);
		}
		return contents;
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
				
				diff_match_patch dmp = new diff_match_patch();
				LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(previousContent);
				Object[] results = dmp.patch_apply(patches, currentContent);
				String patchedText = (String) results[0];
				currentContent = patchedText;
			}
			
			String afterContent = null;
			for(int i = destinationContents.size()-1; i >= 0; i--){
				afterContent = destinationContents.get(i);
				diff_match_patch dmp = new diff_match_patch();
				LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(afterContent);
				Object[] results = dmp.patch_apply(patches, currentContent);
				String patchedText = (String) results[0];
				currentContent = patchedText;
			}
			return currentContent;
		} catch (Exception e) {
			respond(response, 500, "Internal Server Error", true);
		}
		return null;
	}
	
	private boolean updateContent(int docid, List<Integer> sourceVersionIds, List<Integer> destinationVersionIds, int currentVersion,HttpServletResponse response) {
		try {
			int cV = currentVersion;
			for(Integer versionid : sourceVersionIds) {
				if(versionid == cV)
					continue;
				PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as previousContent from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");
				preparedStatement.setInt(1, cV);
				preparedStatement.setInt(2, versionid);
				preparedStatement.setInt(3, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					
					InputStream c = rs.getBinaryStream("currentContent");
					InputStream p = rs.getBinaryStream("previousContent");
					String currentContent = readInputStreamToString(c, response);
					String previousContent = readInputStreamToString(p, response);
				
					String patchedText = getPatchedText(previousContent, currentContent);
					InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());
					
					String patch = getDiffText(patchedText, currentContent);
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					
					PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
					pS.setBinaryStream(1, retrievedText);
					pS.setInt(2, versionid);
					pS.setBinaryStream(3, patchContent);
					pS.setInt(4, cV);
					pS.setInt(5, versionid);
					pS.setInt(6, docid);
					pS.executeUpdate();
					cV = versionid;
				}
			}
			
			for(int i = destinationVersionIds.size()-1; i >= 0; i--){
				PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as afterContent from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");
				preparedStatement.setInt(1, cV);
				preparedStatement.setInt(2, destinationVersionIds.get(i));
				preparedStatement.setInt(3, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					diff_match_patch dmp = new diff_match_patch();
					InputStream c = rs.getBinaryStream("currentContent");
					InputStream a = rs.getBinaryStream("afterContent");
					String currentContent = readInputStreamToString(c, response);
					String afterContent = readInputStreamToString(a, response);
					
					String patchedText = getPatchedText(afterContent, currentContent);
					InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());
					
					String patch = getDiffText(patchedText, currentContent);
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					
					PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
					pS.setBinaryStream(1, retrievedText);
					pS.setInt(2, destinationVersionIds.get(i));
					pS.setBinaryStream(3, patchContent);
					pS.setInt(4, cV);
					pS.setInt(5, destinationVersionIds.get(i));
					pS.setInt(6, docid);
					pS.executeUpdate();
					cV = destinationVersionIds.get(i);
				}
	        }
			return true;
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
		}
		return false;
	}
	
	private String getPatchedText(String diffContent, String currentContent) {
		diff_match_patch dmp = new diff_match_patch();
		LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(diffContent);
		Object[] results = dmp.patch_apply(patches, currentContent);
		String patchedText = (String) results[0];
		return patchedText;
	}
	
	private String getDiffText(String content, String currentContent) {
		diff_match_patch dmp = new diff_match_patch();
		LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(content, currentContent, false);
		dmp.diff_cleanupSemantic(diffs);
		String patch = dmp.patch_toText(dmp.patch_make(diffs));
		return patch;
	}
	
	private void respond(HttpServletResponse response, int statusCode, Versions versions) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(versions);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	private void respond(HttpServletResponse response, int statusCode, String message, boolean error) {
		try(PrintWriter out = response.getWriter()){
			Response res = new Response();
			Gson gson = new Gson();
			response.setStatus(statusCode);
			if(error)
				res.setError(message);
			else
				res.setMessage(message);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(res);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	private String readInputStreamToString(InputStream inputStream, HttpServletResponse response) {
		StringBuilder sb = new StringBuilder();
		try {
			for (int ch; (ch = inputStream.read()) != -1;)
				sb.append((char) ch);
		} catch(IOException e) {
			respond(response, 500, "Internal Server Error", true);
		}
		return sb.toString();
	}
	
}
