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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.screenstudio.gui;

import com.hexidec.ekit.EkitCore;
import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.screenstudio.overlay.HTMLRenderer;
import org.screenstudio.remote.Listener;
import org.screenstudio.remote.WebRemote;
import org.screenstudio.services.Command;
import org.screenstudio.services.Encoder;
import org.screenstudio.services.Layout;
import org.screenstudio.services.Profile;
import org.screenstudio.services.Server;
import org.screenstudio.services.sources.Microphone;
import org.screenstudio.services.sources.Overlay;
import org.screenstudio.services.sources.Screen;
import org.screenstudio.services.sources.Webcam;

/**
 *
 * @author Patrick Balleux
 */
public class ScreenStudio extends javax.swing.JFrame implements Listener, HotKeyListener {

    private Process streamProcess = null;
    private TrayIcon trayIcon = null;
    private Dimension trayIconSize = null;
    private Image trayIconDefault = null;
    private Image trayIconStarting = null;
    private Image trayIconRunning = null;
    private boolean actionFromTray = false;
    private Image icon = null;
    private Image iconRunning = null;
    private Image iconStarting = null;
    private File loadedConfigurationFile = null;
    private final WebRemote remote;
    private File videoFolder = new File(System.getProperty("user.home"), "ScreenStudio");
    private ArrayList<String> lastLogs = new ArrayList<String>();
    private final int MAXLOGSIZE = 200;
    private boolean processRunning = false;
    private HTMLRenderer currentRenderer = null;
    private Provider keyShortcuts = null;
    private String shortcutRecording = "control shift R";
    private String shortcutStreaming = "control shift S";
    private EkitCore overlayEditor = new EkitCore(false, null, null, null, null, null, true, false, true, false, null, null, false, false, false, true, EkitCore.TOOLBAR_DEFAULT_MULTI, true);

    /**
     * Creates new form ScreenStudio
     */
    public ScreenStudio() {
        initComponents();
        this.setTitle("ScreenStudio 1.5.0");

        if (Screen.isOSX()) {
            mnuCaptureWindow.setVisible(false);
            spinWebcamCaptureHeight.setVisible(false);
            spinWebcamCaptureWidth.setVisible(false);
            mnuSetCaptureArea.setVisible(false);
            lblScreenDimenssion.setVisible(false);
        }
        try {
            icon = javax.imageio.ImageIO.read(this.getClass().getResource("icon.png"));
            iconRunning = javax.imageio.ImageIO.read(this.getClass().getResource("iconRunning.png"));
            iconStarting = javax.imageio.ImageIO.read(this.getClass().getResource("iconStarting.png"));
            this.setIconImage(javax.imageio.ImageIO.read(this.getClass().getResource("logo.png")));
            displaySystemTrayIcon();

        } catch (IOException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }

        panOverlayEditor.add(overlayEditor.getMenuBar(), BorderLayout.NORTH);
        panOverlayEditor.add(overlayEditor.getToolBarFormat(true), BorderLayout.SOUTH);
        panOverlayEditor.add(overlayEditor, BorderLayout.CENTER);
        updateControls(null);
        loadPreferences(false);
        initializeShortCuts();
        updateSourceOrigin();
        remote = new WebRemote(this);
        if (!org.screenstudio.services.sources.SystemCheck.isSystemReady(false)) {
            txtStatus.setText("Some dependencies maybe missing... Have a look at Options/System Check!");
        }
        pack();
    }

    private void savePreferences() {
        try {

            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node(this.getClass().getSimpleName());
            prefs.put("name", txtStreamName.getText());

            if (((Microphone) cboAudioMonitors.getSelectedItem()).getDevice() != null) {
                prefs.put("audiomonitor", ((Microphone) cboAudioMonitors.getSelectedItem()).getDevice());
            } else {
                prefs.put("audiomonitor", "");
            }
            if (((Microphone) cboAudioSource.getSelectedItem()).getDevice() != null) {
                prefs.put("audiosource", ((Microphone) cboAudioSource.getSelectedItem()).getDevice());
            } else {
                prefs.put("audiosource", "");
            }

            if (((Webcam) cbowebcamSource.getSelectedItem()).getDevice() != null) {
                prefs.put("webcamsource", ((Webcam) cbowebcamSource.getSelectedItem()).getDevice());
            } else {
                prefs.remove("webcamsource");
            }
            if (loadedConfigurationFile != null) {
                prefs.put("loadedconfigfile", loadedConfigurationFile.getAbsolutePath());
            }
            prefs.putInt("webcamcapturewidth", (Integer) spinWebcamCaptureWidth.getValue());
            prefs.putInt("webcamcaptureheight", (Integer) spinWebcamCaptureHeight.getValue());
            prefs.putFloat("webcamoffset", (Float) spinWebcamOffset.getValue());
            prefs.putInt("fps", (Integer) spinFPS.getValue());
            prefs.put("screensource", cboScreen.getSelectedItem().toString());
            prefs.put("overlayhtml", overlayEditor.getTextPane().getText());
            prefs.putInt("webcamlayout", cboWebcamLayout.getSelectedIndex());
            prefs.putInt("overlaylayout", cboOverlayLayout.getSelectedIndex());
            prefs.putInt("recordservice", cboRecordServices.getSelectedIndex());
            prefs.putInt("recordprofile", cboRecordProfile.getSelectedIndex());
            prefs.putInt("rtmpservice", cboRTMPServices.getSelectedIndex());
            prefs.putInt("rtmpserver", cboRTMPServer.getSelectedIndex());
            prefs.putInt("rtmpprofile", cboRTMPProfiles.getSelectedIndex());
            prefs.put("videofolder", videoFolder.getAbsolutePath());
            prefs.put("shortcutrecording", shortcutRecording);
            prefs.put("shortcutstreaming", shortcutStreaming);

            if (cboStreamPresets.getSelectedItem() != null) {
                prefs.put("rtmppreset", cboStreamPresets.getSelectedItem().toString());
            }
            if (cboRecordPresets.getSelectedItem() != null) {
                prefs.put("recordpreset", cboRecordPresets.getSelectedItem().toString());
            }
            prefs.flush();
            if (java.util.prefs.Preferences.userRoot().nodeExists(this.getName())) {
                //small bugs where prefs were saved in the wrong node.  Loading old prefs and deleting
                prefs = java.util.prefs.Preferences.userRoot().node(this.getName());
                prefs.removeNode();
                prefs.flush();
            }
        } catch (Exception ex) {
            MsgLogs logs = new MsgLogs("Saving preferences...", ex, this, true);
            logs.setLocationByPlatform(true);
            logs.setVisible(true);
        }
    }

    private void updateSourceOrigin() {
        lblMadeBY.setText("By " + Encoder.Author);
        lblMadeFor.setText(Encoder.OS + " (" + Encoder.Backend + ") v" + Encoder.Version);
    }

