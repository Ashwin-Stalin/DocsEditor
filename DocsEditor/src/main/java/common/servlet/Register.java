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

import common.db.Database;

public class Register extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();
	
	@Override
	public void init() throws ServletException {
		connection = db.getConnection();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()) {
			String uname = req.getParameter("uname");
			String pass = req.getParameter("pass");
			String cpass = req.getParameter("cpass");
			PreparedStatement preparedStatement = null;
			if(pass.contentEquals(cpass)) {
				preparedStatement = connection.prepareStatement("select * from users where username=?");
				preparedStatement.setString(1, uname);
		    	ResultSet rs = preparedStatement.executeQuery();
		    	if(rs.next()) {
		    		String scriptUnameTag = "<script>document.querySelector('#username-taken').style=\"color: red;\";</script> ";
					req.setAttribute("username-taken", scriptUnameTag);
					req.getRequestDispatcher("register-page").include(req, resp);
		    	}else {
		    		preparedStatement = connection.prepareStatement("insert into users(username, password) values(?, ?);");
		    		preparedStatement.setString(1, uname);
			    	preparedStatement.setString(2, pass);
			    	preparedStatement.executeUpdate();
			    	String scriptRegistrationTag = "<script>document.querySelector('#registration').style=\"color: blue;\";</script>";
			    	req.setAttribute("registration", scriptRegistrationTag);
			    	req.getRequestDispatcher("login-page").include(req, resp);
		    	}
			}else {
	    		String scriptPassTag = "<script>document.querySelector('#pass-cpass').style=\"color: red;\";</script> ";
				req.setAttribute("pass-cpass", scriptPassTag);
				req.getRequestDispatcher("register-page").include(req, resp);
	    	}
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendRedirect("register-page");
		} catch (IOException e) {
            System.out.println("Catch IO Exception : " + e.getMessage());
        }
	}
	
	
}
