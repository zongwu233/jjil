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
		Image image = getToolkit().getImage("Z:\\Data transfer\\101_PANA\\P1010106.JPG"); //$NON-NLS-1$
		jjil.core.RgbImage inimg = RgbImageJ2se.toRgbImage(image);
		DetectBarcode db = new DetectBarcode(20000);
		try {
			if (!db.Push(inimg)) {
				/**
				 * Couldn't find the barcode. Tell the user.
				 */
				System.out.println(Messages.getString("ReadBarJ.1")); //$NON-NLS-1$
			} else {
				ReadBarcode rb = new ReadBarcode();
				rb.setRect(db.getRect());
				rb.Push(inimg);
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
		} catch (Exception ex) {
			Throwable t = ex.getCause();
			if (t instanceof jjil.core.Error) {
				jjil.j2se.Error e = new jjil.j2se.Error((jjil.core.Error) t);
				System.out.println(e.getLocalizedMessage());
			}
			ex.printStackTrace();
		}
	  }
}