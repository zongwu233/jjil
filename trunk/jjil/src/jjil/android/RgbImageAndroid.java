package jjil.android;

import jjil.core.RgbImage;
import android.graphics.Bitmap;

public class RgbImageAndroid  {
    static public RgbImage toRgbImage(Bitmap bmp) {
    	RgbImage rgb = new RgbImage(bmp.width(), bmp.height());
    	bmp.getPixels(
    			rgb.getData(), 
    			0, 
    			rgb.getWidth(), 
    			0, 
    			0, 
    			rgb.getWidth(), 
    			rgb.getHeight());
    	return rgb;
    }

    static public Bitmap toBitmap(RgbImage rgb)
    {
    	return Bitmap.createBitmap(
    			rgb.getData(), 
    			rgb.getWidth(), 
    			rgb.getHeight(), 
    			false);
    }
    
}
