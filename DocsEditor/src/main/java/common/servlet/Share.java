package common.servlet;

import java.io.IOException;
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

public class Share extends HttpServlet {
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
			int docid = Integer.parseInt(req.getParameter("doc_id"));
			String toSendUname = req.getParameter("uname");
			String permission = req.getParameter("permission");
			
			PreparedStatement preparedStatement = connection.prepareStatement("select userid from users where username=? and userid!=?");
			preparedStatement.setString(1, toSendUname);
			preparedStatement.setInt(2, userid);
			ResultSet rs = preparedStatement.executeQuery();
			
			while (rs.next()) {
				int toSendUserid = rs.getInt("userid");
				
				preparedStatement = connection.prepareStatement("select * from docshared where docid=? and receiverid=?");
				preparedStatement.setObject(1, docid);
				preparedStatement.setInt(2, toSendUserid);
				ResultSet res = preparedStatement.executeQuery();
				
				if (res.next()) {
					out.println("<font color=red> You already shared this document to this user. Try changing permission </font>");
					
					req.getRequestDispatcher("documents").include(req, resp);

				} else {
					preparedStatement = connection.prepareStatement("insert into docshared(docid, receiverid, permission) values(?,?,?)");
					preparedStatement.setObject(1, docid);
					preparedStatement.setInt(2, toSendUserid);
					preparedStatement.setString(3, permission);
					preparedStatement.executeUpdate();
					
					String scriptTag = "<script>document.querySelector('#sharedSuccess').style=\"color: blue;\";</script> ";
					
					req.setAttribute("sharedSuccess", scriptTag);
					req.getRequestDispatcher("documents").include(req, resp);
				}
			}
			out.println("Error sharing");
		} catch (IOException e) {
			System.out.println("Catched IO Exception : " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception : " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception : " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendRedirect("home");
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		}
	}
}
