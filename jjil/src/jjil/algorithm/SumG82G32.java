/*
 * SumG82G32.java
 *   "Sum Gray8[Sub]Image to Gray32[Sub]Image".
 *   Forms integral image by summing pixels in a
 *   Gray8[Sub]Image to form a Gray32[Sub]Image.
 * The computation is O(i,j) = Sum for k<=i,l<=j of I(k,l)
 * Note output type is 32 bit because otherwise we'd get
 * truncation. With 32-bit output we can go up to 
 * 65,536 = 256x256 pixels in the input image.
 *
 * Created on July 1, 2007, 3:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jjil.algorithm;
import jjil.core.Gray32Image;
import jjil.core.Gray32SubImage;
import jjil.core.Gray8Image;
import jjil.core.Gray8SubImage;
import jjil.core.Image;
import jjil.core.PipelineStage;

/**
 *
 * @author webb
 */
public class SumG82G32 extends PipelineStage {
    
    /**
     * Creates a new instance of SumG82G32
     */
    public SumG82G32() {
    }
    
    public void Push(Image image) throws IllegalArgumentException
    {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() +
                    Messages.getString("SumG82G32.0")); //$NON-NLS-1$
        }
        Gray32Image imageResult;
        if (image instanceof Gray8SubImage) {
            Gray8SubImage sub = (Gray8SubImage) image;
            imageResult = 
                    new Gray32SubImage(sub.getWidth(),
                        sub.getHeight(),
                        sub.getXOffset(),
                        sub.getYOffset());
        } else {
            // must be a Gray8Image
            imageResult = 
                    new Gray32Image(image.getWidth(), image.getHeight());
        }
        byte[] inData = ((Gray8Image) image).getData();
        // pointer to output data area, whether Gray32Image or Gray32SubImage
        int[] outData = imageResult.getData();
        // initialize first row
        int prevPixel = 0;
        for (int i=0; i<image.getWidth(); i++) {
            prevPixel += inData[i];
            outData[i] = prevPixel;
        }
        // initialize first column
        prevPixel = 0;
        for (int i=0; i<image.getHeight(); i++) {
            prevPixel += inData[i*image.getWidth()];
            outData[i*image.getWidth()] = prevPixel;
        }
        // fill in the rest of the array
        int nPrevRow = 0;
        for (int i=1; i<image.getHeight(); i++) {
            int nCurrRow = nPrevRow + image.getWidth();
            prevPixel = inData[nCurrRow];
            for (int j=1; j<image.getWidth(); j++) {
                outData[nCurrRow + j] = 
                    inData[nCurrRow + j] +
                    prevPixel +
                    outData[nPrevRow + j];
                prevPixel += inData[nCurrRow + j];
            }
            nPrevRow = nCurrRow;
        }
        super.setOutput(imageResult);
    }
    
}
