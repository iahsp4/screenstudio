# Set the working directory
DIR=$(pwd)

# Error message for NO JAVA dialog
ERROR_TITLE="Cannot launch $APP_NAME"
ERROR_MSG="$APP_NAME requires Java version $JAVA_MAJOR.$JAVA_MINOR or later to run."
DOWNLOAD_URL="http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html"

# Is Java installed?
if type java; then
    _java="java"
elif type "$JAVA_HOME/bin/java" ; then
    _java="$JAVA_HOME/bin/java"
else
	echo "$ERROR_TITLE"
	echo "$ERROR_MSG"
	exit 1
fi

# Run the application
cd $DIR
echo $DIR
#chmod +x ./ffmpeg
$_java -jar ScreenStudio.jar default.xml


