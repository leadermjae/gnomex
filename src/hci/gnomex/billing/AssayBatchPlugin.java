package hci.gnomex.billing;

import hci.gnomex.constants.Constants;
import hci.gnomex.model.Application;
import hci.gnomex.model.BioanalyzerChipType;
import hci.gnomex.model.BillingItem;
import hci.gnomex.model.BillingPeriod;
import hci.gnomex.model.BillingStatus;
import hci.gnomex.model.Hybridization;
import hci.gnomex.model.LabeledSample;
import hci.gnomex.model.Price;
import hci.gnomex.model.PriceCategory;
import hci.gnomex.model.PriceCriteria;
import hci.gnomex.model.PropertyEntry;
import hci.gnomex.model.Request;
import hci.gnomex.model.Sample;
import hci.gnomex.model.SequenceLane;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;


// Note that Assays are actually stored in the BioanalyzerChipType object
public class AssayBatchPlugin implements BillingPlugin {

  public List constructBillingItems(Session sess, String amendState, BillingPeriod billingPeriod, PriceCategory priceCategory, Request request, 
      Set<Sample> samples, Set<LabeledSample> labeledSamples, Set<Hybridization> hybs, Set<SequenceLane> lanes, Map<String, ArrayList<String>> sampleToAssaysMap, 
      String billingStatus, Set<PropertyEntry> propertyEntries) {

    List billingItems = new ArrayList<BillingItem>();
    
    if (samples == null || samples.size() == 0) {
      return billingItems;
    }

    
    // Generate the billing item.  Find the price using the
    // criteria of the application.
    int numSamples = samples.size();
    int qty = 0;

    // Find the price - there is only one
    Price price = null;
    for(Iterator i1 = priceCategory.getPrices().iterator(); i1.hasNext() && price == null;) {
      Price p = (Price)i1.next();
      if (p.getIsActive() != null && p.getIsActive().equals("Y")) {
        // If the price an application criteria, find the appropriate one.
        if ( p.getPriceCriterias()!=null && p.getPriceCriterias().size()>0 ) {
          for(Iterator i2 = p.getPriceCriterias().iterator(); i2.hasNext();) {
            PriceCriteria criteria = (PriceCriteria)i2.next();
            if (criteria.getFilter1().equals(request.getCodeApplication())) {
              if ((request.getCodeBioanalyzerChipType() == null && (criteria.getFilter2() == null || criteria.getFilter2().length() == 0))
                  || (request.getCodeBioanalyzerChipType() != null && criteria.getFilter2() != null && criteria.getFilter2().length() > 0 
                      && criteria.getFilter2().equals(request.getCodeBioanalyzerChipType()))) {          
                price = p;
                break;        
              }
            }
          }
        } else {
          price = p;
          break;
        }
      }
    }
    
    BioanalyzerChipType assay = null;
    if (request.getCodeBioanalyzerChipType() != null) {
      assay = (BioanalyzerChipType) sess.get( BioanalyzerChipType.class, request.getCodeBioanalyzerChipType() );
    }
    int batch = 0;
    if ( assay != null) {
      batch = assay.getSampleWellsPerChip()!=null ? assay.getSampleWellsPerChip():0;
    }
    
    if ( batch == 0 ) {
      qty = numSamples; 
    } else {
      if ( batch >= numSamples ) {
        qty = batch;
      } else {
        qty = (numSamples + (batch-1))/batch * batch;
      } 
    }
    
    // Instantiate a BillingItem for the matched price
    if (price != null) {
      BigDecimal theUnitPrice = price.getEffectiveUnitPrice(request.getLab());

      BillingItem billingItem = new BillingItem();
      billingItem.setCategory(priceCategory.getName());
      billingItem.setCodeBillingChargeKind(priceCategory.getCodeBillingChargeKind());
      billingItem.setIdBillingPeriod(billingPeriod.getIdBillingPeriod());
      billingItem.setDescription(price.getName());
      billingItem.setQty(qty);
      billingItem.setUnitPrice(theUnitPrice);
      billingItem.setPercentagePrice(new BigDecimal(1));
      if (qty > 0 && theUnitPrice != null) {
        billingItem.setInvoicePrice(theUnitPrice.multiply(new BigDecimal(qty)));          
      }
      billingItem.setCodeBillingStatus(billingStatus);
      if (!billingStatus.equals(BillingStatus.NEW) && !billingStatus.equals(BillingStatus.PENDING)) {
        billingItem.setCompleteDate(new java.sql.Date(System.currentTimeMillis()));
      }
      billingItem.setIdRequest(request.getIdRequest());
      billingItem.setIdBillingAccount(request.getIdBillingAccount());
      billingItem.setIdLab(request.getIdLab());
      billingItem.setIdPrice(price.getIdPrice());
      billingItem.setIdPriceCategory(price.getIdPriceCategory());
      billingItem.setSplitType(Constants.BILLING_SPLIT_TYPE_PERCENT_CODE);
      billingItem.setIdCoreFacility(request.getIdCoreFacility());

      // Hold off on saving the notes.  Need to reserve note field
      // for complete date, etc at this time.
      //billingItem.setNotes(notes);


      billingItems.add(billingItem);

    }
    
    
    return billingItems;
  }

  

}