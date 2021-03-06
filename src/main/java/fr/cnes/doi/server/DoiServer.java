/*
 * Copyright (C) 2017-2018 Centre National d'Etudes Spatiales (CNES).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package fr.cnes.doi.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.LocalReference;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.routing.Filter;
import org.restlet.service.LogService;
import org.restlet.service.Service;
import org.restlet.util.Series;

import fr.cnes.doi.application.DoiCrossCiteApplication;
import fr.cnes.doi.application.DoiMdsApplication;
import fr.cnes.doi.application.AdminApplication;
import fr.cnes.doi.logging.api.DoiLogDataServer;
import fr.cnes.doi.logging.business.JsonMessage;
import fr.cnes.doi.logging.security.DoiSecurityLogFilter;
import fr.cnes.doi.security.RoleAuthorizer;
import fr.cnes.doi.settings.Consts;
import fr.cnes.doi.settings.DoiSettings;
import fr.cnes.doi.settings.EmailSettings;
import fr.cnes.doi.settings.JettySettings;
import fr.cnes.doi.settings.ProxySettings;
import fr.cnes.doi.utils.Utils;
import fr.cnes.doi.utils.spec.Requirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DoiServer contains the configuration of this server and the methods to
 * start/stop it.
 *
 * @author Jean-Christophe Malapert (jean-christophe.malapert@cnes.fr)
 */
public class DoiServer extends Component {
    
    /**
     * Default value for {@link #SSL_CTX_FACTORY} parameter : {@value #DEFAULT_SSL_CTX}.
     */
    public static final String DEFAULT_SSL_CTX = "org.restlet.engine.ssl.DefaultSslContextFactory";
    /**
     * SslContectFactory parameter {@value #SSL_CTX_FACTORY}.
     */
    public static final String SSL_CTX_FACTORY = "sslContextFactory";
    /**
     * Key store parameter {@value #KEY_STORE_PATH}.
     */
    public static final String KEY_STORE_PATH = "keyStorePath";
    /**
     * Key store password parameter {@value #KEY_STORE_PWD}.
     */
    public static final String KEY_STORE_PWD = "keyStorePassword";    
    /**
     * Key store type parameter {@value #KEY_STORE_TYPE}.
     */
    public static final String KEY_STORE_TYPE = "keyStoreType"; 
    /**
     * Key password parameter {@value #KEY_PWD}.
     */
    public static final String KEY_PWD = "keyPassword"; 
    /**
     * Trust store path parameter {@value #TRUST_STORE_PATH}.
     */
    public static final String TRUST_STORE_PATH = "trustStorePath"; 
    /**
     * Trust store password parameter {@value #TRUST_STORE_PATH}.
     */
    public static final String TRUST_STORE_PWD = "trustStorePassword"; 
    /**
     * Trust store type parameter {@value #TRUST_STORE_PATH}.
     */
    public static final String TRUST_STORE_TYPE = "trustStoreType";      
    /**
     * JKS file, which is stored in the JAR.
     */
    public static final String JKS_FILE = "doiServerKey.jks";

    /**
     * URI of the Meta Data Store application.
     */
    public static final String MDS_URI = "/mds";

    /**
     * URI of the Citation application.
     */
    public static final String CITATION_URI = "/citation";

    /**
     * URI of the Datacite status.
     */
    public static final String STATUS_URI = "/status";

    /**
     * Default port HTTP server.
     */
    public static final String DEFAULT_HTTP_PORT = "8182";

    /**
     * Default port for HTTPS server.
     */
    public static final String DEFAULT_HTTPS_PORT = "8183";

    /**
     * Logger.
     */
    private static final Logger LOG = LogManager.getLogger(DoiServer.class.getName());
    
    /**
     * Template message {@value #MESSAGE_TPL}.
     */
    private static final String MESSAGE_TPL = "{} : {}";
    
    /**
     * Text to display {@value #PARAMETERS} in log messages.
     */
    private static final String PARAMETERS = "Parameters"; 
    
    /**
     * Configuration.
     */
    private final DoiSettings settings;    

    /**
     * Creates an instance of the server with settings coming from the
     * config.properties
     *
     * @param settings settings
     */
    public DoiServer(final DoiSettings settings) {
        super();
        this.settings = settings;
        startWithProxy();
    }

