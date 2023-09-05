package common.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;
import common.model.diff_match_patch;

public class Save extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");

			int userid = (int) session.getAttribute("userid");
			int docid = Integer.parseInt(req.getParameter("doc_id"));
			String newContent = req.getParameter("textToSave");
			InputStream newfileContent = new ByteArrayInputStream(newContent.getBytes());
			// Retrieving content of current version by joining document and versions table
			PreparedStatement ps = connection.prepareStatement("select versionid, content from document join versions on document.currentversion=versions.versionid where document.docid=?");
			ps.setInt(1, docid);
			ResultSet res = ps.executeQuery();
			out.println("<html>");
			out.println("<body>");
			if (res.next()) {
				int versionid = res.getInt("versionid");
				InputStream content = res.getBinaryStream("content");
				String previousContent = readInputStreamToString(content);
				// Checking both the current version content and new content to save are same
				if (newContent.contentEquals(previousContent))
					out.println("Nothing to Save");
				else {
					// Inserting new content into version table and returning versionid
					PreparedStatement preparedStatement = connection.prepareStatement("insert into versions(docid, content, editeduserid) values(?,?,?) returning versionid");
					preparedStatement.setInt(1, docid);
					preparedStatement.setBinaryStream(2, newfileContent, newfileContent.available());
					preparedStatement.setInt(3, userid);
					ResultSet rs = preparedStatement.executeQuery();
					if (rs.next()) {
						int newversionid = rs.getInt("versionid");
						// Updating the version id to document table
						preparedStatement = connection.prepareStatement("update document set currentversion=? where docid=?; insert into versionmapping(docid, fromversion, toversion) values(?,?,?);");
						preparedStatement.setInt(1, newversionid);
						preparedStatement.setInt(2, docid);
						preparedStatement.setInt(3, docid);
						preparedStatement.setInt(4, versionid);
						preparedStatement.setInt(5, newversionid);
						preparedStatement.executeUpdate();
					}

					diff_match_patch dmp = new diff_match_patch();
					LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(newContent, previousContent, false);
					dmp.diff_cleanupSemantic(diffs);
					String patch = dmp.patch_toText(dmp.patch_make(diffs));
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					// Now that current version is updated ,here updating the versions table's previous version's content for diff
					preparedStatement = connection.prepareStatement("update versions set content=? where versionid=?");
					preparedStatement.setBinaryStream(1, patchContent);
					preparedStatement.setInt(2, versionid);
					preparedStatement.executeUpdate();
					String scriptTag = "<script>document.querySelector('#savedsuccess').style=\"color: blue;\";</script> ";
					req.setAttribute("savedsuccess", scriptTag);
					req.getRequestDispatcher("open?doc_id"+docid).include(req, resp);
				}
			}
			out.println("</body>");
			out.println("</html");
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		} catch (ServletException e) {
			System.out.println("Catched Servlet Exception " + e.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendRedirect("home");
		} catch (IOException e) {
			System.out.println("Catch IO Exception : " + e.getMessage());
		}
	}

	private String readInputStreamToString(InputStream inputStream) {
		StringBuilder sb = new StringBuilder();
		try {
			for (int ch; (ch = inputStream.read()) != -1;)
				sb.append((char) ch);
		} catch(IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage() );
		}
		return sb.toString();
	}
}
