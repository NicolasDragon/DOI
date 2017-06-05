/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.server;

import fr.cnes.doi.settings.JettySettings;
import fr.cnes.doi.settings.Consts;
import fr.cnes.doi.application.DoiCrossCiteApplication;
import fr.cnes.doi.application.DoiMdsApplication;
import fr.cnes.doi.application.DoiStatusApplication;
import fr.cnes.doi.application.WebSiteApplication;
import fr.cnes.doi.settings.DoiSettings;
import fr.cnes.doi.logging.DoiLogDataServer;
import fr.cnes.doi.logging.MonitoringLogFilter;
import fr.cnes.doi.logging.DoiSecurityLogFilter;
import fr.cnes.doi.resource.DoiResource;
import fr.cnes.doi.resource.DoisResource;
import fr.cnes.doi.resource.MetadataResource;
import fr.cnes.doi.resource.MetadatasResource;
import fr.cnes.doi.settings.EmailSettings;
import fr.cnes.doi.settings.ProxySettings;
import java.security.KeyStore;
import java.util.logging.Logger;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.routing.Filter;
import org.restlet.security.Group;
import org.restlet.security.MemoryRealm;
import org.restlet.security.Role;
import org.restlet.security.User;
import org.restlet.service.LogService;
import org.restlet.service.Service;
import org.restlet.util.Series;

/**
 *
 * @author malapert
 */
public class DoiServer extends Component {

    public static final String MDS_URI = "/mds";
    public static final String CITATION_URI = "/citation";
    public static final String STATUS_URI = "/status";

    public static final String DEFAULT_HTTP_PORT = "8182";

    private final DoiSettings settings;

    private static final Logger LOGGER = Logger.getLogger(DoiServer.class.getName());

    public DoiServer(final DoiSettings settings) {
        super();
        this.settings = settings;        
        startWithProxy(settings);
    }

    /**
     * Init Monitoring
     * @return monitoring object
     */
    private DoiMonitoring initMonitoring() {
        LOGGER.entering(getClass().getName(),"initMonitoring");
        DoiMonitoring monitoring = new DoiMonitoring();
        monitoring.register(Method.GET, MDS_URI + DoiMdsApplication.DOI_URI, DoisResource.LIST_ALL_DOIS);
        monitoring.register(Method.POST, MDS_URI + DoiMdsApplication.DOI_URI, DoisResource.CREATE_DOI);
        monitoring.register(Method.GET, MDS_URI + DoiMdsApplication.DOI_NAME_URI, DoiResource.GET_DOI);
        monitoring.register(Method.POST, MDS_URI + DoiMdsApplication.METADATAS_URI, MetadatasResource.CREATE_METADATA);
        monitoring.register(Method.GET, MDS_URI + DoiMdsApplication.METADATAS_URI + DoiMdsApplication.DOI_NAME_URI, MetadataResource.GET_METADATA);
        monitoring.register(Method.DELETE, MDS_URI + DoiMdsApplication.METADATAS_URI + DoiMdsApplication.DOI_NAME_URI, MetadataResource.DELETE_METADATA);
        LOGGER.exiting(getClass().getName(),"initMonitoring");        
        return monitoring;
    }
    
    private void initLogServices() {
        LOGGER.entering(getClass().getName(),"initLogServices");
        this.setLogService(new DoiLogDataServer("fr.cnes.doi.api", true));

        LogService logServiceApplication = new DoiLogDataServer("fr.cnes.doi.app", true) {
            /*
           * (non-Javadoc)
           * 
           * @see org.restlet.service.LogService#createInboundFilter(org.restlet.Context)
             */
            @Override
            public Filter createInboundFilter(Context context) {
                return new MonitoringLogFilter(context, initMonitoring(), this);
            }
        };
        this.getServices().add(logServiceApplication);
        //final String securityLoggerName = settings.getString("Starter.SecurityLogName");

        Service logServiceSecurity = new LogService(true) {
            /*
             * (non-Javadoc)
             * 
             * @see org.restlet.service.LogService#createInboundFilter(org.restlet.Context)
             */
            @Override
            public Filter createInboundFilter(Context context) {
                return new DoiSecurityLogFilter("fr.cnes.doi.security");
            }
        };
        this.getServices().add(logServiceSecurity);        
        LOGGER.exiting(getClass().getName(),"initLogServices");        
    }

