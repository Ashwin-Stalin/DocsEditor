package common.test;

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

public class CheckingPurpose extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();
	@Override
	public void init()  {
		connection = db.getConnection();
	}
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()){
			HttpSession session = req.getSession(false);  
		    if(session==null){  
		    	resp.sendRedirect("login-page"); 
		    }
		    int userid = (int) session.getAttribute("userid");
		    int docid = Integer.parseInt(req.getParameter("doc_id"));
		    PreparedStatement preparedStatement = connection.prepareStatement("select * from versions where docid=?");
		    preparedStatement.setInt(1, docid);
		    ResultSet rs = preparedStatement.executeQuery();
		    while(rs.next()) {
		    	int versionid = rs.getInt("versionid");
		    	InputStream c = rs.getBinaryStream("content");
		    	String content = readInputStreamToString(c);
		    	int editeduserid = rs.getInt("editeduserid");
		    	out.println("Version ID : " + versionid);
		    	out.println("Content : " + content);
		    	out.println("Edited User ID : " + editeduserid);
		    	out.println("");
		    }
		    
		}catch(IOException e) {
			System.out.println("Catched IO Exception : " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception : " + e.getMessage());
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
