/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.doi.server.monitoring;

import fr.cnes.doi.client.ClientLandingPage;
import fr.cnes.doi.client.ClientSearchDataCite;
import fr.cnes.doi.settings.EmailSettings;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jean-Christophe Malapert (jean-christophe.malapert@cnes.fr)
 */
public class LandingPageMonitoring implements Runnable {

    public LandingPageMonitoring(final String publisher) {
        super();
    }

    public LandingPageMonitoring() {
        super();
    }

    @Override
    public void run() {
        EmailSettings email = EmailSettings.getInstance();
        String subject;
        String msg;
        try {

            ClientSearchDataCite client = new ClientSearchDataCite();
            List<String> response = client.getDois();
            ClientLandingPage clientLandingPage = new ClientLandingPage(response);

            if (clientLandingPage.isSuccess()) {
                subject = "Landing pages checked with success";
                msg = "All landing pages (" + response.size() + ") are on-line";
            } else {
                subject = "Landing pages checked with errors";
                List<String> errors = clientLandingPage.getErrors();
                msg = errors.size() + " are off-line !!!\n";
                msg += "List of off-line landing pages:\n";
                msg += "-------------------------------\n";
                for (String error : errors) {
                    msg += "- " + error + "\n";
                }
            }
            email.sendMessage(subject, msg);
            Logger.getLogger(LandingPageMonitoring.class.getName()).log(Level.INFO, msg);
        } catch (Exception ex) {
            email.sendMessage("Unrecoverable errors when checking landing pages", ex.toString());
            Logger.getLogger(LandingPageMonitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}