package hci.gnomex.controller;

import hci.gnomex.model.Analysis;
import hci.gnomex.model.DataTrack;
import hci.gnomex.model.PropertyDictionary;
import hci.gnomex.model.Request;
import hci.gnomex.model.Topic;
import hci.gnomex.model.Visibility;
import hci.gnomex.utility.HibernateSession;
import hci.gnomex.utility.JspHelper;
import hci.gnomex.utility.PropertyDictionaryHelper;
import hci.gnomex.utility.TopicQuery;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by jholmberg on 5/12/2016.
 */
public class LoginServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(LoginServlet.class);
  private static final String TARGET_JSP = "/WEB-INF/login.jsp";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    LOG.debug("In goGet.");
    String message = (String) ((request.getAttribute("message") != null) ? request.getAttribute("message") : "");
    String errFlag = (String) ((request.getParameter("err") != null) ? request.getParameter("err") : "N");
    Integer idCoreFacility = JspHelper.getIdCoreFacility(request);
    String idCoreParm = idCoreFacility == null ? "" : ("?idCore=" + idCoreFacility.toString());

    // We can't obtain a hibernate session unless webcontextpath is initialized.  See HibernateSession.
    String webContextPath = getServletConfig().getServletContext().getRealPath("/");
    GNomExFrontController.setWebContextPath(webContextPath);

    boolean showCampusInfoLink = false;
    boolean itemNotPublic = false;

    boolean isPublicExperiment = false;
    boolean isPublicAnalysis = false;
    boolean isPublicDataTrack = false;
    boolean isPublicTopic = false;

    boolean showUserSignup = true;
    boolean allowGuest = false;
    String itemType = "";
    String siteLogo = "";

    Session sess = null;

    try {
      sess = HibernateSession.currentReadOnlySession("guest");
      LOG.debug("Loading properties from PropertyDictionary");
      PropertyDictionary propUniversityUserAuth = (PropertyDictionary) sess.createQuery("from PropertyDictionary p where p.propertyName='" + PropertyDictionary.UNIVERSITY_USER_AUTHENTICATION + "'").uniqueResult();
      if (propUniversityUserAuth != null && propUniversityUserAuth.getPropertyValue() != null && propUniversityUserAuth.getPropertyValue().equals("Y")) {
        showCampusInfoLink = true;
      }
      LOG.debug("Show campus info link: "+ showCampusInfoLink);

      // Get site specific logo
      siteLogo = PropertyDictionaryHelper.getSiteLogo(sess, idCoreFacility);
      LOG.debug("Site specific logo: " + siteLogo);

      // Determine if user sign up screen is enabled
      PropertyDictionary disableUserSignup = (PropertyDictionary) sess.createQuery("from PropertyDictionary p where p.propertyName='" + PropertyDictionary.DISABLE_USER_SIGNUP + "'").uniqueResult();
      if (disableUserSignup != null && disableUserSignup.getPropertyValue().equals("Y")) {
        showUserSignup = false;
      }
      LOG.debug("Show user signup: " + showUserSignup);

      // Determine if guest access is allowed
      PropertyDictionary noGuestAccess = (PropertyDictionary) sess.createQuery("from PropertyDictionary p where p.propertyName='" + PropertyDictionary.NO_GUEST_ACCESS + "'").uniqueResult();
      if (noGuestAccess == null) {
        allowGuest = true;
      }
      LOG.debug("Allow guest access:  " + allowGuest);
      // If launching experiment, analysis, data track, or topic then check for public
      // If public then launch directly as guest user without requiring login
      String requestNumber = (String) ((request.getParameter("requestNumber") != null) ? request.getParameter("requestNumber") : "");
      if (requestNumber.length() > 0) {
        LOG.debug("Requested request number: " + requestNumber);
        Request experiment = GetRequest.getRequestFromRequestNumber(sess, requestNumber);
        if (experiment != null && experiment.getCodeVisibility().equals(Visibility.VISIBLE_TO_PUBLIC)) {
          LOG.debug("Public experiment: " + experiment.getName() + " for request number: " + requestNumber + " found");
          // Add request number to request
          request.setAttribute("requestNumber",requestNumber);
          isPublicExperiment = true;
        } else {
          LOG.debug("No public experiment for request number: " + requestNumber + " found. ");
          itemNotPublic = true;
          itemType = "Experiment";
        }
      } else {
        String analysisNumber = (String) ((request.getParameter("analysisNumber") != null) ? request.getParameter("analysisNumber") : "");
        if (analysisNumber.length() > 0) {
          Analysis analysis = GetAnalysis.getAnalysisFromAnalysisNumber(sess, analysisNumber);
          if (analysis != null && analysis.getCodeVisibility().equals(Visibility.VISIBLE_TO_PUBLIC)) {
            LOG.debug("Public analysis: " + analysis.getName() + " for request number: " + analysisNumber + " found");
            request.setAttribute("analysisNumber",analysisNumber);
            isPublicAnalysis = true;
          } else {
            LOG.debug("No public analysis for analysis number: " + requestNumber + " found. ");
            itemNotPublic = true;
            itemType = "Analysis";
          }
        } else {
          String dataTrackNumber = (String) ((request.getParameter("dataTrackNumber") != null) ? request.getParameter("dataTrackNumber") : "");
          if (dataTrackNumber.length() > 0) {
            DataTrack dt = GetDataTrack.getDataTrackFromDataTrackNumber(sess, dataTrackNumber);
            if (dt != null && dt.getCodeVisibility().equals(Visibility.VISIBLE_TO_PUBLIC)) {
              LOG.debug("Public analysis: " + dt.getName() + " for request number: " + dataTrackNumber + " found");
              // If public data track then skip login screen and launch directly as guest user
              request.setAttribute("dataTrackNumber",dataTrackNumber);
              isPublicDataTrack = true;
            } else {
              LOG.debug("No public datatrack for datatrack number: " + dataTrackNumber + " found. ");
              itemNotPublic = true;
              itemType = "Data Track";
            }
          } else {
            String topicNumber = (String) ((request.getParameter("topicNumber") != null) ? request.getParameter("topicNumber") : "");
            if (topicNumber.length() > 0) {
              Topic t = TopicQuery.getTopicFromTopicNumber(sess, topicNumber);
              if (t != null && t.getCodeVisibility().equals(Visibility.VISIBLE_TO_PUBLIC)) {
                LOG.debug("Public topic: " + t.getName() + " for request number: " + topicNumber + " found");
                // If public topic then skip login screen and launch directly as guest user
                request.setAttribute("topicNumber",topicNumber);
                isPublicTopic = true;
              } else {
                LOG.debug("No public topic for topic number: " + topicNumber + " found. ");
                itemNotPublic = true;
                itemType = "Topic";
              }
            }
          }
        }
      }
      LOG.debug("Setting all attributes on the request.");
      request.setAttribute("message",message);
      request.setAttribute("errFlag",errFlag);
      request.setAttribute("idCoreParm",idCoreParm);
      request.setAttribute("showCampusInfoLink",showCampusInfoLink);
      request.setAttribute("itemNotPublic",itemNotPublic);
      request.setAttribute("isPublicExperiment",isPublicExperiment);
      request.setAttribute("isPublicAnalysis",isPublicAnalysis);
      request.setAttribute("isPublicDataTrack",isPublicDataTrack);
      request.setAttribute("isPublicTopic",isPublicTopic);
      request.setAttribute("showUserSignup",showUserSignup);
      request.setAttribute("allowGuest",allowGuest);
      request.setAttribute("itemType",itemType);
      request.setAttribute("siteLogo",siteLogo);
    } catch (Exception e) {
      message = "Cannot obtain property " + PropertyDictionary.UNIVERSITY_USER_AUTHENTICATION + " " + e.toString() + " sess=" + sess;
      LOG.error(message,e);
    } finally {
      try {
        HibernateSession.closeSession();
      } catch (Exception e) {
        LOG.error("Error closing Hibernate session.");
      }
    }
    LOG.debug("Dispatching to " + TARGET_JSP);
    request.getRequestDispatcher(TARGET_JSP).forward(request,response);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("In doPost, delegating to doGet.");
    doGet(req,resp);
  }

  @Override
  public void init() throws ServletException {
    LOG.debug("Initializing " + this.getClass().getSimpleName());
  }
}
