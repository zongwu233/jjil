/*
 * Gray8AffineWarp.java
 *
 * Created on September 9, 2006, 3:17 PM
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

package jjil.algorithm.j2se;
import jjil.algorithm.*;
import jjil.core.Error;
import jjil.core.Gray8Image;
import jjil.core.Gray8OffsetImage;
import jjil.core.Image;
import jjil.core.PipelineStage;

/**
 * This PipelineStage performs an affine transformation on an input
 * @author webb
 */
public class Gray8AffineWarp extends PipelineStage {   
    private enum WarpOrder {
        WARP_X_FIRST,
        WARP_Y_FIRST
    };
    
    private WarpOrder eWarpOrder;
    int nMaxX, nMaxY, nMinX, nMinY;
    int nXOffset, nYOffset;
    double rdWarp[][];
    double rdWarpX[];
    double rdWarpY[];
    
    /** Creates a new instance of Gray8AffineWarp. Gray8AffineWarp performs
     * an affine warp on an input Gray8Image. The affine transformation is
     * decomposed into two stages, following the work of George Wolberg.
     * See http://www-cs.ccny.cuny.edu/~wolberg/diw.html for the definitive
     * work on image warping.
     * <p>
     * @param warp the 2x3 affine warp to be performed.
     * @throws jjil.core.Error if the warp is null or not a 2x3 matrix.
     */
    public Gray8AffineWarp(double[][] warp) throws jjil.core.Error {
        this.setWarp(warp);
    }
    
    

    private Vec2 affineTrans(double a[][], Vec2 p) {
        return new Vec2(
                a[0][0] * p.getX() + a[0][1] * p.getY() + a[0][2],
                a[1][0] * p.getX() + a[1][1] * p.getY() + a[1][2]);
    }

    /**
     * Affine warp of an image.
     *
     * @param image the input gray image.
     * @throws jjil.core.Error if the input image is not gray,
     * or the trapezoid already specified extends outside its bounds.
     */
    public void push(Image image) throws jjil.core.Error {
        if (!(image instanceof Gray8Image)) {
            throw new Error(
    				Error.PACKAGE.ALGORITHM,
    				ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
    				image.toString(),
    				null,
    				null);
        }
        // first calculate bounds of output image
        Vec2 p00 = affineTrans(this.rdWarp, new Vec2(0.0d, 0.0d));
        Vec2 p01 = affineTrans(this.rdWarp, new Vec2(0.0d, (double) image.getHeight()));
        Vec2 p10 = affineTrans(this.rdWarp, new Vec2((double) image.getWidth(), 0.0d));
        Vec2 p11 = affineTrans(this.rdWarp, new Vec2((double) image.getWidth(), 
                (double) image.getHeight()));
        this.nMinX = (int) Math.min(p00.getX(), Math.min(p01.getX(), Math.min(p10.getX(), p11.getX())));
        this.nMaxX = (int) Math.max(p00.getX(), Math.max(p01.getX(), Math.max(p10.getX(), p11.getX())));
        this.nMinY = (int) Math.min(p00.getY(), Math.min(p01.getY(), Math.min(p10.getY(), p11.getY())));
        this.nMaxY = (int) Math.max(p00.getY(), Math.max(p01.getY(), Math.max(p10.getY(), p11.getY())));
        this.nXOffset = -this.nMinX;
        this.nYOffset = -this.nMinY;
        if (this.eWarpOrder == WarpOrder.WARP_X_FIRST) {
            Gray8Image grayX = warpX((Gray8Image) image);
            //super.setOutput(new Gray8OffsetImage(grayX, this.nXOffset, this.nYOffset));
            Gray8Image grayY = warpY(grayX);
            super.setOutput(new Gray8OffsetImage(grayY, this.nXOffset, this.nYOffset));
        } else {
            Gray8Image grayY = warpY((Gray8Image) image);
            //super.setOutput(new Gray8OffsetImage(grayY, this.nXOffset, this.nYOffset));
            Gray8Image grayX = warpX(grayY);
            super.setOutput(new Gray8OffsetImage(grayX, this.nXOffset, this.nYOffset));
        }
    }
    
