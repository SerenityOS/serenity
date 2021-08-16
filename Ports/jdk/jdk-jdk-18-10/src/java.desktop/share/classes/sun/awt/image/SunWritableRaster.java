/*
 * Copyright (c) 2001, 2008, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import sun.java2d.StateTrackable.State;
import sun.java2d.SurfaceData;
import sun.java2d.StateTrackableDelegate;

/**
 * This class exists as a middle layer between WritableRaster and its
 * implementation specific subclasses (ByteComponentRaster, ShortBandedRaster,
 * etc).
 * It provides utilities to steal the data arrays from the standard DataBuffer
 * types and also steals the StateTrackableDelegate from the associated
 * DataBuffer so that it can be updated when the data is changed.
 */
public class SunWritableRaster extends WritableRaster {
    private static DataStealer stealer;

    public static interface DataStealer {
        public byte[] getData(DataBufferByte dbb, int bank);
        public short[] getData(DataBufferUShort dbus, int bank);
        public int[] getData(DataBufferInt dbi, int bank);
        public StateTrackableDelegate getTrackable(DataBuffer db);
        public void setTrackable(DataBuffer db, StateTrackableDelegate trackable);
    }

    public static void setDataStealer(DataStealer ds) {
        if (stealer != null) {
            throw new InternalError("Attempt to set DataStealer twice");
        }
        stealer = ds;
    }

    public static byte[] stealData(DataBufferByte dbb, int bank) {
        return stealer.getData(dbb, bank);
    }

    public static short[] stealData(DataBufferUShort dbus, int bank) {
        return stealer.getData(dbus, bank);
    }

    public static int[] stealData(DataBufferInt dbi, int bank) {
        return stealer.getData(dbi, bank);
    }

    public static StateTrackableDelegate stealTrackable(DataBuffer db) {
        return stealer.getTrackable(db);
    }

    public static void setTrackable(DataBuffer db, StateTrackableDelegate trackable) {
        stealer.setTrackable(db, trackable);
    }

    public static void makeTrackable(DataBuffer db) {
        stealer.setTrackable(db, StateTrackableDelegate.createInstance(State.STABLE));
    }

    public static void markDirty(DataBuffer db) {
        stealer.getTrackable(db).markDirty();
    }

    public static void markDirty(WritableRaster wr) {
        if (wr instanceof SunWritableRaster) {
            ((SunWritableRaster) wr).markDirty();
        } else {
            markDirty(wr.getDataBuffer());
        }
    }

    public static void markDirty(Image img) {
        SurfaceData.getPrimarySurfaceData(img).markDirty();
    }

    private StateTrackableDelegate theTrackable;

    public SunWritableRaster(SampleModel sampleModel, Point origin) {
        super(sampleModel, origin);
        theTrackable = stealTrackable(dataBuffer);
    }

    public SunWritableRaster(SampleModel sampleModel,
                             DataBuffer dataBuffer,
                             Point origin)
    {
        super(sampleModel, dataBuffer, origin);
        theTrackable = stealTrackable(dataBuffer);
    }

    public SunWritableRaster(SampleModel sampleModel,
                             DataBuffer dataBuffer,
                             Rectangle aRegion,
                             Point sampleModelTranslate,
                             WritableRaster parent)
    {
        super(sampleModel, dataBuffer, aRegion, sampleModelTranslate, parent);
        theTrackable = stealTrackable(dataBuffer);
    }

    /**
     * Mark the TrackableDelegate of the associated DataBuffer dirty.
     */
    public final void markDirty() {
        theTrackable.markDirty();
    }
}