    private void initializeShortCuts() {
        final ScreenStudio instance = this;
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (keyShortcuts == null) {
                        keyShortcuts = Provider.getCurrentProvider(false);
                    }
                    keyShortcuts.reset();
                    keyShortcuts.register(KeyStroke.getKeyStroke(shortcutRecording), instance);
                    keyShortcuts.register(KeyStroke.getKeyStroke(shortcutStreaming), instance);
                } catch (Exception ex) {
                    keyShortcuts = null;
                    MsgLogs logs = new MsgLogs("Setting shortcuts", ex, instance, true);
                    logs.setLocationByPlatform(true);
                    logs.setVisible(true);
                }
            }
        }).start();

    }

    private void stopShortcuts() {
        final ScreenStudio instance = this;
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (keyShortcuts != null) {
                        keyShortcuts.reset();
                        keyShortcuts.stop();
                    }

                } catch (Exception ex) {
                    keyShortcuts = null;
                    MsgLogs logs = new MsgLogs("Setting shortcuts", ex, instance, true);
                    logs.setLocationByPlatform(true);
                    logs.setVisible(true);
                }
            }
        }).start();
    }

    private void loadPreferences(boolean reloading) {
        try {

            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node(this.getClass().getSimpleName());
            if (java.util.prefs.Preferences.userRoot().nodeExists(this.getName())) {
                //small bugs where prefs were saved in the wrong node.  Loading old prefs and deleting
                prefs = java.util.prefs.Preferences.userRoot().node(this.getName());
            }
            if (!reloading) {
                String file = prefs.get("loadedconfigfile", null);
                if (file != null) {
                    loadedConfigurationFile = new File(file);
                    if (loadedConfigurationFile.exists()) {
                        updateControls(loadedConfigurationFile);
                    } else {
                        loadedConfigurationFile = null;
                    }
                }
            }
            txtStreamName.setText(prefs.get("name", "ScreenStudio"));
            String adev = prefs.get("audiosource", "ScreenStudio");
            for (int i = 0; i < cboAudioSource.getItemCount(); i++) {
                Microphone s = (Microphone) cboAudioSource.getItemAt(i);
                if (adev.equals(s.getDevice())) {
                    cboAudioSource.setSelectedIndex(i);
                    break;
                }
            }
            adev = prefs.get("audiomonitor", "ScreenStudio");
            for (int i = 0; i < cboAudioMonitors.getItemCount(); i++) {
                Microphone s = (Microphone) cboAudioMonitors.getItemAt(i);
                if (adev.equals(s.getDevice())) {
                    cboAudioMonitors.setSelectedIndex(i);
                    break;
                }
            }
            String wdev = prefs.get("webcamsource", "ScreenStudio");
            for (int i = 0; i < cbowebcamSource.getItemCount(); i++) {
                Webcam s = (Webcam) cbowebcamSource.getItemAt(i);
                if (s.getDevice() != null && s.getDevice().equals(wdev)) {
                    cbowebcamSource.setSelectedIndex(i);
                    break;
                }
            }
            spinWebcamCaptureWidth.setValue(prefs.getInt("webcamcapturewidth", 320));
            spinWebcamCaptureHeight.setValue(prefs.getInt("webcamcaptureheight", 240));
            spinWebcamOffset.setValue(prefs.getFloat("webcamoffset", 0));
            spinFPS.setValue(prefs.getInt("fps", 10));
            String screenID = prefs.get("screensource", "");
            for (int i = 0; i < cboScreen.getItemCount(); i++) {
                Screen s = (Screen) cboScreen.getItemAt(i);
                if (s.getLabel().equals(screenID)) {
                    cboScreen.setSelectedIndex(i);
                    break;
                }
            }
            lblScreenDimenssion.setText(((Screen) cboScreen.getSelectedItem()).getSize().toString().replaceAll("java.awt.Rectangle", ""));
            String html = prefs.get("overlayhtml", "NONE");
            if (html.equals("NONE")) {
                overlayEditor.getTextPane().setText(loadDefaultHTML());
            } else {
                overlayEditor.getTextPane().setText(html);
            }

            cboRecordServices.setSelectedIndex(prefs.getInt("recordservice", 0));
            cboRecordProfile.setSelectedIndex(prefs.getInt("recordprofile", 0));
            cboRecordPresets.setSelectedItem(prefs.get("recordpreset", "ultrafast"));
            cboRTMPServices.setSelectedIndex(prefs.getInt("rtmpservice", 0));
            cboRTMPServer.setSelectedIndex(prefs.getInt("rtmpserver", 0));
            cboRTMPProfiles.setSelectedIndex(prefs.getInt("rtmpprofile", 0));
            cboStreamPresets.setSelectedItem(prefs.get("rtmppreset", "ultrafast"));
            videoFolder = new File(prefs.get("videofolder", new File(System.getProperty("user.home"), "ScreenStudio").getAbsolutePath()));

            cboWebcamLayout.setSelectedIndex(prefs.getInt("webcamlayout", cboWebcamLayout.getSelectedIndex()));
            if (cboOverlayLayout.getItemCount() > 0) {
                cboOverlayLayout.setSelectedIndex(prefs.getInt("overlaylayout", cboOverlayLayout.getSelectedIndex()));
            }

            shortcutRecording = prefs.get("shortcutrecording", shortcutRecording);
            shortcutStreaming = prefs.get("shortcutstreaming", shortcutStreaming);

            chkOptionRecordControl.setSelected(shortcutRecording.contains("control"));
            chkOptionRecordAlt.setSelected(shortcutRecording.contains("alt"));
            chkOptionRecordShift.setSelected(shortcutRecording.contains("shift"));

            chkOptionStreamControl.setSelected(shortcutStreaming.contains("control"));
            chkOptionStreamAlt.setSelected(shortcutStreaming.contains("alt"));
            chkOptionStreamShift.setSelected(shortcutStreaming.contains("shift"));

            txtOptionRecordKey.setText(shortcutRecording.substring(shortcutRecording.length() - 1));
            txtOptionStreamKey.setText(shortcutStreaming.substring(shortcutStreaming.length() - 1));
        } catch (Exception ex) {
            MsgLogs logs = new MsgLogs("Loading preferences...", ex, this, true);
            logs.setLocationByPlatform(true);
            logs.setVisible(true);
        }
    }

    private void updateControls(File file) {

        try {
            cboRTMPServices.setModel(new DefaultComboBoxModel());
            cboRecordServices.setModel(new DefaultComboBoxModel());
            for (Encoder enc : Encoder.getEncoders(file)) {
                if (enc.getTarget().equals("RTMP")) {
                    cboRTMPServices.addItem(enc);
                } else if (enc.getTarget().equals("FILE")) {

                    cboRecordServices.addItem(enc);
                }
            }
            tabs.setEnabledAt(0, cboRecordServices.getItemCount() > 0);
            tabs.setEnabledAt(1, cboRTMPServices.getItemCount() > 0);
            if (tabs.isEnabledAt(0)) {
                tabs.setSelectedIndex(0);
            } else if (tabs.isEnabledAt(1)) {
                tabs.setSelectedIndex(1);
            } else {
                tabs.setSelectedIndex(2);
            }
            mnuRecord.setEnabled(tabs.isEnabledAt(0));
            mnuStream.setEnabled(tabs.isEnabledAt(1));
            updateSources();

        } catch (Exception ex) {
            MsgLogs logs = new MsgLogs("Updating controls...", ex, this, true);
            logs.setLocationByPlatform(true);
            logs.setVisible(true);
        }
    }

    private void updateSources() throws IOException {
        int selectedAudio = cboAudioSource.getSelectedIndex();
        int selectedMonitor = cboAudioMonitors.getSelectedIndex();
        int selectedWebcam = cbowebcamSource.getSelectedIndex();
        Microphone[] audios = Microphone.getSources();
        cboAudioSource.setModel(new DefaultComboBoxModel());
        cboAudioMonitors.setModel(new DefaultComboBoxModel());

        cboAudioSource.addItem(new Microphone());
        cboAudioMonitors.addItem(new Microphone());
        for (Microphone m : audios) {
            if (m.getDevice().endsWith(".monitor")) {
                cboAudioMonitors.addItem(m);
            } else {
                cboAudioSource.addItem(m);
            }
        }
        cbowebcamSource.setModel(new DefaultComboBoxModel(Webcam.getSources()));
        cboScreen.setModel(new DefaultComboBoxModel(Screen.getSources()));
        if (selectedAudio < cboAudioSource.getItemCount()) {
            cboAudioSource.setSelectedIndex(selectedAudio);
        }
        if (selectedMonitor < cboAudioMonitors.getItemCount()) {
            cboAudioMonitors.setSelectedIndex(selectedMonitor);
        }
        if (selectedWebcam < cbowebcamSource.getItemCount()) {
            cbowebcamSource.setSelectedIndex(selectedWebcam);
        }
    }

    private void updateControls(boolean isStreaming) {
        lblScreenDimenssion.setEnabled(!isStreaming);
        mnuOpen.setEnabled(!isStreaming);
        txtStreamName.setVisible(!isStreaming);
        btnPreview.setEnabled(!isStreaming);
        cboAudioSource.setEnabled(!isStreaming);
        cboAudioMonitors.setEnabled(!isStreaming);
        cbowebcamSource.setEnabled(!isStreaming);
        cboScreen.setEnabled(!isStreaming);
        overlayEditor.setEnabled(!isStreaming);
        cboStreamPresets.setEnabled(!isStreaming);
        spinFPS.setEnabled(!isStreaming);
        spinWebcamCaptureWidth.setEnabled(!isStreaming);
        spinWebcamCaptureHeight.setEnabled(!isStreaming);
        spinWebcamOffset.setEnabled(!isStreaming);
        cboWebcamLayout.setEnabled(!isStreaming);
        cboOverlayLayout.setEnabled(!isStreaming);
        cboRecordProfile.setEnabled(!isStreaming);
        cboRecordPresets.setEnabled(!isStreaming);
        cboRecordServices.setEnabled(!isStreaming);
        cboRTMPServices.setEnabled(!isStreaming);
        cboRTMPServer.setEnabled(!isStreaming && cboRTMPServer.getItemCount() > 0);
        cboRTMPProfiles.setEnabled(!isStreaming);
        if (isStreaming) {
            this.setIconImage(iconStarting);
            if (trayIcon != null) {
                trayIcon.setImage(trayIconStarting);
            }
        } else {
            this.setIconImage(icon);
            if (trayIcon != null) {
                trayIcon.setImage(trayIconDefault);
            }
        }
    }

    private void updateStatus() {
        long started = System.currentTimeMillis();
        long delta;

        if (streamProcess != null) {
            BufferedReader reader = null;
            try {
                String line;
                boolean isStarting = true;
                reader = new BufferedReader(new InputStreamReader(streamProcess.getErrorStream()));
                line = reader.readLine();
                long lastTime = -1;
                lastLogs = new ArrayList<String>();
                while (line != null) {
                    lastLogs.add(line);
                    if (lastLogs.size() >= MAXLOGSIZE) {
                        lastLogs.remove(0);
                    }
                    if (isStarting && line.startsWith("frame")) {
                        isStarting = false;
                        this.setIconImage(iconRunning);
                        if (trayIcon != null) {
                            trayIcon.setImage(trayIconRunning);
                            trayIcon.setToolTip("Recording Time: 0 minutes...");
                        }
                        started = System.currentTimeMillis();
                    }
                    if (!isStarting) {
                        delta = (System.currentTimeMillis() - started) / 1000 / 60;
                        if (trayIcon != null && delta != lastTime) {
                            lastTime = delta;
                            BufferedImage img = new BufferedImage((int) trayIconSize.getWidth(), (int) trayIconSize.getHeight(), BufferedImage.TRANSLUCENT);
                            Graphics2D g = img.createGraphics();
                            g.setBackground(Color.GREEN);
                            g.setRenderingHint(
                                    RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                            g.setRenderingHint(
                                    RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);
                            g.clearRect(0, 0, img.getWidth(), img.getHeight());
                            g.setColor(Color.BLACK);
                            g.setStroke(new BasicStroke(2));
                            g.drawRect(1, 1, img.getWidth() - 2, img.getHeight() - 2);
                            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, img.getHeight() / 3));
                            String time = (delta) + "";
                            int x = (img.getWidth() / 2) - (g.getFontMetrics(g.getFont()).stringWidth(time) / 2);
                            g.drawString(time, x, (img.getHeight() / 2));
                            time = "MIN";
                            x = (img.getWidth() / 2) - (g.getFontMetrics(g.getFont()).stringWidth(time) / 2);
                            g.drawString(time, x, img.getHeight() - 4);
                            g.dispose();
                            this.trayIcon.setImage(img);

                            this.trayIcon.setToolTip("Recording Time: " + delta + " minutes...");
                        }
                    }
                    txtStatus.setText(line);
                    System.out.println(line);
                    if (streamProcess != null) {
                        line = reader.readLine();
                    } else {
                        line = null;
                    }
                }
            } catch (IOException ex) {

            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }
        processRunning = false;

    }

    private void displaySystemTrayIcon() throws IOException {
        if (SystemTray.isSupported() && trayIcon == null) {
            SystemTray tray = SystemTray.getSystemTray();
            try {
                trayIconSize = tray.getTrayIconSize();
                trayIconDefault = icon.getScaledInstance(-1, (int) trayIconSize.getHeight(), Image.SCALE_SMOOTH);
                trayIconStarting = iconStarting.getScaledInstance(-1, (int) trayIconSize.getHeight(), Image.SCALE_SMOOTH);
                trayIconRunning = iconRunning.getScaledInstance(-1, (int) trayIconSize.getHeight(), Image.SCALE_SMOOTH);
                trayIcon = new TrayIcon(trayIconDefault, this.getTitle(), trayMenu);
                trayIcon.setImageAutoSize(false);
                tray.add(trayIcon);
            } catch (AWTException ex) {
                Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (trayIcon == null) {
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
    }

    private void removeSystemTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(trayIcon);
        }
    }

    private void startProcess(String command) {
        processRunning = true;
        System.out.println(lblTitle.getText());
        System.out.println("-----------------------");
        System.out.println("Started");
        System.out.println(command);
        try {
            updateControls(true);
            streamProcess = Runtime.getRuntime().exec(command);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    updateStatus();
                }
            }).start();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    monitorProcess();
                }
            }).start();

        } catch (IOException ex) {
            txtStatus.setText(ex.getMessage());
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void logCommand(File output, String command) throws FileNotFoundException, IOException {
        File log = new File(output.getAbsolutePath() + ".log");
        FileOutputStream out = new FileOutputStream(log, true);
        out.write(command.getBytes());
        out.flush();
        out.close();
    }

    private void monitorProcess() {
        while (streamProcess != null) {
            try {
                System.out.println("Exit Code: " + streamProcess.exitValue());
                stopStream("An error occured...");
                tglStreamToServer.setSelected(false);
                tglRecordVideo.setSelected(false);
                tglStreamToServer.setEnabled(true);
                tglRecordVideo.setEnabled(true);
                MsgLogs logs = new MsgLogs(lastLogs, this, true);
                logs.setLocationByPlatform(true);
                logs.setVisible(true);
            } catch (Exception ex) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    private void stopStream(String message) {
        if (streamProcess != null) {
            streamProcess.destroy();
            streamProcess = null;
        }
        while (processRunning) {
            //Waiting for the process to completly stop...
            try {
                Thread.sleep(100);
                Thread.yield();
            } catch (InterruptedException ex) {
            }
        }
        System.out.println(message);
        txtStatus.setText(message);
        updateControls(false);
        if (trayIcon != null) {
            trayIcon.setToolTip("ScreenStudio: " + message);
        }
        //Cleanup virtual audio
        try {
            Microphone.getVirtualAudio(null, null);
        } catch (IOException ex) {
        } catch (InterruptedException ex) {
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

        trayMenu = new java.awt.PopupMenu();
        mnuShow = new java.awt.MenuItem();
        mnuRecord = new java.awt.CheckboxMenuItem();
        mnuStream = new java.awt.CheckboxMenuItem();
        mnuExit = new java.awt.MenuItem();
        lblTitle = new javax.swing.JLabel();
        txtStatus = new javax.swing.JTextField();
        lblMadeBY = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        tabs = new javax.swing.JTabbedPane();
        panRecord = new javax.swing.JPanel();
        cboRecordProfile = new javax.swing.JComboBox();
        lblRecordProfile = new javax.swing.JLabel();
        lblRecordEncoders = new javax.swing.JLabel();
        cboRecordServices = new javax.swing.JComboBox();
        tglRecordVideo = new javax.swing.JToggleButton();
        lblRecordTargetRecordingFile = new javax.swing.JLabel();
        lblRemoteRecordMessage = new javax.swing.JLabel();
        lblRecordPresets = new javax.swing.JLabel();
        cboRecordPresets = new javax.swing.JComboBox();
        panStream = new javax.swing.JPanel();
        txtStreamName = new javax.swing.JTextField();
        lblStreamName = new javax.swing.JLabel();
        lblEncoders = new javax.swing.JLabel();
        cboRTMPServices = new javax.swing.JComboBox();
        lblServer = new javax.swing.JLabel();
        cboRTMPServer = new javax.swing.JComboBox();
        lblProfile = new javax.swing.JLabel();
        cboRTMPProfiles = new javax.swing.JComboBox();
        tglStreamToServer = new javax.swing.JToggleButton();
        lblRemoteRTMPMessage = new javax.swing.JLabel();
        lblStreamPresets = new javax.swing.JLabel();
        cboStreamPresets = new javax.swing.JComboBox();
        panSources = new javax.swing.JPanel();
        cboAudioSource = new javax.swing.JComboBox();
        lblAudioSource = new javax.swing.JLabel();
        lblWebcam = new javax.swing.JLabel();
        cbowebcamSource = new javax.swing.JComboBox();
        lblFrameRate = new javax.swing.JLabel();
        spinFPS = new javax.swing.JSpinner();
        spinWebcamCaptureWidth = new javax.swing.JSpinner();
        lblScreen = new javax.swing.JLabel();
        cboScreen = new javax.swing.JComboBox();
        spinWebcamOffset = new javax.swing.JSpinner();
        lblDelay = new javax.swing.JLabel();
        lblWebcamWidth = new javax.swing.JLabel();
        spinWebcamCaptureHeight = new javax.swing.JSpinner();
        jSeparator2 = new javax.swing.JSeparator();
        lblScreen2 = new javax.swing.JLabel();
        lblDelay1 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        lblScreenDimenssion = new javax.swing.JLabel();
        cboAudioMonitors = new javax.swing.JComboBox();
        lblWebcamLayout = new javax.swing.JLabel();
        cboWebcamLayout = new javax.swing.JComboBox();
        panOptions = new javax.swing.JPanel();
        lblOptionsRecordShortkey = new javax.swing.JLabel();
        lblOptionsStreamShortkey = new javax.swing.JLabel();
        chkOptionRecordControl = new javax.swing.JCheckBox();
        chkOptionRecordAlt = new javax.swing.JCheckBox();
        chkOptionRecordShift = new javax.swing.JCheckBox();
        chkOptionStreamControl = new javax.swing.JCheckBox();
        chkOptionStreamAlt = new javax.swing.JCheckBox();
        chkOptionStreamShift = new javax.swing.JCheckBox();
        txtOptionRecordKey = new javax.swing.JTextField();
        txtOptionStreamKey = new javax.swing.JTextField();
        btnOptionsApply = new javax.swing.JButton();
        lblOverlayLayout = new javax.swing.JLabel();
        cboOverlayLayout = new javax.swing.JComboBox();
        panOverlayEditor = new javax.swing.JPanel();
        lblMadeFor = new javax.swing.JLabel();
        btnPreview = new javax.swing.JButton();
        mnuBar = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mnuOpen = new javax.swing.JMenuItem();
        mnuChkRemoteWebServer = new javax.swing.JCheckBoxMenuItem();
        mnuSetCaptureArea = new javax.swing.JMenuItem();
        mnuCaptureWindow = new javax.swing.JMenuItem();
        mnuRefreshSources = new javax.swing.JMenuItem();
        mnuAdvanced = new javax.swing.JMenu();
        mnuResetPreferences = new javax.swing.JMenuItem();
        mnuSetVideoFolder = new javax.swing.JMenuItem();
        mnuIdentifyScreen = new javax.swing.JMenuItem();
        mnuExportXMLToFile = new javax.swing.JMenuItem();
        mnuSystemCheck = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        mnuBarExit = new javax.swing.JMenuItem();

        trayMenu.setFont(new java.awt.Font("Ubuntu", 1, 14)); // NOI18N
        trayMenu.setLabel("ScreenStudio");

        mnuShow.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        mnuShow.setLabel("Show");
        mnuShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuShowActionPerformed(evt);
            }
        });
        trayMenu.add(mnuShow);

        mnuRecord.setLabel("Record");
        mnuRecord.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                mnuRecordItemStateChanged(evt);
            }
        });
        trayMenu.add(mnuRecord);

        mnuStream.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        mnuStream.setLabel("Stream!");
        mnuStream.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                mnuStreamItemStateChanged(evt);
            }
        });
        trayMenu.add(mnuStream);
        trayMenu.addSeparator();
        mnuExit.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        mnuExit.setLabel("Exit");
        mnuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExitActionPerformed(evt);
            }
        });
        trayMenu.add(mnuExit);

        setBackground(java.awt.Color.lightGray);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblTitle.setFont(new java.awt.Font("Ubuntu", 1, 24)); // NOI18N
        lblTitle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/icon.png"))); // NOI18N
        lblTitle.setText("ScreenStudio");
        lblTitle.setToolTipText("Click to visit ScreenStudio Homepage");
        lblTitle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblTitle.setIconTextGap(6);
        lblTitle.setName("lblTitle"); // NOI18N
        lblTitle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                lblTitleMousePressed(evt);
            }
        });

        txtStatus.setEditable(false);
        txtStatus.setBackground(javax.swing.UIManager.getDefaults().getColor("EditorPane.inactiveForeground"));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/screenstudio/gui/lang/languages"); // NOI18N
        txtStatus.setText(bundle.getString("WELCOME")); // NOI18N
        txtStatus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtStatusMouseClicked(evt);
            }
        });

        lblMadeBY.setFont(new java.awt.Font("Ubuntu", 3, 12)); // NOI18N
        lblMadeBY.setForeground(new java.awt.Color(164, 164, 164));
        lblMadeBY.setText("by Patrick Balleux");

        tabs.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        cboRecordProfile.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblRecordProfile.setText(bundle.getString("PROFILE")); // NOI18N

        lblRecordEncoders.setText(bundle.getString("FILE")); // NOI18N

        cboRecordServices.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboRecordServices.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboRecordServicesItemStateChanged(evt);
            }
        });

        tglRecordVideo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconPlay.png"))); // NOI18N
        tglRecordVideo.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconStarting.png"))); // NOI18N
        tglRecordVideo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconRunning.png"))); // NOI18N
        tglRecordVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglRecordVideoActionPerformed(evt);
            }
        });

        lblRecordTargetRecordingFile.setForeground(java.awt.Color.gray);
        lblRecordTargetRecordingFile.setText(" ");

        lblRemoteRecordMessage.setText(" ");

        lblRecordPresets.setText(bundle.getString("PRESET")); // NOI18N

        cboRecordPresets.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout panRecordLayout = new javax.swing.GroupLayout(panRecord);
        panRecord.setLayout(panRecordLayout);
        panRecordLayout.setHorizontalGroup(
            panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panRecordLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRecordTargetRecordingFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panRecordLayout.createSequentialGroup()
                        .addComponent(lblRemoteRecordMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tglRecordVideo))
                    .addGroup(panRecordLayout.createSequentialGroup()
                        .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblRecordProfile, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRecordEncoders, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRecordPresets, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(4, 4, 4)
                        .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cboRecordServices, 0, 428, Short.MAX_VALUE)
                            .addComponent(cboRecordProfile, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboRecordPresets, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        panRecordLayout.setVerticalGroup(
            panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panRecordLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRecordEncoders)
                    .addComponent(cboRecordServices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRecordProfile)
                    .addComponent(cboRecordProfile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panRecordLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(tglRecordVideo))
                    .addGroup(panRecordLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panRecordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblRecordPresets)
                            .addComponent(cboRecordPresets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblRecordTargetRecordingFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
                        .addComponent(lblRemoteRecordMessage)))
                .addContainerGap())
        );

        tabs.addTab(bundle.getString("TAB_RECORD"), panRecord); // NOI18N

        txtStreamName.setText("Stream Key");

        lblStreamName.setText(bundle.getString("RTMPKEY")); // NOI18N
        lblStreamName.setToolTipText("Used for identifying your RTMP stream");

        lblEncoders.setText(bundle.getString("SERVICE")); // NOI18N

        cboRTMPServices.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboRTMPServices.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboRTMPServicesItemStateChanged(evt);
            }
        });

        lblServer.setText(bundle.getString("SERVER")); // NOI18N

        cboRTMPServer.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblProfile.setText(bundle.getString("PROFILE")); // NOI18N

        cboRTMPProfiles.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        tglStreamToServer.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        tglStreamToServer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconPlay.png"))); // NOI18N
        tglStreamToServer.setToolTipText("Stream!");
        tglStreamToServer.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tglStreamToServer.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconStarting.png"))); // NOI18N
        tglStreamToServer.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconRunning.png"))); // NOI18N
        tglStreamToServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglStreamToServerActionPerformed(evt);
            }
        });

        lblRemoteRTMPMessage.setText(" ");

        lblStreamPresets.setText(bundle.getString("PRESET")); // NOI18N

        cboStreamPresets.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout panStreamLayout = new javax.swing.GroupLayout(panStream);
        panStream.setLayout(panStreamLayout);
        panStreamLayout.setHorizontalGroup(
            panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panStreamLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panStreamLayout.createSequentialGroup()
                        .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblEncoders, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblServer, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblProfile, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStreamName, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(4, 4, 4)
                        .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cboRTMPProfiles, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboRTMPServer, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtStreamName)
                            .addComponent(cboRTMPServices, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(panStreamLayout.createSequentialGroup()
                        .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panStreamLayout.createSequentialGroup()
                                .addComponent(lblStreamPresets, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(cboStreamPresets, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 175, Short.MAX_VALUE))
                            .addComponent(lblRemoteRTMPMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tglStreamToServer)))
                .addContainerGap())
        );
        panStreamLayout.setVerticalGroup(
            panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panStreamLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEncoders)
                    .addComponent(cboRTMPServices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblServer)
                    .addComponent(cboRTMPServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblProfile)
                    .addComponent(cboRTMPProfiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStreamName)
                    .addComponent(txtStreamName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panStreamLayout.createSequentialGroup()
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(tglStreamToServer))
                    .addGroup(panStreamLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(panStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cboStreamPresets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStreamPresets))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblRemoteRTMPMessage)))
                .addContainerGap())
        );

        tabs.addTab(bundle.getString("TAB_STREAM"), panStream); // NOI18N

        cboAudioSource.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboAudioSource.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboAudioSourceItemStateChanged(evt);
            }
        });

        lblAudioSource.setText(bundle.getString("AUDIO")); // NOI18N
        lblAudioSource.setToolTipText("The list of available Pulseaudio sources");

        lblWebcam.setText(bundle.getString("WEBCAM")); // NOI18N
        lblWebcam.setToolTipText("Available webcams, by their name or generic device");

        cbowebcamSource.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbowebcamSource.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbowebcamSourceItemStateChanged(evt);
            }
        });

        lblFrameRate.setText(bundle.getString("FPS")); // NOI18N

        spinFPS.setModel(new javax.swing.SpinnerNumberModel(10, 5, 60, 1));

        spinWebcamCaptureWidth.setModel(new javax.swing.SpinnerNumberModel(320, 160, 1920, 1));

        lblScreen.setText(bundle.getString("SCREEN")); // NOI18N

        cboScreen.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboScreen.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboScreenItemStateChanged(evt);
            }
        });

        spinWebcamOffset.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-10.0f), Float.valueOf(10.0f), Float.valueOf(0.1f)));
        spinWebcamOffset.setToolTipText("<html><h1>Webcam Offset</h1>  \n<p>From -10.0 sec to 10.0 sec</p>\n<p>\nWhen applied, the webcam can be synchronized with the audio if there is a delay\n</p>\n</html>");

        lblDelay.setText(bundle.getString("DELAY")); // NOI18N

        lblWebcamWidth.setText("X");

        spinWebcamCaptureHeight.setModel(new javax.swing.SpinnerNumberModel(240, 120, 1080, 1));

        lblScreen2.setText(bundle.getString("SIZE")); // NOI18N

        lblDelay1.setText(bundle.getString("SHORTSECONDS")); // NOI18N

        lblScreenDimenssion.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        lblScreenDimenssion.setForeground(java.awt.SystemColor.controlShadow);
        lblScreenDimenssion.setText("Screen Size...");
        lblScreenDimenssion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblScreenDimenssion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblScreenDimenssionMouseClicked(evt);
            }
        });

        cboAudioMonitors.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cboAudioMonitors.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cboAudioMonitorsItemStateChanged(evt);
            }
        });

        lblWebcamLayout.setText(bundle.getString("WEBCAMLAYOUT")); // NOI18N

        cboWebcamLayout.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Lower Right", "Lower Left", "Top Right", "Top Left", "Center" }));

        javax.swing.GroupLayout panSourcesLayout = new javax.swing.GroupLayout(panSources);
        panSources.setLayout(panSourcesLayout);
        panSourcesLayout.setHorizontalGroup(
            panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSourcesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(panSourcesLayout.createSequentialGroup()
                        .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblWebcam, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(3, 3, 3)
                        .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panSourcesLayout.createSequentialGroup()
                                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(spinWebcamOffset)
                                    .addComponent(spinWebcamCaptureWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(2, 2, 2)
                                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblDelay1)
                                    .addGroup(panSourcesLayout.createSequentialGroup()
                                        .addComponent(lblWebcamWidth)
                                        .addGap(4, 4, 4)
                                        .addComponent(spinWebcamCaptureHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(lblWebcamLayout)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cboWebcamLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(cbowebcamSource, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jSeparator3)
                    .addGroup(panSourcesLayout.createSequentialGroup()
                        .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panSourcesLayout.createSequentialGroup()
                                .addComponent(lblScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(cboScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinFPS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblFrameRate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblScreenDimenssion))
                            .addComponent(lblScreen2, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panSourcesLayout.createSequentialGroup()
                        .addComponent(lblAudioSource, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(cboAudioSource, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboAudioMonitors, 0, 176, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panSourcesLayout.setVerticalGroup(
            panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panSourcesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblScreen)
                    .addComponent(cboScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinFPS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFrameRate)
                    .addComponent(lblScreenDimenssion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblWebcam)
                    .addComponent(cbowebcamSource, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblWebcamLayout)
                        .addComponent(cboWebcamLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblScreen2)
                        .addComponent(spinWebcamCaptureWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblWebcamWidth)
                        .addComponent(spinWebcamCaptureHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDelay)
                    .addComponent(spinWebcamOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDelay1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAudioSource)
                    .addComponent(cboAudioSource, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cboAudioMonitors, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(49, 49, 49))
        );

        tabs.addTab(bundle.getString("TAB_SOURCES"), panSources); // NOI18N

        lblOptionsRecordShortkey.setText(bundle.getString("SHORTKEYRECORD")); // NOI18N

        lblOptionsStreamShortkey.setText(bundle.getString("SHORTKEYSTREAM")); // NOI18N

        chkOptionRecordControl.setText("CTRL");

        chkOptionRecordAlt.setText("ALT");

        chkOptionRecordShift.setText("SHIFT");

        chkOptionStreamControl.setText("CTRL");

        chkOptionStreamAlt.setText("ALT");

        chkOptionStreamShift.setText("SHIFT");

        txtOptionRecordKey.setEditable(false);
        txtOptionRecordKey.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtOptionRecordKey.setText("R");
        txtOptionRecordKey.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtOptionRecordKeyKeyPressed(evt);
            }
        });

        txtOptionStreamKey.setEditable(false);
        txtOptionStreamKey.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtOptionStreamKey.setText("S");
        txtOptionStreamKey.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtOptionStreamKeyKeyPressed(evt);
            }
        });

        btnOptionsApply.setText(bundle.getString("OPTIONAPPLY")); // NOI18N
        btnOptionsApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOptionsApplyActionPerformed(evt);
            }
        });

        lblOverlayLayout.setText(bundle.getString("IMAGELAYOUT")); // NOI18N

        cboOverlayLayout.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Bottom", "Top", "Bottom Right", "Top Right" }));

        javax.swing.GroupLayout panOptionsLayout = new javax.swing.GroupLayout(panOptions);
        panOptions.setLayout(panOptionsLayout);
        panOptionsLayout.setHorizontalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panOptionsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnOptionsApply))
                    .addGroup(panOptionsLayout.createSequentialGroup()
                        .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(panOptionsLayout.createSequentialGroup()
                                .addComponent(lblOptionsRecordShortkey)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkOptionRecordControl)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkOptionRecordAlt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkOptionRecordShift)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtOptionRecordKey, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panOptionsLayout.createSequentialGroup()
                                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblOptionsStreamShortkey)
                                    .addComponent(lblOverlayLayout))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cboOverlayLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panOptionsLayout.createSequentialGroup()
                                        .addComponent(chkOptionStreamControl)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(chkOptionStreamAlt)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(chkOptionStreamShift)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtOptionStreamKey)))))
                        .addGap(0, 181, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panOptionsLayout.setVerticalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblOptionsRecordShortkey)
                    .addComponent(chkOptionRecordControl)
                    .addComponent(chkOptionRecordAlt)
                    .addComponent(chkOptionRecordShift)
                    .addComponent(txtOptionRecordKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblOptionsStreamShortkey)
                    .addComponent(chkOptionStreamControl)
                    .addComponent(chkOptionStreamAlt)
                    .addComponent(chkOptionStreamShift)
                    .addComponent(txtOptionStreamKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblOverlayLayout)
                    .addComponent(cboOverlayLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 92, Short.MAX_VALUE)
                .addComponent(btnOptionsApply)
                .addContainerGap())
        );

        tabs.addTab(bundle.getString("OPTIONS"), panOptions); // NOI18N

        panOverlayEditor.setLayout(new java.awt.BorderLayout());
        tabs.addTab("Overlay", panOverlayEditor);

        lblMadeFor.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        lblMadeFor.setForeground(new java.awt.Color(164, 164, 164));
        lblMadeFor.setText("by Patrick Balleux");

        btnPreview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/screenstudio/gui/iconPreview.png"))); // NOI18N
        btnPreview.setToolTipText("Preview banner location");
        btnPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviewActionPerformed(evt);
            }
        });

        mnuBar.setFont(new java.awt.Font("Ubuntu", 0, 8)); // NOI18N

        mnuFile.setText(bundle.getString("OPTIONS")); // NOI18N
        mnuFile.setFont(new java.awt.Font("Ubuntu", 0, 8)); // NOI18N

        mnuOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuOpen.setText(bundle.getString("OPENCUSTOMXML")); // NOI18N
        mnuOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuOpen);

        mnuChkRemoteWebServer.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuChkRemoteWebServer.setText(bundle.getString("REMOTECONTROL")); // NOI18N
        mnuChkRemoteWebServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuChkRemoteWebServerActionPerformed(evt);
            }
        });
        mnuFile.add(mnuChkRemoteWebServer);

        mnuSetCaptureArea.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        mnuSetCaptureArea.setText(bundle.getString("SETCAPTUREAREA")); // NOI18N
        mnuSetCaptureArea.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSetCaptureAreaActionPerformed(evt);
            }
        });
        mnuFile.add(mnuSetCaptureArea);

        mnuCaptureWindow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        mnuCaptureWindow.setText(bundle.getString("CAPTURE_WINDOW")); // NOI18N
        mnuCaptureWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuCaptureWindowActionPerformed(evt);
            }
        });
        mnuFile.add(mnuCaptureWindow);

        mnuRefreshSources.setText(bundle.getString("REFRESHSOURCES")); // NOI18N
        mnuRefreshSources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuRefreshSourcesActionPerformed(evt);
            }
        });
        mnuFile.add(mnuRefreshSources);

        mnuAdvanced.setText(bundle.getString("ADVANCED")); // NOI18N

        mnuResetPreferences.setText(bundle.getString("RESETPREFERENCES")); // NOI18N
        mnuResetPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuResetPreferencesActionPerformed(evt);
            }
        });
        mnuAdvanced.add(mnuResetPreferences);

        mnuSetVideoFolder.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuSetVideoFolder.setText(bundle.getString("SETVIDEOFOLDER")); // NOI18N
        mnuSetVideoFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSetVideoFolderActionPerformed(evt);
            }
        });
        mnuAdvanced.add(mnuSetVideoFolder);

        mnuIdentifyScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        mnuIdentifyScreen.setText(bundle.getString("IDENTIFYSCREEN")); // NOI18N
        mnuIdentifyScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuIdentifyScreenActionPerformed(evt);
            }
        });
        mnuAdvanced.add(mnuIdentifyScreen);

        mnuExportXMLToFile.setText(bundle.getString("EXPORTXMLTOFILE")); // NOI18N
        mnuExportXMLToFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExportXMLToFileActionPerformed(evt);
            }
        });
        mnuAdvanced.add(mnuExportXMLToFile);

        mnuFile.add(mnuAdvanced);

        mnuSystemCheck.setText(bundle.getString("SYSTEMCHECK")); // NOI18N
        mnuSystemCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSystemCheckActionPerformed(evt);
            }
        });
        mnuFile.add(mnuSystemCheck);
        mnuFile.add(jSeparator4);

        mnuBarExit.setText(bundle.getString("EXIT")); // NOI18N
        mnuBarExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuBarExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuBarExit);

        mnuBar.add(mnuFile);

        setJMenuBar(mnuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMadeFor, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblMadeBY, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tabs)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtStatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPreview)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMadeBY)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMadeFor)))
                .addGap(2, 2, 2)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, 280, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnPreview, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cboScreenItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboScreenItemStateChanged
        if (cboScreen.getSelectedIndex() > 0) {
            lblScreenDimenssion.setText(((Screen) cboScreen.getSelectedItem()).getSize().toString().replaceAll("java.awt.Rectangle", ""));
            for (int i = 0; i < cboScreen.getItemCount(); i++) {
                Screen s = (Screen) cboScreen.getItemAt(i);
                new ScreenIdentifier(s.getLabel(), (int) s.getSize().getX() + 100, (int) s.getSize().getY() + 100).setVisible(true);
            }
        } else {
            lblScreenDimenssion.setText("---");
        }
    }//GEN-LAST:event_cboScreenItemStateChanged

    private void tglStreamToServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglStreamToServerActionPerformed
        if (!actionFromTray) {
            mnuStream.setState(tglStreamToServer.isSelected());
        }
        if (tglStreamToServer.isSelected()) {
            tglRecordVideo.setEnabled(false);
            mnuRecord.setEnabled(false);
            try {
                Screen s = configureScreenRecorder((Encoder) cboRTMPServices.getSelectedItem());
                if (s != null) {
                    Profile p = (Profile) cboRTMPProfiles.getSelectedItem();
                    if (cboStreamPresets.getSelectedItem() != null) {
                        p.getVideo().setPreset(cboStreamPresets.getSelectedItem().toString());
                    }
                    s.setProfile(p);
                    Server server = (Server) cboRTMPServer.getSelectedItem();

                    String commandLine = Encoder.parse(s, server, txtStreamName.getText(), videoFolder);
                    startProcess(commandLine);

                }
            } catch (Exception ex) {
                txtStatus.setText("An error has occured... " + ex.getMessage());
                tglStreamToServer.setSelected(false);
                tglRecordVideo.setEnabled(true);
                mnuRecord.setEnabled(true);
                MsgLogs logs = new MsgLogs("Streaming to server...", ex, this, true);
                logs.setLocationByPlatform(true);
                logs.setVisible(true);
            }
        } else {
            stopStream("Stopped...");
            tglRecordVideo.setEnabled(true);
            mnuRecord.setEnabled(tabs.isEnabledAt(0));
        }

    }//GEN-LAST:event_tglStreamToServerActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        savePreferences();
        if (this.getDefaultCloseOperation() != JFrame.EXIT_ON_CLOSE) {
            MessageBox msg = new MessageBox(this, true);
            msg.setLocationRelativeTo(this);
            msg.setVisible(true);
            if (msg.doExit()) {
                mnuBarExit.doClick();
            }
        } else {
            if (keyShortcuts != null) {
                stopShortcuts();
            }
        }
    }//GEN-LAST:event_formWindowClosing


    private void mnuStreamItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_mnuStreamItemStateChanged
        if (!actionFromTray) {
            actionFromTray = true;
            tglStreamToServer.doClick();
            actionFromTray = false;
        }
    }//GEN-LAST:event_mnuStreamItemStateChanged

    private void mnuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExitActionPerformed
        stopStream("Exiting...");
        remote.stop();
        savePreferences();
        removeSystemTrayIcon();
        this.dispose();
        System.exit(0);
    }//GEN-LAST:event_mnuExitActionPerformed

    private void mnuShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuShowActionPerformed
        if (!this.isVisible()) {
            this.setLocationByPlatform(true);
            this.setVisible(true);
        }
    }//GEN-LAST:event_mnuShowActionPerformed

    private void cbowebcamSourceItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbowebcamSourceItemStateChanged
        cbowebcamSource.setToolTipText(cbowebcamSource.getSelectedItem().toString());
    }//GEN-LAST:event_cbowebcamSourceItemStateChanged

    private void cboAudioSourceItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboAudioSourceItemStateChanged
        cboAudioSource.setToolTipText(cboAudioSource.getSelectedItem().toString());
    }//GEN-LAST:event_cboAudioSourceItemStateChanged

    private void lblTitleMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblTitleMousePressed
        if (Desktop.isDesktopSupported() && evt.getClickCount() == 2) {
            try {
                Desktop.getDesktop().browse(URI.create("http://screenstudio.crombz.com"));
            } catch (IOException ex) {
                Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_lblTitleMousePressed

    private void cboRTMPServicesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboRTMPServicesItemStateChanged
        Encoder enc = (Encoder) cboRTMPServices.getSelectedItem();
        if (enc != null) {
            cboWebcamLayout.setModel(new DefaultComboBoxModel(enc.getLayouts("webcam")));
            cboRTMPServer.setModel(new DefaultComboBoxModel(enc.getServers()));
            cboRTMPServer.setEnabled(cboRTMPServer.getItemCount() > 0);
            if (cboRTMPServer.isEnabled()) {
                lblStreamName.setText("Stream Key");
            } else {
                lblStreamName.setText("URL/Key");
            }
            cboRTMPProfiles.setModel(new DefaultComboBoxModel(enc.getProfiles()));
            cboStreamPresets.setModel(new DefaultComboBoxModel(enc.getPresets().toArray()));
            cboStreamPresets.setVisible(cboStreamPresets.getItemCount() > 0);
        }
    }//GEN-LAST:event_cboRTMPServicesItemStateChanged

    private void cboRecordServicesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboRecordServicesItemStateChanged
        Encoder enc = (Encoder) cboRecordServices.getSelectedItem();
        if (enc != null) {
            cboWebcamLayout.setModel(new DefaultComboBoxModel(enc.getLayouts("webcam")));
            cboRecordProfile.setModel(new DefaultComboBoxModel(enc.getProfiles()));
            cboRecordPresets.setModel(new DefaultComboBoxModel(enc.getPresets().toArray()));
            cboRecordPresets.setVisible(cboRecordPresets.getItemCount() > 0);
        }
    }//GEN-LAST:event_cboRecordServicesItemStateChanged

    private Screen configureScreenRecorder(Encoder enc) throws IOException {
        Screen s = null;
        if (enc != null) {
            s = (Screen) cboScreen.getSelectedItem();
            int valign = JLabel.BOTTOM;
            int halign = JLabel.LEFT;
            switch (cboOverlayLayout.getSelectedIndex()) {
                case 0:
                    valign = JLabel.BOTTOM;
                    break;
                case 1:
                    valign = JLabel.TOP;
                    break;
                case 2:
                    valign = JLabel.BOTTOM;
                    halign = JLabel.RIGHT;
                    break;
                case 3:
                    valign = JLabel.TOP;
                    halign = JLabel.RIGHT;
                    break;
            }
            if (cboScreen.getSelectedIndex() > 0) {
                currentRenderer = new HTMLRenderer((int) s.getSize().getWidth(), (int) s.getSize().getHeight(), halign, valign);
            } else {
                //No screen, just the webcam is selected...
                s.setSize(new Rectangle((Integer) spinWebcamCaptureWidth.getValue(), (Integer) spinWebcamCaptureHeight.getValue()));
                currentRenderer = new HTMLRenderer((Integer) spinWebcamCaptureWidth.getValue(), (Integer) spinWebcamCaptureHeight.getValue(), halign, valign);
            }
            File currentBanner;
            URL url = isURL(overlayEditor.getTextPane().getText());
            if (url == null) {
                currentBanner = currentRenderer.getFile(overlayEditor.getTextPane().getText());
            } else {
                currentBanner = currentRenderer.getFile(url);
            }
            s.setFps((Integer) spinFPS.getValue());
            s.setMicrophone((Microphone) cboAudioSource.getSelectedItem());
            s.setMonitor((Microphone) cboAudioMonitors.getSelectedItem());
            s.setOverlay(new Overlay(currentBanner));
            Command c;
            if (cbowebcamSource.getSelectedIndex() > 0) {
                s.setWebcam((Webcam) cbowebcamSource.getSelectedItem());
                s.getWebcam().setLayout((Layout) cboWebcamLayout.getSelectedItem());
                s.getWebcam().setFps((Integer) spinFPS.getValue());
                s.getWebcam().setHeight((Integer) spinWebcamCaptureHeight.getValue());
                s.getWebcam().setWidth((Integer) spinWebcamCaptureWidth.getValue());
                s.getWebcam().setOffset((Float) spinWebcamOffset.getValue());
                if (cboScreen.getSelectedIndex() > 0) {
                    c = enc.getCommands().get("WEBCAMDESKTOP");
                } else {
                    c = enc.getCommands().get("WEBCAM");
                }
            } else {
                s.setWebcam(null);
                c = enc.getCommands().get("DESKTOP");
            }
            s.setCommand(c);
        }
        return s;
    }

    private String loadDefaultHTML() throws IOException {
        String content = "";
        java.io.DataInputStream din = new DataInputStream(this.getClass().getResourceAsStream("/org/screenstudio/overlay/ScreenStudio.html"));
        byte[] buffer = new byte[64000];
        int count = din.read(buffer);

        while (count != -1) {
            content += new String(buffer, 0, count);
            count = din.read(buffer);
        }
        return content;
    }
    private void tglRecordVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglRecordVideoActionPerformed
        if (!actionFromTray) {
            mnuRecord.setState(tglRecordVideo.isSelected());
        }

        if (tglRecordVideo.isSelected()) {
            try {
                tglStreamToServer.setEnabled(false);
                mnuStream.setEnabled(false);
                Screen s = configureScreenRecorder((Encoder) cboRecordServices.getSelectedItem());
                if (s != null) {
                    Profile p = (Profile) cboRecordProfile.getSelectedItem();
                    if (cboRecordPresets.getSelectedItem() != null) {
                        p.getVideo().setPreset(cboRecordPresets.getSelectedItem().toString());
                    }
                    s.setProfile(p);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                    String filename = "screenstudio-" + format.format(new Date()) + "." + s.getProfile().getMuxer();
                    if (!videoFolder.exists()) {
                        videoFolder = new File(System.getProperty("user.home"), "ScreenStudio");
                        if (!videoFolder.exists()) {
                            videoFolder.mkdir();
                        }
                    }
                    File out = new File(videoFolder, filename);
                    lblRecordTargetRecordingFile.setText("Video file " + out.getAbsolutePath());
                    String commandLine = Encoder.parse(s, out, videoFolder);
                    logCommand(out, commandLine);
                    startProcess(commandLine);
                }
            } catch (Exception ex) {
                System.out.println("An error has occured... " + ex.getMessage());
                txtStatus.setText("An error has occured... " + ex.getMessage());
                tglRecordVideo.setSelected(false);
                MsgLogs logs = new MsgLogs("Recording video...", ex, this, true);
                logs.setLocationByPlatform(true);
                logs.setVisible(true);
            }
        } else {
            stopStream("Stopped");
            tglStreamToServer.setEnabled(true);
            mnuStream.setEnabled(tabs.isEnabledAt(1));
        }
    }//GEN-LAST:event_tglRecordVideoActionPerformed

    private void mnuRecordItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_mnuRecordItemStateChanged
        if (!actionFromTray) {
            actionFromTray = true;
            tglRecordVideo.doClick();
            actionFromTray = false;
        }
    }//GEN-LAST:event_mnuRecordItemStateChanged

    private void mnuBarExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuBarExitActionPerformed
        stopStream("Exiting...");
        remote.stop();
        savePreferences();
        removeSystemTrayIcon();
        stopShortcuts();
        System.exit(0);
    }//GEN-LAST:event_mnuBarExitActionPerformed

    private void mnuOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuOpenActionPerformed
        JFileChooser chooser = new JFileChooser(loadedConfigurationFile);
        chooser.setSelectedFile(loadedConfigurationFile);
        chooser.setDialogTitle("ScreenStudio: Open Service file...");
        chooser.setFileFilter(new FileNameExtensionFilter("XML Configuration", "xml", "XML"));
        chooser.showOpenDialog(this);

        if (chooser.getSelectedFile() != null) {
            loadedConfigurationFile = chooser.getSelectedFile();
            updateControls(chooser.getSelectedFile());
            updateSourceOrigin();
            loadPreferences(true);
        }
    }//GEN-LAST:event_mnuOpenActionPerformed

    private void lblScreenDimenssionMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblScreenDimenssionMouseClicked
        setCaptureArea();
    }//GEN-LAST:event_lblScreenDimenssionMouseClicked

    private void mnuChkRemoteWebServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuChkRemoteWebServerActionPerformed
        if (mnuChkRemoteWebServer.isSelected()) {
            remote.start();
        } else {
            remote.stop();
        }
    }//GEN-LAST:event_mnuChkRemoteWebServerActionPerformed

    private void cboAudioMonitorsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cboAudioMonitorsItemStateChanged
        cboAudioMonitors.setToolTipText(cboAudioMonitors.getSelectedItem().toString());
    }//GEN-LAST:event_cboAudioMonitorsItemStateChanged

    private void mnuSetVideoFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSetVideoFolderActionPerformed
        JFileChooser chooser = new JFileChooser(videoFolder);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("ScreenStudio - Select Custom Video Folder");
        chooser.showOpenDialog(this);
        if (chooser.getSelectedFile() != null) {
            videoFolder = chooser.getSelectedFile();
        }
    }//GEN-LAST:event_mnuSetVideoFolderActionPerformed

    private void txtStatusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtStatusMouseClicked
        if (evt.getClickCount() == 2) {
            MsgLogs msg = new MsgLogs(lastLogs, this, true);
            msg.setVisible(true);
        }
    }//GEN-LAST:event_txtStatusMouseClicked

    private void mnuSetCaptureAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSetCaptureAreaActionPerformed
        setCaptureArea();
    }//GEN-LAST:event_mnuSetCaptureAreaActionPerformed

    private void mnuIdentifyScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuIdentifyScreenActionPerformed
        for (int i = 0; i < cboScreen.getItemCount(); i++) {
            Screen s = (Screen) cboScreen.getItemAt(i);
            new ScreenIdentifier(s.getLabel(), (int) s.getSize().getX() + 100, (int) s.getSize().getY() + 100).setVisible(true);
        }
    }//GEN-LAST:event_mnuIdentifyScreenActionPerformed

    private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviewActionPerformed
        Screen s = (Screen) cboScreen.getSelectedItem();
        int valign = JLabel.BOTTOM;
        int halign = JLabel.LEFT;
        switch (cboOverlayLayout.getSelectedIndex()) {
            case 0:
                valign = JLabel.BOTTOM;
                halign = JLabel.LEFT;
                break;
            case 1:
                valign = JLabel.TOP;
                halign = JLabel.LEFT;
                break;
            case 2:
                valign = JLabel.BOTTOM;
                halign = JLabel.RIGHT;
                break;
            case 3:
                valign = JLabel.TOP;
                halign = JLabel.RIGHT;
                break;
        }
        int h = 480;
        int w = 480;
        if (cboScreen.getSelectedIndex() > 0) {
            w = (int) ((s.getSize().getWidth() / s.getSize().getHeight()) * h);
            currentRenderer = new HTMLRenderer((int) s.getSize().getWidth(), (int) s.getSize().getHeight(), halign, valign);
        } else {
            h = (Integer) spinWebcamCaptureHeight.getValue();
            w = (Integer) spinWebcamCaptureWidth.getValue();
            currentRenderer = new HTMLRenderer((Integer) spinWebcamCaptureWidth.getValue(), (Integer) spinWebcamCaptureHeight.getValue(), halign, valign);
        }
        URL url = isURL(overlayEditor.getTextPane().getText());
        BufferedImage img;
        try {
            if (url == null) {
                img = currentRenderer.getImage(overlayEditor.getTextPane().getText());
            } else {
                img = currentRenderer.getImage(url);
            }
            PreviewBanner preview;

            preview = new PreviewBanner(img, s.getId(), w, h, this, true);
            preview.setVisible(true);
        } catch (Exception ex) {
            System.out.println("An error has occured... " + ex.getMessage());
            txtStatus.setText("An error has occured... " + ex.getMessage());
            MsgLogs logs = new MsgLogs("Previewing overlay...", ex, this, true);
            logs.setLocationByPlatform(true);
            logs.setVisible(true);
        }
    }//GEN-LAST:event_btnPreviewActionPerformed

    private void mnuResetPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuResetPreferencesActionPerformed
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node(this.getName());
            prefs.clear();
            prefs.flush();
            prefs = null;
            loadPreferences(false);
        } catch (BackingStoreException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mnuResetPreferencesActionPerformed

    private void mnuExportXMLToFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExportXMLToFileActionPerformed

        try {
            InputStream in = ScreenStudio.class.getResourceAsStream(Encoder.getXMLResourceName());
            File destFile = new File(videoFolder, "DefaultScreenStudio.xml");
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            out.write(buffer);
            out.flush();
            in.close();
            out.close();
            txtStatus.setText("Exported to " + destFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            txtStatus.setText("Export failed: " + ex.getMessage());
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            txtStatus.setText("Export failed: " + ex.getMessage());
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_mnuExportXMLToFileActionPerformed

    private void txtOptionRecordKeyKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtOptionRecordKeyKeyPressed
        txtOptionRecordKey.setText(("" + evt.getKeyChar()).toUpperCase());
    }//GEN-LAST:event_txtOptionRecordKeyKeyPressed

    private void txtOptionStreamKeyKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtOptionStreamKeyKeyPressed
        txtOptionStreamKey.setText(("" + evt.getKeyChar()).toUpperCase());
    }//GEN-LAST:event_txtOptionStreamKeyKeyPressed

    private void btnOptionsApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOptionsApplyActionPerformed
        shortcutRecording = "";
        shortcutStreaming = "";

        if (chkOptionRecordControl.isSelected()) {
            shortcutRecording += "control ";
        }
        if (chkOptionRecordAlt.isSelected()) {
            shortcutRecording += "alt ";
        }
        if (chkOptionRecordShift.isSelected()) {
            shortcutRecording += "shift ";
        }
        shortcutRecording += txtOptionRecordKey.getText();

        if (chkOptionStreamControl.isSelected()) {
            shortcutStreaming += "control ";
        }

        if (chkOptionStreamAlt.isSelected()) {
            shortcutStreaming += "alt ";
        }

        if (chkOptionStreamShift.isSelected()) {
            shortcutStreaming += "shift ";
        }
        shortcutStreaming += txtOptionStreamKey.getText();
        if (keyShortcuts != null) {
            initializeShortCuts();
        }
    }//GEN-LAST:event_btnOptionsApplyActionPerformed

    private void mnuCaptureWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuCaptureWindowActionPerformed

        txtStatus.setText("Capturing Window: Click on window to capture");
        new Thread(new Runnable() {

            @Override
            public void run() {
                Screen s = (Screen) cboScreen.getSelectedItem();
                if (s != null) {
                    try {
                        s.setSize(Screen.captureWindowArea());
                        lblScreenDimenssion.setText(s.getSize().toString().replaceAll("java.awt.Rectangle", ""));
                        txtStatus.setText("Captured " + lblScreenDimenssion.getText());
                    } catch (Exception ex) {
                        System.out.println("An error has occured... " + ex.getMessage());
                        txtStatus.setText("An error has occured... " + ex.getMessage());
                        MsgLogs logs = new MsgLogs("Capturing Windows Area...", ex, null, true);
                        logs.setLocationByPlatform(true);
                        logs.setVisible(true);
                    }
                }
            }
        }).start();

    }//GEN-LAST:event_mnuCaptureWindowActionPerformed

    private void mnuSystemCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSystemCheckActionPerformed
        if (!org.screenstudio.services.sources.SystemCheck.isSystemReady(true)) {
            txtStatus.setText("Some dependencies maybe missing...");
        }
    }//GEN-LAST:event_mnuSystemCheckActionPerformed

    private void mnuRefreshSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuRefreshSourcesActionPerformed
        try {
            updateSources();
        } catch (IOException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mnuRefreshSourcesActionPerformed

    private void setCaptureArea() {
        CaptureScreenSize capture = new CaptureScreenSize(this, true);
        Screen s = (Screen) cboScreen.getSelectedItem();
        capture.setVisible(true);
        if (capture.isSelected()) {
            s.setSize(capture.getBounds());
        } else {
            GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = g.getScreenDevices();
            for (GraphicsDevice d : devices) {
                if (s.getId().equals(d.getIDstring())) {
                    s.setSize(d.getDefaultConfiguration().getBounds());
                    break;
                }
            }
        }
        lblScreenDimenssion.setText(s.getSize().toString().replaceAll("java.awt.Rectangle", ""));
    }

    private URL isURL(String text) {
        URL urlTest = null;
        try {
            urlTest = new URL(text);
        } catch (MalformedURLException ex) {
            //do nothing...
        }
        return urlTest;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ScreenStudio s = new ScreenStudio();
                s.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOptionsApply;
    private javax.swing.JButton btnPreview;
    private javax.swing.JComboBox cboAudioMonitors;
    private javax.swing.JComboBox cboAudioSource;
    private javax.swing.JComboBox cboOverlayLayout;
    private javax.swing.JComboBox cboRTMPProfiles;
    private javax.swing.JComboBox cboRTMPServer;
    private javax.swing.JComboBox cboRTMPServices;
    private javax.swing.JComboBox cboRecordPresets;
    private javax.swing.JComboBox cboRecordProfile;
    private javax.swing.JComboBox cboRecordServices;
    private javax.swing.JComboBox cboScreen;
    private javax.swing.JComboBox cboStreamPresets;
    private javax.swing.JComboBox cboWebcamLayout;
    private javax.swing.JComboBox cbowebcamSource;
    private javax.swing.JCheckBox chkOptionRecordAlt;
    private javax.swing.JCheckBox chkOptionRecordControl;
    private javax.swing.JCheckBox chkOptionRecordShift;
    private javax.swing.JCheckBox chkOptionStreamAlt;
    private javax.swing.JCheckBox chkOptionStreamControl;
    private javax.swing.JCheckBox chkOptionStreamShift;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JLabel lblAudioSource;
    private javax.swing.JLabel lblDelay;
    private javax.swing.JLabel lblDelay1;
    private javax.swing.JLabel lblEncoders;
    private javax.swing.JLabel lblFrameRate;
    private javax.swing.JLabel lblMadeBY;
    private javax.swing.JLabel lblMadeFor;
    private javax.swing.JLabel lblOptionsRecordShortkey;
    private javax.swing.JLabel lblOptionsStreamShortkey;
    private javax.swing.JLabel lblOverlayLayout;
    private javax.swing.JLabel lblProfile;
    private javax.swing.JLabel lblRecordEncoders;
    private javax.swing.JLabel lblRecordPresets;
    private javax.swing.JLabel lblRecordProfile;
    private javax.swing.JLabel lblRecordTargetRecordingFile;
    private javax.swing.JLabel lblRemoteRTMPMessage;
    private javax.swing.JLabel lblRemoteRecordMessage;
    private javax.swing.JLabel lblScreen;
    private javax.swing.JLabel lblScreen2;
    private javax.swing.JLabel lblScreenDimenssion;
    private javax.swing.JLabel lblServer;
    private javax.swing.JLabel lblStreamName;
    private javax.swing.JLabel lblStreamPresets;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblWebcam;
    private javax.swing.JLabel lblWebcamLayout;
    private javax.swing.JLabel lblWebcamWidth;
    private javax.swing.JMenu mnuAdvanced;
    private javax.swing.JMenuBar mnuBar;
    private javax.swing.JMenuItem mnuBarExit;
    private javax.swing.JMenuItem mnuCaptureWindow;
    private javax.swing.JCheckBoxMenuItem mnuChkRemoteWebServer;
    private java.awt.MenuItem mnuExit;
    private javax.swing.JMenuItem mnuExportXMLToFile;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenuItem mnuIdentifyScreen;
    private javax.swing.JMenuItem mnuOpen;
    private java.awt.CheckboxMenuItem mnuRecord;
    private javax.swing.JMenuItem mnuRefreshSources;
    private javax.swing.JMenuItem mnuResetPreferences;
    private javax.swing.JMenuItem mnuSetCaptureArea;
    private javax.swing.JMenuItem mnuSetVideoFolder;
    private java.awt.MenuItem mnuShow;
    private java.awt.CheckboxMenuItem mnuStream;
    private javax.swing.JMenuItem mnuSystemCheck;
    private javax.swing.JPanel panOptions;
    private javax.swing.JPanel panOverlayEditor;
    private javax.swing.JPanel panRecord;
    private javax.swing.JPanel panSources;
    private javax.swing.JPanel panStream;
    private javax.swing.JSpinner spinFPS;
    private javax.swing.JSpinner spinWebcamCaptureHeight;
    private javax.swing.JSpinner spinWebcamCaptureWidth;
    private javax.swing.JSpinner spinWebcamOffset;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JToggleButton tglRecordVideo;
    private javax.swing.JToggleButton tglStreamToServer;
    private java.awt.PopupMenu trayMenu;
    private javax.swing.JTextField txtOptionRecordKey;
    private javax.swing.JTextField txtOptionStreamKey;
    private javax.swing.JTextField txtStatus;
    private javax.swing.JTextField txtStreamName;
    // End of variables declaration//GEN-END:variables

    @Override
    public void requestStart() {
        switch (tabs.getSelectedIndex()) {
            case 0:
                if (!tglRecordVideo.isSelected()) {
                    tglRecordVideo.doClick();
                }
                break;
            case 1:
                if (tglStreamToServer.isSelected()) {
                    tglStreamToServer.doClick();
                }
                break;
        }
    }

    @Override
    public void requestStop() {
        switch (tabs.getSelectedIndex()) {
            case 0:
                if (tglRecordVideo.isSelected()) {
                    tglRecordVideo.doClick();
                }
                break;
            case 1:
                if (tglStreamToServer.isSelected()) {
                    tglStreamToServer.doClick();
                }
                break;
        }
    }

    @Override
    public void listening(String localURL) {
        lblRemoteRTMPMessage.setText("Remote Listening on " + localURL);
        lblRemoteRecordMessage.setText(lblRemoteRTMPMessage.getText());
    }

    @Override
    public void onHotKey(HotKey hotkey) {
        String shortcut = "";

        if (hotkey.toString().contains("ctrl")) {
            shortcut += " control";
        }
        if (hotkey.toString().contains("alt")) {
            shortcut += " alt";
        }
        if (hotkey.toString().contains("shift")) {
            shortcut += " shift";
        }
        shortcut += " " + KeyEvent.getKeyText(hotkey.keyStroke.getKeyCode());

        shortcut = shortcut.trim().toUpperCase().replaceAll("  ", " ");
        if (shortcut.equals(shortcutRecording.toUpperCase())) {
            if (tglRecordVideo.isEnabled()) {
                tglRecordVideo.doClick();
            }
        } else if (shortcut.equals(shortcutStreaming.toUpperCase())) {
            if (tglStreamToServer.isEnabled()) {
                tglStreamToServer.doClick();
            }
        }
    }
}
