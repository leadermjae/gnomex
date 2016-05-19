package hci.gnomex.controller;

import hci.gnomex.model.PropertyDictionary;
import hci.gnomex.utility.HibernateSession;
import hci.gnomex.utility.JspHelper;
import hci.gnomex.utility.PropertyDictionaryHelper;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by jholmberg on 5/18/2016.
 */
public class ResetPasswordServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(ResetPasswordServlet.class);
  private static final String TARGET_JSP = "/WEB-INF/reset_password.jsp";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String message = (String) ((request.getAttribute("message") != null)?request.getAttribute("message"):"");
    Integer coreToPassThru = JspHelper.getIdCoreFacility(request);
    String idCoreParm = coreToPassThru == null?"":("?idCore=" + coreToPassThru.toString());
    boolean showUserSignup = true;

    // We can't obtain a hibernate session unless webcontextpath is initialized.  See HibernateSession.
    String webContextPath = getServletConfig().getServletContext().getRealPath("/");
    GNomExFrontController.setWebContextPath(webContextPath);

    boolean showCampusInfoLink = false;
    String siteLogo = "";
    Session sess = null;
    try {
      sess = HibernateSession.currentSession("guest");
      PropertyDictionary propUniversityUserAuth = (PropertyDictionary)sess.createQuery("from PropertyDictionary p where p.propertyName='" + PropertyDictionary.UNIVERSITY_USER_AUTHENTICATION + "'").uniqueResult();
      if (propUniversityUserAuth != null && propUniversityUserAuth.getPropertyValue() != null && propUniversityUserAuth.getPropertyValue().equals("Y")) {
        showCampusInfoLink = true;
      }

      // Determine if user sign up screen is enabled
      PropertyDictionary disableUserSignup = (PropertyDictionary)sess.createQuery("from PropertyDictionary p where p.propertyName='" + PropertyDictionary.DISABLE_USER_SIGNUP + "'").uniqueResult();
      if (disableUserSignup != null && disableUserSignup.getPropertyValue().equals("Y")) {
        showUserSignup = false;
      }

      // Get site specific log
      siteLogo = PropertyDictionaryHelper.getSiteLogo(sess, coreToPassThru);

      LOG.debug("Setting all attributes on request");
      request.setAttribute("message",message);
      request.setAttribute("idCoreParm",idCoreParm);
      request.setAttribute("showUserSignup",showUserSignup);
      request.setAttribute("showCampusInfoLink",showCampusInfoLink);
      request.setAttribute("siteLogo",siteLogo);


    } catch (Exception e){
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
}
