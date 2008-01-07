/*
 * Gray2Rgb.java
 *
 * Created on August 27, 2006, 9:02 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
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

package jjil.algorithm;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;
import jjil.core.RgbImage;
/** Gray2Rgb converts an 8-bit gray image to RGB by replicating the 
 * gray values into R, G, and B. The signed byte values in the gray
 * image are changed into unsigned byte values in the ARGB word.
 *
 * @author webb
 */
public class Gray2Rgb extends PipelineStage {
    
    /** Creates a new instance of Gray2Rgb */
    public Gray2Rgb() {
    }
    
    /** Converts an 8-bit gray image into an RGB image by replicating
     * R, G, and B values. Also changes the data range of the bytes
     * from -128->127 to 0->255.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input is not a Gray8Image
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() + "" +
                    " should be a Gray8Image, but isn't");
        }
        Gray8Image gray = (Gray8Image) image;
        RgbImage rgb = new RgbImage(image.getWidth(), image.getHeight());
        byte[] grayData = gray.getData();
        int[] rgbData = rgb.getData();
        for (int i=0; i<gray.getWidth() * gray.getHeight(); i++) {
            /* Convert from signed byte value to unsigned byte for storage
             * in the RGB image.
             */
            int grayUnsigned = ((int)grayData[i]) - Byte.MIN_VALUE;
            /* Create ARGB word */
            rgbData[i] = 
                    ((grayUnsigned)<<16) | 
                    ((grayUnsigned)<<8) | 
                    grayUnsigned;
        }
        super.setOutput(rgb);
    }
}
