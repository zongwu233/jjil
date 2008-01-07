package jjil.android;

import java.io.IOException;
import java.io.OutputStream;

import jjil.core.RgbImage;
import android.content.Context;
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
    
    static public void toDisplay(Context context, RgbImage rgb) 
    {
    	Bitmap bmp = toBitmap(rgb);
    	
    }
    
    static public void toFile(Context context, RgbImage rgb, int nQuality, String szPath) 
    	throws IOException
    {
     	OutputStream os = context.openFileOutput(szPath, 0);
     	try {
	     	Bitmap bmp = toBitmap(rgb);
	     	Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
	     	szPath = szPath.toLowerCase();
	     	if (szPath.endsWith("jpg") || szPath.endsWith("jpeg")) {
     			format = Bitmap.CompressFormat.JPEG;
	     	} else if (szPath.endsWith("png")) {
     			format = Bitmap.CompressFormat.PNG;
     		}
	     	bmp.compress(format, nQuality, os);
     	} finally {
     		os.close();
     	}
    }
}
