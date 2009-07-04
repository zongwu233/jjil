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

import jjil.core.Point;
import jjil.core.Rect;
import jjil.j2se.algorithm.FloatVec;

// TODO: Auto-generated Javadoc
/**
 * Tesseract uses a coordinate system where left and right are reversed
 * relative to JJIL. The origin is the bottom left corner of the page,
 * x increasing to the right and y increasing upwards.
 * 
 * So the top left corner has minimum x coordinate
 * and maximum y coordinate.
 * 
 * @author webb
 */
public class JessRect extends Rect {
    
    /**
     * Instantiates a new jess rect.
     * 
     * @param nTlx the n tlx
     * @param nTly the n tly
     * @param nWidth the n width
     * @param nHeight the n height
     */
    public JessRect(int nTlx, int nTly, int nWidth, int nHeight) {
        super(nTlx, nTly, nWidth, nHeight);
    }
    
    /**
     * Create a new Rect specifying two corners.
     * 
     * @param p1 the first corner.
     * @param p2 the second corner.
     */
    public JessRect(Point p1, Point p2) {
        super(Math.min(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()),
                Math.max(p1.getX(), p2.getX()) - Math.min(p1.getX(), p2.getX()),
                Math.max(p2.getY(), p2.getY()) - Math.min(p1.getY(), p2.getY()));
    }
    
    /**
     * Create a new Rect (0 width and height) from a single point.
     * 
     * @param p the point.
     */
    public JessRect(Point p) {
        super(p.getX(), p.getY(), 0, 0);
    }
    
    /**
     * Add a new point to the Rect, extending it if necessary.
     * 
     * @param p the new Point
     * 
     * @return the jess rect
     */
    public JessRect add(Point p) {
        if (p.getX() < this.nTlx) {
            this.nWidth = getRight() - p.getX();
            this.nTlx = p.getX();
        } else if (p.getX() > getRight()) {
            this.nWidth = p.getX() - this.nTlx;
        }
        if (p.getY() > this.nTly) {
            this.nHeight = p.getY() - getBottom();
            this.nTly = p.getY();
        } else if (p.getY() < getBottom()) {
            this.nHeight = this.nTly - p.getY();
        }
        return this;
    }
    
    /**
     * Test a point for inclusion in a rectangle, including
     * boundaries.
     * 
     * @param p the point to test
     * 
     * @return true iff the point is in the rectangle
     */
    public boolean contains(Point p) {
    	return p.getX() >= this.nTlx && p.getX() <= this.nTlx + this.nWidth &&
    		p.getY() <= this.nTly && p.getY() >= this.nTly + this.nHeight;
    }
    
    /* (non-Javadoc)
     * @see jjil.core.Rect#getBottom()
     */
    public int getBottom() {
        return this.nTly - this.nHeight;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof JessRect)) {
            return false;
        }
        JessRect jr = (JessRect) o;
        return this.nTlx == jr.nTlx &&
                this.nTly == jr.nTly &&
                this.nHeight == jr.nHeight &&
                this.nWidth == jr.nWidth;
    }
    
    /**
     * Return the top (vertical) position of the rectangle.
     * 
     * @return the top (vertical) edge of the rectangle.
     */
    public int getTop() {
    	return this.nTly;
    }
        
    /**
     * Move.
     * 
     * @param fv the fv
     * 
     * @return the jess rect
     */
    public JessRect move(FloatVec fv) {
        this.nTlx = (int) Math.floor(this.nTlx + fv.getX());
        this.nTly = (int) Math.floor(this.nTly + fv.getY());
        this.nWidth = (int) Math.ceil(this.nWidth + fv.getX());
        this.nHeight = (int) Math.ceil(this.nHeight + fv.getY());
        return this;
    }
    
    /**
     * Scale.
     * 
     * @param f the f
     * 
     * @return the jess rect
     */
    public JessRect scale(float f) {
        this.nTlx = (int) Math.floor(f * this.nTlx);
        this.nTly = (int) Math.floor(f * this.nTly);
        this.nHeight = (int) Math.ceil(f * this.nHeight);
        this.nWidth = (int) Math.ceil(f * this.nWidth);
        return this;
    }
}
