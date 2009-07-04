/*
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

import jjil.core.Point;
import jjil.core.TimeTracker;
import jjil.core.Vec2;
import jjil.j2se.algorithm.CircularList;
import jjil.j2se.algorithm.Pair;

/**
 * The Class EdgePts manages a list of EdgePt objects which represent
 * a boundary (internal or external) of a character to be recognized.
 * This is a circular list, with each EdgePt including information that
 * refers to the next, including at the end of the list. This class includes
 * methods for polygonal approximation of the EdgePt objects, thereby reducing
 * the number of EdgePt objects in the list.
 * 
 * @author webb
 */
public class EdgePts extends CircularList<EdgePt> {
    
    /** The Constant FIXED_DIST. Controls the polygonal approximation. */
    private final static int FIXED_DIST = 20;

    /* Controls the recursion step in the Douglas-Peucker polygonal approximation */
    /** The Constant PAR1. */
    private final static int PAR1 = 50;

    /** The EdgePt objects are allocated from a static list to avoid garbage collection in Android, which slows things down quite a bit. This is the maximum number of EdgePt objects that can be used. It must exceed the need for the polygonal approximation etc. to work. */
	private static final int MAX_EDGEPTS = 10000;
	
	/** The array of EdgePt objects from which the actual EdgePt's in use are taken. */
	private static EdgePt srEdgePtStore[] = new EdgePt[MAX_EDGEPTS];
	
	/** The number of EdgePt objects in use at any one time. */
	private static int snEdgePtsUsed = 0;
	
    /** The rectangular bounding box enclosing this collection of EdgePt's. */
    private JessRect rectBounds = null;
    
    /** A time tracker object, used to measure the execution time of expensive operations. */
    private TimeTracker mTimeTracker;
    
    /** Used for vector calculation, so as to avoid creating a new vector *. */
    private Vec2 mvSum = new Vec2(0,0);
    
    /** Used to hold lists of EdgePt objects while splitting */
    
    /* Create an initialize the EdgePt store */
    static {
    	for (int i=0; i<srEdgePtStore.length; i++) {
    		srEdgePtStore[i] = new EdgePt();
    	}
    }
    
    /**
     * The Class SortableCircListIter is used when dividing an EdgePt list into
     * two parts when it is suspected that the list includes two adjacent characters
     * which touch. The edges that cross the split point have to be sorted from top
     * to bottom so we can connect them correctly and not create any figure-8 type
     * loops (i.e., the inside connected to the outside). This is done by adding
     * all edges that cross the split point to an array as members of this class,
     * sorting them, then connecting them from bottom to top. This is guaranteed
     * not to connect inside to outside.
     */
    private class SortableCircListIter implements Comparable<SortableCircListIter> {
        
        /** The mn y. */
        Integer mnY;
        
        /** The mcli. */
        CircularList<EdgePt>.CircListIter mcli;
        
        /**
         * Instantiates a new sortable circular list iterator.
         * 
         * @param nY the y position associated with this iterator
         * @param cli the circular list iterator
         */
        SortableCircListIter(int nY, CircularList<EdgePt>.CircListIter cli) {
            this.mnY = nY;
            this.mcli = cli;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(SortableCircListIter o) {
            return this.mnY.compareTo(o.mnY);
        }
        
        /**
         * Gets the circular list iterator.
         * 
         * @return the circular list iterator
         */
        CircularList<EdgePt>.CircListIter getCircListIter() {
            return this.mcli;
        }
        
        /**
         * Gets the y position associated with this iterator.
         * 
         * @return the y position of this iterator
         */
        int getY() {
            return this.mnY;
        }
    }

    /**
     * Instantiates a new edge pts class.
     * 
     * @param tt the TimeTracker object used to track expensive operations.
     * May be null if no tracking is needed.
     */
    EdgePts(TimeTracker tt) {
    	this.mTimeTracker = tt;
    }

