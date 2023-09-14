package common.filter;

import java.io.IOException;
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

import common.db.Database;
import common.model.Documents;
import common.model.Paths;
import common.model.Respond;

public class Authorization implements Filter {
	private final Connection connection = Database.getConnection();
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
				String pathInfo = req.getPathInfo();
				if(pathInfo == null) {
					String method = req.getMethod();
					if(method.contentEquals("GET") | method.contentEquals("POST")) {
						request.setAttribute("paths", Paths.NONE); 
						chain.doFilter(request, response);
					} else
						Respond.sendData(resp, 405, method + " Method Not Allowed", true);
				} else if(pathInfo.startsWith("/")) {
					Documents documents = new Documents();
					String[] paths = pathInfo.split("/");
					if(paths.length>4 | paths.length < 2)
						Respond.sendData(resp, 400, "Invalid Request!", true);
					if(paths.length == 2) {
						int docid = Integer.parseInt(paths[1]);
						String method = req.getMethod();
						if(!(method.contentEquals("GET") | method.contentEquals("PUT") | method.contentEquals("DELETE")))
							Respond.sendData(resp, 405, method + " Method Not Allowed", true);
						if((method.contentEquals("GET") & documents.canViewDocument(docid, userid, resp)) | (method.contentEquals("PUT") & (documents.checkOwner(docid, userid, resp) | documents.checkReceivedWithPermissionToEdit(docid, userid, resp)) | (method.contentEquals("DELETE") & documents.checkOwner(docid, userid, resp)))) {
							request.setAttribute("paths", Paths.DOCID);
							request.setAttribute("docid", docid);
							chain.doFilter(request, response);
						} else
							Respond.sendData(resp, 401, "Unauthorized", true);
					} 
					if(paths.length == 3) {
						String path= paths[2];
						if(path.contentEquals("versions")) {
							int docid = Integer.parseInt(paths[1]);
							String method = req.getMethod();
							if(!method.contentEquals("GET"))
								Respond.sendData(resp, 405, method + " Method Not Allowed", true);
							else if(documents.canViewDocument(docid, userid, resp)) {
								request.setAttribute("paths", Paths.VERSIONS);
								request.setAttribute("docid", docid);
								chain.doFilter(request, response);
							}else
								Respond.sendData(resp, 401, "Unauthorized", true);
						}else if(path.contentEquals("shared-users")) {
							int docid = Integer.parseInt(paths[1]);
							String method = req.getMethod();
							if(!(method.contentEquals("GET") | method.contentEquals("POST") | method.contentEquals("DELETE")))
								Respond.sendData(resp, 405, method + " Method Not Allowed", true);
							else if(documents.checkOwner(docid, userid, resp)) {
								request.setAttribute("paths", Paths.SHAREDUSERS);
								request.setAttribute("docid", docid);
								chain.doFilter(request, response);
							}else
								Respond.sendData(resp, 401, "Unauthorized", true);
						}else
							Respond.sendData(resp, 400, "Invalid Request!", true);
					} 
					if(paths.length == 4) {
						String path= paths[2];
						if(path.contentEquals("versions")) {
							int docid = Integer.parseInt(paths[1]);
							int versionid = Integer.parseInt(paths[3]);
							String method = req.getMethod();
							if(!(method.contentEquals("GET") | method.contentEquals("PUT")))
								Respond.sendData(resp, 405, method+" Method Not Allowed", true);
							if((method.contentEquals("GET") & documents.canViewDocument(docid, userid, resp)) | (method.contentEquals("PUT") & (documents.checkOwner(docid, userid, resp) | documents.checkReceivedWithPermissionToEdit(docid, userid, resp)))) {
								request.setAttribute("paths", Paths.VERSIONID);
								request.setAttribute("docid", docid);
								request.setAttribute("versionid", versionid);
								chain.doFilter(request, response);
							}else
								Respond.sendData(resp, 401, "Unauthorized", true);
						}else if(path.contentEquals("shared-users")) {
							int docid = Integer.parseInt(paths[1]);
							int shareduserid = Integer.parseInt(paths[3]);
							String method = req.getMethod();
							if(!(method.contentEquals("GET") | method.contentEquals("PUT") | method.contentEquals("DELETE")))
								Respond.sendData(resp, 405, method + " Method Not Allowed", true);
							else if(documents.checkOwner(docid, userid, resp)) {
								request.setAttribute("paths", Paths.SHAREDUSERID);
								request.setAttribute("docid", docid);
								request.setAttribute("shareduserid", shareduserid);
								chain.doFilter(request, response);
							}else
								Respond.sendData(resp, 401, "Unauthorized", true);
						}else 
							Respond.sendData(resp, 400, "Invalid Request!", true);
					}
				}
			}else
				Respond.sendData(resp, 401, "Unauthorized User", true);
		} catch (IOException e) {
			Respond.sendData(resp, 500, "Internal Server Error", true);
		} catch (ServletException e) {
			Respond.sendData(resp, 500, "Internal Server Error", true);
		} catch (SQLException e) {
			Respond.sendData(resp, 500, "Internal Server Error", true);
		} catch(NumberFormatException e) {
			Respond.sendData(resp, 400, "Invalid Request Check Value", true);
		}
	}

	@Override
	public void init(FilterConfig fConfig) throws ServletException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}
}