    /**
     * Init log services.
     */
    private void initLogServices() {
        LOG.traceEntry();
        this.getLogService().setLoggerName(Utils.HTTP_LOGGER_NAME);

        final LogService logServiceApplication = new DoiLogDataServer(Utils.APP_LOGGER_NAME, true);
        this.getServices().add(logServiceApplication);

        final Service logServiceSecurity = new LogService(true) {
            /**
             * Creates a filter
             * @param context context
             * @return Filter
             * @see org.restlet.service.LogService#createInboundFilter(org.restlet.Context)
             */
            @Override
            public Filter createInboundFilter(final Context context) {
                return new DoiSecurityLogFilter("fr.cnes.doi.logging.security");
            }
        };
        this.getServices().add(logServiceSecurity);
        LOG.traceExit();
    }

    /**
     * Configures the Server in HTTP and HTTPS.
     */
    @Requirement(reqId = Requirement.DOI_ARCHI_010,reqName = Requirement.DOI_ARCHI_010_NAME)
    private void configureServer() {
        LOG.traceEntry();
        initHttpServer();
        initHttpsServer();
        initClients();
        initAttachApplication();
        LOG.traceExit();
    }

    /**
     * Inits the HTTP server.
     */
    private void initHttpServer() {        
        LOG.traceEntry();
        final Server serverHttp = startHttpServer(
                settings.getInt(Consts.SERVER_HTTP_PORT, DEFAULT_HTTP_PORT)
        );
        this.getServers().add(serverHttp);
        initJettyConfiguration(serverHttp);
        LOG.traceExit();
    }

    /**
     * Inits the HTTPS server.
     */
    private void initHttpsServer() {
        LOG.traceEntry();
        final Server serverHttps = startHttpsServer(
                settings.getInt(Consts.SERVER_HTTPS_PORT, DEFAULT_HTTPS_PORT)
        );
        this.getServers().add(serverHttps);
        initJettyConfiguration(serverHttps);
        LOG.traceExit();
    }

    /**
     * Init the Jetty configuration and applies it to the server.
     *
     * @param server HTTP or HTTPS server
     */
    private void initJettyConfiguration(final Server server) {
        LOG.traceEntry(new JsonMessage(server));
        final JettySettings jettyProps = new JettySettings(server, settings);
        jettyProps.addParamsToServerContext();
        LOG.traceExit();
    }

    /**
     * Inits supported protocols. Theses protocols are used by the server to
     * access to resources
     */
    private void initClients() {
        LOG.traceEntry();
        this.getClients().add(Protocol.HTTP);
        this.getClients().add(Protocol.HTTPS);
        this.getClients().add(Protocol.CLAP);
        this.getClients().add(Protocol.FILE);
        LOG.traceExit();
    }

    /**
     * Routes the applications.
     */
    private void initAttachApplication() {
        LOG.traceEntry();
        final Application appDoiProject = new DoiMdsApplication();
        final Application appAdmin = new AdminApplication();
        this.getDefaultHost().attach(MDS_URI, appDoiProject);
        this.getDefaultHost().attach(CITATION_URI, new DoiCrossCiteApplication());
        this.getDefaultHost().attachDefault(appAdmin);
        // Set authentication
        RoleAuthorizer.getInstance().createRealmFor(appDoiProject);
        RoleAuthorizer.getInstance().createRealmFor(appAdmin);
        LOG.traceExit();
    }

    /**
     * Starts with proxy.
     *
     */
    private void startWithProxy() {
        LOG.traceEntry();
        initLogServices();
        ProxySettings.getInstance();
        EmailSettings.getInstance();
        configureServer();
        LOG.traceExit();
    }

    /**
     * Creates a HTTP server
     *
     * @param port HTTP port
     * @return the HTTP server
     */
    private Server startHttpServer(final Integer port) {
        LOG.traceEntry(MESSAGE_TPL, PARAMETERS, port);
        final Server server = new Server(Protocol.HTTP, port, this);
        return LOG.traceExit(server);
    }

