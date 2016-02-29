package net.stankay.ewsgmail;

import java.net.URI;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.credential.WebCredentials;

/**
 * Client that encapsulates EWS artifacts
 */
public class EWSClient {

	private ExchangeService service;
	private Folder inbox;

	/**
	 * Connect to EWS using passed values
	 * 
	 * @param ewsEndpoint Exact URL to EWS endpoint
	 * @param username EWS username
	 * @param password EWS password
	 * @throws Exception
	 */
	public EWSClient(String ewsEndpoint, String username, String password) throws Exception {
		this(new URI(ewsEndpoint), ExchangeVersion.Exchange2010_SP2, username, password);
	}

	/**
	 * Connect to EWS using passed values
	 * 
	 * @param ewsUrl Exact URL to EWS endpoint
	 * @param ewsEndpoint URL to EWS endpoint
	 * @param exchangeVersion Version of Exchange server
	 * @param username Username used for logging in
	 * @param password Password used for logging in
	 * @throws Exception
	 */
	public EWSClient(URI ewsEndpoint, ExchangeVersion exchangeVersion, String username, String password) throws Exception {
		this.service = new ExchangeService(exchangeVersion);
		service.setCredentials(new WebCredentials(username, password));
		service.setUrl(ewsEndpoint);

		this.inbox = Folder.bind(service, WellKnownFolderName.Inbox);
	}

	/**
	 * Getter for Inbox
	 * @return Inbox
	 */
	public Folder getInbox() {
		return inbox;
	}

	/**
	 * Getter for ExchangeService
	 * @return ExchangeService
	 */
	public ExchangeService getService() {
		return this.service;
	}
}