    /**
     * Instantiates a new edge pts, initializing it from a list of Point objects.
     * This is the normal way an EdgePts object is first created.
     * 
     * @param lpt the list of points that will make up the EdgePts object
     * @param tt the TimeTracker object used to track expensive operations. May be
     * null if tracking is not required.
     */
    EdgePts(CircularList<Point> lpt, TimeTracker tt) {
    	this.mTimeTracker = tt;
        CircularList<Point>.CircListIter cli = lpt.circListIterator();
        Point ptPrev = null;
        Vec2 vPrev = null;
        int nRunLength = 0;
        /* for each point in the list */
        while (cli.hasNext() && snEdgePtsUsed < MAX_EDGEPTS-1) {
            Point pt = cli.next();
            Point ptNext = cli.getNext();
            /* get vector to next point */
            Vec2 v = pt.diff(ptNext);
            /* check for start of list */
            if (vPrev == null) {
            	/* initialize bounding box */
                this.rectBounds = new JessRect(pt);
                /* initialize previous point, run length, and previous vector */
                ptPrev = pt;
                vPrev = v;
                nRunLength = 1;
            } else {
            	/* extend bounding box */
                this.rectBounds.add(pt);
                /* check to see if we are continuing in the same direction */
                if (vPrev.equals(v)) {
                    nRunLength ++;
                } else {
                	/* new direction. Record chain code of previous direction
                	 * and create EdgePt
                	 */
                    byte bDir = getChainCode(vPrev);
                    EdgePt ep = srEdgePtStore[snEdgePtsUsed++];
                    ep.set(ptPrev, vPrev.times(nRunLength), bDir, nRunLength);
                    this.add(ep);
                    /* start new direction */
                    ptPrev = pt;
                    vPrev = v;
                    nRunLength = 1;
                }
            }
        }
        /* close circular list */
        byte bDir = getChainCode(vPrev);
        EdgePt ep = new EdgePt(ptPrev, vPrev.times(nRunLength), bDir,
                nRunLength);
        this.add(ep);
    }

    /**
     * Instantiates a new EdgePts object from an existing list of EdgePt objects.
     * Note that this code does not do runlength tracking or measure the chain
     * code. This is because this code is used when we are creating an EdgePts
     * object from an object which has already been subjected to polygonal
     * approximation, i.e., when we are splitting an EdgePts object into two parts.
     * 
     * @param leps the list of EdgePt objects
     */
    EdgePts(List<EdgePt> leps) {
        EdgePt firstEdgePt = null;
        EdgePt prevEdgePt = null;
        for (EdgePt edgept : leps) {
            EdgePt ep = edgept;
            /* check for start of list */
            if (firstEdgePt == null) {
                firstEdgePt = ep;
            }
            /* initialize bounding box */
            if (this.rectBounds == null) {
                this.rectBounds = new JessRect(ep.getPos());
            } else {
                this.rectBounds.add(ep.getPos());
            }
            /* set vector from previous point to this one */
            if (prevEdgePt != null) {
                prevEdgePt.setVec(ep);
            }
            prevEdgePt = ep;
            this.add(ep);
        }
        // close circular list
        if (firstEdgePt != prevEdgePt) {
            prevEdgePt.setVec(firstEdgePt);
        }
    }
    
    /**
     * Count the number of fixed points in the EdgePts object. This is used
     * to determine when we have enough corners to consider a polygonal
     * approxmation done.
     * 
     * @return the number of fixed points in the EdgePts object
     */
    private int countFixed() {
        int nCount = 0;
        for (EdgePt ep: this) {
            if (ep.getFixed()) {
                nCount ++;
            }
        }
        return nCount;
    }
    
