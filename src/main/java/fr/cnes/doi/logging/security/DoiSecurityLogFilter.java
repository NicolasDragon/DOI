/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.logging.security;

import fr.cnes.doi.utils.spec.CoverageAnnotation;
import fr.cnes.doi.utils.spec.Requirement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.engine.Engine;
import org.restlet.routing.Filter;
import org.restlet.security.Role;

/**
 *
 * @author malapert
 */
@Requirement(
        reqId = Requirement.DOI_ARCHI_020,
        reqName = Requirement.DOI_ARCHI_020_NAME,
        coverage = CoverageAnnotation.PARTIAL,
        comment = "Log4J n'est pas utilisé"        
)
public class DoiSecurityLogFilter extends Filter {

    /**
     * The name of the logger to use
     */
    private final String loggerName;

    /**
     * Instantiates a new sitools log filter.
     *
     * @param loggerName the name of the logger to use
     */
    public DoiSecurityLogFilter(String loggerName) {
        super();
        this.loggerName = loggerName;
    }

    /*
   * (non-Javadoc)
   * 
   * @see org.restlet.routing.Filter#afterHandle(org.restlet.Request, org.restlet.Response)
     */
    /**
     *
     * @param request
     * @param response
     */
    @Override
    protected void afterHandle(Request request, Response response) {
        super.afterHandle(request, response);
        Object logRecordObj = response.getAttributes().get("LOG_RECORD");
        if (logRecordObj != null) {

            ClientInfo clientInfo = request.getClientInfo();
            String user = null;
            String profile = null;
            if (clientInfo != null && clientInfo.getUser() != null) {
                user = clientInfo.getUser().getIdentifier();
                profile = "";
                List<Role> roles = clientInfo.getRoles();
                Set<String> rolesStr = new HashSet<>();
                for (Role role : roles) {
                    rolesStr.add(role.getName());
                }
                //profile += Joiner.on(",").join(rolesStr);
                profile += "," + rolesStr;
            }

            LogRecord logRecord = (LogRecord) logRecordObj;
            logRecord.setMessage("User: " + user + "\tProfile: " + profile + "\t" + logRecord.getMessage());
            Logger logger = Engine.getLogger(loggerName);
            logger.log(logRecord);

        }
    }
}
