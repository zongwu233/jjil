package com.backwoodsprogrammer.app.gencdialit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jjil.j2se.ocr.PrecomputeMath;
import jjil.j2se.ocr.PrototypesCollection;

public class GenCDialIt {
	public static void main(String args[]) {
        FileOutputStream fos = null;
        try {
            PrototypesCollection it = new PrototypesCollection(
            		"c:\\users\\webb\\workspace\\tesseract-2.03\\tessdata\\eng.unicharset", 
            		"c:\\users\\webb\\workspace\\tesseract-2.03\\tessdata\\eng.pffmtable", 
            		"c:\\users\\webb\\workspace\\tesseract-2.03\\tessdata\\eng.inttemp");
            Character szChars[] = new Character[]{'c'};
            szChars = it.keySet().toArray(szChars);
            for (Character ch: szChars) {
            	if ("0123456789".indexOf(ch) < 0) {
            		it.remove(ch);
            	}
            }
            fos = new FileOutputStream("c:\\users\\webb\\workspace-android\\cdialit\\res\\raw\\inttemp.ser");
//            GZIPOutputStream gzos = new GZIPOutputStream(fos);
//            ObjectOutputStream oos = new ObjectOutputStream(gzos);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(it);
            oos.close();
            fos.close();
            PrecomputeMath pm = new PrecomputeMath();
            pm.InitFromMath();
            fos = new FileOutputStream("c:\\users\\webb\\workspace-android\\cdialit\\res\\raw\\pcmath.ser");
//            gzos = new GZIPOutputStream(fos);
//            oos = new ObjectOutputStream(gzos);
            oos = new ObjectOutputStream(fos);
//            oos.writeObject(pm);
            oos.writeObject(pm);
            oos.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GenCDialIt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioe) {
            Logger.getLogger(GenCDialIt.class.getName()).log(Level.SEVERE, null, ioe);
        } catch (Exception ex) {
        	Logger.getLogger(GenCDialIt.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(GenCDialIt.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
		
	}
}
