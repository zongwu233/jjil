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
import jjil.core.Vec2;

/**
 * The Class EdgePt describes a single point on a boundary of a
 * character to be recognized. It includes the current position
 * and a vector to the next edge point.
 * 
 * @author webb
 */
class EdgePt {
    
    /** True if this edge point is "fixed", i.e., should be left alone during polygonal approximation. */
    boolean mbFixed = false;
    
    /** True if this edge point has been traversed. There are methods which work their way around the boundary of the character and which need a way to tell if they're repeating themselves. These set traversed to false for all edge points and then report. */
    boolean mbTraversed = false;
    
    /** The direction from this point to the next. The direction is a number from 0-8 with 0 = right and proceeding in clockwise order, i.e., 1 = down and right, 2 = down, etc. */
    byte mbtDir = 0;
    
    /** The run length. This is used to track the distance from this edge point to the next during polygonal approximation. */
    int mnRunLength = 0;
    
    /** The position of this point in pixels. */
    private Point mptPos = null;
    
    /** Vector to the next point in the boundary. */
    private Vec2 mvNext = null;
    
    /**
     * Instantiates a new edge point, assigning null values to
     * everything.
     */
    EdgePt() {
    }
    
    /**
     * Instantiates a new edge pt, assigning values to the point
     * position, vector to the next point, and setting the
     * direction, and run length to zero.
     * 
     * @param ptPos the point position
     * @param vNext the vector to the next point
     */
    EdgePt(Point ptPos, Vec2 vNext) {
        set(ptPos, vNext, (byte)0, 0);
    }
    
    /**
     * Instantiates a new edge point, assigning values to the
     * point position, vector to next point, direction, and
     * run length.
     * 
     * @param ptPos the point position
     * @param vNext the vector to the next point
     * @param btDir the direction (chain code) for this point
     * @param nRunLength the run length assigned to this point
     */
    EdgePt(Point ptPos, Vec2 vNext, byte btDir, int nRunLength) {
        set(ptPos, vNext, btDir, nRunLength);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof EdgePt)) {
            return false;
        }
        EdgePt ep = (EdgePt) o;
        return this.mptPos.equals(ep.mptPos) &&
                this.mvNext.equals(ep.mvNext);
    }
    
    /**
     * Gets the direction from this EdgePt to the next. This is used when
     * converting the EdgePts from a series of adjacent points into a polygonal
     * approximation. The direction is measured as 0 to the right, 1 down and right,
     * and so on clockwise, up to 8 up and right.
     * 
     * @return the direction
     */
    byte getDir() {
        return this.mbtDir;
    }
    
    /**
     * Gets whether this point should be considered to be "fixed" during the
     * polygonal approximation.
     * 
     * @return a bit indicating whether to treat this point as fixed, usually
     * because it is a significant corner in the approximation.
     */
    boolean getFixed() {
        return this.mbFixed;
    }
    
    /**
     * Gets the position of this EdgePt.
     * 
     * @return the posisiton of this EdgePt
     */
    Point getPos() {
        return this.mptPos;
    }
    
    /**
     * Gets the run length from this EdgePt to the next, along a straight line. Used
     * during polygonal approximation.
     * 
     * @return the run length from this EdgePt to the next
     */
    int getRunLength() {
        return this.mnRunLength;
    }

    /**
     * Gets the vector from this EdgePt to the next.
     * 
     * @return the vector from this EdgePt to the next
     */
    Vec2 getVec() {
        return this.mvNext;
    }
    
    /**
     * Checks if this EdgePt has been traversed during a scan through the EdgePts,
     * for example when extracting a completed polygonal approximation from a collection
     * of boundaries.
     * 
     * @return true, if this EdgePt has been traversed
     */
    boolean isTraversed() {
        return this.mbTraversed;
    }

    /**
     * Mark this EdgePt as non-fixed during polygonal approximation.
     */
    void resetFixed() {
        this.mbFixed = false;
    }
    
    /**
     * Reset traversed. Used when we are resetting all the traversed bits so we can
     * traverse EdgePts and extract them.
     */
    void resetTraversed() {
        this.mbTraversed = false;
    }
    
    /**
     * Sets the position, vector, direction, and run length of this EdgePt.
     * 
     * @param ptPos the position
     * @param vNext the vector to the next EdgePt
     * @param btDir the direction from this EdgePt to the next
     * @param nRunLength the run length from this EdgePt to the next
     */
    void set(Point ptPos, Vec2 vNext, byte btDir, int nRunLength) {
        this.mbFixed = false;
        this.mbTraversed = false;
    	this.mptPos = ptPos;
    	this.mvNext = vNext;
        this.mbtDir = btDir;
        this.mnRunLength = nRunLength;
   }

    /**
     * Mark this EdgePt as a fixed point during polygonal approximation.
     */
    void setFixed() {
        this.mbFixed = true;
    }
    
    /**
     * Mark this EdgePt as traversed as we are working our way around a sequence
     * of EdgePts.
     */
    void setTraversed() {
        this.mbTraversed = true;
    }

    /**
     * Sets the vector from this EdgePt to the next.
     * 
     * @param nextPt the new vector to the next EdgePt
     */
    void setVec(EdgePt nextPt) {
        this.mvNext = new Vec2(this.getPos(), nextPt.getPos());
    }
}
