package common.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import common.db.Database;
import common.model.Response;

public class Verification implements Filter {
	private final Connection connection = Database.getConnection();
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		try {
			String apikey = req.getHeader("x-api-key");
			PreparedStatement prepareStatement = connection.prepareStatement("select userid from users where apikey=?");
			prepareStatement.setString(1, apikey);
			ResultSet rs = prepareStatement.executeQuery();
			if(rs.next()) {
				int userid = rs.getInt("userid");
				request.setAttribute("userid", userid);
				chain.doFilter(request, response);
			}else
				respond(resp, 401, "Unauthorized", true);
		} catch (IOException e) {
			respond(resp, 500, "Internal Server Error", true);
		} catch (ServletException e) {
			respond(resp, 500, "Internal Server Error", true);
		} catch (SQLException e) {
			respond(resp, 500, "Internal Server Error", true);
		}
	}
	
	@Override
	public void init(FilterConfig fConfig) throws ServletException {

	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
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
