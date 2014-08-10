/*
 * Copyright (C) 2014 patrick
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.screenstudio.services.Layout;

/**
 *
 * @author patrick
 */
public class Webcam {

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the offset
     */
    public double getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(double offset) {
        this.offset = offset;
    }

    private int width = 320;
    private int height = 240;
    private int fps = 10;
    private File device = null;
    private Layout layout = null;
    private String description = "";
    private String id = "";
    private double offset = 0;

    /**
     * @return the location
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * @param layout the location to set
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    private Webcam(File dev, String id, String desc) {
        device = dev;
        description = desc;
        this.id = id;
    }

    public static Webcam[] getSources() throws IOException {
        java.util.ArrayList<Webcam> list = new java.util.ArrayList<Webcam>();
        System.out.println("Webcam List:");
        Webcam w = new Webcam(null, "None", "None");
        list.add(w);
        File dev = new File("/dev");
        if (dev.isDirectory()) {
            File[] files = dev.listFiles();
            for (File f : files) {
                if (f.getName().startsWith("video")) {
                    System.out.println(f.getName());
                    Webcam source = new Webcam(f, f.getName(), "");
                    list.add(source);
                }
            }
        }

        for (Webcam s : list) {
            File desc = new File("/sys/class/video4linux", s.id + "/name");
            if (desc.exists()) {
                InputStream in = desc.toURI().toURL().openStream();
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();
                s.description = new String(buffer);
            }
        }
        return list.toArray(new Webcam[list.size()]);
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the heigh
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the heigh to set
     */
    public void setHeight(int height) {
        this.height = height;
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
     * @return the device
     */
    public File getDevice() {
        return device;
    }
    
    @Override
    public String toString(){
        return description;
    }
}
