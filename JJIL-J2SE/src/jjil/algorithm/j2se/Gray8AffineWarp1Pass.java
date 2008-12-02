/*
 * Gray8AffineWarp1Pass.java
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
public class Gray8AffineWarp1Pass extends PipelineStage {   
    private enum WarpOrder {
        WARP_X_FIRST,
        WARP_Y_FIRST
    };
    
    private WarpOrder eWarpOrder;
    int nXOffset, nYOffset;
    double rdWarp[][], rdInvWarp[][];
    double rdWarpX[];
    double rdWarpY[];
    
    /** Creates a new instance of Gray8AffineWarp1Pass. Gray8AffineWarp1Pass performs
     * an affine warp on an input Gray8Image. The affine transformation is
     * done in a single pass, calculating an affine transformation at every
     * point of the output image. This is slower but easier to debug than
     * Gray8AffineWarp, which should be used instead of this class.
     * <p>
     * @param warp the 2x3 affine warp to be performed.
     * @throws jjil.core.Error if the warp is null or not a 2x3 matrix.
     */
    public Gray8AffineWarp1Pass(double[][] warp) throws jjil.core.Error {
        this.setWarp(warp);
    }
    
    private Vec2 affineTrans(double a[][], Vec2 p) {
        return new Vec2(
                a[0][0] * p.getX() + a[0][1] * p.getY() + a[0][2],
                a[1][0] * p.getX() + a[1][1] * p.getY() + a[1][2]);
    }
    
    private void calculateInverse() {
        double denom = this.rdWarp[0][0] * this.rdWarp[1][1] -
                this.rdWarp[0][1] * this.rdWarp[1][0];
        this.rdInvWarp = new double[2][];
        this.rdInvWarp[0] = new double[3];
        this.rdInvWarp[1] = new double[3];
        this.rdInvWarp[0][0] = this.rdWarp[1][1] / denom;
        this.rdInvWarp[0][1] = -this.rdWarp[0][1] / denom;
        this.rdInvWarp[0][2] = -(this.rdInvWarp[0][0] * this.rdWarp[0][2] +
                this.rdInvWarp[0][1] * this.rdWarp[1][2]);
        this.rdInvWarp[1][0] = -this.rdWarp[1][0] / denom;
        this.rdInvWarp[1][1] = this.rdWarp[0][0] / denom;
        this.rdInvWarp[1][2] = -(this.rdInvWarp[1][0] * this.rdWarp[0][2] +
                this.rdInvWarp[1][1] * this.rdWarp[1][2]);
        
        /* Verify inverse
        double prod[][] = new  double[3][];
         for (int i=0; i<2; i++) {
        prod[i] = new double[3];
        for (int j=0; j<3; j++) {
        prod[i][j] = 0.0d;
        for (int k=0; k<2; k++) {
        prod[i][j] += rdInvWarp[i][k] * rdWarp[k][j];
        }
        if (j == 2) {
        prod[i][j] += rdInvWarp[i][2];
        }
        }
        }
        prod[2] = new double[3];
        prod[2][0] = 0.0d;
        prod[2][1] = 0.0d;
        prod[2][2] = 1.0d;*/
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
        
        super.setOutput(
                new Gray8OffsetImage(doWarp((Gray8Image) image),
                    this.nXOffset,
                    this.nYOffset));
    }
    
    public Gray8Image doWarp(Gray8Image grayIn) {
        // first calculate bounds of output image
        Vec2 p00 = affineTrans(this.rdWarp, new Vec2(0.0d, 0.0d));
        Vec2 p01 = affineTrans(this.rdWarp, new Vec2(0.0d, (double) grayIn.getHeight()));
        Vec2 p10 = affineTrans(this.rdWarp, new Vec2((double) grayIn.getWidth(), 0.0d));
        Vec2 p11 = affineTrans(this.rdWarp, new Vec2((double) grayIn.getWidth(), 
                (double) grayIn.getHeight()));
        int nMinX = (int) Math.min(p00.getX(), Math.min(p01.getX(), Math.min(p10.getX(), p11.getX())));
        int nMaxX = (int) Math.max(p00.getX(), Math.max(p01.getX(), Math.max(p10.getX(), p11.getX())));
        int nMinY = (int) Math.min(p00.getY(), Math.min(p01.getY(), Math.min(p10.getY(), p11.getY())));
        int nMaxY = (int) Math.max(p00.getY(), Math.max(p01.getY(), Math.max(p10.getY(), p11.getY())));
        this.nXOffset = -nMinX;
        this.nYOffset = -nMinY;
        int nWidth = nMaxX - nMinX;
        int nHeight = nMaxY - nMinY;
        Gray8Image grayOut = new Gray8Image(nWidth, nHeight, Byte.MIN_VALUE);
        byte[] dataIn = grayIn.getData();
        byte[] dataOut = grayOut.getData();
        for (int x = nMinX; x<nMaxX; x++) {
            for (int y=nMinY; y<nMaxY; y++) {
                Vec2 p = new Vec2((double) x, (double) y);
                Vec2 pMap = affineTrans(this.rdInvWarp, p);
                double xFloor = Math.floor(pMap.getX());
                double yFloor = Math.floor(pMap.getY());
                int nX = (int) xFloor;
                int nY = (int) yFloor;
                if (nX >= 0 && nX < grayIn.getWidth()-1 &&
                        nY >= 0 && nY < grayIn.getHeight()-1) {
                    double xFrac = pMap.getX() - xFloor;
                    double yFrac = pMap.getY() - yFloor;
                    // interpolate x value
                    int x1 = (int) (
                            (1.0d-xFrac) * dataIn[nY*grayIn.getWidth() + nX] +
                            xFrac * dataIn[nY*grayIn.getWidth() + nX + 1]);
                    int x2 = (int) (
                            (1.0d-xFrac) * dataIn[(nY+1)*grayIn.getWidth() + nX] +
                            xFrac * dataIn[(nY+1)*grayIn.getWidth() + nX + 1]);
                    // interpolate y
                    dataOut[(y-nMinY)*grayOut.getWidth()+(x-nMinX)] = (byte) (
                            (1.0d-yFrac) * x1 + yFrac * x2);
                }
            }
        }
        return new Gray8OffsetImage(grayOut, this.nXOffset, this.nYOffset);
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
        this.rdWarp = warp;
        calculateInverse();
    }
    
    public Vec2 warpVec(Vec2 p) {
        double x = p.getX() * this.rdWarp[0][0] + p.getY() * this.rdWarp[0][1] +
                this.rdWarp[0][2];
        double y = p.getX() * this.rdWarp[1][0] + p.getY() * this.rdWarp[1][1] +
                this.rdWarp[1][2];
        return new Vec2(x,y);
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
