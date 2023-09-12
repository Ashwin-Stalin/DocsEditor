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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import common.db.Database;
import common.model.Document;
import common.model.Response;
import common.diff_match_patch;

public class DocsControl extends HttpServlet {
	private final Connection connection = Database.getConnection();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		try {
			if((req.getParameter("sharedwithme") == null) | (req.getParameter("sharedwithme").contentEquals("false"))) {
				sendUserDocs(req, resp, userid);
			} else if(req.getParameter("sharedwithme").contentEquals("true")) {
				sendSharedWithUserDocs(req, resp, userid);
			} else {
				respond(resp, 400, "Invalid Request!", true);
			}
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		} catch(NullPointerException e) {
			respond(resp, 400, "Invalid Request!", true);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		String docName = req.getParameter("name");
		
		try {
			String fileContent = readInputStreamToString(req.getInputStream(), resp);
			
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
					respond(resp, 200, "Document Created Successfully", false);
				}
			}
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		} catch (IOException e) {
			respond(resp, 500, "Internal Server Error", true);
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		
		if(req.getParameter("docid") == null)
			respond(resp, 400, "Invalid Request!", true);
		else {
			try {
				int docid = Integer.parseInt(req.getParameter("docid"));
				String newContent = readInputStreamToString(req.getInputStream(), resp);
				
				PreparedStatement preparedS = connection.prepareStatement("select ownerid, receiverid, permission, currentversion from document join docshared on document.docid=docshared.docid where (document.ownerid=? and document.docid=?) or (docshared.receiverid=? and document.docid=?)");
				preparedS.setInt(1, userid);
				preparedS.setInt(2, docid);
				preparedS.setInt(3, userid);
				preparedS.setInt(4, docid);
				ResultSet resultS = preparedS.executeQuery();
				if(resultS.next()) {
					int ownerid = resultS.getInt("ownerid");
					int receiverid = resultS.getInt("receiverid");
					int cversionid = resultS.getInt("currentversion");
					String permission = resultS.getString("permission");
					if((userid == ownerid) | ((receiverid == userid) & permission.contentEquals("All"))) {
						PreparedStatement ps = connection.prepareStatement("select content from versions where versionid=?");
						ps.setInt(1, cversionid);
						ResultSet res = ps.executeQuery();
						if(res.next()) {
							InputStream c = res.getBinaryStream("content");
							String oldContent = readInputStreamToString(c, resp);
		
							if(newContent.contentEquals(oldContent))
								respond(resp, 200, "Nothying to change!", false);
							else {
								makeNewVersion(resp, docid, userid, cversionid, newContent, oldContent);
							}	
						}else
							respond(resp, 404, "No Such Id Exists!", true);
					} else
						respond(resp, 401, "Unauthorized", true);
				} else
					respond(resp, 401, "Unauthorized", true);
			} catch(NumberFormatException e) {
				respond(resp, 400, "Invalid Request!", true);
			} catch (SQLException e) {
				respond(resp, 500, "Internal Server Error", true);
			} catch(Exception e) {
				respond(resp, 415, "UnSupported Type!", true);
			} 
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		
		if(req.getParameter("docid") == null)
			respond(resp, 400, "Invalid Request!", true);
		else {
			try {
				int docid = Integer.parseInt(req.getParameter("docid"));
				PreparedStatement preparedStatement = connection.prepareStatement("delete from document where docid=? and ownerid=?");
				preparedStatement.setInt(1, docid);
				preparedStatement.setInt(2, userid);
				if(preparedStatement.executeUpdate() == 1)
					respond(resp, 200, "Deleted Successfully!", false);
				else
					respond(resp, 404, "No Such Id Exist!", true);
			} catch(NumberFormatException e) {
				respond(resp, 400, "Invalid Request!", true);
			} catch(Exception e) {
				respond(resp, 415, "UnSupported Type!", true);
			} 
		}
	}
	
	private void sendUserDocs(HttpServletRequest request, HttpServletResponse response, int userid) {
		try {
			if(request.getParameter("docid") == null) {
				PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content from document join versions on document.currentversion=versions.versionid where document.ownerid=?");
				preparedStatement.setInt(1, userid);
				ResultSet rs = preparedStatement.executeQuery();
				List<Document> docs = new ArrayList<>();
				while(rs.next()) {
					int docid = rs.getInt("docid");
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					docs.add(new Document(docid, docName, content));
				}
				respond(response, 200, docs);
			}else {
				int docid = Integer.parseInt(request.getParameter("docid"));
				
				PreparedStatement preparedStatement = connection.prepareStatement("select name, content from document join versions on document.currentversion=versions.versionid where document.ownerid=? and document.docid=?");
				preparedStatement.setInt(1, userid);
				preparedStatement.setInt(2, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					respond(response, 200, new Document(docid, docName, content));
				}else
					respond(response, 404, "No Such Id Exists!", true);
			}
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			respond(response, 400, "Invalid Request!", true);
		}
	}
	
	private void sendSharedWithUserDocs(HttpServletRequest request, HttpServletResponse response, int userid) {
		try {
			if(request.getParameter("docid") == null) {
				PreparedStatement preparedStatement = connection.prepareStatement("select name, document.docid, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=?");
				preparedStatement.setInt(1, userid);
				ResultSet rs = preparedStatement.executeQuery();
				List<Document> docs = new ArrayList<>();
				while(rs.next()) {
					int docid = rs.getInt("docid");
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					String permission = rs.getString("permission");
					Document document = new Document(docid, docName, content); 
					document.setPermission(permission);
					docs.add(document);
				}
				respond(response, 200, docs);
			}else {
				int docid = Integer.parseInt(request.getParameter("docid"));
				
				PreparedStatement preparedStatement = connection.prepareStatement("select name, content, permission from document join docshared on document.docid = docshared.docid join versions on document.currentversion=versions.versionid where receiverid=? and document.docid=?");
				preparedStatement.setInt(1, userid);
				preparedStatement.setInt(2, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					String docName = rs.getString("name");
					InputStream c = rs.getBinaryStream("content");
					String content = readInputStreamToString(c, response);
					String permission = rs.getString("permission");
					Document document = new Document(docid, docName, content); 
					document.setPermission(permission);
					respond(response, 200, document);
				}else
					respond(response, 404, "No Such Id Exists!", true);
			}
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			respond(response, 400, "Invalid Request!", true);
		}
	}
	
	private void makeNewVersion(HttpServletResponse response, int docid, int userid, int versionid, String newContent, String oldContent) {
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
				respond(response, 500, "Internal Server Error", true);

			diff_match_patch dmp = new diff_match_patch();
			LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(newContent, oldContent, false);
			dmp.diff_cleanupSemantic(diffs);
			String patch = dmp.patch_toText(dmp.patch_make(diffs));
			InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
			
			preparedStatement = connection.prepareStatement("update versions set content=? where versionid=?");
			preparedStatement.setBinaryStream(1, patchContent);
			preparedStatement.setInt(2, versionid);
			if(preparedStatement.executeUpdate() == 1)
				respond(response, 200, "Updated Successfully!", false);
			else
				respond(response, 500, "Internal Server Error", true);
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
		} catch (IOException e) {
			respond(response, 500, "Internal Server Error", true);
		}
	}

	private void respond(HttpServletResponse response, int statusCode, List<Document> docs) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(docs);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	private void respond(HttpServletResponse response, int statusCode, Document doc) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(doc);
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
