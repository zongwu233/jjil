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

import java.io.Serializable;
import java.nio.ByteBuffer;

import jjil.core.Vec2;
import jjil.j2se.algorithm.Pair;

// TODO: Auto-generated Javadoc
/**
 * The Class PrecomputeMath.
 * 
 * @author webb
 */
public class PrecomputeMath implements Serializable {
    
    /** The Constant serialVersionUID. */
	private static final long serialVersionUID = -2926944121519385162L;
	
	/** The Constant ATAN_TABLE_SIZE. */
	private static final int ATAN_TABLE_SIZE = 64;
    
    /** The Constant RESULT_TABLE_REDUCTION. */
    private static final int RESULT_TABLE_REDUCTION = 4;
    
    /** The Constant RadiusGyrMaxExp. */
    private static final short RadiusGyrMaxExp = 8;
    
    /** The Constant RadiusGyrMaxMan. */
    private static final short RadiusGyrMaxMan = 158;
    
    /** The Constant RadiusGyrMinExp. */
    private static final short RadiusGyrMinExp = 0;
    
    /** The Constant RadiusGyrMinMan. */
    private static final short RadiusGyrMinMan = 255;
    
    /** The Constant SE_TABLE_SIZE. */
    private static final int SE_TABLE_SIZE = 512;
    
    /** The Constant SimilarityCenter. */
    private static final double SimilarityCenter = 0.0075;
    
    /** The Constant SE_TABLE_BITS. */
    private static final int SE_TABLE_BITS = 9;
    
    /** The rpr result. */
    private int rprResult[][][];
    
    /** The Atan table. */
    private short AtanTable[];
    
    /** The Similarity evidence table. */
    public short SimilarityEvidenceTable[];
    
    /**
     * Instantiates a new precompute math.
     */
    public PrecomputeMath() {
    	
    }

    /**
     * Instantiates a new precompute math.
     * 
     * @param bb the bb
     */
    public PrecomputeMath(ByteBuffer bb) {
    };
    
//    public void initFromByteBuffer() {
//		rprResult = new int[32][64][64];
//		int nPos = 0;
//	   	for (int i=0; i<rprResult.length; i++) {
//	   		rprResult[i] = new IntBuffer[64];
//    		for (int j=0; j<rprResult[i].length; j++) {
//    			this.mbb.position(nPos);
//    			this.mbb.limit(nPos += 64*Integer.SIZE/Byte.SIZE);
//    			rprResult[i][j] = this.mbb.slice().asIntBuffer();
//    		}
//    	}
//	   	this.mbb.position(nPos);
//	   	this.mbb.limit(nPos += ATAN_TABLE_SIZE*Short.SIZE/Byte.SIZE);
//	   	AtanTable = this.mbb.slice().asShortBuffer();
//    	this.mbb.position(nPos);
//    	this.mbb.limit(nPos += SE_TABLE_SIZE*Short.SIZE/Byte.SIZE);
//    	SimilarityEvidenceTable = this.mbb.slice().asShortBuffer();
//    }
    
    /**
 * Inits the from math.
 */
public void InitFromMath() {
        this.rprResult = new int[32/RESULT_TABLE_REDUCTION][64][64];
for (int i=0; i<32; i+=RESULT_TABLE_REDUCTION) {
            for (int j=0; j<64; j++) {
//            	this.rprResult[i][j] = IntBuffer.allocate(64);
                for (int k=0; k<64; k++) {
                    Vec2 sqrtX = MySqrt2(i<<2, j<<13);
                    Vec2 sqrtY = MySqrt2(i<<2, k<<13);
                    this.rprResult[i/RESULT_TABLE_REDUCTION][j][k] =
                            PairVec2Int(ClipRadius(sqrtX, sqrtY));
//                    this.rprResult[i][j][k] = 
//                            ClipRadius(sqrtX, sqrtY);
                }
            }
        }        
//        AtanTable = ShortBuffer.allocate(ATAN_TABLE_SIZE);
        AtanTable = new short[ATAN_TABLE_SIZE];
        for (int i = 0; i < ATAN_TABLE_SIZE; i++) {
            AtanTable[i] =
              (short) (Math.atan ((i / (float) ATAN_TABLE_SIZE)) * 128.0 / 
                Math.PI + 0.5);
        }
        /* Initialize table for evidence to similarity lookup */
        SimilarityEvidenceTable = new short[SE_TABLE_SIZE];
        for (int i = 0; i < SE_TABLE_SIZE; i++) {
            int IntSimilarity = i << (27 - SE_TABLE_BITS);
            double Similarity = ((double) IntSimilarity) / 65536.0 / 65536.0;
            double Evidence = Similarity / SimilarityCenter;
            Evidence *= Evidence;
            Evidence += 1.0;
            Evidence = 1.0 / Evidence;
            Evidence *= 255.0;

            SimilarityEvidenceTable[i] = (short) (Evidence + 0.5);
        }
    }
    
