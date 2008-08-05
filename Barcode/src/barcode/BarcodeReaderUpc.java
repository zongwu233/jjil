package barcode;
/*
 * BarcodeReaderUpc.java
 *
 * Created on September 14, 2006, 10:59 AM
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
import jjil.algorithm.GrayVertAvg;
import jjil.core.*;

/**
 * Class for reading UPC barcodes from gray 8-bit images. The class requires
 * the setting of a "blur" parameter that defines the degree of blurring
 * expected in the image, due to defocus. The class models the blurred
 * barcode and uses this to match against the image.
 * @author webb
 * @deprecated This code has been supersded by BarcodeReaderEan13, which includes UPC barcodes.
 */
public class BarcodeReaderUpc implements BarcodeReader {
    
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
       
    /* These contants define the layout of a UPC barcode.
     */
    private static final int Digits = 6; // number of digits in a barcode
    private static final int LeftWidth = 3; // number of elementary bars in the left-side pattern
    private static final int RightWidth = 3; // number of elementary bars in the right-side pattern
    private static final int MidWidth = 5; // number of elementary bars in the middle pattern
    private static final int DigitWidth = 7; // number of elementary bars in a digit
    // the offset to the middle pattern, in elementary bars
    private static final int MidOffset = LeftWidth + Digits*DigitWidth;
    // the offset to the right-side pattern, in elementary bars
    private static final int RightOffset = LeftWidth + Digits*DigitWidth*2 + MidWidth;
    // the total number of elementary bars in a UPC barcode
    private static final int TotalWidth = 
            LeftWidth + DigitWidth * Digits + MidWidth + DigitWidth * Digits + 
            RightWidth;
    
    /////////////////////////////////////////////////////////////////////////
    //
    // FIELDS
    //
    ////////////////////////////////////////////////////////////////////////
    
    /** Used to store the number of times each barcode is seen as we scan the
     * image.
     */
    private CodeHash chLeft; // for left barcode half
    private CodeHash chRight; // for right barcode half
    
