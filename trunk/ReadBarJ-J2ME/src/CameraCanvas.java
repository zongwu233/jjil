/*
 * CameraCanvas.java
 *
 * Created on August 22, 2006, 2:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2006 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 *
 * @author webb
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.rms.*;

public class CameraCanvas
        extends Canvas
        implements CommandListener
{
    
    /* For calculating barcode and displaying result.
     */
    private BarcodeParam bcp;
    private CharacterizeCamera cc;
    private Command cmdCapture=null;
    private final Command cmdExit;
    private Command cmdFileCapture;
    private Hashtable htFormatCmds = new Hashtable();
    private Hashtable htSourceCmds = new Hashtable();
    private Hashtable htResCmds = new Hashtable();
    private final ReadBarJ midlet;
    private int nCurrentHeight = 0;
    private int nCurrentWidth = 0;
    
    private int nCaptureSource = 0;

    private final int nIdSettings = 0;
    
    private String szCurrentSource;
    //Two strings for displaying error messages
    private String szMessage1=null;
    private String szMessage2=null;
    private final String szRsSettings = "settings";
    private boolean oCaptureFromFile = false;
    private boolean oResultShown = false;
    private boolean oScreenUpdated = false;
    private Player player=null;
    // there doesn't appear to be a reliable way to determine the
    // legal camera resolutions from the API. So here are some
    // reasonable guesses.
    private final int[][] rgxnCameraResolutions =
        {{160,120}, 
         {320, 240},
         {640, 480},
         {1280, 960}
        };

    private String szCaptureFormat = null;
    private VideoControl videoControl=null;

    /** Creates a new instance of CameraCanvas */
    CameraCanvas(ReadBarJ midlet)
    {
        this.midlet=midlet;
        cmdExit=new Command("Exit",Command.EXIT,1);
        addCommand(cmdExit);
        setCommandListener(this);

        // add a special command for capturing from file
        this.cmdFileCapture = new Command(
            "Choose " + this.nCaptureSource++,
            "Photo from file",
            Command.ITEM, 
            2);
        addCommand(this.cmdFileCapture);
 
        this.cc = new CharacterizeCamera();
        restoreSettings();
        this.bcp = new BarcodeParam();
        // add a command for vanilla capture. if it works add the other
        // source, resolution, and format commands
        if (addcmdCapture()) {
            setupCommands();
        } else {
            Alert a = new Alert(
                    "No camera!", 
                    "No cameras are available!", 
                    null, 
                    AlertType.ERROR);
            try {
                javax.microedition.lcdui.Image i = 
                     javax.microedition.lcdui.Image.createImage("/images/camera.png");
                a.setImage(i);
            } catch (Exception e) {
            }
            Display.getDisplay(midlet).setCurrent(a);
        }
    }
    
    private boolean addcmdCapture()
    {
        // if a source hasn't already been set use the first
        Enumeration e = cc.getCaptureSources();
        String szSource = (String) e.nextElement();
        // make sure there's a valid capture source
        if (this.szCurrentSource == null || this.szCurrentSource == "") {
            this.szCurrentSource = szSource;
        }

        if (createPlayer(this.szCurrentSource)) {
            cmdCapture = new Command("Capture", Command.SCREEN, 1);
            addCommand(cmdCapture);
            return true;
        }
        return false;
    }
    
    /**
     * The format and resolution commands are stored in a hashtable so
     * that when we get an input of one of them we can look up the appropriate
     * hashed value and determine what format or resolution we should switch
     * to.
     */
    private void addFormatCommands()
    {
        for (Enumeration e = this.cc.getCaptureFormats(); e.hasMoreElements();) {
            VideoFormat v = (VideoFormat) e.nextElement();
            String szFormat = v.getEncoding();
            int n = szFormat.indexOf("=");
            if (n >= 0) {
                szFormat = szFormat.substring(n+1);
            }
            String szCmdStr = "Photo in " + szFormat;
            if (this.szCaptureFormat == v.getEncoding()) {
                szCmdStr = "+ " + szCmdStr;
            }
            Command command = new Command(
                szFormat,
                szCmdStr,
                Command.ITEM,
                3
            );
            addCommand(command);
            this.htFormatCmds.put(command, v.getEncoding());
        }
    }
    
    private void addResolutionCommands()
    {
        for (int i=0; i<this.rgxnCameraResolutions.length; i++) {
            String szRes = Integer.toString(this.rgxnCameraResolutions[i][0]) +
                "x" +
                Integer.toString(this.rgxnCameraResolutions[i][1]);
            String szCmdStr = "Photo at " + szRes;
            if (this.nCurrentHeight == this.rgxnCameraResolutions[i][1]) {
                szCmdStr = "+ " + szCmdStr;
            }
            Command command = new Command(
                szRes,
                szCmdStr,
                Command.ITEM,
                3
            );
            addCommand(command);
            this.htResCmds.put(command, this.rgxnCameraResolutions[i]);
        }
    }
    
    private void addSourceCommands() 
    {
        for (Enumeration e = cc.getCaptureSources(); e.hasMoreElements();)  {
            String szSource = (String) e.nextElement();
            // make sure there's a valid capture source
            if (this.szCurrentSource == null || this.szCurrentSource == "") {
                this.szCurrentSource = szSource;
            }
            Command comSource = 
                new Command(
                    "Choose " + nCaptureSource++,
                    "Photo from " + szSource,
                    Command.ITEM, 
                    2);
            this.htSourceCmds.put(comSource, szSource);
            addCommand(comSource);
        }

    }
    
    private void clearCommands() 
    {
        for (Enumeration e = this.htFormatCmds.keys(); e.hasMoreElements();) {
            Command cmd = (Command) e.nextElement();
            removeCommand(cmd);
        }
        for (Enumeration e = this.htResCmds.keys(); e.hasMoreElements();) {
            Command cmd = (Command) e.nextElement();
            removeCommand(cmd);
        }
        for (Enumeration e = this.htSourceCmds.keys(); e.hasMoreElements();) {
            Command cmd = (Command) e.nextElement();
            removeCommand(cmd);
        }
        this.htFormatCmds.clear();
        this.htResCmds.clear();
        this.htSourceCmds.clear();
    }
    
    private boolean createPlayer(String szSource)
    {
        try {
            this.oCaptureFromFile = false;
            this.player=Manager.createPlayer("capture://" + szSource);
            this.player.realize();
            //Grab the video control and set it to the current display.
            this.videoControl=(VideoControl)(player.getControl("VideoControl"));
            if (this.videoControl==null)
            {
                discardPlayer();
                szMessage1="Unsupported:";
                szMessage2="Can't get videocontrol";
            }
            else 
            {
                this.videoControl.initDisplayMode(
                        VideoControl.USE_DIRECT_VIDEO,
                        this);
                //centre video, letting it be clipped if it's too big 
                int canvasWidth=getWidth();
                int canvasHeight=getHeight();
                int displayWidth=videoControl.getDisplayWidth();
                int displayHeight=videoControl.getDisplayHeight();
                int x=(canvasWidth-displayWidth)/2;
                int y=(canvasHeight-displayHeight)/2;
                this.videoControl.setDisplayLocation(x,y);
                }
        } catch(IOException ioe)
        {
            discardPlayer();
            szMessage1="IOException:";
            szMessage2=ioe.getMessage();
            return false;
        } catch(MediaException me)
        {
            discardPlayer();
            szMessage1="MediaException:";
            szMessage2=me.getMessage();
            return false;
        } catch(SecurityException se)
        {
            discardPlayer();
            szMessage1="SecurityException";
            szMessage2=se.getMessage();
            return false;
        }
        this.szCurrentSource = szSource;
        return true;
    }

    // Called in case of exception to make sure invalid players are closed
    private void discardPlayer()
    {
        if(player!=null)
        {
            player.close();
            player=null;
        }
        videoControl=null;
    }
    
    private void doCapture()
    {
        if (this.player.getState() == Player.STARTED) {
            takeSnapshot();
            this.oResultShown = false;
        } else {
            this.bcp.reset();
            repaint();
            start();
        }
    }
    
    public void paint(Graphics g)
    {
        if (this.bcp.imageInput == null) {
            g.setColor(0x00808080); // gray
            g.fillRect(0,0,getWidth(),getHeight());
        }
        this.bcp.paint(g);
        if (!this.oResultShown) {
            if (bcp.getSuccessful()) {
                // successful results are shown by the bcp paint method
            } else if (bcp.getAttempted()) {
                Alert a = new Alert("Barcode not found", 
                        "No barcode found", 
                        null, 
                        AlertType.WARNING);
                a.setTimeout(1000);
                Display.getDisplay(this.midlet).setCurrent(a);
                this.oResultShown = true;
           }
        }
        if(szMessage1!=null)
        {
            Alert a = new Alert(szMessage1, szMessage2,
                    null, AlertType.ERROR);
            a.setTimeout(Alert.FOREVER);
            Display.getDisplay(this.midlet).setCurrent(a);
            szMessage1 = null;
            szMessage2 = null;
        }
    } 
    
    
    private void setupCommands() {
        clearCommands();
        addSourceCommands();
        addFormatCommands();
        addResolutionCommands();
    }
    
     synchronized void start()
    {
        if((player!=null) && this.player.getState() != player.STARTED)
        {
            try
            {
                player.start();
                videoControl.setVisible(true);
            } catch(MediaException me)
            {
                szMessage1="Mediaexception:";
                szMessage2=me.getMessage();
            } catch(SecurityException se)
            {
                szMessage1="SecurityException";
                szMessage2=se.getMessage();
            }
        }
    }
    synchronized void stop()
    {
        if((player!=null)&& this.player.getState() == Player.STARTED)
        {
            try
            {
                videoControl.setVisible(false);
                player.stop();
                this.repaint();
            } catch(MediaException me)
            {
                szMessage1="MediaException:";
                szMessage2=me.getMessage();
            }
        }
    }
    
    public void commandAction(Command c,Displayable d)
    {
        if(c==cmdExit)
        {
            midlet.cameraCanvasExit();
        } 
        else if(c==cmdCapture)
        {
            doCapture();
        } else if (c == this.cmdFileCapture) {
            this.oCaptureFromFile = true;
            // don't restart player -- we just use the file instead of
            // the videoControl when we capture
        } else { // check source and resolution commands
            boolean oCommandFound = false;
            for (Enumeration cmds = this.htSourceCmds.keys(); 
                cmds.hasMoreElements() && !oCommandFound;) {
                Command cmd = (Command) cmds.nextElement();
                if (cmd == c) {
                    oCommandFound = true;
                     String szOldSource = this.szCurrentSource;
                    String szSource = (String) this.htSourceCmds.get(cmd);
                    discardPlayer();
                    if (!createPlayer(szSource)) {
                        Alert a = new Alert(szSource + " can't capture", 
                                "Can't capture from " + szSource,
                                null,
                                AlertType.ERROR);
                        a.setTimeout(Alert.FOREVER);
                        this.htSourceCmds.remove(cmd);
                        this.removeCommand(cmd);
                        createPlayer(szOldSource);
                    }
                }
            }
            for (Enumeration cmds = this.htFormatCmds.keys();
                cmds.hasMoreElements() && !oCommandFound;) {
                Command cmd = (Command) cmds.nextElement();
                if (cmd == c) {
                    oCommandFound = true;
                    this.szCaptureFormat = (String) this.htFormatCmds.get(cmd);
                }
            }
            if (!oCommandFound) {
                for (Enumeration cmds = this.htResCmds.keys();
                    cmds.hasMoreElements() && !oCommandFound;) {
                    Command cmd = (Command) cmds.nextElement();
                    if (cmd == c) {
                        oCommandFound = true;
                        int[] rgnRes = (int[]) this.htResCmds.get(cmd);
                        this.nCurrentWidth = rgnRes[0];
                        this.nCurrentHeight = rgnRes[1];
                    }
                }
            }
            try {
               saveSettings();
            } catch (Exception ex) {
                Alert a = new Alert("Could not save settings: " + ex.getMessage());
                a.setTimeout(Alert.FOREVER);
                a.setType(AlertType.ERROR);
                Display.getDisplay(this.midlet).setCurrent(a);
           }
            // redisplay commands to show current selection
            setupCommands();
        }
    }
    
    public void keyPressed(int keyCode)
    {
        switch (getGameAction(keyCode)) {
            case FIRE:
                doCapture();
                break;
            default:
        }
    }
    
    /**
     * Note that this and the saveSettings procedure have to stay in sync
     */
    private void restoreSettings()
    {
        try {
            RecordStore rs = RecordStore.openRecordStore(this.szRsSettings, 
                    true);
            try {
                RecordEnumeration re = rs.enumerateRecords(null, null, false);
                if (this.cc != null && re.hasNextElement()) {
                    byte[] bSource = re.nextRecord();
                    re.destroy();
                    if (bSource != null) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(bSource);
                        DataInputStream dis = new DataInputStream(bais);
                        int nSource = dis.readInt();
                        int nCapture = dis.readInt();
                        this.nCurrentHeight = dis.readInt();
                        this.nCurrentWidth = dis.readInt();
                        dis = null;
                        bais = null;
                        int n=0;
                        // translate stored indices for source and format back into
                        // strings
                        for (Enumeration e = this.cc.getCaptureSources(); e.hasMoreElements(); n++) {
                            if (n == nSource) {
                                this.szCurrentSource = (String) e.nextElement();
                                break;
                            } else {
                                e.nextElement();
                            }
                        }
                        n = 0;
                        for (Enumeration e = this.cc.getCaptureFormats(); e.hasMoreElements(); n++) {
                            if (n == nCapture) {
                                Object vFormat = e.nextElement();
                                this.szCaptureFormat = vFormat.toString();
                                break;
                            } else {
                                e.nextElement();
                            }
                        }
                   }
                }
                rs.closeRecordStore();
            } catch (Exception ex) {
                /* the record store might not be created yet. We use the
                 * default settings, which have already been set.
                 */
                rs.closeRecordStore();
                return;
            }
        } catch (RecordStoreException rex)  {
            return;
        }
    }
    
    /**
     * Note that this and the restoreSettings procedure have to stay in sync
     */
    private void saveSettings() 
    {
        try {
            RecordStore rs = RecordStore.openRecordStore(this.szRsSettings, true);
            // first clear all existing records
            for (RecordEnumeration e = rs.enumerateRecords(null, null, false);
                e.hasNextElement();) {
                rs.deleteRecord(e.nextRecordId());
            }
            // I tried storing the source as a string but couldn't read it back
            // in for some reason. So I store it as an index into the list
            // of sources, which shouldn't change
            int nSource = 0;
            for (Enumeration e = this.cc.getCaptureSources(); e.hasMoreElements(); nSource++) {
                String szSource = (String) e.nextElement();
                if (szSource == this.szCurrentSource) {
                    break;
                }
            }
            // Similarly, couldn't store capture format as a string,
            // so store it as an index instead
            int nCapture = 0;
            for (Enumeration e = this.cc.getCaptureFormats(); e.hasMoreElements(); nCapture++) {
                Object vFormat = e.nextElement();
                String szFormat = vFormat.toString();
                if (szFormat == this.szCaptureFormat) {
                    break;
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(nSource);
            dos.writeInt(nCapture);
            dos.writeInt(this.nCurrentHeight);
            dos.writeInt(this.nCurrentWidth);
            dos.flush();
            baos.flush();
            byte[] bRes = baos.toByteArray();
            dos = null;
            baos = null;
            rs.addRecord(bRes, 0, bRes.length);
            bRes = null;
            rs.closeRecordStore();
        } catch (Exception ex) {
            Alert a = new Alert("Could not save settings: " + ex.getMessage());
            a.setTimeout(Alert.FOREVER);
            a.setType(AlertType.ERROR);
            Display.getDisplay(this.midlet).setCurrent(a);
        }
    }
    
    private void setBcpFromIndex() 
    {
        this.bcp = new BarcodeParam();        
    }
    
    /** set a particular record in an open record store to a particular 
     * byte array.
     * @param rs the record store which will be updated.
     * @param nRecordId the record id to update. This will be created if it 
     *        isn't present. Other records may also be created.
     * @param bData the byte array that the record is to be set to.
     * @returns true if the record was successfully set; false if not.
     *
     */
    private void setRecord(RecordStore rs, int nRecordId, byte[] bData)
        throws IllegalArgumentException, 
            RecordStoreNotOpenException,
            RecordStoreException
    {
        if (nRecordId <= 0) {
            throw new IllegalArgumentException("nRecordId should be > 0, but" +
                    "is " + nRecordId);
        }
        try {
            /* try to read the record of concern. This will throw 
             * InvalidRecordIDException if the record doesn't exist, and
             * other exceptions if the RecordStore can't be read. 
             */
            rs.getRecord(nRecordId);
            /* the record is there; set it to the new value.
             */
            rs.setRecord(nRecordId, bData, 0, bData.length);
        } catch (InvalidRecordIDException iriex) {
            /* the record wasn't there. Attempt to write it. There doesn't
             * seem to be any way to ensure that a particular record id
             * is returned from the addRecord so we just keep adding it
             * until we get the right returned number. Note that this
             * might add extra unnecessary records.
             */
            int nReturnedId;
            do {
                 nReturnedId = rs.addRecord(bData, 0, bData.length);
            } while (nRecordId != nReturnedId);                
        }
    }
    
    private void takeSnapshot()
    {
        if(player!=null)
        {
            try
            {
                Image lcdImage;
                if (this.oCaptureFromFile) {
                    lcdImage = javax.microedition.lcdui.Image.createImage(
                         "/images/barcode.png"
                    );
                } else {
                    String szCapture = this.szCaptureFormat;
                    if (this.nCurrentWidth != 0) {
                        if (szCapture != null && szCapture != "") {
                            szCapture += "&";
                        }
                        szCapture += "width=" + this.nCurrentWidth + 
                                "&height=" + this.nCurrentHeight;
                    }
                    byte [] image=videoControl.getSnapshot(szCapture);
                    midlet.cameraCanvasCaptured();
                    lcdImage = Image.createImage(image, 0, image.length);
                }
                this.player.stop();
                this.videoControl.setVisible(false);
                this.bcp.setImage(lcdImage);
                repaint();
                serviceRepaints();
                this.bcp.push();
                repaint();
                serviceRepaints();
            } catch (Exception e)
            {
                e.printStackTrace();
                Alert a = new Alert("Error while capturing image");
                a.setString(e.getMessage());
                a.setType(AlertType.ERROR);
                a.setTimeout(Alert.FOREVER);
                Display.getDisplay(this.midlet).setCurrent(a);
            }
        }
    }
}
