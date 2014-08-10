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
public class Profile {

    private String name;
    private String bufferSize;
    private String muxer;
    private String format;
    private String group;
    private Video video;
    private Audio audio;

    public Profile(Node xml) {
        name = xml.getAttributes().getNamedItem("name").getNodeValue();
        bufferSize = xml.getAttributes().getNamedItem("buffersize").getNodeValue();
        muxer = xml.getAttributes().getNamedItem("muxer").getNodeValue();
        format = xml.getAttributes().getNamedItem("format").getNodeValue();
        group = xml.getAttributes().getNamedItem("group").getNodeValue();
        Node node = xml.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals("video")){
                    video = new Video(node);
                } else if (node.getNodeName().equals("audio")){
                    audio = new Audio(node);
                }
            }
            node = node.getNextSibling();
        }
    }

    @Override
    public String toString() {
        return getName() + ": " + getVideo().toString() +" ("+format+"@"+ getVideo().getBitrate()+"k)"+ " / " + getAudio().toString() + " ("+getAudio().getBitrate()+"k)";
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the bufferSize
     */
    public String getBufferSize() {
        return bufferSize;
    }

    /**
     * @return the muxer
     */
    public String getMuxer() {
        return muxer;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the video
     */
    public Video getVideo() {
        return video;
    }

    /**
     * @return the audio
     */
    public Audio getAudio() {
        return audio;
    }

}
