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

public class DownloadDoc extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();
	
	@Override
	public void init() {
		connection = db.getConnection();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);  
		    if(session == null){  
		    	resp.sendRedirect("login-page"); 
		    }else {
			    int userid = (int) session.getAttribute("userid");
			    int docid = Integer.parseInt(req.getParameter("doc_id"));
			    PreparedStatement preparedStatement = connection.prepareStatement("select ownerid,name, content from document where docid=? AND ownerid=?");
			    preparedStatement.setObject(1, docid);
			    preparedStatement.setObject(2, userid);
			    ResultSet rs = preparedStatement.executeQuery();
			    if(rs.next()) {
		    		String content = rs.getString("content");
	    			String docName = rs.getString("name");
	    			resp.setContentType("text/plain");
	    			resp.setHeader("Content-Disposition", "attachment; filename="+ docName);
	    			out.write(content);
			    } else {
			    	out.print("UnAuthorized !");
			    }
		    }
		} catch (IOException e) {
            System.out.println("Catch IO Exception : " + e.getMessage());
        } catch (SQLException e) {
        	System.out.println("Catch SQL Exception : " + e.getMessage());
        }
        
	}
	
}
