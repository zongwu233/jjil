/*
 * FftGray8.java
 *
 * Created on October 31, 2007, 4:02 PM
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
import jjil.core.Complex;
import jjil.core.Complex32Image;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;

/**
 * Takes the fast Fourier transform of the input Gray8Image. The output image
 * is a Complex32Image. The image size must be a power of 2.
 * @author webb
 */
public class FftGray8 extends PipelineStage {
    /**
     * Defines the scale factor applied to the image as a power of two, for accuracy.
     */
    public static int SCALE = 8;
    
    private Fft1d fft = null;
    
    /**
     * Creates a new instance of FftGray8.
     */
    public FftGray8() {
    }
    
    /**
     * Performs the fast Fourier transform on an image. The input image is a Gray8Image,
     * and the output is a Complex32Image. The input is scaled by shifting left SCALE
     * bits before the transformation, for accuracy.
     * @param im Input image. Must be a Gray8Image.
     * @throws java.lang.IllegalArgumentException If the input is not a Gray8Image or the image size is not a power of two.
     */
    public void Push(Image im) throws IllegalArgumentException {
        if (!(im instanceof Gray8Image)) {
            throw new IllegalArgumentException(im.toString() + Messages.getString("FftGray8.0") + //$NON-NLS-1$
                    Messages.getString("FftGray8.1")); //$NON-NLS-1$
        }
        int nWidth = im.getWidth();
        int nHeight = im.getHeight();
        if ((nWidth & (nWidth-1)) != 0) {
            throw new IllegalArgumentException(Messages.getString("FftGray8.2") + //$NON-NLS-1$
                    Messages.getString("FftGray8.3") + nWidth); //$NON-NLS-1$
        }
        if ((nHeight & (nHeight-1)) != 0) {
            throw new IllegalArgumentException(Messages.getString("FftGray8.4") + //$NON-NLS-1$
                    Messages.getString("FftGray8.5") + nHeight); //$NON-NLS-1$
        }
        // initialize FFT
        if (this.fft == null) {
            this.fft = new Fft1d(Math.max(nWidth, nHeight));
        } else {
            this.fft.setMaxWidth(Math.max(nWidth, nHeight));
        }
        
        Gray8Image gray = (Gray8Image) im;
        byte data[] = gray.getData();
        // create output
        Complex32Image cxmResult = new Complex32Image(nWidth, nHeight);
        // take FFT of each row
        int nIndex = 0;
        Complex cxRow[] = new Complex[nWidth];
        for (int i=0; i<nHeight; i++) {
            for (int j=0; j<nWidth; j++) {
                // convert each byte to a complex number. Imaginary component is 0.
                // everything gets scaled for accuracy
                cxRow[j] = new Complex((data[nIndex++] - Byte.MIN_VALUE) << SCALE);
            }
            // compute FFT
            Complex cxResult[] = this.fft.fft(cxRow);
            // save result
            System.arraycopy(cxResult, 0, cxmResult.getData(), i*nWidth, nWidth);
        }
        // take FFT of each column
        Complex cxCol[] = new Complex[nHeight];
        for (int j=0; j<nWidth; j++) {
            // copy column into a 1-D array
            for (int i=0; i<nHeight; i++) {
                cxCol[i] = cxmResult.getData()[i*nWidth+j];
            }
            // compute FFT
            Complex cxResult[] = this.fft.fft(cxCol);
            // save result back into column
             for (int i=0; i<nHeight; i++) {
                cxmResult.getData()[i*nWidth+j] = cxResult[i];
            }
       }
       super.setOutput(cxmResult);
    }
}
