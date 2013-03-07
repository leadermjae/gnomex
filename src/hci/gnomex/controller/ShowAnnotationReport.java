package hci.gnomex.controller;

import hci.framework.control.Command;
import hci.framework.control.RollBackCommandException;
import hci.framework.security.UnknownPermissionException;
import hci.gnomex.model.AnalysisGroupFilter;
import hci.gnomex.model.AppUser;
import hci.gnomex.model.DataTrack;
import hci.gnomex.model.GenomeBuild;
import hci.gnomex.model.Lab;
import hci.gnomex.model.Organism;
import hci.gnomex.model.Property;
import hci.gnomex.model.PropertyOption;
import hci.gnomex.model.PropertyType;
import hci.gnomex.model.RequestSampleFilter;
import hci.gnomex.security.SecurityAdvisor;
import hci.gnomex.utility.DataTrackQuery;
import hci.gnomex.utility.DictionaryHelper;
import hci.report.constants.ReportFormats;
import hci.report.model.Column;
import hci.report.model.ReportRow;
import hci.report.model.ReportTray;
import hci.report.utility.ReportCommand;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;


public class ShowAnnotationReport extends ReportCommand implements Serializable {
  
  private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ShowAnnotationReport.class);
  
  
  private SecurityAdvisor           secAdvisor;
  private List<Integer>             idProperties = new ArrayList<Integer>();
  private String                    target;
  private Integer                   idLab;
  private RequestSampleFilter       sampleFilter;
  private AnalysisGroupFilter       analysisFilter;
  private DataTrackQuery            dataTrackQuery;
  private TreeMap<Integer, Map>     propertyEntryAnnotationMap = new TreeMap<Integer, Map>();
  private TreeMap<String, Integer>  propertyColumnMap = new TreeMap<String, Integer>();
  
  private static final String       TARGET_SAMPLE = "SAMPLE";
  private static final String       TARGET_ANALYSIS = "ANALYSIS";
  private static final String       TARGET_DATATRACK = "DATATRACK";
  
  private String                    today;
 
  public void validate() {
  }
  
  public void loadCommand(HttpServletRequest request, HttpSession session) {
    if (request.getParameter("target") != null && !request.getParameter("target").equals("")) {
      target = request.getParameter("target");
    } else {
      this.addInvalidField("target", "target is required");
    }
    
    if (request.getParameter("idLab") != null && !request.getParameter("idLab").equals("")) {
      idLab = Integer.valueOf(request.getParameter("idLab"));
    } 


    if (target.equals(TARGET_SAMPLE)) {
      sampleFilter = new RequestSampleFilter();
      this.loadDetailObject(request, sampleFilter);
    } else if (target.equals(TARGET_ANALYSIS)) {
      analysisFilter = new AnalysisGroupFilter();
      this.loadDetailObject(request, analysisFilter);
    } else if (target.equals(TARGET_DATATRACK)) {
      dataTrackQuery = new DataTrackQuery(request);
    }
    
    if (request.getParameter("idProperties") != null && !request.getParameter("idProperties").equals("")) {
      String idPropertiesString = request.getParameter("idProperties");
      String[] tokens = idPropertiesString.split(",");
      for (int x = 0; x < tokens.length; x++) {
        Integer idProperty = Integer.valueOf(tokens[x]);
        idProperties.add(idProperty);
      }
    } 

    
    secAdvisor = (SecurityAdvisor)session.getAttribute(SecurityAdvisor.SECURITY_ADVISOR_SESSION_KEY);
    if (secAdvisor == null) {
      this.addInvalidField("secAdvisor", "A security advisor must be created before this command can be executed.");
    }
    
    today = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());

  }

  @SuppressWarnings("unchecked")
  public Command execute() throws RollBackCommandException {
    
    this.SUCCESS_JSP_HTML = "/report.jsp";
    this.SUCCESS_JSP_CSV = "/report_csv.jsp";
    this.SUCCESS_JSP_PDF = "/report_pdf.jsp";
    this.SUCCESS_JSP_XLS = "/report_xls.jsp";
    this.ERROR_JSP = "/message.jsp";
    
    
    try {
         
      Session sess = secAdvisor.getReadOnlyHibernateSession(this.getUsername());
      DictionaryHelper dh = DictionaryHelper.getInstance(sess);
      
      // Create the report and define the columns
      createReportTray(sess, dh);
    
      // Get the results
      StringBuffer queryBuf = null;
      if (target.equals(TARGET_SAMPLE)) {
        queryBuf = sampleFilter.getQuery(secAdvisor);
      } else if (target.equals(TARGET_ANALYSIS)) {
        queryBuf = analysisFilter.getQuery(secAdvisor);
      } else if (target.equals(TARGET_DATATRACK)) {
        queryBuf = dataTrackQuery.getDataTrackQuery(secAdvisor);
      } else {
        this.addInvalidField("target incorrect", "Incorrect target parameter provided");
      }

      if (this.isValid()) {
        
        List results = (List)sess.createQuery(queryBuf.toString()).list();

        // Get the annotations
        if (target.equals(TARGET_SAMPLE)) {
          queryBuf = sampleFilter.getAnnotationQuery(secAdvisor);
        } else if (target.equals(TARGET_ANALYSIS)) {
          queryBuf = analysisFilter.getAnnotationQuery(secAdvisor);
        }  else if (target.equals(TARGET_DATATRACK)) {
          queryBuf = dataTrackQuery.getAnnotationQuery(secAdvisor);
        }      
        hashAnnotations(sess, dh, queryBuf);
        

        Map<Integer, Integer> idsToSkip = new HashMap<Integer, Integer>();
        if (target.equals(TARGET_SAMPLE)) {
          idsToSkip = secAdvisor.getBSTXSecurityIdsToExclude(sess, dh, results, RequestSampleFilter.COL_ID_REQUEST, RequestSampleFilter.COL_CODE_REQUEST_CATEGORY);
        }
        
        for(Iterator i = results.iterator(); i.hasNext();) {
          Object[] row = (Object[])i.next();

          ReportRow reportRow = new ReportRow();
          List values  = new ArrayList();
          
          Integer theId = null;
          
          if (target.equals(TARGET_SAMPLE)) {
            theId =  (Integer)row[RequestSampleFilter.COL_ID_SAMPLE];
            String labLastName = (String)row[RequestSampleFilter.COL_LAB_LAST_NAME];
            String labFirstName = (String)row[RequestSampleFilter.COL_LAB_FIRST_NAME];
            String submitterLastName = (String)row[RequestSampleFilter.COL_SUBMITTER_LAST_NAME];
            String submitterFirstName = (String)row[RequestSampleFilter.COL_SUBMITTER_FIRST_NAME];
            Integer idRequest = (Integer)row[RequestSampleFilter.COL_ID_REQUEST];
            String requestNumber = (String)row[RequestSampleFilter.COL_REQUEST_NUMBER];
            String sampleNumber = (String)row[RequestSampleFilter.COL_SAMPLE_NUMBER];
            String sampleName = (String)row[RequestSampleFilter.COL_SAMPLE_NAME];
            String sampleDescription = (String)row[RequestSampleFilter.COL_SAMPLE_DESCRIPTION];
            Integer idOrganism = (Integer)row[RequestSampleFilter.COL_SAMPLE_ID_ORGANISM];
            
            if (idsToSkip.get(idRequest) != null) {
              // Skip for BSTX Security
              continue;
            }
            
            String labName = Lab.formatLabName(labLastName, labFirstName);
            String submitterName = AppUser.formatName(submitterLastName, submitterFirstName);
            
            String organism = idOrganism != null ? dh.getOrganism(idOrganism) : "";
            values.add(labName);
            values.add(submitterName);
            values.add(requestNumber);
            values.add(sampleNumber);
            values.add(sampleName);
            values.add(sampleDescription != null ? sampleDescription : "");
            values.add(organism);
          } else if (target.equals(TARGET_ANALYSIS)) {
            theId =  (Integer)row[AnalysisGroupFilter.COL_ID_ANALYSIS];
            String labLastName = (String)row[AnalysisGroupFilter.COL_LAB_LAST_NAME];
            String labFirstName = (String)row[AnalysisGroupFilter.COL_LAB_FIRST_NAME];
            String ownerLastName = (String)row[AnalysisGroupFilter.COL_OWNER_LAST_NAME];
            String ownerFirstName = (String)row[AnalysisGroupFilter.COL_OWNER_FIRST_NAME];
            String analysisNumber = (String)row[AnalysisGroupFilter.COL_ANALYSIS_NUMBER];
            String analysisName = (String)row[AnalysisGroupFilter.COL_ANALYSIS_NAME];
            Integer idOrganism = (Integer)row[AnalysisGroupFilter.COL_ID_ORGANISM];
            String folderName = (String)row[AnalysisGroupFilter.COL_ID_ANALYSIS_GROUP_NAME];
            Integer idAnalysisType = (Integer)row[AnalysisGroupFilter.COL_ID_ANALYSIS_TYPE];
            
            String labName = Lab.formatLabName(labLastName, labFirstName);
            String ownerName = AppUser.formatName(ownerLastName, ownerFirstName);
            String organism = idOrganism != null ? dh.getOrganism(idOrganism) : "";
            String analysisType = idAnalysisType != null ? dh.getAnalysisType(idAnalysisType) : "";
            
            values.add(labName);
            values.add(ownerName);
            values.add(analysisNumber);
            values.add(analysisName);
            values.add(organism);
            values.add(folderName != null ? folderName : "");
            values.add(analysisType);
          } else if (target.equals(TARGET_DATATRACK)) {
            DataTrack dataTrack =  (DataTrack)row[DataTrackQuery.COL_DATATRACK];
            if (dataTrack != null) {
              theId = dataTrack.getIdDataTrack();
              Organism organism = (Organism)row[DataTrackQuery.COL_ORGANISM];
              GenomeBuild genomeBuild = (GenomeBuild)row[DataTrackQuery.COL_GENOME_BUILD];
              
              String labName = dataTrack.getLab() != null ? dataTrack.getLab().getName() : "";
              String ownerName = dataTrack.getAppUser() != null ? dataTrack.getAppUser().getDisplayName() : "";
              
              values.add(labName);
              values.add(ownerName);
              values.add(dataTrack.getNumber());
              values.add(dataTrack.getName());
              values.add(dataTrack.getSummary());
              values.add(organism.getDas2Name());
              values.add(genomeBuild.getDas2Name());
            }
          }
         
          // For analysis and data track results, we may encounter null entries (folders
          // without content); in this case,  just bypass this "null" results row.
          if (theId == null) {
            continue;
          }
          
          addAnnotationValues(values, theId);
         
          reportRow.setValues(values);
          tray.addRow(reportRow);
        }
      }
      
      if (isValid()) {
        this.setSuccessJsp(this, tray.getFormat());
      } else {
        setResponsePage(this.ERROR_JSP);
      }
    
    }catch (UnknownPermissionException e){
      log.error("An exception has occurred in ShowAnnotationReport ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
        
    }catch (NamingException e){
      log.error("An exception has occurred in ShowAnnotationReport ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
        
    }catch (SQLException e) {
      log.error("An exception has occurred in ShowAnnotationReport ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
      
    } catch (Exception e) {
      log.error("An exception has occurred in ShowAnnotationReport ", e);
      e.printStackTrace();
      throw new RollBackCommandException(e.getMessage());
    } finally {
      try {
        secAdvisor.closeReadOnlyHibernateSession();    
      } catch(Exception e) {
        
      }
    }
    
    return this;
  }
  
  private void createReportTray(Session sess, DictionaryHelper dh) {
    // Get the lab
    String labQualifier = "";
    if (idLab != null) {
      Lab lab = (Lab)sess.get(Lab.class, idLab);
      labQualifier += "_" + lab.getLastName();
    }
    
    String title = "GNomEx " + target.toLowerCase() + " annotations";
    String fileName = "gnomex_" + target.toLowerCase() + "_annotation"+ labQualifier + "_" + today;
    
    // set up the ReportTray
    tray = new ReportTray();
    tray.setReportDate(new java.util.Date(System.currentTimeMillis()));
    tray.setReportTitle(title);
    tray.setReportDescription(title);
    tray.setFileName(fileName);
    tray.setFormat(ReportFormats.XLS);
    
    Set columns = new TreeSet();
    if (target.equals(TARGET_SAMPLE)) {
      columns.add(makeReportColumn("Lab", 1));
      columns.add(makeReportColumn("Submitter", 2));
      columns.add(makeReportColumn("Experiment Number", 3));
      columns.add(makeReportColumn("Sample Number", 4));
      columns.add(makeReportColumn("Sample Name", 5));
      columns.add(makeReportColumn("Sample Description", 6));
      columns.add(makeReportColumn("Organism", 7));
    } else if (target.equals(TARGET_ANALYSIS)) {
      columns.add(makeReportColumn("Lab", 1));
      columns.add(makeReportColumn("Submitter", 2));
      columns.add(makeReportColumn("Analysis Number", 3));
      columns.add(makeReportColumn("Name", 4));
      columns.add(makeReportColumn("Organism", 5));
      columns.add(makeReportColumn("Folder", 6));
      columns.add(makeReportColumn("Analysis Type", 7));
    } else if (target.equals(TARGET_DATATRACK)) {
      columns.add(makeReportColumn("Lab", 1));
      columns.add(makeReportColumn("Submitter", 2));
      columns.add(makeReportColumn("Data Track Number", 3));
      columns.add(makeReportColumn("Name", 4));
      columns.add(makeReportColumn("Summary", 5));
      columns.add(makeReportColumn("Organism", 6));
      columns.add(makeReportColumn("Folder", 7));
      
    }
    
    tray.setColumns(columns);

    
    int columnNumber = columns.size() + 1;
    for(Integer idProperty : idProperties) {
      Property property = dh.getPropertyObject(idProperty);
      propertyColumnMap.put(property.getName(), columnNumber++);
    }
    
    for (Iterator i = propertyColumnMap.keySet().iterator(); i.hasNext();) {
      String propertyName = (String)i.next();
      Integer col = propertyColumnMap.get(propertyName);
      columns.add(makeReportColumn(propertyName, col.intValue()));
    }
    
  }
  
  private void hashAnnotations(Session sess, DictionaryHelper dh, StringBuffer queryBuf) {
    
    List annotations = (List)sess.createQuery(queryBuf.toString()).list();
    propertyEntryAnnotationMap = new TreeMap<Integer, Map>();
    for (Iterator i = annotations.iterator(); i.hasNext();) {
      Object[] row = (Object[])i.next();
      Integer theId = null;
      Integer idProperty = null;
      String codePropertyType= null;
      String propertyName  = null;
      String propertyValue  = null;
      String propertyMultiValue  = null;
      String propertyOption  = null;
      if (target.equals(TARGET_SAMPLE)) {
        theId = (Integer)row[RequestSampleFilter.PROPCOL_ID_SAMPLE];
        idProperty = (Integer) row[RequestSampleFilter.COL_ID_PROPERTY];
        codePropertyType = (String) row[RequestSampleFilter.COL_PROPERTY_TYPE];
        propertyName = (String) row[RequestSampleFilter.COL_PROPERTY_NAME];
        propertyValue = (String) row[RequestSampleFilter.COL_PROPERTY_VALUE];
        propertyMultiValue = (String) row[RequestSampleFilter.COL_PROPERTY_MULTI_VALUE];
        propertyOption = (String) row[RequestSampleFilter.COL_PROPERTY_OPTION];
      } else if (target.equals(TARGET_ANALYSIS)) {
        theId = (Integer)row[AnalysisGroupFilter.PROPCOL_ID_ANALYSIS];
        idProperty = (Integer) row[AnalysisGroupFilter.PROPCOL_ID_PROPERTY];
        codePropertyType = (String) row[AnalysisGroupFilter.PROPCOL_PROPERTY_TYPE];
        propertyName = (String) row[AnalysisGroupFilter.PROPCOL_PROPERTY_NAME];
        propertyValue = (String) row[AnalysisGroupFilter.PROPCOL_PROPERTY_VALUE];
        propertyMultiValue = (String) row[AnalysisGroupFilter.PROPCOL_PROPERTY_MULTI_VALUE];
        propertyOption = (String) row[AnalysisGroupFilter.PROPCOL_PROPERTY_OPTION];
      } else if (target.equals(TARGET_DATATRACK )) {
        theId = (Integer)row[DataTrackQuery.COL_ID_DATATRACK];
        idProperty = (Integer) row[DataTrackQuery.COL_ID_PROPERTY];
        codePropertyType = (String) row[DataTrackQuery.COL_PROPERTY_TYPE];
        propertyName = (String) row[DataTrackQuery.COL_PROPERTY_NAME];
        propertyValue = (String) row[DataTrackQuery.COL_PROPERTY_VALUE];
        propertyMultiValue = (String) row[DataTrackQuery.COL_PROPERTY_MULTI_VALUE];
        propertyOption = (String) row[DataTrackQuery.COL_PROPERTY_OPTION];
      } 
      
      Map annotationMap = propertyEntryAnnotationMap.get(theId);
      if (annotationMap == null) {
        annotationMap = new TreeMap<String, String>();
        propertyEntryAnnotationMap.put(theId, annotationMap);
      }
      
      if (propertyName == null) {
        continue;
      }
      
      String value = (String)annotationMap.get(propertyName);
      if (value == null) {
        value = "";
      }
      
      // Work around problem where single options are not stored in separate PropertyOption.  
      // Instead, the idPropertyOption is stored in PropertyEntry.valueString.  Obtain
      // the option from the dictionary by matching the idPropertyOption to the Property's
      // option
      boolean grabOption = false;
      if (codePropertyType.equals(PropertyType.MULTI_OPTION)) {
        grabOption = true;
      } else if (codePropertyType.equals(PropertyType.OPTION) && !target.equals(TARGET_SAMPLE)) {
        grabOption = true;
      } 
      
      if (grabOption) {
        if (propertyOption != null) {
          if (value.length() > 0) {
            value += ", ";
          }
          value += propertyOption;
        }
      } else if (codePropertyType.equals(PropertyType.OPTION) && target.equals(TARGET_SAMPLE))  {
        // Workaround code (see comment above) until sample annotations save property entry options correctly.
        if (propertyValue != null) {
          if (value.length() > 0) {
            value += ", ";
          }
          try {
            Integer idPropertyOption = Integer.valueOf(propertyValue);
            Property property = dh.getPropertyObject(idProperty);
            for (PropertyOption option : (Set<PropertyOption>)property.getOptions()) {
              if (option.getIdPropertyOption().equals(idPropertyOption)) {
                value += option.getOption();                
              }
            }            
          } catch (NumberFormatException e) {
            // If the valueString is not an integer, just show the value in the report.
            value += propertyValue;
          }
        }
      } else {
        if (propertyValue != null) {
          if (value.length() > 0) {
            value += ", ";
          }
          value += propertyValue;
        } else if (propertyMultiValue != null) {
          if (value.length() > 0) {
            value += ", ";
          }
          value += propertyMultiValue;
        } 
      }
      annotationMap.put(propertyName, value);
    }
    
  }
  
  private void addAnnotationValues(List values, Integer theId) {

    Map annotationMap = propertyEntryAnnotationMap.get(theId);
    for (Iterator i1 = propertyColumnMap.keySet().iterator(); i1.hasNext();) {
      String propertyName = (String)i1.next();
      String annotationValue = "";
      if (annotationMap != null) {
        annotationValue = (String)annotationMap.get(propertyName);
      }
      values.add(annotationValue != null ? annotationValue : "");
    }
    
  }
  
  
  private Column makeReportColumn(String name, int colNumber) {
    Column reportCol = new Column();
    reportCol.setName(name);
    reportCol.setCaption(name);
    reportCol.setDisplayOrder(new Integer(colNumber));
    return reportCol;
  }
  

  /* (non-Javadoc)
   * @see hci.framework.control.Command#setRequestState(javax.servlet.http.HttpServletRequest)
   */
  public HttpServletRequest setRequestState(HttpServletRequest request) {
    request.setAttribute("tray", this.tray);
    return request;
  }

  /* (non-Javadoc)
   * @see hci.framework.control.Command#setResponseState(javax.servlet.http.HttpServletResponse)
   */
  public HttpServletResponse setResponseState(HttpServletResponse response) {
    // TODO Auto-generated method stub
    return response;
  }

  /* (non-Javadoc)
   * @see hci.framework.control.Command#setSessionState(javax.servlet.http.HttpSession)
   */
  public HttpSession setSessionState(HttpSession session) {
    // TODO Auto-generated method stub
    return session;
  }

  /* (non-Javadoc)
   * @see hci.report.utility.ReportCommand#loadContextPermissions()
   */
  public void loadContextPermissions(){
    
  }
  public void loadContextPermissions(String userName) throws SQLException {
    
  }
  
}