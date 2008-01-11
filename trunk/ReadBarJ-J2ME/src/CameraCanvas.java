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
    private final Command cmdFileCapture;
    private Hashtable htSourceCmds = new Hashtable();
    private Hashtable htResCmds = new Hashtable();
    private final ReadBarJ midlet;
    private int nCurrentHeight = 240;
    private int nCurrentWidth = 320;
    private final int nIdSettings = 2;
    private final int nIdSource = 1;
    private String szCurrentSource;
    //Two strings for displaying error messages
    private String szMessage1=null;
    private String szMessage2=null;
    private final String szRsSettings = "settings";
    private boolean oCaptureFromFile = false;
    private boolean oResultShown = false;
    private boolean oScreenUpdated = false;
    private Player player=null;
    private final int[][] rgxnCameraResolutions =
        {{320, 240},
        {640, 480},
        {1280, 960}
        };
    private int nCurrentBarcodeIndex = 1;
    // note: these coordinates are measured as fractions of a 640 x 480
    // image. 
    private final int[][] rgxnBarcodeParams = 
    {
        {195, 116, 253, 250},
//        {70, 116, 506, 250},
        {242, 192, 150, 61},
        {70, 178, 506, 125},
	{195, 178, 253, 125},
        {0, 0, 640, 480}
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
        this.cc = new CharacterizeCamera();
        int nCaptureSource = 0;
        for (Enumeration e = cc.getCaptureSources(); e.hasMoreElements();)  {
                String szSource = (String) e.nextElement();
                if (this.szCurrentSource == null) {
                    this.szCurrentSource = szSource;
                }
                Command comSource = 
                    new Command(
                        "Choose " + nCaptureSource++,
                        "Capture from " + szSource,
                        Command.ITEM, 
                        2);
                this.htSourceCmds.put(comSource, szSource);
                addCommand(comSource);
        }
        this.cmdFileCapture = new Command(
                        "Choose " + nCaptureSource++,
                        "Capture from file",
                        Command.ITEM, 
                        2);
        addCommand(this.cmdFileCapture);
        restoreSettings();
        setBcpFromIndex();
        if (addcmdCapture()) {
            addResolutionCommands();
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
        if (createPlayer(this.szCurrentSource)) {
            cmdCapture = new Command("Capture", Command.SCREEN, 1);
            addCommand(cmdCapture);
            setCaptureFormat();
            return true;
        }
        return false;
    }
    
    private void addResolutionCommands()
    {
        for (int i=0; i<this.rgxnCameraResolutions.length; i++) {
            String szRes = Integer.toString(this.rgxnCameraResolutions[i][0]) +
                "x" +
                Integer.toString(this.rgxnCameraResolutions[i][1]);
            Command command = new Command(
                szRes,
                "Capture at " + szRes,
                Command.ITEM,
                3
            );
            addCommand(command);
            this.htResCmds.put(command, this.rgxnCameraResolutions[i]);
        }
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
            g.setColor(0x00FF0000); // red
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(0x0000FF00); // green
            int clipLeft = this.bcp.dCropLeft * 
                    videoControl.getDisplayWidth() / 640 + 
                    videoControl.getDisplayX();
            int clipTop = this.bcp.dCropTop * 
                    videoControl.getDisplayHeight() / 480 +
                    videoControl.getDisplayY();
            int clipWidth = this.bcp.cCropWidth * 
                    videoControl.getDisplayWidth() / 640;
            int clipHeight = this.bcp.cCropHeight * 
                    videoControl.getDisplayWidth() / 480;
            g.fillRect(clipLeft, 0, clipWidth, getHeight());
            g.fillRect(0, clipTop, getWidth(), clipHeight);
        }
        this.bcp.Paint(g);
        if (!this.oResultShown) {
            if (bcp.getSuccessful()) {
                String sz = bcp.getBarcode();
                Alert a = new Alert("Barcode found: " + sz, 
                        sz, 
                        null, 
                        AlertType.INFO);
                a.setTimeout(1000);
                Display.getDisplay(this.midlet).setCurrent(a);
                this.oResultShown = true;
                //this.bcp.imageInput = null;
            } else if (bcp.getAttempted()) {
                Alert a = new Alert("Barcode not found", 
                        "No barcode found", 
                        null, 
                        AlertType.ERROR);
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
    
    private void setCaptureFormat() {
        Enumeration e = this.cc.getCaptureFormats();
        VideoFormat v = (VideoFormat) e.nextElement();
        this.szCaptureFormat = v.getEncoding();
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
            try {
               saveSettings();
            } catch (Exception ex) {
                Alert a = new Alert("Could not save settings: " + ex.getMessage());
                a.setTimeout(Alert.FOREVER);
                a.setType(AlertType.ERROR);
                Display.getDisplay(this.midlet).setCurrent(a);
           }
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
    }
    
    public void keyPressed(int keyCode)
    {
        switch (getGameAction(keyCode)) {
            case FIRE:
                doCapture();
                break;
            case UP:
                if (this.nCurrentBarcodeIndex > 0) {
                    this.nCurrentBarcodeIndex --;
                } else {
                    this.nCurrentBarcodeIndex = this.rgxnBarcodeParams.length-1;
                }
                setBcpFromIndex();
                repaint();
                break;
            case DOWN:
                if (this.nCurrentBarcodeIndex < this.rgxnBarcodeParams.length-1) {
                    this.nCurrentBarcodeIndex ++;
                } else {
                    this.nCurrentBarcodeIndex = 0;
                }
                setBcpFromIndex();
                repaint();
                break;
            default:
        }
    }
    
    private void restoreSettings()
    {
        try {
            RecordStore rs = RecordStore.openRecordStore(this.szRsSettings, 
                    true);
            try {
                byte[] bSource = rs.getRecord(this.nIdSource);
                this.szCurrentSource = new String(bSource);
                bSource = rs.getRecord(this.nIdSettings);
                ByteArrayInputStream bais = new ByteArrayInputStream(bSource);
                DataInputStream dis = new DataInputStream(bais);
                this.nCurrentHeight = dis.readInt();
                this.nCurrentWidth = dis.readInt();
                dis = null;
                bais = null;
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
    
    private void saveSettings() 
    {
        try {
            RecordStore rs = RecordStore.openRecordStore(this.szRsSettings, true);
            byte[] bSource = this.szCurrentSource.getBytes();
            setRecord(rs, this.nIdSource, bSource);
            bSource = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(this.nCurrentHeight);
            dos.writeInt(this.nCurrentWidth);
            dos.flush();
            baos.flush();
            byte[] bRes = baos.toByteArray();
            dos = null;
            baos = null;
            setRecord(rs, this.nIdSettings, bRes);
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
        this.bcp = new BarcodeParam(
                this.rgxnBarcodeParams[nCurrentBarcodeIndex][0],
                this.rgxnBarcodeParams[nCurrentBarcodeIndex][1],
                this.rgxnBarcodeParams[nCurrentBarcodeIndex][2],
                this.rgxnBarcodeParams[nCurrentBarcodeIndex][3]);        
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
                } else if (this.szCaptureFormat != null) {
                    String szCapture = this.szCaptureFormat + "&width=" +
                            this.nCurrentWidth + "&height=" +
                            this.nCurrentHeight;
                    byte [] image=videoControl.getSnapshot(szCapture);
                    //this.player.stop();
                    midlet.cameraCanvasCaptured();
                    lcdImage = Image.createImage(image, 0, image.length);
                } else {
                    Alert a = new Alert("Error while capturing image");
                    a.setString("No legal capture format exists!");
                    a.setType(AlertType.ERROR);
                    a.setTimeout(Alert.FOREVER);
                    Display.getDisplay(this.midlet).setCurrent(a);
                    return;
                }
                this.player.stop();
                this.videoControl.setVisible(false);
                this.bcp.setImage(lcdImage);
                repaint();
                serviceRepaints();
                this.bcp.Push();
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
