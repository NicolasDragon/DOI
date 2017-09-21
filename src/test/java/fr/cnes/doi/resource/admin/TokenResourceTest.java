/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.resource.admin;

import fr.cnes.doi.InitServerForTest;
import static fr.cnes.doi.resource.mds.MediaResourceTest.DOI;
import fr.cnes.doi.settings.Consts;
import fr.cnes.doi.settings.DoiSettings;
import java.io.IOException;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class TokenResourceTest {

    private static Client cl;
    public static final String DOI = "10.5072/828606/8c3e91ad45ca855b477126bc073ae44b";

    public TokenResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        InitServerForTest.init();
        cl = new Client(new Context(), Protocol.HTTPS);
        Series<Parameter> parameters = cl.getContext().getParameters();
        parameters.add("truststorePath", "jks/doiServerKey.jks");
        parameters.add("truststorePassword", DoiSettings.getInstance().getSecret(Consts.SERVER_HTTPS_TRUST_STORE_PASSWD));
        parameters.add("truststoreType", "JKS");
    }

    @AfterClass
    public static void tearDownClass() {
        InitServerForTest.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of createToken method, of class TokenResource.
     */
    @Test
    public void testCreateToken() throws IOException {
        System.out.println("createToken");
        String port = DoiSettings.getInstance().getString(Consts.SERVER_HTTPS_PORT);
        ClientResource client = new ClientResource("https://localhost:" + port + "/admin/token");
        client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        client.setNext(cl);
        Form form = new Form();
        form.add("identifier", "jcm");
        form.add("projectID", "828606");
        Representation response = client.post(form);
        String token = response.getText();
        client.release();
        assertNotNull(token);
    }

    /**
     * Test of getTokenInformation method, of class TokenResource.
     */
    @Test
    public void testGetTokenInformation() throws IOException {
        System.out.println("getTokenInformation");
        String port = DoiSettings.getInstance().getString(Consts.SERVER_HTTPS_PORT);
        ClientResource client = new ClientResource("https://localhost:" + port + "/admin/token");
        client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        client.setNext(cl);
        Form form = new Form();
        form.add("identifier", "jcm");
        form.add("projectID", "828606");
        Representation response = client.post(form);
        String token = response.getText();
        client.release();

        client = new ClientResource("https://localhost:" + port + "/admin/token/" + token);
        client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        try {
            Representation rep = client.get();
            String text = rep.getText();
            assertNotNull(text);
            assertTrue(text.contains("{"));
        } catch (ResourceException ex) {
            Assert.fail();
        }
        client.release();

    }

    /**
     * Test of getTokenInformation method, of class TokenResource.
     */
    @Test
    public void testTokenAuthenticationWithBadRole() throws IOException {
        System.out.println("getTokenInformation");
        String port = DoiSettings.getInstance().getString(Consts.SERVER_HTTPS_PORT);
        ClientResource client = new ClientResource("https://localhost:" + port + "/admin/token");
        client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        client.setNext(cl);
        Form form = new Form();
        form.add("identifier", "jcm");
        form.add("projectID", "828606");
        Representation response = client.post(form);
        String token = response.getText();
        client.release();

        Form mediaForm = new Form();
        mediaForm.add("image/fits", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/jpeg", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/png", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        client = new ClientResource("https://localhost:" + port + "/mds/media/" + DOI);
        client.setNext(cl);
        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_OAUTH_BEARER);
        cr.setRawValue(token);
        //cr.setRawValue("asdsqqscsqcqdcqscqc");
        client.setChallengeResponse(cr);

        Status status;
        try {
            client.post(mediaForm);
            status = client.getStatus();
        } catch (ResourceException ex) {
            status = ex.getStatus();
        }
        assertEquals(Status.CLIENT_ERROR_FORBIDDEN.getCode(), status.getCode());     
    }
    
    /**
     * Test of getTokenInformation method, of class TokenResource.
     */
    @Test
    public void testTokenAuthenticationWithRightRole() throws IOException {
        System.out.println("getTokenInformation");
        String port = DoiSettings.getInstance().getString(Consts.SERVER_HTTPS_PORT);
        ClientResource client = new ClientResource("https://localhost:" + port + "/admin/token");
        client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        client.setNext(cl);
        Form form = new Form();
        form.add("identifier", "malapert");
        form.add("projectID", "828606");
        Representation response = client.post(form);
        String token = response.getText();
        client.release();

        Form mediaForm = new Form();
        mediaForm.add("image/fits", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/jpeg", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/png", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        client = new ClientResource("https://localhost:" + port + "/mds/media/" + DOI);
        client.setNext(cl);
        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_OAUTH_BEARER);
        cr.setRawValue(token);
        //cr.setRawValue("asdsqqscsqcqdcqscqc");
        client.setChallengeResponse(cr);

        Status status;
        try {
            client.post(mediaForm);
            status = client.getStatus();
        } catch (ResourceException ex) {
            status = ex.getStatus();
        }
        assertEquals(Status.SUCCESS_OK.getCode(), status.getCode());     
    }    
    
    /**
     * Test of getTokenInformation method, of class TokenResource.
     */
    @Test
    public void testTokenAuthenticationWithWrongToken() throws IOException {
        System.out.println("getTokenInformation");
        String port = DoiSettings.getInstance().getString(Consts.SERVER_HTTPS_PORT);
        Form mediaForm = new Form();
        mediaForm.add("image/fits", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/jpeg", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        mediaForm.add("image/png", "https://cnes.fr/sites/default/files/drupal/201508/default/is_cnesmag65-interactif-fr.pdf");
        ClientResource client = new ClientResource("https://localhost:" + port + "/mds/media/" + DOI);
        client.setNext(cl);
        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_OAUTH_BEARER);        
        cr.setRawValue("asdsqqscsqcqdcqscqc");
        client.setChallengeResponse(cr);

        Status status;
        try {
            client.post(mediaForm);
            status = client.getStatus();
        } catch (ResourceException ex) {
            status = ex.getStatus();
        }
        assertEquals(Status.CLIENT_ERROR_FORBIDDEN.getCode(), status.getCode());     
    }    

}