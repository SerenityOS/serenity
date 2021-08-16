/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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



package sun.awt.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import sun.java2d.SurfaceData;

/**
 * WritableRasterNative
 * This class exists to wrap a native DataBuffer object.  The
 * standard WritableRaster object assumes that a DataBuffer
 * of a given type (e.g., DataBuffer.TYPE_INT) implies a certain
 * subclass (e.g., DataBufferInt).  But this is not always the
 * case.  DataBufferNative, for example, may allow access to
 * integer-based data, but it is not DataBufferInt (which is a
 * final class and cannot be subclassed).
 * So this class exists simply to allow the WritableRaster
 * functionality for this new kind of DataBuffer object.
 */
public class WritableRasterNative extends WritableRaster {

    public static WritableRasterNative createNativeRaster(SampleModel sm,
                                                          DataBuffer db)
    {
        return new WritableRasterNative(sm, db);
    }

    protected WritableRasterNative(SampleModel sm, DataBuffer db) {
        super(sm, db, new Point(0, 0));
    }

    public static WritableRasterNative createNativeRaster(ColorModel cm,
                                                          SurfaceData sd,
                                                          int width,
                                                          int height)
    {
        SampleModel smHw = null;
        int dataType = 0;
        int scanStride = width;

        switch (cm.getPixelSize()) {
        case 8:
        case 12:
            // 8-bits uses PixelInterleavedSampleModel
            if (cm.getPixelSize() == 8) {
                dataType = DataBuffer.TYPE_BYTE;
            } else {
                dataType = DataBuffer.TYPE_USHORT;
            }
            int[] bandOffsets = new int[1];
            bandOffsets[0] = 0;
            smHw = new PixelInterleavedSampleModel(dataType, width,
                                                   height,
                                                   1, scanStride,
                                                   bandOffsets);
            break;

            // all others use SinglePixelPackedSampleModel
        case 15:
        case 16:
            dataType = DataBuffer.TYPE_USHORT;
            int[] bitMasks = new int[3];
            DirectColorModel dcm = (DirectColorModel)cm;
            bitMasks[0] = dcm.getRedMask();
            bitMasks[1] = dcm.getGreenMask();
            bitMasks[2] = dcm.getBlueMask();
            smHw = new SinglePixelPackedSampleModel(dataType, width,
                                                    height, scanStride,
                                                    bitMasks);
            break;
        case 24:
        case 32:
            dataType = DataBuffer.TYPE_INT;
            bitMasks = new int[3];
            dcm = (DirectColorModel)cm;
            bitMasks[0] = dcm.getRedMask();
            bitMasks[1] = dcm.getGreenMask();
            bitMasks[2] = dcm.getBlueMask();
            smHw = new SinglePixelPackedSampleModel(dataType, width,
                                                    height, scanStride,
                                                    bitMasks);
            break;
        default:
            throw new InternalError("Unsupported depth " +
                                    cm.getPixelSize());
        }

        DataBuffer dbn = new DataBufferNative(sd, dataType,
                                              width, height);
        return new WritableRasterNative(smHw, dbn);
    }
}
