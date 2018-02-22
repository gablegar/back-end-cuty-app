# back-end-cuty-app
This is an Rest API that is used from a reservation mobile app or a web app to do things like
login, booking, calendar, reserving and CRUD operations to a Elastic Search database
this api is capable of sending emails.

This will need a local Elastic Search database with the proper schemas created.

if JAF dependency not available en mvn repo, please download manually and install in local repo

mvn install:install-file -DgroupId=javax.activation -DartifactId=activation -Dversion=1.0.2 -Dpackaging=jar -Dfile=activation-1.0.2.jar

if ssl communications error please deactivate antivirus