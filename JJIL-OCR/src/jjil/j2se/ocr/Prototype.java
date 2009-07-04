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
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Auto-generated Javadoc
/**
 * Copyright 2008 by Jon A. Webb
 * 
 * @author webb
 */
public class Prototype implements Serializable {
    
    /** The Constant BITSINLONG. */
    public static final int BITSINLONG = 32;
    
    /** The Constant NUM_PP_PARAMS. */
    public static final int NUM_PP_PARAMS = 3;
    
    /** The Constant NUM_PP_BUCKETS. */
    public static final int NUM_PP_BUCKETS = 64;
    
    /** The Constant PROTOS_PER_PROTO_SET. */
    public static final int PROTOS_PER_PROTO_SET = 64;
    
    /** The Constant WERDS_PER_PP_VECTOR. */
    public final static int WERDS_PER_PP_VECTOR = 
            (PROTOS_PER_PROTO_SET+Prototypes.BITS_PER_WERD-1) / 
                Prototypes.BITS_PER_WERD;
    
    /** The Constant PRUNER_X. */
    public static final int PRUNER_X = 0;
    
    /** The Constant PRUNER_Y. */
    public static final int PRUNER_Y = 1;
    
    /** The Constant PRUNER_ANGLE. */
    public static final int PRUNER_ANGLE = 2;

    /** The n int protos. */
    private int nIntProtos = 0;
    
    /** The Protos. */
    private LearntFeature Protos[] = new LearntFeature[PROTOS_PER_PROTO_SET];
    // ProtoPruner is unsigned int in the Tess code
//    private int ProtoPruner[][][] = 
//                new int[NUM_PP_PARAMS][NUM_PP_BUCKETS][WERDS_PER_PP_VECTOR];
    /** The Proto pruner. */
    private BitSet ProtoPruner[][] = 
                new BitSet[NUM_PP_PARAMS][NUM_PP_BUCKETS];
    
    {
        for (int i=0; i<NUM_PP_PARAMS; i++) {
            for (int j=0; j<NUM_PP_BUCKETS; j++) {
                this.ProtoPruner[i][j] = new BitSet(PROTOS_PER_PROTO_SET);
            }
        }        
    }
    
    /**
     * Instantiates a new prototype.
     */
    public Prototype() {
        
    }
    
    /**
     * Instantiates a new prototype.
     * 
     * @param fis the fis
     * @param bo the bo
     * @param nProtosToRead the n protos to read
     */
    public Prototype(FileInputStream fis, ByteOrder bo, int nProtosToRead) {
        try {
            final int nIntByteSize = Integer.SIZE / Byte.SIZE;
            ByteBuffer bb = ByteBuffer.allocate(nIntByteSize * NUM_PP_PARAMS * 
                    NUM_PP_BUCKETS * WERDS_PER_PP_VECTOR);
            bb.order(bo);
            fis.read(bb.array());
            IntBuffer ib = bb.asIntBuffer();
            int p = 0;
            for (int i=0; i<NUM_PP_PARAMS; i++) {
                for (int j=0; j<NUM_PP_BUCKETS; j++) {
                    for (int k=0; k<WERDS_PER_PP_VECTOR; k++) {
                        int nWord = ib.get(p++);
                        for (int l=0; l<Integer.SIZE && nWord != 0; l++) {
                            if ((nWord&1) == 1) {
                                this.ProtoPruner[i][j].set(k*Integer.SIZE+l);
                            }
                            nWord >>>= 1;
                        }
                    }
                }
            }
            for (int i=0; i<PROTOS_PER_PROTO_SET; i++) {
                LearntFeature ip =  new LearntFeature(fis, bo);
                if (i < nProtosToRead) {
                    this.Protos[i] = ip;
                }
            }
            this.nIntProtos = nProtosToRead;
        } catch (IOException ex) {
            Logger.getLogger(Prototype.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Adds the.
     * 
     * @param ip the ip
     */
    public void add(LearntFeature ip) {
        this.Protos[this.nIntProtos++] = ip;
    }
    
    /**
     * Gets the.
     * 
     * @param nIndex the n index
     * 
     * @return the learnt feature
     */
    public LearntFeature get(int nIndex) {
        return this.Protos[nIndex];
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Prototype)) {
            return false;
        }
        Prototype other = (Prototype) o;
        if (!Arrays.equals(this.Protos, other.Protos)) {
            return false;
        }
        if (this.ProtoPruner.length != other.ProtoPruner.length) {
            return false;
        }
        for (int i=0; i<NUM_PP_PARAMS; i++) {
            if (this.ProtoPruner[i].length != other.ProtoPruner[i].length) {
                return false;
            }
            for (int j=0; j<NUM_PP_BUCKETS; j++) {
                if (!this.ProtoPruner[i][j].equals(other.ProtoPruner[i][j])) {
                    return false;
                }
            }
        }        
        return true;
    }
    
    /**
     * Gets the num int protos.
     * 
     * @return the num int protos
     */
    public int getNumIntProtos() {
        return this.nIntProtos;
    }
    
    /**
     * Gets the proto pruner.
     * 
     * @return the proto pruner
     */
    public BitSet[][] getProtoPruner() {
        return this.ProtoPruner;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.Protos != null ? this.Protos.hashCode() : 0);
        return hash;
    }    
}