    /** Sets the warp in use and decomposes it into two stages, determining
     * the order of the warp (x first or y first).
     * @param warp the 2 x 3 affine warp transformation.
     * @throws jjil.core.Error if the warp is not 2x3 or the warp is not
     * decomposable (warp[0][0] or warp[1][1] is 0).
     */
    public void setWarp(double[][] warp) throws jjil.core.Error {
        if (warp.length != 2 || warp[0].length != 3 || warp[1].length != 3) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_WRONG_SIZE,
                            warp.toString(),
                            null,
                            null);
        }
        double fDivisorY = warp[0][0] * warp[1][1] - warp[0][1] * warp[1][0];
        if (warp[0][0] == 0.0d || warp[1][1] == 0.0d || fDivisorY == 0.0d) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_OUT_OF_RANGE,
                            warp.toString(),
                            null,
                            null);
        }
        this.rdWarp = warp;
        if (Math.abs(warp[0][0]) > Math.abs(warp[1][1])) {
            // warp in x direction first
            this.eWarpOrder = WarpOrder.WARP_X_FIRST;
            // copy x warp
            this.rdWarpX = new double[3];
            this.rdWarpX[0] = warp[1][1] / fDivisorY;
            this.rdWarpX[1] = -warp[0][1] / fDivisorY;
            this.rdWarpX[2] = (warp[0][1]*warp[1][2] - warp[0][2]*warp[1][1])/fDivisorY;
            // calculate y warp
            this.rdWarpY = new double[3];
            this.rdWarpY[0] = -warp[1][0]/warp[1][1];
            this.rdWarpY[1] = 1.0d / warp[1][1];
            this.rdWarpY[2] = -warp[1][2] / warp[1][1];
        } else {
            // warp in y direction first
            this.eWarpOrder = WarpOrder.WARP_Y_FIRST;
            // copy y warp
            this.rdWarpY = new double[3];
            this.rdWarpY[0] = -warp[1][0] / fDivisorY;
            this.rdWarpY[1] = warp[0][0] / fDivisorY;
            this.rdWarpY[2] = (warp[0][2]*warp[1][0] - warp[0][0]*warp[1][2])/fDivisorY;
            // calculate x warp
            this.rdWarpX = new double[3];
            this.rdWarpX[0] = 1.0d / warp[0][0];
            this.rdWarpX[1] = -warp[0][1] / warp[0][0];
            this.rdWarpX[2] = -warp[0][2] / warp[0][0];
        }
    }
    
    public Vec2 warpVec(Vec2 p) {
        double x = p.getX() * this.rdWarp[0][0] + p.getY() * this.rdWarp[0][1] +
                this.rdWarp[0][2];
        double y = p.getX() * this.rdWarp[1][0] + p.getY() * this.rdWarp[1][1] +
                this.rdWarp[1][2];
        return new Vec2(x,y);
    }

    private Gray8Image warpX(Gray8Image grayIn) {
        // allocate image. it is implicitly offset by nMinX
        Gray8Image grayOut = new Gray8Image(
                nMaxX - nMinX, 
                grayIn.getHeight(), 
                Byte.MIN_VALUE);
        // pointer to input
        byte[] bDataIn = grayIn.getData();
        byte[] bDataOut = grayOut.getData();
        for (int x = nMinX; x<nMaxX; x++) {
            for (int y = 0; y<grayIn.getHeight(); y++) {
                // calculate x in original image
                // y does not change but is offset by nYOffset
                double fX = x * this.rdWarpX[0] +
                        this.rdWarpX[1] * (y + this.nYOffset) +
                        this.rdWarpX[2];
                double fXfloor = Math.floor(fX);
                double fXfrac = fX - fXfloor;
                int nX = (int) fXfloor;
                // interpolate to get point
                if (nX >= 0 && nX < grayIn.getWidth()-1) {
                    int bIn = bDataIn[y*grayIn.getWidth() + nX];
                    int bInP1 = bDataIn[y*grayIn.getWidth() + nX + 1];
                    int bOut = (int) ((bIn * (1.0d - fXfrac)) + (bInP1 * fXfrac));
                    bDataOut[grayOut.getWidth()*y + x-nMinX] = (byte) bOut;
                }
            }
        }
        this.nXOffset = nMinX;
        return grayOut;
    }

    private Gray8Image warpY(Gray8Image grayIn) {
        // allocate image. it is implicitly offset by nMinY
        Gray8Image grayOut = new Gray8Image(
                grayIn.getWidth(), 
                nMaxY - nMinY, 
                Byte.MIN_VALUE);
        // pointer to input
        byte[] bDataIn = grayIn.getData();
        byte[] bDataOut = grayOut.getData();
        for (int y = nMinY; y<nMaxY; y++) {
            for (int x = 0; x<grayIn.getWidth(); x++) {
                // calculate y in original image
                // x does not change
                double fY = this.rdWarpY[0] * (x + this.nXOffset) +
                        this.rdWarpY[1] * y +
                        this.rdWarpY[2];
                double fYfloor = Math.floor(fY);
                double fYfrac = fY - fYfloor;
                int nY = (int) fYfloor;
                // interpolate to get point
                if (nY >= 0 && nY < grayIn.getHeight()-1) {
                    int bIn = bDataIn[nY*grayIn.getWidth() + x];
                    int bInP1 = bDataIn[(nY+1)*grayIn.getWidth() + x];
                    int bOut = (int) ((bIn * (1.0d - fYfrac)) + (bInP1 * fYfrac));
                    bDataOut[grayOut.getWidth()*(y-nMinY) + x] = (byte) bOut;
                }
            }
        }
        this.nYOffset = nMinY;
        return grayOut;
    }
    
    /**
     * Returns a string describing the current instance. All the constructor
     * parameters are returned in the order specified in the constructor.
     * @return The string describing the current instance. The string is of the form 
     * "jjil.algorithm.Gray8AffineWarpxxx (startRow,endRow,leftColStart,
     * rightColStart,leftColEnd,rightColEnd)"
     */
    public String toString() {
        return super.toString() + " (" + this.rdWarp.toString() + ")"; //$NON-NLS-1$
    }
}
