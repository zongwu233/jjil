/*
 * Gray82Gray32.java
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
import jjil.core.Gray32Image;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;
/** Gray82Gray32 converts an 32-bit gray image to a 8-bit
 *  gray image. Input values are clamped between 0 and 255
 *  and offset for signed output.
 *
 * @author webb
 */
public class Gray322Gray8 extends PipelineStage {
    
    /** Creates a new instance of Gray82Gray32 */
    public Gray322Gray8() {
    }
    
    /** Converts a 32-bit gray image into an 8-bit gray image. 
     *
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input is not a Gray8Image
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray32Image)) {
            throw new IllegalArgumentException(image.toString() + "" + //$NON-NLS-1$
                    Messages.getString("Gray322Gray8.1")); //$NON-NLS-1$
        }
        Gray32Image gray32 = (Gray32Image) image;
        Gray8Image gray8 = new Gray8Image(image.getWidth(), image.getHeight());
        int[] gray32Data = gray32.getData();
        byte[] gray8Data = gray8.getData();
        for (int i=0; i<gray32.getWidth() * gray32.getHeight(); i++) {
            /* Convert from signed byte value to unsigned byte for storage
             * in the 32-bit image.
             */
             /* Assign 32-bit output */
            gray8Data[i] = (byte) 
                (Math.min(Byte.MAX_VALUE-Byte.MIN_VALUE, 
                    Math.max(0, gray32Data[i])) + Byte.MIN_VALUE);
        }
        super.setOutput(gray8);
    }
}
