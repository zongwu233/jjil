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
import barcode.DetectBarcode;
import barcode.ReadBarcode;

/**
 * Hungarian notation prefix: bcp.
 * @author webb
 */
public class BarcodeParam {
    /** Image cropped to the region where the barcode should be
     */
    RgbImage imageCropped;
    /** Image to display 
     */
    Image imageDisplay = null;
    /** Input image.
     */
    Image imageInput;
    /** Whether or not barcode detection has been attempted.
     */
    boolean oAttempted = false;
    /** Where or not barcode was found
     */
    boolean oFound = false;
    /** Whether barcode detection was successful or not.
     */
    boolean oSuccessful = false;
    
    ReadBarcode rb = null;
    /** The detected barcode (if any).
     */
    String szBarcode;
    /** Creates a new instance of BarcodeParam */
    public BarcodeParam() throws IllegalArgumentException {
        this.imageCropped = null;
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
        if (this.imageCropped != null) {
            if (this.oAttempted) {
                try {
                    if (this.imageDisplay == null && this.oFound) {
                        if (this.imageCropped.getWidth() > g.getClipWidth() ||
                            this.imageCropped.getHeight() > g.getClipHeight()) {
                            int nWidth = Math.min(this.imageCropped.getWidth(), 
                                    g.getClipWidth());
                            int nHeight = Math.min(this.imageCropped.getHeight(), 
                                    g.getClipHeight());
                            RgbShrink rs = new RgbShrink(nWidth, nHeight);
                            rs.Push(this.imageCropped);
                            this.imageDisplay = RgbImageJ2me.toImage((RgbImage)rs.Front());
                        } else {
                            this.imageDisplay = RgbImageJ2me.toImage(this.imageCropped);
                        }
                    } 
                    if (this.imageDisplay != null) {
                        g.drawImage(
                                this.imageDisplay, 
                                g.getClipWidth()/2, 
                                g.getClipHeight()/2, 
                                Graphics.VCENTER | Graphics.HCENTER);
                    }
                    if (this.oSuccessful) {
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
                        // draw the barcode string
                        // white background
                        int nHeight = g.getFont().getHeight();
                        g.setColor(255, 255, 255); // white
                        g.fillRect(
                                g.getClipX(), 
                                g.getClipY() + g.getClipHeight() - nHeight, 
                                g.getClipX() + g.getClipWidth(), 
                                g.getClipY() + g.getClipHeight());
                        // black text
                        g.setColor(0, 0, 0);
                        g.drawString(
                                " Read: " + rb.getCode(), 
                                g.getClipX(), 
                                g.getClipY() + g.getClipHeight(), 
                                Graphics.BOTTOM | Graphics.LEFT);
                    }
                } catch (jjil.core.Error e) {
                    e.printStackTrace();
                    jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);
                    g.drawString(eJ2me.getLocalizedMessage(), 0, 0, 0);
                }
            } else {
            }
        }
    }
    
    public void Push() {
        this.oAttempted = true;
        this.imageDisplay = null;
        this.imageCropped = RgbImageJ2me.toRgbImage(this.imageInput);
        DetectBarcode db = new DetectBarcode(20000);
        try {
            if (!db.Push(this.imageCropped)) {
                /**
                 * Couldn't find the barcode. Tell the user.
                 */
                this.oFound = false;
                this.oSuccessful = false;
                this.szBarcode = null;
            } else {
                this.oFound = true;
                this.rb = new ReadBarcode();
                this.rb.setRect(db.getRect());
                this.rb.Push(this.imageCropped);
                if (!rb.getSuccessful()) {
                        /**
                         * Couldn't read the barcode.
                         */
                        this.oSuccessful = false;
                        this.szBarcode = null;
                } else {
                        /**
                         * Read the barcode. Tell the user.
                         */
                        this.oSuccessful = true;
                        this.szBarcode = this.rb.getCode();
                    }
            }
        } catch (jjil.core.Error e) {
            e.printStackTrace();
            jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);
            /**
             * Report error somehow.
             */
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
    
}
