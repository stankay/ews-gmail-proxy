package net.stankay.ewsgmail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

/**
 * This class is a modification of official GMail example taken from https://developers.google.com/gmail/api/quickstart/java
 */
public class GmailClient {
    /** Application name. */
    private static final String APPLICATION_NAME = "EWS-Gmail Proxy";

    /** Directory to store user credentials for this application. */
    private final File DATA_STORE_DIR = new File(
        System.getProperty("user.home"), ".credentials/ews-gmail-proxy");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private HttpTransport HTTP_TRANSPORT;
    
    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/gmail-java-quickstart.json
     */
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY);

	private final String emailAddress;

	private final String pathToSecret;

    public GmailClient(String emailAddress, String pathToSecret) {
    	this.emailAddress = emailAddress;
    	this.pathToSecret = pathToSecret;

        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
	}

	/**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    private Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(new File(this.pathToSecret));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        //System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Gmail client service.
     * @param emailAddress 
     * @return an authorized Gmail client service
     * @throws IOException
     */
    private Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * List labels that have been created for given account
     * 
     * @param emailAddress GMail e-mail address
     * @throws IOException 
     */
    public void listLabels() throws IOException {
        Gmail service = getGmailService();
    	
        ListLabelsResponse listResponse = service.users().labels().list(emailAddress).execute();
        
        List<Label> labels = listResponse.getLabels();
        if (labels.isEmpty()) {
            System.out.println("No labels found.");
        } else {
            System.out.println("Label name (label ID):");
            for (Label label : labels) {
                System.out.printf("- %s (%s)\n", label.getName(), label.getId());
            }
        }
    }
    
    /**
     * Insert message into given mailbox.
     * 1. Insert it into Inbox of the account
     * 2. Mark as unread
     * 3. Label with labels from configuration
     * 
     * @param emailAddress GMail address to use for inserting
     * @param rawMessage RFC2822 message
     * @param labelIds Comma-separated list of labels to be applied
     * @return Newly created message in the inbox
     * @throws IOException
     */
    public Message insertUnreadMessage(String rawMessage, String labelIds) throws IOException {
        Gmail service = getGmailService();

        String base64 = java.util.Base64.getEncoder().encodeToString(rawMessage.getBytes());
        base64 = base64.replace("/", "_").replace("+", "-");
        
        Message newMes = new Message().setRaw(base64);
        
        newMes.setLabelIds(generateLabelList(labelIds));
        
		return service.users().messages().insert(emailAddress, newMes).execute();
    }

    /**
     * Transform comma-separated list of GMail labels into List<String>.
     * Add two default labels: INBOX, UNREAD
     * 
     * @param labelIds Comma-separated list of label ids
     * @return java.util.List of label ids 
     */
	public static List<String> generateLabelList(String labelIds) {
        List<String> labelIdsList = new ArrayList<>();
        labelIdsList.add("INBOX");
        labelIdsList.add("UNREAD");

        if (labelIds != null && !labelIds.trim().isEmpty()) {
        	String[] labels = labelIds.split(",");
        
	        for (String label : labels) {
	        	labelIdsList.add(label.trim());
	        }
        }
        
        return labelIdsList;
	}
}