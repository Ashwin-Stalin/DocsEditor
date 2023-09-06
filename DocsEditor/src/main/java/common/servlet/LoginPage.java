package common.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginPage extends HttpServlet {
	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session != null)
				resp.sendRedirect("home");
			
			String invalidScriptTag = (String) req.getAttribute("invalid");
			String registrationScriptTag = (String) req.getAttribute("registration");
			
			out.println("<html><body");
			out.println("<h3><i id=\"registration\" style=\"color: blue; display: none;\">Registration Successfull</i><br></h3>");
			out.println("<div class=\"form\"><form action=\"login\" method=\"post\">");
			out.println("<h2>Login<h2>");
			out.println("<input type=\"text\" name=\"uname\" placeholder=\"Enter Your UserName\" required><br><br>");
			out.println("<input type=\"password\" name=\"pass\" placeholder=\"Enter Your Password\" required><br>");
			out.println("<i id=\"invalid\" style=\"color: red; display: none;\">*Invalid Credentials</i><br>");
			out.println("<input type=\"submit\" value=\"LOGIN\"><br><br>");
			out.println("<h4>Don't have an account? <a href=\"register-page\">Sign Up</a></h4>");
			out.println("</div></form>");
			
			if (invalidScriptTag != null)
				out.println(invalidScriptTag);
			if (registrationScriptTag != null)
				out.println(registrationScriptTag);
			
			out.println("</body></html>");
			
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		}
	}
}