    /**
     * Creates a HTTPS server
     *
     * @param port HTTPS port
     * @return the HTTPS server
     */
    private Server startHttpsServer(final Integer port) {
        LOG.traceEntry(MESSAGE_TPL, PARAMETERS, port);
        final String pathKeyStore;
        if (settings.hasValue(Consts.SERVER_HTTPS_KEYSTORE_PATH)) {
            pathKeyStore = settings.getString(Consts.SERVER_HTTPS_KEYSTORE_PATH);
            LOG.debug("path key store value loaded from a custom configuration file");            
        } else {
            pathKeyStore = extractKeyStoreToPath();
            LOG.debug("path key store value loaded from an internal configuration"); 
        }

        final String pathKeyTrustStore;
        if (settings.hasValue(Consts.SERVER_HTTPS_TRUST_STORE_PATH)) {
            pathKeyTrustStore = settings.getString(Consts.SERVER_HTTPS_TRUST_STORE_PATH);
        } else {
            pathKeyTrustStore = extractKeyStoreToPath();
        }

        // create embedding https jetty server
        final Server server = new Server(new Context(), Protocol.HTTPS, port, this);
        final Series<Parameter> parameters = server.getContext().getParameters();

        LOG.debug(MESSAGE_TPL,SSL_CTX_FACTORY, DEFAULT_SSL_CTX);
        parameters.add(SSL_CTX_FACTORY, DEFAULT_SSL_CTX);
        
        // Specifies the path for the keystore used by the server
        LOG.debug(MESSAGE_TPL,KEY_STORE_PATH, pathKeyStore);
        parameters.add(KEY_STORE_PATH, pathKeyStore);
        
        // Specifies the password for the keystore containing several keys
        LOG.debug(MESSAGE_TPL,KEY_STORE_PWD, "xxxxxxx");
        parameters.add(KEY_STORE_PWD, settings.getSecret(Consts.SERVER_HTTPS_KEYSTORE_PASSWD));
        
        // Specifies the type of the keystore
        LOG.debug(MESSAGE_TPL,KEY_STORE_TYPE, KeyStore.getDefaultType());
        parameters.add(KEY_STORE_TYPE, KeyStore.getDefaultType());
        
        // Specifies the password of the specific key used
        LOG.debug(MESSAGE_TPL,KEY_PWD, "xxxxxxxx");
        parameters.add(KEY_PWD, settings.getSecret(Consts.SERVER_HTTPS_SECRET_KEY));
        
        // Specifies the path to the truststore
        LOG.debug(MESSAGE_TPL,TRUST_STORE_PATH, pathKeyTrustStore);
        parameters.add(TRUST_STORE_PATH, pathKeyTrustStore);
        
        // Specifies the password of the truststore
        LOG.debug(MESSAGE_TPL,TRUST_STORE_PWD, "xxxxx");
        parameters.add(TRUST_STORE_PWD, settings.getSecret(Consts.SERVER_HTTPS_TRUST_STORE_PASSWD));
        
        // Specifies the type of the truststore
        LOG.debug(MESSAGE_TPL,TRUST_STORE_TYPE, KeyStore.getDefaultType());
        parameters.add(TRUST_STORE_TYPE, KeyStore.getDefaultType());
        
        return LOG.traceExit(server);
    }

    /**
     * Extracts keystore for JAR and copy it in a directory in order to use it.
     *
     * @return the path of the new location of the keystore.
     */
    private String extractKeyStoreToPath() { 
        LOG.traceEntry();
        String result;
        final Representation jks = new ClientResource(
                LocalReference.createClapReference("class/"+JKS_FILE)
        ).get();
        try {
            final Path outputDirectory = new File("jks").toPath();
            if (Files.notExists(outputDirectory)) {
                Files.createDirectory(outputDirectory);
                LOG.info("Creates {} directory to extract {} in it", outputDirectory, JKS_FILE);
            }
            final String outputJksLocation = outputDirectory.getFileName() 
                    + File.separator 
                    + JKS_FILE;

            final File outputFile = new File(outputJksLocation);
            Files.copy(jks.getStream(), 
                    outputFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            LOG.info("Copy/replace if exists {} in {}",JKS_FILE, outputDirectory, JKS_FILE);
            result = outputJksLocation;
        } catch (IOException ex) {
            LOG.fatal("Unable to extract keystore from class/"+JKS_FILE,ex);
            result = "";
        }
        return LOG.traceExit(result);
    }

}
