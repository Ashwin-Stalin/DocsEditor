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

public class SharedWithMe extends HttpServlet {
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
			if(session == null)
				resp.sendRedirect("login-page");
			int userid = (int) session.getAttribute("userid");
			String scriptdeleteTag = (String) req.getAttribute("deleteSuccess");
			PreparedStatement preparedStatement = connection.prepareStatement(" select ds.shareid ,ds.docid, ds.receiverid, ds.permission, d.ownerid, d.name  from docshared as ds join document as d on ds.docid=d.docid  where receiverid=?");
			preparedStatement.setInt(1, userid);
			ResultSet rs = preparedStatement.executeQuery();
			out.println("<html>");
			out.println("<body>");
			out.println("<h3><i id=\"deleteSuccess\" style=\"color: blue; display: none;\">Document deleted Successfully</i></h3>");
			out.println("<h2>Shared With Me</h2>");
			req.getRequestDispatcher("links.html").include(req, resp);
			out.println("<br><br>");
		    out.println("<div id=\"container\">");
		    while(rs.next()) {
		    	int docid = rs.getInt("docid");
		    	out.println("<div> ");
		    	out.println(rs.getString("name"));
		    	out.println("<a href=\"open?doc_id="+ docid +"\"><input type=\"button\" value=\"Open\" /></a>");
		    	out.println("<a href=\"delete?doc_id="+ docid +"\"><input type=\"button\" value=\"Delete\" /></a>");
		    	out.println("</div>");
		    }
		    out.println("</div>");
            if(scriptdeleteTag != null)
		    	out.println(scriptdeleteTag);
			out.println("</body>");
			out.println("</html>");
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (ServletException e) {
        	System.out.println("Catched Servlet Exception : " + e.getMessage());
        } catch (SQLException e) {
        	System.out.println("Catched SQL Exception : " + e.getMessage());
        }
	}

}