    /**
     * Configures the Server
     */
    private void configureServer() {        
        LOGGER.entering(getClass().getName(), "init");                

        Server serverHttp = startHttpServer(settings.getInt(Consts.SERVER_HTTP_PORT, DEFAULT_HTTP_PORT));
        //Server serverHttps = startHttpsServer(this, 443);
                
        //this.getServers().add(serverHttps);
        this.getServers().add(serverHttp);
        this.getClients().add(Protocol.HTTP);
        this.getClients().add(Protocol.HTTPS);
        this.getClients().add(Protocol.CLAP);
                    
        // Add configuration parameters to Servers
        JettySettings jettyProps = new JettySettings(serverHttp, settings);
        jettyProps.addParamsToServerContext();        
        //jettyProps = new JettySettings(serverHttps, settings);
        //jettyProps.addParamsToServerContext();                

        Application appDoiProject = new DoiMdsApplication();

        // Attach the application to the this and start it
        this.getDefaultHost().attach(MDS_URI, appDoiProject);
        this.getDefaultHost().attach(CITATION_URI, new DoiCrossCiteApplication());
        this.getDefaultHost().attach(STATUS_URI, new DoiStatusApplication());
        this.getDefaultHost().attachDefault(new WebSiteApplication());        

        // Set authentication
        MemoryRealm realm = new MemoryRealm();
        Role project1 = new Role(appDoiProject, "Project1");
        Role project2 = new Role(appDoiProject, "Project2");
        User jc = new User("jcm", "myPwd", "Jean-Christophe", "Malapert", "jcmalapert@gmail.com");
        User claire = new User("claire", "myPwd2");
        User software = new User("software", "pwd");
        Group human = new Group("human", "human users");
        human.getMemberUsers().add(jc);
        human.getMemberUsers().add(claire);
        Group soft = new Group("software", "software users");
        soft.getMemberUsers().add(software);

        appDoiProject.getContext().setDefaultEnroler(realm.getEnroler());
        appDoiProject.getContext().setDefaultVerifier(realm.getVerifier());

        realm.map(human, project1);
        realm.map(human, project2);

        this.getLogService().setResponseLogFormat(settings.getLogFormat());
        
        LOGGER.exiting(getClass().getName(), "init");
    }
    
    /**
     * Starts with proxy.
     * @param settings 
     */
    private void startWithProxy(final DoiSettings settings) {
        LOGGER.entering(getClass().getName(), "startWithProxy");
        initLogServices();
        ProxySettings.getInstance().init(settings);
        EmailSettings.getInstance().init(settings);
        configureServer();
        LOGGER.exiting(getClass().getName(), "startWithProxy");        
    }

    /**
     * Creates a HTTP server
     *
     * @param this this
     * @param port HTTP port
     * @return the HTTP server
     * @throws Exception
     */
    private Server startHttpServer(final Integer port) {
        LOGGER.entering(getClass().getName(), "startHttpServer", port);
        Server server = new Server(Protocol.HTTP, port, this);
        LOGGER.exiting(getClass().getName(), "startHttpServer");
        return server;
    }

    /**
     * Creates a HTTPS server
     *
     * @param this this
     * @param port HTTPS port
     * @return the HTTPS server
     * @throws Exception
     */
    private Server startHttpsServer(final Integer port) {
        LOGGER.entering(getClass().getName(), "startHttpsServer", port);
        // create embedding https jetty server
        Server server = new Server(new Context(), Protocol.HTTPS, port, this);
        Series<Parameter> parameters = server.getContext().getParameters();
        parameters.add("keystore", "jks/keystore.jks");
        parameters.add("keyStorePath", "jks/keystore.jks");
        parameters.add("keyStorePassword", "xxx");
        parameters.add("keyManagerPassword", "xxx");
        parameters.add("keyPassword", "xxx");
        parameters.add("password", "xxx");
        parameters.add("keyStoreType", KeyStore.getDefaultType());
        parameters.add("tracing", "true");
        parameters.add("truststore", "jks/keystore.jks");
        parameters.add("trustStorePath", "jks/keystore.jks");
        parameters.add("trustStorePassword", "xxx");
        parameters.add("trustPassword", "xxx");
        parameters.add("trustStoreType", KeyStore.getDefaultType());
        parameters.add("allowRenegotiate", "true");
        parameters.add("type", "1");
        LOGGER.exiting(getClass().getName(), "startHttpsServer", server);
        return server;
    }


}
