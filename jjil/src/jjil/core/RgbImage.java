/*
 * RgbImage.java
 *
 * Created on August 27, 2006, 12:47 PM
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

package jjil.core;

/**
 * RgbImage is the type used to hold an RGB image, which
 * is stored as an ARGB image type (32-bits) with the
 * A byte always 0. 
 * <p>
 * Implementation-specific libraries define methods that allow the creation
 * of an RgbImage from a native image type. RgbImage is therefore the first and
 * last jjil.core object used after capture and before display of an image.
 * @author webb
 */
public class RgbImage extends Image {
    /** A pointer to the image data
     */
    private final int[] wImage;
    
    /** Creates a new instance of RgbImage
     *
     * @param cWidth   the image width
     * @param cHeight  the image height 
     */
    public RgbImage(int cWidth, int cHeight) {
        super(cWidth, cHeight);
        this.wImage = new int[getWidth()*getHeight()];
    }
    
    /**
     * Creates a new instance of RgbImage, assigning a constant value
     * @param bR the red color value to be assigned.
     * @param bG the green color value to be assigned.
     * @param bB the blue color value to be assigned.
     * @param cWidth the image width
     * @param cHeight the image height
     */
    public RgbImage(int cWidth, int cHeight, byte bR, byte bG, byte bB) {
        super(cWidth, cHeight);
        this.wImage = new int[getWidth()*getHeight()];
        int nRgb = RgbVal.toRgb(bR, bG, bB); 
        for (int i=0; i<this.getWidth()*this.getHeight();i++) {
            this.wImage[i] = nRgb;
        }
    }
    
    
    /** Creates a shallow copy of this image
     *
     * @return the image copy.
     */
    public Image clone()
    {
        RgbImage image = new RgbImage(getWidth(), getHeight());
        System.arraycopy(
                this.getData(), 
                0, 
                image.getData(), 
                0, 
                getWidth()*getHeight());
        return image;
    }
    
    /** Get a pointer to the image data.
     *
     * @return the data pointer.
     */
    public int[] getData()
    {
        return this.wImage;
    }
    
    
    /** Return a string describing the image.
     *
     * @return the string.
     */
    @Override
	public String toString()
    {
        return super.toString() + " (" + getWidth() + "x" + getHeight() + //$NON-NLS-1$ //$NON-NLS-2$
                ")"; //$NON-NLS-1$
    }
}
