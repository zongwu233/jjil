package barcode;
/*
 * BarcodeReaderEan13.java
 *
 * Created on December 19, 2006, 9:55 AM
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
import java.util.*;

import jjil.algorithm.Gray2Rgb;
import jjil.algorithm.GrayCrop;
import jjil.algorithm.GrayVertAvg;
import jjil.core.*;
import jjil.debug.Debug;

/**
 * Class for reading EAN barcodes from gray 8-bit images. 
 * @author webb
 */
public class BarcodeReaderEan13 implements BarcodeReader {
    
    /** CodeConv is a simple class for storing and retrieving a String 
     * representing a partial barcode, and an integer representing its 
     * convolution value. The convolution value is a measure of how good the
     * match is between the barcode and the image.
     */
    private final class CodeConv
    {
        /** Create a new CodeConv object with a given first digit and 
         * convolution value. Hungarian prefix is 'cc'.
         *
         * @param digitFirst the initial first digit.
         * @param wConv the initial convolution value.
         */
        public CodeConv(int digitFirst, int wConv)
        {
                this.szBarcode = new Integer(digitFirst).toString();
                this.wConv = wConv;
        }

        /** Create a new CodeConv object with a given barcode string and 
         * convolution value.
         *
         * @param szCode the barcode string
         * @param wConv the convolution value.
         */
        public CodeConv(String szBarcode, int wConv) {
            this.wConv = wConv;
            this.szBarcode = szBarcode;
        }

        // Accessors
        
        /** Returns the barcode associated with this CodeConv object.
         *
         * @return the barcode stored in this CodeConv object (szBarcode).
         */
        public String getBarcode() 
        {
            return this.szBarcode;
        }

        
        /** Returns the convolution value associated with this CodeConv object.
         *
         * @return the convolution value stored in this CodeConv object (wConv).
         */
        public int getConv() 
        {
            return this.wConv;
        }

        
        /** Returns the last digit stored in the barcode in this CodeConv
         * object, as an integer value.
         *
         * @return the last digit of the barcode stored in this CodeConv
         * object.
         */
        public int getLastDigit() 
        {
            return Character.digit(szBarcode.charAt(szBarcode.length()-1), 10);
        }

        // Fields
        /** The convolution value of this CodeConv object.
         */
        private int wConv;
        
        /** The barcode in this CodeConv object.
         */
        private String szBarcode;
    }; // end of CodeConv class
    
    /** CodeHash wraps a HashTable in a type-safe manner to
     * hash the number of times each barcode string is found in the
     * image. The Hungarian prefix is 'ch'.
     */
    private final class CodeHash {
        private Hashtable h;
        
        /** Creates a new CodeHash object
         */
        public CodeHash() {
            this.h = new Hashtable();
        }
        
        /** Returns true iff the CodeHash object contains the given code.
         *
         * @param code the barcode to look up.
         * @return true iff the CodeHash object contains the code.
         */
        public boolean containsKey(String szCode) {
            return this.h.containsKey(szCode);
        }
        
        /** Returns the convolution value associated with the given code,
         * or 0 if there is no entry for it yet.
         *
         * @return 0 if the code is not in the CodeHash table; otherwise, its
         * convolution value.
         */
        public int get(String szCode) {
            Integer w = (Integer) this.h.get(szCode);
            if (w == null) {
                return 0;
            } else {
                return w.intValue();
            }
        }
        
        /** Returns an enumerator for enumerating all the barcodes stored in
         * the CodeHash table.
         *
         * @return the Enumeration for enumerating the barcodes.
         */
        public Enumeration keys() {
            return this.h.keys();
        }
        
        /** Adds a new barcode and convolution value to the CodeHash table, or
         * updates the current convolution value if the barcode is already
         * in the table.
         *
         * @param szCode the barcode to add or update.
         * @param wConv the convolution value.
         * @return the old convolution value, or 0 if the barcode didn't already
         * exist in the table.
         */
        public int put(String szCode, int wConv) {
            Integer w = (Integer) this.h.put(szCode, new Integer(wConv));
            if (w == null) {
                return 0;
            } else {
                return w.intValue();
            }
        }
    }; // end of CodeHash class
    
    /**
     * CodeVector wraps a Vector and presents a type-safe interface for
     * storing and retrieving CodeConv objects from it. Hungarian prefix is
     * 'cv'.
     */
    private final class CodeVector {
        private Vector v;
        
        /** Create a new CodeVector object, with a certain amount of space
         * available for storing CodeConv objects.
         *
         * @param c the minimum number of elements that can be stored in the
         * object without resizing. Passed to Vector.
         */
        public CodeVector(int c) {
            this.v = new Vector(c);
        }
        
        /** Add a new CodeConv object to the end of the vector.
         *
         * @param obj the new CodeConv object to be appended to the end of 
         * the array.
         */
        public void addCode(CodeConv cc) {
            this.v.addElement(cc);
        }
        
        /** Find the CodeConv object with the best (largest) convolution value.
         * Does a linear search through the vector. This is adequate for the
         * anticipated use, where there are only a few different CodeConv
         * objects stored in the array.
         *
         * @return the index of the best (largest convolution value) CodeConv
         * object in the vector.
         */
        private int findBestCode() {
            int wConv = Integer.MIN_VALUE;
            int dPos = -1;
            int i=0;
            for (Enumeration e = this.v.elements(); 
                 e.hasMoreElements();
                 i++) {
                CodeConv ccCode = (CodeConv) e.nextElement();
                if (ccCode.getConv() > wConv) {
                    wConv = ccCode.getConv();
                    dPos = i;
                }
            }
            return dPos;
        }
        
        /** Returns the CodeConv object at the given index.
         *
         * @param i the position at which to retrieve the CodeConv object.
         * @return the CodeConv object at the given index.
         */
        public CodeConv getCodeAt(int i) {
            if (i < 0 || i >= this.v.size()) {
                throw new ArrayIndexOutOfBoundsException("getCodeAt"); //$NON-NLS-1$
            }
            return (CodeConv) this.v.elementAt(i);
        }
        
        /** Returns the number of codes stored in this CodeVector.
         *
         * @return the number of elements in v.
         */
        public int size() {
            return this.v.size();
        }
        
        /** Removes the CodeConv object at the given index from the array.
         *
         * @param index the index at which to remove the CodeConv object.
         */
        public void removeCodeAt(int i) {
            if (i < 0 || i >= this.v.size()) {
                throw new ArrayIndexOutOfBoundsException("getCodeAt"); //$NON-NLS-1$
            }
            this.v.removeElementAt(i);
        }
    }; // end of CodeConv class

    //////////////////////////////////////////////////////////////////////////
    // 
    // CONSTANTS
    //
    /////////////////////////////////////////////////////////////////////////
       
