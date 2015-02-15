echo "What is the new version?"
read -e -p "Enter version:" -i "" VERSION
echo $VERSION

echo "Updating version..."
sed "s/version = \x22.*\x22/version = \x22ScreenStudio $VERSION\x22/g" src/org/screenstudio/gui/Version.java>src/org/screenstudio/gui/VersionNEW.java
rm src/org/screenstudio/gui/Version.java
mv src/org/screenstudio/gui/VersionNEW.java src/org/screenstudio/gui/Version.java
echo "Building Binaries"
ant -f . -Dnb.internal.action.name=rebuild clean jar

echo "Cleaning up files..."
rm ScreenStudio.Ubuntu/*~
rm ScreenStudio.app/Contents/MacOS/*~
rm Packages/*
rmdir Packages

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



