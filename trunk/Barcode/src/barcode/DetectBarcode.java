package barcode;


import jjil.algorithm.Gray16Threshold;
import jjil.algorithm.Gray2Rgb;
import jjil.algorithm.GrayConnComp;
import jjil.algorithm.GrayHistEq;
import jjil.algorithm.GrayHorizVertContrast;
import jjil.algorithm.RgbAvg2Gray;
import jjil.algorithm.RgbSubSample;
import jjil.core.Image;
import jjil.core.Rect;
import jjil.core.RgbImage;
import jjil.core.Sequence;
import jjil.debug.Debug;

/**
 * Detects a barcode in an image and returns the rectangle where the barcode lies. 
 * Applies a series of heuristic tests to find the barcode.
 */
public class DetectBarcode {
	private static final boolean bDebug = true;
	int nMinArea;
	Rect rDetected;
	
	/**
	 * Construct a new DetectBarcode operation.
	 * @param nMinArea the minimum barcode area in pixels.
	 */
	public DetectBarcode(int nMinArea) {
		this.nMinArea = nMinArea;
	}
	
	/**
	 * Returns rectangle detected by the push operation below.
	 * @return a rectangle outlining the barcode in the image.
	 */
	public Rect getRect() {
		return this.rDetected;
	}
	
	/**
     * Accepts an RgbImage and tries to detect a barcode in it.
     * It doesn't read the barcode. The barcode is searched for
     * as an area where there are lots of vertical edges. 
     * The detection is done at low resolution for speed.
     * <br>
     * The minimum area of the barcode is set in pixels in the
     * constructor. The barcode position is saved and can be
     * retrieved through getRect.
     * @param rgb Input image.
     * @return true iff a barcode appeared to have been found.
     * @throws jjil.core.Error Should not throw this except in the case of coding error.
     */
	public boolean push(RgbImage rgb) throws jjil.core.Error {
		final int nReducedHeight = 240;
		final int nReducedWidth = 320;
		// build pipeline
		Sequence seq = new Sequence();
		seq.add(new RgbSubSample(nReducedWidth, nReducedHeight));
		seq.add(new RgbAvg2Gray());
		seq.add(new GrayHistEq());
		seq.add(new GrayHorizVertContrast(4, 2, -8, 3));
		seq.add(new Gray16Threshold(200));
		seq.push(rgb);
		GrayConnComp gcc = new GrayConnComp();
		Image imThresh = seq.getFront();
		Gray2Rgb g2r = new Gray2Rgb();
		g2r.push(imThresh);
		if (DetectBarcode.bDebug) {
			Debug debug = new Debug();
			debug.toFile((RgbImage)g2r.getFront(), "test.jpg");
		}
		gcc.push(imThresh);
		if (gcc.getComponents() == 0) {
			return false;
		}
		// get component which is largest than the minimum size and
		// closest to a 4/3 ratio of width to height
		Rect rReduced;
		int nBestDiff = Integer.MAX_VALUE;
		this.rDetected = null;
		for (int i=0; i<gcc.getComponents(); i++) {
			rReduced = gcc.getComponent(0);
			// we detected the barcode at reduced resolution
			// for speed. Stretch the rectangle back to its
			// original size
			Rect rThisDetected = new Rect(
					(rReduced.getLeft() * rgb.getWidth()) / nReducedWidth,
					(rReduced.getTop() * rgb.getHeight()) / nReducedHeight,
					(rReduced.getWidth() * rgb.getWidth()) / nReducedWidth,
					(rReduced.getHeight() * rgb.getHeight()) / nReducedHeight
					);
			if (rThisDetected.getArea() >= this.nMinArea) {
				int nRatio = 3 * rThisDetected.getWidth() / rThisDetected.getHeight();
				// the ratio should be close to 4 since in the ideal case
				// width = 4x and height = 3x so 3 * width / height = 4
				int nDiff = Math.abs(nRatio-4);
				if (nDiff < nBestDiff) {
					this.rDetected = rThisDetected;
					nBestDiff = nDiff;
				}
			} else {
				break;
			}
		}
		return this.rDetected != null;
	}
	
}