    /** dAvgLeftEdge is the average observed offset of the left edge of the barcode
     * pattern over the entire image.
     */
    private int dAvgLeftEdge;
    
  
    /** These are the unblurred digit cvCodes for the digits 0-9 (left side 
     * pattern). A white bar is encoded as 1, a dark bar as -1.
     */
    private int[][] rgxwDigitCodes;
    
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
     * Creates a new instance of BarcodeReaderUpc.
     * @deprecated This code has been supersded by BarcodeReaderEan13, which includes UPC barcodes.
     */
    public BarcodeReaderUpc() {
        MakeDigitMasks();
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
        for (int i=0; i<Digits; i++) {
            int digit = Character.digit(barcode.charAt(i), 10);
            System.arraycopy(
                    this.rgxwDigitCodes[digit], 
                    0, 
                    rgwMask, 
                    LeftWidth + i*DigitWidth, 
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
        int[] rgwMask = new int[MidWidth + Digits*DigitWidth + RightWidth];
        // build left mask
        rgwMask[0] = -1;
        rgwMask[1] = 1;
        rgwMask[2] = -1;
        rgwMask[3] = 1;
        rgwMask[4] = -1;
        // add digit mask
        for (int i=0; i<Digits; i++) {
            int digit = Character.digit(barcode.charAt(i), 10);
            for (int j=0; j<DigitWidth; j++) {
                // note sign reversal below -- because black and white
                // are exchanged on the right
                rgwMask[MidWidth + i*DigitWidth + j] = 
                        - this.rgxwDigitCodes[digit][j];
            }
        }
        // add right mask
        int dRightOffset = MidWidth + Digits*DigitWidth;
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
    private static int convolve(
            byte[] image,
            int[] rgWeights, 
            int cPixelsPerBar, 
            int dRow,
            int cCols,
            int dCol) {
        if (image.length <= dRow*cCols + dCol + rgWeights.length*cPixelsPerBar) {
            throw new ArrayIndexOutOfBoundsException("Convolve"); //$NON-NLS-1$
        }
        int wConv = 0;
        for (int j=0; j<rgWeights.length; j++) {
            for (int k=0; k<cPixelsPerBar; k++) {
                wConv += image[dRow*cCols + dCol + j*cPixelsPerBar + k] * 
                        rgWeights[j];
            }
        }
        return wConv;
    }

    /**
     * 
     * Read a UPC barcode in an image. The image must be an 8-bit gray image.
     * The returned barcode is the most likely barcode seen based on a series
     * of tests that take into account the blur in the image. The returned
     * String object will always pass the check digit test. Also returned
     * is a "goodness" which measures how well the returned barcode's
     * image accounts for the image provided.
     * @return the "goodness" of the barcode
     * @param image the input image
     * @throws jjil.core.Error if image is not an 8-bit gray image
     */
    
    public int decode(Image image) 
        throws jjil.core.Error {
        if (!(image instanceof Gray8Image)) {
            throw new jjil.core.Error(
    				jjil.core.Error.PACKAGE.ALGORITHM,
    				jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
    				image.toString(),
    				null,
    				null);
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
        byte[] rgbAvg = GrayVertAvg.push(gray);
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
                            if (verifyCheckDigit(szCompleteCode)) {
                                this.szBarCode = szCompleteCode;
                                wGoodness = wThisGoodness;
                            }
                        }
                }
        }
        return wGoodness;
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
        this.chLeft = new CodeHash();
        this.chRight = new CodeHash();
        int cPixelsPerBar = image.getWidth() / TotalWidth;
        int wSumLeftEdge = 0;
        for (int dRow = 0; dRow<image.getHeight(); dRow++) {
                int dOffset = EstimateLeftEdge(image, dRow);
                if (dOffset < 0) {
                    return false;
                }
                wSumLeftEdge += dOffset;
                String code =  EstimateLeftCode(image, dRow, dOffset);
                int c = this.chLeft.get(code);
                this.chLeft.put(code, c+1);
                code = EstimateRightCode(image, 
                        dRow, 
                        dOffset + MidOffset * cPixelsPerBar);
                c = this.chRight.get(code);
                this.chRight.put(code, c+1);
        }
        this.dAvgLeftEdge = wSumLeftEdge / image.getHeight();
        return true;
    }
    
   /** Estimates the barcode for the left-side half of a UPC barcode, in the
     * given image, at the given row, starting in the given column. The barcode
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
        Gray8Image image,
        int dRow, 
        int dCol) {
        int cPixelsPerBar = image.getWidth() / TotalWidth;
        String szCode = ""; //$NON-NLS-1$
        for (int i=0; i<Digits; i++) {
            // try all digits
            int nBestDigit = -1;
            int wBestConv = Integer.MIN_VALUE;
            for (int j=0; j<10;j++) {
                int wConv = convolve(
                        image.getData(),
                        this.rgxwDigitCodes[j],
                        cPixelsPerBar,
                        dRow,
                        image.getWidth(),
                        dCol + cPixelsPerBar * (LeftWidth + DigitWidth * i));
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
     * @param image the input image.
     * @param dRow the dRow at which to perform the estimation.
     * @param dCol the column at which to start the estimation.
     */
    private String EstimateRightCode(
        Gray8Image image,
        int dRow, 
        int dCol) {
        int cPixelsPerBar = image.getWidth() / TotalWidth;
        String szCode = ""; //$NON-NLS-1$
        for (int i=0; i<Digits; i++) {
            // try all digits
            int nBestDigit = -1;
            int wBestConv = Integer.MAX_VALUE;
            for (int j=0; j<10;j++) {
                int wConv = convolve(
                        image.getData(),
                        this.rgxwDigitCodes[j],
                        cPixelsPerBar,
                        dRow,
                        image.getWidth(),
                        dCol + cPixelsPerBar * (MidWidth + DigitWidth * i));
                if (wConv < wBestConv) {
                    nBestDigit = j;
                    wBestConv = wConv;
                }
            }
            szCode += nBestDigit;
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
    private int EstimateLeftEdge(Gray8Image image, int dRow) {
        int cWidth = image.getWidth();
        int cExp = cWidth / TotalWidth; // number of pixels per bar
        // shift the barcode across the image
        int wBestConv = Integer.MIN_VALUE;
        int dBestPos = -1;
        byte[] in = image.getData();
        for (int dCol = 4 * cExp; dCol < cWidth - (TotalWidth + 4) * cExp; dCol++) {
            int wConv = 0;
            for (int j=0; j<cExp; j++) {
                int dOffset = j + dCol;
                // left pattern is 1, 0, 1, surrounded by 0's
                wConv += 
                          in[dRow*cWidth + dOffset - 4*cExp] // 0
                        + in[dRow*cWidth + dOffset - 3*cExp] // 0
                        + in[dRow*cWidth + dOffset - 2*cExp] // 0
                        + in[dRow*cWidth + dOffset - cExp] // 0
                        - in[dRow*cWidth + dOffset] // 1
                        + in[dRow*cWidth + dOffset + cExp] // 0
                        - in[dRow*cWidth + dOffset + 2*cExp] // 1
                        + in[dRow*cWidth + dOffset + 3*cExp] // 0
                        // mid pattern in 0 1 0 1 0 surrounded by 1 1
                        - in[dRow*cWidth + dOffset + (MidOffset - 1) * cExp] // 1
                        + in[dRow*cWidth + dOffset + (MidOffset) * cExp] // 0
                        - in[dRow*cWidth + dOffset + (MidOffset + 1) * cExp] // 1
                        + in[dRow*cWidth + dOffset + (MidOffset + 2) * cExp] // 0
                        - in[dRow*cWidth + dOffset + (MidOffset + 3) * cExp] // 1
                        + in[dRow*cWidth + dOffset + (MidOffset + 4) * cExp] // 0
                        - in[dRow*cWidth + dOffset + (MidOffset + 5) * cExp] // 1
                        // right pattern is 1, 0, 1, surrounded by 0's
                        + in[dRow*cWidth + dOffset + (RightOffset - 1) * cExp] // 0
                        - in[dRow*cWidth + dOffset + (RightOffset) * cExp] // 1
                        + in[dRow*cWidth + dOffset + (RightOffset + 1) * cExp] // 0
                        - in[dRow*cWidth + dOffset + (RightOffset + 2) * cExp] // 1
                        + in[dRow*cWidth + dOffset + (RightOffset + 3) * cExp] // 0
                        + in[dRow*cWidth + dOffset + (RightOffset + 4) * cExp] // 0
                        + in[dRow*cWidth + dOffset + (RightOffset + 5) * cExp] // 0
                        // note RightOffset = TotalWidth - 3 so RightOffset + 6 =
                        // TotalWidth + 3, hence loop bound above guarantees we
                        // stay in the array
                        + in[dRow*cWidth + dOffset + (RightOffset + 6) * cExp] // 0
                      ;
            }
            if (wConv > wBestConv) {
                wBestConv = wConv;
                dBestPos = dCol;
            }
        }
        return dBestPos;
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
        return "UPC"; //$NON-NLS-1$
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
                int conv = convolve(rgbSum, rgwMask, cPixelsPerBar, 0, 
                        rgbSum.length, dPos);
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
        /*  The (L) cvCodes for the ten digits are:
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
        // The R cvCodes for the digits are the 1-complements
        // of the L cvCodes. But we encode the R digits by 
        // encoded a white bar as 1 and a black bar as 0
        // so we automatically get the 1-complement without
        // while using the same table
        byte rgwDigitCodes[] = 
            {0x0d, 0x19, 0x13, 0x3d, 0x23, 0x31, 0x2f, 0x3b, 0x37, 0x0b};

        // initialize convolution table
        // white bars are encoded as 1, dark bars as -1.
        this.rgxwDigitCodes = new int[10][DigitWidth];
        for (int i=0; i<10; i++) {
            int wDigitCode = rgwDigitCodes[i];
            for (int j=DigitWidth-1;j>=0;j--) {
                if ((wDigitCode & 1) == 1) {
                    this.rgxwDigitCodes[i][j] = -1;
                } else {
                    this.rgxwDigitCodes[i][j] = 1;
                }
                wDigitCode = wDigitCode >> 1;
            }
        }
    }
    
    /**
     * Verifies the check digit in a decoded barcode string. Returns true
     * if the check digit passes the verify test.
     *
     * @param digits the barcode to be verified.
     * @return true iff the barcode passes the check digit test.
     */
    private static boolean verifyCheckDigit(String digits) {
        // compute check digit
        // add even numbered digits
        int evenSum = 0;
        int i;
        for (i=0;i<Digits*2;i+=2) {
                evenSum += Character.digit(digits.charAt(i), 10);
        }
        // add odd numbered digits
        int oddSum = 0;
        for (i=1;i<Digits*2-1;i+=2) {
                oddSum += Character.digit(digits.charAt(i), 10);
        }
        // compute even digit sum * 3 + odd digit sum;
        int total = evenSum*3+oddSum;
        // check digit is this sum subtracted from the next higher multiple of 10
        int checkDigit = (total/10 + 1) * 10 - total;
        return Character.digit(digits.charAt(Digits*2-1), 10) == checkDigit;
    }

}
