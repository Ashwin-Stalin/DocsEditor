package common.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Registerpage extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try(PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);  
		    if(session!=null){  
		    	resp.sendRedirect("home");  
		    }else{
		    	String scriptUnameTag = (String) req.getAttribute("username-taken");
		    	String scriptPassTag = (String) req.getAttribute("pass-cpass");
			    out.println("<html>");
			    out.println("<body>");
			    out.println("<br>");
			    out.println("<form action=\"register\" method=\"post\">");
			    out.println("<br><h2>Register</h2>");
			    out.println("<input type=\"text\" name=\"uname\" placeholder=\"Username\" required><br>");
			    out.println("<i id=\"username-taken\" style=\"color: red; display: none;\">*Username Already Taken</i><br>");
			    out.println("<input type=\"password\" name=\"pass\" placeholder=\"Password\" required><br><br>");
			    out.println("<input type=\"password\" name=\"cpass\" placeholder=\"Confirm Password\" required><br>");
			    out.println("<i id=\"pass-cpass\" style=\"color: red; display: none;\">*Password and confirm password both should be equal!</i><br>");
			    out.println("<input type=\"submit\" value=\"REGISTER\"><br><br>");
			    out.println("<h4>Have an account? <a href=\"login-page\">Sign In</a></h4>");
			    out.println("</form>");
			    if(scriptUnameTag != null)
			    	out.print(scriptUnameTag);
			    if(scriptPassTag != null)
			    	out.print(scriptPassTag);
			    out.println("</body>");
			    out.println("</html>");
		    }
		} catch (IOException e) {
            System.out.println("Catch IO Exception : " + e.getMessage());
        }
	}
	
}
