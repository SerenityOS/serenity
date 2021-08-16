/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * @test
 * @bug 8257500
 * @summary Drawing MultiResolutionImage with ImageObserver may "leaks" memory
 */
public final class ImageObserverLeak {

    public static void main(String[] args) throws Exception {
        Reference<ImageObserver> ref = test();

        while (!ref.refersTo(null)) {
            Thread.sleep(500);
            // Cannot generate OOM here, it will clear the SoftRefs as well
            System.gc();
        }
    }

    private static Reference<ImageObserver> test() throws Exception {
        BufferedImage src = new BufferedImage(200, 200, TYPE_INT_RGB);
        Image mri = new BaseMultiResolutionImage(src);
        ImageObserver observer = new ImageObserver() {
            @Override
            public boolean imageUpdate(Image img, int infoflags, int x, int y,
                                       int width, int height) {
                return false;
            }
        };
        Reference<ImageObserver> ref = new WeakReference<>(observer);

        BufferedImage dst = new BufferedImage(200, 300, TYPE_INT_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(mri, 0, 0, observer);
        g2d.dispose();
        return ref;
    }
}
