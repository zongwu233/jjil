/*
 * LinefitHoughHoriz.java
 *
 * Created on September 9, 2006, 1:15 PM
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

package jjil.algorithm.j2se;
import java.util.Enumeration;
import java.util.Vector;

import jjil.core.Error;
import jjil.core.Point;

/**
 * Finds a line in an array of points using Hough transform. Not a pipeline
 * stage. Returns the most likely line as slope and Y-intercept through
 * member access functions. The line search for must be oriented more or less
 * horizontally (within the slope range specified).
 * @author webb
 */
public class LinefitHoughHoriz {
    /** @var cHoughAccum the Hough accumulator array */
    int[][] cHoughAccum;
    /** @var cCount the number of points on the line that was found */
    int cCount = 0;
    /** @var cMaxSlope the maximum allowable slope, times 256 */
    final double cMaxSlope;
    /** @var cMaxY the maximum allowable y-intercept */
    final int cMaxY;
    /** @var cMinSlope the minimum allowable slope, times 256 */
    final double cMinSlope;
    /** @var cMinY the minimum allowable y-intercept */
    final int cMinY;
    /** @var cSlope the slope of the line that was found, times 256 */
    double cSlope;
    /** @var cSlopeSteps the number of steps to take from cMinSlope to cMaxSlope */
    final int cSlopeSteps;
    /** @var cYInt the y-intercept of the line that was found */
    int cYInt;
    /**
     * @var cYSteps the number of steps taken from y intercept min to max
     */
    int cYSteps;
    
    /** Creates a new instance of LinefitHoughHoriz 
     *
     * @param cMinY minimum Y value
     * @param cMaxY maximum Y value
     * @param cMinSlope minimum slope 
     * @param cMaxSlope maximum slope 
     * @param cSlopeSteps steps taken in Hough accumulator between minimum and
     * maximum slope.
     * @throws jjil.core.Error if Y or slope range is empty, or
     * cSlopeSteps is not positive.
     */
    public LinefitHoughHoriz(
            int cMinY, 
            int cMaxY, 
            int cYSteps,
            double cMinSlope, 
            double cMaxSlope, 
            int cSlopeSteps) throws jjil.core.Error {
        if (cMaxY < cMinY) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_RANGE_NULL_OR_NEGATIVE,
                            new Integer(cMinY).toString(),
                            new Integer(cMaxY).toString(),
                            null);
        }
        this.cMinY = cMinY;
        this.cMaxY = cMaxY;
        if (cMaxSlope < cMinSlope) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_RANGE_NULL_OR_NEGATIVE,
                            new Double(cMinSlope).toString(),
                            new Double(cMaxSlope).toString(),
                            null);
        }
        if (cSlopeSteps <= 0) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_OUT_OF_RANGE,
                            new Integer(cSlopeSteps).toString(),
                            new Integer(1).toString(),
                            new Integer(Integer.MAX_VALUE).toString());
        }
        if (cYSteps <= 0) {
            throw new Error(
                            Error.PACKAGE.ALGORITHM,
                            jjil.algorithm.ErrorCodes.PARAMETER_OUT_OF_RANGE,
                            new Integer(cYSteps).toString(),
                            new Integer(1).toString(),
                            new Integer(Integer.MAX_VALUE).toString());
        }
        this.cMinSlope = cMinSlope;
        this.cMaxSlope = cMaxSlope;
        this.cSlopeSteps = cSlopeSteps;
        this.cYSteps = cYSteps;
    }
    
    /** Add a new point to the Hough accumulator array. We increment along the 
     * line in the array
     * from (cMinSlope, yIntStart) to (cMaxSlope, yIntEnd), where
     * yIntStart is the y-intercept assuming the slope is at the minimum,
     * and yIntEnd is the y-intercept assuming the slope is maximal.
     *
     * @param p the point to add to the accumulator array
     */
    private void addPoint(Point p) {
        /** work along the line from (0,yIntStart) to (cSlopeSteps,yIntEnd),
         * incrementing the Hough accumulator.
         */
        for (int nSlopePos = 0; 
            nSlopePos < this.cSlopeSteps; 
            nSlopePos++) {
            double fSlope = nSlopePos * (this.cMaxSlope - this.cMinSlope) / 
                    this.cSlopeSteps + this.cMinSlope;
           int yInt = (int) (p.getY() - p.getX() * fSlope);
           /** check if the current position falls inside the Hough 
            * accumulator.
            */
           if (yInt >= this.cMinY && yInt < this.cMaxY) {
               int yPos = this.cYSteps * (yInt-this.cMinY) / (this.cMaxY - this.cMinY);
                this.cHoughAccum[nSlopePos][yPos]++;               
           }
        };
    }
    
    /** Find the peak in the Hough array. Updates cCount, cSlope, and cYInt.
     */
    private void findPeak() {
        this.cCount = Integer.MIN_VALUE;
        for (int slope=1; slope<this.cSlopeSteps-1; slope++) {
            for (int y=1; y<this.cYSteps-1; y++) {
                int sum=0;
                for (int i=-1; i<=1; i++) {
                    for (int j=-1; j<=1; j++) {
                        sum+= this.cHoughAccum[slope+i][y+j];
                    }
                }
                if (sum > this.cCount) {
                    this.cCount = sum;
                    this.cSlope = slope * (this.cMaxSlope - this.cMinSlope) 
                        / this.cSlopeSteps + this.cMinSlope;
                    this.cYInt = y * (this.cMaxY - this.cMinY) / this.cYSteps + 
                            this.cMinY;
                }
            }
        }
    }
    
    /** Returns the count of points on the line that was found.
     *
     * @return the point count.
     */
    public int getCount() {
        return this.cCount;
    }
    
    /** Returns the slope of the line that was found.
     *
     * @return the line slope 
     */
    public double getSlope() {
        return this.cSlope;
    }
    
    /** Returns the y-intercept of the line that was found.
     *
     * @return the y-intercept.
     */
    public int getY() {
        return this.cYInt;
    }
    
    /** Finds the most likely line passing through the points in the Vector.
     * 
     * @param points the input Vector of point positions
     * @throws jjil.core.Error if points is not a Vector of 
     * point objects.
     */
    public void push(Vector points) throws jjil.core.Error {
        /* create Hough accumulator */
        this.cHoughAccum = 
                new int[this.cSlopeSteps][this.cYSteps];
        /* fill the Hough accumulator
         */
        for (Enumeration e = points.elements(); e.hasMoreElements();) {
            Object o = e.nextElement(); 
            if (!(o instanceof Point)) {
                throw new Error(
                                Error.PACKAGE.ALGORITHM,
                                jjil.algorithm.ErrorCodes.OBJECT_NOT_EXPECTED_TYPE,
                                o.toString(),
                                "Point",
                                null);
             }
            Point p = (Point) o;
            addPoint(p);
        }
        findPeak(); // sets cYInt, cSlope, cCount for access by caller
        this.cHoughAccum = null; // free memory
    }
       
    /** Return a string describing the current instance, giving the values
     * of the constructor parameters.
     *
     * @return the string describing the current instance.
     */
    public String toString() {
        return super.toString() + "(" + this.cMinY + "," + this.cMaxY + "," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                this.cYSteps + "," +
                this.cMinSlope + "," + this.cMaxSlope + "," + this.cSlopeSteps + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
