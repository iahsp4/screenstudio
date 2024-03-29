/*
 * Copyright (C) 2014 Patrick Balleux
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.screenstudio.services.sources;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.screenstudio.services.Command;
import org.screenstudio.services.Profile;

/**
 *
 * @author patrick
 */
public class Screen {

    private Rectangle size = new Rectangle(0, 0);
    private String id = "";
    private Webcam webcam = null;
    private Overlay overlay = null;
    private Microphone microphone = null;
    private Microphone monitor = null;
    private Profile profile = null;
    private Command command = null;
    private int screenIndex = -1;
    private int fps = 10;
    private boolean isRemote = false;
    

    @Override
    public String toString() {
        return getLabel();
    }

    public static Screen[] getSources() throws IOException, InterruptedException {
        java.util.ArrayList<Screen> list = new java.util.ArrayList<Screen>();
        System.out.println("Screen List:");
        list.add(new Screen());
        if (Screen.isOSX()) {
            list.addAll(getOSXDevices());
        } else {
            GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = g.getScreenDevices();
            int i = 1;
            for (GraphicsDevice d : devices) {
                System.out.println(d.getIDstring() + " " + d.getDefaultConfiguration().getBounds().toString().replaceAll("java.awt.Rectangle", ""));
                Screen s = new Screen();
                s.setId(d.getIDstring());
                s.setScreenIndex(i++);
                s.setSize(d.getDefaultConfiguration().getBounds());
                list.add(s);
            }
            Screen remote = new Screen();
            remote.isRemote = true;
            remote.id = "";
            remote.setScreenIndex(i++);
            remote.setSize(new Rectangle(720,480));
            list.add(remote);
        }
        return list.toArray(new Screen[list.size()]);
    }

    public boolean isRemote(){
        return isRemote;
    }
    public static Rectangle captureWindowArea() throws IOException {
        Rectangle r = new Rectangle();
        System.out.println("Capture Window Area");
        Process p = Runtime.getRuntime().exec("xwininfo");
        InputStream in = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        String line = reader.readLine();
        int x = 0, y = 0, w = 0, h = 0;
        while (line != null) {
            System.out.println(line);
            if (line.trim().startsWith("Absolute upper-left X:")) {
                x = new Integer(line.trim().replaceAll(" ", "").split(":")[1]);
            } else if (line.trim().startsWith("Absolute upper-left Y:")) {
                y = new Integer(line.trim().replaceAll(" ", "").split(":")[1]);
            } else if (line.trim().startsWith("Width:")) {
                w = new Integer(line.trim().replaceAll(" ", "").split(":")[1]);
            } else if (line.trim().startsWith("Height:")) {
                h = new Integer(line.trim().replaceAll(" ", "").split(":")[1]);
            }
            line = reader.readLine();
        }
        r.setSize(w, h);
        r.setLocation(x, y);
        in.close();
        isr.close();
        reader.close();
        p.destroy();
        return r;
    }

    /**
     * @return the size
     */
    public Rectangle getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Rectangle size) {
        this.size = size;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the webcam
     */
    public Webcam getWebcam() {
        return webcam;
    }

    /**
     * @param webcam the webcam to set
     */
    public void setWebcam(Webcam webcam) {
        this.webcam = webcam;
    }

    /**
     * @return the overlay
     */
    public Overlay getOverlay() {
        return overlay;
    }

    /**
     * @param overlay the overlay to set
     */
    public void setOverlay(Overlay overlay) {
        this.overlay = overlay;
    }

    /**
     * @return the microphone
     */
    public Microphone getMicrophone() {
        return microphone;
    }

    /**
     * @param microphone the microphone to set
     */
    public void setMicrophone(Microphone microphone) {
        this.microphone = microphone;
    }

    /**
     * @return the fps
     */
    public int getFps() {
        return fps;
    }

    /**
     * @param fps the fps to set
     */
    public void setFps(int fps) {
        this.fps = fps;
    }

    /**
     * @return the profile
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    /**
     * @return the command
     */
    public Command getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * @return the monitor
     */
    public Microphone getMonitor() {
        return monitor;
    }

    /**
     * @param monitor the monitor to set
     */
    public void setMonitor(Microphone monitor) {
        this.monitor = monitor;
    }

    /**
     * @return the screenIndex
     */
    public int getScreenIndex() {
        return screenIndex;
    }

    /**
     * @param screenIndex the screenIndex to set
     */
    public void setScreenIndex(int screenIndex) {
        this.screenIndex = screenIndex;
    }

    public String getLabel() {
        if (screenIndex == -1) {
            return "None";
        } else if (isRemote) {
            return "Remote Source";
        } else {
            return "Screen " + screenIndex;
        }
    }

    public static void main(String[] args) {
        try {
            Screen.captureWindowArea();
        } catch (IOException ex) {
            Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean isOSX() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.startsWith("mac os x");
    }

    private static ArrayList<Screen> getOSXDevices() throws IOException, InterruptedException {
        ArrayList<Screen> list = new ArrayList<Screen>();
        String command = "./ffmpeg -list_devices true -f avfoundation -i dummy";
        String line = "";
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        line = reader.readLine();
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = g.getScreenDevices();
        while (line != null) {
            if (line.endsWith("AVFoundation video devices:")) {
                // we have some audio sources
                line = reader.readLine();
                int index = 1;
                while (line != null && line.indexOf("input device") > 0 && !line.contains("audio devices")) {
                    if (line.contains("Capture screen")) {
                        Screen s = new Screen();
                        if (index < devices.length) {
                            GraphicsDevice d = devices[index - 1];
                            s.setSize(d.getDefaultConfiguration().getBounds());
                        } else {
                            GraphicsDevice d = devices[0];
                            s.setSize(d.getDefaultConfiguration().getBounds());
                        }
                        s.screenIndex = index;
                        s.id = "";
                        String[] parts = line.split(" ");
                        System.out.println(line);
                        for (int i = parts.length - 1; i >= 0; i--) {
                            if (parts[i].startsWith("[")) {
                                // reached device id
                                s.id = parts[i].substring(1, parts[i].length() - 1);
                                break;
                            }
                        }
                        System.out.println(s.getLabel());
                        list.add(s);
                    }
                    line = reader.readLine();
                }
            } else {
                line = reader.readLine();
            }
        }
        reader.close();
        isr.close();
        in.close();
        p.destroy();

        return list;
    }
    public String getEncoderName(){
        if (isRemote){
            if (webcam == null){
                return "REMOTE";
            } else {
                return "WEBCAMREMOTE";
            }
        } else {
            if (webcam == null){
                return "DESKTOP";
            } else {
                if (screenIndex == -1){
                    return "WEBCAM";
                } else {
                    return "WEBCAMDESKTOP";
                }
            }
        }
    }
}