    /**
     * Gets the result.
     * 
     * @param nBLFeat the n bl feat
     * @param nX the n x
     * @param nY the n y
     * 
     * @return the result
     */
    public Pair<Vec2,Vec2> getResult(int nBLFeat, int nX, int nY) {
        nBLFeat = Math.max(0, Math.min(31, nBLFeat>>2));
        nX = Math.max(0, Math.min(63, nX>>13));
        nY = Math.max(0, Math.min(63, nY>>13));
        Pair<Vec2,Vec2> pvvResult = Int2PairVec(this.rprResult[nBLFeat/RESULT_TABLE_REDUCTION][nX][nY]);
//        Pair<Vec2,Vec2> pvvResult = this.rprResult[nBLFeat][nX][nY];        
       	return pvvResult;
    }
    
    /**
     * Clip radius.
     * 
     * @param Rx the rx
     * @param Ry the ry
     * 
     * @return the pair< vec2, vec2>
     */
    private static Pair<Vec2, Vec2> ClipRadius(Vec2 Rx, Vec2 Ry) {
        int RxInvLarge, RyInvSmall;

        short AM = RadiusGyrMinMan;
        short AE = RadiusGyrMinExp;
        short BM = (short) Rx.getX();
        short BE = (short) Rx.getY();
        short LastCarry = 1;
        short BitN;
        while ((AM != 0) || (BM != 0)) {
            if (AE > BE) {
                BitN = (short) (LastCarry + (AM & 1) + 1);
                AM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
            } else if (AE < BE) {
                BitN = (short) (LastCarry + ((~(BM & 1))&1));
                BM >>= 1;
                BE = (short) ((BE==0)?255:BE-1);
            } else {                       /* AE == BE */
                BitN = (short) (LastCarry + (AM & 1) + ((~(BM & 1))&1));
                AM >>= 1;
                BM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
                BE = (short) ((BE==0)?255:BE-1);
            }
            LastCarry = (short) ((BitN & 2) > 1? 1: 0);
            BitN = (short) (BitN & 1);
        }
        BitN = (short) (LastCarry + 1);
        LastCarry = (short) ((BitN & 2) > 1? 1: 0);
        BitN = (short) (BitN & 1);

        if (BitN == 1) {
            Rx = new Vec2(RadiusGyrMinMan, RadiusGyrMinExp);
        }

        AM = RadiusGyrMinMan;
        AE = RadiusGyrMinExp;
        BM = (short) Ry.getX();
        BE = (short) Ry.getY();
        LastCarry = 1;
        while ((AM != 0) || (BM != 0)) {
            if (AE > BE) {
                BitN = (short) (LastCarry + (AM & 1) + 1);
                AM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
            } else if (AE < BE) {
                BitN = (short) (LastCarry + ((~(BM & 1))&1));
                BM >>= 1;
                BE = (short) ((BE==0)?255:BE-1);
            } else {                       /* AE == BE */
                BitN = (short) (LastCarry + (AM & 1) + ((~(BM & 1))&1));
                AM >>= 1;
                BM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
                BE = (short) ((BE==0)?255:BE-1);
            }
            LastCarry = (short) ((BitN & 2) > 1? 1: 0);
            BitN = (short) (BitN & 1);
        }
        BitN = (short) (LastCarry + 1);
        LastCarry = (short) ((BitN & 2) > 1? 1 : 0);
        BitN = (short) (BitN & 1);

        if (BitN == 1) {
            Ry = new Vec2(RadiusGyrMinMan, RadiusGyrMinExp);
        }

        AM = RadiusGyrMaxMan;
        AE = RadiusGyrMaxExp;
        BM = (short) Rx.getX();
        BE = (short) Rx.getY();
        LastCarry = 1;
        while ((AM != 0) || (BM != 0)) {
            if (AE > BE) {
                BitN = (short) (LastCarry + (AM & 1) + 1);
                AM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
            } else if (AE < BE) {
                BitN = (short) (LastCarry + ((~(BM & 1))&1));
                BM >>= 1;
                BE = (short) ((BE==0)?255:BE-1);
            } else {                       /* AE == BE */
                BitN = (short) (LastCarry + (AM & 1) + ((~(BM & 1))&1));
                AM >>= 1;
                BM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
                BE = (short) ((BE==0)?255:BE-1);
            }
            LastCarry = (short) ((BitN & 2) > 1? 1 : 0);
            BitN = (short) (BitN & 1);
        }
        BitN = (short) (LastCarry + 1);
        LastCarry = (short) ((BitN & 2) > 1? 1 : 0);
        BitN = (short) (BitN & 1);

        if (BitN == 1) {
            RxInvLarge = 1;
        } else {
            RxInvLarge = 0;
        }
        AM = (short) Ry.getX();
        AE = (short) Ry.getY();
        BM = RadiusGyrMaxMan;
        BE = RadiusGyrMaxExp;
        LastCarry = 1;
        while ((AM != 0) || (BM != 0)) {
            if (AE > BE) {
                BitN = (short) (LastCarry + (AM & 1) + 1);
                AM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
            } else if (AE < BE) {
                BitN = (short) (LastCarry + ((~(BM & 1))&1));
                BM >>= 1;
                BE = (short) ((BE==0)?255:BE-1);
            } else {                       /* AE == BE */
                BitN = (short) (LastCarry + (AM & 1) + ((~(BM & 1))&1));
                AM >>= 1;
                BM >>= 1;
                AE = (short) ((AE==0)?255:AE-1);
                BE = (short) ((BE==0)?255:BE-1);
            }
            LastCarry = (short) ((BitN & 2) > 1? 1 : 0);
            BitN = (short) (BitN & 1);
        }
        BitN = (short) (LastCarry + 1);
        LastCarry = (short) ((BitN & 2) > 1? 1 : 0);
        BitN = (short) (BitN & 1);

        if (BitN == 1) {
            RyInvSmall = 1;
        } else {
            RyInvSmall = 0;
        }
        if (RxInvLarge != 0 && RyInvSmall != 0) {
            Ry = new Vec2(RadiusGyrMaxMan, RadiusGyrMaxExp);
        }
        return new Pair(Rx, Ry);
    }
    
