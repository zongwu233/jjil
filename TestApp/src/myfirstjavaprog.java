import java.awt.Frame;
import java.awt.Image;

import jjil.debug.Debug;
import jjil.j2se.RgbImageJ2se;
import barcode.DetectBarcode;

class myfirstjavaprog extends Frame
{  
	  public static void main(String argv[])
	  {
	    new myfirstjavaprog();
	  }
	  myfirstjavaprog() {
		this.setVisible(true);
		RgbImageJ2se debug = new RgbImageJ2se(this.getGraphics());
		Debug.setShow(debug);
		Image image = getToolkit().getImage("c:\\images\\barcode.jpg");
		jjil.core.RgbImage inimg = RgbImageJ2se.toRgbImage(image);
		DetectBarcode db = new DetectBarcode(20000);
		db.Push(inimg);
	  }
}