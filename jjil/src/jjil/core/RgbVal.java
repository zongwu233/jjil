/*
 * RgbVal.java
 *
 * Created on September 9, 2006, 10:42 AM
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

package jjil.core;

/**
 * Helper class for manipulating RGB values. All functions are static.
 * @author webb
 */
public class RgbVal {
    /**
     * Converts byte R, G, and B values to an ARGB word.
     * byte is a signed data type but the ARGB word has unsigned bit fields.
     * In other words the minimum byte value is Byte.MIN_VALUE but the color black in
     * the ARGB word is represented as 0x00. So we must subtract Byte.MIN_VALUE to get an
     * unsigned byte value before shifting and combining the bit fields.
     *
     * @param R input signed red byte
     * @param G input signed green byte
     * @param B input signed blue byte
     * @return the color ARGB word.
     */
    public static int toRgb(byte R, byte G, byte B) {
        return (toUnsignedInt(R)<<16) |
                (toUnsignedInt(G)<<8)  |
                toUnsignedInt(B);
    }
    
    /**
     * Extracts blue byte from input ARGB word.
     * The bit fields in ARGB word are unsigned, ranging from 0x00 to 0xff.
     * To convert these to the returned signed byte value we must add
     * Byte.MIN_VALUE.
     * @return the blue byte value, converted to a signed byte
     */
    public static byte getB(int ARGB) {
        return toSignedByte((byte)(ARGB & 0xff));
    }
    
    /**
     * Extracts green byte from input ARGB word.
     * The bit fields in ARGB word are unsigned, ranging from 0x00 to 0xff.
     * To convert these to the returned signed byte value we must add
     * Byte.MIN_VALUE.
     * @param ARGB the input color ARGB word.
     * @return the green byte value, converted to a signed byte
     */
    public static byte getG(int ARGB) {
        return toSignedByte((byte) ((ARGB>>8) & 0xff));
    }
    
    /**
     * Extracts red byte from input ARGB word.
     * The bit fields in ARGB word are unsigned, ranging from 0x00 to 0xff.
     * To convert these to the returned signed byte value we must add
     * Byte.MIN_VALUE.
     * @param ARGB the input color ARGB word.
     * @return the red byte value, converted to a signed byte
     */
    public static byte getR(int ARGB) {
        return toSignedByte((byte) ((ARGB>>16) & 0xff));
    }
    
    /** Converts from an unsigned bit field (as stored in an ARGB word
     * to a signed byte value (that we can do computation on).
     * @return the signed byte value
     */
    public static byte toSignedByte(byte b) {
        return (byte) (b + Byte.MIN_VALUE);
    }
    
    /** Converts from a signed byte value (which we do computation on)
     * to an unsigned bit field (as stored in an ARGB word). The result
     * is returned as an int because the unsigned 8 bit value cannot be
     * represented as a byte.
     *
     * @return the unsigned bit field
     */
    public static int toUnsignedInt(byte b) {
        return (b - Byte.MIN_VALUE);
    }
}
