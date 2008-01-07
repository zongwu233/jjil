package barcode;
/*
 * ReadBarcode.java
 *
 * Created on September 13, 2006, 11:19 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
*
 * Copyright 2006 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the Lesser GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
import java.util.Vector;

import jjil.algorithm.CannyHoriz;
import jjil.algorithm.Gray2Rgb;
import jjil.algorithm.GrayCrop;
import jjil.algorithm.GrayHistEq;
import jjil.algorithm.GrayRectStretch;
import jjil.algorithm.GrayReduce;
import jjil.algorithm.GrayTrapWarp;
import jjil.algorithm.GrayVertAvg;
import jjil.algorithm.LinefitHough;
import jjil.algorithm.RgbSelect2Gray;
import jjil.algorithm.ZeroCrossingHoriz;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.Point;
import jjil.core.RgbImage;
import jjil.core.Sequence;
import jjil.debug.Debug;

/** Read an image barcode using a variety of different barcode formats, and
 * return the best match.
 *
 * @author webb
 */
public class ReadBarcode {
    private int dBestLeftPos;
    private int dBestRightPos;
    private Gray8Image imageCropped;
    private Gray8Image imageEdge;
    private Gray8Image imageRectified;
    private String szBestCode;
    private String szBestName;
    private int wBestMatch;
    private int wSlopeLeft;
    private int wSlopeRight;
    private int cYLeft;
    private int cYRight;
    
    /** Creates a new instance of ReadBarcode */
    public ReadBarcode() {
    }
    
    /** 
     * Crop the image to a specified cropping window and detect edges
     * using an appropriate Canny operator. The cWidth of the Canny operator
     * is set based on the cWidth of the cropped image, so that wider images
     * use lower-frequency edge detection.
     * <p>
     * This method assigns a value to the imageCropped and imageEdge fields.
     *
     * @param image the input image.
     * @param dTopLeftX top left corner of the cropping window, x coordinate
     * @param dTopLeftX top left corner of the cropping window, y coordinate
     * @param cWidth cWidth of the cropping window
     * @param cHeight height of the cropping window
     */
    private void CropAndDetectEdges(
            Image image, 
            int dTopLeftX, 
            int dTopLeftY,
            int cWidth, 
            int cHeight)
    {
        /* Build an image pipeline to do the work.
         */
        Sequence seq = new Sequence();
        /* First convert the color image to black and white by selecting the
         * green channel. We use the green channel because it is the highest
         * resolution channel on the CCDs used in cellphones.
         */
        if (image instanceof RgbImage) {
            seq.Add(new RgbSelect2Gray(RgbSelect2Gray.GREEN));
        }
        /* Now crop the gray image.
         */
        seq.Add(new GrayCrop(dTopLeftX, dTopLeftY, cWidth, cHeight));
        /* Apply the pipeline to get the cropped image.
         */
        seq.Push(image);
        Image imageResult = seq.Front();
        if (!(imageResult instanceof Gray8Image)) {
            throw new IllegalStateException(imageResult.toString() +
                    " should be an 8-bit gray image");
        }
        this.imageCropped = (Gray8Image) imageResult;
        /* Then do edge detection. The cWidth of the Canny operator is set
         * based on the cropped cWidth, which is supposed to be the
         * barcode cWidth.
         */
        int cCannyWidth = cWidth / 6;
        CannyHoriz canny = new CannyHoriz(cCannyWidth);
        /* Now apply the edge detection to the cropped mage
         */
        canny.Push(imageCropped.Clone());
        /* And obtain the result 
         */
        imageResult = canny.Front();
        if (!(imageResult instanceof Gray8Image)) {
            throw new IllegalStateException(imageResult.toString() +
                    " should be an 8-bit gray image");
        }
        this.imageEdge = (Gray8Image) imageResult;
    }
    
