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
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

// TODO: Auto-generated Javadoc
/**
 * The Class PrototypesCollection.
 */
public class PrototypesCollection extends HashMap<Character, Prototypes> 
        implements Serializable
{
    
    /** The Constant ILLEGAL_CLASS. */
    private static final short ILLEGAL_CLASS = -1;
    
    /** The Constant NO_CLASS. */
    public static final short NO_CLASS = 0;
    
    /** The Constant MAX_NUM_CLASSES. */
    private static final int MAX_NUM_CLASSES = 8192;
    
    /** The Constant MAX_CLASS_ID. */
    private static final int MAX_CLASS_ID = (MAX_NUM_CLASSES - 1);
    
    /** The Constant NUM_CP_BUCKETS. */
    public static final int NUM_CP_BUCKETS = 24;
    
    /** The Constant CLASSES_PER_CP. */
    public static final int CLASSES_PER_CP = 32;
    
    /** The Constant NUM_BITS_PER_CLASS. */
    private static final int NUM_BITS_PER_CLASS = 2;
    
    /** The Constant BITS_PER_WERD. */
    private static final int BITS_PER_WERD = 32;
    
    /** The Constant BITS_PER_CP_VECTOR. */
    private static final int BITS_PER_CP_VECTOR = (CLASSES_PER_CP * NUM_BITS_PER_CLASS);
    
    /** The Constant CLASSES_PER_CP_WERD. */
    public static final int CLASSES_PER_CP_WERD = (CLASSES_PER_CP / NUM_BITS_PER_CLASS);
    
    /** The Constant MAX_NUM_CLASS_PRUNERS. */
    private static final int MAX_NUM_CLASS_PRUNERS = 
            ((MAX_NUM_CLASSES + CLASSES_PER_CP - 1) / CLASSES_PER_CP);
    
    /** The Constant WERDS_PER_CP_VECTOR. */
    private static final int WERDS_PER_CP_VECTOR = (BITS_PER_CP_VECTOR / BITS_PER_WERD);
    
    /** The mch unichars. */
    char mchUnichars[];
    
    /** The mhm char cutoffs. */
    private HashMap<Character, Integer> mhmCharCutoffs = 
            new HashMap<Character, Integer>();
    
    /** The Index for. */
    short IndexFor[] = new short[MAX_CLASS_ID + 1];
    
    /** The Class id for. */
    int ClassIdFor[] = new int[MAX_NUM_CLASSES];   

    {
        Arrays.fill(this.IndexFor, ILLEGAL_CLASS);
        Arrays.fill(this.ClassIdFor, NO_CLASS);
    }
    
    /**
     * Instantiates a new prototypes collection.
     */
    public PrototypesCollection() {
    }
    
    /**
     * Instantiates a new prototypes collection.
     * 
     * @param szUnicharFile the sz unichar file
     * @param szPfmTable the sz pfm table
     * @param szIntTempFile the sz int temp file
     */
    public PrototypesCollection(
            String szUnicharFile, 
            String szPfmTable,
            String szIntTempFile) {
        
        final int nIntByteSize = Integer.SIZE / Byte.SIZE;
        final int nShortByteSize = Short.SIZE / Byte.SIZE;
        try {
            load_from_file(szUnicharFile);
            loadPffmTable(szPfmTable);
            FileInputStream fis = new FileInputStream(szIntTempFile);
            ByteBuffer bb = ByteBuffer.allocate(nIntByteSize * 3);
            ByteOrder bo = ByteOrder.BIG_ENDIAN;
            fis.read(bb.array());
            IntBuffer ib = bb.asIntBuffer();
            boolean swap = (ib.get(2) < 0 || ib.get(2) > MAX_NUM_CLASS_PRUNERS);
            if (swap) {
                bo = ByteOrder.LITTLE_ENDIAN;
                bb.order(bo);
                ib = bb.asIntBuffer();
            }
            int unicharset_size = ib.get(0);
            int NumClasses = ib.get(1);
            int NumClassPruners = ib.get(2);
            int version_id = 0;
            if (NumClasses < 0) {
                // handle version id
                version_id = -NumClasses;
                fis.read(bb.array(), 0, 4);
                NumClasses = ib.get(0);
            }    
            //this.ClassPruner.ensureCapacity(NumClassPruners);
            bb = ByteBuffer.allocate(nShortByteSize * unicharset_size);
            bb.order(bo);
            fis.read(bb.array());
            ShortBuffer sb = bb.asShortBuffer();
            for (int i=0; i<unicharset_size; i++) {
                this.IndexFor[i] = sb.get(i);
            }
            bb = ByteBuffer.allocate(nIntByteSize * NumClasses);
            bb.order(bo);
            fis.read(bb.array());
            ib = bb.asIntBuffer();
            for (int i=0; i<NumClasses; i++) {
                this.ClassIdFor[i] = ib.get(i);
            }
            ArrayList<byte[][][][]> ClassPruners = new ArrayList<byte[][][][]>();
            for (int i=0; i<NumClassPruners; i++) {
                byte[][][][] Pruner = 
                        new byte[NUM_CP_BUCKETS][NUM_CP_BUCKETS][NUM_CP_BUCKETS][];
                bb = ByteBuffer.allocate(nIntByteSize * NUM_CP_BUCKETS * 
                        NUM_CP_BUCKETS * NUM_CP_BUCKETS * WERDS_PER_CP_VECTOR);
                bb.order(bo);
                fis.read(bb.array());
                ib = bb.asIntBuffer();
                int p = 0;
                for (int j=0; j<NUM_CP_BUCKETS; j++) {
                    for (int k=0; k<NUM_CP_BUCKETS; k++) {
                        for (int l=0; l<NUM_CP_BUCKETS; l++) {
                            Pruner[j][k][l] = 
                                new byte[CLASSES_PER_CP];
                            int nClass = 0;
                            for (int m=0; m<WERDS_PER_CP_VECTOR; m++) {
                                int nWord = ib.get(p++);
                                for (int n=0; n<CLASSES_PER_CP_WERD; n++) {
                                    Pruner[j][k][l][nClass++] = (byte) (nWord & 3);
                                    nWord >>>= 2;
                                }
                            }
                        }
                    }
                }
                ClassPruners.add(Pruner);
            } 
            for (int i=0; i<NumClasses; i++) {
                this.put(
                    this.mchUnichars[i+1],
                    new Prototypes(this.mchUnichars[i+1], 
                        this.mhmCharCutoffs.get(this.mchUnichars[i+1]),
                        fis, bo, version_id, ClassPruners, i));
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractMap#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrototypesCollection)) {
            return false;
        }
        PrototypesCollection other = (PrototypesCollection) o;
        if (this.IndexFor.length != other.IndexFor.length) {
            return false;
        }
        for (int i=0; i<this.IndexFor.length; i++) {
            if (this.IndexFor[i] != other.IndexFor[i]) {
                return false;
            }
        }
        if (this.ClassIdFor.length != other.ClassIdFor.length) {
            return false;
        }
        for (int i=0; i<this.ClassIdFor.length; i++) {
            if (this.ClassIdFor[i] != other.ClassIdFor[i]) {
                return false;
            }
        }
        if (this.size() != other.size()) {
            return false;
        }
        for (int i=0; i<this.size(); i++) {
            if (!this.get(i).equals(other.get(i))) {
                return false;
            }
        }
//        if (this.ClassPruner.size() != other.ClassPruner.size()) {
//            return false;
//        }
//        for (int i=0; i<this.ClassPruner.size(); i++) {
//            for (int j=0; j<this.ClassPruner.get(i).length; j++) {
//                for (int k=0; k<this.ClassPruner.get(i)[j].length; k++) {
//                    for (int l=0; l<this.ClassPruner.get(i)[j][k].length; l++) {
//                        if (!this.ClassPruner.get(i)[j][k][l].equals( 
//                                other.ClassPruner.get(i)[j][k][l])) {
//                            return false;
//                        }
//                    }
//                }
//            }
//        }
        return true;
    }
    
    /**
     * Gets the class id for.
     * 
     * @param index the index
     * 
     * @return the class id for
     */
    public int getClassIdFor(int index) {
        return this.ClassIdFor[index];
    }
    
    /**
     * Gets the unichar.
     * 
     * @param index the index
     * 
     * @return the unichar
     */
    public char getUnichar(int index) {
        return this.mchUnichars[index];
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractMap#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this.IndexFor != null ? this.IndexFor.hashCode() : 0);
        hash = 83 * hash + (this.ClassIdFor != null ? this.ClassIdFor.hashCode() : 0);
//        hash = 83 * hash + (this.ClassPruner != null ? this.ClassPruner.hashCode() : 0);
        return hash;
    }

    /**
     * Load_from_file.
     * 
     * @param szFilename the sz filename
     * 
     * @return true, if successful
     * 
     * @throws FileNotFoundException the file not found exception
     */
    public boolean load_from_file(String szFilename) 
            throws FileNotFoundException
    {
        try {
            FileLineReader flr = new FileLineReader(szFilename);
            String szLine = flr.fgets(256);
            int nUnicharSetSize = Integer.decode(szLine);
            int size_reserved = nUnicharSetSize;
            this.mchUnichars = new char[size_reserved];
            for (int id = 0; id<nUnicharSetSize; id++) {
                String szBuffer = flr.fgets(256);
                if (szBuffer == null) {
                    return false;
                }
                // parse the line. format is %s %x %63s or %s %x
                String szParse[] = szBuffer.split(" +");
                if (szParse.length < 2) {
                    return false; // parse failed
                }
                String szUnichar = szParse[0];
                if (szUnichar.equals("NULL")) {
                    szUnichar = " ";
                }
                try {
                } catch (Exception ex) {
                    // parse failed
                    return false;
                }
                if (szParse.length == 3) {
                }
                this.mchUnichars[id] = szUnichar.charAt(0);
            }
            flr.close();
        } catch (IOException ex) {
            if (ex instanceof FileNotFoundException) {
                throw (FileNotFoundException) ex;
            }
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Load pffm table.
     * 
     * @param szFilename the sz filename
     * 
     * @return true, if successful
     * 
     * @throws FileNotFoundException the file not found exception
     */
    public boolean loadPffmTable(String szFilename) 
            throws FileNotFoundException
    {
        try {
            FileLineReader flr = new FileLineReader(szFilename);
            for (int id = 0; id<this.mchUnichars.length; id++) {
                String szBuffer = flr.fgets(256);
                if (szBuffer == null) {
                    return false;
                }
                // parse the line. format is %s %x %63s or %s %x
                String szParse[] = szBuffer.split(" +");
                if (szParse.length < 2) {
                    return false; // parse failed
                }
                String szUnichar = szParse[0];
                int nCutoff;
                try {
                    nCutoff = Integer.decode(szParse[1]);
                } catch (Exception ex) {
                    // parse failed
                    return false;
                }
                this.mhmCharCutoffs.put(szUnichar.charAt(0), nCutoff);
            }
            flr.close();
        } catch (IOException ex) {
            if (ex instanceof FileNotFoundException) {
                throw (FileNotFoundException) ex;
            }
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
