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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;
import common.model.diff_match_patch;

public class Undo extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null) 
				resp.sendRedirect("login-page");

			int docid = Integer.parseInt(req.getParameter("doc_id"));
			// Retrieving current version content by joining document and versions table
			PreparedStatement ps = connection.prepareStatement("select versionid, content from document join versions on document.currentversion=versions.versionid where document.docid=? limit 1");
			ps.setInt(1, docid);
			ResultSet res = ps.executeQuery();
			if (res.next()) {
				int currentVersionid = res.getInt("versionid");
				InputStream content = res.getBinaryStream("content");
				String currentContent = readInputStreamToString(content);
				// Retrieving previous version content form versions table
				PreparedStatement preparedStatement = connection.prepareStatement("select versionid,content from versions where docid=? and versionid<? order by versionid desc limit 1");
				preparedStatement.setInt(1, docid);
				preparedStatement.setInt(2, currentVersionid);
				ResultSet rs = preparedStatement.executeQuery();
				if (rs.next()) {
					diff_match_patch dmp = new diff_match_patch();
					int previousVersionid = rs.getInt("versionid");
					InputStream c = rs.getBinaryStream("content");
					String previousContent = readInputStreamToString(c);

					LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(previousContent);
					Object[] results = dmp.patch_apply(patches, currentContent);
					String patchedText = (String) results[0];
					InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());

					LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(patchedText, currentContent, false);
					dmp.diff_cleanupSemantic(diffs);
					String patch = dmp.patch_toText(dmp.patch_make(diffs));
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					// Updating versions and document table (for undo functionality)
					PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
					pS.setBinaryStream(1, retrievedText);
					pS.setInt(2, previousVersionid);
					pS.setBinaryStream(3, patchContent);
					pS.setInt(4, currentVersionid);
					pS.setInt(5, previousVersionid);
					pS.setInt(6, docid);
					pS.executeUpdate();
				}
				resp.sendRedirect("open?doc_id=" + docid);
			}
		} catch (IOException e) {
			System.out.println("Catched IO Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
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
