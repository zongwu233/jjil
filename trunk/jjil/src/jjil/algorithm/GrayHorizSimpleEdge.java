/*
 * CannyHoriz.java
 *
 * Created on August 27, 2006, 4:32, PM
 *
 * To change this templatef, choose Tools | Template Manager
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
import jjil.core.Error;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;

/**
 * Computes the horizontal Canny operator for an input gray image. The sigma
 * value for the operator is set in the constructor or setSigma. All calculations
 * are done in integer (per CLDC 1.0) and the sigma value is specified as
 * multiplied by 10.0. The minimum value for the unmultiplied sigma is 0.1;
 * the maximum value is about 10.0. Larger sigma values give an operator which
 * is less sensitive to high frequencies and more sensitive to low frequencies.
 * <p>
 * Hungarian prefix is 'canny'.
 * @author webb
 */
public class GrayHorizSimpleEdge extends PipelineStage {
    
    /** Creates a new instance of CannyHoriz 
     *
     * @param cSigma the sigma value for the operator, which is the sigma
     * in the Gaussian distribution multipied by 10.0 and converted to integer.
     * @throws IllegalArgumentException is sigma is out of range.
     */
    public GrayHorizSimpleEdge() {
    }
    
    /** Apply the Canny operator horizontally to the input input image.
     * The sigma value for the operator is set in the class constructor.
     * We handle the borders of the image a little carefully to avoid creating
     * spurious edges at them. The image value at the border is reflected so
     * that image(0,-1), for example, is made equal to image(0,1). 
     *
     * @param image the input Gray8Image
     * @throws IllegalArgumentException if image is not a Gray8Image
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(
                	new Error(
                			Error.PACKAGE.ALGORITHM,
                			ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
                			image.toString(),
                			null,
                			null));
        }
        Gray8Image input = (Gray8Image) image;
        byte[] bIn = input.getData();
        int cWidth = input.getWidth();
        for (int i=0; i<input.getHeight(); i++) {
        	int nPrev;
        	int nThis = bIn[i*cWidth];
            for (int j=0; j<cWidth; j++) {
            	nPrev = nThis;
            	nThis = bIn[i*cWidth + j];
            	int nVal = Math.max(Byte.MIN_VALUE, 
            			Math.min(Byte.MAX_VALUE, nPrev - nThis));
            	bIn[i*cWidth + j] = (byte) nVal;
            }
        }
        super.setOutput(input);
    }
    
}
