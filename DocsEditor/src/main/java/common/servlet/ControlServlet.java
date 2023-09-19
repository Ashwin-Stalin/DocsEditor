package common.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import common.model.Doc;
import common.model.Respond;
import common.model.Versions;
import common.model.SharedUser;
import common.model.DocsHandling;
import common.model.Paths;

public class ControlServlet extends HttpServlet {
	private final DocsHandling docsHandling = new DocsHandling();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		if(paths == Paths.NONE) {
			List<Doc> docs = docsHandling.getDocsForUser(userid, resp);
			Respond.sendData(resp, 200, docs);
		} else if(paths == Paths.DOCID) {
			String docid = (String) req.getAttribute("docid");
			Doc doc = docsHandling.getDocForUser(userid, docid, resp);
			if(doc != null)
				Respond.sendData(resp, 200, doc);
			else
				Respond.sendData(resp, 404, "Not found",true);
		} else if(paths == Paths.VERSIONS) {
			String docid = (String) req.getAttribute("docid");
			Versions versions = docsHandling.getDocVersions(docid, resp);
			Respond.sendData(resp, 200, versions);
		} else if(paths == Paths.VERSIONID) {
			String docid = (String) req.getAttribute("docid");
			int versionToViewid = (int) req.getAttribute("versionid");
			String content = docsHandling.getVersionContent(docid, versionToViewid, resp);
			if(content == null)
				Respond.sendData(resp, 404, "Requested versionid not found",true);
			else
				Respond.sendData(resp, 200, content, false);
		} else if(paths == Paths.SHAREDUSERS) {
			String docid = (String) req.getAttribute("docid");
			List<SharedUser> sharedDetails = docsHandling.getSharedUsers(userid, docid, resp);
			Respond.sendData(200, resp, sharedDetails);
		} else if(paths == Paths.SHAREDUSERID) {
			String docid = (String) req.getAttribute("docid");
			int shareduserid = (int) req.getAttribute("shareduserid");
			SharedUser sharedDetail = docsHandling.getSharedUser(userid, docid, shareduserid, resp);
			if(sharedDetail != null)
				Respond.sendData(200, resp, sharedDetail);
			else
				Respond.sendData(resp, 404, "Requested userid not found",true);
		} else
			Respond.sendData(resp, 400, "Invalid Request!", true);
		
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		try {
			if(paths == Paths.NONE) {
				String docName = req.getParameter("doc_name");
				if(docName!=null) {
					String fileContent = docsHandling.readInputStreamToString(req.getInputStream(), resp);
					boolean created = docsHandling.createDoc(userid, docName, fileContent, resp);
					if(created)
						Respond.sendData(resp, 200, "Document Created Successfully", false);
					else
						Respond.sendData(resp, 500, "Internal Server Error", true);
				}else
					Respond.sendData(resp, 400, "Invalid Request", true);
			} else if(paths == Paths.SHAREDUSERS) {
				String docid = (String) req.getAttribute("docid");
				String toSendUname = req.getParameter("username");
				String permission = req.getParameter("permission");
				if(!(permission.contentEquals("View") || permission.contentEquals("View_Edit")))
					Respond.sendData(resp, 400, "Invalid Permission", true);
				else {
					if(docsHandling.shareWithUser(toSendUname, docid, permission, resp))
						Respond.sendData(resp, 200, "Document Shared Successfully", false);
					else
						Respond.sendData(resp, 500, "Internal Server Error", true);
				}
			} else
				Respond.sendData(resp, 400, "Invalid Request!", true);
		} catch(IOException e) {
			Respond.sendData(resp, 500, "Internal Server Error", true);
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		try {
			if(paths == Paths.DOCID) {
				String docid = (String) req.getAttribute("docid");
				String fileContent = docsHandling.readInputStreamToString(req.getInputStream(), resp);
				boolean updated = docsHandling.updateContentOfDoc(docid, userid, fileContent, resp);
				if(updated)
					Respond.sendData(resp, 200, "Document Updated Successfully", false);
				else
					Respond.sendData(resp, 500, "Internal Server Error", true);
			} else if(paths == Paths.SHAREDUSERID) {
				String docid = (String) req.getAttribute("docid");
				int toSendUserId = (int) req.getAttribute("shareduserid");
				String permission = req.getParameter("permission");
				if(!(permission.contentEquals("View") || permission.contentEquals("View_Edit")))
					Respond.sendData(resp, 400, "Invalid Permission", true);
				else {
					boolean updated = docsHandling.updatePermission(docid, toSendUserId, permission, resp);
					if(updated)
						Respond.sendData(resp, 200, "Permission Updated Successfully", false);
					else
						Respond.sendData(resp, 500, "Internal Server Error", true);
				}
			} else if(paths == Paths.VERSIONID) {
				String docid = (String) req.getAttribute("docid");
				int versionToViewid = (int) req.getAttribute("versionid");
				if(docsHandling.updateVersion(docid, versionToViewid, resp))
					Respond.sendData(resp, 200, "Current Version Updated Successfully", false);
				else
					Respond.sendData(resp, 500, "Internal Server Error", true);
			} else
				Respond.sendData(resp, 400, "Invalid Request!", true);
		} catch(IOException e) {
			Respond.sendData(resp, 500, "Internal Server Error", true);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		if(paths == Paths.DOCID) {
			String docid = (String) req.getAttribute("docid");
			if(docsHandling.deleteDocById(docid, userid, resp))
				Respond.sendData(resp, 200, "Document Deleted Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else if(paths == Paths.SHAREDUSERS) {
			String docid = (String) req.getAttribute("docid");
			if(docsHandling.deleteSharedDocs(docid, resp))
				Respond.sendData(resp, 200, "All Shared Documents Removed Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else if(paths == Paths.SHAREDUSERID) {
			String docid = (String) req.getAttribute("docid");
			int shareduserid = (int) req.getAttribute("shareduserid");
			if(docsHandling.deleteSharedDocForUser(shareduserid, docid, resp))
				Respond.sendData(resp, 200, "Removed Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else
			Respond.sendData(resp, 400, "Invalid Request!", true);
	}
}