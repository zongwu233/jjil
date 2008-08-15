/**
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
 *
 */

package jjil.algorithm.j2se;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Vector;

/**
 * RectCollection includes a data structure and algorithms to efficiently represent
 * a collection of axis-aligned rectangles and allow the fast (O(log n)) 
 * determination of whether a given point is in one of them.
 * @author webb
 */
public class RectCollection {
    
    /**
     * Vector of all rectangles in the collection.
     */
    private Vector<Rectangle> vAllRect = new Vector<Rectangle>();
    
    /**
     * treeHoriz is a collection of all the rectangles, projected horizontally.
     * Each node in the tree contains the rectangles at that y-coordinate.
     * There is one y-coordinate for each unique top or bottom position in a
     * rectangle.
     */
    private ThreadedBinaryTree<Double,Vector<Rectangle>> treeHoriz = null;
    /**
     * treeVert is a collection of all the rectangles, projected vertically.
     * Each node in the tree contains the rectangles at that x-coordinate.
     * There is one x-coordinate for each unique left and right position in a
     * rectangle
     */
    private ThreadedBinaryTree<Double,Vector<Rectangle>> treeVert = null;
    
    /**
     * Default constructor.
     */
    public RectCollection() {
    }
    
    /**
     * Add a new rectangle to the collection. This includes adding its top and
     * bottom coordinates to the horizontal projection collection and its
     * left and right coordinates to the vertical projection collection, and
     * adding the rectangle itself to every rectangle list between its starting
     * and ending coordinates.
     * @param r the rectangle to add
     */
    public void add(Rectangle r) {
        this.vAllRect.addElement(r);
        this.treeHoriz =
                addRect(r,
                r.getMinY(),
                r.getMinY() + r.getHeight(),
                this.treeHoriz);
        this.treeVert =
                addRect(r,
                r.getMinX(),
                r.getMinX() + r.getWidth(),
                this.treeVert);
    }
    
    /**
     * Add a rectangle to all rectangle lists between a starting and ending
     * coordinate. First we get pointers to the nodes in the trees for the
     * starting and ending coordinates. Then we add the rectangle to all the
     * lists in the inorder traversal from the start to the end node.
     * @param r the rectangle to add
     * @param start starting coordinate
     * @param end ending coordinate
     * @param tbtRoot the root of the ThreadedBinaryTree that we are modifying
     * @return the modified binary tree (= tbtRoot if it already exists, 
     * otherwise it will be created)
     */
    @SuppressWarnings({"unchecked"})
	private ThreadedBinaryTree<Double,Vector<Rectangle>> addRect(
            Rectangle r, 
            double start, 
            double end, 
            ThreadedBinaryTree<Double,Vector<Rectangle>> tbtRoot) {
        // get lists of Rectangles enclosing the given rectangle start and end
        ThreadedBinaryTree<Double,Vector<Rectangle>> tbtFind = null;
        if (tbtRoot != null) {
            tbtFind = tbtRoot.findNearest(new Double(start));
        }
        Vector<Rectangle> vExistingStart = null;
        if (tbtFind != null) {
            vExistingStart = tbtFind.getValue();
        }
        Vector<Rectangle> vExistingEnd = null;
        if (tbtRoot != null) {
            tbtFind = tbtRoot.findNearest(new Double(end));
        }
        if (tbtFind != null) {
            vExistingEnd = tbtFind.getValue();
        }
        // now add the new points to the tree
        ThreadedBinaryTree<Double,Vector<Rectangle>> tbtStart = null;
        // the ThreadedBinaryTree may not already exist. Create it if necessary
        if (tbtRoot == null) {
            tbtRoot = tbtStart = 
                    new ThreadedBinaryTree<Double,Vector<Rectangle>>(new Double(start));
        } else {
            // already exists, add or get the start node
            tbtStart = tbtRoot.add(new Double(start));
            // add existing enclosing rectangles to this node's list
            // if it doesn't already exist
            if (vExistingStart != null) {
                if (tbtStart.getValue() == null) {
                    tbtStart.setValue((Vector<Rectangle>) vExistingStart.clone());
                }
            }
        }
        // add or get the end node
        ThreadedBinaryTree<Double,Vector<Rectangle>> tbtEnd = 
                tbtRoot.add(new Double(end));
        // add existing enclosing rectangles to this node's list
        // if it doesn't already exist
        if (vExistingEnd != null) {
            if (tbtEnd.getValue() == null) {
                tbtEnd.setValue((Vector<Rectangle>) vExistingEnd.clone());
            }
        }
        // now traverse the path from tbtStart to tbtEnd and add r to all 
        // rectangle lists
        Vector<ThreadedBinaryTree<Double,Vector<Rectangle>>> vTrav = 
                tbtStart.inorderTraverse(tbtEnd);
        // for eacn node in the inorder traversal, create the Vector of 
        // rectangles if necessary, and put this rectangle on it
        for (Enumeration<ThreadedBinaryTree<Double,Vector<Rectangle>>> e = 
                vTrav.elements(); e.hasMoreElements();) {
            ThreadedBinaryTree<Double,Vector<Rectangle>> tbt = 
                    (ThreadedBinaryTree<Double,Vector<Rectangle>>) e.nextElement();
            // Vector doesn't exist
            if (tbt.getValue() == null) {
                Vector<Rectangle> v = new Vector<Rectangle>();
                v.addElement(r);
                // set value
                tbt.setValue(v);
            } else {
                // already exists, modify value
                tbt.getValue().addElement(r);
            }
        }
        // return modified / created tree
        return tbtRoot;
    }
    
