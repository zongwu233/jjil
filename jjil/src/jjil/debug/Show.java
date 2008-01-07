package jjil.debug;

import jjil.core.RgbImage;

public interface Show {
	void toDisplay(RgbImage rgb);
	
	void toFile(RgbImage rgb, String szFilename);
}
