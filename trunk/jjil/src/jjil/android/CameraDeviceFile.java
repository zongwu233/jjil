package jjil.android;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import jjil.algorithm.RgbShrink;
import jjil.algorithm.RgbStretch;
import jjil.core.RgbImage;
import android.content.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.CameraDevice.CaptureParams;

public class CameraDeviceFile {
	private class BitmapKey {
		private int nFile;
		private int nHeight;
		private int nWidth;
		
		public BitmapKey(int nFile, int nWidth, int nHeight) {
			this.nFile = nFile;
			this.nWidth = nWidth;
			this.nHeight = nHeight;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof BitmapKey)) {
				return false;
			}
			BitmapKey bk = (BitmapKey) o;
			return this.nFile == bk.nFile && 
				this.nHeight == bk.nHeight && 
				this.nWidth == bk.nWidth;
		}
		
		public int hashCode() {
			return this.nFile * 1000 + this.nHeight * this.nWidth;
		}
		
		public String toString() {
			return super.toString() + 
				"(" + this.nFile + Messages.getString("CameraDeviceFile.1") +  //$NON-NLS-1$ //$NON-NLS-2$
					this.nWidth + Messages.getString("CameraDeviceFile.2") +  //$NON-NLS-1$
					this.nHeight + ")"; //$NON-NLS-1$
		}
	}
	private CaptureParams captParams;
	private int [] files;
	private Map<BitmapKey, Bitmap> map = new HashMap<BitmapKey, Bitmap>();
	private int nFile = -1;
	private Resources r;
	
	private CameraDeviceFile(Resources r, int[] files) {
		this.r = r;
		this.files = files;
	}
	
	
	public boolean capture(Canvas canvas) {
		if (++this.nFile >= this.files.length) {
			this.nFile = 0;
		}
		Bitmap bmp;
		BitmapKey key = new BitmapKey(this.nFile, this.captParams.outputWidth, this.captParams.outputHeight);
		if (this.map.containsKey(key)) {
			bmp = this.map.get(key);
		} else {			
			InputStream is = this.r.openRawResource(this.files[this.nFile]);
			bmp = BitmapFactory.decodeStream(is);
			if (this.captParams.outputHeight != bmp.height() ||
				this.captParams.outputWidth != bmp.width()) {
				RgbImage rgb = RgbImageAndroid.toRgbImage(bmp);
				// First stretch the image if necessary
				if (this.captParams.outputHeight > bmp.height() || 
						this.captParams.outputWidth > bmp.width()) {
					RgbStretch rs = new RgbStretch(
							Math.max(this.captParams.outputWidth,
									bmp.width()), 
							Math.max(this.captParams.outputHeight,
									bmp.height()));
					rs.Push(rgb);
					rgb = (RgbImage) rs.Front();
				}
				// Now shrink the image if necessary
				if (this.captParams.outputHeight < bmp.height() ||
						this.captParams.outputWidth < bmp.width()) {
					RgbShrink rsh = new RgbShrink(
							this.captParams.outputWidth, 
							this.captParams.outputHeight);
					rsh.Push(rgb);
				}
				bmp = RgbImageAndroid.toBitmap(rgb);
			}
			this.map.put(key, bmp);
		}
		canvas.drawBitmap(
				bmp, 
				new Rect(0, 0, bmp.width(), bmp.height()),
				new Rect(0, 0, canvas.getBitmapWidth(), canvas.getBitmapHeight()),
				new Paint());
		return true;
	}
	
	public final void close() {
		
	}
	
	public final static CameraDeviceFile open(Resources r, int[] files) {
		return new CameraDeviceFile(r, files);
	}
	
	public boolean setCaptureParams(CaptureParams captParams) {
		this.captParams = captParams;
		return true;
	}
}