    /**
     * My sqrt2.
     * 
     * @param N the n
     * @param I the i
     * 
     * @return the vec2
     */
    private static Vec2 MySqrt2(int N, int I) {
        if (N == 0 || I == 0) {
            return new Vec2(0,0);
        }
        long N2 = N * 41943;

        short k = 9;
        while ((N2 & 0xc0000000) == 0) {
            N2 <<= 2;
            k += 1;
        }

        while ((I & 0xc0000000) == 0) {
            I <<= 2;
            k -= 1;
        }

        if (((N2 & 0x80000000) == 0) && ((I & 0x80000000) == 0)) {
            N2 <<= 1;
            I <<= 1;
        }

        N2 &= 0xffff0000;
        I >>>= 14;
        int Ratio = (int) (N2 / I);

        short BitLocation = 128;
        int SqRoot = 0;
        do {
            int Square = (SqRoot | BitLocation) * (SqRoot | BitLocation);
            if (Square <= Ratio) {
                SqRoot |= BitLocation;
            }
            BitLocation >>>= 1;
        } while (BitLocation != 0);

        if (k < 0) {
            return new Vec2(255, 0);
        } else {
            return new Vec2(SqRoot, k);
        }
    }

    /**
     * Table lookup.
     * 
     * @param v the v
     * 
     * @return the short
     */
    public short TableLookup(Vec2 v) {
      int X = v.getX();
      int Y = v.getY();
      assert ((X != 0) || (Y != 0));
      int AbsX = Math.abs(X);
      int AbsY = Math.abs(Y);

      int Ratio;
      if (AbsX > AbsY)
        Ratio = Math.min(ATAN_TABLE_SIZE-1, AbsY * ATAN_TABLE_SIZE / AbsX);
      else
        Ratio = Math.min(ATAN_TABLE_SIZE-1, AbsX * ATAN_TABLE_SIZE / AbsY);

      int Angle = this.AtanTable[Ratio];
      int nBits = ((X >= 0)? 4:0) + ((Y >= 0)? 2: 0) + ((AbsX > AbsY)? 1:0);
      switch (nBits) {
          case 0: // X < 0, Y < 0, AbsX <= AbsY
              Angle = 192 - Angle;
              break;
          case 1: // X < 0, Y < 0, AbsX > AbsY
              Angle = 128 + Angle;
              break;
          case 2: // X < 0, Y >= 0, AbsX <= AbsY
              Angle = 64 + Angle;
              break;
          case 3: // X < 0, Y >= 0, AbsX > AbsY
              Angle = 128 - Angle;
              break;
          case 4: // X >= 0, Y < 0, AbsX <= AbsY
              Angle = 192 + Angle;
              break;
          case 5: // X >= 0, Y < 0, AbsX > AbsY
              Angle = 256 - Angle;
              break;
          case 6: // X >= 0, Y >= 0, AbsX <= AbsY
              Angle = 64 - Angle;
              break;
          case 7: // X >= 0, Y >= 0, AbsX > AbsY
              break;
      }

      /* reverse angles to match old feature extractor:   Angle += PI */
      Angle += 128;
      Angle &= 255;
      return (short) Angle;
    }
    
