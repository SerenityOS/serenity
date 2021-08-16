/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.awt;

import java.awt.*;
import java.awt.image.*;

/**
 * A class to encapsulate a custom image-based cursor.
 *
 * @author      ThomasBall
 */
@SuppressWarnings("serial") // JDK-implementation class
public abstract class CustomCursor extends Cursor {

    protected Image image;

    public CustomCursor(Image cursor, Point hotSpot, String name)
            throws IndexOutOfBoundsException {
        super(name);
        image = cursor;
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        // Make sure image is fully loaded.
        Component c = new Canvas(); // for its imageUpdate method
        MediaTracker tracker = new MediaTracker(c);
        tracker.addImage(cursor, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) {
        }
        int width = cursor.getWidth(c);
        int height = cursor.getHeight(c);

        // Fix for bug 4212593 The Toolkit.createCustomCursor does not
        //                     check absence of the image of cursor
        // If the image is invalid, the cursor will be hidden (made completely
        // transparent). In this case, getBestCursorSize() will adjust negative w and h,
        // but we need to set the hotspot inside the image here.
        if (tracker.isErrorAny() || width < 0 || height < 0) {
              hotSpot.x = hotSpot.y = 0;
        }

        // Scale image to nearest supported size.
        Dimension nativeSize = toolkit.getBestCursorSize(width, height);
        if ((nativeSize.width != width || nativeSize.height != height) &&
            (nativeSize.width != 0 && nativeSize.height != 0)) {
            cursor = cursor.getScaledInstance(nativeSize.width,
                                              nativeSize.height,
                                              Image.SCALE_DEFAULT);
            width = nativeSize.width;
            height = nativeSize.height;
        }

        // Verify that the hotspot is within cursor bounds.
        if (hotSpot.x >= width || hotSpot.y >= height || hotSpot.x < 0 || hotSpot.y < 0) {
            throw new IndexOutOfBoundsException("invalid hotSpot");
        }

        /* Extract ARGB array from image.
         *
         * A transparency mask can be created in native code by checking
         * each pixel's top byte -- a 0 value means the pixel's transparent.
         * Since each platform's format of the bitmap and mask are likely to
         * be different, their creation shouldn't be here.
         */
        int[] pixels = new int[width * height];
        ImageProducer ip = cursor.getSource();
        PixelGrabber pg = new PixelGrabber(ip, 0, 0, width, height,
                                           pixels, 0, width);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        createNativeCursor(image, pixels, width, height, hotSpot.x, hotSpot.y);
    }

    protected abstract void createNativeCursor(Image im,  int[] pixels,
                                               int width, int height,
                                               int xHotSpot, int yHotSpot);
}
