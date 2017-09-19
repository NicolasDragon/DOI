/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.doi.settings.Consts;
import fr.cnes.doi.settings.DoiSettings;
import fr.cnes.doi.utils.spec.Requirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.restlet.data.Status;
import org.restlet.representation.Representation;

/**
 *
 * @author Jean-Christophe Malapert (jean-christophe.malapert@cnes.fr)
 */
@Requirement(
        reqId = Requirement.DOI_INTER_010,
        reqName = Requirement.DOI_INTER_010_NAME
)
public class ClientSearchDataCite extends BaseClient {
    
    private static final String BASE_URI = "https://search.datacite.org/api?";    
    private static final int COUNT = 1000;
    
    private final List<String> doiList = new ArrayList<>();    
    
    public ClientSearchDataCite() throws Exception {
        super(BASE_URI);
        computeListDOI(0);
    }  
    
    public final void computeListDOI(int start) throws Exception {
        this.client.setReference(BASE_URI);
        this.client.addQueryParameter("q", "prefix:"+DoiSettings.getInstance().getString(Consts.INIST_DOI));
        this.client.addQueryParameter("fl", "doi");
        this.client.addQueryParameter("wt", "json");
        this.client.addQueryParameter("indent", "true");
        this.client.addQueryParameter("rows", String.valueOf(COUNT));
        this.client.addQueryParameter("start", String.valueOf(start));
        Representation rep = client.get();
        Status status = client.getStatus();        
        if(status.isSuccess()) { 
            ObjectMapper mapper = new ObjectMapper();
            Map responseJson = mapper.readValue(rep.getStream() , Map.class);             
            Map responseMap = (Map) responseJson.get("response");
            int numFound = (int) responseMap.get("numFound");
            List<Map> dois = (List) responseMap.get("docs");
            for(Map doi : dois) {
                this.doiList.add(String.valueOf(doi.get("doi")));
            }
            if(this.doiList.size() != numFound) {
                computeListDOI(this.doiList.size());
            }
            
        } else {
            throw new Exception();
        }
    }   
    
    public List<String> getDois() {
        return this.doiList;
    }
    
}
