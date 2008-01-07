package readbarj.app;
/*
 * BarcodeParam.java
 *
 * Created on October 10, 2006, 3:07 PM
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
import jjil.algorithm.Gray2Rgb;
import jjil.algorithm.GrayCrop;
import jjil.algorithm.GrayShrink;
import jjil.android.RgbImageAndroid;
import jjil.core.Gray8Image;
import jjil.core.RgbImage;
import jjil.core.Sequence;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import barcode.DetectBarcode;
import barcode.ReadBarcode;

/**
 * Hungarian notation prefix: bcp.
 * @author webb
 */
public class BarcodeParam {
	Context context;
    /** Image cropped to the region where the barcode should be
     */
    Gray8Image imageCropped;
    /** Image to display 
     */
    Bitmap imageDisplay = null;
    /** Input image.
     */
    Bitmap imageInput;
    /** Cropping window height. From 1-480
     */
    int cCropHeight;
    /** Cropping window left edge. From 0-639.
     */
    int dCropLeft;
    /** Cropping window top. From 0-479.
     */
    int dCropTop;
    /** Cropping window width. From 1-640.
     */
    int cCropWidth;
    /** Whether or not barcode detection has been attempted.
     */
    boolean oAttempted = false;
    /** Whether barcode detection was successful or not.
     */
    boolean oSuccessful = false;
    
    ReadBarcode rb = null;
    /** The detected barcode (if any).
     */
    String szBarcode;
    /** Creates a new instance of BarcodeParam */
    public BarcodeParam(
    		Context context,
            int dCropLeft,
            int dCropTop,
            int cCropWidth,
            int cCropHeight) throws IllegalArgumentException {
    	this.context = context;
        if (dCropLeft < 0 || dCropLeft > 639 ||
            dCropTop < 0 || dCropTop > 479 ||
            cCropWidth < 1 || cCropWidth > 640 ||
            cCropHeight < 1 || cCropHeight > 480) {
            throw new IllegalArgumentException("Cropping window is illegal: " +
                    "(" + dCropLeft + "," + dCropTop + "," + cCropWidth + 
                    "," + cCropHeight + ")");
        }
        this.dCropLeft = dCropLeft;
        this.dCropTop = dCropTop;
        this.cCropWidth = cCropWidth;
        this.cCropHeight = cCropHeight;
    }
    
    public boolean getAttempted() {
        return this.oAttempted;
    }
    
    public String getBarcode() {
        return this.szBarcode;
    }
    
    public boolean getSuccessful() {
        return this.oSuccessful;
    }
    
    public void Paint(Canvas g) {
        if (this.imageInput != null) {
            if (this.oAttempted) {
                if (this.imageDisplay == null) {
                    if (this.imageCropped.getWidth() > g.getBitmapWidth() ||
                        this.imageCropped.getHeight() > g.getBitmapHeight()) {
                        int nWidth = Math.min(this.imageCropped.getWidth(), 
                                g.getBitmapWidth());
                        int nHeight = Math.min(this.imageCropped.getHeight(), 
                                g.getBitmapHeight());
                        GrayShrink rs = new GrayShrink(nWidth, nHeight);
                        Sequence seq = new Sequence(rs);
                        seq.Add(new Gray2Rgb());
                        seq.Push(this.imageCropped);
                        this.imageDisplay = RgbImageAndroid.toBitmap((RgbImage)seq.Front());
                    } else {
                        Gray2Rgb g2r = new Gray2Rgb();
                        g2r.Push(this.imageCropped);
                        this.imageDisplay = RgbImageAndroid.toBitmap((RgbImage)g2r.Front());
                    }
                }
                g.drawBitmap(
                        this.imageDisplay, 
                        new Rect(0, 0, this.imageDisplay.width(), this.imageDisplay.height()),
                        new Rect(
                        		(g.getBitmapWidth() - this.imageDisplay.width())/2, 
                        		(g.getBitmapHeight() - this.imageDisplay.height())/2,
                        		this.imageDisplay.width(),
                        		this.imageDisplay.height()),
                        new Paint());
                
                // draw approximate barcode rectangle
                int nTL = this.rb.getLeftApproxPos() * g.getBitmapWidth()
                    / this.imageCropped.getWidth();
                int nTR = this.rb.getRightApproxPos() * g.getBitmapWidth()
                    / this.imageCropped.getWidth();
                int nBL = nTL + 
                        this.rb.getLeftApproxSlope() * g.getBitmapHeight() / 256
                        * g.getBitmapWidth() / this.imageCropped.getWidth();
                int nBR = nTR + 
                        this.rb.getRightApproxSlope() * g.getBitmapHeight() / 256
                        * g.getBitmapWidth() / this.imageCropped.getWidth();
                Paint p = new Paint();                
                p.setARGB(255, 0, 255, 0); // Green
                g.drawLine(nTL, 0, nBL, g.getBitmapHeight(), p);
                g.drawLine(nTR, 0, nBR, g.getBitmapHeight(), p);
            } else {
                Paint p = new Paint();                
                p.setARGB(255, 255, 0, 0); // Green
                g.drawRect(
                        (float) this.dCropLeft * g.getBitmapWidth() / 640,
                        (float) this.dCropTop * g.getBitmapHeight() / 480,
                        (float) this.cCropWidth * g.getBitmapWidth() / 640,
                        (float) this.cCropHeight * g.getBitmapHeight() / 480,
                        p);
            }
        }
    }
    
    public void Push() {
        this.oAttempted = true;
        this.imageDisplay = null;
        jjil.core.RgbImage inimg = RgbImageAndroid.toRgbImage(this.imageInput);
        DetectBarcode db = new DetectBarcode(20000);
        db.Push(inimg);
        
        this.rb = new ReadBarcode();
        Sequence seq = new Sequence();
        int dTopLeftX = this.dCropLeft * inimg.getWidth() / 640;
        int dTopLeftY = this.dCropTop * inimg.getHeight() / 480;
        int cWidth = this.cCropWidth * inimg.getWidth() / 640;
        int cHeight = this.cCropHeight * inimg.getHeight() / 480;
        GrayCrop crop = new GrayCrop(dTopLeftX, dTopLeftY, cWidth, cHeight);
        seq.Add(crop);
        seq.Push(inimg);
        Gray8Image imageResult = (Gray8Image) seq.Front();
        this.rb.Push(inimg,dTopLeftX,dTopLeftY,cWidth,cHeight);
        this.imageCropped = imageResult;
       // this.imageCropped = ((RgbImage) g2r.Front()).getImage();
        if (this.rb.getSuccessful()) {
            this.oSuccessful = true;
            this.szBarcode = this.rb.getCode();
        } else {
            this.oSuccessful = false;
            this.szBarcode = null;
        }
     }
    
    public void reset() {
        this.imageCropped = null;
        this.imageInput = null;
        this.oAttempted = false;
        this.oSuccessful = false;
    }
    
    public void setImage(Bitmap imageInput) {
        this.oAttempted = false;
        this.imageInput = imageInput;       
    }
    
    public String toString() {
        return super.toString() + " (" + this.dCropLeft + "," + this.dCropTop + 
                "," + this.cCropWidth + "," + this.cCropHeight + ")";
    }
}
