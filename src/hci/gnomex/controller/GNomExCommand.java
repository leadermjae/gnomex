
package hci.gnomex.controller;

import hci.framework.control.Command;
import hci.gnomex.constants.Constants;
import hci.gnomex.model.Analysis;
import hci.gnomex.model.DataTrack;
import hci.gnomex.model.FlowCell;
import hci.gnomex.model.Invoice;
import hci.gnomex.model.Notification;
import hci.gnomex.model.PropertyDictionary;
import hci.gnomex.model.Request;
import hci.gnomex.model.RequestCategory;
import hci.gnomex.model.Topic;
import hci.gnomex.security.SecurityAdvisor;
import hci.gnomex.utility.PropertyDictionaryHelper;

import java.io.Serializable;
import java.util.List;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

/**
 *
 *@author
 *@created
 *@version    1.0
 * Generated by the CommandBuilder tool - Kirt Henrie
 */

public abstract class GNomExCommand extends Command implements Serializable {


  // put any instance variables here (usually the DetailObjects used by this command)
  protected String xmlResult = "<SUCCESS/>";

  public String SUCCESS_JSP = "/getXML.jsp";
  public String ERROR_JSP = "/message.jsp";
  protected String rowSaveCMD;
  protected String rowDeleteCMD;

  /**  Constructor for the Command object */
  public GNomExCommand() {
  }

  protected void setRowCommands(HttpServletRequest request) {
    String URI = request.getRequestURI();
    if (URI.lastIndexOf("/") > 0) {
      URI = URI.substring(0, URI.lastIndexOf("/"));
    }
    this.rowDeleteCMD = URI + "/DeleteCODGridRow.aw";
    this.rowSaveCMD = URI + "/SaveCODGridRow.aw";
  }

  /**
   *  The callback method allowing you to manipulate the HttpServletRequest
   *  prior to forwarding to the response JSP. This can be used to put the
   *  results from the execute method into the request object for display in the
   *  JSP.
   *
   *@param  request  The new requestState value
   *@return          Description of the Return Value
   */
  public HttpServletRequest setRequestState(HttpServletRequest request) {
    // load any result objects into request attributes, keyed by the useBean id in the jsp
    request.setAttribute("xmlResult",this.xmlResult);

    // Garbage collect
    this.xmlResult = null;
    System.gc();

    return request;
  }

  /**
   *  The callback method called after the loadCommand, and execute methods,
   *  this method allows you to manipulate the HttpServletResponse object prior
   *  to forwarding to the result JSP (add a cookie, etc.)
   *
   *@param  request  The HttpServletResponse for the command
   *@return          The processed response
   */
  public HttpServletResponse setResponseState(HttpServletResponse response) {
    response.setHeader("Cache-Control", "max-age=0, must-revalidate");
    return response;
  }

  /**
   *  The callback method called after the loadCommand and execute methods
   *  allowing you to do any post-execute processing of the HttpSession. Should
   *  be used to add/remove session data resulting from the execution of this
   *  command
   *
   *@param  session  The HttpSession
   *@return          The processed HttpSession
   */
  public HttpSession setSessionState(HttpSession session) {
    return session;
  }

  public SecurityAdvisor getSecAdvisor() {
    return (SecurityAdvisor)this.getSecurityAdvisor();
  }

  public static String getRemoteIP(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null) {
      return xff.split("[\\s,]+")[0];
    }
    return request.getRemoteAddr();
  }

  public String getAppURL(HttpServletRequest request) throws Exception {
    String requestURL = request.getRequestURL().toString();
    String appURL = requestURL.substring(0, requestURL.indexOf("/gnomex"));
    appURL += ":" + request.getServerPort() + "/gnomex";

    return appURL;    
  }

  public String getLaunchAppURL(HttpServletRequest request) throws Exception {
    String url = getAppURL(request) + Constants.LAUNCH_APP_JSP;
    if (this.getSecAdvisor() != null) {
      url = this.getSecAdvisor().appendIdCoreForUrl(url);
    }
    return url;
  }

  public String getShowRequestFormURL(HttpServletRequest request) throws Exception {
    return getAppURL(request) + Constants.SHOW_REQUEST_FORM;
  }

  public void sendNotification(Object obj, Session sess, String state, String targetGroup, String type){
    //Don't bother making notifications for activity feed if we aren't showing it on the dashboard
    if(PropertyDictionaryHelper.getInstance(sess).getProperty(PropertyDictionary.SHOW_ACTIVITY_DASHBOARD).equals("") || 
        PropertyDictionaryHelper.getInstance(sess).getProperty(PropertyDictionary.SHOW_ACTIVITY_DASHBOARD).equals("N")) {
      return;
    }
    Integer idLab = null;
    Integer idAppUser = getSecAdvisor().getIdAppUser();
    String objectNumber = null;
    String imageSource = null;
    Integer idCoreFacility = null;

    if(obj instanceof Request){
      Request r = (Request) obj;
      idLab = r.getIdLab();
      objectNumber = r.getNumber();
      imageSource = ((RequestCategory) sess.load(RequestCategory.class, r.getCodeRequestCategory())).getIcon();
      idCoreFacility = r.getIdCoreFacility();
    } else if (obj instanceof Analysis){
      idLab = ((Analysis) obj).getIdLab();
      objectNumber = ((Analysis) obj).getNumber();
      imageSource = "assets/map.png";
      idCoreFacility = ((Analysis) obj).getIdCoreFacility();
    } else if(obj instanceof Invoice){
      idCoreFacility = ((Invoice) obj).getIdCoreFacility();
      String useInvoiceNumbering = PropertyDictionaryHelper.getInstance(sess).getCoreFacilityProperty(idCoreFacility, PropertyDictionary.USE_INVOICE_NUMBERING);
      if (useInvoiceNumbering == null || !useInvoiceNumbering.equals("N")) {
    	  objectNumber = ((Invoice) obj).getInvoiceNumber();  
      }
    } else if(obj instanceof DataTrack){
      idLab = ((DataTrack) obj).getIdLab();
      objectNumber = ((DataTrack) obj).getNumber();
      imageSource = "assets/datatrack.png";
    } else if(obj instanceof Topic){
      idLab = ((Topic) obj).getIdLab();
      objectNumber = ((Topic) obj).getNumber();
      imageSource = "assets/topic_tag.png";
    } else if(obj instanceof FlowCell){
      objectNumber = ((FlowCell) obj).getNumber();
      imageSource = "assets/rectangle.png";
    }

    Notification note = new Notification();
    note.setSourceType(targetGroup);
    note.setType(type);
    note.setExpID(objectNumber);
    note.setDate(new java.sql.Date(System.currentTimeMillis()));
    note.setIdLabTarget(idLab);
    note.setIdUserTarget(idAppUser);
    note.setMessage(state);
    note.setImageSource(imageSource);
    note.setIdCoreFacility(idCoreFacility);

    StringBuffer buf = new StringBuffer();
    if(idAppUser != null){
      buf.append("SELECT firstName, lastName FROM AppUser WHERE idAppUser='" + idAppUser +"'");

      List rows = (List) sess.createQuery(buf.toString()).list();
      String fullName = "";
      if (rows.size() > 0) {
        for(Iterator i = rows.iterator(); i.hasNext();) {
          Object[] row = (Object[])i.next();
          fullName = row[0] == null ? "" : (String)row[0].toString();
          fullName += " ";
          fullName += row[1] == null ? "" : (String)row[1].toString();
        }
      }
      note.setFullNameUser(fullName);
    }

    sess.save(note);
    sess.flush();
  }


}
