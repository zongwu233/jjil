/*
 * Gray8Rect.java
 *
 * Created on September 9, 2006, 2:52 PM
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
import jjil.core.*;

/**
 * Pipeline stage assigns a constant value to a rectangle in an image.
 * @author webb
 */
public class Gray8Rect extends PipelineStage {
    private int cX, cY, nWidth, nHeight;
    private byte bValue;
    
    /**
     * Creates a new instance of Gray8Rect.
     * 
     * @param cX The horizontal offset of the rectangle.
     * @param cY the vertical offset of the rectangle.
     * @param nWidth the width of the rectangle.
     * @param nHeight the height of the rectangle.
     * @param bValue the value to be assigned to the rectangle.
     */
    public Gray8Rect(int cX, int cY, int nWidth, int nHeight, byte bValue) {
        this.cX = cX;
        this.cY = cY;
        this.nWidth = nWidth;
        this.nHeight = nHeight;
        this.bValue = bValue;
    }
        
    /**
     * Assigns a constant rectangle to the input Gray8Image, replacing values in the image.
     * @param image the input image (output replaces input).
     * @throws java.lang.IllegalArgumentException if image is not a Gray8Image.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() + " should be " +
                    "a Gray8Image");
        }
        Gray8Image input = (Gray8Image) image;
        byte[] data = input.getData();
        int nLimitY = Math.min(input.getHeight(), this.cY +this.nHeight);
        int nLimitX = Math.min(input.getWidth(), this.cX + this.nWidth);
        for (int i=this.cY; i<nLimitY; i++) {
            int nStart = i * image.getWidth();
            for (int j=this.cX; j<nLimitX; j++) {
                data[nStart+j] = this.bValue;
            }
        }
         super.setOutput(input);
    }
    
}
