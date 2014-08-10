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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrick
 */
public class Microphone {

    private String device = "";
    private String description = "";

    @Override
    public String toString() {
        return getDescription();
    }

    public static Microphone[] getSources() throws IOException {
        java.util.ArrayList<Microphone> list = new java.util.ArrayList<Microphone>();
        System.out.println("Source Audio List:");
        Process p = Runtime.getRuntime().exec("pactl list sources");
        InputStream in = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        String line = reader.readLine();
        while (line != null) {
            if (line.trim().toUpperCase().matches("^.* \\#\\d{1,2}$")) {
                reader.readLine();
                line = reader.readLine();
                String l = line.trim().split(":")[1];
                //Ignoring already loaded virtual module
                if (!l.contains("ScreenStudio")) {
                    Microphone s = new Microphone();
                    System.out.println(l);
                    s.device = l.trim();
                    line = reader.readLine();
                    l = line.trim().split(":")[1];
                    s.description = l.trim();
                    list.add(s);
                }
            }
            line = reader.readLine();
        }
        in.close();
        isr.close();
        reader.close();
        p.destroy();
        return list.toArray(new Microphone[list.size()]);
    }

    public static String getVirtualAudio(Microphone source1, Microphone source2) throws IOException, InterruptedException {
        ArrayList<String> loadedModules = new ArrayList<String>();
        Process p = Runtime.getRuntime().exec("pactl list modules");
        InputStream in = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);

        String line = reader.readLine();
        while (line != null) {
            if (line.trim().toUpperCase().matches("^.* \\#\\d{1,4}$")) {
                String id = line.split("#")[1];
                //Skipping module name
                reader.readLine();
                //Reading arguments...
                line = reader.readLine();
                if (line.contains("ScreenStudio")) {
                    loadedModules.add(id);
                }
            }
            line = reader.readLine();
        }
        in.close();
        isr.close();
        reader.close();
        p.destroy();
        // Unloading previous modules...
        for (int i = loadedModules.size()-1;i >=0;i--) {
            execPACTL("pactl unload-module " + loadedModules.get(i));
            pause(100);
        }
        if (source1 != null && source2 != null) {
            execPACTL("pactl load-module module-null-sink sink_name=ScreenStudio sink_properties=device.description=\"ScreenStudio\"");
            pause(100);
            execPACTL("pactl load-module module-loopback sink=ScreenStudio source=" + source1.device);
            pause(100);
            execPACTL("pactl load-module module-loopback sink=ScreenStudio source=" + source2.device);
            pause(100);
        }
        return "ScreenStudio.monitor";
    }

    private static void pause(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Logger.getLogger(Microphone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static String execPACTL(String command) throws IOException, InterruptedException {
        String output;
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        output = reader.readLine();
        reader.close();
        isr.close();
        in.close();
        p.waitFor();
        p.destroy();
        System.out.println("Output: " + output);
        return output;
    }

    /**
     * @return the device
     */
    public String getDevice() {
        return device;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