    /**
     * splitDP implements the Douglas-Peucker algorithm to
     * recursively break a sequence of edge points into polygonal
     * segments by finding the point of maximum departure from a line linking
     * the first and last points and marking that point as fixed, then
     * applying splitDP to the sequence before and after that point,
     * until no point is more than a fixed distance from the line connecting
     * the beginning and end of the sequence.
     * 
     * @param cliStart the starting point
     * @param cliEnd the ending point
     * 
     * @return the number of new fixed points added
     */
    private int splitDP(CircListIter cliStart, CircListIter cliEnd) {
        // check for points only 1 point apart
        if (cliStart.getNext() == cliEnd.getPrevious()) {
            return 0;
        }
        // vecLine is the direction from the ending point to
        // the starting point
         Vec2 vecLine = cliEnd.getNext().getPos().diff(cliStart.getNext().getPos());
        // check for starting and ending points the same
        if (vecLine.getX() == 0 && vecLine.getY() == 0) {
        	// measure from curve direction at start
            vecLine = cliStart.getPrevious().getVec();
        }
        // maxPerp is the max distance from the line to the current point
        int maxPerp = Integer.MIN_VALUE;
        CircListIter cli = cliStart.clone();
        cli.next();
        CircListIter cliMaxPoint = cli;
        this.mvSum.setXY(0, 0);
        // for each point from start to end
        while (cli.getNext() != cliEnd.getNext()) {
        	// get the point
            this.mvSum.add(cli.next().getVec());
            // perp is the magnitude of the cross product
            // of vec (current position) x vecLine (line)
            // which is the distance of vec to the line
            // (times the length of vecLine which doesn't change)
            int perp = this.mvSum.crossMag(vecLine);
            perp *= perp;
            // if we're further from the line than we've
            // been before
            if (perp > maxPerp) {
                maxPerp = perp;
                cliMaxPoint = cli.clone();
                cliMaxPoint.previous();
            }
        };
        int vLen = 0;
        try {
        	vLen = vecLine.length();
        } catch (jjil.core.Error er) {
        	
        }
        // check to see if we should split again
        // remember maxPerp = | vLen | * (max perpendicular distance to vecLine)
        if (maxPerp >= PAR1 * vLen) {
	        cliMaxPoint.getNext().setFixed();
            return 1 + splitDP(cliStart, cliMaxPoint) + splitDP(cliMaxPoint, cliEnd);
        } else {
        	return 0;
        }
    };
    

