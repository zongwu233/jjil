/*
 * GrayHistEq.java
 *
 * Created on September 9, 2006, 2:05 PM
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
 * Equalize the histogram of a gray image.
 * <p>
 * Hungarian prefix is 'he'.
 * @author webb
 */
public class GrayHistEq extends PipelineStage {
    private int cPixels = 0;
    private GrayHistMatch histMatch;
    
    /** Creates a new instance of GrayHistEq */
    public GrayHistEq() {
        int[] nullHist = new int[256];
        this.histMatch = new GrayHistMatch(nullHist);
    }
    
    /** Equalize the histogram of an input gray image.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input image is not gray.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() + " should " +
                    "be a gray image");
        }
        Gray8Image gray = (Gray8Image) image;
        /* In order to avoid recreating histMatch every call, we recompute
         * the target histogram only when the image size (total # pixels)
         * changes.
         */
        if (image.getWidth() * image.getHeight() != cPixels) {
            this.cPixels = image.getWidth() * image.getHeight();
            int cPixelsRemaining = cPixels;
            /* (Re)compute the equalizing histogram. We want a histogram
             * which is as flat as possible. The calculation below evens
             * out the histogram values so all are within 1 of each other.
             */
            int[] histogram = new int[256];
            for (int i=0; i<256; i++) {
                int c = cPixelsRemaining / (256 - i);
                histogram[i] = c;
                cPixelsRemaining -= c;
            }
            this.histMatch.setHistogram(histogram);
        }
        /* Apply the histogram match algorithm to equalize the histogram
         * of the input.
         */
        this.histMatch.Push(gray);
        super.setOutput(this.histMatch.Front());
    }
}