    /* These contants define the layout of an EAN barcode.
     */
    private static final int LeftDigits = 6; // number of digits in the left half of the barcode
    private static final int RightDigits = 6; // number of digits in the right half of the barcode
    private static final int LeftWidth = 3; // number of elementary bars in the left-side pattern
    private static final int RightWidth = 3; // number of elementary bars in the right-side pattern
    private static final int MidWidth = 5; // number of elementary bars in the middle pattern
    private static final int DigitWidth = 7; // number of elementary bars in a digit
    // the offset to the middle pattern, in elementary bars
    private static final int MidOffset = LeftWidth + LeftDigits*DigitWidth;
    // the offset to the right-side pattern, in elementary bars
    private static final int RightOffset = LeftWidth + 
            (LeftDigits+RightDigits)*DigitWidth + MidWidth;
    // the total number of elementary bars in a UPC barcode
    private static final int TotalWidth = 
            LeftWidth + DigitWidth * LeftDigits + MidWidth + 
            DigitWidth * RightDigits + RightWidth;
    
    /////////////////////////////////////////////////////////////////////////
    //
    // FIELDS
    //
    ////////////////////////////////////////////////////////////////////////
    
    /** Used to store the number of times each barcode is seen as we scan the
     * image.
     */
    private CodeHash chBarCodes; 
    private CodeHash chLeft; // for left barcode half
    private CodeHash chRight; // for right barcode half
    
    /** dAvgLeftEdge is the average observed offset of the left edge of the barcode
     * pattern over the entire image.
     */
    private int dAvgLeftEdge;
    
    /** Lookup table for determining the first digit when the parity of
     * the left-side barcode has been determined.
     */
    private Hashtable hLookupFirstDigit;
    
    /** nFirstDigit is the first (leftmost) digit in the barcode currently
     * being recognized. It determines the parity used to recognize the other
     * digits in the left half of the barcode.
     */
    private int nFirstDigit;
    
    /**
     * rgnRunningSumLeft and rgnRunningSumRight are sums of the image used
     * to choose the best barcodes when there is more than one barcode which
     * could match the image and passes the check digit test.
     */
    int[] rgnRunningSumLeft;
    int[] rgnRunningSumRight;
    
    /** rgxnLeftParity is the parity that is used to recognize the digits after
     * the first in the left side code.
     * The element rgxnLeftParity[i][j] = 0 if the digit in position j has
     * even parity if the first digit is i, and 1 if the digit in position j
     * has odd parity if the first digit is i.
     */
    int[][] rgxnLeftParity = {
        {1, 1, 1, 1, 1, 1},
        {1, 1, 0, 1, 0, 0},
        {1, 1, 0, 0, 1, 0},
        {1, 1, 0, 0, 0, 1},
        {1, 0, 1, 1, 0, 0},
        {1, 0, 0, 1, 1, 0},
        {1, 0, 0, 0, 1, 1},
        {1, 0, 1, 0, 1, 0},
        {1, 0, 1, 0, 0, 1},
        {1, 0, 0, 1, 0, 1}
    };
    
    /** These are the unblurred digit cvCodes for the digits 0-9 (left side 
     * pattern). A white bar is encoded as 1, a dark bar as -1.
     * The indexing is 
     *      First level: 0 for even parity, 1 for odd parity
     *      Second level: 0 to 9 for the digits
     *      Third level: the encoded bars, from 0 to DigitWidth-1
     */
    private int[][][] rgxwDigitCodes;
    
    /** This is the barcode read from the image. It will be null if no
     * image has been decoded or no barcode was read from the image
     */
    private String szBarCode;
    
    ////////////////////////////////////////////////////////////////////////
    //
    // LIFECYCLE
    //
    ///////////////////////////////////////////////////////////////////////
    
    /**
     * Creates a new instance of BarcodeReaderEan13.
     */
    public BarcodeReaderEan13() throws IllegalArgumentException {
        MakeDigitMasks();
        MakeParityLookupTable();
    }
    
    ////////////////////////////////////////////////////////////////////////
    //
    //  METHODS
    //
    ////////////////////////////////////////////////////////////////////////
    
    /**
     * Builds a complete mask for the left side of the barcode, including all
     * digits and the pattern before and after them. The mask is blurred and
     * normalized so it can be used as a image convolution mask.
     *
     * @param barcode the left-side barcode to build the mask for.
     */
    private int[] BuildLeftSideMask(String barcode) {
        int[] rgwMask = new int[MidOffset + MidWidth];
        // build left mask
        rgwMask[0] = -1;
        rgwMask[1] = 1;
        rgwMask[2] = -1;
        // add digit mask
        int nFirstDigit = Character.digit(barcode.charAt(0), 10);
        for (int i=1; i<=LeftDigits; i++) {
            int digit = Character.digit(barcode.charAt(i), 10);
            int nParity = this.rgxnLeftParity[nFirstDigit][i-1];
            System.arraycopy(
                    this.rgxwDigitCodes[nParity][digit], 
                    0, 
                    rgwMask, 
                    LeftWidth + (i-1)*DigitWidth, 
                    DigitWidth);
        }
        // add right mask
        rgwMask[MidOffset] = -1;
        rgwMask[MidOffset+1] = 1;
        rgwMask[MidOffset+2] = -1;
        rgwMask[MidOffset+3] = 1;
        rgwMask[MidOffset+4] = -1;
        return rgwMask;
    }
    
    /**
     * Build a complete mask for the right side of the barcode, including the
     * middle and right-side patterns. The mask is blurred and normalized so
     * it can be used as an image convolution mask.
     *
     * @param barcode the right-side barcode to build the mask for.
     */
    private int[] BuildRightSideMask(String barcode) {
        int[] rgwMask = new int[MidWidth + RightDigits*DigitWidth + RightWidth];
        // build left mask
        rgwMask[0] = -1;
        rgwMask[1] = 1;
        rgwMask[2] = -1;
        rgwMask[3] = 1;
        rgwMask[4] = -1;
        // add digit mask
        for (int i=0; i<RightDigits; i++) {
            int digit = Character.digit(barcode.charAt(i), 10);
            for (int j=0; j<DigitWidth; j++) {
                // note sign reversal below -- because black and white
                // are exchanged on the right
                rgwMask[MidWidth + i*DigitWidth + j] = 
                        - this.rgxwDigitCodes[1][digit][j];
            }
        }
        // add right mask
        int dRightOffset = MidWidth + RightDigits*DigitWidth;
        rgwMask[dRightOffset] = -1;
        rgwMask[dRightOffset+1] = 1;
        rgwMask[dRightOffset+2] = -1;
        return rgwMask;
    }
    
