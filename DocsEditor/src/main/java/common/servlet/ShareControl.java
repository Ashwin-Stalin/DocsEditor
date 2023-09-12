package common.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import common.db.Database;
import common.model.DocSharedDetail;
import common.model.Response;

public class ShareControl extends HttpServlet {
	private final Connection connection = Database.getConnection();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		
		try {
			if(req.getParameter("docid") == null) {
				PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=?");
				ps.setInt(1, userid);
				ResultSet rs = ps.executeQuery();
				List<DocSharedDetail> sharedDetails = new ArrayList<>();
				while(rs.next()) {
					int docid = rs.getInt("docid");
					String docname = rs.getString("name");
					int receivedUserId = rs.getInt("receiverid");
					String receivedUserName = rs.getString("receivedUserName");
					String permission = rs.getString("permission");
					sharedDetails.add(new DocSharedDetail(docid, docname, receivedUserId, receivedUserName, permission));
				}
				respond(resp, 200, sharedDetails);
				
			}else {
				int docid = Integer.parseInt(req.getParameter("docid"));
				
				PreparedStatement ps = connection.prepareStatement("select document.docid, name, receiverid, users.username as receivedUserName, permission from document join docshared on document.docid=docshared.docid join users on docshared.receiverid=users.userid where ownerid=? and document.docid=?");
				ps.setInt(1, userid);
				ps.setInt(2, docid);
				ResultSet rs = ps.executeQuery();
				
				if (rs.next() == false)
					respond(resp, 404, "No Such Id Exists!", true);
			    else {
			    	List<DocSharedDetail> sharedDetails = new ArrayList<>();
			        do {
						String docname = rs.getString("name");
						int receivedUserId = rs.getInt("receiverid");
						String receivedUserName = rs.getString("receivedUserName");
						String permission = rs.getString("permission");
						sharedDetails.add(new DocSharedDetail(docid, docname, receivedUserId, receivedUserName, permission));
					} while (rs.next());
			        respond(resp, 200, sharedDetails);
			    }
			}
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		try {
			int docid = Integer.parseInt(req.getParameter("docid"));
			String toSendUname = req.getParameter("username");
			String permission = req.getParameter("permission");
			if(!(permission.contentEquals("View-Only") || permission.contentEquals("All")))
				respond(resp, 400, "Invalid Permission", true);
		
			PreparedStatement ps = connection.prepareStatement("select * from document where ownerid=? and docid=?");
			ps.setInt(1, userid);
			ps.setInt(2, docid);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				ps = connection.prepareStatement("select userid from users where username=?");
				ps.setString(1, toSendUname);
				ResultSet rs = ps.executeQuery();
				if(rs.next()) {
					int toSendUserId = rs.getInt("userid");
					ps = connection.prepareStatement("select * from docshared where docid=? and receiverid=?");
					ps.setInt(1, docid);
					ps.setInt(2, toSendUserId);
					ResultSet resultSet = ps.executeQuery();
					if(resultSet.next())
						respond(resp, 200, "You Already Shared This Document With This User.", false);
					else {
						ps = connection.prepareStatement("insert into docshared(docid, receiverid, permission) values(?,?,?)");
						ps.setInt(1, docid);
						ps.setInt(2, toSendUserId);
						ps.setString(3, permission);
						if(ps.executeUpdate() == 1)
							respond(resp, 200, "Shared Successfully", false);
						else
							respond(resp, 500, "Internal Server Error", true);
					}
				}else
					respond(resp, 404, "No Such User Exists!", true);
			}else
				respond(resp, 401, "You are not the owner!", true);
				
		} catch (SQLException e) {
			e.printStackTrace();
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
			int toShareId = Integer.parseInt(req.getParameter("userid"));
			String permission = req.getParameter("permission");
			if(!(permission.contentEquals("View-Only") || permission.contentEquals("All")))
				respond(resp, 400, "Invalid Permission", true);
		
			PreparedStatement ps = connection.prepareStatement("select * from document where ownerid=? and docid=?");
			ps.setInt(1, userid);
			ps.setInt(2, docid);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				ps = connection.prepareStatement("select userid from users where userid=?");
				ps.setInt(1, toShareId);
				ResultSet rs = ps.executeQuery();
				if(rs.next()) {
					ps = connection.prepareStatement("update docshared set permission=? where docid=? and receiverid=?");
					ps.setString(1, permission);
					ps.setInt(2, docid);
					ps.setInt(3, toShareId);
					if(ps.executeUpdate() == 1)
						respond(resp, 200, "Permission Updated Successfully!", false);
					else
						respond(resp, 500, "Internal Server Error!", true);
				}else
					respond(resp, 404, "No Such User Exists!", true);
			}else
				respond(resp, 401, "You are not the owner!", true);
				
		} catch (SQLException e) {
			e.printStackTrace();
			respond(resp, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		
		try {
			int docid = Integer.parseInt(req.getParameter("docid"));
			PreparedStatement preparedStatement = connection.prepareStatement("select * from document where docid=? and ownerid=?");
			preparedStatement.setInt(1, docid);
			preparedStatement.setInt(2, userid);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next()) {
				if(req.getParameter("userid") != null) {
					int receivedUserId = Integer.parseInt(req.getParameter("userid"));
					PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=? and receiverid=?");
					ps.setInt(1, docid);
					ps.setInt(2, receivedUserId);
					if(ps.executeUpdate() == 1)
						respond(resp, 200, "Deleted Successfully!", false);
					else
						respond(resp, 404, "Check Inputs!", true);
				}else {
					PreparedStatement ps = connection.prepareStatement("delete from docshared where docid=?");
					ps.setInt(1, docid);
					if(ps.executeUpdate() == 1)
						respond(resp, 200, "Deleted Successfully!", false);
					else
						respond(resp, 404, "Check Inputs!", true);
				}
			}else
				respond(resp, 401, "Unauthorized", true);
			
		} catch(NumberFormatException e) {
			respond(resp, 400, "Invalid Request!", true);
		} catch(Exception e) {
			respond(resp, 415, "UnSupported Type!", true);
		} 
		
	}

	private void respond(HttpServletResponse response, int statusCode, List<DocSharedDetail> sharedDetails) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(sharedDetails);
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
}
