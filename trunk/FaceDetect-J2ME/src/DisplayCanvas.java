/*
 * DisplayCanvas.java
 *
 * Created on August 20, 2006, 2:24 PM
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
import javax.microedition.lcdui.*;

public class DisplayCanvas 
    extends Canvas
    implements CommandListener
{
    private final FaceDetect midlet;
    private Image image=null;
     /** Creates a new instance of DisplayCanvas */
    DisplayCanvas(FaceDetect midlet)
    {
        this.midlet=midlet;
        addCommand(new Command("Back",Command.BACK,1));
        setCommandListener(this);
    }
    
    public void paint(Graphics g)
    {
        g.setColor(0x0000FFFF);// cyan
        g.fillRect(0,0,getWidth(),getHeight());
        if(image!=null)
        {
            g.drawImage(image,getWidth()/2,getHeight()/2,Graphics.VCENTER|Graphics.HCENTER);
        }
    }
    
    void setImage(byte[]pngImage)
    {
        image=Image.createImage(pngImage,0,pngImage.length);
    }
    
    void setImage(Image inimg)
    {
        image=inimg;
    }
    
    public void commandAction(Command c,Displayable d)
    {
        midlet.displayCanvasBack();
    }
 }
