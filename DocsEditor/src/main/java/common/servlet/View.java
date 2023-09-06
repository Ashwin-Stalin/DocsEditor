package common.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;
import common.model.diff_match_patch;

public class View extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()){
			HttpSession session = req.getSession(false);
			if (session == null) 
				resp.sendRedirect("login-page");

			int docid = Integer.parseInt(req.getParameter("doc_id"));
			int currentVersion = Integer.parseInt(req.getParameter("current_version"));
			int versionToView = Integer.parseInt(req.getParameter("version_to_view"));
			
			List<Integer> sourceVersionIds = new ArrayList<>();
			List<Integer> destinationVersionIds = new ArrayList<>();
			
			int versionToAdd = currentVersion;
			sourceVersionIds.add(versionToAdd);
			while(true) {
				PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, docid);
				ResultSet rs = prepareStatement.executeQuery();
				if(rs.next()) {
					int version = rs.getInt("fromversion");
					sourceVersionIds.add(version);
					versionToAdd = version;
				}else
					break;
			}
			
			versionToAdd = versionToView;
			destinationVersionIds.add(versionToAdd);
			while(true) {
				PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, docid);
				ResultSet rs = prepareStatement.executeQuery();
				if(rs.next()) {
					int version = rs.getInt("fromversion");
					destinationVersionIds.add(version);
					versionToAdd = version;
				}else
					break;
			}
			
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
			
			List<String> sourceContents = new ArrayList<>();
			List<String> destinationContents = new ArrayList<>();
			
			for(Integer versionid : sourceVersionIds) {
				PreparedStatement prepareStatement = connection.prepareStatement("select content from versions where versionid=?");
				prepareStatement.setInt(1, versionid);
				ResultSet resultSet = prepareStatement.executeQuery();
				while(resultSet.next()) {
					InputStream c = resultSet.getBinaryStream("content");
					String content = readInputStreamToString(c);
					sourceContents.add(content);
				}
			}
			
			for(Integer versionid : destinationVersionIds) {
				PreparedStatement prepareStatement = connection.prepareStatement("select content from versions where versionid=?");
				prepareStatement.setInt(1, versionid);
				ResultSet resultSet = prepareStatement.executeQuery();
				while(resultSet.next()) {
					InputStream c = resultSet.getBinaryStream("content");
					String content = readInputStreamToString(c);
					destinationContents.add(content);
				}
			}
			
			
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
			
			req.setAttribute("content_to_display", currentContent);
			req.getRequestDispatcher("history?doc_id=" + docid + "&current_version="+ currentVersion).include(req, resp);
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch(SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}
	
	private String readInputStreamToString(InputStream inputStream) {
		StringBuilder sb = new StringBuilder();
		try {
			for (int ch; (ch = inputStream.read()) != -1;)
				sb.append((char) ch);
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage() );
		}
		return sb.toString();
	}
}
