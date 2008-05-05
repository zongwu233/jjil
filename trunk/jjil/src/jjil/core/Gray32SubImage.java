/*
 * Gray32SubImage.java
 *
 * Created on June 29, 2007, 5:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the Lesser GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jjil.core;

/**
 *
 * @author webb
 */
public class Gray32SubImage extends Gray32Image {
    int cX;
    int cY;
    
    /** Creates a new instance of Gray32SubImage */
    public Gray32SubImage(int cWidth, int cHeight, int cX, int cY) {
        super(cWidth, cHeight);
        this.cX = cX;
        this.cY = cY;
    }
    
    /** Copy this image
     *
     * @return the image copy.
     */
    public Image Clone()
    {
        Gray32Image image = new Gray32SubImage(getWidth(),getHeight(),getXOffset(),getYOffset());
        System.arraycopy(
                this.getData(),
                0,
                image.getData(),
                0,
                getWidth()*getHeight());
        return image;
    }
    
    public int getXOffset()
    {
        return this.cX;
    }
    
    public int getYOffset()
    {
        return this.cY;
    }
    
    public void setXOffset(int nX)
    {
        this.cX = nX;
    }
    
    public void setYOffset(int nY)
    {
        this.cY = nY;
    }
    
    /** Return a string describing the image.
     *
     * @return the string.
     */
    public String toString()
    {
        return super.toString() + " (" + getWidth() + "x" + getHeight() + //$NON-NLS-1$ //$NON-NLS-2$
                "," + getXOffset() + "," + getYOffset() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }


}
