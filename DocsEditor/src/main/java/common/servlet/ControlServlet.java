package common.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import common.model.Doc;
import common.model.Respond;
import common.model.Versions;
import common.model.SharedUser;
import common.model.Documents;
import common.model.Paths;

public class ControlServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		Documents documents = new Documents();
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		
		if(paths == Paths.NONE) {
			List<Doc> docs = documents.getDocsForUser(userid, resp);
			Respond.sendData(resp, 200, docs);
		} else if(paths == Paths.DOCID) {
			int docid = (int) req.getAttribute("docid");
			Doc doc = documents.getDocForUser(userid, docid, resp);
			if(doc != null)
				Respond.sendData(resp, 200, doc);
			else
				Respond.sendData(resp, 404, "Not found",true);
		} else if(paths == Paths.VERSIONS) {
			int docid = (int) req.getAttribute("docid");
			Versions versions = documents.getDocVersions(docid, resp);
			Respond.sendData(resp, 200, versions);
		} else if(paths == Paths.VERSIONID) {
			int docid = (int) req.getAttribute("docid");
			int versionToViewid = (int) req.getAttribute("versionid");
			String content = documents.getVersionContent(docid, versionToViewid, resp);
			if(content == null)
				Respond.sendData(resp, 404, "Requested versionid not found",true);
			else
				Respond.sendData(resp, 200, content, false);
		} else if(paths == Paths.SHAREDUSERS) {
			int docid = (int) req.getAttribute("docid");
			List<SharedUser> sharedDetails = documents.getSharedUsers(userid, docid, resp);
			Respond.sendData(200, resp, sharedDetails);
		} else if(paths == Paths.SHAREDUSERID) {
			int docid = (int) req.getAttribute("docid");
			int shareduserid = (int) req.getAttribute("shareduserid");
			SharedUser sharedDetail = documents.getSharedUser(userid, docid, shareduserid, resp);
			if(sharedDetail != null)
				Respond.sendData(200, resp, sharedDetail);
			else
				Respond.sendData(resp, 404, "Requested userid not found",true);
		} else
			Respond.sendData(resp, 400, "Invalid Request!", true);
		
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		Documents documents = new Documents();
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		try {
			if(paths == Paths.NONE) {
				String docName = req.getParameter("doc_name");
				if(docName!=null) {
					String fileContent = documents.readInputStreamToString(req.getInputStream(), resp);
					boolean created = documents.createDoc(userid, docName, fileContent, resp);
					if(created)
						Respond.sendData(resp, 200, "Document Created Successfully", false);
					else
						Respond.sendData(resp, 500, "Internal Server Error", true);
				}else
					Respond.sendData(resp, 400, "Invalid Request", true);
			} else if(paths == Paths.SHAREDUSERS) {
				int docid = (int) req.getAttribute("docid");
				String toSendUname = req.getParameter("username");
				String permission = req.getParameter("permission");
				if(!(permission.contentEquals("View") || permission.contentEquals("View_Edit")))
					Respond.sendData(resp, 400, "Invalid Permission", true);
				else {
					if(documents.shareWithUser(toSendUname, docid, permission, resp))
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
		Documents documents = new Documents();
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		
		try {
			if(paths == Paths.DOCID) {
				int docid = (int) req.getAttribute("docid");
				String fileContent = documents.readInputStreamToString(req.getInputStream(), resp);
				boolean updated = documents.updateContentOfDoc(docid, userid, fileContent, resp);
				if(updated)
					Respond.sendData(resp, 200, "Document Updated Successfully", false);
				else
					Respond.sendData(resp, 500, "Internal Server Error", true);
			} else if(paths == Paths.SHAREDUSERID) {
				int docid = (int) req.getAttribute("docid");
				int toSendUserId = (int) req.getAttribute("shareduserid");
				String permission = req.getParameter("permission");
				if(!(permission.contentEquals("View") || permission.contentEquals("View_Edit")))
					Respond.sendData(resp, 400, "Invalid Permission", true);
				else {
					boolean updated = documents.updatePermission(docid, toSendUserId, permission, resp);
					if(updated)
						Respond.sendData(resp, 200, "Permission Updated Successfully", false);
					else
						Respond.sendData(resp, 500, "Internal Server Error", true);
				}
			} else if(paths == Paths.VERSIONID) {
				int docid = (int) req.getAttribute("docid");
				int versionToViewid = (int) req.getAttribute("versionid");
				if(documents.updateVersion(docid, versionToViewid, resp))
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
		Documents documents = new Documents();
		int userid = (int) req.getAttribute("userid");
		Paths paths = (Paths) req.getAttribute("paths");
		
		if(paths == Paths.DOCID) {
			int docid = (int) req.getAttribute("docid");
			if(documents.deleteDocById(docid, userid, resp))
				Respond.sendData(resp, 200, "Document Deleted Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else if(paths == Paths.SHAREDUSERS) {
			int docid = (int) req.getAttribute("docid");
			if(documents.deleteSharedDocs(docid, resp))
				Respond.sendData(resp, 200, "All Shared Documents Removed Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else if(paths == Paths.SHAREDUSERID) {
			int docid = (int) req.getAttribute("docid");
			int shareduserid = (int) req.getAttribute("shareduserid");
			if(documents.deleteSharedDocForUser(shareduserid, docid, resp))
				Respond.sendData(resp, 200, "Removed Successfully", false);
			else
				Respond.sendData(resp, 500, "Internal Server Error", true);
		} else
			Respond.sendData(resp, 400, "Invalid Request!", true);
	}
}