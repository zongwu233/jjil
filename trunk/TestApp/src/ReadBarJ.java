import java.awt.Frame;
import java.awt.Image;

import jjil.debug.Debug;
import jjil.j2se.RgbImageJ2se;
import barcode.DetectBarcode;
import barcode.ReadBarcode;

class ReadBarJ extends Frame
{  
	  public static void main(String argv[])
	  {
	    new ReadBarJ();
	  }
	  ReadBarJ() {
		this.setVisible(true);
		RgbImageJ2se debug = new RgbImageJ2se(this.getGraphics());
		Debug.setShow(debug);
		Image image = getToolkit().getImage("c:\\images\\barcode.jpg");
		jjil.core.RgbImage inimg = RgbImageJ2se.toRgbImage(image);
		DetectBarcode db = new DetectBarcode(20000);
		if (!db.Push(inimg)) {
			/**
			 * Couldn't find the barcode. Tell the user.
			 */
		} else {
			ReadBarcode rb = new ReadBarcode();
			rb.setRect(db.getRect());
			rb.Push(inimg);
			if (!rb.getSuccessful()) {
				/**
				 * Couldn't read the barcode.
				 */
			} else {
				/**
				 * Read the barcode. Tell the user.
				 */
			}
		}
	  }
}