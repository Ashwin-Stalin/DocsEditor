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

public class DeleteDoc extends HttpServlet {
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
		    if(session==null){  
		    	resp.sendRedirect("login-page"); 
		    }
		    int userid = (int) session.getAttribute("userid");
		    int docid = Integer.parseInt(req.getParameter("doc_id"));
		    
		    PreparedStatement ps = connection.prepareStatement("select * from document where docid=? and ownerid=?");
		    ps.setInt(1, docid);
		    ps.setInt(2, userid);
		    ResultSet rs = ps.executeQuery();
		    if(rs.next()) {
		    	PreparedStatement preparedStatement = connection.prepareStatement("delete from docshared where docid=? ; delete from document where docid=?;");
			    preparedStatement.setInt(1, docid);
			    preparedStatement.setInt(2, docid);
			    preparedStatement.executeUpdate();
			    String scriptTag = "<script>document.querySelector('#deleteSuccess').style=\"color: blue;\";</script> ";
				req.setAttribute("deleteSuccess", scriptTag);
				req.getRequestDispatcher("documents").include(req, resp);
		    }else {
		    	PreparedStatement preparedStatement = connection.prepareStatement("delete from docshared where docid=? and receiverid=?");
		    	preparedStatement.setInt(1, docid);
			    preparedStatement.setInt(2, userid);
			    preparedStatement.executeUpdate();
			    String scriptTag = "<script>document.querySelector('#deleteSuccess').style=\"color: blue;\";</script> ";
				req.setAttribute("deleteSuccess", scriptTag);
				req.getRequestDispatcher("sharedwithme").include(req, resp);
		    }
		    
		    
		} catch (IOException e) {
            System.out.println("Catched IO Exception : " + e.getMessage());
        } catch (ServletException e) {
        	System.out.println("Catched Servlet Exception : " + e.getMessage());
        } catch (SQLException e) {
        	System.out.println("Catched SQL Exception : " + e.getMessage());
        }
	}
	
	
	
	
	
}
