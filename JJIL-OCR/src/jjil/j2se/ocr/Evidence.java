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

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import jjil.core.TimeTracker;
import jjil.core.Vec2;

/**
 * The Class Evidence.
 * 
 * @author webb
 */
public class Evidence {
    
    /** The Constant EvidenceTableBits. */
    private static final int EvidenceTableBits = 9;
    
    /** The Constant EvidenceTableMask. */
    private static final int EvidenceTableMask =
            ((1 << EvidenceTableBits) - 1) << (9 - EvidenceTableBits);
    
    /** The Constant IntEvidenceTruncBits. */
    private static final int IntEvidenceTruncBits = 14;
    
    /** The Constant SE_TABLE_BITS. */
    private static final int SE_TABLE_BITS = 9;
    
    /** The Constant TableTruncShiftBits. */
    private static final int TableTruncShiftBits = (27 - SE_TABLE_BITS);
    
    /** The Constant EvidenceMultMask. */
    private static final int EvidenceMultMask = ((1 << IntEvidenceTruncBits) - 1);
    
    /** The Constant EvidenceTableTest. */
    private static final int EvidenceTableTest = EvidenceTableMask << TableTruncShiftBits;
    
    /** The s precompute math. */
    private static PrecomputeMath sPrecomputeMath = null;
    
    /** The sv evidence. */
    Vec2 svEvidence = new Vec2(EvidenceMultMask, EvidenceMultMask);
    
    /** The mrn sum of feature evidence. */
    int mrnSumOfFeatureEvidence[];
    
    /** The mrs feature evidence. */
    short mrsFeatureEvidence[];
    
    /** The mrs proto evidence. */
    short mrsProtoEvidence[];
    
    /** The m time tracker. */
    private TimeTracker mTimeTracker;
    
    /** The m v. */
    Vec2 mV = new Vec2(0,0);
    
    /**
     * The Class Result.
     */
    public static class Result {
        
        /** The Rating. */
        int Rating = 0;
        
