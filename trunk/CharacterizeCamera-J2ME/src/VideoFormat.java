/*
 * VideoFormat.java
 *
 * Created on October 23, 2007, 5:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
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

/**
 * VideoFormat describes an image or video format like that returned from
 * System.getProperty. The reason for the class is to provide a way to
 * parse and interpret the contents of the format.
 * @author webb
 */
public class VideoFormat {
   private final ImageFormat fmt;
   private final int nWidth, nHeight, nFps;
   private final String szFormat;

    private String getAttribute(String sz, String szAttr) {
        int n = sz.indexOf(szAttr);
        if (n < 0) return null;
        n += szAttr.length();
        sz = sz.substring(n);
        int nAmp = sz.indexOf("&");
        if (nAmp < 0) {
            nAmp = sz.length();
        }
        return sz.substring(0, nAmp);
    }

    /**
     * Parse a video or image format string and use the contents to create a new
     * VideoFormat object.
     * @param sz This should be a string returned from System.getProperty("video.encodings")
     * or System.getProperty("video.snapshot.encodings"). A typical value is
     * "encoding=rgb888&amp;width=160&amp;height=120". 
     */
    public VideoFormat(String sz) {
        this.szFormat = sz;
        String szFormat = getAttribute(sz, "encoding=");
        if (szFormat != null) {
            this.fmt = ImageFormat.valueOf(szFormat);
        } else {
            this.fmt = null;
        }
        String szWidth = getAttribute(sz,"width=");
        if (szWidth != null) {
            this.nWidth = Integer.parseInt(szWidth);
        } else {
            this.nWidth = 0;
        }
        String szHeight = getAttribute(sz,"height=");
        if (szHeight != null) {
            this.nHeight = Integer.parseInt(szHeight);
        } else {
            this.nHeight = 0;
        }
        String szFps = getAttribute(sz,"fps=");
        if (szFps != null) {
            this.nFps = Integer.parseInt(szFps);
        } else {
            this.nFps = 0;
        }
    }
    
    public String getEncoding() {
        return this.szFormat;
    }
    
    public int getHeight() {
        return this.nHeight;
    }
    
    public int getWidth() {
        return this.nWidth;
    }
}