    /**
     * Int2 pair vec.
     * 
     * @param l the l
     * 
     * @return the pair< vec2, vec2>
     */
    private Pair<Vec2,Vec2> Int2PairVec(int l) {
        short s1 = (short) ((l >>> 24));
        short s2 = (short) (((l << 8) >>> 24));
        short s3 = (short) (((l << 16) >>> 24));
        short s4 = (short) (((l << 24) >>> 24));
        return new Pair<Vec2,Vec2>(new Vec2(s1,s2), new Vec2(s3,s4));
    }

    /**
     * Pair vec2 int.
     * 
     * @param pvv the pvv
     * 
     * @return the int
     */
    private int PairVec2Int(Pair<Vec2,Vec2> pvv) {
        int s1 = pvv.getFirst().getX();
        int s2 = pvv.getFirst().getY();
        int s3 = pvv.getSecond().getX();
        int s4 = pvv.getSecond().getY();
        assert s1 >= 0 && s2 <= 255 &&
                s2 >= 0 && s2 <= 255 &&
                s3 >= 0 && s3 <= 255 &&
                s4 >= 0 && s4 <= 255;
        return ((int)s1 << 24) | ((int)s2 << 16) | ((int)s3 << 8) | (int)s4;
    }
    
//	@Override
//	public void readExternal(ObjectInput oi) throws IOException,
//			ClassNotFoundException {
//		rprResult = new IntBuffer[32][];
//		int nPos = 0;
//	   	for (int i=0; i<rprResult.length; i++) {
//	   		rprResult[i] = new IntBuffer[64];
//    		for (int j=0; j<rprResult[i].length; j++) {
//    			this.mbb.position(nPos);
//    			this.mbb.limit(nPos += 64*Integer.SIZE/Byte.SIZE);
//    			rprResult[i][j] = this.mbb.slice().asIntBuffer();
//    			for (int k=0; k<rprResult[i][j].capacity(); k++) {
//    				rprResult[i][j].put(k, oi.readInt());
//    			}
//    		}
//    	}
//	   	this.mbb.position(nPos);
//	   	this.mbb.limit(nPos += ATAN_TABLE_SIZE*Short.SIZE/Byte.SIZE);
//	   	AtanTable = this.mbb.slice().asShortBuffer();
//    	for (int i=0; i<AtanTable.capacity(); i++) {
//    		AtanTable.put(i, oi.readShort());
//    	}
//    	this.mbb.position(nPos);
//    	this.mbb.limit(nPos += SE_TABLE_SIZE*Short.SIZE/Byte.SIZE);
//    	SimilarityEvidenceTable = this.mbb.slice().asShortBuffer();
//    	for (int i=0; i<SimilarityEvidenceTable.capacity(); i++) {
//    		SimilarityEvidenceTable.put(i, oi.readShort());
//    	}
// 	}
//
//	@Override
//	public void writeExternal(ObjectOutput oo) throws IOException {
//	   	for (int i=0; i<rprResult.length; i++) {
//    		for (int j=0; j<rprResult[i].length; j++) {
//    			for (int k=0; k<rprResult[i][j].capacity(); k++) {
//    				oo.writeInt(rprResult[i][j].get(k));
//    			}
//    		}
//    	}
//    	for (int i=0; i<AtanTable.capacity(); i++) {
//    		oo.writeShort(AtanTable.get(i));
//    	}
//    	for (int i=0; i<SimilarityEvidenceTable.capacity(); i++) {
//    		oo.writeShort(SimilarityEvidenceTable.get(i));
//    	}
//	}
}
