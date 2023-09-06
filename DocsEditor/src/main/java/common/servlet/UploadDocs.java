package common.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UploadDocs extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");
			
			String errorScriptTag = (String) req.getAttribute("error");
			String uploadedScriptTag = (String) req.getAttribute("uploaded");
			
			out.println("<html>");
			out.println("<body>");
			out.println("<h3><i id=\"uploaded\" style=\"color: blue; display: none;\">Uploaded Successfully</i></h3>");
			out.println("<h1>Upload Documents</h1>");
			req.getRequestDispatcher("links.html").include(req, resp);
			out.println("<br><br>");
			out.println("<form action=\"upload\" method=\"post\" enctype=\"multipart/form-data\">");
			out.println("<input type=\"file\" name=\"docs\" required multiple><br>");
			out.println("<i id=\"error-file\" style=\"color: red; display: none;\">*Invalid File Type</i><br>");
			out.println("<input type=\"submit\" value=\"Upload\">");
			out.println("</form>");
			
			if (errorScriptTag != null)
				out.print(errorScriptTag);
			if (uploadedScriptTag != null)
				out.print(uploadedScriptTag);
			
			out.println("</body>");
			out.println("</html>");
		} catch (IOException e) {
			System.out.println("Catched IO Exception : " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception : " + e.getMessage());
		}
	}

}
