package common.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import common.db.Database;
import common.model.diff_match_patch;

public class ChangeVersion extends HttpServlet {
	private static Connection connection = null;
	private final static Database db = Database.getInstance();

	@Override
	public void init() {
		connection = db.getConnection();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (PrintWriter out = resp.getWriter()) {
			HttpSession session = req.getSession(false);
			if (session == null)
				resp.sendRedirect("login-page");
			
			int docid = Integer.parseInt(req.getParameter("doc_id"));
			int currentVersion = Integer.parseInt(req.getParameter("current_version_id"));
			int versionToChange = Integer.parseInt(req.getParameter("version_id_to_change"));
			
			List<Integer> sourceVersionIds = new ArrayList<>();
			List<Integer> destinationVersionIds = new ArrayList<>();
			int versionToAdd = currentVersion;
			sourceVersionIds.add(versionToAdd);
			while(true) {
				PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, docid);
				ResultSet rs = prepareStatement.executeQuery();
				if(rs.next()) {
					int version = rs.getInt("fromversion");
					sourceVersionIds.add(version);
					versionToAdd = version;
				}else
					break;
			}
			versionToAdd = versionToChange;
			destinationVersionIds.add(versionToAdd);
			while(true) {
				PreparedStatement prepareStatement = connection.prepareStatement("select fromversion from versionmapping where toversion=? and docid=?");
				prepareStatement.setInt(1, versionToAdd);
				prepareStatement.setInt(2, docid);
				ResultSet rs = prepareStatement.executeQuery();
				if(rs.next()) {
					int version = rs.getInt("fromversion");
					destinationVersionIds.add(version);
					versionToAdd = version;
				}else
					break;
			}
			System.out.println("Changing to version " + versionToChange + " From " + currentVersion);
			System.out.println("Source versions "  + sourceVersionIds.toString());
			System.out.println("Destination versions "  + destinationVersionIds.toString());
			int removedVersionId = 0 ;
			while(true) {
				if(sourceVersionIds.size() == 0 || destinationVersionIds.size() == 0) {
					if(removedVersionId!=0)
						sourceVersionIds.add(removedVersionId);
					break;
				}
				if(sourceVersionIds.get(sourceVersionIds.size() - 1).equals(destinationVersionIds.get(destinationVersionIds.size()-1))) {
					removedVersionId = sourceVersionIds.get(sourceVersionIds.size()-1);
					sourceVersionIds.remove(sourceVersionIds.size()-1);
					destinationVersionIds.remove(destinationVersionIds.size()-1);
				}else {
					if(removedVersionId != 0)
						sourceVersionIds.add(removedVersionId);
					break;
				}
			}
			
			System.out.println("Changing to version " + versionToChange + " From " + currentVersion);
			System.out.println("Source versions "  + sourceVersionIds.toString());
			System.out.println("Destination versions "  + destinationVersionIds.toString());
			
			int cV = currentVersion;
			for(Integer versionid : sourceVersionIds) {
				if(versionid == cV)
					continue;
				PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as previousContent from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");
				preparedStatement.setInt(1, cV);
				preparedStatement.setInt(2, versionid);
				preparedStatement.setInt(3, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					diff_match_patch dmp = new diff_match_patch();
					InputStream c = rs.getBinaryStream("currentContent");
					InputStream p = rs.getBinaryStream("previousContent");
					String currentContent = readInputStreamToString(c);
					String previousContent = readInputStreamToString(p);
					
					System.out.println("Current Content : " + currentContent);
					System.out.println("Previous Content : " + previousContent);
					
					LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(previousContent);
					Object[] results = dmp.patch_apply(patches, currentContent);
					String patchedText = (String) results[0];
					InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());
					
					LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(patchedText, currentContent, false);
					dmp.diff_cleanupSemantic(diffs);
					String patch = dmp.patch_toText(dmp.patch_make(diffs));
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					
					PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
					pS.setBinaryStream(1, retrievedText);
					pS.setInt(2, versionid);
					pS.setBinaryStream(3, patchContent);
					pS.setInt(4, cV);
					pS.setInt(5, versionid);
					pS.setInt(6, docid);
					pS.executeUpdate();
					cV = versionid;
				}
			}
			System.out.println("Redo Called");
			for(int i = destinationVersionIds.size()-1; i >= 0; i--){
				PreparedStatement preparedStatement = connection.prepareStatement("select c.content as currentContent, p.content as afterContent from versions as c join versions as p on c.docid=p.docid where c.versionid=? and p.versionid=? and c.docid=?");
				preparedStatement.setInt(1, cV);
				preparedStatement.setInt(2, destinationVersionIds.get(i));
				preparedStatement.setInt(3, docid);
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()) {
					diff_match_patch dmp = new diff_match_patch();
					InputStream c = rs.getBinaryStream("currentContent");
					InputStream a = rs.getBinaryStream("afterContent");
					String currentContent = readInputStreamToString(c);
					String afterContent = readInputStreamToString(a);
					
					System.out.println("Current Content : " + currentContent);
					System.out.println("After Content : " + afterContent);
					
					LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(afterContent);
					Object[] results = dmp.patch_apply(patches, currentContent);
					String patchedText = (String) results[0];
					InputStream retrievedText = new ByteArrayInputStream(patchedText.getBytes());

					LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(patchedText, currentContent, false);
					dmp.diff_cleanupSemantic(diffs);
					String patch = dmp.patch_toText(dmp.patch_make(diffs));
					InputStream patchContent = new ByteArrayInputStream(patch.getBytes());
					
					System.out.println(patchedText);
					System.out.println(patch);
					
					PreparedStatement pS = connection.prepareStatement("update versions set content=? where versionid=?;update versions set content=? where versionid=?;update document set currentversion=? where docid=?");
					pS.setBinaryStream(1, retrievedText);
					pS.setInt(2, destinationVersionIds.get(i));
					pS.setBinaryStream(3, patchContent);
					pS.setInt(4, cV);
					pS.setInt(5, destinationVersionIds.get(i));
					pS.setInt(6, docid);
					pS.executeUpdate();
					cV = destinationVersionIds.get(i);
				}
	        }
			resp.sendRedirect("open?doc_id=" + docid);
		} catch (IOException e) {
			System.out.println("Catched IO Exception : " + e.getMessage());
		} catch (NumberFormatException e) {
			System.out.println("Catched NumberFormatException Exception : " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception : " + e.getMessage());
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