    /** Convolves a row of the image, starting at a particular column, with
     * the passed convolution weights. Each convolution weight
     * is repeated cPixelsPerBar times.
     *
     * @param image the input image data. The width of the image must be
     * cCols.
     * @param rgWeights the array of convolution weights.
     * @param cPixelsPerBar the number of pixels each elementary dark or light
     * bar (corresponding to an element of rgWeights) is represented by.
     * @param dRow the image dRow being convolved.
     * @param cCols the width of the image.
     * @param dCol the starting column for the convolution.
     */
    private static int Convolve(
            byte[] image,
            int[] rgWeights, 
            int cPixelsPerBar, 
            int dCol) {
        if (image.length < dCol + rgWeights.length*cPixelsPerBar) {
            throw new ArrayIndexOutOfBoundsException("Convolve"); //$NON-NLS-1$
        }
        int wConv = 0;
        for (int j=0; j<rgWeights.length; j++) {
            for (int k=0; k<cPixelsPerBar; k++) {
                wConv += image[dCol + j*cPixelsPerBar + k] * 
                        rgWeights[j];
            }
        }
        return wConv;
    }

    /** 
     * Read a UPC barcode in an image. The image must be an 8-bit gray image.
     * The returned barcode is the most likely barcode seen based on a series
     * of tests that take into account the blur in the image. The returned
     * String object will always pass the check digit test. Also returned
     * is a "goodness" which measures how well the returned barcode's
     * image accounts for the image provided.
     *
     * @param image the input image
     * @param szCode the returned barcode
     * @return the "goodness" of the barcode
     * @throws IllegalArgumentException if image is not an 8-bit gray image 
     */
    
