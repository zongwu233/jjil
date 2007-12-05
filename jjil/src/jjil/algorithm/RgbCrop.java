/*
 * RgbCrop.java
 *
 * Created on August 27, 2006, 2:38 PM
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
 * Pipeline stage crops a gray image to a given rectangular cropping window.
 * <p>
 * Hungarian prefix is 'crop'.
 * @author webb
 */
public class RgbCrop extends PipelineStage {
    int cHeight; /* height of cropping window */
    int cWidth; /* width of cropping window */
    int cX; /* left edge of cropping window */
    int cY; /* top of cropping window */
    
    /** Creates a new instance of RgbCrop. The cropping window
     * is specified here.
     *
     * @param x left edge of cropping window
     * @param y top edge of cropping window
     * @param width width of cropping window
     * @param height height of cropping window
     * @throws IllegalArgumentException if the top left corner of the
     * window is negative, or the window area is non-positive.
     */
    public RgbCrop(
            int x,
            int y,
            int width,
            int height) throws IllegalArgumentException {
        if (x<0 || y<0 || width<=0 || height<=0) {
            throw new IllegalArgumentException(
                    "Illegal cropping window (" + 
                    x + "," + y + "," + 
                    width + "," + height + ")");
        }
        this.cX = x;
        this.cY = y;
        this.cWidth = width;
        this.cHeight = height;
    }
    
    /** Crops the input RGB image to the cropping window that was
     * specified in the constructor.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the cropping window
     *    extends outside the input image, or the input image
     *    is not an RgbImage.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof RgbImage)) {
            throw new IllegalArgumentException(image.toString() +
                    " should be a RgbImage, but isn't");
        }
        RgbImage imageInput = (RgbImage) image;
        if (this.cX + this.cWidth > image.getWidth() ||
            this.cY + this.cHeight > image.getHeight()) {
            throw new IllegalArgumentException("Cropping window (" +
                    this.cX + "," + this.cY + "," + 
                    this.cWidth + "," + this.cHeight + 
                    ") extends outside image " +
                    image.toString());
        }
        RgbImage imageResult = new RgbImage(this.cWidth,this.cHeight);
        int[] src = imageInput.getData();
        int[] dst = imageResult.getData();
        for (int i=0; i<this.cHeight; i++) {
            System.arraycopy(
                    src, 
                    (i+this.cY)*image.getWidth() + this.cX,
                    dst,
                    i*this.cWidth,
                    this.cWidth);
        }
        super.setOutput(imageResult);
    }
    
    /**
     * Gets the cropping window height
     * @return the cropping window height
     */
    public int getHeight() {
        return this.cHeight;
    }
    
    /**
     * Gets the cropping window left edge
     * @return the cropping window left edge
    */
    public int getLeft() {
        return this.cX;
    }
    
    /**
     * Gets the cropping window top
     * @return the cropping window top
     */
    public int getTop() {
        return this.cY;
    }
    
    /**
     * Gets the cropping window width
     * @return the cropping window width
     */
    public int getWidth() {
        return this.cWidth;
    }
    
    /** Change the cropping window. 
     *
     * @param x left edge of cropping window
     * @param y top edge of cropping window
     * @param width width of cropping window
     * @param height height of cropping window
     * @throws IllegalArgumentException if the top left corner of the
     * window is negative, or the window area is non-positive.
     */
    public void setWindow(
            int x,
            int y,
            int width,
            int height) throws IllegalArgumentException {
        if (x<0 || y<0 || width<=0 || height<=0) {
            throw new IllegalArgumentException(
                    "Illegal cropping window (" + 
                    x + "," + y + "," + 
                    width + "," + height + ")");
        }
        this.cX = x;
        this.cY = y;
        this.cWidth = width;
        this.cHeight = height;
    }
    
    /** Return a string describing the cropping operation.
     *
     * @return the string describing the cropping operation.
     */
    public String toString() {
        return super.toString() + " (" + this.cX + "," + this.cY + 
                "," + this.cWidth + "," + this.cHeight + ")";
    }
}
