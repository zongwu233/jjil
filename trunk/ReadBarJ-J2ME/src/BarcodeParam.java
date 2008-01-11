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
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import jjil.algorithm.*;
import jjil.core.Gray8Image;
import jjil.core.RgbImage;
import jjil.core.Sequence;
import jjil.j2me.RgbImageJ2me;
import barcode.ReadBarcode;

/**
 * Hungarian notation prefix: bcp.
 * @author webb
 */
public class BarcodeParam {
    /** Image cropped to the region where the barcode should be
     */
    Gray8Image imageCropped;
    /** Image to display 
     */
    Image imageDisplay = null;
    /** Input image.
     */
    Image imageInput;
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
            int dCropLeft,
            int dCropTop,
            int cCropWidth,
            int cCropHeight) throws IllegalArgumentException {
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
    
    public void Paint(Graphics g) {
        if (this.imageInput != null) {
            if (this.oAttempted) {
                if (this.imageDisplay == null) {
                    if (this.imageCropped.getWidth() > g.getClipWidth() ||
                        this.imageCropped.getHeight() > g.getClipHeight()) {
                        int nWidth = Math.min(this.imageCropped.getWidth(), 
                                g.getClipWidth());
                        int nHeight = Math.min(this.imageCropped.getHeight(), 
                                g.getClipHeight());
                        GrayShrink rs = new GrayShrink(nWidth, nHeight);
                        Sequence seq = new Sequence(rs);
                        seq.Add(new Gray2Rgb());
                        seq.Push(this.imageCropped);
                        this.imageDisplay = RgbImageJ2me.toImage((RgbImage)seq.Front());
                    } else {
                        Gray2Rgb g2r = new Gray2Rgb();
                        g2r.Push(this.imageCropped);
                        this.imageDisplay = RgbImageJ2me.toImage((RgbImage)g2r.Front());
                    }
                }
                g.drawImage(
                        this.imageDisplay, 
                        g.getClipWidth()/2, 
                        g.getClipHeight()/2, 
                        Graphics.VCENTER | Graphics.HCENTER);
                
                // draw approximate barcode rectangle
                int nTL = this.rb.getLeftApproxPos() * g.getClipWidth()
                    / this.imageCropped.getWidth() + g.getClipX();
                int nTR = this.rb.getRightApproxPos() * g.getClipWidth()
                    / this.imageCropped.getWidth() + g.getClipX();
                int nBL = nTL + 
                        this.rb.getLeftApproxSlope() * g.getClipHeight() / 256
                        * g.getClipWidth() / this.imageCropped.getWidth();
                int nBR = nTR + 
                        this.rb.getRightApproxSlope() * g.getClipHeight() / 256
                        * g.getClipWidth() / this.imageCropped.getWidth();
                g.setColor(0, 255, 0); // Green
                g.drawLine(nTL, g.getClipY(), nBL, g.getClipY() + g.getClipHeight());
                g.drawLine(nTR, g.getClipY(), nBR, g.getClipY() + g.getClipHeight());
            } else {
                g.setColor(0, 255, 0); // Green
                g.drawRect(
                        g.getClipX() + this.dCropLeft * g.getClipWidth() / 640,
                        g.getClipY() + this.dCropTop * g.getClipHeight() / 480,
                        this.cCropWidth * g.getClipWidth() / 640,
                        this.cCropHeight * g.getClipHeight() / 480
                        );
            }
        }
    }
    
    public void Push() {
        this.oAttempted = true;
        this.imageDisplay = null;
        jjil.core.Image inimg = RgbImageJ2me.toRgbImage(this.imageInput);
        Sequence seq = new Sequence(new RgbSelect2Gray(RgbSelect2Gray.GREEN));
        int nSize = 1;
        while (nSize < Math.min(inimg.getWidth(), inimg.getHeight())) nSize <<= 1;
        nSize >>= 1;
        seq.Add(new GrayHistEq());
        //seq.Add(new DeblurHorizHalftone());
        //seq.Add(new GrayHistEq());
        seq.Push(inimg);
        inimg = seq.Front();
        this.rb = new ReadBarcode();
        seq = new Sequence();
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
    
    public void setImage(Image imageInput) {
        this.oAttempted = false;
        this.imageInput = imageInput;       
    }
    
    public String toString() {
        return super.toString() + " (" + this.dCropLeft + "," + this.dCropTop + 
                "," + this.cCropWidth + "," + this.cCropHeight + ")";
    }
}
