/*
 * CharacterizeCamera.java
 *
 * Created on October 21, 2006, 3:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2006 by Jon A. Webb
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
import java.util.*;
import javax.microedition.media.Manager;
/**
 *
 * @author webb
 */
public class CharacterizeCamera {
    private final boolean oSupportsCapture;
    private final Hashtable htszCaptureTypes = new Hashtable();
    private final Vector vVidEncFmt = new Vector();
    private final Vector vVidCapFmt = new Vector();
        
    private class SplitEnumeration implements Enumeration {
        String sz;
        String szSep;
        
        public SplitEnumeration(String sz, String szSep) {
            this.sz = sz;
            this.szSep = szSep;
        }
        private void stringLeadingSeparators() {
            while (this.sz.length() > 0 && sz.startsWith(this.szSep)) {
                this.sz = this.sz.substring(this.szSep.length());
            }
        }
        public boolean hasMoreElements() {
            if (sz == null) {
                return false;
            }
            stringLeadingSeparators();
            if (sz.length() == 0) {
                return false;
            }
            return true;
        }
        public Object nextElement() {
            if (this.sz == null) {
                throw new NoSuchElementException();
            }
            stringLeadingSeparators();
            if (this.sz.length() == 0) {
                throw new NoSuchElementException();
            }
            int nNextSep = this.sz.indexOf(this.szSep);
            String szElement;
            if (nNextSep > 0) {
                szElement = this.sz.substring(0, nNextSep);
                this.sz = this.sz.substring(nNextSep+1);
            } else {
                szElement = this.sz;
                this.sz = null;
            }
            return szElement;
        }
    }
    
    /** Creates a new instance of CharacterizeCamera */
    public CharacterizeCamera() {
        if (System.getProperty("supports.video.capture").equals("true")) {
            this.oSupportsCapture = true;
            String [] rgszContentTypes = 
                    Manager.getSupportedContentTypes(null);
            Boolean oTrue = new Boolean(true);
            // look for content types that look like image content types
            // there doesn't appear to be any defined list of things that
            // are cameras
            for (int i=0; i<rgszContentTypes.length; i++) {
                if (rgszContentTypes[i].startsWith("video")) {
                    this.htszCaptureTypes.put("video", oTrue);
                } else if (rgszContentTypes[i].startsWith("image")) {
                    this.htszCaptureTypes.put("image", oTrue);
                } else if (rgszContentTypes[i].startsWith("devcam")) {
                    String szContentType = rgszContentTypes[i];
                    int nQuestion = szContentType.indexOf("?");
                    if (nQuestion > 0) {
                        szContentType = szContentType.substring(0, nQuestion);
                    }
                    this.htszCaptureTypes.put(szContentType, oTrue);
                } 
            }
            
            // parse the video encoding formats and save them
            for (SplitEnumeration se = 
                    new SplitEnumeration(
                        System.getProperty("video.encodings"), " ");
                se.hasMoreElements();) {
                    String sz= (String) se.nextElement();
                    this.vVidEncFmt.addElement(new VideoFormat(sz));
            }            

            // parse the video capture formats and save them
            for (SplitEnumeration se = 
                    new SplitEnumeration(
                        System.getProperty("video.snapshot.encodings"), " ");
                se.hasMoreElements();) {
                    String sz= (String) se.nextElement();
                    this.vVidCapFmt.addElement(new VideoFormat(sz));
            }            

        } else {
            this.oSupportsCapture = false;
        }
    }
    
    private class CaptureFormats implements Enumeration {
        int n;
        
        CaptureFormats() {
            this.n = 0;
        }
        public boolean hasMoreElements() {
            return this.n < vVidCapFmt.size();
        }
        public Object nextElement() throws NoSuchElementException {
            if (this.n < vVidCapFmt.size()) {
                return vVidCapFmt.elementAt(this.n ++);
            } else {
                throw new NoSuchElementException("there are only " + 
                        vVidCapFmt.size() + 
                        " capture formats");
            }
        }
    }
    
     private class EncodingFormats implements Enumeration {
        int n;
        
        EncodingFormats() {
            this.n = 0;
        }
        public boolean hasMoreElements() {
            return this.n < vVidEncFmt.size();
        }
        public Object nextElement() throws NoSuchElementException {
            if (this.n < vVidEncFmt.size()) {
                return vVidEncFmt.elementAt(this.n ++);
            } else {
                throw new NoSuchElementException("there are only " + 
                        vVidEncFmt.size() + 
                        " encoding formats");
            }
        }
    }
    
    public Enumeration getCaptureFormats(){
       return new CaptureFormats();
    }

    public Enumeration getCaptureSources() {
        return this.htszCaptureTypes.keys();
    }
    
    public Enumeration getEncodingFormats(){
       return new EncodingFormats();
    }

    public int getSourceCount() {
        return this.htszCaptureTypes.size();
    }
    public boolean getSupportsCapture() {
        return this.oSupportsCapture;
    }
}
