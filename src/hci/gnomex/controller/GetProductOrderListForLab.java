package hci.gnomex.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import hci.framework.control.Command;
import hci.framework.control.RollBackCommandException;
import hci.gnomex.model.ProductOrder;
import hci.gnomex.model.ProductOrderFilter;
import hci.gnomex.utility.DictionaryHelper;
import org.apache.log4j.Logger;
public class GetProductOrderListForLab extends GNomExCommand implements Serializable {

  private static Logger LOG = Logger.getLogger(GetProductOrderListForLab.class);

  private Integer idLab;
  private ProductOrderFilter productOrderFilter;

  @Override
  public void loadCommand(HttpServletRequest request, HttpSession sess) {
    if(request.getParameter("idLab") != null && !request.getParameter("idLab").equals("")) {
      idLab = Integer.parseInt(request.getParameter("idLab"));
    } else {
      this.addInvalidField("Missing idLab", "idLab is required");
    }

    productOrderFilter = new ProductOrderFilter(this.getSecAdvisor());
    HashMap errors = this.loadDetailObject(request, productOrderFilter);
    this.addInvalidFields(errors);

  }

  public Command execute() throws RollBackCommandException {
    try {
      if(this.isValid()) {
        Session sess = this.getSecAdvisor().getReadOnlyHibernateSession(this.getUsername());

        DictionaryHelper dh = DictionaryHelper.getInstance(sess);

        List productOrders = sess.createQuery("SELECT po from ProductOrder as po where idLab = " +  idLab).list();

        Document doc = new Document(new Element("productOrdersList"));

        for(Iterator i = productOrders.iterator(); i.hasNext();) {
          ProductOrder po = (ProductOrder) i.next();
          Element e = new Element("ProductOrder");

          String submitter = dh.getAppUserObject(po.getIdAppUser()).getDisplayName();
          //String productType = dh.getProductTypeObject(po.getIdProductType()).getDisplay();
          String orderStatus = po.getStatus();

          e.setAttribute("display", po.getDisplay() );
          e.setAttribute("submitter", submitter);
          e.setAttribute("submitDate", po.getSubmitDate().toString());
          e.setAttribute("status", orderStatus);

          doc.getRootElement().addContent(e);
        }

        XMLOutputter out = new org.jdom.output.XMLOutputter();
        this.xmlResult = out.outputString(doc);

        setResponsePage(this.SUCCESS_JSP);
      } else {
        setResponsePage(this.ERROR_JSP);
      }

    } catch(Exception e) {
      LOG.error("An exception has occurred in GetProductOrderList ", e);

      throw new RollBackCommandException(e.getMessage());

    } finally {
      try {
        //closeReadOnlyHibernateSession;
      } catch(Exception e){
        LOG.error("Error", e);
      }

    }

    return this;
  }

  @Override
  public void validate() {
    // TODO Auto-generated method stub

  }

}
