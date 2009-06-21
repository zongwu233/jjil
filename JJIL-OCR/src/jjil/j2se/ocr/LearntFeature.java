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
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Auto-generated Javadoc
/**
 * Copyright 2008 by Jon A. Webb
 * 
 * @author webb
 */
public class LearntFeature implements Serializable {
    
    /** The Constant serialVersionUID. */
	private static final long serialVersionUID = 3424433140320312872L;

	/** The Constant WERDS_PER_CONFIG_VEC. */
	private final static int WERDS_PER_CONFIG_VEC = 
            ((Prototypes.MAX_NUM_CONFIGS + Integer.SIZE - 1) / Integer.SIZE);
    
    /** The A. */
    private int A = 0;
    
    /** The B. */
    private short B = 0; // actually, unsigned byte
    
    /** The C. */
    private int C = 0;
    
    /** The Angle. */
    private short Angle = 0; // actually, unsigned byte
    
    /** The mrb config. */
    private byte[] mrbConfig;

   
    /**
     * Instantiates a new learnt feature.
     */
    public LearntFeature() {
    }
    
    /**
     * Instantiates a new learnt feature.
     * 
     * @param fis the fis
     * @param bo the bo
     */
    LearntFeature(FileInputStream fis, ByteOrder bo) {
        try {
            final int nIntByteSize = Integer.SIZE / Byte.SIZE;
            ByteBuffer bb = ByteBuffer.allocate(4 + 
                    nIntByteSize * WERDS_PER_CONFIG_VEC);
            bb.order(bo);
            fis.read(bb.array());
            this.A = bb.get(0) << 1;
            // Java doesn't have unsigned byte
            this.B = unsignedByteToShort(bb.get(1));
            this.C = bb.get(2) << 9;
            this.Angle = unsignedByteToShort(bb.get(3));
            int[] nInts = new int[WERDS_PER_CONFIG_VEC];
            IntBuffer ib = bb.asIntBuffer();
            for (int i=0; i<WERDS_PER_CONFIG_VEC; i++) {
                nInts[i] = ib.get(i+1);
            }
            setConfigBits(nInts);
        } catch (IOException ex) {
            Logger.getLogger(LearntFeature.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LearntFeature)) {
            return false;
        }
        LearntFeature other = (LearntFeature) o;
        return this.A == other.A && this.B == other.B && this.C == other.C &&
                this.Angle == other.Angle;
    }

    /**
     * Gets the a.
     * 
     * @return the a
     */
    public int getA() {
        return this.A;
    }
    
    /**
     * Gets the angle.
     * 
     * @return the angle
     */
    public short getAngle() {
        return this.Angle;
    }
    
    /**
     * Gets the b.
     * 
     * @return the b
     */
    public short getB() {
        return this.B;
    }
    
    /**
     * Gets the c.
     * 
     * @return the c
     */
    public int getC() {
        return this.C;
    }
    
    /**
     * Gets the configs.
     * 
     * @return the configs
     */
    public byte[] getConfigs() {
        return this.mrbConfig;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.A;
        hash = 97 * hash + this.B;
        hash = 97 * hash + this.C;
        hash = 97 * hash + this.Angle;
        return hash;
    }
    
    /**
     * Sets the config bits.
     * 
     * @param nInts the new config bits
     */
    private void setConfigBits(int[] nInts) {
    	BitSet Configs = new BitSet(Prototypes.MAX_NUM_CONFIGS);
        int k = 0;
        for (int n: nInts) {
            int nBit = 1;
            for (int j=0; j<32; j++, nBit <<= 1) {
                if ((nBit & n) != 0) {
                    Configs.set(k);
                }
                k++;
            }
        }
        this.mrbConfig = new byte[Configs.cardinality()];
        int nBit = 0;
        for (int nConfig = 0;
        -1 != (nConfig = Configs.nextSetBit(nConfig));
        nConfig++) {
        	this.mrbConfig[nBit++] = (byte) nConfig;
        }
    }
    
    /**
     * Java doesn't support unsigned byte so we store as a short and convert
     * here.
     * 
     * @param bB the b b
     * 
     * @return the short
     */
    public static short unsignedByteToShort(byte bB) {
        return  (bB < 0) ? (short) (256 + bB) : bB;
    }
}
