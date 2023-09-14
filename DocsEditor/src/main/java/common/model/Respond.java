package common.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class Respond {
	public static void sendData(HttpServletResponse response, int statusCode, String message, boolean error) {
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
	
	public static void sendData(HttpServletResponse response, int statusCode, Doc doc) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(doc);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	public static void sendData(HttpServletResponse response, int statusCode, List<Doc> docs) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(docs);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	public static void sendData(HttpServletResponse response, int statusCode, Versions versions) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(versions);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	public static void sendData(int statusCode, HttpServletResponse response, List<SharedUser> sharedDetails) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(sharedDetails);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
	
	public static void sendData(int statusCode, HttpServletResponse response, SharedUser sharedDetail) {
		try(PrintWriter out = response.getWriter()){
			Gson gson = new Gson();
			response.setStatus(statusCode);
			response.setContentType("application/json");
			String jsonResponse = gson.toJson(sharedDetail);
			out.println(jsonResponse);
		}catch(IOException ioexception) {
			ioexception.printStackTrace();
		}
	}
}
