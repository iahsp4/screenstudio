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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.screenstudio.services.sources.Microphone;
import org.screenstudio.services.sources.Screen;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author patrick
 */
public class Encoder {

    public static String Author = "";
    public static String OS = "";
    public static String Backend = "";
    public static String Version = "";

    private final ArrayList<Profile> profiles = new ArrayList<Profile>();
    private final ArrayList<Server> servers = new ArrayList<Server>();
    private final TreeMap<String, Command> commands = new TreeMap<String, Command>();
    private final ArrayList<Layout> layouts = new ArrayList<Layout>();
    private ArrayList<String> presets = new ArrayList<String>();

    private final String serviceName;
    private final String target;

    public static String parse(Screen s, Server server, String streamKey, File videoFolder) throws IOException, InterruptedException {
        String command = parseGeneric(s, videoFolder);
        if (server == null) {
            command = command.replaceAll("@OUTSINK", streamKey);
        } else {
            command = command.replaceAll("@OUTSINK", server.getUrl() + "/" + streamKey);
        }
        return command;
    }

    public static String parse(Screen s, File f, File videoFolder) throws IOException, InterruptedException {
        String command = parseGeneric(s, videoFolder);
        command = command.replaceAll("@OUTSINK", f.getAbsolutePath());
        return command;
    }

    private static String parseGeneric(Screen s, File videoFolder) throws IOException, InterruptedException {
        String command = s.getCommand().getCommandLine();

        if (command.contains("@SCREENDEV")) {
            command = command.replaceAll("@SCREENDEV", s.getId());
        }
        if (command.contains("@REMOTEURL")){
            command = command.replaceAll("@REMOTEURL",s.getId());
        }
        if (command.contains("@LOCALSINK")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String filename = "screenstudio-" + format.format(new Date()) + "." + s.getProfile().getMuxer();
            if (!videoFolder.exists()) {
                videoFolder = new File(System.getProperty("user.home"), "ScreenStudio");
                if (!videoFolder.exists()) {
                    videoFolder.mkdir();
                }
            }
            File out = new File(videoFolder, filename);
            command = command.replaceAll("@LOCALSINK", out.getAbsolutePath());
        }
        if (s.getWebcam() != null && s.getWebcam().getDevice() != null) {
            command = command.replaceAll("@WEBCAMFPS", s.getWebcam().getFps() + "");
            command = command.replaceAll("@WEBCAMOFFSET", s.getWebcam().getOffset() + "");
            command = command.replaceAll("@WEBCAMW", s.getWebcam().getWidth() + "");
            command = command.replaceAll("@WEBCAMH", s.getWebcam().getHeight() + "");
            command = command.replaceAll("@WEBCAMDEV", s.getWebcam().getDevice());
            command = command.replaceAll("@WEBCAMLAYOUT", s.getWebcam().getLayout().getValue());
        }
        if (s.getOverlay() != null) {
            command = command.replaceAll("@BANNERSRC", s.getOverlay().getImage().getAbsolutePath());
        }
        if (s.getProfile().getVideo().getFps() == null) {
            command = command.replaceAll("@OUTFPS", s.getFps() + "");
        } else {
            command = command.replaceAll("@OUTFPS", s.getProfile().getVideo().getFps());
        }
        command = command.replaceAll("@DESKTOPW", ((int) s.getSize().getWidth()) + "");
        command = command.replaceAll("@DESKTOPH", ((int) s.getSize().getHeight()) + "");
        command = command.replaceAll("@DESKTOPFPS", s.getFps() + "");
        command = command.replaceAll("@DESKTOPX", ((int) s.getSize().getX()) + "");
        command = command.replaceAll("@DESKTOPY", ((int) s.getSize().getY()) + "");

        //using virtual device...
        if (s.getMicrophone() != null && s.getMonitor() != null) {
            String virtual = Microphone.getVirtualAudio(s.getMicrophone(), s.getMonitor());
            System.out.println("Using Virtual Input Device " + virtual);
            command = command.replaceAll("@PULSEDEV", virtual);
        } else if (s.getMicrophone() != null) {
            command = command.replaceAll("@PULSEDEV", s.getMicrophone().getDevice());
        } else if (s.getMonitor() != null) {
            command = command.replaceAll("@PULSEDEV", s.getMonitor().getDevice());
        } else {
            command = command.replaceAll("@PULSEDEV", "default");
        }

        int outputWidth = (int) s.getSize().getWidth();
        int outputHeight = (int) s.getSize().getHeight();

        if (s.getProfile().getFormat().equals("1080p")) {
            outputHeight = 1080;
            outputWidth = (int) (s.getSize().getWidth() * outputHeight / s.getSize().getHeight());
        } else if (s.getProfile().getFormat().equals("720p")) {
            outputHeight = 720;
            outputWidth = (int) (s.getSize().getWidth() * outputHeight / s.getSize().getHeight());
        } else if (s.getProfile().getFormat().equals("480p")) {
            outputHeight = 480;
            outputWidth = (int) (s.getSize().getWidth() * outputHeight / s.getSize().getHeight());
        } else if (s.getProfile().getFormat().equals("360p")) {
            outputHeight = 360;
            outputWidth = (int) (s.getSize().getWidth() * outputHeight / s.getSize().getHeight());
        } else if (s.getProfile().getFormat().equals("240p")) {
            outputHeight = 240;
            outputWidth = (int) (s.getSize().getWidth() * outputHeight / s.getSize().getHeight());
        }

        //To ensure that the width is even
        if (outputWidth % 2 != 0) {
            outputWidth -= 1;
        }
        if (outputHeight % 2 != 0) {
            outputHeight -= 1;
        }
        if (s.getProfile().getGroup().length() > 0) {
            command = command.replaceAll("@GROUP", (Integer.parseInt(s.getProfile().getGroup()) * s.getFps()) + "");
        }
        command = command.replaceAll("@PRESET", s.getProfile().getVideo().getPreset());
        command = command.replaceAll("@OUTW", outputWidth + "");
        command = command.replaceAll("@OUTH", outputHeight + "");
        command = command.replaceAll("@OUTAUDIORATE", s.getProfile().getAudio().getRate());
        command = command.replaceAll("@OUTVIDEORATE", s.getProfile().getVideo().getBitrate());
        command = command.replaceAll("@OUTAUDIOBITRATE", s.getProfile().getAudio().getBitrate());
        command = command.replaceAll("@VIDEOCODEC", s.getProfile().getVideo().getCodec());
        command = command.replaceAll("@AUDIOCODEC", s.getProfile().getAudio().getCodec());
        command = command.replaceAll("@BUFFERSIZE", s.getProfile().getBufferSize());
        command = command.replaceAll("@MUXER", s.getProfile().getMuxer());
        return command;
    }

