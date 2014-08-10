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
public class Audio {

    private String codec = "aac";
    private String bitrate = "224";
    private String channel = "2";
    private String rate = "44100";

    public Audio(Node xml) {
        codec = xml.getAttributes().getNamedItem("codec").getNodeValue();
        bitrate = xml.getAttributes().getNamedItem("bitrate").getNodeValue();
        channel = xml.getAttributes().getNamedItem("channel").getNodeValue();
        rate = xml.getAttributes().getNamedItem("rate").getNodeValue();
    }

    @Override
    public String toString() {
        return "Audio: " + getCodec();
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
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @return the rate
     */
    public String getRate() {
        return rate;
    }

}
