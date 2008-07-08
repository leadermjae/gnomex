package hci.gnomex.utility;

import hci.framework.model.DetailObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.jdom.Document;
import org.jdom.Element;


public class AnalysisLaneParser extends DetailObject implements Serializable {
  
  protected Document    doc;
  protected List        idSequenceLaneList = new ArrayList();
  protected HashMap     idRequestMap = new HashMap();
  
  public AnalysisLaneParser(Document doc) {
    this.doc = doc;
 
  }
  
  public void parse(Session sess) throws Exception{
    
    Element root = this.doc.getRootElement();
    
    
    for(Iterator i = root.getChildren("SequenceLane").iterator(); i.hasNext();) {
      Element node = (Element)i.next();
      
      String idSequenceLaneString = node.getAttributeValue("idSequenceLane");
      Integer idSequenceLane = new Integer(idSequenceLaneString);

      String idRequestString = node.getAttributeValue("idRequest");

      idSequenceLaneList.add(idSequenceLane);
      idRequestMap.put(idSequenceLane, new Integer(idRequestString));

    }
  }

  
  public List getIdSequenceLanes() {
    return idSequenceLaneList;
  }
  
  public Integer getIdRequest(Integer idSequenceLane) {
    return (Integer)idRequestMap.get(idSequenceLane);
  }
  
}