    /**
     * Fix2.
     * 
     * @param area the area
     */
    void fix2(int area) {
        CircularList<EdgePt>.CircListIter cliSearch;
        EdgePt ep = null;
        for (cliSearch = this.circListIterator(); cliSearch.hasNext();) {
            EdgePt prev = cliSearch.getPrevious();
            ep = cliSearch.next();
            EdgePt next = cliSearch.getNext();
            int dir1;
            if (!((((ep.getDir() - prev.getDir() + 1) & 7) < 3 && 
                    (dir1 = (prev.getDir() - next.getDir()) & 7) != 2 && dir1 != 6))) {
                break;
            }
        }

        boolean stopped = false;                   /*not finished yet */
        ep.setFixed(); /*fix it */
        CircularList<EdgePt>.CircListIter cli = new CircListIter(cliSearch);
        do {
            EdgePt linestartPrevPrev = cli.getPrevX3();
            EdgePt linestartPrev = cli.getPrevPrev();
            EdgePt linestart = ep;          /*possible start of line */
            EdgePt linestartNext = cli.getNext();
            int dir1 = ep.getDir();   /*first direction */
            /*length of dir1 */
            int sum1 = ep.getRunLength();
            EdgePt prev = ep;
            ep = cli.next();
            int dir2 = ep.getDir();   /*2nd direction */
            /*length in dir2 */
            int sum2 = ep.getRunLength();
            if (((dir1 - dir2 + 1) & 7) < 3) {
                while (prev.getDir() == cli.getNext().getDir()) {
                    prev = ep;
                    ep = cli.next();   /*look at next */
                    if (ep.getDir() == dir1) /*sum lengths */ {
                        sum1 += ep.getRunLength();
                    } else {
                        sum2 += ep.getRunLength();
                    }
                }

                if (!cli.hasNext()) {
                    stopped = true;             /*finished */

                }
                if (sum2 + sum1 > 2 && linestartPrev.getDir() == dir2 && 
                        (linestartPrev.getRunLength() >
                        linestart.getRunLength() || sum2 > sum1)) {
                    /*start is back one */
                    linestartNext = linestart;
                    linestart = linestartPrev;
                    linestartPrev = linestartPrevPrev;
                    linestart.setFixed();
                }

                if (((cli.getNext().getDir() - ep.getDir() + 1) & 7) >= 3 || 
                        (ep.getDir() == dir1 && sum1 >= sum2) || 
                        ((prev.getRunLength() < ep.getRunLength() || 
                        (ep.getDir() == dir2 && sum2 >= sum1)) && 
                        linestartNext != ep)) {
                    ep = cli.next();
                }
            }
            /*sharp bend */
            ep.setFixed();
        } while (cli.hasNext() && !stopped);
        /*do whole loop */
        cli = this.circListIterator();
        do {
            ep = cli.next();
            if (((ep.getRunLength() >= 8) &&
                    (ep.getDir() != 2) && (ep.getDir() != 6)) ||
                    ((ep.getRunLength() >= 8) &&
                    ((ep.getDir() == 2) || (ep.getDir() == 6)))) {
                ep.setFixed();
                EdgePt next = cli.getNext();
                next.setFixed();
            }
        } while (cli.hasNext());

        cli = this.circListIterator();
        do {
            EdgePt prevprev = cli.getPrevPrev();
            EdgePt prev = cli.getPrevious();
            ep = cli.next();
            EdgePt next = cli.getNext();
            EdgePt nextnext = cli.getNextNext();
            /*single fixed step */
            if (ep.getFixed() &&
                    ep.getRunLength() == 1 /*and neighours free */ && (next.getFixed() &&
                    !prev.getFixed()) /*same pair of dirs */ && (!cli.getNextNext().getFixed() &&
                    prev.getDir() == next.getDir() &&
                    prevprev.getDir() == nextnext.getDir()) && ((prev.getDir() - ep.getDir() + 1) & 7) < 3) {
                /*unfix it */
                ep.resetFixed();
                next.resetFixed();
            }
        } while (cli.hasNext());       /*until finished */

        stopped = false;
        if (area < 450) {
            area = 450;
        }
        int gapmin = area * FIXED_DIST * FIXED_DIST / 44000;

        cli = this.circListIterator();
        int fixed_count = 0;
        do {
            ep = cli.next();
            if (ep.getFixed()) {
                fixed_count++;
            }
        } while (cli.hasNext());
        
        cli = this.circListIterator();
        
        do {
            ep = cli.next();
        } while (!ep.getFixed());

        EdgePt edgefix0 = ep;

        do {
            ep = cli.next();
        } while (!ep.getFixed());

        EdgePt edgefix1 = ep;
        do {
            ep = cli.next();
        } while (!ep.getFixed());

        EdgePt edgefix2 = ep;

        do {
            ep = cli.next();
        } while (!ep.getFixed());

        EdgePt edgefix3 = ep;

        EdgePt startfix = edgefix2;

        do {
            if (fixed_count <= 3) {
                break;                     //already too few

            }
            Vec2 d12vec = edgefix1.getPos().diff(edgefix2.getPos());
            int d12 = d12vec.lengthSqr();
            if (d12 <= gapmin) {
                Vec2 d01vec = edgefix0.getPos().diff(edgefix1.getPos());
                int d01 = d01vec.lengthSqr();
                Vec2 d23vec = edgefix2.getPos().diff(edgefix3.getPos());
                int d23 = d23vec.lengthSqr();
                if (d01 > d23) {
                    edgefix2.resetFixed();
                    fixed_count--;
                } else {
                    edgefix1.resetFixed();
                    fixed_count--;
                    edgefix1 = edgefix2;
                }
            } else {
                edgefix0 = edgefix1;
                edgefix1 = edgefix2;
            }
            edgefix2 = edgefix3;
            ep = cli.next();
            while (!ep.getFixed()) {
                if (ep == startfix) {
                    stopped = true;
                }
                ep = cli.next();
            }
            edgefix3 = ep;
        } while ((edgefix2 != startfix) && (!stopped));

    }

    /** sbLookup translates the offset to the next point to the chain code. The offset is represented as ((x+1)*3+(y+1)) where -1<=x<=1 and -1<=y<=1 are the differences between the current point and the next point. So the chain codes and offsets around the current point are  y         offset                    chain code ^         6  7  8                   5  6  7 |         3  4  5                   4 -1  0  (-1 is unused) .->x      0  1  2                   3  2  1 */
    static byte sbLookup[] = new byte[]{
        3, 2, 1, 4, -1, 0, 5, 6, 7
    };
    
