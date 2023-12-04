This repository is a template for creating a confluence plugin for versions other the the default 6.1.4,
which is the only version Atlassian 'supports' with their sdk.


These are the steps required in order to build this Atlassian Plugin:

* Install JDK11 (OpenJDK - Adoptium temurin)[https://adoptium.net/temurin/releases/]
* Install atlassian-sdk from (here)[https://developer.atlassian.com/server/framework/atlassian-sdk/downloads/]
* Install Apache Maven v3.6.3, the bundled version in the atlas-sdk (v3.5.4) doesn't support JDK11 and or the dependencies
* Set ATLAS_MVN Environment variable to the installed binary of Maven v3.6.3
* Navigate to the installation directory of the atlas-sdk - atlassian-plugin-sdk-8.2.7\apache-maven-3.5.4\conf\
  Take the settings.xml file and copy it (or it's settings) into the installation directory of Maven v3.6.3, overwriting the default config file

The pom.xml file already includes the required dependencies for Confluence 8.1.3,
those include `javax.inject`, `atlassian-spring-scanner-annotation`

and the plugin `atlassian-spring-scanner-maven-plugin` required for running scanner, which 'hoists'/reflects the confluence Classes
into the servlet context of the module.

You have successfully created an Atlassian Plugin!

Here are the SDK commands you'll use immediately:

* atlas-run   --version 8.1.3  # installs this plugin into the product and starts it on localhost
* atlas-debug --version 8.1.3  # same as atlas-run, but allows a debugger to attach at port 5005
* atlas-help  # prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