    /**
     * Clear collection.
     */
    public void clear() {
    	this.vAllRect.clear();
    	this.treeHoriz = null;
    	this.treeVert = null;
    }
    
    /**
     * Determines whether a point is in the collection by intersecting the lists
     * of rectangles that the point projects into horizontally and vertically,
     * and then determining if the point lies in one of the rectangles in the
     * intersection.
     * @param p the point to test
     * @return a Rectangle that contains p if it is contained by any Rectangle, otherwise
     * null
     */
    public Rectangle contains(Point p) {
        // if no rectangles are in collection answer is null
        if (this.treeHoriz == null || this.treeVert == null) {
            return null;
        }
        ThreadedBinaryTree<Double,Vector<Rectangle>> tbtHorizProj =
                this.treeHoriz.findNearest(new Double(p.getY()));
        ThreadedBinaryTree<Double,Vector<Rectangle>> tbtVertProj =
                this.treeVert.findNearest(new Double(p.getX()));
        // if no tree node is <= this point the return null
        if (tbtHorizProj == null || tbtVertProj == null) {
            return null;
        }
        // check for intersection between two lists; if non-empty check
        // for contains
        Vector<Rectangle> vHoriz = tbtHorizProj.getValue();
        Vector<Rectangle> vVert = tbtVertProj.getValue();
        for (Enumeration<Rectangle> e = vHoriz.elements(); e.hasMoreElements();) {
            Rectangle r = e.nextElement();
            for (Enumeration<Rectangle> f = vVert.elements(); f.hasMoreElements();) {
                Rectangle s = f.nextElement();
                if (r == s) {
                    if (s.contains(p)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Returns an enumeration of all rectangles in the collection
     * @return Enumeration that gives all rectangles in the collection, in
     * the order they were added.
     */
    public Enumeration<Rectangle> elements() {
        return this.vAllRect.elements();
    }
    
    /**
     * Implement toString
     */
    public String toString() {
    	String szRes = super.toString() + "(all=" + this.vAllRect.toString() + 
    		",horiz=";
    	if (this.treeHoriz != null) {
    		szRes += this.treeHoriz.toString();
    	} else {
    		szRes += "null";
    	}
    	szRes += ",vert=";;
    	if (this.treeVert != null) {
    		szRes += this.treeVert.toString();
    	} else {
    		szRes += "null";
    	}
    	szRes += ")";
    	return szRes;
    }
}