    /**
     * Gets the chain code.
     * 
     * @param v the vector to the next point:
     * (x,y) where -1<=x<=1 and -1<=y<=1, (x,y) != (0,0)
     * 
     * @return the chain code -- byte 0-8, clockwise around
     * central point, starting at 0 to right
     */
    private byte getChainCode(Vec2 v) {
        assert Math.abs(v.getX()) <= 1 &&
                Math.abs(v.getY()) <= 1;
        int nIndex = (v.getX()+1)*3 + (v.getY()+1);
        return sbLookup[nIndex];
    }
    
    /**
     * Gets the EdgePt objects which have not yet been traversed, starting
     * at the given iterator. The purpose of this is to pull out closed loops
     * of EdgePt objects from an existing EdgePts object when splitting.
     * 
     * @param lcli the iterator marking the starting point
     * 
     * @return the edge pts, up to the first traversed point. EdgePts is modified
     * by marking points as traversed as we add them to the resulting list.
     */
    private List<EdgePts> getEdgePts(List<EdgePts.CircListIter> lcli) {
        List<EdgePts> lcp = new ArrayList<EdgePts>();
        for (CircularList<EdgePt>.CircListIter cli: lcli) {
            EdgePt ep = cli.getNext();
            if (!ep.isTraversed()) {
                EdgePts eps = new EdgePts(this.mTimeTracker);
                while (cli.hasNext() && !cli.getNext().isTraversed()) {
                    ep = cli.next();
                    ep.setTraversed();
                    eps.add(ep);
                }
                lcp.add(eps);
            } else {
                break;
            }
        }
        return lcp;
    }
    
    /**
     * We have a situation like this
     * <code>
     * cliPrev.getNext() -> cliPrev.getNextNext()
     * 
     * cliNext.getNextNext() <= cliNext.getNext()
     * we exchange iterators and add points so we get this
     * cliPrev --------> ptPrev      |---> cliPrev.getNext()
     * |         |
     * cliNext.getNext <---|         |<--- cliNext
     * </code>
     * This is for splitting characters which are joined together.
     * 
     * @param cliPrev "top" iterator
     * @param cliNext "bottom" iterator
     * @param ptPrev intermediate point for top
     * @param ptNext intermediate point for bottom
     */
    private void insertAndSplit(CircListIter cliPrev, CircListIter cliNext,
            Point ptPrev, Point ptNext) {
        cliPrev.exchange(cliNext);
        EdgePt ep = new EdgePt(ptPrev, cliNext.getNext().getPos().diff(ptPrev));
        cliPrev.getNext().setVec(ep);
        this.add(cliPrev, ep);
        ep = new EdgePt(ptNext, cliPrev.getNext().getPos().diff(ptNext));
        cliNext.getNext().setVec(ep);
        this.add(cliNext, ep);
    }

    /**
     * Poly2.
     */
    void poly2() {
        boolean bFound = false;
        CircListIter cli = this.circListIterator();
        while (cli.hasNext()) {
            if (cli.getNext().getFixed() &&
                    !cli.getNextNext().getFixed()) {
                bFound = true;
                break;
            }
            cli.next();
        }
        if (!bFound) {
            cli = this.circListIterator();
            if (!cli.getNext().getFixed()) {
                cli.getNext().setFixed();
                bFound = true;
            }
        }
        if (bFound) {
            CircListIter loopstart = cli.clone();
            int nFixed = countFixed();
            while (nFixed < 3) {
                cli = new CircListIter(loopstart);
                do {
                    CircListIter linestart = cli.clone();
                    do {
                        cli.next();
                    } while (!cli.getNext().getFixed() &&
                            cli.hasNext());
                    nFixed += splitDP(linestart, cli);

                    while (cli.getNextNext().getFixed() &&
                            cli.hasNext()) {  /*look for next non-fixed */
                        cli.next();
                    }
                } while (cli.hasNext());
            };
            cli = this.circListIterator();
            // remove all the non-fixed points from the loop
            do {
                if (!cli.next().getFixed()) {
                    cli.remove();
                    cli.getPrevious().setVec(cli.getNext());
                };
            } while (cli.hasNext());
        }
    }
    
