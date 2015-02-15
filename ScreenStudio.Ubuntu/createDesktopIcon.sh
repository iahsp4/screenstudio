DIR=$(pwd)
echo "[Desktop Entry]">ScreenStudio.desktop
echo "Encoding=UTF-8">>ScreenStudio.desktop
echo "Name=ScreenStudio">>ScreenStudio.desktop
echo "Comment=Streaming, made easy!">>ScreenStudio.desktop
echo "Path=$DIR">>ScreenStudio.desktop
echo "Exec=$DIR/ScreenStudio.sh">>ScreenStudio.desktop
echo "Icon=$DIR/logo.png">>ScreenStudio.desktop
echo "Categories=Application;">>ScreenStudio.desktop
echo "Version=1.1.3">>ScreenStudio.desktop
echo "Type=Application">>ScreenStudio.desktop
echo "Terminal=0">>ScreenStudio.desktop
chmod +x ScreenStudio.desktop
mv ScreenStudio.desktop $(xdg-user-dir DESKTOP)/ScreenStudio.desktop


