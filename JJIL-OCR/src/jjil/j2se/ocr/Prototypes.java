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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class Prototypes.
 */
public class Prototypes extends ArrayList<Prototype> {
	
	/** The Constant MAX_PROTO_INDEX. */
	public static final int MAX_PROTO_INDEX = 24;
	
	/** The Constant MAX_NUM_PROTOS. */
	public static final int MAX_NUM_PROTOS = 512;
	
	/** The Constant NO_PROTO. */
	private static final int NO_PROTO = -1;
	
	/** The Constant MAX_NUM_PROTO_SETS. */
	private static final int MAX_NUM_PROTO_SETS = (MAX_NUM_PROTOS / Prototype.PROTOS_PER_PROTO_SET);
	
	/** The Constant BITS_PER_WERD. */
	public final static int BITS_PER_WERD = 8 * 4;
	
	/** The Constant MAX_NUM_CONFIGS. */
	public final static int MAX_NUM_CONFIGS = 32;
	
	/** The Constant CP_CUTOFF_STRENGTH. */
	private static final int CP_CUTOFF_STRENGTH = 7;

	/** The Constant ANGLE_SHIFT. */
	public static final float ANGLE_SHIFT = 0.0f;
	
	/** The Constant PICO_FEATURE_LENGTH. */
	public static final float PICO_FEATURE_LENGTH = 0.05f;
	
	/** The Constant X_SHIFT. */
	public static final float X_SHIFT = 0.5f;
	
	/** The Constant Y_SHIFT. */
	public static final float Y_SHIFT = 0.5f;
	
	/** The Constant PPAnglePad. */
	public static final float PPAnglePad = 45.0f;
	
	/** The Constant PPEndPad. */
	public static final float PPEndPad = 0.5f;
	
	/** The Constant PPSidePad. */
	public static final float PPSidePad = 2.5f;

	/** The m ch. */
	private char mCh;
	
	/** The Num protos. */
	private int NumProtos = 0; // actually unsigned short
	
	/** The Num configs. */
	private short NumConfigs = 0; // actually unsigned byte
	
	/** The Proto lengths. */
	private short ProtoLengths[] = new short[MAX_NUM_PROTO_SETS
			* Prototype.PROTOS_PER_PROTO_SET];
	
	/** The Config lengths. */
	private int ConfigLengths[] = new int[MAX_NUM_CONFIGS]; // actually unsigned
															// short
	/** The mn cutoff. */
															private int mnCutoff;
	
	/** The mrb pruner. */
	private byte mrbPruner[][][];

	{
		Arrays.fill(this.ProtoLengths, (short) 0);
		Arrays.fill(this.ConfigLengths, 0);
	}

	/**
	 * Instantiates a new prototypes.
	 * 
	 * @param MaxNumProtos the max num protos
	 * @param MaxNumConfigs the max num configs
	 */
	public Prototypes(int MaxNumProtos, int MaxNumConfigs) {
		int nProtoSets = ((MaxNumProtos + Prototype.PROTOS_PER_PROTO_SET - 1) / Prototype.PROTOS_PER_PROTO_SET);
		this.ensureCapacity(nProtoSets);
		for (int i = 0; i < nProtoSets; i++) {
			this.add(new Prototype());
		}
		Arrays.fill(this.ProtoLengths, (short) 0);
	}

