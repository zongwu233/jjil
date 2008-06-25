/*
 * ComparableJ2me.java
 *  This is a replacement (for use with J2ME / CLDC 1.0) of the
 *  Java class Comparable defined in J2SE.
 *
 * Created on April 15, 2008, 7:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jjil.algorithm;

/**
 *
 * @author webb
 */
public interface ComparableJ2me {
    int compareTo(Object o) throws jjil.core.Error;
}
