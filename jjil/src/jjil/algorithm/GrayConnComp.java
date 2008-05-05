/*
 * GrayConnComp.java
 *
 * Created on September 9, 2006, 10:25 AM
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

import jjil.core.Error;
import jjil.core.Gray16Image;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.Point;
import jjil.core.Rect;
import jjil.core.RgbImage;
import jjil.core.Sequence;

/**
 * Gray connected components. Input is a gray image. Pixels with value
 * Byte.MIN_VALUE are taken to be background. Other pixels are labeled with
 * unique labels. The connected component image can be retrieved, as can the
 * connected component bounding rectangles, sorted by area.
 * 
 * @author webb
 */
public class GrayConnComp {
	private class Label implements ComparableJ2me {
		private int nPixelCount = 0;
		private Rect rectBounding;

		public Label(Point p) {
			this.rectBounding = new Rect(p);
			this.nPixelCount = 1;
		}

		public void add(Point p) {
			this.rectBounding.add(p);
			this.nPixelCount++;
		}

		public int compareTo(Object o) {
			if (o == null) {
				throw new RuntimeException(
		            new Error(
		        		Error.PACKAGE.ALGORITHM,
		        		ErrorCodes.CONN_COMP_LABEL_COMPARETO_NULL,
		        		null,
		        		null,
		        		null));
			}
			if (!(o instanceof Label)) {
				throw new RuntimeException(
			        new Error(
				    	Error.PACKAGE.ALGORITHM,
				    	ErrorCodes.OBJECT_NOT_EXPECTED_TYPE,
				    	o.toString(),
				    	Label.class.toString(),
				    	null));
			}
			Label l = (Label) o;
			if (l.nPixelCount == this.nPixelCount) {
				return 0;
			}
			return (l.nPixelCount < this.nPixelCount) ? -1 : 1;
		}

		public Rect rect() {
			return this.rectBounding;
		}
	}

	// class variables
	private final int nReducedHeight = 64;
	private final int nReducedWidth = 64;
	private boolean bComponents = false;
	private Gray16Image imLabeled = null;
	private int nSortedLabels = -1;
	private PriorityQueue pqLabels = null;
	private EquivalenceClass reClasses[];
	private short sClasses = 0;
	private Label rSortedLabels[] = null;

	/**
	 * Creates a new instance of GrayConnComp.
	 * 
	 */
	public GrayConnComp() {
	}

	/**
	 * Returns the nComponent'th bounding rectangle in order by size. Sorts only
	 * as many components as are necessary to reach the requested component.
	 * Does this by observing the state of rSortedLabels. If it is null, it has
	 * to be allocated. If the nComponent'th element is null, more need to be
	 * copied from pqLabels. This is done by copying and deleting the minimum
	 * element until we reach the requested component.
	 * 
	 * @param nComponent:
	 *            the number of the component to return.
	 * @return the nComponent'th bounding rectangle, ordered by pixel count,
	 *         largest first
	 */
	public Rect getComponent(int nComponent) {
		// see if we've created the sorted labels array
		// allocate it if we haven't.
		// If the components haven't been computed getComponents()
		// will compute them.
		if (this.rSortedLabels == null) {
			this.rSortedLabels = new Label[getComponents()];
			this.nSortedLabels = -1;
		}
		// see if the requested component is out of bounds
		if (nComponent >= this.rSortedLabels.length) {
			throw new RuntimeException(
			        new Error(
					    	Error.PACKAGE.ALGORITHM,
					    	ErrorCodes.CONN_COMP_LABEL_OUT_OF_BOUNDS,
					    	new Integer(nComponent).toString(),
					    	this.rSortedLabels.toString(),
					    	null));
		}
		// now see if we've figured out what the nComponent'th
		// component is. If not compute it by finding and
		// deleting min until we reach it.
		if (this.nSortedLabels < nComponent) {
			while (this.nSortedLabels <= nComponent) {
				this.rSortedLabels[++this.nSortedLabels] = (Label) this.pqLabels
						.findMin();
				this.pqLabels.deleteMin();
			}
		}
		return rSortedLabels[nComponent].rect();
	}

