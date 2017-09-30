/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.application;

import org.restlet.Restlet;
import org.restlet.routing.Router;

import fr.cnes.doi.client.ClientCrossCiteCitation;
import fr.cnes.doi.resource.citation.FormatCitationResource;
import fr.cnes.doi.resource.citation.LanguageCitationResource;
import fr.cnes.doi.resource.citation.StyleCitationResource;
import fr.cnes.doi.utils.spec.Requirement;

/**
 * Provides an application to get citation from a registered DOI. Books and
 * journal articles have long benefited from an infrastructure that makes them
 * easy to cite, a key element in the process of research and academic
 * discourse. In this mind, a data should cited in just the same way. DataCite
 * DOIs help further research and assures reliable, predictable, and unambiguous
 * access to research data in order to:
 * <ul>
 * <li>support proper attribution and credit</li>
 * <li>support collaboration and reuse of data</li>
 * <li>enable reproducibility of findings</li>
 * <li>foster faster and more efficient research progress, and</li>
 * <li>provide the means to share data with future researchers</li>
 * </ul>
 * DataCite also looks to community practices that provide data citation
 * guidance. The Joint Declaration of Data Citation Principles is a set of
 * guiding principles for data within scholarly literature, another dataset, or
 * any other research object (Data Citation Synthesis Group 2014).
 * <p>
 * The FAIR Guiding Principles provide a guideline for the those that want to
 * enhance reuse of their data (Wilkinson 2016).
 *
 * @author Jean-Christophe Malapert (jean-christophe.malapert@cnes.fr)
 * @see "http://citation.crosscite.org/"
 */
@Requirement(
    reqId = Requirement.DOI_SRV_100,
    reqName = Requirement.DOI_SRV_100_NAME
    )
@Requirement(
    reqId = Requirement.DOI_SRV_110,
    reqName = Requirement.DOI_SRV_110_NAME
    )
@Requirement(
    reqId = Requirement.DOI_SRV_120,
    reqName = Requirement.DOI_SRV_120_NAME
    )
@Requirement(
    reqId = Requirement.DOI_MONIT_020,
    reqName = Requirement.DOI_MONIT_020_NAME
    )
public class DoiCrossCiteApplication extends AbstractApplication {       

    /**
     * URI to get the styles, which are used to format the citation.
     */
    public static final String STYLES_URI = "/style";
    /**
     * URI to get the languages, which are used to format the citation.
     */
    public static final String LANGUAGE_URI = "/language";
    /**
     * Retrieves the citation.
     */
    public static final String FORMAT_URI = "/format";
    /**
     * Application name.
     */
    public static final String NAME = "Cross Cite Application";

    /**
     * Class name.
     */
    private static final String CLASS_NAME = DoiCrossCiteApplication.class.getName();  
    
    /**
     * Client to query CrossCite.
     */
    private final ClientCrossCiteCitation client = new ClientCrossCiteCitation();

    /**
     * Constructs the application by setting the proxy authentication to the
     * null null     {@link fr.cnes.doi.client.ClientCrossCiteCitation
	 * ClientCrossCiteCitation} proxy when the configuration is set.
     */
    public DoiCrossCiteApplication() {
        super();
        getLogger().entering(CLASS_NAME, "Constructor");

        setName(NAME);
        final StringBuilder description = new StringBuilder();
        description.append("Books and journal articles have long benefited from "
                + "an infrastructure that makes them easy to cite, a key element"
                + " in the process of research and academic discourse. "
                + "We believe that you should cite data in just the same way "
                + "that you can cite other sources of information, " 
                + "such as articles and books.");
        description.append("DataCite DOIs help further research and assures "
                + "reliable, predictable, and unambiguous access to research " 
                + "data in order to:");
        description.append("<ul>");
        description.append("<li>support proper attribution and credit</li>");
        description.append("<li>support collaboration and reuse of data</li>");
        description.append("<li>enable reproducibility of findings</li>");
        description.append("<li>foster faster and more efficient research progress, and</li>");
        description.append("<li>provide the means to share data with future researchers</li>");
        description.append("</ul>");
        description.append("DataCite also looks to community practices that provide"
                + " data citation guidance. The Joint Declaration of Data Citation"
                + " Principles is a set of guiding principles for data within "
                + "scholarly literature, another dataset, or any other research "
                + "object (Data Citation Synthesis Group 2014). The FAIR Guiding "
                + "Principles provide a guideline for the those that want to "
                + "enhance reuse of their data (Wilkinson 2016).");
        setDescription(description.toString());

        getLogger().exiting(CLASS_NAME, "Constructor");
    }

    /**
     * Creates router the DOICrossCiteApplication. This routes routes the following
     * resources:
     * <ul>
     * <li>{@link DoiCrossCiteApplication#STYLES_URI} to access to the different styles
     * for a citation</li>
     * <li>{@link DoiCrossCiteApplication#LANGUAGE_URI} to access to the different
     * languages for a citation</li>
     * <li>{@link DoiCrossCiteApplication#FORMAT_URI} to access to the formatted citation</li>
     * </ul>
     * @return router
     */
    @Override
    public Restlet createInboundRoot() {
        getLogger().entering(CLASS_NAME, "createInboundRoot");

        final Router router = new Router(getContext());
        router.attach(STYLES_URI, StyleCitationResource.class);
        router.attach(LANGUAGE_URI, LanguageCitationResource.class);
        router.attach(FORMAT_URI, FormatCitationResource.class);

        getLogger().exiting(CLASS_NAME, "createInboundRoot");
        return router;
    }

    /**
     * Returns the client to query cross cite.
     *
     * @return the client
     */
    public ClientCrossCiteCitation getClient() {
        return this.client;
    }

}
