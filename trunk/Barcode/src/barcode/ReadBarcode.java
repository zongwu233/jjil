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

import jjil.algorithm.Gray8CannyHoriz;
import jjil.algorithm.Gray8Rgb;
import jjil.algorithm.Gray8Crop;
import jjil.algorithm.Gray8HistEq;
import jjil.algorithm.Gray8RectStretch;
import jjil.algorithm.Gray8Reduce;
import jjil.algorithm.Gray8TrapWarp;
import jjil.algorithm.Gray8VertAvg;
import jjil.algorithm.LinefitHough;
import jjil.algorithm.RgbSelectGray;
import jjil.algorithm.Gray8ZeroCrossingHoriz;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.Point;
import jjil.core.Rect;
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
    private Rect rectBarcode;
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
    private void cropAndDetectEdges(
            Image image, 
            int dTopLeftX, 
            int dTopLeftY,
            int cWidth, 
            int cHeight) throws jjil.core.Error
    {
        /* Build an image pipeline to do the work.
         */
        Sequence seq = new Sequence();
        /* First convert the color image to black and white by selecting the
         * green channel. We use the green channel because it is the highest
         * resolution channel on the CCDs used in cellphones.
         */
        if (image instanceof RgbImage) {
            seq.add(new RgbSelectGray(RgbSelectGray.GREEN));
        }
        /* Now crop the gray image.
         */
        int dLeft = Math.max(0, dTopLeftX - cWidth / 12);
        int cWidthExp = Math.min(image.getWidth() - dLeft, cWidth * 7 / 6);
        seq.add(new Gray8Crop(dLeft, dTopLeftY, cWidthExp, cHeight));
        /* Apply the pipeline to get the cropped image.
         */
        seq.push(image);
        Image imageResult = seq.getFront();
        if (!(imageResult instanceof Gray8Image)) {
            throw new jjil.core.Error(
    				jjil.core.Error.PACKAGE.ALGORITHM,
    				jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
    				imageResult.toString(),
    				null,
    				null);
        }
        this.imageCropped = (Gray8Image) imageResult;
        {   Debug debug = new Debug();
        	Gray8Rgb g2r = new Gray8Rgb();
        	g2r.push(this.imageCropped);
        	RgbImage rgb = (RgbImage) g2r.getFront();
        	debug.toFile(rgb, "cropped.png"); //$NON-NLS-1$
        }
        /* Then do edge detection. The cWidth of the Canny operator is set
         * based on the cropped cWidth, which is supposed to be the
         * barcode cWidth.
         */
        int cCannyWidth = cWidth / 6;
        Gray8CannyHoriz canny = new Gray8CannyHoriz(cCannyWidth);
        /* Now apply the edge detection to the cropped mage
         */
        canny.push(imageCropped.clone());
        /* And obtain the result 
         */
        imageResult = canny.getFront();
        if (!(imageResult instanceof Gray8Image)) {
            throw new jjil.core.Error(
    				jjil.core.Error.PACKAGE.ALGORITHM,
    				jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
    				imageResult.toString(),
    				null,
    				null);
        }
        this.imageEdge = (Gray8Image) imageResult;
        {   
        	Debug debug = new Debug();
	    	Gray8Rgb g2r = new Gray8Rgb();
	    	g2r.push(this.imageEdge);
	    	RgbImage rgb = (RgbImage) g2r.getFront();
	    	debug.toFile(rgb, "edges.png"); //$NON-NLS-1$
	    }
    }
    
    private static Image cropAndStretchImage(
            Image imageInput, 
            int dLeft, 
            int dRight, 
            int cHeightReduce, 
            int cInputWidth,
            int cTargetWidth) throws jjil.core.Error
    {
        // so it is divisible by cHeightReduce, a power of 2
        int cCroppedHeight = cHeightReduce * 
                (imageInput.getHeight() / cHeightReduce); 
        int cTotalWidth = dRight - dLeft;
        Gray8Crop crop = new Gray8Crop(dLeft, 0, cTotalWidth, cCroppedHeight);
        Sequence seq = new Sequence(crop);
        seq.add(new Gray8Reduce(1,cHeightReduce));
        int cReducedHeight = cCroppedHeight / cHeightReduce;
        // we stretch the cropped image so cInputWidth becomes cTargetWidth. This means
        // cTotalWidth must become cTargetWidth * cTotalWidth / cInputWidth
        Gray8RectStretch stretch = new Gray8RectStretch(
                cTargetWidth * cTotalWidth / cInputWidth, cReducedHeight);
        seq.add(stretch);
        seq.add(new Gray8HistEq());
        seq.push(imageInput);
        return seq.getFront();
    }
    
    private boolean findBarcodeEdges() throws jjil.core.Error
    {
        Gray8ZeroCrossingHoriz zeroesFind = new Gray8ZeroCrossingHoriz(8);
        int[][] wZeroes = zeroesFind.push(this.imageEdge);
        Vector v = getPointsFromZeroes(wZeroes, true);
        if (v.size() < 5) {
            return false;
        }
        LinefitHough fit = 
                new LinefitHough(0, this.imageEdge.getWidth(), -76, 76, 100);
        fit.push(v);
        if (fit.getCount() < 5) {
            return false;
        }
        this.cYLeft = fit.getY();
        this.wSlopeLeft = fit.getSlope();
        v = getPointsFromZeroes(wZeroes, false);
        if (v.size() < 5) {
            return false;
        }
        fit.push(v);
        if (fit.getCount() < 5) {
            return false;
        }
        this.cYRight = fit.getY();
        this.wSlopeRight = fit.getSlope();
        {
        	Debug debug = new Debug();
        	Gray8Rgb g2r = new Gray8Rgb();
        	g2r.push(this.imageEdge);
        	debug.toFile((RgbImage) g2r.getFront(), "edges.png"); //$NON-NLS-1$
        }
        return true;
     }
    
    private void findPreciseBarcodeLimits()
    {
        int cWidth = this.imageRectified.getWidth();
        int cShortWidth = cWidth / 24, 
            cLongWidth = cWidth / 4;
        byte[] bAvg = Gray8VertAvg.push(this.imageRectified);
        int[] wShortSum = new int[bAvg.length], 
                wLongSum = new int[bAvg.length];
        formShortAndLongSums(bAvg, cShortWidth, cLongWidth, wShortSum, wLongSum);
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

    private static void formShortAndLongSums(
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
    
    /**
     * Get left top position of barcode.
     * @return the approximate column in the top row where the barcode lies.
     */
    public int getLeftApproxPos() {
        return this.cYLeft;
    }
    
    /**
     * Get the slope of the left edge of the barcode.
     * @return the approximate slope of the left edge of the barcode, multiplied by
     * 256.
     */
    public int getLeftApproxSlope() {
        return this.wSlopeLeft;
    }

    /**
     * Get the right top position of the barcode.
     * @return the approximate right edge of the barcode, in the top row of the image.
     */
    public int getRightApproxPos() {
        return this.cYRight;
    }
    
    /**
     * Get the slope of the right edge of the barcode.
     * @return the slope of the right side of the barcode.
     */
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
    
    /**
     * Read the barcode. The image must be an 8-bit gray image. The other
     * parameters specify a cropping window. The barcode should almost fill
     * the cropping window. The barcode will be located and rectified in the
     * window. It is assumed to extend horizontally across the window. The
     * resulting barcode, if read, will be returned from getCode(). 
     * getSuccessful() can be queried to determine whether the barcode was
     * successfully read.
     * <p>
     * The image is cropped as all pixels whose position is greater than or
     * equal to the top or left, and less than the bottom and right.
     * @param image the input image.
     * @throws jjil.core.Error if the barcode rectangle is outside the image, or the input image is not
     * an RgbImage.
     */
    public void push(Image image) throws jjil.core.Error {
        int dTopLeftX = this.rectBarcode.getLeft();
        int dTopLeftY = this.rectBarcode.getTop();
        int cWidth = this.rectBarcode.getWidth();
        int cHeight = this.rectBarcode.getHeight();
        
        this.szBestCode = null;
        this.szBestName = null;
        this.wBestMatch = Integer.MIN_VALUE;
        if (dTopLeftX < 0 || dTopLeftX > image.getWidth() ||
            dTopLeftY < 0 || dTopLeftY > image.getHeight() ||
            cWidth < 0 || dTopLeftX + cWidth > image.getWidth() ||
            cHeight < 0 || dTopLeftY + cHeight > image.getHeight()) {
            throw new jjil.core.Error(
    				jjil.core.Error.PACKAGE.ALGORITHM,
    				jjil.algorithm.ErrorCodes.PARAMETER_OUT_OF_RANGE,
    				"(" + dTopLeftX + "," + dTopLeftY + "," + cWidth + "," + cHeight + ")",
    				image.toString(),
    				null);
        }
        /* Convert the RGB input image to gray, crop, and detect edges. This
         * initializes imageCropped and imageEdge.
         */
        cropAndDetectEdges(image, dTopLeftX, dTopLeftY, cWidth, cHeight);
        /* Find the approximate left and right edges of the barcode. Sets
         * cYLeft, cYRight, wSlopeLeft, and wSlopeRight
         */
        if (!findBarcodeEdges()) {
            return;
        }

        /* Rectify the barcode image so the left and right edges are vertical
         */
        // rectifyBarcode();
        /* Find the exact left and right edges of the barcode in the rectified
         * image. Initializes dBestLeftPos and dBestRightPos.
         */
        // findPreciseBarcodeLimits();
        // if (this.dBestLeftPos == -1 || this.dBestRightPos == -1) {
        //     return;
        // }
        BarcodeReaderEan13 reader = new BarcodeReaderEan13();
        /*
        Gray8Crop gc = new Gray8Crop(dBestLeftPos, 
        		0,
        		dBestRightPos - dBestLeftPos,
        		this.imageRectified.getHeight());
        gc.push(this.imageRectified);
        Gray8Image imCropped = (Gray8Image) gc.getFront();
        {
           	Debug debug = new Debug();
        	Gray8Rgb g2r = new Gray8Rgb();
        	g2r.push(this.imageCropped);
        	debug.toFile((RgbImage) g2r.getFront(), "rectCropped.png");
        	
        }
        */
        int wGoodness = reader.decode(
        		this.imageCropped, 
        		this.cYLeft, 
        		this.cYRight, 
        		this.wSlopeLeft, 
        		this.wSlopeRight);
		if (reader.getCode() != null && wGoodness > this.wBestMatch) {
		    this.szBestCode = reader.getCode();
		    this.wBestMatch = wGoodness;
		    this.szBestName = reader.getName();
		}
       
        /*
        for (int cInputWidth = dBestRightPos - dBestLeftPos - 2*cJitter;
            cInputWidth <= dBestRightPos - dBestLeftPos + 2*cJitter;
            cInputWidth++) {
            tryWidth(cInputWidth, cJitter, reader);
        }
        */
    }
    
    private void rectifyBarcode()
        throws jjil.core.Error
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
        Gray8TrapWarp warp = new Gray8TrapWarp(0,	this.imageCropped.getHeight(), 
                cTl, 
                cTr, 
                cBl, 
                cBr);
        warp.push(this.imageCropped);
        Image imageResult = warp.getFront();
        if (!(imageResult instanceof Gray8Image)) {
            throw new jjil.core.Error(
    				jjil.core.Error.PACKAGE.ALGORITHM,
    				jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
    				imageResult.toString(),
    				null,
    				null);
        }
        {
        	Debug debug = new Debug();
        	Gray8Rgb g2r = new Gray8Rgb();
        	g2r.push(imageResult);
        	debug.toFile((RgbImage) g2r.getFront(), "rectified.png"); //$NON-NLS-1$
        }
        this.imageRectified = (Gray8Image) imageResult;
    }
    
    /**
     * Set barcode rectangle.
     * @param rect The rectangle in which to look for a barcode.
     */
    public void setRect(Rect rect) {
    	this.rectBarcode = rect;
    }
    
    private void tryWidth(int cInputWidth, int cJitter, BarcodeReader reader)
    	throws jjil.core.Error
    {
        // find smallest cTargetWidth that is a cMultiple of reader.Width()
        // and larger than cInputWidth.
        // cMultiple is the smallest number of barcode cWidth's greater than the 
        // input cWidth
        int cMultiple = ((cInputWidth + reader.getWidth() - 1) / reader.getWidth());
            // cTargetWidth is the width we'll stretch input cWidth to
        int cTargetWidth = reader.getWidth() * cMultiple;		
        Image imageStretched =
            cropAndStretchImage(
                    imageRectified, 
                    dBestLeftPos-cJitter, 
                    dBestRightPos+cJitter, 
                    4, 
                    cInputWidth, 
                    cTargetWidth);
        {
        	Gray8Rgb g2r = new Gray8Rgb();
        	g2r.push(imageStretched);
        	new Debug().toFile((RgbImage) g2r.getFront(), "stretched.png"); //$NON-NLS-1$
        }
        // now look for a barcode in the stretched rectangle at positions 0..2*cJitter-1
        // (in the input image) which must be scaled by cTargetWidth / cInputWidth in
        // the stretched image
        int wGoodness = reader.decode(
                        imageStretched);
        if (reader.getCode() != null && wGoodness > this.wBestMatch) {
            this.szBestCode = reader.getCode();
            this.wBestMatch = wGoodness;
            this.szBestName = reader.getName();
        }

    }
            
    private Vector getPointsFromZeroes(int[][] wZeroes, boolean oLeft) {
        Vector vResult = new Vector();
        for (int i=0; i<wZeroes.length; i++) {
            if (wZeroes[i] != null) {
                if (wZeroes[i].length > 5) {
                    if (oLeft) {
                    	// on the left side of the barcode we are looking for a positive
                    	// zero crossing, from dark to light 
                    	if (wZeroes[i][0] > 0) {
                            vResult.addElement(new Point(i, Math.abs(wZeroes[i][0]) / 256));                   		
                    	} else {
                    		vResult.addElement(new Point(i, Math.abs(wZeroes[i][1]) / 256));
                    	}
                    } else {
                    	// on the right side of the barcode we are looking for a positive
                    	// zero crossing, from dark to light
                    	if (wZeroes[i][wZeroes[i].length-1] < 0) {
                            vResult.addElement(
                                    new Point(i, Math.abs(wZeroes[i][wZeroes[i].length-1]) / 256));
                   		
                    	} else {
                    		vResult.addElement(
                    				new Point(i, Math.abs(wZeroes[i][wZeroes[i].length-2]) / 256));
                    		
                    	}
                    }
                }
            }
        }
        return vResult;
    }
}
