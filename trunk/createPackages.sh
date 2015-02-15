echo "Building Binaries"
ant -f . -Dnb.internal.action.name=rebuild clean jar

echo "Cleaning up files..."
rm ScreenStudio.Ubuntu/*~
rm ScreenStudio.app/Contents/MacOS/*~
rm Packages/*
rmdir Packages

echo "Getting version number..."
VERSION=$(java -cp dist/ScreenStudio.jar org.screenstudio.gui.Version)
echo $VERSION
echo "Copying files..."
cp dist/ScreenStudio.jar ScreenStudio.app/Contents/MacOS/ScreenStudio.jar
cp dist/ScreenStudio.jar ScreenStudio.Ubuntu/ScreenStudio.jar
cp dist/lib/* ScreenStudio.app/Contents/MacOS/lib
cp dist/lib/* ScreenStudio.Ubuntu/lib

mkdir Packages
echo "Archiving MacOS App"
zip -r Packages/ScreenStudio-$VERSION-bin-OSX.zip ScreenStudio.app
echo "Archiving Ubuntu App"
tar cvzf Packages/ScreenStudio-$VERSION-bin-Ubuntu.tar.gz ScreenStudio.Ubuntu/