    /**
     * Reset traversed.
     */
    void resetTraversed() {
        for (EdgePt ep: this) {
            ep.resetTraversed();
        }
    }
    
    /**
     * Split.
     * 
     * @param nX the n x
     * 
     * @return the pair< list< edge pts>, list< edge pts>>
     */
    Pair<List<EdgePts>, List<EdgePts>> split(int nX) {
    	if (this.mTimeTracker != null) {
    		this.mTimeTracker.startTask("EdgePts.split");
    	}
        ArrayList<Pair<SortableCircListIter,SortableCircListIter>> sep = 
                new ArrayList<Pair<SortableCircListIter,SortableCircListIter>>();
        boolean mbLeft = true;
        SortableCircListIter scliLast = null;
        for (CircularList<EdgePt>.CircListIter cli = this.circListIterator();
            cli.hasNext();) {
            EdgePt epThis = cli.getNext();
            EdgePt epNext = cli.getNextNext();
            // see if this edge crosses the split point
            Point ptThis = epThis.getPos();
            Point ptNext = epNext.getPos();
            if (ptThis.getX() >= nX) {
                mbLeft = false;
            }
            if ((ptThis.getX() < nX) != (ptNext.getX() < nX)) {
                int nY = ptThis.getY() + (ptNext.getY() - ptThis.getY()) * 
                        (nX - ptThis.getX()) / (ptNext.getX() - ptThis.getX());
                SortableCircListIter scliThis = 
                        new SortableCircListIter(nY, cli.clone());
                if (scliLast == null) {
                    scliLast = scliThis;
                } else {
                    sep.add(new Pair<SortableCircListIter,SortableCircListIter>
                    	(scliLast, scliThis));
                    scliLast = null;
                }
            }
            cli.next();
        }
        assert scliLast == null;
        if (sep.size() == 0) {
            ArrayList<EdgePts> al = new ArrayList<EdgePts>();
            al.add(this);
            if (mbLeft) {
                return new Pair<List<EdgePts>, List<EdgePts>>(al, null);
            } else {
                return new Pair<List<EdgePts>, List<EdgePts>>(null, al);
            }
        }
        this.resetTraversed();
        ArrayList<CircularList<EdgePt>.CircListIter> alcliLeft =
                new ArrayList<CircularList<EdgePt>.CircListIter>(),
                alcliRight = new ArrayList<CircularList<EdgePt>.CircListIter>();
        for (Pair<SortableCircListIter,SortableCircListIter> prscli:
                sep) {
            SortableCircListIter sepTop = prscli.getFirst();
            SortableCircListIter sepBottom = prscli.getSecond();
            EdgePts.CircListIter cliTop = sepTop.getCircListIter();
            EdgePts.CircListIter cliBottom = sepBottom.getCircListIter();
            if (cliTop.getNext().getPos().getX() < cliBottom.getNext().getPos().getX()) {
                Point ptTop = new Point(nX-1, sepTop.getY());
                Point ptBottom = new Point(nX, sepBottom.getY());
                insertAndSplit(cliTop, cliBottom, ptTop, ptBottom);
                alcliLeft.add(new CircListIter(cliTop));
                alcliRight.add(new CircListIter(cliBottom));                
            } else {
                Point ptTop = new Point(nX, sepTop.getY());
                Point ptBottom = new Point(nX-1, sepBottom.getY());
                insertAndSplit(cliTop, cliBottom, ptTop, ptBottom);
                alcliRight.add(new CircListIter(cliTop));
                alcliLeft.add(new CircListIter(cliBottom));                
                
            }
        }
        List<EdgePts> lcpLeft = getEdgePts(alcliLeft),
                lcpRight = getEdgePts(alcliRight);
        assert lcpLeft.size() > 0 && lcpRight.size() > 0;
    	if (this.mTimeTracker != null) {
    		this.mTimeTracker.endTask("EdgePts.split");
    	}
        return new Pair<List<EdgePts>,List<EdgePts>>(lcpLeft, lcpRight);
    }
    
    /**
     * Reset.
     */
    public static void reset() {
    	snEdgePtsUsed = 0;
    }
}
