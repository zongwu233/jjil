/*
 * ImageFormat.java
 *
 * Created on October 23, 2007, 5:16 PM
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

/**
 * Image format represents the internal contents of an image and how the image
 * data is encoded -- 8-bits grayvalue, 24-bits RGB, etc.
 * @author webb
 */
public class ImageFormat {
        private static final ImageFormat fmtGray8 = new ImageFormat("gray8");
        private static final ImageFormat fmtRgb888 = new ImageFormat("rgb888");
        private static final ImageFormat fmtBgr888 = new ImageFormat("bgr888");
        private static final ImageFormat fmtRgb555 = new ImageFormat("rgb555");
        private static final ImageFormat fmtRgb565 = new ImageFormat("rgb565");
        private static final ImageFormat fmtYuv444 = new ImageFormat("yuv444");
        private static final ImageFormat fmtYuv422 = new ImageFormat("yuv422");
        private static final ImageFormat fmtJpeg = new ImageFormat("jpeg");
        private static final ImageFormat fmtPng = new ImageFormat("png");

        private String szName;
        private ImageFormat(String szName) {
            this.szName = szName;
        }

    /**
     * Returns a new ImageFormat object created from the string. The string originally
     * comes from a System.getProperty like video.encodings.
     * @param szFormat The string defining the image format. The Multi-media API defines the legal
     * values. Typical value is "rgb888" or "jpeg".
     * @return An ImageFormat object corresponding to the given string. There are a fixed
     * number of ImageFormat objects so if two ImageFormat objects are equal they
     * describe the same format.
     */
        public static ImageFormat valueOf(String szFormat) {
            if (szFormat.compareTo("gray8") == 0) {
               return fmtGray8;
            } else if (szFormat.compareTo("rgb888") == 0) {
                return fmtRgb888;
            } else if (szFormat.compareTo("rgb888") == 0) {
                return fmtRgb888;
            } else if (szFormat.compareTo("bgr888") == 0) {
                return fmtBgr888;
            } else if (szFormat.compareTo("rgb565") == 0) {
                return fmtRgb565;
            } else if (szFormat.compareTo("rgb555") == 0) {
                return fmtRgb555;
            } else if (szFormat.compareTo("yuv444") == 0) {
                return fmtYuv444;
            } else if (szFormat.compareTo("yuv422") == 0) {
                return fmtYuv422;
            } else if (szFormat.compareTo("jpeg") == 0) {
                return fmtJpeg;
            } else if (szFormat.compareTo("png") == 0) {
                return fmtPng;
            } else return null;
        }
}

