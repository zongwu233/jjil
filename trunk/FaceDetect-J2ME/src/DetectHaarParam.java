/*
 * DetectHaarParam.java
 *
 * Created on October 7, 2006, 3:07 PM
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
import java.io.*;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import jjil.algorithm.DetectHaarMultiScale;
import jjil.core.*;
import jjil.algorithm.*;
import jjil.j2me.RgbImageJ2me;
/**
 * Hungarian notation prefix: dhp.
 * @author webb
 */
public class DetectHaarParam {
    /** DetectHaar pipeline
     */
    DetectHaarMultiScale dh = null;
    /** Input image.
     */
    Image imageInput = null;
    /** Input image as a jjil.core.RgbImage
     */
    jjil.core.RgbImage imInput = null;
    
    /**
     * Creates a new instance of DetectHaarParam
     */
    public DetectHaarParam(int nMinScale, int nMaxScale) 
        throws jjil.core.Error, java.io.IOException
    {
    	// Note: you can detect other features than frontal faces by
    	// changing HCSB.txt in this directory to one of the files in
    	// the OtherCascades subdirectory. See the README file there
    	// for more information.
        InputStream is  = getClass().getResourceAsStream("HCSB.txt");
        this.dh = new DetectHaarMultiScale(is, nMinScale, nMaxScale);
    }
       
    public void Paint(Graphics g) 
        throws jjil.core.Error
    {
        if (this.imageInput != null && this.imInput != null) {
            int nTargetWidth = Math.min(imInput.getWidth(),g.getClipWidth());
            int nTargetHeight = Math.min(imInput.getHeight(),g.getClipHeight());
            // convert the image to gray and then
            // warp the image to the clipping size
            RgbShrink rs = new RgbShrink(nTargetWidth, nTargetHeight);
            rs.Push(this.imInput);
            
            if (this.dh != null && !this.dh.Empty()) {
                jjil.core.Gray8Image imMask = null;
                jjil.core.Image image = dh.Front();
                if (image instanceof jjil.core.Gray8Image) {
                   imMask = (Gray8Image) image;
                } else if (image instanceof jjil.core.Gray32Image) {
                    jjil.algorithm.Gray322Gray8 g322g8 = 
                            new jjil.algorithm.Gray322Gray8();
                    g322g8.Push(image);
                    imMask = (Gray8Image) g322g8.Front();
                } else if (image instanceof jjil.core.RgbImage) {
                    RgbAvg2Gray r2g = new RgbAvg2Gray();
                    r2g.Push(image);
                    imMask = (Gray8Image) r2g.Front();
                }
                if (imMask != null) {
                    // shrink the mask to fit the display
                    GrayShrink gs = new GrayShrink(nTargetWidth, nTargetHeight);
                    gs.Push(imMask);
                    imMask = (Gray8Image) gs.Front();
                    // combine the gray image and the mask to make a displayable image
                    ApplyMaskRgb am = new ApplyMaskRgb();
                    RgbImage rgb = am.Push((RgbImage)rs.Front(), imMask);
                    g.setColor(0x0000FF00); // green
                    g.fillRect(
                            g.getClipX(),
                            g.getClipY(),
                            g.getClipWidth(),
                            g.getClipHeight());
                    g.drawImage(
                            RgbImageJ2me.toImage(rgb), 
                            g.getClipX() + (g.getClipWidth() - rgb.getWidth())/2, 
                            g.getClipY() + (g.getClipHeight() - rgb.getHeight())/2, 
                            0);
                } else {
                    RgbImage rgb = (RgbImage) rs.Front();
                    g.setColor(0x00FF0000); // red
                    g.fillRect(
                            g.getClipX(),
                            g.getClipY(),
                            g.getClipWidth(),
                            g.getClipHeight());
                    g.drawImage(
                            RgbImageJ2me.toImage(rgb), 
                            g.getClipX() + (g.getClipWidth() - rgb.getWidth())/2, 
                            g.getClipY() + (g.getClipHeight() - rgb.getHeight())/2, 
                            0);
                }
             }
        }
    }
    
    public void Push() 
        throws jjil.core.Error
    {
        if (this.imageInput != null) {
            this.imInput = RgbImageJ2me.toRgbImage(this.imageInput);
            RgbAvg2Gray rg = new RgbAvg2Gray();
            rg.Push(this.imInput);
            if (this.dh != null) this.dh.Push(rg.Front());
        }
     }
    
    public void reset() {
        this.imageInput = null;
    }
    
    public void setScale(int nMinScale, int nMaxScale) {
        if (this.dh != null) {
            this.dh.setScale(nMinScale, nMaxScale);
        }
    }
    
    public void setImage(Image imageInput) {
        this.imageInput = imageInput;       
    }
    
    public String toString() {
        return super.toString();
    }
}
