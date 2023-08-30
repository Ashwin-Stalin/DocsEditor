package common.servlet;

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

public class OpenDoc extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();
	
	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()){
			HttpSession session = req.getSession(false);  
		    if(session==null){  
		    	resp.sendRedirect("login-page"); 
		    }
		    int userid = (int) session.getAttribute("userid");
		    int docid = Integer.parseInt(req.getParameter("doc_id"));
		    PreparedStatement ps = connection.prepareStatement("select currentversion from document where docid=? and ownerid=?");
		    ps.setInt(1, docid);
		    ps.setInt(2, userid);
		    ResultSet rs = ps.executeQuery();
		    out.println("<html>");
		    out.println("<body>");
		    req.getRequestDispatcher("links.html").include(req, resp);
		    out.println("<br><br>");
		    if(rs.next()) {
		    	int currentVersion = rs.getInt("currentversion");
		    	ps = connection.prepareStatement("select content from versions where versionid=?");
			    ps.setInt(1, currentVersion);
			    ResultSet res = ps.executeQuery();
			    if(res.next()) {
			    	InputStream content = res.getBinaryStream("content");
			    	String fileContent = readInputStreamToString(content);
			    	out.println("<form action=\"save?doc_id="+ docid +"\" method=\"POST\">");
				    out.println("<input type=\"submit\" value=\"Save\">");
				    out.println("<button type=\"button\"><a href=\"undo?doc_id="+ docid+"\">Undo</a></button>");
				    out.println("<button type=\"button\"><a href=\"redo?doc_id="+ docid+"\">Redo</a></button><br><br>");
				    out.println("<textarea name=\"textToSave\" rows=\"40\" cols=\"80\">"+ fileContent);
				    out.println("</textarea>");
				    out.println("</form>");
			    }
			    return;
		    }else {
		    	PreparedStatement preparedStatement = connection.prepareStatement("select permission, currentversion from docshared join document on docshared.docid=? and docshared.receiverid=?");
			    preparedStatement.setInt(1, docid);
			    preparedStatement.setInt(2, userid);
			    ResultSet rsss = preparedStatement.executeQuery();
			    if(rsss.next()) {
			    	String permission = rsss.getString("permission");
			    	int currentVersion = rsss.getInt("currentversion");
			    	PreparedStatement pS = connection.prepareStatement("select content from versions where versionid=?");
				    pS.setInt(1, currentVersion);
				    ResultSet rss = pS.executeQuery();
				    if(rss.next()) {
				    	InputStream content = rss.getBinaryStream("content");
				    	String fileContent = readInputStreamToString(content);
				    	if(permission.contentEquals("All")) {
				    		out.println("<form action=\"save?doc_id="+ docid + "\" method=\"POST\">");
				    		out.println("<input type=\"submit\" value=\"Save\">");
				    		out.println("<button type=\"button\"><a href=\"undo?doc_id="+ docid+"\">Undo</a></button>");
				    		out.println("<button type=\"button\"><a href=\"redo?doc_id="+docid+"\">Redo</a></button><br><br>");
				    		out.println("<textarea name=\"textToSave\" rows=\"40\" cols=\"80\">");
				    	}else if(permission.contentEquals("View-Only"))
					    	out.println("<textarea name=\"textToSave\" rows=\"40\" cols=\"80\" readonly>");
					    else
					    	out.println("Error");
				    	out.println(fileContent);
					    out.println("</textarea>");
					    out.println("</form>");
			    	}
			    }else {
			    	out.print("Not authorized");
			    }
		    }
		    out.println("</body>");
		    out.println("</html");
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}
	
	private String readInputStreamToString(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int ch; (ch = inputStream.read()) != -1; ) {
		    sb.append((char) ch);
		}
		return sb.toString();
    }
	
}
