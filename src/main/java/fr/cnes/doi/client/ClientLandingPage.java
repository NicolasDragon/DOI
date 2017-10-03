/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.client;

import java.util.ArrayList;
import java.util.List;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
//TODO : Utiliser getDoi de Mds à la place


/**
 * Checks the status of the landing page.
 * @author Jean-Christophe Malapert (jean-christophe.malapert@cnes.fr)
 */
public class ClientLandingPage extends BaseClient {

    /**
     * DOI name resolver {@value #BASE_URI}.
     */
    private static final String BASE_URI = "http://doi.org";
    
    /**
     * List of offline landing pages.
     */
    private final List<String> errors = new ArrayList<>();

    /**
     * Constructor
     * @param dois List of DOIs to check
     */
    public ClientLandingPage(final List<String> dois) {
        super(BASE_URI);
        checkDoi(dois);
    }

    /**
     * Tests for each DOI if the landing page is online.
     * @param dois dois to check
     */
    //TODO : check with Head before. If not implemented, check with get
    private void checkDoi(final List<String> dois) {
        this.getClient().setFollowingRedirects(true);
        this.getClient().setLoggable(true);
        for (final String doi : dois) {
            this.getClient().setReference(BASE_URI);
            this.getClient().addSegment(doi);
            try {
                this.getClient().get();
                final Status status = this.getClient().getStatus();
                if (status.isError()) {
                    this.errors.add(doi);
                }
            } catch (ResourceException ex) {
                this.errors.add(doi);
                //ClientLandingPage.getLOGGER().fine(ex.getMessage());
            } finally {
                this.getClient().release();
            }
        }
    }

    /**
     * Returns true when there is no error otherwise False.
     * @return true when there is no error otherwise False
     */
    public boolean isSuccess() {
        return this.errors.isEmpty();
    }

    /**
     * Returns True when there is more than zero error otherwise False
     * @return True when there is more than zero error otherwise False
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * Returns the errors.
     * @return the error
     */
    public List<String> getErrors() {
        return this.errors;
    }

}
