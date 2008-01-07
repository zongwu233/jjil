package barcode;

import java.awt.Rectangle;

import jjil.algorithm.Gray2Rgb;
import jjil.algorithm.GrayAbs;
import jjil.algorithm.GrayConnComp;
import jjil.algorithm.GrayHistEq;
import jjil.algorithm.GrayHorizSimpleEdge;
import jjil.algorithm.GrayThreshold;
import jjil.algorithm.RgbAvg2Gray;
import jjil.algorithm.RgbSubSample;
import jjil.core.Image;
import jjil.core.Rect;
import jjil.core.RgbImage;
import jjil.core.Sequence;
import jjil.debug.Debug;

public class DetectBarcode {
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
	 * Returns rectangle detected by the Push operation below.
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
	 * @param rgb: the input RgbImage
	 * @return true iff a barcode appeared to have been found.
	 */
	public boolean Push(RgbImage rgb) {
		final int nReducedHeight = 64;
		final int nReducedWidth = 64;
		// build pipeline
		Sequence seq = new Sequence();
		seq.Add(new RgbSubSample(nReducedWidth, nReducedHeight));
		seq.Add(new RgbAvg2Gray());
		seq.Add(new GrayHorizSimpleEdge());
		seq.Add(new GrayAbs());
		seq.Add(new GrayHistEq());
		seq.Add(new GrayThreshold(88));
		seq.Push(rgb);
		GrayConnComp gcc = new GrayConnComp();
		Image imThresh = seq.Front();
		Gray2Rgb g2r = new Gray2Rgb();
		g2r.Push(imThresh);
		Debug debug = new Debug();
		debug.toFile((RgbImage)g2r.Front(), "test.jpg");
		gcc.Push(imThresh);
		if (gcc.getComponents() == 0) {
			return false;
		}
		Rect rReduced = gcc.getComponent(0);
		// we detected the barcode at reduced resolution
		// for speed. Stretch the rectangle back to its
		// original size
		this.rDetected = new Rect(
				(rReduced.getLeft() * rgb.getWidth()) / nReducedWidth,
				(rReduced.getTop() * rgb.getHeight()) / nReducedHeight,
				(rReduced.getWidth() * rgb.getWidth()) / nReducedWidth,
				(rReduced.getHeight() * rgb.getHeight()) / nReducedHeight
				);
		if (this.rDetected.getArea() < this.nMinArea) {
			return false;
		}
		ReadBarcode rb = new ReadBarcode();
		rb.Push(
				rgb, 
				rDetected.getLeft() + 60, 
				rDetected.getTop() + 80, 
				rDetected.getWidth() - 100, 
				rDetected.getHeight() - 200);
		if (!rb.getSuccessful()) {
			return false;
		}
		return true;
	}
	
}
