package common.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;

public class History extends HttpServlet {
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
			String fileContent = (String) req.getAttribute("content_to_display");
			
			PreparedStatement preparedStatement = connection.prepareStatement("select versionid from versions where docid=? order by versionid desc");
			preparedStatement.setInt(1, docid);
			ResultSet rsss = preparedStatement.executeQuery();
			
			out.println("<html><body>");
			out.println("<h2>Viewing History..</h2>");
			out.println("<button type=\"button\"><a href=\"open?doc_id="+docid+"\">Close</a></button><br>");
			out.println("<br>");
			out.println("<div style=\"display:flex\">");
			out.println("<div style=\"margin-right : 20px\">");
			
			while (rsss.next()) {
				int versionid = rsss.getInt("versionid");
				
				out.println("<button type=\"button\"><a href=\"view?doc_id=" + docid + "&current_version=" + currentVersion + "&version_to_view=" + versionid + "\">Version Id : " + versionid + "</a></button>");
				out.println("<button type=\"button\"><a href=\"changeVersion?version_id_to_change=" + versionid + "&current_version_id="+ currentVersion +"&doc_id="+docid+"\">Change</a></button>");
				out.println("<br><br>");
			}
			
			out.println("</div>");
			out.println("<div>");
			out.println("<textarea name=\"textToSave\" rows=\"40\" cols=\"80\">"+ fileContent +"</textarea>");
			out.println("</div>");
			out.println("</div>");
			out.println("</body></html>");
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch(SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		}
	}
	
}