    private Encoder(Node xml) {
        serviceName = xml.getAttributes().getNamedItem("name").getNodeValue();
        target = xml.getAttributes().getNamedItem("target").getNodeValue();
        Node node = xml.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals("server")) {
                    servers.add(new Server(node));
                } else if (node.getNodeName().equals("profile")) {
                    profiles.add(new Profile(node));
                } else if (node.getNodeName().equals("commands")) {
                    Node cNode = node.getFirstChild();
                    while (cNode != null) {
                        if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                            if (cNode.getNodeName().equals("command")) {
                                Command c = new Command(cNode);
                                commands.put(c.getName(), c);
                            } else if (cNode.getNodeName().equals("layout")) {
                                layouts.add(new Layout(cNode));
                            }
                        }
                        cNode = cNode.getNextSibling();
                    }
                } else if (node.getNodeName().equals("presets")) {
                    Node cNode = node.getFirstChild();
                    while (cNode != null) {
                        if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                            if (cNode.getNodeName().equals("preset")) {
                                presets.add(cNode.getTextContent());

                            }
                        }
                        cNode = cNode.getNextSibling();
                    }
                }
            }
            node = node.getNextSibling();
        }
    }

    public static Encoder[] getEncoders() throws ParserConfigurationException, SAXException, IOException {
        File customConfig = new File(System.getProperty("user.home"), ".screenstudio/Encoders.xml");
        if (customConfig.exists()) {
            return getEncoders(customConfig);
        } else {
            return getEncoders(null);
        }
    }

    public static Encoder[] getEncoders(File f) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<Encoder> list = new ArrayList<Encoder>();

        if (f != null) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream in = f.toURI().toURL().openStream();

            Document doc = builder.parse(in);
            NodeList setting = doc.getElementsByTagName("services");
            for (int i = 0; i < setting.getLength(); i++) {
                Node node = setting.item(i);
                if (node.getAttributes().getNamedItem("author") != null) {
                    Author = node.getAttributes().getNamedItem("author").getNodeValue();
                }
                if (node.getAttributes().getNamedItem("os") != null) {
                    OS = node.getAttributes().getNamedItem("os").getNodeValue();
                }
                if (node.getAttributes().getNamedItem("backend") != null) {
                    Backend = node.getAttributes().getNamedItem("backend").getNodeValue();
                }
                if (node.getAttributes().getNamedItem("version") != null) {
                    Version = node.getAttributes().getNamedItem("version").getNodeValue();
                }
            }
            NodeList nodes = doc.getElementsByTagName("service");
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(new Encoder(nodes.item(i)));
            }
            in.close();
        }
        return list.toArray(new Encoder[list.size()]);
    }

    @Override
    public String toString() {
        return serviceName;
    }

    /**
     * @return the profiles
     */
    public Profile[] getProfiles() {
        return profiles.toArray(new Profile[profiles.size()]);
    }

    /**
     * @return the servers
     */
    public Server[] getServers() {
        return servers.toArray(new Server[servers.size()]);
    }

    public ArrayList<String> getPresets() {
        return presets;
    }

    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    public TreeMap<String, Command> getCommands() {
        return commands;
    }

    /**
     * @param type
     * @return the layouts
     */
    public Layout[] getLayouts(String type) {
        ArrayList<Layout> subList = new ArrayList<Layout>();
        for (Layout l : layouts) {
            if (l.getType().equals(type)) {
                subList.add(l);
            }
        }
        return subList.toArray(new Layout[subList.size()]);
    }
}
