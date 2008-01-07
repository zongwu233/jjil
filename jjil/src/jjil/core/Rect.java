/*
 * Rect.java
 *
 * Created on December 10, 2007, 1:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jjil.core;

/**
 *
 * @author webb
 */
public class Rect {
    int nTlx = 0, nTly = 0, nWidth = 0, nHeight = 0;
    
    /** Creates a new instance of Rect */
    public Rect() {
    }
    
    public Rect(int nTlx, int nTly, int nWidth, int nHeight) {
        this.nTlx = nTlx;
        this.nTly = nTly;
        this.nWidth = nWidth;
        this.nHeight = nHeight;
    }
    
    public Rect(Point p1, Point p2) {
        this.nTlx = Math.min(p1.wX, p2.wX);
        this.nTly = Math.min(p1.wY, p2.wY);
        this.nWidth = Math.max(p1.wX, p2.wX) - this.nTlx;
        this.nHeight = Math.max(p1.wY, p2.wY) - this.nTly;
    }
    
    public Rect(Point p) {
        this.nTlx = p.wX;
        this.nTly = p.wY;
        this.nWidth = 0;
        this.nHeight = 0;
    }
    
    public void add(Point p) {
    	if (p.wX < this.nTlx) {
    		this.nTlx = p.wX;
    	}
    	if (p.wY < this.nTly) {
    		this.nTly = p.wY;
    	}
    	this.nWidth = Math.max(this.nWidth, p.wX - this.nTlx);
    	this.nHeight = Math.max(this.nHeight, p.wY - this.nTly);
    }
    
    public int getArea() {
        return this.nWidth * this.nHeight;
    }
    
    public int getLeft() {
    	return this.nTlx;
    }
    
    public int getHeight() {
        return this.nHeight;
    }
    
    public int getTop() {
    	return this.nTly;
    }
    
    public int getWidth() {
        return this.nWidth;
    }
    
}
