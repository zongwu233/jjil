package jjil.debug;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRenderedImage;

import jjil.core.RgbImage;

public class Debug implements Show {
	private static Show show;
	
	public static void setShow(Show show) {
		Debug.show = show;
	}
	
	public synchronized void toDisplay(RgbImage rgb) {
		if (Debug.show != null) {
			Debug.show.toDisplay(rgb);
		}
	}
	
	public void toFile(RgbImage rgb, String szFilename) {
		if (Debug.show != null) {
			Debug.show.toFile(rgb, szFilename);
		}
	}
}