    private static Image CropAndStretchImage(
            Image imageInput, 
            int dLeft, 
            int dRight, 
            int cHeightReduce, 
            int cInputWidth,
            int cTargetWidth)
    {
        // so it is divisible by cHeightReduce, a power of 2
        int cCroppedHeight = cHeightReduce * 
                (imageInput.getHeight() / cHeightReduce); 
        int cTotalWidth = dRight - dLeft;
        GrayCrop crop = new GrayCrop(dLeft, 0, cTotalWidth, cCroppedHeight);
        Sequence seq = new Sequence(crop);
        seq.Add(new GrayReduce(1,cHeightReduce));
        int cReducedHeight = cCroppedHeight / cHeightReduce;
        // we stretch the cropped image so cInputWidth becomes cTargetWidth. This means
        // cTotalWidth must become cTargetWidth * cTotalWidth / cInputWidth
        GrayRectStretch stretch = new GrayRectStretch(
                cTargetWidth * cTotalWidth / cInputWidth, cReducedHeight);
        seq.Add(stretch);
        seq.Add(new GrayHistEq());
        seq.Push(imageInput);
        return seq.Front();
    }
    
    private boolean FindBarcodeEdges()
    {
        ZeroCrossingHoriz zeroesFind = new ZeroCrossingHoriz(8);
        int[][] wZeroes = zeroesFind.Push(this.imageEdge);
        Vector v = ZeroesToPoints(wZeroes, true);
        if (v.size() < 5) {
            return false;
        }
        LinefitHough fit = 
                new LinefitHough(0, this.imageEdge.getWidth(), -76, 76, 100);
        fit.Push(v);
        if (fit.getCount() < 5) {
            return false;
        }
        this.cYLeft = fit.getY();
        this.wSlopeLeft = fit.getSlope();
        v = ZeroesToPoints(wZeroes, false);
        if (v.size() < 5) {
            return false;
        }
        fit.Push(v);
        if (fit.getCount() < 5) {
            return false;
        }
        this.cYRight = fit.getY();
        this.wSlopeRight = fit.getSlope();
        {
        	Debug debug = new Debug();
        	Gray2Rgb g2r = new Gray2Rgb();
        	g2r.Push(this.imageEdge);
        	debug.toFile((RgbImage) g2r.Front(), "edges.png");
        }
        return true;
     }
    
    private void FindPreciseBarcodeLimits()
    {
        int cWidth = this.imageRectified.getWidth();
        int cShortWidth = cWidth / 24, 
            cLongWidth = cWidth / 4;
        byte[] bAvg = GrayVertAvg.Push(this.imageRectified);
        int[] wShortSum = new int[bAvg.length], 
                wLongSum = new int[bAvg.length];
        FormShortAndLongSums(bAvg, cShortWidth, cLongWidth, wShortSum, wLongSum);
        this.dBestLeftPos = -1;
        this.dBestRightPos = -1;
        int wBestLeftJump = Integer.MIN_VALUE, 
                wBestRightJump = Integer.MIN_VALUE;
        for (int i=0; i<cWidth; i++) {
            if (i < cWidth / 3 && i-cShortWidth >= 0 && i+cLongWidth < cWidth) {
                int wLeftJump = wShortSum[i-cShortWidth] * cLongWidth - 
                        wLongSum[i+cLongWidth] * cShortWidth;
                if (wLeftJump > wBestLeftJump) {
                        this.dBestLeftPos = i;
                        wBestLeftJump = wLeftJump;
                }
            }
            if (i > cWidth * 2 / 3 && i-cLongWidth >= 0 && i+cShortWidth < cWidth) {
                int wRightJump = wShortSum[i+cShortWidth] * cLongWidth - 
                        wLongSum[i-cLongWidth] * cShortWidth;
                if (wRightJump > wBestRightJump) {
                        this.dBestRightPos = i;
                        wBestRightJump = wRightJump;
                }
            }
        }
    }

    private static void FormShortAndLongSums(
            byte[] bAvg, 
            int cShortWidth,
            int cLongWidth,
            int[] wShortSumResult, 
            int[] wLongSumResult)
    {
        int wShortSum = 0, wLongSum = 0;
        for (int i=0; i<cShortWidth; i++) {
            wShortSum += bAvg[i];
        }
        for (int i=0; i<cLongWidth; i++) {
            wLongSum += bAvg[i];
        }
        for (int i=0; i<bAvg.length; i++) {
            wShortSumResult[i] = wShortSum;
            wLongSumResult[i] = wLongSum;
            if (i+cShortWidth < bAvg.length) {
                wShortSum += bAvg[i+cShortWidth];
            }
            if (i+cLongWidth < bAvg.length) {
                wLongSum += bAvg[i+cLongWidth];
            }
            if (i-cShortWidth >= 0) {
                wShortSum -= bAvg[i-cShortWidth];
            }
            if (i-cLongWidth >= 0) {
                wLongSum -= bAvg[i-cLongWidth];
            }
        }
    }
    
