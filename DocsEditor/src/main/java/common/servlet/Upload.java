package common.servlet;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import common.db.Database;

public class Upload extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");
			int userid = (int) session.getAttribute("userid");
			for (Part part : req.getParts()) {
				if (part.getName().equals("docs") && part.getSize() > 0) {
					String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
					InputStream filecontent = part.getInputStream();
					// Inserting into document table and returning docid
					PreparedStatement preparedStatement = connection.prepareStatement("insert into document(name,ownerid) values(?,?) returning docid");
					preparedStatement.setString(1, fileName);
					preparedStatement.setInt(2, userid);
					ResultSet resultSet = preparedStatement.executeQuery();
					if (resultSet.next()) {
						int docid = resultSet.getInt("docid");
						// Inserting into versions table and returning versionid
						preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?, ?, ?) returning versionid");
						preparedStatement.setInt(1, docid);
						preparedStatement.setBinaryStream(2, filecontent, filecontent.available());
						preparedStatement.setInt(3, userid);
						ResultSet rs = preparedStatement.executeQuery();
						if (rs.next()) {
							int versionid = rs.getInt("versionid");
							// Updating the document table's current version
							preparedStatement = connection.prepareStatement("update document set currentversion=? where docid=?");
							preparedStatement.setInt(1, versionid);
							preparedStatement.setInt(2, docid);
							preparedStatement.executeUpdate();
						} else {
							out.print("error");
						}
					} else {
						out.println("error");
					}
				}
			}
			String scriptTag = "<script>document.querySelector('#uploaded').style=\"color: blue;\";</script> ";
			req.setAttribute("uploaded", scriptTag);
			req.getRequestDispatcher("uploadDocs").include(req, resp);
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catch Servlet Exception : " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catch SQL Exception : " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");
			else
				resp.sendRedirect("uploadDocs");
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		}
	}

}
