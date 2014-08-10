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
package org.screenstudio.services;

import org.w3c.dom.Node;

/**
 *
 * @author patrick
 */
public class Video {

    private String preset = "ultrafast";
    private String codec = "libx264";
    private String bitrate = "6000";

    private String minrate = "0";
    private String maxrate = "0";
    private String fps = null;

    public Video(Node xml) {
        preset = xml.getAttributes().getNamedItem("preset").getNodeValue();
        codec = xml.getAttributes().getNamedItem("codec").getNodeValue();
        bitrate = xml.getAttributes().getNamedItem("bitrate").getNodeValue();
        if (xml.getAttributes().getNamedItem("minrate") != null) {
            minrate = xml.getAttributes().getNamedItem("minrate").getNodeValue();
        }
        if (xml.getAttributes().getNamedItem("maxrate") != null) {
            maxrate = xml.getAttributes().getNamedItem("maxrate").getNodeValue();
        }
        if (xml.getAttributes().getNamedItem("fps") != null) {
            fps = xml.getAttributes().getNamedItem("fps").getNodeValue();
        }
    }

    public void setPreset(String p){
        preset = p;
    }
    @Override
    public String toString() {
        return "Video: " + getCodec();
    }

    /**
     * @return the preset
     */
    public String getPreset() {
        return preset;
    }

    /**
     * @return the codec
     */
    public String getCodec() {
        return codec;
    }

    /**
     * @return the bitrate
     */
    public String getBitrate() {
        return bitrate;
    }

    /**
     * @return the minrate
     */
    public String getMinrate() {
        return minrate;
    }

    /**
     * @return the maxrate
     */
    public String getMaxrate() {
        return maxrate;
    }

    /**
     * @return the fps
     */
    public String getFps() {
        return fps;
    }

    /**
     * @param fps the fps to set
     */
    public void setFps(String fps) {
        this.fps = fps;
    }

}
