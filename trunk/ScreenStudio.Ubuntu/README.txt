ScreenStudio
------------

To launch, execute the script ScreenStudio.sh
To launch with your own setup, java -jar ScreenStudio.jar custom.xml
To install a launcher on your desktop (Ubuntu), execute createDesktopIcon.sh

Custom configuration file
-------------------------
If you want to use your own configuration file, you can replate the default "default.xml" file or simply
make a copy and edit the ScreenStudio.sh to use your own file instead of "default.xml"

At the bottom of the script, edit 
	"$_java -jar ScreenStudio.jar default.xml" 
by	"$_java -jar ScreenStudio.jar custom.xml"

When updating to a new version of ScreenStudio, you will have to repeat this process
