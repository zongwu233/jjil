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

package jjil.algorithm;

import jjil.core.Error;
import jjil.core.Point;
import jjil.core.Triangle;
import jjil.core.Vec2;

/**
 * Class manages a mapping from one triangle to another (an affine warp).<p>
 * @author webb
 */
public class TriangleMap {
    Point p1, p2;
    Vec2 s12, s13, s22, s23;
    int l12, l13;
    
    /**
     * Create a new TriangleMap mapping points in one triangle into another.
     * @param t1 source triangle
     * @param t2 target triangle
     * @throws jjil.core.Error if some of the edges in t1 are of length zero
     */
    public TriangleMap(Triangle t1, Triangle t2) throws Error {
        this.p1 = t1.getP1();
        this.p2 = t2.getP1();
        this.s12 = new Vec2(this.p1, t1.getP2());
        this.s13 = new Vec2(this.p1, t1.getP3());
        this.s22 = new Vec2(this.p2, t2.getP2());
        this.s23 = new Vec2(this.p2, t2.getP3());
        // we precompute the length of the sides of the first triangle,
        // for use later in doing the affine transformation.
        // for accuracy we scale the square root by 8 bits (16 bits before
        // sqrt).
        this.l12 = this.s12.dot(this.s12);
        this.l13 = this.s13.dot(this.s13);
        if (this.l12 == 0 || this.l13 == 0 ) {
            throw new Error(
                            Error.PACKAGE.CORE,
                            jjil.core.ErrorCodes.ILLEGAL_PARAMETER_VALUE,
                            t1.toString(),
                            null,
                            null);
        }
    }
    
    /**
     * Map point in one triangle into the other triangle
     * @param p Point to map
     * @return mapped Point
     */
    public Point map(Point p) {
        Vec2 v = new Vec2(p1, p);
        // project along two sides of t1
        int nProj2 = v.dot(s12);
        int nProj3 = v.dot(s13);
        // map vector into coordinate system of t2
        // remember that l12 and l13 have been scaled by 8 bits
        // the computation is: normalize nProj2 and nProj3 then multiply by
        // s22 and s23
        Vec2 v2 = new Vec2(s22).times(nProj2).div(this.l12).
                add(new Vec2(s23).times(nProj3).div(this.l13));
        // finally offset by p2 to get the position in the other triangle
        return v2.add(p2);
    }
}
