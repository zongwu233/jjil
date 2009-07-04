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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

// TODO: Auto-generated Javadoc
/**
 * Copyright 2008 by Jon A. Webb
 * 
 * @author webb
 */
public class FileLineReader extends InputStreamReader {
    
    /** The Constant CHARS_PER_LINE. */
    public final static int CHARS_PER_LINE = 500;
    
    /**
     * Instantiates a new file line reader.
     * 
     * @param szFilename the sz filename
     * 
     * @throws FileNotFoundException the file not found exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public FileLineReader(String szFilename) throws 
            FileNotFoundException, 
            UnsupportedEncodingException {
        super(new FileInputStream(szFilename), "UTF-8");
    }
    
    /**
     * Fgets.
     * 
     * @param nMaxChars the n max chars
     * 
     * @return the string
     */
    public String fgets(int nMaxChars) {
        String szValue = "";
        try {
            while (nMaxChars > 0) {
                int nCh = this.read();
                nMaxChars--;
                if (nCh != -1) { // EOF
                    if (nCh == new Character('\n').charValue()) {
                        return szValue;
                    } else {
                        szValue += new Character((char)nCh).charValue();
                    }
                } else {
                    if (szValue.length() > 0) {
                        return szValue;
                    } else {
                        return null;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return szValue;
    }
    
    /**
     * Gets the line fields.
     * 
     * @return the line fields
     */
    public String[] getLineFields() {
        String szLine = fgets(Integer.MAX_VALUE);
        return szLine.split(" +");
    }
}
