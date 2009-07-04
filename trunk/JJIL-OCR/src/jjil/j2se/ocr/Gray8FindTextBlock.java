/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the Lesser GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jjil.j2se.ocr;

import jjil.core.Gray8Image;
import jjil.core.Rect;

// TODO: Auto-generated Javadoc
/**
 * Given a binary image, search for the largest likely text block. The text
 * block is assumed to consist of non Byte.MIN_VALUE-pixels which are overlap
 * when projected along the horizontal axis and which are more than 1/2 the
 * maximum character height when projected along the vertical axis.
 * 
 * @author webb
 */
public class Gray8FindTextBlock {
    
    /**
     * Instantiates a new gray8 find text block.
     */
    public Gray8FindTextBlock() {
        
    }
    
    /**
     * Determine horizontal extent.
     * 
     * @param r the r
     * @param nWidth the n width
     * @param rb the rb
     * 
     * @return the rect
     */
    private Rect determineHorizontalExtent(Rect r, int nWidth, byte[] rb) {
        // examine pixels in this Rect to determine the horizontal extent
        int rnPixels[] = new int[r.getWidth()];
        for (int i=r.getTop(); i<r.getBottom(); i++) {
            for (int j=r.getLeft(); j<r.getRight(); j++) {
                if (rb[i*nWidth+j] != Byte.MIN_VALUE) {
                    rnPixels[j-r.getLeft()]++;
                }
            }
        }
        final int nMinPixels = r.getHeight() / 3;
        int i = 0;
    	while (i < r.getWidth() && rnPixels[i] > nMinPixels) {
    		i++;
    	}
    	int nLeft = r.getWidth()-1;
        while (++i < r.getWidth()) {
            if (rnPixels[i] > nMinPixels) {
            	nLeft = i;
            	break;
            }
        }
        i = r.getWidth() - 1;
        while (i > 0 && rnPixels[i] > nMinPixels) {
        	i--;
        }
        int nRight = 0;
        while (--i > nLeft) {
            if (rnPixels[i] > nMinPixels) {
            	nRight = i;
            	break;
            }
        }
//        if (bRightEdgeWhite && nRightEdgeNonWhite - nRight < r.getWidth()/10) {
//        	nRight = r.getWidth()-1;
//        }
        // on both sides we don't want to trim off any part of a character
        // at the edge, even if that character isn't 1/3 the vertical block
        // height yet
        while (nLeft > 0 && rnPixels[nLeft] != 0) {
            nLeft --;
        }
        while (nRight < r.getWidth()-1 && rnPixels[nRight] != 0) {
            nRight ++;
        }
        if (nLeft > nRight) {
        	return null;
        }
        return new Rect(nLeft+r.getLeft(), r.getTop(), nRight-nLeft, r.getHeight());
    }
    
    /**
     * Determine vertical extent.
     * 
     * @param r the r
     * @param nWidth the n width
     * @param rb the rb
     * 
     * @return the rect
     */
    private Rect determineVerticalExtent(Rect r, int nWidth, byte[] rb) {
        // first project horizontally
        int rnPixels[] = new int[r.getHeight()];
        for (int i=r.getTop(); i<r.getBottom(); i++) {
            for (int j=r.getLeft(); j<r.getRight(); j++) {
                if (rb[i*nWidth+j] != Byte.MIN_VALUE) {
                    rnPixels[i-r.getTop()]++;
                }
            }
        }
        // find longest continuous stretch of non-zero pixels horizontally
        boolean bInRun = false;
        int nStart = 0, nBestStart = 0;
        int nLength = Integer.MIN_VALUE, nBestLength = Integer.MIN_VALUE;
        for (int i=0; i<r.getHeight(); i++) {
            if (rnPixels[i] != 0) {
                if (!bInRun) {
                    bInRun = true;
                    nStart = i;
                    nLength = 1;
                } else {
                    nLength ++;
                }
            } else if (bInRun) {
                if (nLength > nBestLength) {
                    nBestLength = nLength;
                    nBestStart = nStart;
                 }
                 bInRun = false;
            }
        }
        if (bInRun && nLength > nBestLength) {
            nBestLength = nLength;
            nBestStart = nStart;
        }      
        if (nBestLength <= 0) {
        	return null;
        }
        return new Rect(r.getLeft(), nBestStart+r.getTop(), r.getWidth(), nBestLength);
    }
    
    /**
     * Push.
     * 
     * @param gray the gray
     * 
     * @return the rect
     */
    public Rect push(Gray8Image gray) {
    	Rect rAll = new Rect(0, 0, gray.getWidth(), gray.getHeight());
        Rect rVert = determineVerticalExtent(rAll, gray.getWidth(), gray.getData());
        if (rVert == null) {
        	return null;
        }
        Rect rHoriz = determineHorizontalExtent(rVert, gray.getWidth(), gray.getData());
        // do it again to eliminate edge effects
        if (rHoriz == null) {
        	return null;
        }
        rVert = determineVerticalExtent(rHoriz, gray.getWidth(), gray.getData());
        if (rVert == null) {
        	return null;
        }
        return determineHorizontalExtent(rVert, gray.getWidth(), gray.getData());
    }
}