    /** Get the barcode read from the image.
     * 
     * @return a String whose value is the barcode read from the image. Will
     * be null if no barcode was read (yet).
     */
    public String getCode() {
        return this.szBestCode;
    }
    
    public int getLeftApproxPos() {
        return this.cYLeft;
    }
    
    public int getLeftApproxSlope() {
        return this.wSlopeLeft;
    }

    public int getRightApproxPos() {
        return this.cYRight;
    }
    
    public int getRightApproxSlope() {
        return this.wSlopeRight;
    }
    
    /** Returns true iff a barcode was read from the image.
     * 
     * @return whether or not a barcode was read.
     */
    public boolean getSuccessful() {
        return this.szBestCode != null;
    }
    
    /** Read the barcode. The image must be an 8-bit gray image. The other
     * parameters specify a cropping window. The barcode should almost fill
     * the cropping window. The barcode will be located and rectified in the
     * window. It is assumed to extend horizontally across the window. The
     * resulting barcode, if read, will be returned from getCode(). 
     * getSuccessful() can be queried to determine whether the barcode was
     * successfully read.
     * <p>
     * The image is cropped as all pixels whose position is greater than or
     * equal to the top or left, and less than the bottom and right.
     * 
     * @param image the input image.
     * @param dTopLeftX top left corner of the cropping window
     * @param dTopLeftY top row of the cropping window
     * @param cWidth cWidth of the cropping window
     * @param cHeight height of the cropping window.
     * @throws java.lang.IllegalArgumentException if the input image is not
     * an 8-bit gray image.
     */
    public void Push(
            Image image, 
            int dTopLeftX, 
            int dTopLeftY,
            int cWidth, 
            int cHeight) throws IllegalArgumentException {
        this.szBestCode = null;
        this.szBestName = null;
        this.wBestMatch = Integer.MIN_VALUE;
        if (dTopLeftX < 0 || dTopLeftX > image.getWidth() ||
            dTopLeftY < 0 || dTopLeftY > image.getHeight() ||
            cWidth < 0 || dTopLeftX + cWidth > image.getWidth() ||
            cHeight < 0 || dTopLeftY + cHeight > image.getHeight()) {
            throw new IllegalArgumentException("Cropping rectangle " +
                    "(" + dTopLeftX + "," + dTopLeftY + "," + cWidth + "," +
                    cHeight + ") doesn't lie within the image " + 
                    image.toString());
        }
        /* Convert the RGB input image to gray, crop, and detect edges. This
         * initializes imageCropped and imageEdge.
         */
        CropAndDetectEdges(image, dTopLeftX, dTopLeftY, cWidth, cHeight);
        /* Find the approximate left and right edges of the barcode. Sets
         * cYLeft, cYRight, wSlopeLeft, and wSlopeRight
         */
        if (!FindBarcodeEdges()) {
            return;
        }
        /* Rectify the barcode image so the left and right edges are vertical
         */
        RectifyBarcode();
        /* Find the exact left and right edges of the barcode in the rectified
         * image. Initializes dBestLeftPos and dBestRightPos.
         */
        FindPreciseBarcodeLimits();
        if (this.dBestLeftPos == -1 || this.dBestRightPos == -1) {
            return;
        }
        BarcodeReader reader = new BarcodeReaderEan13();
        GrayCrop gc = new GrayCrop(dBestLeftPos, 
        		0,
        		dBestRightPos - dBestLeftPos,
        		this.imageRectified.getHeight());
        gc.Push(this.imageRectified);
        Gray8Image imCropped = (Gray8Image) gc.Front();
        {
           	Debug debug = new Debug();
        	Gray2Rgb g2r = new Gray2Rgb();
        	g2r.Push(this.imageCropped);
        	debug.toFile((RgbImage) g2r.Front(), "rectCropped.png");
        	
        }
        int wGoodness = reader.Decode(this.imageCropped);
		if (reader.getCode() != null && wGoodness > this.wBestMatch) {
		    this.szBestCode = reader.getCode();
		    this.wBestMatch = wGoodness;
		    this.szBestName = reader.getName();
		}
       
        /*
        for (int cInputWidth = dBestRightPos - dBestLeftPos - 2*cJitter;
            cInputWidth <= dBestRightPos - dBestLeftPos + 2*cJitter;
            cInputWidth++) {
            TryWidth(cInputWidth, cJitter, reader);
        }
        */
    }
    
