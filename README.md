EWS-Gmail proxy
===============
EWS-GMail proxy is a command line application that synchronizes Exchange account
(using Exchange Web Services AKA EWS) to GMail account. It searches for unread
messages in Exchange user's Inbox folder, inserts them into GMail Inbox and
marks Exchange messages as read.

When invoked, the application fetches/lists only once, then terminates. For
periodic fetching, please use scheduler/cron
or enclose app in your own script.

Prerequisities
--------------
* Exchange account on server with EWS enabled and is visible from your network
* Gmail account
* Working JDK 1.7+
* Maven

Building
---------
App is built using Maven. Use `mvn package` to build the distribution. Then use
script `ews-gmail-proxy.sh` or `ews-gmail-proxy.bat` to run the app depending on
your platform.

Configuration
-------------
Before running the app for the first time, please enter proper values into
`ews-gmail-proxy.properties` config file. You can leave `gmailLabelIds` empty
for now.

Before first run
----------------
You need to create credentials so the application can login to GMail. Follow
these steps:

1. Go to https://console.developers.google.com/project
2. Click 'Create project' and enter name of the project, e.g. 'EWS Gmail Proxy'
3. Click 'Use Google APIs'
4. Find GMail API and click Enable
5. Click 'Go to credentials'
6. From 'Where will you be calling the API from?' choose 'Other UI'
7. From 'What data will you be accessing?' choose 'User data'
8. Click 'What credentials do I need?'
9. Fill in form 'Add credentials to your project'
10. Click 'Download credentials' and finish the form. A JSON file with credentials
will be downloaded to your disk.

First Run
---------
It is necessary to run the app for the first time on a desktop computer, because
you need to confirm that you want to allow this app to access your account in a
browser. After initial confirmation this is no longer necessary, unless you
delete folder `HOME/.credentials/ews-gmail-proxy`.

For the first run, run command ` ews-gmail-proxy.sh -c [path to config] -s
[path to JSON with secret] -l`. This command should display a list of labels
created for given GMail account. You can now configure `gmailLabelIds` config
option.

Regular runs
---------------
For full list of options, just run the app without any arguments. In case you
are intending to run the app on remote server, make sure you upload
`HOME/.credentials` as well, otherwise you will be not able to authenticate in
case of headless server.

Troubleshooting
---------------
Send me an e-mail :) This is alpha-stage code, tested only against one EWS,
one GMail account, many things can go wrong.

Known issues
------------
* Application does not group EWS emails that were in one thread into GMail
threads
* So far tested only against Exchange2010_SP2

License
-------
This software is released under GNU GPL v3 license.

Contributing
------------
Contributions to the source code are welcome.

Liability
---------
You are using this code at your own responsibility and risk. I am not
responsible for any damage done by using this application.

Michal Stankay <michal@stankay.net>
