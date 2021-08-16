/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import java.awt.*;
import java.awt.image.*;
import java.nio.IntBuffer;
import sun.awt.image.SunWritableRaster;

public class IntegerNIORaster extends SunWritableRaster {

    protected IntBuffer data;

    public static WritableRaster createNIORaster(int w, int h, int[] bandMasks, Point location) {
        if ((w <= 0) || (h <= 0)) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h +
                                               ") cannot be <= 0");
        }
        // This is cribbed from java.awt.image.Raster.
        DataBufferNIOInt db = new DataBufferNIOInt(w * h);
        if (location == null) {
            location = new Point(0, 0);
        }
        SinglePixelPackedSampleModel sppsm =  new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, w, bandMasks);
        return new IntegerNIORaster(sppsm, db, location);
    }

    public IntegerNIORaster(SampleModel sampleModel, DataBufferNIOInt dataBuffer, Point origin) {
        // This is all cribbed from sun.awt.image.IntegerInterleavedRaster & sun.awt.image.IntegerComponentRaster
        super(sampleModel, dataBuffer, new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()), origin, null);

        this.data = dataBuffer.getBuffer();
    }

    public WritableRaster createCompatibleWritableRaster() {
        return new IntegerNIORaster(sampleModel, new DataBufferNIOInt(sampleModel.getWidth() * sampleModel.getHeight()), new Point(0,0));
    }

    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <=0) {
            throw new RasterFormatException("negative " + ((w <= 0) ? "width" : "height"));
        }

        SampleModel sm = sampleModel.createCompatibleSampleModel(w,h);

        return new IntegerNIORaster(sm, new DataBufferNIOInt(w * h), new Point(0,0));
    }

    public WritableRaster createCompatibleWritableRaster(Rectangle rect) {
        if (rect == null) {
            throw new NullPointerException("Rect cannot be null");
        }
        return createCompatibleWritableRaster(rect.x, rect.y, rect.width, rect.height);
    }

    public WritableRaster createCompatibleWritableRaster(int x, int y, int w, int h) {
        WritableRaster ret = createCompatibleWritableRaster(w, h);
        return ret.createWritableChild(0,0,w,h,x,y,null);
    }


    public IntBuffer getBuffer() {
        return data;
    }

    public String toString() {
        return new String ("IntegerNIORaster: width = "+width
                           +" height = " + height
                           +" #Bands = " + numBands
                           +" xOff = "+sampleModelTranslateX
                           +" yOff = "+sampleModelTranslateY);
    }
}