    private void RectifyBarcode()
        throws IllegalStateException
    {
        int cYLeft = Math.max(0, this.cYLeft - this.imageCropped.getWidth() / 8);

        int cClip = -this.wSlopeLeft * (this.imageCropped.getHeight() - 1) / 256;
        cYLeft = Math.max(cYLeft, cClip);
        int cYRight = Math.min(this.imageCropped.getWidth(), 
                this.cYRight + this.imageCropped.getWidth() / 8);
        cClip = this.imageCropped.getWidth() 
            - (this.imageCropped.getHeight() - 1) * this.wSlopeRight / 256;
        cYRight = Math.min(cYRight, cClip);
        int cTl = cYLeft;
        int cTr = cYRight;
        int cBl = cYLeft + 
                (this.imageCropped.getHeight() - 1) * this.wSlopeLeft / 256;
        int cBr = cYRight + 
                (this.imageCropped.getHeight() - 1) * this.wSlopeRight / 256;
        GrayTrapWarp warp = new GrayTrapWarp(0,	this.imageCropped.getHeight(), 
                cTl, 
                cTr, 
                cBl, 
                cBr);
        warp.Push(this.imageCropped);
        Image imageResult = warp.Front();
        if (!(imageResult instanceof Gray8Image)) {
            throw new IllegalStateException(imageResult.toString() +
                    " should be an 8-bit gray image");
        }
        {
        	Debug debug = new Debug();
        	Gray2Rgb g2r = new Gray2Rgb();
        	g2r.Push(imageResult);
        	debug.toFile((RgbImage) g2r.Front(), "rectified.png");
        }
        this.imageRectified = (Gray8Image) imageResult;
    }
    
    private void TryWidth(int cInputWidth, int cJitter, BarcodeReader reader)
    {
        // find smallest cTargetWidth that is a cMultiple of reader.Width()
        // and larger than cInputWidth.
        // cMultiple is the smallest number of barcode cWidth's greater than the 
        // input cWidth
        int cMultiple = ((cInputWidth + reader.getWidth() - 1) / reader.getWidth());
            // cTargetWidth is the width we'll stretch input cWidth to
        int cTargetWidth = reader.getWidth() * cMultiple;		
        Image imageStretched =
            CropAndStretchImage(
                    imageRectified, 
                    dBestLeftPos-cJitter, 
                    dBestRightPos+cJitter, 
                    4, 
                    cInputWidth, 
                    cTargetWidth);
        {
        	Gray2Rgb g2r = new Gray2Rgb();
        	g2r.Push(imageStretched);
        	new Debug().toFile((RgbImage) g2r.Front(), "stretched.png");
        }
        // now look for a barcode in the stretched rectangle at positions 0..2*cJitter-1
        // (in the input image) which must be scaled by cTargetWidth / cInputWidth in
        // the stretched image
        int wGoodness = reader.Decode(
                        imageStretched);
        if (reader.getCode() != null && wGoodness > this.wBestMatch) {
            this.szBestCode = reader.getCode();
            this.wBestMatch = wGoodness;
            this.szBestName = reader.getName();
        }

    }
            
    private Vector ZeroesToPoints(int[][] wZeroes, boolean oLeft) {
        Vector vResult = new Vector();
        for (int i=0; i<wZeroes.length; i++) {
            if (wZeroes[i] != null) {
                if (wZeroes[i].length > 10) {
                    if (oLeft) {
                        vResult.addElement(new Point(i, wZeroes[i][0] / 256));
                    } else {
                        vResult.addElement(
                            new Point(i, wZeroes[i][wZeroes[i].length-1] / 256));
                    }
                }
            }
        }
        return vResult;
    }
}
