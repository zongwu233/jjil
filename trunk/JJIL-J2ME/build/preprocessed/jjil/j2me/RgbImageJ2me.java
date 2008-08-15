/*
 * RgbImageJ2me.java
 *
 * Created on November 29, 2007, 3:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jjil.j2me;

import javax.microedition.lcdui.Image;
import jjil.core.RgbImage;
/**
 * Implements a path in and out of RgbImage for J2ME Image objects.
 * @author webb
 */
public class RgbImageJ2me {
    
    /**
     * Create an RgbImage from a javax.microedition.lcdui.Image
     * image. The appropriate sized image area is created and
     * the contents are copied from the input.
     * @param image the javax.microedition.lcdui.Image image
     * @return the RgbImage with the same contents as the javax.microedition.lcdui.Image.
     */
    static public RgbImage toRgbImage(javax.microedition.lcdui.Image image)
    {
        RgbImage rgb = new RgbImage(image.getWidth(), image.getHeight());
        image.getRGB(
                rgb.getData(), 
                0, 
                rgb.getWidth(), 
                0, 
                0, 
                rgb.getWidth(), 
                rgb.getHeight());
        return rgb;
    }
    
    /**
     * Return a javax.microedition.lcdui.Image image
     * with contents equal to the RGB image stored here.
     * The transparency parameter is set to false so that
     * the contents of the RgbImage's A byte are not examined.
     * @return An javax.microedition.lcdui.Image object with contents equal
     * to this Image.
     * @param rgb the input RgbImage which is to be converted to a javax.microedition.lcdui.Image.
     */
    static public javax.microedition.lcdui.Image toImage(RgbImage rgb)
    {
        javax.microedition.lcdui.Image image = 
            javax.microedition.lcdui.Image.createRGBImage(
                rgb.getData(),
                rgb.getWidth(),
                rgb.getHeight(),
                false);
        return image;
    }
    
}
