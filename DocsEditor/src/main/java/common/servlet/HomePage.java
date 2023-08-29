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

public class HomePage extends HttpServlet {
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()){
			HttpSession session = req.getSession(false);
			if(session == null)
				resp.sendRedirect("login-page");
			String name = (String)session.getAttribute("name"); 
//			int userid = (int) session.getAttribute("userid");
			out.println("<html>");
		    out.println("<body>");
		    req.getRequestDispatcher("links.html").include(req, resp); 
		    out.println("<br>");
		    out.println("<h1>Welcome , " + name + "</h1>");
		    out.println("<br><br>");
		    out.println("</body>");
		    out.println("</html>");
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}
}
