package hci.gnomex.controller;

import hci.gnomex.utility.DictionaryHelper;
import hci.framework.control.Command;
import hci.framework.control.RollBackCommandException;
import hci.framework.model.DetailObject;
import hci.framework.utilities.XMLReflectException;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import hci.gnomex.model.RequestCategory;
import hci.gnomex.model.RequestFilter;
import hci.gnomex.model.Request;


public class GetRequestList extends GNomExCommand implements Serializable {
  
  private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GetRequestList.class);
  
  private RequestFilter requestFilter;
  
  public void validate() {
  }
  
  public void loadCommand(HttpServletRequest request, HttpSession session) {

    requestFilter = new RequestFilter();
    HashMap errors = this.loadDetailObject(request, requestFilter);
    this.addInvalidFields(errors);
  }

  public Command execute() throws RollBackCommandException {
    
    try {
    Session sess = this.getSecAdvisor().getReadOnlyHibernateSession(this.getUsername());
    DictionaryHelper dh = DictionaryHelper.getInstance(sess);

    StringBuffer buf = requestFilter.getQuery(this.getSecAdvisor());
    
    log.info("Query for GetRequestList: " + buf.toString());
    List reqs = sess.createQuery(buf.toString()).list();
    
    Document doc = new Document(new Element("RequestList"));
    for(Iterator i = reqs.iterator(); i.hasNext();) {
      Request req = (Request)i.next();
      
      Element node = req.toXMLDocument(null, DetailObject.DATE_OUTPUT_SQL).getRootElement();
      
      RequestCategory requestCategory = dh.getRequestCategoryObject(req.getCodeRequestCategory());
      String requestStatus = dh.getRequestStatus(req.getCodeRequestStatus());
      
      node.setAttribute("icon", requestCategory != null && requestCategory.getIcon() != null ? requestCategory.getIcon() : "");
      
      List label = sess.createQuery("Select plate.label from Request as req Left Join req.samples as samps Left Join samps.wells as pws Left Join pws.plate as plate where req.idRequest = " + req.getIdRequest() + " AND plate.codePlateType = 'REACTION'").list();
      node.setAttribute("plateLabel", label.size() != 0 && label.get(0) != null ? (String)label.get(0) : "");
      
      doc.getRootElement().addContent(node);
      
    }
    
    XMLOutputter out = new org.jdom.output.XMLOutputter();
    this.xmlResult = out.outputString(doc);
    
    setResponsePage(this.SUCCESS_JSP);
    }catch (NamingException e){
      log.error("An exception has occurred in GetRequestList ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
    }catch (SQLException e) {
      log.error("An exception has occurred in GetRequestList ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
    } catch (XMLReflectException e){
      log.error("An exception has occurred in GetRequestList ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
    } catch (Exception e){
      log.error("An exception has occurred in GetRequestList ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
    } finally {
      try {
        this.getSecAdvisor().closeReadOnlyHibernateSession();        
      } catch(Exception e) {
        
      }
    }
    
    return this;
  }

}