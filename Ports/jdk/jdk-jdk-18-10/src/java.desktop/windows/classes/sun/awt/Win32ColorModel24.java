/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.ComponentColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.color.ColorSpace;
import java.awt.Transparency;

/**
 * This class creates a standard ComponentColorModel with the slight
 * difference that it creates its Raster objects with the components
 * in the reverse order from the base ComponentColorModel to match
 * the ordering on a Windows 24-bit display.
 */
public class Win32ColorModel24 extends ComponentColorModel {
    public Win32ColorModel24() {
        super(ColorSpace.getInstance(ColorSpace.CS_sRGB),
              new int[] {8, 8, 8}, false, false,
              Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
    }

    /**
     * Creates a WritableRaster with the specified width and height, that
     * has a data layout (SampleModel) compatible with this ColorModel.
     * @see WritableRaster
     * @see SampleModel
     */
    public WritableRaster createCompatibleWritableRaster (int w, int h) {
        int[] bOffs = {2, 1, 0};
        return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                              w, h, w*3, 3,
                                              bOffs, null);
    }

    /**
     * Creates a SampleModel with the specified width and height, that
     * has a data layout compatible with this ColorModel.
     * @see SampleModel
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] bOffs = {2, 1, 0};
        return new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                               w, h, 3, w*3, bOffs);
    }
}
