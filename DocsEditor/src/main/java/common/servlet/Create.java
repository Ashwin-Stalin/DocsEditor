package common.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;

public class Create extends HttpServlet {
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
			if (session == null) {
				resp.sendRedirect("login-page");
			}
			int userid = (int) session.getAttribute("userid");
			String docName = req.getParameter("docname");
			PreparedStatement preparedStatement = connection.prepareStatement("insert into document(name, ownerid) values(?,?) returning docid");
			preparedStatement.setString(1, docName);
			preparedStatement.setInt(2, userid);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				int docid = rs.getInt("docid");
				String c = "";
				InputStream content = new ByteArrayInputStream(c.getBytes());
				preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");
				preparedStatement.setInt(1, docid);
				preparedStatement.setBinaryStream(2, content);
				preparedStatement.setInt(3, userid);
				ResultSet res = preparedStatement.executeQuery();
				if (res.next()) {
					int newversionid = res.getInt("versionid");
					preparedStatement = connection.prepareStatement("update document set currentversion=? where docid=?");
					preparedStatement.setInt(1, newversionid);
					preparedStatement.setInt(2, docid);
					preparedStatement.executeUpdate();
					resp.sendRedirect("documents");
				}
			}
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendRedirect("documents");
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		}
	}

}
