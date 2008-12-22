package jjil.app.facedetect;

/*
 * DetectHaarParam.java
 *
 * Created on October 7, 2006, 3:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2006 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Vector;

import jjil.algorithm.Gray8Crop;
import jjil.algorithm.HaarClassifierCascade;
import jjil.algorithm.RgbAvgGray;
import jjil.core.Gray8Image;
import jjil.core.Rect;
import jjil.core.RgbImage;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
/**
 * @author webb
 */
public class DetectHaarParam {
    /** DetectHaar pipeline
     */
	private static HaarClassifierCascade mHcc = null;
    private Rect mRectCurrent;
    Vector<Rect> mVecRects = new Vector<Rect>();
    
    /**
     * Creates a new instance of DetectHaarParam
     */
       
    public Rect getCurrentRect() {
    	return this.mRectCurrent;
    }
    
    public Enumeration<Rect> getResult() {
    	return this.mVecRects.elements();
    }
        
    public void push(final RgbImage rgb) {
    	if (mHcc != null) {
	    	mVecRects.clear();
	        RgbAvgGray rg = new RgbAvgGray();
	        try {
	        	rg.push(rgb);
	        	Gray8Image gray = (Gray8Image) rg.getFront();
	        	Gray8Crop crop = new Gray8Crop(0, 0, mHcc.getWidth(), mHcc.getHeight());
	        	int nHorizSkip = Math.max(rgb.getWidth()/16, mHcc.getWidth() / 10);
	        	int nVertSkip = Math.max(rgb.getHeight()/16, mHcc.getHeight() / 10);
	        	for (int i=0; 
	        		i<=gray.getHeight()-mHcc.getHeight() && mVecRects.size() == 0; 
	        		i+=nVertSkip) {
	        		for (int j=0; 
	        			j<=gray.getWidth()-mHcc.getWidth() && mVecRects.size() == 0; 
	        			j+=nHorizSkip) {
	        			mRectCurrent = new Rect(j, i, mHcc.getWidth(), mHcc.getHeight());
	        			crop.setWindow(mRectCurrent);
	        			crop.push(gray);
	        			Gray8Image cropped = (Gray8Image) crop.getFront();
	        			boolean bRect = false;
	        			if (mHcc.eval(cropped) || bRect) {
	        				Rect r = new Rect(j, i, mHcc.getWidth(), mHcc.getHeight());
	        				mVecRects.add(r);
	        			}
	        		}
	        	}
	        } catch (jjil.core.Error er) {
				er.printStackTrace();
	        	Log.v("DetectHaarParam: push", er.getLocalizedMessage());
	        }
		}
     }
    
    public static void setCascade(HaarClassifierCascade hcc) {
    	mHcc = hcc;
    }
               
    public String toString() {
        return super.toString();
    }
}
