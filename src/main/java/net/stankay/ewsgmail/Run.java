package net.stankay.ewsgmail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.property.complex.MimeContent;
import microsoft.exchange.webservices.data.search.ItemView;

/**
 * MS Exchange -> Gmail proxy that fetches mails from Exchange using
 * Exchange Web Services and stores them into chosen gmail account's Inbox
 */
public final class Run {

    private static Options options;
    private static Properties config = new Properties();
    private static CommandLine cmd;
    private static HelpFormatter formatter = new HelpFormatter();
    private static GmailClient gmailClient;
    private static String appName;
    private static String appVersion;
    private static final Logger LOG = Logger.getLogger(Run.class.getSimpleName());

    static {
        options = new Options();
        options.addOption("c", "config" , true , "Path to config file, mandatory");
        options.addOption("f", "fetch"  , false, "Fetch unread e-mails from EWS and save them to GMail");
        options.addOption("h", "help"   , false, "Print this help");
        options.addOption("l", "labels" , false, "List GMail labels");
        options.addOption("r", "readfile", true, "Read e-mail from file and store it to GMail");
        options.addOption("s", "secret" , true , "Path to file with GMail secret, mandatory");
        options.addOption("v", "version", false, "Display application version");
    }

    private Run() {}

    /**
     * Validate command line and print eventual error messages
     * @param args Command line args
     */
    private static void processCommandLine(String[] args) {
        if (args.length < 1 || cmd.hasOption('h')) {
            formatter.printHelp("ews-gmail-proxy [args]", options);
            System.exit(0);
        }

        if (!cmd.hasOption('c')) {
            printHelp("Missing config file", -2);
        }

        if (!cmd.hasOption('s')) {
            printHelp("Missing GMail secret file", -3);
        }

        if (!cmd.hasOption('f') && !cmd.hasOption('l') && !cmd.hasOption('r')
                || (cmd.hasOption('f') && cmd.hasOption('l'))
                || (cmd.hasOption('f') && cmd.hasOption('r'))
                || (cmd.hasOption('l') && cmd.hasOption('r'))
                ) {
            printHelp("Choose action, must be exactly one of -f, -l, -r", -4);
        }
    }

    /**
     * Read config file and load into Properties structure.
     * Then validate config
     *
     * @throws IOException
     */
    private static void processConfig() throws IOException {
        try {
            config.load(new FileInputStream(new File(cmd.getOptionValue('c'))));
        } catch (FileNotFoundException e) {
            printHelp(e.getMessage(), -5);
        }

        if (!configIsValid()) {
            System.err.println("Error: Config is invalid: " + cmd.getOptionValue('c'));
            System.exit(-1);
        }
    }

    /**
     * Validate if passed config has all the required keys
     *
     * @return True if it contains all required key, false otherwise
     */
    private static boolean configIsValid() {
        return     config.containsKey("ewsUrl")
                && config.containsKey("ewsUsername")
                && config.containsKey("ewsPassword")
                && config.containsKey("gmailAddress")
                && config.containsKey("gmailLabelIds");
    }

    /**
     * Print error message and usage information to stderr.
     * Return passed return code.
     *
     * @param message Text of error to be displayed
     * @param returnCode Value of return code to be returned
     */
    private static void printHelp(String message, int returnCode) {
        System.err.println(message);
        System.err.println();
        formatter.printHelp(appName + " [args]", options);
        System.exit(returnCode);
    }

    /**
     * Fetch EWS messages, upload them to GMail
     *
     * @throws Exception
     */
    private static void insertMessage() throws Exception {
        EWSClient ews = null;
        try {
            ews = new EWSClient(config.getProperty("ewsUrl"), config.getProperty("ewsUsername"), config.getProperty("ewsPassword"));
        } catch (Exception e) {
            String errorMessage = "From: "+config.getProperty("gmailAddress")+"\n";
            errorMessage += "To: "+config.getProperty("gmailAddress")+"\n";
            errorMessage += "Subject: ews-gmail-proxy - Problem connecting to EWS\n";
            errorMessage += "\nThere was a problem connecting to Exchange Web Services";

            if (config.getProperty("reportErrorsToGmail") != null && Boolean.parseBoolean(config.getProperty("reportErrorsToGmail"))) {
                gmailClient.insertUnreadMessage(errorMessage + ": " + e.getMessage(), config.getProperty("gmailLabelIds"));
            }

            LOG.log(Level.SEVERE, errorMessage, e);
            return;
        }

        Folder inbox = ews.getInbox();

        LOG.info(String.format("Querying EWS at %s with username %s: total/unread messages = %s/%s",
                config.getProperty("ewsUrl"),
                config.getProperty("ewsUsername"),
                inbox.getTotalCount(),
                inbox.getUnreadCount()));

        for (Item i : inbox.findItems(new ItemView(100,0))) {
            EmailMessage e = (EmailMessage)i;

            if (!e.getIsRead()) {
                e.load(new PropertySet(ItemSchema.MimeContent));
                MimeContent mimeContent = e.getMimeContent();

                gmailClient.insertUnreadMessage(new String(mimeContent.getContent()), config.getProperty("gmailLabelIds"));

                e.setIsRead(true);
                e.update(ConflictResolutionMode.AutoResolve);
            }
        }
    }

    /**
     * Read e-mail message from file and put into GMail inbox
     */
    private static void readAndInsertMessage() {

        String filename = cmd.getOptionValue('r');

        try {
            byte[] content = Files.readAllBytes(Paths.get(filename));
            gmailClient.insertUnreadMessage(new String(content), config.getProperty("gmailLabelIds"));
        } catch (Exception e) {
            System.err.println("ERROR: could not read " + filename + ". " + e);
            System.exit(-5);
        }
    }

    public static void main(String[] args) throws Exception {
        readVersion();

        try {
            cmd = new DefaultParser().parse( options, args);
        } catch (Exception e) {
            printHelp(e.getMessage(), -1);
        }

        if (cmd.hasOption("v")) {
            System.out.println(appName + " " + appVersion);
            return;
        }

        processCommandLine(args);
        processConfig();

        gmailClient = new GmailClient(config.getProperty("gmailAddress"), cmd.getOptionValue('s'));

        if (cmd.hasOption("f")) {
            insertMessage();
        } if (cmd.hasOption("r")) {
            readAndInsertMessage();
        } else if (cmd.hasOption("l")) {
            gmailClient.listLabels();
        }
    }

    private static void readVersion() throws IOException {
        Properties versionProps = new Properties();
        versionProps.load(Run.class.getResourceAsStream("/version.properties"));
        appVersion = versionProps.getProperty("appVersion");
        appName = versionProps.getProperty("appName");
    }
}