	/**
	 * Get the number of connected components in the labeled image. Computes the
	 * components from the labeled image if necessary.
	 * 
	 * @return the number of connected components.
	 */
	public int getComponents() {
		// see if we've already calculated the components
		if (this.bComponents) {
			return this.pqLabels.size();
		}
		// no, we need to calculate it. Get the labeled image,
		// assigning final labels.
		getLabeledImage();
		// now determine the pixel count and bounding rectangle
		// of all the components in the image
		short sData[] = this.imLabeled.getData();
		Label vLabels[] = new Label[this.sClasses];
		int nComponents = 0;
		for (int i = 0; i < this.imLabeled.getHeight(); i++) {
			int nRow = i * this.imLabeled.getWidth();
			for (int j = 0; j < this.imLabeled.getWidth(); j++) {
				if (sData[nRow + j] != 0) {
					int nLabel = sData[nRow + j];
					// has this label been seen before?
					if (vLabels[nLabel] == null) {
						// no, create a new label
						vLabels[nLabel] = new Label(new Point(j, i));
						nComponents++;
					} else {
						// yes, extend its bounding rectangle
						vLabels[nLabel].add(new Point(j, i));
					}
				}
			}
		}
		// set up priority queue
		// first create a new array of the labels
		// with all the null elements eliminated
		Label vCompressLabels[] = new Label[nComponents];
		int j = 0;
		for (int i = 0; i < vLabels.length; i++) {
			if (vLabels[i] != null) {
				vCompressLabels[j++] = vLabels[i];
			}
		}
		// now create the priority queue from the array
		this.pqLabels = new BinaryHeap(vCompressLabels);
		// clear the sorted labels array, it has to be recomputed.
		this.rSortedLabels = null;
		// we're done
		this.bComponents = true;
		return this.pqLabels.size();
	}

	/**
	 * Get the labeled image
	 * 
	 * @return a Gray16Image with final labels assigned to every pixel
	 */
	public Gray16Image getLabeledImage() {
		return this.imLabeled;
	}

