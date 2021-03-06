package hci.gnomex.controller;

import hci.dictionary.model.NullDictionaryEntry;
import hci.dictionary.utility.DictionaryManager;
import hci.framework.control.Command;
import hci.framework.control.RollBackCommandException;
import hci.framework.model.DetailObject;
import hci.gnomex.model.AppUser;
import hci.gnomex.model.CoreFacility;
import hci.gnomex.security.SecurityAdvisor;
import hci.gnomex.utility.PropertyDictionaryHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class GetAppUser extends GNomExCommand implements Serializable {

// the static field for logging in Log4J
private static Logger LOG = Logger.getLogger(GetAppUser.class);

private AppUser appUser;

public void validate() {
}

public void loadCommand(HttpServletRequest request, HttpSession session) {

	appUser = new AppUser();
	HashMap errors = this.loadDetailObject(request, appUser);
	this.addInvalidFields(errors);

	if (appUser.getIdAppUser() == null) {
		this.addInvalidField("idAppUser required", "idAppUser required");
	}

	if (isValid()) {
		setResponsePage(this.SUCCESS_JSP);
	} else {
		setResponsePage(this.ERROR_JSP);
	}

}

public Command execute() throws RollBackCommandException {

	try {

		Session sess = this.getSecAdvisor().getReadOnlyHibernateSession(this.getUsername(), "GetAppUser");

		AppUser theAppUser = (AppUser) sess.get(AppUser.class, appUser.getIdAppUser());

		if (this.getSecAdvisor().hasPermission(SecurityAdvisor.CAN_ADMINISTER_USERS)) {
			Hibernate.initialize(theAppUser.getLabs());
			Hibernate.initialize(theAppUser.getCollaboratingLabs());
			Hibernate.initialize(theAppUser.getManagingLabs());

			theAppUser.excludeMethodFromXML("getPasswordExternal");

			Document doc = new Document(new Element("OpenAppUserList"));
			doc.getRootElement().addContent(
					theAppUser.toXMLDocument(null, DetailObject.DATE_OUTPUT_SQL).getRootElement());

			getCoreFacilities(sess, doc, theAppUser);
			getCoreFacilitiesICanSubmitTo(sess, doc, theAppUser);

			XMLOutputter out = new org.jdom.output.XMLOutputter();
			this.xmlResult = out.outputString(doc);

			setResponsePage(this.SUCCESS_JSP);

		} else {
			this.addInvalidField("insufficient permission", "Insufficient permission to access user details");
		}
	} catch (Exception e) {
		LOG.error("An exception has occurred in GetAppUser ", e);
		throw new RollBackCommandException(e.getMessage());
	}
	/*
	 * finally { try { //closeReadOnlyHibernateSession; } catch(Exception e) { LOG.error("An exception has occurred in GetAppUser ", e); }
	 * }
	 */
	if (isValid()) {
		setResponsePage(this.SUCCESS_JSP);
	} else {
		setResponsePage(this.ERROR_JSP);
	}

	return this;
}

private void getCoreFacilities(Session sess, Document doc, AppUser theAppUser) {
	Element facilitiesNode = new Element("managingCoreFacilities");
	doc.getRootElement().addContent(facilitiesNode);
	for (Iterator coreIter = DictionaryManager.getDictionaryEntries("hci.gnomex.model.CoreFacility").iterator(); coreIter
			.hasNext();) {
		Object de = coreIter.next();
		if (de instanceof NullDictionaryEntry) {
			continue;
		}
		CoreFacility facility = (CoreFacility) de;
		String selected = "N";
		for (Iterator userIter = theAppUser.getManagingCoreFacilities().iterator(); userIter.hasNext();) {
			CoreFacility userFacility = (CoreFacility) userIter.next();
			if (userFacility.getIdCoreFacility().equals(facility.getIdCoreFacility())) {
				selected = "Y";
				break;
			}
		}
		if (selected.equals("N") && (facility.getIsActive() == null || facility.getIsActive().equals("N"))) {
			continue;
		}

		String name = facility.getFacilityName();
		if (facility.getIsActive() == null || facility.getIsActive().equals("N")) {
			name += " (inactive)";
		}
		Element facilityNode = new Element("coreFacility");
		facilitiesNode.addContent(facilityNode);
		facilityNode.setAttribute("value", facility.getIdCoreFacility().toString());
		facilityNode.setAttribute("display", name);
		facilityNode.setAttribute("selected", selected);
	}
}

private void getCoreFacilitiesICanSubmitTo(Session sess, Document doc, AppUser theAppUser) {
	Element facilitiesNode = new Element("coreFacilitiesICanSubmitTo");
	doc.getRootElement().addContent(facilitiesNode);
	PropertyDictionaryHelper pdh = PropertyDictionaryHelper.getInstance(sess);

	Set coresToCheck = new TreeSet();
	if (this.getSecAdvisor().hasPermission(SecurityAdvisor.CAN_ADMINISTER_ALL_CORE_FACILITIES)) {
		coresToCheck = DictionaryManager.getDictionaryEntries("hci.gnomex.model.CoreFacility");
	} else {
		coresToCheck = this.getSecAdvisor().getCoreFacilitiesIManage();
	}

	for (Iterator coreIter = coresToCheck.iterator(); coreIter.hasNext();) {
		Object de = coreIter.next();
		if (de instanceof NullDictionaryEntry) {
			continue;
		}
		CoreFacility facility = (CoreFacility) de;
		String selected = "N";
		String allowed = "N";
		if (this.getSecAdvisor().coreAllowsGlobalSubmission(facility.getIdCoreFacility())) {
			allowed = "Y";
			for (Iterator userIter = theAppUser.getCoreFacilitiesICanSubmitTo().iterator(); userIter.hasNext();) {
				CoreFacility userFacility = (CoreFacility) userIter.next();
				if (userFacility.getIdCoreFacility().equals(facility.getIdCoreFacility())) {
					selected = "Y";
					break;
				}
			}
		}

		if (selected.equals("N") && (facility.getIsActive() == null || facility.getIsActive().equals("N"))) {
			continue;
		}

		String name = facility.getFacilityName();
		if (facility.getIsActive() == null || facility.getIsActive().equals("N")) {
			name += " (inactive)";
		}
		Element facilityNode = new Element("coreFacility");
		facilitiesNode.addContent(facilityNode);
		facilityNode.setAttribute("value", facility.getIdCoreFacility().toString());
		facilityNode.setAttribute("display", name);
		facilityNode.setAttribute("selected", selected);
		facilityNode.setAttribute("allowed", allowed);
	}
}
}