package hci.gnomex.controller;

import hci.gnomex.model.PropertyDictionary;
import hci.gnomex.utility.HibernateSession;
import hci.gnomex.utility.PropertyDictionaryHelper;
import hci.gnomex.utility.ServletUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;

public class UploadExperimentURLServlet extends HttpServlet {
private static Logger LOG = Logger.getLogger(UploadExperimentURLServlet.class);

protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

	// Restrict commands to local host if request is not secure
	if (!ServletUtil.checkSecureRequest(req)) {
		ServletUtil.reportServletError(res, "Secure connection is required. Prefix your request with 'https'");
		return;
	}

	Session sess = null;

	try {

		boolean useSecureDownload = !req.getServerName().equalsIgnoreCase("localhost")
				&& !req.getServerName().equals("127.0.0.1") && !req.getServerName().equals("h005973");
		useSecureDownload = false; // https doesn't work
		//
		// COMMENTED OUT CODE:
		// String baseURL = "http"+ (isLocalHost ? "://" : "s://") + req.getServerName() + req.getContextPath();
		//
		// To fix upload problem (missing session in upload servlet for FireFox, Safari), encode session in URL
		// for upload servlet. Also, use non-secure (http: rather than https:) when making http request;
		// otherwise, existing session is not accessible to upload servlet.
		//
		//

		sess = HibernateSession.currentReadOnlySession(req.getUserPrincipal().getName());
		String portNumber = PropertyDictionaryHelper.getInstance(sess).getQualifiedProperty(
				PropertyDictionary.HTTP_PORT, req.getServerName());
		if (portNumber == null) {
			portNumber = "";
		} else {
			portNumber = ":" + portNumber;
		}

		String baseURL = "http" + (useSecureDownload ? "s" : "") + "://" + req.getServerName() + portNumber
				+ req.getContextPath();
		String URL = baseURL + "/" + "UploadExperimentFileServlet.gx";
		// Encode session id in URL so that session maintains for upload servlet when called from
		// Flex upload component inside FireFox, Safari
		URL += ";jsessionid=" + req.getRequestedSessionId();

		res.setContentType("application/xml");
		res.getOutputStream().println("<UploadExperimentURL url='" + URL + "'/>");

	} catch (Exception e) {
		LOG.error("An exception has occurred in UploadExperimentURLServlet ", e);
	} finally {
		if (sess != null) {
			try {
				HibernateSession.closeSession();
			} catch (Exception e) {
				LOG.error("An exception has occurred in UploadExperimentURLServlet ", e);
			}
		}
	}
}
}
