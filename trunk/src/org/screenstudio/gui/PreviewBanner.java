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
package org.screenstudio.gui;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import static java.awt.Image.*;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 *
 * @author patrick
 */
public class PreviewBanner extends javax.swing.JDialog {

    private String screenID = "";
    private final BufferedImage image;

    /**
     * Creates new form PreviewBanner
     *
     * @param img
     * @param screenID
     * @param width
     * @param height
     * @param parent
     * @param modal
     */
    public PreviewBanner(BufferedImage img, String screenID,
            int width, int height,
            java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.image = img;
        this.screenID = screenID;
        this.setTitle("ScreenStudio - Preview");
        generatePreview(width, height);
        this.pack();
    }

    private void generatePreview(int width, int height) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        GraphicsDevice dev = null;
        for (GraphicsDevice d : devices) {
            if (d.getIDstring().equals(screenID)) {
                dev = d;
                break;
            }
        }
        BufferedImage capture = null;
        try {
            if (dev != null) {
                capture = new Robot().createScreenCapture(dev.getDefaultConfiguration().getBounds());
                Graphics2D g = capture.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

                g.drawImage(image, 0, 0, this);
                g.dispose();
                Image resized = capture.getScaledInstance(width, height, SCALE_SMOOTH);
                lblPreview.setIcon(new ImageIcon(resized));
                lblPreview.setSize(width, height);
            }
        } catch (AWTException ex) {
            Logger.getLogger(PreviewBanner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblPreview = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        lblPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPreview.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        lblPreview.setDoubleBuffered(true);
        lblPreview.setIconTextGap(0);
        getContentPane().add(lblPreview, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblPreview;
    // End of variables declaration//GEN-END:variables
}
