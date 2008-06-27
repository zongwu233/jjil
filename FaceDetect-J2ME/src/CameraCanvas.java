/*
 * CameraCanvas.java
 *
 * Created on August 20, 2006, 2:08 PM
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
    private class ScaleDescription {
        private final int nMinScale;
        private final int nMaxScale;
        private final String szName;
        
        public ScaleDescription(String szName, int nMinScale, int nMaxScale) {
            this.szName = szName;
            this.nMinScale = nMinScale;
            this.nMaxScale = nMaxScale;
        }
        public int MaxScale() {
            return nMaxScale;
        }
        public int MinScale() {
            return nMinScale;
        }
        public String Name() {
            return szName;
        }
    }
    
    /* For calculating barcode and displaying result.
     */
    private CharacterizeCamera cc;
    private DetectHaarParam dhp = null;      
    private Command cmdCapture=null;
    private final Command cmdExit;
    private final Command cmdFileCapture;
    private Hashtable htSourceCmds = new Hashtable();
    private Hashtable htResCmds = new Hashtable();
    private final FaceDetect midlet;
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
    private boolean oVideoControlVisible = false;
    private Player player=null;
    private VideoControl videoControl=null;

    /** Creates a new instance of CameraCanvas */
    CameraCanvas(FaceDetect midlet)
        throws jjil.core.Error, java.io.IOException
    {
        this.dhp = new DetectHaarParam(1, 40);
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
        if (!addcmdCapture()) {
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
            this.oCaptureFromFile = true;
        }
    }
    
    private boolean addcmdCapture()
    {
        if (createPlayer(this.szCurrentSource)) {
            cmdCapture = new Command("Capture", Command.SCREEN, 1);
            addCommand(cmdCapture);
            return true;
        }
        return false;
    }
      
    private boolean createPlayer(String szSource)
    {
        try {
            //this.oCaptureFromFile = false;
            this.player=Manager.createPlayer("capture://" + szSource);
            if (this.player != null) {
                this.player.realize();
                //Grab the video control and set it to the current display.
                this.videoControl=(VideoControl)(player.getControl("VideoControl"));
            }
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
        throws jjil.core.Error
    {
        if (this.player != null && this.player.getState() == Player.STARTED) {
            takeSnapshot();
            this.oResultShown = false;
        } else {
            // if (this.dhp != null) this.dhp.reset();
            repaint();
            start();
        }
    }
    
    private void reportJjilError(jjil.core.Error e) {
        e.printStackTrace();
        jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);
        String szMsg = eJ2me.getLocalizedMessage();
        Alert a = new Alert("JJIL Error", szMsg, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        Display.getDisplay(this.midlet).setCurrent(a);
    }
    
    public void paint(Graphics g)
    {
        try
        {
            if (this.dhp != null) this.dhp.Paint(g);
            if(szMessage1!=null)
            {
                Alert a = new Alert(szMessage1, szMessage2,
                        null, AlertType.ERROR);
                a.setTimeout(Alert.FOREVER);
                Display.getDisplay(this.midlet).setCurrent(a);
                szMessage1 = null;
                szMessage2 = null;
            }
        } catch (jjil.core.Error e) {
            reportJjilError(e);
        }
    } 
    
     synchronized void start()
    {
        if(player != null && this.player.getState() != player.STARTED)
        {
            try
            {
                this.player.start();
                this.videoControl.setVisible(true);
                this.oVideoControlVisible = true;
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
        if(player!=null && this.player.getState() == Player.STARTED)
        {
            try
            {
                if (this.videoControl != null) {
                    this.videoControl.setVisible(false);
                    this.oVideoControlVisible = false;
                }
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
            if (this.videoControl != null) {
                if (this.oVideoControlVisible) {
                    try {
                       doCapture();
                    } catch (jjil.core.Error e) {
                        reportJjilError(e);
                    }
                } else {
                    this.videoControl.setVisible(true);
                    this.oVideoControlVisible = true;
                }
            }
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
                    this.oCaptureFromFile = false;
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
                    ScaleDescription sd = (ScaleDescription) this.htResCmds.get(cmd);
                    this.dhp.setScale(sd.MinScale(), sd.MaxScale());
                }
            }
        }
    }
    
    public void keyPressed(int keyCode)
    {
        switch (getGameAction(keyCode)) {
            case FIRE:
                if (this.videoControl != null) {
                    if (this.oVideoControlVisible) {
                        try {
                           doCapture();
                        } catch (jjil.core.Error e) {
                            reportJjilError(e);
                        }
                    } else {
                        this.videoControl.setVisible(true);
                        this.oVideoControlVisible = true;
                    }
                }
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
        throws jjil.core.Error
    {
        if(player!=null)
        {
            try
            {
                Image lcdImage;
                if (this.oCaptureFromFile) {
                    lcdImage = javax.microedition.lcdui.Image.createImage(
                         "/lena.png"
                    );
                } else {
                    byte [] pngImage=videoControl.getSnapshot(null);
                    if (this.player != null) this.player.stop();
                    midlet.cameraCanvasCaptured();
                    lcdImage = Image.createImage(pngImage, 0, pngImage.length);
                }
                if (this.videoControl != null) {
                    this.videoControl.setVisible(false);
                    this.oVideoControlVisible = false;
                }
                if (this.dhp != null && lcdImage != null) this.dhp.setImage(lcdImage);
                repaint();
                serviceRepaints();
                if (this.dhp != null) this.dhp.Push();
                repaint();
                serviceRepaints();
            } catch (Exception e)
            {
                e.printStackTrace();
                Alert a = new Alert("Error while capturing image");
                a.setString(e.toString());
                a.setType(AlertType.ERROR);
                a.setTimeout(Alert.FOREVER);
                Display.getDisplay(this.midlet).setCurrent(a);
            }
        }
    }
}
