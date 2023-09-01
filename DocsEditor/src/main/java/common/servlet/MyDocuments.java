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

public class MyDocuments extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");
			
			int userid = (int) session.getAttribute("userid");
			String scriptShareTag = (String) req.getAttribute("sharedSuccess");
			String scriptdeleteTag = (String) req.getAttribute("deleteSuccess");
			// Retrieving name and docid from document table
			PreparedStatement preparedStatement = connection.prepareStatement("select name,docid from document where ownerid=?");
			preparedStatement.setInt(1, userid);
			ResultSet rs = preparedStatement.executeQuery();
			out.println("<html>");
			out.println("<body>");
			out.println("<h3><i id=\"deleteSuccess\" style=\"color: blue; display: none;\">Document deleted Successfully</i></h3>");
			out.println("<h3><i id=\"sharedSuccess\" style=\"color: blue; display: none;\">Document Shared Successfully</i></h3>");
			out.println("<h2>My Documents</h2>");
			req.getRequestDispatcher("links.html").include(req, resp);
			out.println("<br><br>");
			out.println("<form action=\"create\" method=\"post\">");
			out.println("<input type=\"text\" name=\"docname\" placeholder=\"name.txt\" required>");
			out.println("<input type=\"submit\" value=\"Create New Document\">");
			out.println("</form><br>");
			out.println("<div id=\"container\">");
			while (rs.next()) {
				int docid = rs.getInt("docid");
				out.println("<div> ");
				out.println(rs.getString("name"));
				out.println("<a href=\"open?doc_id=" + docid + "\"><input type=\"button\" value=\"Open\" /></a>");
				out.println("<a href=\"download?doc_id=" + docid + "\"><input type=\"button\" value=\"Download\" /></a>");
				out.println("<a href=\"delete?doc_id=" + docid + "\"><input type=\"button\" value=\"Delete\" /></a>");
				out.println("<input class=\"showFormButton\" type=\"button\" value=\"Share\" />");
				out.println("<div class=\"formContainer\" style=\"display: none;position: fixed;top: 50%;left: 50%;transform: translate(-50%, -50%);z-index: 100;background-color: rgba(255, 255, 255, 0.9);padding: 20px;border-radius: 8px;box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);width: auto;\">");
				out.println("<input class=\"closeFormButton\" type=\"button\" value=\"Close\" style=\"position: absolute;right: 10px; color: red;\" />");
				out.println("<form action=\"share?doc_id=" + docid + "\" method=\"post\">");
				out.println("<h2>Share</h2>");
				out.println("<input type=\"text\" name=\"uname\" placeholder=\"Enter username \" required><br><br>");
				out.println("<label for=\"permissions\">Choose permission to share:</label>");
				out.println("<select name=\"permission\"><option value=\"View-Only\">View-Only</option><option value=\"All\">All</option></select>");
				out.println("<input type=\"submit\" value=\"Share\"><br>");
				out.println("</form>");
				out.println("</div>");
				out.println("<div class=\"overlay\" style=\"display: none;position: fixed;top: 0;left: 0;width: 100%;height: 100%;z-index: 99;background-color: rgba(0, 0, 0, 0.5);\"></div>");
				out.println("</div>");
			}
			out.println("</div>");
			// script tags for share button
			out.println("<script>");
			out.println("document.addEventListener('DOMContentLoaded', function() {");
			out.println("const container = document.getElementById('container');");
			out.println("container.addEventListener('click', function(event) {");
			out.println("const clickedElement = event.target;");
			out.println("if (clickedElement.classList.contains('showFormButton')) {");
			out.println("const formContainer = clickedElement.nextElementSibling;");
			out.println("const overlay = formContainer.nextElementSibling;");
			out.println("formContainer.style.display = 'block';");
			out.println("overlay.style.display = 'block';");
			out.println("} else if (clickedElement.classList.contains('closeFormButton')) {");
			out.println("const formContainer = clickedElement.parentElement;");
			out.println("const overlay = formContainer.nextElementSibling;");
			out.println("formContainer.style.display = 'none';");
			out.println("overlay.style.display = 'none';");
			out.println("}");
			out.println("});");
			out.println("});");
			out.println("</script>");
			if (scriptShareTag != null)
				out.println(scriptShareTag);
			if (scriptdeleteTag != null)
				out.println(scriptdeleteTag);
			out.println("</body>");
			out.println("</html>");
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception : " + e.getMessage());
		}
	}

}
