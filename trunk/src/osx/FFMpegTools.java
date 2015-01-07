/*
 * Copyright (C) 2015 patrickballeux
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
package osx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrickballeux
 */
public class FFMpegTools {
    
    
    
    public static boolean checkForFFMPEG(){
        File app = new File(System.getProperty("user.home"),"Applications");
        File ffmpeg = new File(app,"ffmpeg");
        boolean found = false;
        if (ffmpeg.exists()){
            found=true;
        } else {
            try {
                InputStream in = FFMpegTools.class.getResourceAsStream("ffmpeg");
                byte[] buffer = new byte[65000];
                FileOutputStream out = new FileOutputStream(ffmpeg);
                int count = in.read(buffer);
                while (count != -1){
                    out.write(buffer, 0, count);
                    count = in.read(buffer);
                }
                out.close();
                in.close();
                found=true;
            } catch (IOException ex) {
                Logger.getLogger(FFMpegTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return found;
    }
    public static String getBinaryPath(){
        return new File(System.getProperty("user.home"),"Applications").getAbsolutePath();
    }
}
