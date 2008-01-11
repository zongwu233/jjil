package facedetect.app;

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

import jjil.algorithm.ApplyMaskRgb;
import jjil.algorithm.DetectHaarMultiScale;
import jjil.algorithm.GrayShrink;
import jjil.algorithm.RgbAvg2Gray;
import jjil.algorithm.RgbShrink;
import jjil.android.RgbImageAndroid;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.RgbImage;
import android.content.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
    Bitmap imageInput = null;
    /** Input image as a jjil.core.RgbImage
     */
    jjil.core.RgbImage imInput = null;
    
    /**
     * Creates a new instance of DetectHaarParam
     */
    public DetectHaarParam(Resources r, int nMinScale, int nMaxScale) 
        throws IllegalArgumentException 
    {
        InputStream is  = r.openRawResource(R.raw.hcsb);
        this.dh = new DetectHaarMultiScale(is, nMinScale, nMaxScale);
    }
       
    public void Paint(Canvas g) {
        if (this.imageInput != null && this.imInput != null) {
            int nTargetWidth = Math.min(imInput.getWidth(),g.getBitmapWidth());
            int nTargetHeight = Math.min(imInput.getHeight(),g.getBitmapHeight());
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
                    g.drawARGB(0, 0, 255, 0); // green
                    g.drawBitmap(
                            RgbImageAndroid.toBitmap(rgb), 
                            new Rect(0, 0, rgb.getWidth(), rgb.getHeight()),
                            new Rect(
                            		(g.getBitmapWidth() - rgb.getWidth())/2, 
                            		(g.getBitmapHeight() - rgb.getHeight())/2,
                            		rgb.getWidth(),
                            		rgb.getHeight()),
                            new Paint());
                } else {
                    RgbImage rgb = (RgbImage) rs.Front();
                    g.drawARGB(0, 255, 0, 0); // red
                    g.drawBitmap(
                            RgbImageAndroid.toBitmap(rgb), 
                            new Rect(0, 0, rgb.getWidth(), rgb.getHeight()),
                            new Rect(
                            		(g.getBitmapWidth() - rgb.getWidth())/2, 
                            		(g.getBitmapHeight() - rgb.getHeight())/2,
                            		rgb.getWidth(),
                            		rgb.getHeight()),
                            new Paint());
                }
             }
        }
    }
    
    public void Push() {
        if (this.imageInput != null) {
            this.imInput = RgbImageAndroid.toRgbImage(this.imageInput);
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
    
    public void setImage(Bitmap imageInput) {
        this.imageInput = imageInput;       
    }
    
    public String toString() {
        return super.toString();
    }
}
