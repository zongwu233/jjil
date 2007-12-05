/*
 * RgbShrink.java.
 *    Reduces a color image to a new size by averaging the pixels nearest each
 * target pixel's pre-image. This is done by converting each band of the image
 * into a gray image, shrinking them individually, then recombining them into
 * an RgbImage
 *
 * Created on October 13, 2007, 2:21 PM
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
 * Shrinks a color (RgbImage) to a given size. Each band is shrunk independently.
 * The pixels that each pixel maps to are averaged. There is no between-target-pixel
 * smoothing. The output image must be smaller than or equal to the size of the 
 * input.
 * @author webb
 */
public class RgbShrink extends PipelineStage {
    private int cHeight;
    private int cWidth;
    private Sequence seqR, seqG, seqB;
    /** Creates a new instance of GrayRectStretch. 
     *
     * @param cWidth new image width
     * @param cHeight new image height
     * @throws IllegalArgumentException if either is less than or equal to zero.
     */
    public RgbShrink(int cWidth, int cHeight) 
        throws IllegalArgumentException {
        this.cWidth = cWidth;
        this.cHeight = cHeight;
        SetupPipeline();
    }
         
    /** Gets current target height 
     *
     * @return current height
     */
    public int getHeight() {
        return this.cHeight;
    }
    
    /** Gets current target width
     *
     * @return current width
     */
    public int getWidth() {
        return this.cWidth;
    }
    
    /**
     * Process an image.
     * @param image the input RgbImage.
     * @throws java.lang.IllegalArgumentException if the input is not an RgbImage, or is smaller than the target image either
     * horizontally or vertically.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof RgbImage)) {
            throw new IllegalArgumentException("image " + image.toString() +
                    " should be a RgbImage");
        }
        if (image.getWidth() < this.cWidth || image.getHeight() < this.cHeight) {
            throw new IllegalArgumentException("RgbShrink is for " +
                    " shrinking images only, but input size is (" +
                    image.getWidth() + "," + image.getHeight() + ") " +
                    "but target size is (" + this.cWidth + "," + 
                    this.cHeight + ")");
        }
        /* shrink R band */
        this.seqR.Push(image);
        /* shirnk G band */
        this.seqG.Push(image);
        /* shrink B band */
        this.seqB.Push(image);
        /* recombine bands */
        Gray3Bands2Rgb g3rgb = new Gray3Bands2Rgb();
        super.setOutput(g3rgb.Push(
                (Gray8Image)this.seqR.Front(), 
                (Gray8Image)this.seqG.Front(), 
                (Gray8Image)this.seqB.Front()));
    }
        
    /** Changes target height
     * 
     * @param cHeight the new target height.
     * @throws IllegalArgumentException if height is not positive
     */
    public void setHeight(int cHeight) throws IllegalArgumentException {
        if (cHeight <= 0) {
            throw new IllegalArgumentException("new height must be positive, " +
                    "not " + cHeight);
        }
        this.cHeight = cHeight;
        SetupPipeline();
    }
    
    private void SetupPipeline()
    {
        RgbSelect2Gray sel = new RgbSelect2Gray(RgbSelect2Gray.RED);
        this.seqR = new Sequence(sel);
        GrayShrink gs = new GrayShrink(cWidth, cHeight);
        this.seqR.Add(gs);
        sel = new RgbSelect2Gray(RgbSelect2Gray.GREEN);
        this.seqG = new Sequence(sel);
        gs = new GrayShrink(cWidth, cHeight);
        this.seqG.Add(gs);
        sel = new RgbSelect2Gray(RgbSelect2Gray.BLUE);
        this.seqB = new Sequence(sel);
        gs = new GrayShrink(cWidth, cHeight);
        this.seqB.Add(gs);
    }
    
    /** Changes target width
     * 
     * @param cWidth the new target width.
     * @throws IllegalArgumentException if height is not positive
     */
    public void setWidth(int cWidth) throws IllegalArgumentException {
        if (cWidth <= 0) {
            throw new IllegalArgumentException("new width must be positive, " +
                    "not " + cWidth);
        }
        this.cWidth = cWidth;
        SetupPipeline();
    }
    
   
        
    /** Return a string describing the shrinking operation.
     *
     * @return the string describing the shrinking operation.
     */
    public String toString() {
        return super.toString() + " (" + this.cWidth + "," + this.cHeight + ")";
    }
}
