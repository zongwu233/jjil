package jjil.debug;


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
