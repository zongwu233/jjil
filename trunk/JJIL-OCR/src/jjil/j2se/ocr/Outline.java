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

import java.util.ArrayList;
import java.util.List;

import jjil.core.MathPlus;
import jjil.core.Point;
import jjil.core.TimeTracker;
import jjil.core.Vec2;
import jjil.j2se.algorithm.CircularList;
import jjil.j2se.algorithm.CircularList.CircListIter;

// TODO: Auto-generated Javadoc
/**
 * The Class Outline.
 * 
 * @author webb
 */
public class Outline extends ArrayList<EdgePts> {
	
	/** The Constant MIN_BOUNDARY_SIZE. */
	private static final int MIN_BOUNDARY_SIZE = 10;
	
    /** The mn offset y. */
    int mnScale, mnOffsetX, mnOffsetY;
    
    /** The m time tracker. */
    private TimeTracker mTimeTracker;
    
    /**
     * Instantiates a new outline.
     * 
     * @param lcp the lcp
     * @param tt the tt
     */
    public Outline(List<CircularList<Point>> lcp, TimeTracker tt) {
    	this.mTimeTracker = tt;
        assert lcp != null;
        scaleAndCenter(lcp);
        if (lcp.size() > 0) {
            JessRect jrBox = null;
            for (CircListIter cli = lcp.get(0).circListIterator(); cli.hasNext();) {
                Point pt = (Point) cli.next();
                if (jrBox == null) {
                    jrBox = new JessRect(pt);
                } else {
                    jrBox.add(pt);
                }
            }
            for (CircularList<Point> cp: lcp) {
            	if (cp.size() > MIN_BOUNDARY_SIZE) {
            		setPoints(cp, jrBox);
            	}
            }
        } 
    }
    
    /**
     * Approximate.
     * 
     * @param eps the eps
     * @param jrBox the jr box
     */
    private void approximate(EdgePts eps, JessRect jrBox) {
        int area = MathPlus.square(jrBox.getHeight());
        eps.fix2(area);
        eps.poly2();
        if (eps.size() <= 2) {
            return;
        }
        EdgePts polypts = new EdgePts(this.mTimeTracker);
        for (CircularList<EdgePt>.CircListIter cli = eps.circListIterator(); cli.hasNext();) {
            Point p = cli.next().getPos();
            Point pos = new Point((p.getX()*this.mnScale+this.mnOffsetX)/256,
                    (p.getY()*this.mnScale+this.mnOffsetY)/256);
            Point ptNext = cli.getNext().getPos();
            Point posNext = new Point((ptNext.getX()*this.mnScale+this.mnOffsetX)/256,
                    (ptNext.getY()*this.mnScale+this.mnOffsetY)/256);
            Point pt = new Point((int) pos.getX(), (int) pos.getY());
            Vec2 vec = new Vec2((int) (posNext.getX() - pos.getX()),
                    (int) (posNext.getY() - pos.getY()));
            polypts.add(new EdgePt(pt,vec));
        }
        this.add(polypts);
        
    }
    
    /**
     * Sets the points.
     * 
     * @param lp the lp
     * @param jrBox the jr box
     */
    private void setPoints(CircularList<Point> lp, JessRect jrBox) {
        CircularList<Point> clNew = new CircularList<Point>();
        for (CircularList<Point>.CircListIter cli = lp.circListIterator(); cli.hasPrevious();) {
            Point pt = cli.previous();
            Point ptNew = new Point(pt.getX(), jrBox.getTop()-pt.getY());
            clNew.add(ptNew);
        }  
        EdgePts eps = new EdgePts(clNew, this.mTimeTracker);
        approximate(eps, jrBox);
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractList#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Outline)) {
            return false;
        }
        return true;
    }
    
    /**
     * Scale and center.
     * 
     * @param lcp the lcp
     */
    private void scaleAndCenter(List<CircularList<Point>> lcp) {
        // first determine min and max vertically
        int nMinY = Integer.MAX_VALUE, nMaxY = Integer.MIN_VALUE;
        for (CircularList<Point> clp: lcp) {
            for (Point pt: clp) {
                nMinY = Math.min(nMinY, pt.getX());
                nMaxY = Math.max(nMaxY, pt.getY());
            }
        }
        // compute scale factor and offset so nMinY->64, nMaxY->256
        this.mnScale = (256 * (246-64)) / (nMaxY-nMinY);
        this.mnOffsetY = 256 * 64 - nMinY * this.mnScale;
        // now compute X range after scale
        int nMinX = Integer.MAX_VALUE, nMaxX = Integer.MIN_VALUE;
        for (CircularList<Point> clp: lcp) {
            for (Point pt: clp) {
                nMinX = Math.min(nMinX, pt.getX()*this.mnScale);
                nMaxX = Math.max(nMaxX, pt.getX()*this.mnScale);
            }
        }
        // compute offset to center X
        this.mnOffsetX = - (nMaxX - nMinX) / 2;
    }
    
}
