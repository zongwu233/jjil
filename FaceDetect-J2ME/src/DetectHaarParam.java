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
import java.io.InputStream;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import jjil.algorithm.ApplyMaskRgb;
import jjil.algorithm.DetectHaarMultiScale;
import jjil.algorithm.GrayConnComp;
import jjil.algorithm.GrayShrink;
import jjil.algorithm.RgbAvg2Gray;
import jjil.algorithm.RgbShrink;
import jjil.core.Gray8Image;
import jjil.core.Rect;
import jjil.core.RgbImage;
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
     * Creates a new instance of DetectHaarParam, which will scan
     * an image for faces using a Haar cascade over a range of scales.
     * @param nMinScale minimum scale factor. The image will be reduced
     * by at least this factor horizontally and vertically before face
     * detection
     * @param nMaxScale maximum scale factor. The image will be reduced 
     * by at most this factor horizontally and vertically before face
     * detection
     * @throws jjil.core.Error if the Haar cascade format is illegal,
     * or verious other tests fail.
     * @throws java.io.IOException if the Haar cascade can't be loaded
     * from the input file.
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
       
    public void paint(Graphics g) 
        throws jjil.core.Error
    {
        if (this.imageInput != null && this.imInput != null) {
            int nTargetWidth = Math.min(imInput.getWidth(),g.getClipWidth());
            int nTargetHeight = Math.min(imInput.getHeight(),g.getClipHeight());
            // convert the image to gray and then
            // warp the image to the clipping size
            RgbShrink rs = new RgbShrink(nTargetWidth, nTargetHeight);
            rs.push(this.imInput);
            
            if (this.dh != null && !this.dh.isEmpty()) {
                jjil.core.Gray8Image imMask = null;
                jjil.core.Image image = dh.getFront();
                if (image instanceof jjil.core.Gray8Image) {
                   imMask = (Gray8Image) image;
                   GrayConnComp gcc = new GrayConnComp();
                   gcc.push(imMask);
                   if (gcc.getComponents()> 0) {
                       // found a face
                       Rect r = gcc.getComponent(0);
                       // calculate center of face in mask coordinates
                       int x = r.getLeft() + r.getWidth() / 2;
                       int y = r.getTop() + r.getHeight() / 2;
                       // convert from mask to original image
                       x = (x * this.imInput.getWidth()) / imMask.getWidth();
                       y = (y * this.imInput.getHeight()) / imMask.getHeight();
                       // (x,y) is the center of the detected face
                   }
                } else if (image instanceof jjil.core.Gray32Image) {
                    jjil.algorithm.Gray322Gray8 g322g8 = 
                            new jjil.algorithm.Gray322Gray8();
                    g322g8.push(image);
                    imMask = (Gray8Image) g322g8.getFront();
                } else if (image instanceof jjil.core.RgbImage) {
                    RgbAvg2Gray r2g = new RgbAvg2Gray();
                    r2g.push(image);
                    imMask = (Gray8Image) r2g.getFront();
                }
                if (imMask != null) {
                    // shrink the mask to fit the display
                    GrayShrink gs = new GrayShrink(nTargetWidth, nTargetHeight);
                    gs.push(imMask);
                    imMask = (Gray8Image) gs.getFront();
                    // combine the gray image and the mask to make a displayable image
                    ApplyMaskRgb am = new ApplyMaskRgb();
                    RgbImage rgb = am.push((RgbImage)rs.getFront(), imMask);
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
                    RgbImage rgb = (RgbImage) rs.getFront();
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
    
    public void push() 
        throws jjil.core.Error
    {
        if (this.imageInput != null) {
            this.imInput = RgbImageJ2me.toRgbImage(this.imageInput);
            RgbAvg2Gray rg = new RgbAvg2Gray();
            rg.push(this.imInput);
            if (this.dh != null) this.dh.push(rg.getFront());
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
