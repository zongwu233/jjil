import java.awt.Frame;
import java.awt.Image;

import jjil.debug.Debug;
import jjil.j2se.RgbImageJ2se;
import barcode.DetectBarcode;
import barcode.ReadBarcode;

@SuppressWarnings("serial")
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
		Image image = getToolkit().getImage("Z:\\Data transfer\\101_PANA\\barcode.JPG"); //$NON-NLS-1$
		//Image image = getToolkit().getImage("c:\\for\\hans\\Successfully scanned barcode taken from iPhone camera using Delicious Library.jpg");
		jjil.core.RgbImage inimg = RgbImageJ2se.toRgbImage(image);
		DetectBarcode db = new DetectBarcode(20000);
		try {
			if (!db.push(inimg)) {
				/**
				 * Couldn't find the barcode. Tell the user.
				 */
				System.out.println(Messages.getString("ReadBarJ.1")); //$NON-NLS-1$
			} else {
				ReadBarcode rb = new ReadBarcode();
				rb.setRect(db.getRect());
				rb.push(inimg);
				if (!rb.getSuccessful()) {
					/**
					 * Couldn't read the barcode.
					 */
					System.out.println(Messages.getString("ReadBarJ.2")); //$NON-NLS-1$
				} else {
					/**
					 * Read the barcode. Tell the user.
					 */
					System.out.println(Messages.getString("ReadBarJ.3") + rb.getCode()); //$NON-NLS-1$
				}
			}
		} catch (Throwable t) {
			System.out.println(t.getLocalizedMessage());
			t.printStackTrace();
		}
	  }
}