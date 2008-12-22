package jjil.app.facedetect;

import java.io.InputStream;
import java.io.ObjectInputStream;

import jjil.algorithm.HaarClassifierCascade;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Progress extends Activity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        try {
        	Thread t = new Thread() {
        		public void run() {
        			try {
				    	InputStream is = getResources().openRawResource(R.raw.hcsb_ser);
				    	ObjectInputStream ios = new ObjectInputStream(is);
				    	Object o = ios.readObject();
				    	DetectHaarParam.setCascade((HaarClassifierCascade) o);
				    	ios.close();
				    	is.close();
	        		} catch (Exception ex) {
	                	ex.printStackTrace();
	                    System.err.print("Exception: " + ex.toString()); //$NON-NLS-1$ //$NON-NLS-2$       	
        			}
			        startActivity(new Intent("jjil.app.facedetect.preview"));
					finish();
        		}
        	};
	        t.start();
       } catch (Exception ex) {
        	ex.printStackTrace();
            System.err.print("Exception: " + ex.toString()); //$NON-NLS-1$ //$NON-NLS-2$       	
        }
 	}
}