	/**
	 * Compute connected components of input gray image using a union-find
	 * algorithm.
	 * 
	 * @param image
	 *            the input image.
	 * @throws IllegalArgumentException
	 *             if the image is not a gray 8-bit image.
	 */
	public void Push(Image image) throws IllegalArgumentException {
		if (!(image instanceof Gray8Image)) {
			throw new IllegalArgumentException(
			    new Error(
				   	Error.PACKAGE.ALGORITHM,
				   	ErrorCodes.IMAGE_NOT_GRAY8IMAGE,
				   	image.toString(),
				   	null,
				   	null));
		}
		// initialize the label lookup array
		this.reClasses = new EquivalenceClass[image.getWidth() * image.getHeight()];

		// note that we've not computed the final labels or
		// the sorted components yet
		this.bComponents = false;

		Gray8Image gray = (Gray8Image) image;
		byte[] bData = gray.getData();
		// for each pixel in the input image assign a label,
		// performing equivalence operations when two labels
		// are adjacent (8-connected)
		for (int i = 0; i < gray.getHeight(); i++) {
			int nRow = i * gray.getWidth();
			// we use sUpLeft to refer to the pixel up and to
			// the left of the current, etc.
			EquivalenceClass eUpLeft = null, eUp = null, eUpRight = null;
			// after first row, initialize pixels above and
			// to the right
			if (i > 0) {
				eUp = reClasses[nRow - gray.getWidth()];
				eUpRight = reClasses[nRow - gray.getWidth() + 1];
			}
			// starting a new row the pixel to the left is 0
			EquivalenceClass eLeft = null;
			// nBitPatt encodes the state of the pixels around the
			// current pixel. The pattern is
			// 8 4 2
			// 1 current
			int nBitPatt = ((eUp != null) ? 4 : 0)
					+ ((eUpRight != null) ? 2 : 0);
			// (at left column eLeft and eUpLeft will always be 0)
			for (int j = 0; j < gray.getWidth(); j++) {
				if (bData[nRow + j] != Byte.MIN_VALUE) {
					switch (nBitPatt) {
					// the cases below are derived from the bit
					// pattern illustrated above. The general
					// rule is to choose the most recently-scanned
					// label when copying a label. Of course, we
					// also do unions only as necessary
					case 0:
						// 0 0 0
						// 0 X
						reClasses[nRow + j] = 
							new EquivalenceClass();
						this.sClasses ++;
						break;
					case 1:
						// 0 0 0
						// X X
						reClasses[nRow + j] = eLeft.find();
						break;
					case 2:
						// 0 0 X
						// 0 X
						reClasses[nRow + j] = eUpRight.find();
						break;
					case 3:
						// 0 0 X
						// X X
						eLeft.union(eUpRight);
						reClasses[nRow + j] = eLeft.find();
						break;
					case 4:
						// 0 X 0
						// 0 X
						reClasses[nRow + j] = eUp.find();
						break;
					case 5:
						// 0 X 0
						// X X
						// we must already have union'ed
						// eLeft and eUp
						reClasses[nRow + j] = eLeft.find();
						break;
					case 6:
						// 0 X X
						// 0 X
						// we must already have union'ed
						// eUp and eUpRight
						reClasses[nRow + j] = eUpRight.find();
						break;
					case 7:
						// 0 X X
						// X X
						// we must already have union'ed
						// eLeft and eUp, and eUp and eUpRight
						reClasses[nRow + j] = eLeft.find();
						break;
					case 8:
						// X 0 0
						// 0 X
						reClasses[nRow + j] = eUpLeft.find();
						break;
					case 9:
						// X 0 0
						// X X
						// we must already have union'ed
						// eLeft and eUpLeft
						reClasses[nRow + j] = eLeft.find();
						break;
					case 10:
						// X 0 X
						// 0 X
						eUpLeft.union(eUpRight);
						reClasses[nRow + j] = eUpLeft.find();
						break;
					case 11:
						// X 0 X
						// X X
						// we must already have union'ed
						// eLeft and eUpLeft
						eLeft.union(eUpRight);
						reClasses[nRow + j] = eLeft.find();
						break;
					case 12:
						// X X 0
						// 0 X
						// we must already have union'ed
						// eUpLeft and eUp
						reClasses[nRow + j] = eUp.find();
						break;
					case 13:
						// X X 0
						// X X
						// we must already have union'ed
						// eLeft and eUpLeft, and eUpLeft and eUp
						reClasses[nRow + j] = eLeft.find();
						break;
					case 14:
						// X X X
						// 0 X
						// we must already have union'ed
						// eUpLeft, eUp, and eUpRight
						reClasses[nRow + j] = eUpRight.find();
						break;
					case 15:
						// X X X
						// X X
						// we must already have union'ed
						// eLeft, eUpLeft, eUp, and eUpRight
						reClasses[nRow + j] = eLeft.find();
						break;
					}
				}
				// shift right to next pixel
				eUpLeft = eUp;
				eUp = eUpRight;
				eLeft = reClasses[nRow + j];
				// if we're not at the right column and after the first
				// row read a new right pixel
				if (i > 0 && j < gray.getWidth() - 1) {
					eUpRight = reClasses[nRow - gray.getWidth() + j + 2];
				} else {
					eUpRight = null;
				}

				// compute the new bit pattern. This is the old pattern
				// with eUpLeft and eLeft and'ed off (& 6), shifted left,
				// with the new eLeft and eUpRight or'ed in
				nBitPatt = ((nBitPatt & 6) << 1) + ((eLeft != null) ? 1 : 0)
						+ ((eUpRight != null) ? 2 : 0);
			}
		}
		// initialize the labeled image
		this.imLabeled = new Gray16Image(gray.getWidth(), gray.getHeight(),
				(short) 0);
		short[] sLabels = this.imLabeled.getData();
		// assign label pixels their final values
		for (int i = 0; i < sLabels.length; i++) {
			if (reClasses[i] != null) {
				sLabels[i] = (short) reClasses[i].getLabel();
			}
		}
		// free memory for reClasses
		this.reClasses = null;
	}
}
