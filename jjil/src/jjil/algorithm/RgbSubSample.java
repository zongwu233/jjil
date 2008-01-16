/*
 * RgbSubSample.java
 *
 * Created on September 2, 2006, 3:59 PM
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
import jjil.core.Image;
import jjil.core.PipelineStage;
import jjil.core.RgbImage;

/**
 * Pipeline stage reduces an RgbImage's size by subsampling WITHOUT
 * smoothing. This results in an incorrectly subsampled image but can be
 * done very quickly and the artifacts resulting from the subsampling can
 * themselves be useful for detection of certain kinds of objects such
 * as barcodes. This pipeline stage should be used with caution because
 * of the artifacts introduced by subsampling without smoothing.
 *
 * @author webb
 */
public class RgbSubSample extends PipelineStage {
    private int cTargetHeight;
    private int cTargetWidth;
    
    /** Creates a new instance of RgbSubSample.
     *
     * @param cReduceWidth amount to reduce the width by
     * @param cReduceHeight amount to reduce the height by
     */
    public RgbSubSample(int cTargetWidth, int cTargetHeight) {
        setTargetSize(cTargetWidth, cTargetHeight);
    }
    
    /** Reduces an RgbImage by a factor horizontally and vertically through
     * averaging. The reduction factor must be an even multiple of the image
     * size.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input image is not gray, or
     * the reduction factor does not evenly divide the image size.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof RgbImage)) {
            throw new IllegalArgumentException(image.toString() + 
                    Messages.getString("RgbSubSample.0")); //$NON-NLS-1$
        }
        if (image.getWidth() < this.cTargetWidth) {
            throw new IllegalArgumentException(image.toString() + 
                    Messages.getString("RgbSubSample.1") + this.cTargetWidth); //$NON-NLS-1$
        }
        if (image.getHeight() < this.cTargetHeight) {
            throw new IllegalArgumentException(image.toString() + 
                    Messages.getString("RgbSubSample.2") + this.cTargetHeight); //$NON-NLS-1$
        }
        // note that Java division truncates. So 
        // e.g. cReduceWidth * this.cTargetWidth <= image.getWidth
        // This is important in the for loop below to keep from out of bounds
        // array access.
        int cReduceWidth = image.getWidth() / this.cTargetWidth;
        int cReduceHeight = image.getHeight() / this.cTargetHeight;
        RgbImage rgb = (RgbImage) image;
        int[] rnIn = rgb.getData();
        RgbImage result = new RgbImage(
        		this.cTargetWidth, 
        		this.cTargetHeight);
        int[] rnOut = result.getData();
        for (int i=0; i<this.cTargetHeight; i++) {
            for (int j=0; j<this.cTargetWidth; j++) {
                rnOut[i*this.cTargetWidth + j] = 
                	rnIn[(i*image.getWidth()*cReduceHeight) + (j*cReduceWidth)];
            }
        }
        super.setOutput(result);
    }
    
    /** Returns the target height.
     *
     * @return the target height.
     */
    public int getTargetHeight()
    {
        return this.cTargetHeight;
    }
    
    /** Returns the target width.
     *
     * @return the target width.
     */
    public int getTargetWidth()
    {
        return this.cTargetWidth;
    }
    
    /** Sets a new width, height target size.
     *
     * @param cTargetWidth the target image width.
     * @param cTargetHeight the target image height.
     * @throws IllegalArgumentException if either cTargetWidth or cTargetHeight
     * is less than or equal to 0.
     */
    public void setTargetSize(int cTargetWidth, int cTargetHeight) 
        throws IllegalArgumentException {
        if (cTargetWidth <= 0 || cTargetHeight <= 0) {
            throw new IllegalArgumentException(
                    Messages.getString("RgbSubSample.3") + //$NON-NLS-1$
                    Messages.getString("RgbSubSample.4") + cTargetWidth + Messages.getString("Comma") + cTargetHeight + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        this.cTargetWidth = cTargetWidth;
        this.cTargetHeight = cTargetHeight;
    }

    /** Return a string describing the reduction operation.
     *
     * @return the string describing the reduction operation.
     */
    public String toString() {
        return super.toString() + " (" + this.cTargetWidth + Messages.getString("Comma") +  //$NON-NLS-1$ //$NON-NLS-2$
                this.cTargetHeight + ")"; //$NON-NLS-1$
    }
}
