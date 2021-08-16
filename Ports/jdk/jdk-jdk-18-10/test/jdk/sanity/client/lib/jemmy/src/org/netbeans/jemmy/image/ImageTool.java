/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.netbeans.jemmy.image;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import org.netbeans.jemmy.JemmyException;

/**
 * Contains util methods to work with images.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ImageTool {

    /**
     * Gets an image from a rectange on screen.
     *
     * @param rect a rectangle on screen in absolute screen coordinates.
     * @return a captured image.
     */
    public static BufferedImage getImage(Rectangle rect) {
        try {
            return new Robot().createScreenCapture(rect);
        } catch (AWTException e) {
            throw (new JemmyException("Exception during screen capturing", e));
        }
    }

    /**
     * Gets an image from a component.
     *
     * @param comp a visible component.
     * @return a captured image.
     */
    public static BufferedImage getImage(Component comp) {
        return (getImage(new Rectangle(comp.getLocationOnScreen(),
                comp.getSize())));
    }

    /**
     * Gets the whole screen image.
     *
     * @return a captured image.
     */
    public static BufferedImage getImage() {
        return getImage(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
    }

    /**
     * Increases image.
     *
     * @param image an image to enlarge.
     * @param zoom A scale.
     * @return a result image.
     */
    public static BufferedImage enlargeImage(BufferedImage image, int zoom) {
        int wight = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(wight * zoom,
                height * zoom,
                image.getType());
        int rgb;
        for (int x = 0; x < wight; x++) {
            for (int y = 0; y < height; y++) {
                rgb = image.getRGB(x, y);
                for (int i = 0; i < zoom; i++) {
                    for (int j = 0; j < zoom; j++) {
                        result.setRGB(x * zoom + i,
                                y * zoom + j,
                                rgb);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @deprecated Use subtractImage(BufferedImage, BufferedImage) instead.
     * @param minuend an image to subtract from.
     * @param deduction an image to subtract.
     * @return a result image.
     */
    @Deprecated
    public static BufferedImage substractImage(BufferedImage minuend, BufferedImage deduction) {
        return subtractImage(minuend, deduction);
    }

    /**
     * Subtracts second image from first one. Could be used to save file
     * difference for future analysis.
     *
     * @param minuend an image to subtract from.
     * @param deduction an image to subtract.
     * @return a result image.
     */
    public static BufferedImage subtractImage(BufferedImage minuend, BufferedImage deduction) {
        return subtractImage(minuend, deduction, 0, 0);
    }

    /**
     * @deprecated Use subtractImage(BufferedImage, BufferedImage, int, int)
     * instead.
     * @param minuend an image to subtract from.
     * @param deduction an image to subtract.
     * @return a result image.
     */
    @Deprecated
    public static BufferedImage substractImage(BufferedImage minuend, BufferedImage deduction, int relativeX, int relativeY) {
        return subtractImage(minuend, deduction, relativeX, relativeY);
    }

    /**
     * Subtracts subimage from image. Could be used to save file difference for
     * future analysis.
     *
     * @param minuend an image to subtract from.
     * @param deduction an image to subtract.
     * @param relativeX - deduction-in-minuend X coordinate
     * @param relativeY - deduction-in-minuend Y coordinate
     * @return a result image.
     */
    public static BufferedImage subtractImage(BufferedImage minuend, BufferedImage deduction, int relativeX, int relativeY) {
        int mWidth = minuend.getWidth();
        int mHeight = minuend.getHeight();
        int dWidth = deduction.getWidth();
        int dHeight = deduction.getHeight();

        int maxWidth = (mWidth > relativeX + dWidth) ? mWidth : (relativeX + dWidth);
        int maxHeight = (mHeight > relativeY + dHeight) ? mHeight : (relativeY + dHeight);

        BufferedImage result = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        int mColor, dColor;
        for (int x = 0; x < maxWidth; x++) {
            for (int y = 0; y < maxHeight; y++) {
                if (x >= mWidth
                        || y >= mHeight) {
                    mColor = 0;
                } else {
                    mColor = minuend.getRGB(x, y);
                }
                if (x >= dWidth + relativeX
                        || y >= dHeight + relativeY
                        || x < relativeX
                        || y < relativeY) {
                    dColor = 0;
                } else {
                    dColor = deduction.getRGB(x - relativeX, y - relativeY);
                }
                result.setRGB(x, y, subtractColors(mColor, dColor));
            }
        }
        return result;
    }

    private static int subtractColors(int mRGB, int dRGB) {
        Color mColor = new Color(mRGB);
        Color dColor = new Color(dRGB);
        int red = subtractColor(mColor.getRed(), dColor.getRed());
        int green = subtractColor(mColor.getGreen(), dColor.getGreen());
        int blue = subtractColor(mColor.getBlue(), dColor.getBlue());
        return new Color(red, green, blue).getRGB();
    }

    private static int subtractColor(int mColor, int dColor) {
        if (mColor >= dColor) {
            return mColor - dColor;
        } else {
            return mColor - dColor + 0Xff;
        }
    }
}
