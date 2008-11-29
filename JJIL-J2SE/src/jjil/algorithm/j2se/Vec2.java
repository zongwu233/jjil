/*
 * Copyright 2008 by Jon A. Webb
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
 */

package jjil.algorithm.j2se;

import jjil.core.Point;
/**
 * Implementation of 2-dimensional vector.
 * @author webb
 */
public class Vec2 {
    private double fX, fY;
    
    /**
     * Create a new Vec2, specifying x and y values
     * @param nX x value
     * @param nY y value
     */
    public Vec2(double nX, double nY) {
        this.fX = nX;
        this.fY = nY;
    }
    
    /**
     * Create a new Vec2, specifying x and y values
     * @param nX x value
     * @param nY y value
     */
    public Vec2(int nX, int nY) {
        this.fX = nX;
        this.fY = nY;
    }
    
    /**
     * Copy constructor.
     * @param v vector to copy.
     */
    public Vec2(Vec2 v) {
        this.fX = v.fX;
        this.fY = v.fY;
    }
    
    /**
     * Create a new Vec2 extending from one Point (p1) to another (p2).
     * @param p1 starting Point
     * @param p2 ending Point
     */
     public Vec2(Point p1, Point p2) {
        this.fX = p2.getX() - p1.getX();
        this.fY = p2.getY() - p1.getY();
    }
     
   /**
     * Add one Vec2 to this Vec2, modifying and returning this Vec2.
     * @param v Vec2 to add
     * @return modified Vec2
     */
    public Vec2 add(Vec2 v) {
        this.fX += v.fX;
        this.fY += v.fY;
        return this;
    }
    
    /**
     * Add a vector to a point, returning the point
     * @param p point to adjust by this vector
     * @return new point, offset by this Vec2
     */
    public Point add(Point p) {
        return new Point(p.getX() + (int)this.fX, p.getY() + (int)this.fY);
    }
 
    /**
     * Add a vector to a point, returning the point
     * @param p point to adjust by this vector
     * @return new point, offset by this Vec2
     */
    public Point add(int x, int y) {
        return new Point(x + (int)this.fX, y + (int)this.fY);
    }
 
    
    /**
     * Divide a Vec2 by a scalar.
     * @param n divisor
     * @return modified Vec2.
     */
    public Vec2 div(double n) {
        this.fX /= n;
        this.fY /= n;
        return this;
    }
    
    /**
     * Form the scalar dot product of two Vec2's.
     * @param v second Vec2.
     * @return dot product of this and the second Vec2.
     */
    public double dot(Vec2 v) {
        return fX*v.fX + fY*v.fY;
    }
    
    public double getX() {
        return this.fX;
    }
    
    public double getY() {
        return this.fY;
    }
    
    /**
     * Calculate length of this Vec2.
     * @return sqrt(nX<sup>2</sup> + nY<sup>2</sup>)
     * @throws jjil.core.Error if sqrt does, due to coding error
     */
    public double length() throws Error {
        return Math.sqrt(fX * fX + fY * fY);
    }
      
    /**
     * Multiply a Vec2 by a scalar
     * @param n multiplicand
     * @return modified Vec2
     */
    public Vec2 times(double n) {
        this.fX *= n;
        this.fY *= n;
        return this;
    }
    
    /**
     * Implement toString
     * @return object name ( x, y)
     */
    public String toString() {
        return super.toString() + "(" + this.fX + "," + this.fY + ")";
    }
}