    public int Decode(Image image) 
        throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() +
                    Messages.getString("BarcodeReaderEan13.3")); //$NON-NLS-1$
        }
        Gray8Image gray = (Gray8Image) image;
        // number of pixels per bar
        int cPixelsPerBar = image.getWidth() / TotalWidth;
        // estimate the barcodes on the left and right side
        // for each row in the image and store them into hash
        // tables
        EstimateBarcodes(gray);
        // form the vertical average of the image for verifying
        // the codes
        byte[] rgbAvg = GrayVertAvg.Push(gray);
        // take the most common left barcodes and estimate their
        // match with the left side of the average
        /* 
           The point of the code below is that EstimateBarcodes 
           does not use an exact test. As we form the barcode guesses 
           in EstimateBarcodesL, we generate the next digit by testing 
           all pairs combined with the previous digit. This forms the 
           best guess at a digit by taking into account the blurring 
           that results from the digit to the left, but ignores the 
           blurring that results from the digit to the right. So we 
           need a way to weed out those guesses where the inaccuracy 
           of considering just pairs of digits from left to right 
           results in a bad result. When we form the complete digit 
           mask in GrabBestBarcodes we blur both to the left and right, 
           so that the match estimate is more exact. Also, by convolving 
           with a vertically averaged barcode image, we eliminate as much 
           noise as possible.
           */
        CodeVector cvLeftCodes = new CodeVector(10);
        if (!GrabBestBarcodes(
                this.chLeft, 
                rgbAvg, 
                image.getHeight(), 
                cPixelsPerBar, 
                cvLeftCodes, 
                true)) {
                return -1;
        }
        // take the most common right barcodes and estimate their
        // match with the right side of the image
        CodeVector cvRightCodes = new CodeVector(10);
        if (!GrabBestBarcodes(
                this.chRight, 
                rgbAvg, 
                image.getHeight(), 
                cPixelsPerBar, 
                cvRightCodes, 
                false)) {
                return -1;
        }

        int wGoodness = Integer.MIN_VALUE;

        // combine the left and right barcode estimates
        // and verify the check digit. Maximize the match
        // over all pairs of left and right barcodes.
        for (int dLeft = 0; dLeft < cvLeftCodes.size(); dLeft++) {
                for (int dRight = 0; dRight < cvRightCodes.size(); dRight++) {
                        int wThisGoodness = cvLeftCodes.getCodeAt(dLeft).getConv() + 
                                cvRightCodes.getCodeAt(dRight).getConv();
                        if (wThisGoodness > wGoodness) {
                            String szCompleteCode = 
                                    cvLeftCodes.getCodeAt(dLeft).getBarcode() +
                                    cvRightCodes.getCodeAt(dRight).getBarcode();
                            if (VerifyCheckDigit(szCompleteCode)) {
                                this.szBarCode = szCompleteCode;
                                wGoodness = wThisGoodness;
                            }
                        }
                }
        }
        return wGoodness;
    }
    
    /** 
     * Read a UPC barcode in an image. The image must be an 8-bit gray image.
     * The returned barcode is the most likely barcode seen based on a series
     * of tests that take into account the blur in the image. The returned
     * String object will always pass the check digit test. Also returned
     * is a "goodness" which measures how well the returned barcode's
     * image accounts for the image provided.
     *
     * @param image the input image
     * @param szCode the returned barcode
     * @return the "goodness" of the barcode
     * @throws IllegalArgumentException if image is not an 8-bit gray image 
     */
    
    public int Decode( 
    		Image image, 
    		int cYLeft, 
    		int cYRight, 
    		int wSlopeLeft, 
    		int wSlopeRight) 
        throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() +
                    Messages.getString("BarcodeReaderEan13.4")); //$NON-NLS-1$
        }
        Gray8Image gray = (Gray8Image) image;
        // estimate the barcodes on the left and right side
        // for each row in the image and store them into hash
        // tables
        EstimateBarcodes(gray, cYLeft, cYRight, wSlopeLeft, wSlopeRight);
        // scan the barcodes and find the most likely one. This is our
        // result
        int nMostCommon = Integer.MIN_VALUE;
        this.szBarCode = null;
        for (Enumeration e = this.chBarCodes.keys(); e.hasMoreElements();) {
        	String szBarcode = (String) e.nextElement();
        	int nOccurrence = this.chBarCodes.get(szBarcode);
        	if (nOccurrence > nMostCommon) {
        		this.szBarCode = szBarcode;
        		nMostCommon = nOccurrence;
        	}
        }
        return nMostCommon;
    }

    /**
     * Does complete barcode estimation for a given image. In each row of the
     * image we compute an estimate of the left edge of the image by convolving
     * with a mask based on the left, middle, and right-side patterns. Then
     * we use this position to estimate the left side barcode, then the
     * right side barcode. The best matching barcodes in each row are added
     * to hash tables (this.chLeft and this.chRight) for later inspection
     * that takes into account the complete effect of blur and the check 
     * digit.
     * <p>
     * Creates this.chLeft and this.chRight.
     *
     * @param image the input image.
     */
    private boolean EstimateBarcodes(Gray8Image image) {
    	{
    		Gray2Rgb g2r = new Gray2Rgb();
    		g2r.Push(image);
    		Debug debug = new Debug();
    		debug.toFile((RgbImage) g2r.Front(), "barcode.png"); //$NON-NLS-1$
    	}
        this.chLeft = new CodeHash();
        this.chRight = new CodeHash();
        int cPixelsPerBar = image.getWidth() / TotalWidth;
        int wSumLeftEdge = 0;
        int wSumRightEdge = 0;
        this.rgnRunningSumLeft = null;
        this.rgnRunningSumRight = null;
        for (int dRow = 0; dRow<image.getHeight(); dRow++) {
        	// estimate position of left edge of barcode
        	// this will be the position of the first 1 in the
        	// string "00001010"
            int dLeftOffset = EstimateLeftEdge(
            		image, 
            		dRow,
            		0,
            		image.getWidth() / 8,
            		image.getWidth());
            if (dLeftOffset < 0) {
                return false;
            }
            wSumLeftEdge += dLeftOffset;
            // estimate position of the middle of the barcode
            // this will be the position of the first 1 in the
            // string "0101010"
            int dMidOffset = EstimateMiddle(
            		image, 
            		dRow,
            		image.getWidth() * 7 / 16,
            		image.getWidth() * 9 / 16,
            		image.getWidth());
            if (dMidOffset < 0 || dMidOffset <= dLeftOffset) {
            	return false;
            }
            // estimate position of the right of the barcode.
            // this will be the position of the first 1 in the
            // string "1010000"
            int dRightOffset = 
            	EstimateRightEdge(
            			image, 
            			dRow,
            			image.getWidth() * 7 / 8,
            			image.getWidth() - 1,
            			image.getWidth());
            if (dRightOffset < 0 || dRightOffset <= dMidOffset) {
            	return false;
            }
            wSumRightEdge += dRightOffset;
            byte bData[] = new byte[image.getWidth()];
            System.arraycopy(
                    image.getData(), 
                    dRow * image.getWidth(), 
                    bData, 
                    0, 
                    image.getWidth());
            // stretch the left and right sides of the barcode so the
            // pixels are an exact multiple of the number of elementary
            // bars between the measured positions
            int nLeftSideWidth = LeftWidth + LeftDigits + DigitWidth + 2;
            byte bLeft[] = Stretch(
            		bData, 
            		dLeftOffset, 
            		dMidOffset, 
            		nLeftSideWidth);
            int nRightSideWidth = 3 + RightDigits * DigitWidth + 1;
            byte bRight[] = Stretch(
            		bData, 
            		dMidOffset, 
            		dRightOffset, 
            		nRightSideWidth);
            // if we know the implied first digit we can call a different
            // version of EstimateLeftCode that takes that digits as
            // the last parameter. This will significantly improve accuracy
            // because the search for the implied digit requires us to
            // consider two encodings for each digit in the left side
            // of the barcode, doubling the chance of error at each
            // digit. We could get the digit from the country information.
            // However, experience shows that this isn't enough, at least
            // with conventional cellphone optics and resolution. The only
            // option is to use better optics or get more pixels and
            // greater distance to the barcode, given better focus.
            String szLeftCode =  EstimateLeftCode(bLeft);
            if (szLeftCode == null) {
                continue; // parity test didn't work out
            }
            // sum the image for this row into the running sum used
            // for breaking ties between barcodes
            this.rgnRunningSumLeft = SumStretchedImage(
            		bLeft, 
            		nLeftSideWidth, 
            		this.rgnRunningSumLeft);
            int c = this.chLeft.get(szLeftCode);
            this.chLeft.put(szLeftCode, c+1);
            String szRightCode = EstimateRightCode(bRight);
            this.rgnRunningSumRight = SumStretchedImage(
            		bRight,
            		nRightSideWidth,
            		this.rgnRunningSumRight);
            c = this.chRight.get(szRightCode);
            this.chRight.put(szRightCode, c+1);
            String szCode = szLeftCode + szRightCode;
            if (VerifyCheckDigit(szCode)) {
	            c = this.chBarCodes.get(szCode);
	            this.chBarCodes.put(szCode, c + 1);
            }            
        }
        this.dAvgLeftEdge = wSumLeftEdge / image.getHeight();
        return true;
    }
    
    /**
     * Does complete barcode estimation for a given image. In each row of the
     * image we compute an estimate of the left edge of the image by convolving
     * with a mask based on the left, middle, and right-side patterns. Then
     * we use this position to estimate the left side barcode, then the
     * right side barcode. The best matching barcodes in each row are added
     * to hash tables (this.chLeft and this.chRight) for later inspection
     * that takes into account the complete effect of blur and the check 
     * digit.
     * <p>
     * Creates this.chLeft and this.chRight.
     *
     * @param image the input image.
     */
    private boolean EstimateBarcodes(
    		Gray8Image image,
    		int cYLeft,
    		int cYRight,
    		int wSlopeLeft,
    		int wSlopeRight) {
    	{
    		Gray2Rgb g2r = new Gray2Rgb();
    		g2r.Push(image);
    		Debug debug = new Debug();
    		debug.toFile((RgbImage) g2r.Front(), "barcode.png"); //$NON-NLS-1$
    	}
        this.chLeft = new CodeHash();
        this.chRight = new CodeHash();
        this.chBarCodes = new CodeHash();
        int wSumLeftEdge = 0;
        int wSumRightEdge = 0;
        for (int dRow = 0; dRow<image.getHeight(); dRow++) {
        	int nLeft = cYLeft + (wSlopeLeft * dRow) / 256;
        	int nRight = cYRight + (wSlopeRight * dRow) / 256;
        	int nWidth = nRight - nLeft;
        	// estimate position of left edge of barcode
        	// this will be the position of the first 1 in the
        	// string "00001010"
            int dLeftOffset = 
            	EstimateLeftEdge(
            			image, 
            			dRow, 
            			nLeft, 
            			nLeft + nWidth / 8,
            			nWidth);
            if (dLeftOffset < 0) {
                return false;
            }
            wSumLeftEdge += dLeftOffset;
            // estimate position of the middle of the barcode
            // this will be the position of the second 0 in the
            // string "0101010"
            int dMidOffset = 
            	EstimateMiddle(
            			image, 
            			dRow,
            			nLeft + (nWidth * 7) / 16,
            			nLeft + (nWidth * 9) / 16,
            			nWidth);
            if (dMidOffset < 0 || dMidOffset <= dLeftOffset) {
            	return false;
            }
            // estimate position of the right of the barcode.
            // this will be the position of the second 0 in the
            // string "1010000"
            int dRightOffset = 
            	EstimateRightEdge(
            			image, 
            			dRow,
            			nRight - nWidth / 8,
            			nRight,
            			nWidth);
            if (dRightOffset < 0 || dRightOffset <= dMidOffset) {
            	return false;
            }
            wSumRightEdge += dRightOffset;
            byte bData[] = new byte[image.getWidth()];
            System.arraycopy(
                    image.getData(), 
                    dRow * image.getWidth(), 
                    bData, 
                    0, 
                    image.getWidth());
            // stretch the left and right sides of the barcode so the
            // pixels are an exact multiple of the number of elementary
            // bars between the measured positions
            // The "stretched width" reflects the number of elementary bars
            // that should lie between the measured positions of the start,
            // middle, and end patterns if we've detected them correctly.
            // On the left side we measured from the left edge of the start
            // pattern (LeftWidth) to the start of the middle pattern
            byte bLeft[] = Stretch(
            		bData, 
            		dLeftOffset, 
            		dMidOffset, 
            		MidOffset + 1);
            // On the right side we measured from the start of the middle
            // pattern (MidWidth) to the start of the right pattern
            byte bRight[] = Stretch(
            		bData, 
            		dMidOffset, 
            		dRightOffset, 
            		MidWidth - 1 + RightDigits * DigitWidth + 1);
            // if we know the implied first digit we can call a different
            // version of EstimateLeftCode that takes that digits as
            // the last parameter. This will significantly improve accuracy
            // because the search for the implied digit requires us to
            // consider two encodings for each digit in the left side
            // of the barcode, doubling the chance of error at each
            // digit. We could get the digit from the country information.
            // However, experience shows that this isn't enough, at least
            // with conventional cellphone optics and resolution. The only
            // option is to use better optics or get more pixels and
            // greater distance to the barcode, given better focus.
            String szLeftCode =  EstimateLeftCode(bLeft);
            if (szLeftCode == null) {
                continue; // parity test didn't work out
            }
            int c = this.chLeft.get(szLeftCode);
            this.chLeft.put(szLeftCode, c+1);
            String szRightCode = EstimateRightCode(bRight);
            c = this.chRight.get(szRightCode);
            this.chRight.put(szRightCode, c+1);
            String szCode = szLeftCode + szRightCode;
            if (VerifyCheckDigit(szCode)) {
	            c = this.chBarCodes.get(szCode);
	            this.chBarCodes.put(szCode, c + 1);
            }            
        }
        this.dAvgLeftEdge = wSumLeftEdge / image.getHeight();
        return true;
    }

    /** Estimates the barcode for the left-side half of a EAN-13 barcode, in the
     * given row of image data, starting in the given column. The barcode
     * estimation is done as a slightly broadened greedy search. The degree
     * of match of all possible digit pairs is first estimated. For each first
     * digit, the best convolution with all possible second digits is chosen.
     * Then the
     * top few digit pairs are chosen and the degree of match of the first
     * digit of the pair with all possible following digits is estimated.
     * This process is repeated as we work across the barcode, until all
     * digits have been estimated. The result is then the code with the
     * best convolution.
     *
     * @param image the input image row. It has been stretched so the first
     * pixel corresponds to the leftmost pixel of the left start code and
     * the right pixel corresponds to the rightmost pixel of the last left-side
     * digit.
     * @param dRow the dRow at which to perform the estimation.
     * @param dCol the column at which to start the estimation.
     */
    private String EstimateLeftCode(
        byte [] bLeft) {
        int dWidth = bLeft.length;
        // the number of elementary bars in bLeft = 
        // the width of the left pattern + the width of the left
        // digits for the two bars detected in the middle pattern
        int cPixelsPerBar = dWidth / (MidOffset + 1);
        String szCode = ""; //$NON-NLS-1$
        // for recording the parity we find in the left side barcode
        int nParity = 0;
        // Estimate the first digit
        // try all digits
        // dCol is the position of the current digit
        int dCol = cPixelsPerBar * LeftWidth;
        for (int i=0; i<LeftDigits; i++) {
            // try all digits
            int nBestDigit = -1;
            int nBestParity = 0;
            int wBestConv = Integer.MIN_VALUE;
            for (int j=0; j<10;j++) {
                int wConvEven = Integer.MIN_VALUE;
                if (i>0) {
                    // first digit is always odd parity
                    wConvEven = Convolve(
                        bLeft,
                        this.rgxwDigitCodes[0][j],
                        cPixelsPerBar,
                        dCol);
                    
                }
                int wConvOdd = Convolve(
                        bLeft,
                        this.rgxwDigitCodes[1][j],
                        cPixelsPerBar,
                        dCol);
                int wConv;
                int nThisParity;
                if (wConvOdd > wConvEven) {
                    nThisParity = 1;
                    wConv = wConvOdd;
                } else {
                    nThisParity = 0;
                    wConv = wConvEven;
                }
                if (wConv > wBestConv) {
                    nBestDigit = j;
                    wBestConv = wConv;
                    nBestParity = nThisParity;
                }
            }
            szCode += nBestDigit;
            nParity = (nParity << 1) | nBestParity;
            dCol += cPixelsPerBar * DigitWidth;
        }
        int nFirstDigit = FirstDigitFromParity(nParity);
        if (nFirstDigit != -1) {
            szCode = Integer.toString(nFirstDigit) + szCode;
            return szCode;
        } else {
            return null;
        }
    }
  
    /** Estimates the barcode for the left-side half of a EAN-13 barcode, in the
     * given row of image data, starting in the given column, given the implied digit. The barcode
     * estimation is done as a slightly broadened greedy search. The degree
     * of match of all possible digit pairs is first estimated. For each first
     * digit, the best convolution with all possible second digits is chosen.
     * Then the
     * top few digit pairs are chosen and the degree of match of the first
     * digit of the pair with all possible following digits is estimated.
     * This process is repeated as we work across the barcode, until all
     * digits have been estimated. The result is then the code with the
     * best convolution.
     *
     * @param image the input image.
     * @param dRow the dRow at which to perform the estimation.
     * @param dCol the column at which to start the estimation.
     */
    private String EstimateLeftCode(
        byte[] bData, 
        int dLeftCol, 
        int dRightCol,
        int nFirstDigit) {
        int dWidth = dRightCol - dLeftCol;
        String szCode = Integer.toString(nFirstDigit);
        int cPixelsPerBar = dWidth / TotalWidth;
        // for each position in the code
        for (int i=0; i<LeftDigits; i++) {
        	int dCol = dLeftCol + (LeftWidth + i * DigitWidth)
        		* dWidth / TotalWidth;
            // try all digits
            int nBestDigit = -1;
            int wBestConv = Integer.MIN_VALUE;
            for (int j=0; j<10;j++) {
               int wConv = Convolve(
                    bData,
                    this.rgxwDigitCodes[this.rgxnLeftParity[j][i]][j],
                    cPixelsPerBar,
                    dCol);
               if (wConv > wBestConv) {
                    nBestDigit = j;
                    wBestConv = wConv;
               }
            }
            szCode += nBestDigit;
        }
        return szCode;
    }
  
    /** Estimates the barcode for the right-side half of a UPC barcode, in the
     * given image, at the given dRow, starting in the given column. The starting
     * column is the position of the middle pattern in the UPC barcode.
     * The barcode
     * estimation is done as a slightly broadened greedy search. The degree
     * of match of all possible digit pairs is first estimated. For each first
     * digit, the best convolution with all possible second digits is chosen.
     * Then the
     * top few digit pairs are chosen and the degree of match of the first
     * digit of the pair with all possible following digits is estimated.
     * This process is repeated as we work across the barcode, until all
     * digits have been estimated. The result is then the code with the
     * best convolution. 
     *
     * @param image the input image row. It has been stretched so that the
     * first pixel corresponds to the leftmmost pixel of the middle divider pattern
     * and the right edge corresponds to the rightmost pixel of the right digits.
     * @param dRow the dRow at which to perform the estimation.
     * @param dCol the column at which to start the estimation.
     */
    private String EstimateRightCode(
        byte[] bRight) {
    	int dWidth = bRight.length;
        int cPixelsPerBar = dWidth / (MidWidth + RightDigits * DigitWidth);
        String szCode = ""; //$NON-NLS-1$
        // dCol is the position of the current digit
        int dCol = cPixelsPerBar * (MidWidth - 1);
        for (int i=0; i<RightDigits; i++) {
            // try all digits
            int nBestDigit = -1;
            int wBestConv = Integer.MAX_VALUE;
            for (int j=0; j<10;j++) {
                int wConv = Convolve(
                        bRight,
                        this.rgxwDigitCodes[1][j],
                        cPixelsPerBar,
                        dCol);
                if (wConv < wBestConv) {
                    nBestDigit = j;
                    wBestConv = wConv;
                }
            }
            szCode += nBestDigit;
            dCol += cPixelsPerBar * DigitWidth;
        }
        return szCode;
    }

     /** Find the position of the left edge of the barcode at row
     *  dRow. We convolve the image at the given row with a mask formed
     *  from the left, middle, and right-side patterns, ignoring the digits
     *  actually inside the barcode. The peak convolution position gives us
     *  the left-side position of the barcode. No blurring is done here because
     *  the left, middle, and right-side patterns are balanced between white
     *  and black, so we don't expect blurring to improve the estimation 
     *  (unless we knew the digits in the barcode, which we don't).
     *
     * @param image the input image.
     * @param dRow the row where we are estimating the barcode position.
     */
    private int EstimateLeftEdge(
    		Gray8Image image, 
    		int dRow,
    		int nLeft,
    		int nRight,
    		int cWidth) {
    	int nImageWidth = image.getWidth();
        // number of pixels per bar, rounded
        int cExp = (cWidth + TotalWidth/2) / TotalWidth;
        // shift the barcode across the image
        int wBestConv = Integer.MIN_VALUE;
        int dBestPos = -1;
        byte[] in = image.getData();
        nLeft = Math.max(0, nLeft - 2 * cExp);
        for (int dCol = nLeft + cExp; dCol < nRight; dCol++) {
            int wConv = 0;
            for (int j=0; j<cExp; j++) {
                int dOffset = j + dCol;
                // left pattern is 1, 0, 1, surrounded by 0's
                wConv += 
                    	+ (int) in[dRow*nImageWidth + dOffset - (1 * cWidth) / TotalWidth] // dark
                    	- (int) in[dRow*nImageWidth + dOffset + (0 * cWidth) / TotalWidth] // dark
                        + (int) in[dRow*nImageWidth + dOffset + (1 * cWidth) / TotalWidth] // light
                        - (int) in[dRow*nImageWidth + dOffset + (2 * cWidth) / TotalWidth] // dark
                        + (int) in[dRow*nImageWidth + dOffset + (3 * cWidth) / TotalWidth] // light
                      ;
            }
            if (wConv > wBestConv) {
                wBestConv = wConv;
                dBestPos = dCol;
            }
        }
        return dBestPos;
    }
    
    /**
     * We search for the middle pattern (10101, surrounded by 0's) using correlation. 
     * The input is an image and a row in the image. 
     * The search is done between defined positions in the
     * row. We return the position of the second 0 in the pattern 0101010.
     * @param image: the input image.
     * @param dRow: the row in the image.
     * @param nLeft: the starting position to search.
     * @param nRight: the end position to search.
     * @param cWidth
     * @return
     */
    private int EstimateMiddle(
    		Gray8Image image, 
    		int dRow,
    		int nLeft,
    		int nRight,
    		int cWidth) {
    	int nImageWidth = image.getWidth();
    	// number of pixels per bar, rounded
        int cExp = (cWidth + TotalWidth/2) / TotalWidth; 
        // shift the barcode across the image
        int wBestConv = Integer.MIN_VALUE;
        int dBestPos = -1;
        byte[] in = image.getData();
        for (int dCol = nLeft; dCol < nRight; dCol++) {
            int wConv = 0;
            for (int j=0; j<cExp; j++) {
                int dOffset = j + dCol;
                // mid pattern is 1, 0, 1, 0, 1, surrounded by 0's
                wConv += 
                        - (int) in[dRow*nImageWidth + dOffset - (2 * cWidth) / TotalWidth] // dark  / 0
                        + (int) in[dRow*nImageWidth + dOffset - (1 * cWidth) / TotalWidth] // light / 1
                        - (int) in[dRow*nImageWidth + dOffset - (0 * cWidth) / TotalWidth] // dark  / 0
                        + (int) in[dRow*nImageWidth + dOffset + (1 * cWidth) / TotalWidth] // light / 1
                        - (int) in[dRow*nImageWidth + dOffset + (2 * cWidth) / TotalWidth] // dark  / 0
                        + (int) in[dRow*nImageWidth + dOffset + (3 * cWidth) / TotalWidth] // light / 1
                        - (int) in[dRow*nImageWidth + dOffset + (4 * cWidth) / TotalWidth] // dark  / 0
                      ;
            }
            if (wConv > wBestConv) {
                wBestConv = wConv;
                dBestPos = dCol;
            }
        }
        return dBestPos;
    }
    
    /**
     * Returns the position of the second '0' in the string '01010'.
     * @param image
     * @param dRow
     * @param nLeft
     * @param nRight
     * @param cWidth
     * @return
     */
    private int EstimateRightEdge(
    		Gray8Image image, 
    		int dRow,
    		int nLeft,
    		int nRight,
    		int cWidth) {
    	int nImageWidth = image.getWidth();
        // number of pixels per bar, rounded
        int cExp = (cWidth + TotalWidth/2) / TotalWidth; 
        // shift the barcode across the image
        int wBestConv = Integer.MIN_VALUE;
        int dBestPos = -1;
        byte[] in = image.getData();
        nRight = Math.min(image.getWidth(), nRight + 2*cExp);
        for (int dCol = nRight - cExp * 3; dCol > nLeft; dCol--) {
            int wConv = 0;
            for (int j=0; j<cExp; j++) {
                int dOffset = j + dCol;
                wConv = wConv
                        // right pattern is 1, 0, 1, followed by 0's
                        + (int) in[dRow*nImageWidth + dOffset + (-2 * cWidth) / TotalWidth] // light 0
                        - (int) in[dRow*nImageWidth + dOffset + (-1 * cWidth) / TotalWidth] // dark  1
                        + (int) in[dRow*nImageWidth + dOffset + (0 * cWidth) / TotalWidth] // light  0
                        - (int) in[dRow*nImageWidth + dOffset + (1 * cWidth) / TotalWidth] // dark   1
                        + (int) in[dRow*nImageWidth + dOffset + (2 * cWidth) / TotalWidth] // light  0
               ;
            }
            if (wConv > wBestConv) {
                wBestConv = wConv;
                dBestPos = dCol;
            }
        }
        return dBestPos;
    }

    /** Returns the first digit of the barcode, given the parity
     * that was detected, or -1 if no parity matches the detected parity.
     */
    private int FirstDigitFromParity(int nParity) {
        Integer iDigit = (Integer) this.hLookupFirstDigit.get(new Integer(nParity));
        if (iDigit == null) {
            return -1;
        } else {
            return iDigit.intValue();
        }
    }

    /** Returns the code read from the image
     *
     * @return a String containing the barcode read from the image
     */
    public String getCode() {
        return this.szBarCode;
    }
    
    /** Returns the name of this barcode reader.
     *
     * @return "UPC" -- the name of this barcode reader.
     */
    public String getName() {
        return "EAN13"; //$NON-NLS-1$
    }
    
    
    /** Returns the number of elementary light or dark bars in a barcode
     * of this type.
     *
     * @return the number of elementary bars in a UPC barcode.
     */
    public int getWidth() {
        return TotalWidth;
    }
    
    /**
     * Given a code hash table, we create a mask from each barcode, blur and
     * normalize it, then convolve it with the vertical average of the image,
     * offsetting the barcode by the average left edge position. Then we
     * save the convolution result into cvCodes.
     *
     * @param chHash the chHash table we are inspecting.
     * @param rgbSum the vertical image rgbSum.
     * @param cHeight the image height.
     * @param cPixelsPerBar number of pixels per elementary bar.
     * @param cvCodes the resulting barcode/convolution result vector.
     * @param oLeft if true, we are convolving on the left side of the barcode;
     * if false, we are on the right side.
     */
    private boolean GrabBestBarcodes(
            CodeHash chHash, 
            byte[] rgbSum,
            int cHeight,
            int cPixelsPerBar, 
            CodeVector cvCodes,
            boolean oLeft) {
        for (Enumeration e = chHash.keys(); e.hasMoreElements();) {
            String barcode = (String) e.nextElement();
            int count = chHash.get(barcode);
            if (count > cHeight / 10) {
                int[] rgwMask;
                int dPos;
                if (oLeft) {
                    rgwMask = BuildLeftSideMask(barcode);
                    dPos = this.dAvgLeftEdge;
                } else {
                    rgwMask = BuildRightSideMask(barcode);
                    dPos = this.dAvgLeftEdge + MidOffset * cPixelsPerBar;
                }
                int conv = Convolve(rgbSum, rgwMask, cPixelsPerBar, dPos);
                cvCodes.addCode(new CodeConv(barcode, conv));
            } 
        }
        return cvCodes.size() > 0;
    }

    /** Initializes a table of digit masks by expanding the bit representation
     * of the left-side barcodes into int arrays where -1 represents a black
     * stripe and +1 represents a white stripe.
     * <p>
     * Creates this.rgxwDigitCodes.
     */
    private void MakeDigitMasks() {
        /*  The odd parity left (character set A) barcodes for the ten digits are:
            0 = 3-2-1-1 = 0001101 = 0x0d
            1 = 2-2-2-1 = 0011001 = 0x19
            2 = 2-1-2-2 = 0010011 = 0x13
            3 = 1-4-1-1 = 0111101 = 0x3d
            4 = 1-1-3-2 = 0100011 = 0x23
            5 = 1-2-3-1 = 0110001 = 0x31
            6 = 1-1-1-4 = 0101111 = 0x2f
            7 = 1-3-1-2 = 0111011 = 0x3b
            8 = 1-2-1-3 = 0110111 = 0x37
            9 = 3-1-1-2 = 0001011 = 0x0b
        */
        // The R barcodes for the digits are the 1-complements
        // of the L odd barcodes. But we encode the R digits by 
        // encoding a white bar as 1 and a black bar as 0
        // so we automatically get the 1-complement without
        // while using the same table
        byte rgwDigitCodesLeftOdd[] = 
            {0x0d, 0x19, 0x13, 0x3d, 0x23, 0x31, 0x2f, 0x3b, 0x37, 0x0b};
         /*  The even parity left (character set B) barcodes for the ten digits are:
            0 = 1-1-2-3 = 0100111 = 0x27
            1 = 1-2-2-2 = 0110011 = 0x33
            2 = 2-2-1-2 = 0011011 = 0x1b
            3 = 1-1-4-1 = 0100001 = 0x21
            4 = 2-3-1-1 = 0011101 = 0x1d
            5 = 1-3-2-1 = 0111001 = 0x39
            6 = 4-1-1-1 = 0000101 = 0x05
            7 = 2-1-3-1 = 0010001 = 0x11
            8 = 3-1-2-1 = 0001001 = 0x09
            9 = 2-1-1-3 = 0010111 = 0x17
        */
        byte rgwDigitCodesLeftEven[] = 
            {0x27, 0x33, 0x1b, 0x21, 0x1d, 0x39, 0x05, 0x11, 0x09, 0x17};

        // initialize convolution table
        // white bars are encoded as 1, dark bars as -1.
        this.rgxwDigitCodes = new int[2][10][DigitWidth + 1];
        for (int i=0; i<10; i++) {
            int wDigitCode = rgwDigitCodesLeftEven[i];
            for (int j=DigitWidth-1;j>=0;j--) {
                if ((wDigitCode & 1) == 1) {
                    this.rgxwDigitCodes[0][i][j] = -1;
                } else {
                    this.rgxwDigitCodes[0][i][j] = 1;
                }
                wDigitCode = wDigitCode >> 1;
            }
            // symbol ends with same color bar it started with
            this.rgxwDigitCodes[0][i][DigitWidth] =
            	this.rgxwDigitCodes[0][i][0];
        }
        for (int i=0; i<10; i++) {
            int wDigitCode = rgwDigitCodesLeftOdd[i];
            for (int j=DigitWidth-1;j>=0;j--) {
                if ((wDigitCode & 1) == 1) {
                    this.rgxwDigitCodes[1][i][j] = -1;
                } else {
                    this.rgxwDigitCodes[1][i][j] = 1;
                }
                wDigitCode = wDigitCode >> 1;
            }
            // symbol ends with same color bar it started with
            this.rgxwDigitCodes[1][i][DigitWidth] =
            	this.rgxwDigitCodes[1][i][0];
        }
    }
    
    /** 
     * Constructs a hash table for looking up the first digit given
     * the parity of the digits in the left side of the barcode.
     * Parity is encoded as a bit pattern to make the lookup easier.
     */
    private void MakeParityLookupTable() {
        this.hLookupFirstDigit = new Hashtable(10);
        for (int i=0; i<10; i++) {
            int nParity = 0;
            for (int j=0; j<LeftDigits; j++) {
                nParity = (nParity << 1) | this.rgxnLeftParity[i][j];
            }
            this.hLookupFirstDigit.put(
                    new Integer(nParity), 
                    new Integer(i));
         }
    }
    
    /**
     * Stretches the byte array between positions dLeft and dRight so
     * it is an exact multiple of dTargetWidth in length, using linear
     * interpolation. If dRight-dLeft is an exact multiple of dTargetWidth
     * then the array is simply copied. Otherwise the smallest multiple
     * of dTargetWidth > dRight-dLeft is computed and the array is stretched
     * to that size.
     * @param bInput: the input byte array
     * @param dLeft: left position of data to be stretched
     * @param dRight: right position of data to be stretched
     * @param dTargetWidth: the target width. Result will be a multiple of this
     * width.
     * @return an array of width a multiple of dTargetWidth which is a
     * linear interpolation of bInput between dLeft and dRight.
     */
    private byte[] Stretch(byte[] bInput, int dLeft, int dRight, int dTargetWidth) {
    	int dWidth = dRight - dLeft;
    	byte[] bResult;
    	if (dWidth % dTargetWidth == 0) {
    		bResult = new byte[dTargetWidth];
    		System.arraycopy(bInput, dLeft, bResult, 0, dTargetWidth);
    		return bResult;
    	}
    	// compute smallest multiple of dTargetWidth > dWidth
    	// integer division in Java truncates
    	dTargetWidth = ((dWidth / dTargetWidth) + 1) * dTargetWidth;
    	bResult = new byte[dTargetWidth];
    	for (int i=0; i<dTargetWidth; i++) {
    		// j is truncated position in bInput
    		int j = (i * dWidth) / dTargetWidth;
    		// nFrace is fractional position, multiplied by dTargetWidth
    		int nFrac = i * dWidth - j * dTargetWidth;
    		// bytes to left and right of fractional position
    		byte bLeft = bInput[j + dLeft];
    		byte bRight = bInput[j + dLeft + 1];
    		// interpolated value
    		bResult[i] = (byte) ((bLeft * (dTargetWidth - nFrac) +
    			bRight * nFrac) / dTargetWidth);
    	}
    	return bResult;
    }
    
    
    /**
     * Calculates a running sum of rows of an image. The rows are all
     * a multiple of some fixed number in length. The sum is formed by
     * first shrinking the input row to length equal to the fixed length
     * then adding all the different rows together. <br>
     * The idea is to use this function over and over to form an estimate
     * of the barcode image and then to use the correlation of the barcode
     * with this image as a tiebreaker when choosing the best barcode.
     * @param bInput -- input data to be summed
     * @param nLength -- the target length. bInput.length % nLength == 0.
     * @param nSum -- the prior running sum, also the new sum. Initialized
     * if null.
     * @return the new sum. 
     */
    private int[] SumStretchedImage(byte[] bInput, int nLength, int[] nSum) 
    	throws IllegalArgumentException
    {
    	if (bInput.length % nLength != 0) {
    		throw new IllegalArgumentException(
    				Messages.getString("BarcodeReaderEan13.5") + bInput.length +
    				Messages.getString("BarcodeReaderEan13.6") + nLength);
    	}
    	if (nSum == null) {
    		nSum = new int[nLength];
    	}
    	int nFactor = bInput.length / nLength;
    	for (int i=0; i<nLength; i++) {
    		for (int j=0; j<nFactor; j++) {
    			nSum[i] += bInput[i*nFactor + j];    			
    		}
    	}
    	return nSum;
    }
    
    private byte[] ThresholdBars(Gray8Image image, int dRow, int dCol, int cPixelsPerBar) {
        byte bData[] = new byte[image.getWidth()];
        System.arraycopy(image.getData(), dRow * image.getWidth(), bData, 0, image.getWidth());
        for (int j=dCol; j<image.getWidth()-cPixelsPerBar; j+=cPixelsPerBar) {
            int nSum = 0;
            for (int k=0; k<cPixelsPerBar; k++) {
                nSum += bData[j+k] - Byte.MIN_VALUE;
            }
            nSum /= cPixelsPerBar;
            int nNewVal;
            if (nSum >= 128) {
                nNewVal = 255;
            } else {
                nNewVal = 0;
            }
            int nDiff = nSum - nNewVal;
            for (int k=0; k<cPixelsPerBar; k++) {
                bData[j+k] = (byte) (nNewVal + Byte.MIN_VALUE);
                if (j+k+cPixelsPerBar < image.getWidth()) {
                    bData[j+k+cPixelsPerBar] = (byte) Math.max(Byte.MIN_VALUE,
                            Math.min(Byte.MAX_VALUE, bData[j+k+cPixelsPerBar] + nDiff));
                }
            }
        }
        return bData;
    }
    
    /**
     * Verifies the check digit in a decoded barcode string. Returns true
     * if the check digit passes the verify test.
     *
     * @param digits the barcode to be verified.
     * @return true iff the barcode passes the check digit test.
     */
    private static boolean VerifyCheckDigit(String digits) {
        // compute check digit
        // add odd digits
    	int nOddSum = 0;
    	for (int i=1; i<digits.length()-1; i+=2) {
            nOddSum += Character.digit(digits.charAt(i), 10);
        }
        // add even digits
        int nEvenSum = 0;
        for (int i=0;i<digits.length()-1; i+=2) {
            nEvenSum += Character.digit(digits.charAt(i), 10);
        }
        // compute even digit sum * 3 + odd digit sum;
        int nTotal = nOddSum*3 + nEvenSum;
        // check digit is this sum subtracted from the next higher multiple of 10
        int checkDigit = (nTotal/10 + 1) * 10 - nTotal;
        return Character.digit(
                digits.charAt(digits.length()-1), 10) == checkDigit;
    }
}
