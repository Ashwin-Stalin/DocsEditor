package common.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import common.db.Database;
import common.model.Response;

public class Register extends HttpServlet {
	private Database db = new Database();
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try(Connection connection = db.getConnection()){
			String username = req.getParameter("uname");
			String password = req.getParameter("pass");
			String cpassword = req.getParameter("cpass");
			
			PreparedStatement preparedStatement = null;
			if (password.contentEquals(cpassword)) {
				preparedStatement = connection.prepareStatement("select * from users where username=?");
				preparedStatement.setString(1, username);
				ResultSet rs = preparedStatement.executeQuery();
				if (rs.next()) 
					respond(resp, 409, "Username Already Exists", true);
				else {
					String apikey = generateUniqueAPIKey(resp);	
					preparedStatement = connection.prepareStatement("insert into users(username, password, apikey) values(?, ?, ?);");
					preparedStatement.setString(1, username);
					preparedStatement.setString(2, password);
					preparedStatement.setString(3, apikey);
					preparedStatement.executeUpdate();
					respond(resp, 200, "Registration Successfull! Login!!", false);
				}
			} else 
				respond(resp, 422, "Password and confirm password both should be equal!", true);
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		respond(resp, 405, "GET Method Not Allowed", true);
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

	private String randomString(int num) {
        String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder result = new StringBuilder();
        Random rnd = new Random();
        while (result.length() < num) {
            int index = (int) (rnd.nextFloat() * alphaNumeric.length());
            result.append(alphaNumeric.charAt(index));
        }
        return result.toString();
    }
	
	private String generateUniqueAPIKey(HttpServletResponse response) {
		try(Connection connection = db.getConnection()){
			String apiKey = randomString(32);
			PreparedStatement preparedStatement = connection.prepareStatement("select * from users where apikey=?");
			preparedStatement.setString(1, apiKey);
			ResultSet res = preparedStatement.executeQuery();
			if(res.next())
				apiKey = generateUniqueAPIKey(response);
			return apiKey;
			
		} catch (SQLException e) {
			respond(response, 500, "Internal Server Error", true);
			return null;
		}
	}
}
