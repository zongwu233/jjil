/*
 * GrayLookup.java
 *
 * Created on September 9, 2006, 2:52 PM
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

package jjil.algorithm;
import jjil.core.*;

/**
 * Pipeline stage applies a lookup table to an image. The lookup table
 * can be supplied through the constructor or by the setTable procedure.
 * This pipeline stage modifies its input.
 *
 * @author webb
 */
public class GrayLookup extends PipelineStage {
    private byte[] table;
    
    /**
     * Creates a new instance of GrayLookup.
     * @param table The mapping table. Element i maps gray value Byte.MinValue + i to table[i].
     * @throws java.lang.IllegalArgumentException when table is not a 256-element array.
     */
    public GrayLookup(byte[] table) throws IllegalArgumentException {
        setTable(table);
    }
    
    /**
     * Return the lookup table currently being used.
     * @return the lookup table.
     */
    public byte[] getTable() {
        byte[] result = new byte[256];
        System.arraycopy(this.table, 0, result, 0, this.table.length);
        return result;
    }
    
    /**
     * Maps input Gray8Image through the lookup table, replacing values in the image.
     * @param image the input image (output replaces input).
     * @throws java.lang.IllegalArgumentException if image is not a Gray8Image.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() + " should be " +
                    "a Gray8Image");
        }
        Gray8Image input = (Gray8Image) image;
        byte[] data = input.getData();
        for (int i=0; i<data.length; i++) {
            data[i] = this.table[data[i]+128];
        }
        super.setOutput(input);
    }
    
    /**
     * Assign a new lookup table. Images passed to Push() after setTable is called
     * will be mapped by the new image.
     * @param table The lookup table. Input image value g is mapped to table[g + Byte.MinValue]
     * @throws java.lang.IllegalArgumentException if table is not a 256-element array.
     */
    public void setTable(byte[] table) throws IllegalArgumentException {
        if (table.length != 256) {
            throw new IllegalArgumentException("lookup table must have 256" +
                    " entries");
        }
        this.table = new byte[256];
        System.arraycopy(table, 0, this.table, 0, this.table.length);
    }
}