	/**
	 * Instantiates a new prototypes.
	 * 
	 * @param chUnichar the ch unichar
	 * @param nCutoff the n cutoff
	 * @param fis the fis
	 * @param bo the bo
	 * @param nVersionId the n version id
	 * @param lPruners the l pruners
	 * @param nPruner the n pruner
	 */
	public Prototypes(char chUnichar, int nCutoff, FileInputStream fis,
			ByteOrder bo, int nVersionId, List<byte[][][][]> lPruners,
			int nPruner) {
		this.mCh = chUnichar;
		this.mnCutoff = nCutoff;
		try {
			final int nIntByteSize = Integer.SIZE / Byte.SIZE;
			final int nShortByteSize = Short.SIZE / Byte.SIZE;
			ByteBuffer bb = ByteBuffer.allocate(nShortByteSize + 2);
			bb.order(bo);
			fis.read(bb.array());
			this.NumProtos = bb.getShort(0);
			short NumProtoSets = LearntFeature.unsignedByteToShort(bb
					.get(nShortByteSize));
			this.NumConfigs = LearntFeature.unsignedByteToShort(bb
					.get(nShortByteSize + 1));
			if (nVersionId == 0) {
				// Only version 0 writes 5 pointless pointers to the file.
				// skip 5 ints
				bb = ByteBuffer.allocate(5 * nIntByteSize);
				fis.read(bb.array());
			}
			bb = ByteBuffer.allocate(MAX_NUM_CONFIGS * nShortByteSize);
			bb.order(bo);
			fis.read(bb.array());
			ShortBuffer sb = bb.asShortBuffer();
			for (int i = 0; i < this.ConfigLengths.length; i++) {
				this.ConfigLengths[i] = sb.get(i);
			}
			this.ProtoLengths = new short[NumProtoSets
					* Prototype.PROTOS_PER_PROTO_SET];
			Arrays.fill(this.ProtoLengths, (short) 0);
			bb = ByteBuffer.allocate(NumProtoSets
					* Prototype.PROTOS_PER_PROTO_SET);
			bb.order(bo);
			fis.read(bb.array());
			for (int i = 0; i < this.NumProtos; i++) {
				this.ProtoLengths[i] = LearntFeature.unsignedByteToShort(bb.get(i));
			}
			int nProtosLeft = this.NumProtos;
			for (int i = 0; i < NumProtoSets; i++) {
				this.add(new Prototype(fis, bo, Math.min(
						Prototype.PROTOS_PER_PROTO_SET, nProtosLeft)));
				nProtosLeft -= Prototype.PROTOS_PER_PROTO_SET;
			}
		} catch (IOException ex) {
			Logger.getLogger(Prototypes.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		this.mrbPruner = new byte[PrototypesCollection.NUM_CP_BUCKETS][PrototypesCollection.NUM_CP_BUCKETS][PrototypesCollection.NUM_CP_BUCKETS];
		int nPrunerBucket = nPruner / PrototypesCollection.CLASSES_PER_CP;
		int nPrunerIndex = nPruner % PrototypesCollection.CLASSES_PER_CP;
		for (int i = 0; i < PrototypesCollection.NUM_CP_BUCKETS; i++) {
			for (int j = 0; j < PrototypesCollection.NUM_CP_BUCKETS; j++) {
				for (int k = 0; k < PrototypesCollection.NUM_CP_BUCKETS; k++) {
					this.mrbPruner[i][j][k] = lPruners.get(nPrunerBucket)[i][j][k][nPrunerIndex];
				}
			}
		}
	}

	/**
	 * This adds a newly created Prototype to the collection.
	 * 
	 * @param ps new Prototype to add
	 * 
	 * @return true iff successfully added
	 */
	@Override
	public boolean add(Prototype ps) {
		if (this.size() >= MAX_NUM_PROTO_SETS) {
			return false;
		}
		// to preserve consistency with the design of this object in the Tess
		// code we require all previous ProtoSets to be completely full.
		if (this.size() > 0
				&& super.get(this.size() - 1).getNumIntProtos() != Prototype.PROTOS_PER_PROTO_SET) {
			return false;
		}
		super.add(ps);
		// this.NumProtos += ps.getNumIntProtos();
		return true;
	}

	/**
	 * This routine returns the index of the next free config in Class.
	 * 
	 * @return Index of next free config.
	 */
	public int AddIntConfig() {
		assert this.NumConfigs < MAX_NUM_CONFIGS;
		int n = this.NumConfigs;
		this.NumConfigs++;
		this.ProtoLengths[n] = 0;
		return n;
	}

	/**
	 * Add a new LearntFeature to the current class.
	 * 
	 * @return the int
	 */
	public int AddIntProto() {
		return AddIntProto(new LearntFeature());
	}

	/**
	 * Adds the int proto.
	 * 
	 * @param ip the ip
	 * 
	 * @return the int
	 */
	public int AddIntProto(LearntFeature ip) {
		if (this.NumProtos >= MAX_NUM_PROTOS) {
			return NO_PROTO;
		}
		int nP = this.NumProtos;
		this.NumProtos++;
		while (this.NumProtos > MaxNumIntProtos()) {
			Prototype ps = new Prototype();
			super.add(ps);
		}
		this.ProtoLengths[nP] = 0;
		this.get(SetForProto(nP)).add(ip);
		return nP;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractList#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Prototypes)) {
			return false;
		}
		Prototypes other = (Prototypes) o;
		return this.NumProtos == other.NumProtos
				&& super.equals((ArrayList<Prototype>) other);
	}

	/**
	 * Gets the length for config id.
	 * 
	 * @param nConfig the n config
	 * 
	 * @return the length for config id
	 */
	int getLengthForConfigId(int nConfig) {
		return this.ConfigLengths[nConfig];
	}

	/**
	 * Gets the num configs.
	 * 
	 * @return the num configs
	 */
	int getNumConfigs() {
		return this.NumConfigs;
	}

	/**
	 * Gets the num protos.
	 * 
	 * @return the num protos
	 */
	int getNumProtos() {
		return this.NumProtos;
	}

	/**
	 * Gets the proto length.
	 * 
	 * @param nProto the n proto
	 * 
	 * @return the proto length
	 */
	int getProtoLength(int nProto) {
		return this.ProtoLengths[nProto];
	}

	/**
	 * Gets the unichar.
	 * 
	 * @return the unichar
	 */
	public char getUnichar() {
		return this.mCh;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractList#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 41 * hash + this.NumProtos;
		hash = 41 * hash + this.NumConfigs;
		hash = 41
				* hash
				+ (this.ProtoLengths != null ? this.ProtoLengths.hashCode() : 0);
		hash = 41
				* hash
				+ (this.ConfigLengths != null ? this.ConfigLengths.hashCode()
						: 0);
		return hash;
	}

	/**
	 * Max num int protos.
	 * 
	 * @return the int
	 */
	private int MaxNumIntProtos() {
		return this.size() * Prototype.PROTOS_PER_PROTO_SET;
	}

	/**
	 * Sets the for proto.
	 * 
	 * @param P the p
	 * 
	 * @return the int
	 */
	private int SetForProto(int P) {
		return P / Prototype.PROTOS_PER_PROTO_SET;
	}

	/**
	 * Could match.
	 * 
	 * @param nfs the nfs
	 * 
	 * @return true, if successful
	 */
	public boolean couldMatch(Features nfs) {
		int nPrunerCount = 0;
		for (Feature nf : nfs) {
			// indexing of nf.getX() etc should offset whatever
			nPrunerCount += this.mrbPruner[((nf.getX() + 128) * PrototypesCollection.NUM_CP_BUCKETS) >> 8][((nf
					.getY() + 128) * PrototypesCollection.NUM_CP_BUCKETS) >> 8][(nf
					.getTheta() * PrototypesCollection.NUM_CP_BUCKETS) >> 8];
		}
		int nFeatures = nfs.size();
		if (nFeatures < this.mnCutoff) {
			int nDeficit = this.mnCutoff - nFeatures;
			nPrunerCount -= nPrunerCount * nDeficit
					/ (nFeatures * CP_CUTOFF_STRENGTH + nDeficit);

		}
		if (this.mCh == '1') {
			return nPrunerCount > 100;
		} else {
			return nPrunerCount > 125;
		}
	}
}
