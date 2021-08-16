/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4434870 4434886 4441315 4446842
 * @summary Checks that miscellaneous ImageWriteParam methods work properly
 */

import java.awt.Dimension;

import javax.imageio.ImageWriteParam;

public class ImageWriteParamMisc {

    public static void main(String[] args) {
        test4434870();
        test4434886();
        test4441315();
        test4446842();
    }

    public static class ImageWriteParam4434870 extends ImageWriteParam {
        public ImageWriteParam4434870() {
            super(null);
            super.canWriteTiles = true;
            super.preferredTileSizes =
                new Dimension[] {new Dimension(1, 2), new Dimension(5, 6)};
        }
    }

    private static void test4434870() {
        ImageWriteParam iwp = new ImageWriteParam4434870();
        try {
            Dimension[] dimensions = iwp.getPreferredTileSizes();
            iwp.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setTiling(100, 100, 0,0);
            throw new RuntimeException("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static class ImageWriteParam4434886 extends ImageWriteParam {
        public ImageWriteParam4434886() {
            super(null);
            super.canWriteTiles = true;
            super.canOffsetTiles = true;
        }
    }

    private static void test4434886() {
        ImageWriteParam iwp = new ImageWriteParam4434886();
        iwp.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
        try {
            iwp.setTiling(-1,-2,-3,-4);
            throw new RuntimeException("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static class ImageWriteParam4441315 extends ImageWriteParam {
        public ImageWriteParam4441315() {
            super(null);
            super.canWriteProgressive = true;
        }
    }

    private static void test4441315() {
        ImageWriteParam iwp = new ImageWriteParam4441315();
        try {
            iwp.setProgressiveMode(ImageWriteParam.MODE_EXPLICIT);
            throw new RuntimeException("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    private static void test4446842() {
        ImageWriteParam iwp = new ImageWriteParam(null);
        try {
            iwp.getCompressionTypes();
            throw new RuntimeException("Failed to get UOE!");
        } catch (UnsupportedOperationException e) {
        }
    }
}
