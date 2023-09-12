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

import com.google.gson.Gson;
import common.model.Response;

import common.db.Database;

public class Login extends HttpServlet {
	private final Connection connection = Database.getConnection();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try{
			String username = req.getParameter("uname");
			String password = req.getParameter("pass");
			
			PreparedStatement preparedStatement = connection.prepareStatement("select apikey from users where username=? and password=?");
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, password);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				String apikey = rs.getString("apikey");
				respond(resp, 200, apikey, false);
			} else
				respond(resp, 401, "Unauthorized!", true);
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "POST Method Not Allowed", true);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "PUT Method Not Allowed", true);
	}

	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "DELETE Method Not Allowed", true);
	}

	private void respond(HttpServletResponse response, int statusCode, String message, boolean error) {
		try(PrintWriter out = response.getWriter()){
			Response res = new Response();
			Gson gson = new Gson();
			response.setStatus(statusCode);
			if(error)
				res.setError(message);
			else
				res.setMessage(message);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(res);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}

}
