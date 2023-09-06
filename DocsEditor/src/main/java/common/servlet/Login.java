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

public class Login extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			String uname = req.getParameter("uname");
			String pass = req.getParameter("pass");
			
			PreparedStatement preparedStatement = connection.prepareStatement("select * from users where username=? and password=?");
			preparedStatement.setString(1, uname);
			preparedStatement.setString(2, pass);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				HttpSession session = req.getSession();
				
				session.setAttribute("name", uname);
				session.setAttribute("userid", rs.getInt("userid"));
				resp.sendRedirect("home");
			} else {
				String invalidScriptTag = "<script>document.querySelector('#invalid').style=\"color: red;\";</script> ";
				
				req.setAttribute("invalid", invalidScriptTag);
				req.getRequestDispatcher("login-page").include(req, resp);
			}
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendRedirect("login-page");
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		}
	}

}
