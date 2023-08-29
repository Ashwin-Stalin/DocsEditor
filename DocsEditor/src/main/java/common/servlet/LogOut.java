package common.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LogOut extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try {
			HttpSession session = req.getSession();  
			session.invalidate();
			resp.sendRedirect("login-page");
		} catch (IOException e) {
            System.out.println("Catch IO Exception : " + e.getMessage());
        } 
	}

}
