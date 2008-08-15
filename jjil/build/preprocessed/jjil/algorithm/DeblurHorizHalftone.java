/*
 * DeblurHorizHalftone.java
 *
 * Created on November 3, 2007, 3:07 PM
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
import jjil.core.Error;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;
/**
 * Uses deconvolution to remove blur from a Gray8Image. The blur removed is a 
 * horizontal Gaussian blur with a given standard deviation. The background noise
 * level in the input image can be adjusted. The output Gray8Image is rescaled so the
 * maximum and minimum values fill the range from Byte.MIN_VALUE to Byte.MAX_VALUE.
 * @author webb
 */
public class DeblurHorizHalftone extends PipelineStage {

     
    /**
     * Creates a new instance of InverseFilter.
     */
    public DeblurHorizHalftone() {
   }
    
    /**
     * Deblurs an input Gray8Image which has been blurred by a horizontal Gaussian
     * of the given standard deviation and which has a background noise level less
     * than the given level.
     * @param im Input Gray8Image.
     * @throws jjil.core.Error if the input is not of type Gray8Image.
     */
    public void push(Image im) throws jjil.core.Error {
        if (!(im instanceof Gray8Image)) {
            throw new Error(
                			Error.PACKAGE.ALGORITHM,
                			ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
                			im.toString(),
                			null,
                			null);
        }
        byte[] bData = ((Gray8Image) im).getData();
        for (int i=0; i<im.getHeight(); i++) {
            int nRow = i * im.getWidth();
            for (int j=0; j<im.getWidth(); j++) {
                byte bVal = bData[nRow + j];
                byte bNewVal;
                if (bVal >= 0) {
                    bNewVal = Byte.MAX_VALUE;
                } else {
                    bNewVal = Byte.MIN_VALUE;
                }
                int nDiff = bVal - bNewVal;
                /*
                if (j < im.getWidth()-1) {
                    bData[nRow + j + 1] = (byte) Math.max(Byte.MIN_VALUE, 
                            Math.min(Byte.MAX_VALUE, bData[nRow + j + 1] + 0 * nDiff / 8));
                }
                if (j < im.getWidth()-2) {
                    bData[nRow + j + 2] = (byte) Math.max(Byte.MIN_VALUE, 
                            Math.min(Byte.MAX_VALUE, bData[nRow + j + 2] + 0 * nDiff / 8));
                    
                }
                if (j < im.getWidth()-3) {
                    bData[nRow + j + 3] = (byte) Math.max(Byte.MIN_VALUE, 
                            Math.min(Byte.MAX_VALUE, bData[nRow + j + 3] + 0 * nDiff / 8));
                    
                }
                 */
                if (j < im.getWidth()-4) {
                    bData[nRow + j + 4] = (byte) Math.max(Byte.MIN_VALUE, 
                            Math.min(Byte.MAX_VALUE, bData[nRow + j + 4] + 5 * nDiff / 8));
                    
                }
                if (j < im.getWidth()-5) {
                    bData[nRow + j + 5] = (byte) Math.max(Byte.MIN_VALUE, 
                            Math.min(Byte.MAX_VALUE, bData[nRow + j + 5] + 3 * nDiff / 8));
                    
                }
            }
        }
        super.setOutput(im);
    }
    
}
