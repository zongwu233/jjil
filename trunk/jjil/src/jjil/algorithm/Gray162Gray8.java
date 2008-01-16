/*
 * Gray162Gray8.java
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
import jjil.core.Gray16Image;
import jjil.core.Gray32Image;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;
/** Gray162Gray8 converts an 8-bit gray image to a 32-bit
 *  gray image.
 *
 * @author webb
 */
public class Gray162Gray8 extends PipelineStage {
    
    /** Creates a new instance of Gray162Gray8 */
    public Gray162Gray8() {
    }
    
    /** Converts an 16-bit gray image into an 8-bit image by and'ing off
     * the top 8 bits of every pixel.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input is not a Gray8Image
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray16Image)) {
            throw new IllegalArgumentException(image.toString() + "" + //$NON-NLS-1$
                    Messages.getString("Gray162Gray8.1")); //$NON-NLS-1$
        }
        Gray16Image gray = (Gray16Image) image;
        Gray8Image gray8 = new Gray8Image(image.getWidth(), image.getHeight());
        short[] grayData = gray.getData();
        byte[] gray8Data = gray8.getData();
        for (int i=0; i<gray.getWidth() * gray.getHeight(); i++) {
            /* Convert from 16-bit value to 8-bit value, discarding
             * most significant bits.
             */
            gray8Data[i] = (byte) (grayData[i] & 0xff);
        }
        super.setOutput(gray8);
    }
}