        /**
         * Instantiates a new result.
         */
        public Result()
        {
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Result)) {
                return false;
            }
            Result r = (Result) o;
            return Math.abs(this.Rating-r.Rating) == 0;
        }
        
        /**
         * Gets the rating.
         * 
         * @return the rating
         */
        public int getRating() {
            return this.Rating;
        }
    }

    

    /**
     * Instantiates a new evidence.
     * 
     * @param protos the protos
     * @param tt the tt
     */
    public Evidence(Prototypes protos, TimeTracker tt) {
    	this.mTimeTracker = tt;
        this.mrnSumOfFeatureEvidence = new int[Prototypes.MAX_NUM_CONFIGS];
        Arrays.fill(this.mrnSumOfFeatureEvidence, 0);
        this.mrsProtoEvidence = new short[Prototypes.MAX_NUM_PROTOS];
        Arrays.fill(mrsProtoEvidence, (short) 0);
        this.mrsFeatureEvidence = new short[protos.getNumConfigs()];
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Evidence)) {
            return false;
        }
        Evidence e = (Evidence) o;
        if (!Arrays.equals(this.mrsFeatureEvidence, e.mrsFeatureEvidence)) {
            return false;
        }
        if (!Arrays.equals(this.mrnSumOfFeatureEvidence, e.mrnSumOfFeatureEvidence)) {
            return false;
        }
        if (!Arrays.equals(this.mrsProtoEvidence, e.mrsProtoEvidence)) {
        	return false;
        }
        return true;
    }
    
    /**
     * Find best match.
     * 
     * @param protos the protos
     * @param BlobLength the blob length
     * @param r the r
     */
    private void findBestMatch(Prototypes protos, int BlobLength,
            Result r) {
      /* Find best match */
      int BestMatch = 0;
      for (int nEvidence: this.mrnSumOfFeatureEvidence) {
          if (nEvidence > BestMatch) {
              BestMatch = nEvidence;
          }
      }

      /* Compute Certainty Rating */
      r.Rating = (((65536 - BestMatch) / 256 * BlobLength) /
        (BlobLength + 14));
    }
    
    /**
     * Integer matcher.
     * 
     * @param protos the protos
     * @param BlobLength the blob length
     * @param nFeatures the n features
     * @param fs the fs
     * 
     * @return the result
     */
    public Result IntegerMatcher(Prototypes protos,
            int BlobLength,
            int nFeatures,
            List<Feature> fs) {
        Result r = new Result();
        if (this.mTimeTracker != null) {
        	this.mTimeTracker.startTask("im: update");
        }
        for (Feature f: fs) {
            updateTablesForFeature(protos, f);
        }
        if (this.mTimeTracker != null) {
        	this.mTimeTracker.endTask("im: update");
	        this.mTimeTracker.startTask("im: rest");
       }
        updateSumOfProtoEvidences(protos);
        updateSumOfProtoEvidences(protos, nFeatures);
        findBestMatch(protos, BlobLength, r);
        if (this.mTimeTracker != null) {
        	this.mTimeTracker.endTask("im: rest");
        }
        return r;
    }
    
    /**
     * Update sum of proto evidences.
     * 
     * @param protos the protos
     * @param nFeatures the n features
     */
    private void updateSumOfProtoEvidences(Prototypes protos, int nFeatures) {
        int n = 0;
        for (int nConfig = 0; nConfig < protos.getNumConfigs();
            nConfig++, n++) {
            mrnSumOfFeatureEvidence[n] =
                    (mrnSumOfFeatureEvidence[n] << 8) /
                    (nFeatures + protos.getLengthForConfigId(nConfig));
        }
    }
    
    /**
     * Update sum of proto evidences.
     * 
     * @param protos the protos
     */
    private void updateSumOfProtoEvidences(Prototypes protos) {
        int ActualProtoNum = 0;
        for (Prototype proto: protos) {
            for (int ProtoNum = 0; ProtoNum < proto.getNumIntProtos();
                ProtoNum++, ActualProtoNum++) {
                for (byte b: proto.get(ProtoNum).getConfigs()) {
                    mrnSumOfFeatureEvidence[b] += this.mrsProtoEvidence[ActualProtoNum];
                }
            }
        }
    }

    /**
     * Update tables for feature.
     * 
     * @param protos the protos
     * @param f the f
     * 
     * @return the int
     */
    private int updateTablesForFeature(Prototypes protos, Feature f) {
        Arrays.fill(this.mrsFeatureEvidence, (short) 0);
        int ActualProtoNum = 0;
        for (Prototype proto : protos) {
            BitSet bsProtoPruner[][] = proto.getProtoPruner();
            /* Prune Protos of current Proto Set */
            BitSet bsProto = (BitSet) bsProtoPruner[0][(f.getX()+128) >> 2].clone();
            bsProto.and(bsProtoPruner[1][(f.getY()+128) >> 2]);
            bsProto.and(bsProtoPruner[2][f.getTheta() >> 2]);
            
            if (!bsProto.isEmpty()) {
                for (int nProto=0; 
                    -1 != (nProto = bsProto.nextSetBit(nProto));
                    nProto++) {
                    LearntFeature lf = proto.get(nProto);
                    byte rbConfigs[] = lf.getConfigs();
                    int nX = Math.abs(lf.getA() * f.getX() - lf.getB() * f.getY() + lf.getC());
                    int nY = Math.abs((f.getTheta() - lf.getAngle()) << 8);
                    nX = Math.min(nX, svEvidence.getX());
                    nY = Math.min(nY, svEvidence.getY());
                    int nA4 = nX * nX + nY * nY;
                    short sEvidence;
                    if (nA4 > EvidenceTableTest) {
                        sEvidence = 0;
                    } else {
                    	nA4 >>= TableTruncShiftBits;
                        sEvidence = sPrecomputeMath.SimilarityEvidenceTable[nA4];
                    }
                    for (byte b: rbConfigs) {
                    	mrsFeatureEvidence[b] = (short)
                    		Math.max(sEvidence, mrsFeatureEvidence[b]);
                    }
                    
                    int nActualProto = nProto + ActualProtoNum;
                    this.mrsProtoEvidence[nActualProto] += sEvidence;
                }

            }
            ActualProtoNum += Prototype.PROTOS_PER_PROTO_SET;
        }

        int SumOverConfigs = 0;
        for (int i = 0; i < mrsFeatureEvidence.length; i++) {
            short evidence = mrsFeatureEvidence[i];
            SumOverConfigs += evidence;
            mrnSumOfFeatureEvidence[i] += evidence;
        }
        return SumOverConfigs;
    }
    
    /**
     * Sets the precompute math.
     * 
     * @param pm the new precompute math
     */
    public static void setPrecomputeMath(PrecomputeMath pm) {
        sPrecomputeMath = pm;
    }
}
