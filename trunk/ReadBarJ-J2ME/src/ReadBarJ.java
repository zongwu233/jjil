/*
 * ReadBarJ.java
 *
 * Created on August 22, 2006, 2:54 PM
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
import java.util.Enumeration;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import jjil.core.*;
import jjil.algorithm.*;
/**
 *
 * @author  webb
 * @version
 */

public class ReadBarJ extends MIDlet {
    private CameraCanvas cameraCanvas=null;
    private DisplayCanvas displayCanvas=null;
    
    public ReadBarJ() {
        
    }
    
    public void startApp() {
        Displayable current=Display.getDisplay(this).getCurrent();      
        if(current==null) {
            // first call
            if (System.getProperty("supports.video.capture").equals("true")) {
                cameraCanvas=new CameraCanvas(this);
                displayCanvas=new DisplayCanvas(this);
                Display.getDisplay(this).setCurrent(cameraCanvas);
                cameraCanvas.start();
            }   else {
                Alert a = new Alert(
                        "No camera!", 
                         "This phone doesn't support capture!", 
                        null, 
                        AlertType.ERROR);
                try {
                    javax.microedition.lcdui.Image i = 
                         javax.microedition.lcdui.Image.createImage("/images/camera.png");
                    a.setImage(i);
                } catch (Exception e) {
                }
                a.setTimeout(Alert.FOREVER);
                Display.getDisplay(this).setCurrent(a);
            }
        } else {
            //returningfrompauseApp
            if(current==cameraCanvas) {
                cameraCanvas.start();
            }
            Display.getDisplay(this).setCurrent(current);
        }
    }
    
    public void pauseApp() {
        if(Display.getDisplay(this).getCurrent()==cameraCanvas) {
            cameraCanvas.stop();
        }
    }
    public void destroyApp(boolean unconditional) {
        if(Display.getDisplay(this).getCurrent()==cameraCanvas) {
            cameraCanvas.stop();
        }
    }
    
    private void exitRequested() {
        destroyApp(false);
        notifyDestroyed();
    }
    
    void cameraCanvasExit() {
        exitRequested();
    }
    
    void cameraCanvasCaptured() {
        cameraCanvas.stop();
    }
    
    void displayCanvasBack() {
        Display.getDisplay(this).setCurrent(cameraCanvas);
        cameraCanvas.start();
    }
}