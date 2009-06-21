/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jjil.j2se.ocr;
// TODO: Auto-generated Javadoc
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
/**
 * The Class Feature. A feature is a particular (x,y) point at which an
 * angle with a particular value has been measured. These are the basic
 * object matched to recognize characters.
 */
public class Feature {
    
    /** The x position of the feature. */
    short msX = 0;
    
    /** The y position of the feature. */
    short msY = 0;
    
    /** The feature angle. */
    short msTheta = 0;
    
    /**
     * Instantiates a new feature.
     */
    public Feature() {
    	
    }

    /**
     * Instantiates a new feature.
     * 
     * @param X the x position of the feature
     * @param Y the y position of the feature
     * @param Theta the theta (angle) of the feature
     */
    public Feature(short X, short Y, short Theta) {
      set(X, Y, Theta);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof Feature)) {
            return false;
        }
        Feature nf = (Feature) o;
        return Math.abs(this.msX - nf.msX) <= 3 &&
                this.msY == nf.msY &&
                this.msTheta == nf.msTheta;
    }
    
    /**
     * Gets the theta (angle) of the feature
     * 
     * @return the theta (angle) of the feature.
     */
    public short getTheta() {
        return this.msTheta;
    }

    /**
     * Gets the x position of the feature.
     * 
     * @return the x (horizontal) position of the feature
     */
    public short getX() {
        return this.msX;
    }

    /**
     * Gets the y position of the feature.
     * 
     * @return the y (vertical) position of the feature
     */
    public short getY() {
        return this.msY;
    }
    
    /**
     * Sets the (x,y,theta) values for the feature.
     * 
     * @param X the x (horizontal) position of the feature
     * @param Y the y (vertical) position of the feature
     * @param Theta the theta (angle) of the feature
     */
    public void set(short X, short Y, short Theta) {
        this.msX = (byte) Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, X));
        this.msY = (byte) Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, Y));
        this.msTheta = Theta;   	
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Theta = " + this.msTheta + ", X = " + this.msX +
                ", Y = " + this.msY;
    }
}
