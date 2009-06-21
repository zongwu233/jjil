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

import jjil.core.TimeTracker;
import jjil.j2se.algorithm.Pair;

/**
 * The Class CharMatcher provides methods for matching a Features object
 * against a PrototypesCollection.
 * 
 * @author webb
 */
public class CharMatcher {

    /** The Constant MAX_MATCH defines the largest match value which is allowed for a match to be considered good enough to report. */
    private final static int MAX_MATCH = 100;
    
    /** The minimum character width allowed when splitting an unmatched character into two parts for recursive matching. */
    private int mnMinCharWidth;
    
    /** The characters in the PrototypesColleciton to match with. */
    private char[] mrchToTest;
    
    /** The PrototypesCollection, which defines the character prototypes to match with. */
    private PrototypesCollection mPrototypesCollection;
    
    /** A TimeTracker object, which is used to track and report time in various expensive steps in this class. */
    private TimeTracker mTimeTracker;

    /**
     * Instantiates a new char matcher.
     * 
     * @param pc the PrototypesCollection to match against
     * @param rchToTest the characters in the PrototypesCollection that will be
     * matched against
     * @param tt the TimeTracker object used to monitor expensive steps. May be
     * null, in which case time is not measured
     */
    public CharMatcher(PrototypesCollection pc, char[] rchToTest, TimeTracker tt) {
        this.mPrototypesCollection = pc;
        this.mrchToTest = rchToTest;
        this.mTimeTracker = tt;
    }

    /**
     * Compares the computed features against the selected characters in the
     * PrototypesCollection and returns the best match, or null if no match is
     * better than (less than) the maximum match value.
     * 
     * @param cfs the Features object, which contains the measured features
     * of the character to be matched
     * 
     * @return the best matching character from the PrototypesCollection, or null if
     * there is no good match
     */
    private Character getBestMatch(Features cfs) {
        Integer nBestResult = Integer.MAX_VALUE, nSecondBestResult = Integer.MAX_VALUE;
        Character chBestChar = (char) 0, chSecondBestChar = (char) 0;
        for (Character ch : this.mrchToTest) {
            Prototypes ic = this.mPrototypesCollection.get(new Character(ch));
            if (ic.couldMatch(cfs)) {
	            int nBloblLength = cfs.getLength();
	            int nFeatures = cfs.size();
	            Evidence e = new Evidence(ic, this.mTimeTracker);
	            Evidence.Result er = e.IntegerMatcher(ic, nBloblLength, 
                            nFeatures, cfs);
	            if (er.getRating() < nBestResult) {
	            	if (nBestResult < nSecondBestResult) {
	            		nSecondBestResult = nBestResult;
	            		chSecondBestChar = chBestChar;
	            	}
	                nBestResult = er.getRating();
	                chBestChar = ic.getUnichar();
	            }
            }
        }
        // special case -- 3 vs 8
        if (chBestChar == '3' && chSecondBestChar == '8' ||
        		chBestChar == '8' && chSecondBestChar == '3') {
        	if (cfs.getComponents() > 2) {
        		chBestChar = '8';
        	} else {
        		chBestChar = '3';
        	}
        }
        if (nBestResult < MAX_MATCH) {
            return chBestChar;
        } else {
            return null;
        }
    }

    /**
     * Match and split. Get the best match for a character. If there is no good
     * match, examine the character and try to split it into two characters. Repeat
     * if the split parts of the character are both wider than the minimum character
     * width.
     * 
     * @param cfs the Features object containing the measured features of the
     * character
     * 
     * @return the string containing the characters found, in order, from left to
     * right
     */
    public String matchAndSplit(Features cfs) {
        String szResult = "";
        Character ch = getBestMatch(cfs);
        // check to see if character is "good enough" and add to string
        // if it is
        if (ch != null) {
            szResult += ch;
        } else {
            Pair<Features, Features> prnfx = 
                    cfs.testAndSplit(this.mnMinCharWidth);
            if (prnfx != null) {
                if (prnfx.getFirst() != null) {
                    szResult += matchAndSplit(prnfx.getFirst());
                }
                if (prnfx.getSecond() != null) {
                    szResult += matchAndSplit(prnfx.getSecond());
                }
            }
        }
        return szResult;
    }
    
    /**
     * Sets the min char width. This is the minimum width we expect a character
     * to be, based on the resolution of the image and what we know about the
     * font, etc.
     * 
     * @param nMinCharWidth the new minimum character width, in pixels
     */
    public void setMinCharWidth(int nMinCharWidth) {
        this.mnMinCharWidth = nMinCharWidth;    	
    }
}

