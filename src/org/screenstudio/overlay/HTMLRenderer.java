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
package org.screenstudio.overlay;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JLabel;

/**
 *
 * @author patrick
 */
public class HTMLRenderer {

    private int width = 1280;
    private int height = 768;
    private File file = null;
    private final int verticalAlign;
    private final int horizontalAlign;

    public HTMLRenderer(int w, int h, int halign, int valign) {
        width = w;
        height = h;
        verticalAlign = valign;
        horizontalAlign = halign;
        try {
            file = File.createTempFile("screenstudio", ".png");
        } catch (IOException ex) {
            Logger.getLogger(HTMLRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        file.deleteOnExit();
    }

    public File getFile(URL url) throws IOException {
        java.io.DataInputStream din = new DataInputStream(url.openStream());
        byte[] buffer = new byte[64000];
        int count = din.read(buffer);
        String content = "";
        while (count != -1) {
            content += new String(buffer, 0, count);
            count = din.read(buffer);
        }
        return getFile(content);
    }

    public File getFile(String html) throws IOException {
        BufferedImage img;
        img = getImage(html);
        ImageIO.write(img, "png", file);
        return file;
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public BufferedImage getImage(URL url) throws IOException {
        java.io.DataInputStream din = new DataInputStream(url.openStream());
        byte[] buffer = new byte[64000];
        int count = din.read(buffer);
        String content = "";
        while (count != -1) {
            content += new String(buffer, 0, count);
            count = din.read(buffer);
        }
        return getImage(content);
    }
    
    public BufferedImage getImage(String html) throws IOException {
        JLabel label = new JLabel();
        label.setText(html);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        label.setSize(width, height);
        Graphics2D g = img.createGraphics();
        g.clearRect(0, 0, width, height);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.Src);
        label.setHorizontalAlignment(horizontalAlign);
        label.setVerticalAlignment(verticalAlign);
        label.setDoubleBuffered(false);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        try {
            label.paint(g);
        } catch (Exception ex) {
            System.err.println("Could not render HTML content...");
        }
        g.dispose();
        return img;
    }
    
    
}
