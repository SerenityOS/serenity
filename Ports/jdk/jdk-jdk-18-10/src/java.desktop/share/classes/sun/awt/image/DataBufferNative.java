/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.DataBuffer;
import sun.java2d.SurfaceData;
import java.awt.Rectangle;

/**
 * This class extends {@code DataBuffer} and allows access to
 * native data via the DataBuffer methods.  Note that, unlike other
 * DataBuffer classes, the data is not stored in this class but
 * has been created and stored elsewhere and this class is used
 * merely to access that data.  Note also that this class subclasses
 * from DataBuffer and not from any of the standard subclasses
 * (e.g., DataBufferInt); those subclasses allow the user to
 * get a pointer to the data and manipulate it directly.  That
 * operation may not be possible or wise with native data.
 * One important use of this DataBuffer class is in accessing the
 * data stored in an offscreen vram surface, such as that created
 * by the createVolatileImage() method.
 */

public class DataBufferNative extends DataBuffer
{
    protected SurfaceData surfaceData;
    protected int width;

    /**
     * Constructor.  The constructor of this object requires a
     * SurfaceData object; that surfaceData object will be used
     * to access the actual pixel data in native code.
     */
    public DataBufferNative(SurfaceData sData, int type, int width, int height) {
        super(type, width*height);
        this.width = width;
        this.surfaceData = sData;
    }

    protected native int getElem(int x, int y, SurfaceData sData);

    /**
     * getElem returns the pixel value for a given index into the
     * dataBuffer array.  The bank value is currently ignored (the
     * type of data accessed through this class is not stored in
     * separate banks).  The x and y coordinates of a pixel are calculated
     * from the index value and the native getElem() method is
     * called with the internal surfaceData object.
     */
    public int getElem(int bank, int i) {
        return getElem(i % width, i / width, surfaceData);
    }

    protected native void setElem(int x, int y, int val, SurfaceData sData);

    /**
     * setElem sets the pixel value of a given index into the
     * dataBuffer array.  The bank value is currently ignored (the
     * type of data accessed through this class is not stored in
     * separate banks).  The x and y coordinates of a pixel are calculated
     * from the index value and the native setElem() method is
     * called with the internal surfaceData object.
     */
    public void setElem(int bank, int i, int val) {
        setElem(i % width, i / width, val, surfaceData);
    }

}
