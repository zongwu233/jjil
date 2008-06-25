package barcode;
/*
 * BarcodeReader.java
 *
 * Created on September 13, 2006, 4:21 PM
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

/**
 * Reads a barcode from an image, applying multiple different barcode types
 * and returning the best one. The barcode is assumed to extend horizontally
 * across the image and should nearly fill the cropping window. The input
 * image type is RgbImage.
 * @author webb
 */
public interface BarcodeReader {
    /**
     * Attempts to decode an image, returning the
     * quality of the match. The match quality is scaled so that the
     * results of different calls to this method for this barcode reader
     * and for other barcode readers can be compared to determine which is
     * the best matching barcode.
     * <p>
     * After this method is called the getCode() method can be called to 
     * retrieve the barcode what was read.
     * <p>
     * Hungarian prefix is 'reader'.
     * @param image the input image. Should have been scaled to be a
     * multiple of getWidth() in width (see below).
     * @return the match quality.
     * @throws IllegalArgumentException if image is not a multiple of width
     * pixels, or is not a gray image.
     */
    
    int Decode(jjil.core.Image image) 
        throws jjil.core.Error;
    /**
     * This returns the barcode what was read from the image. If Decode
     * has not been called previously or no barcode was found this method
     * returns null.
     *
     * @return the barcode, for example "074400021902".
     */
    String getCode();
    
    /**
     * This returns a short name for the barcode type, to be used when
     * telling the user what type barcode matched the image.
     * 
     * @return a short string (few to several characters) describing the
     * barcode reader, like "UPC" or "EAN-13".
     */
    String getName();
    
    /**
     * Returns the number of bars (light and dark lines) in a barcode of this
     * type. The input image to Decode will be stretched to exactly match
     * this number -- i.e., it will be a multiple of this width.
     * 
     * 
     * @return the number of bars in a barcode of this type. This is the number
     * of light and dark bars, dividing a wide bar into its component
     * segments.
     */
    int getWidth();
}
