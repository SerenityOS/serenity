/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.awt.color.ColorSpace;

/**
 * A dummy {@code ColorSpace} to enable {@code ColorModel}
 * for image data which do not have an innate color representation.
 */
@SuppressWarnings("serial") // JDK-implementation class
public class BogusColorSpace extends ColorSpace {
    /**
     * Return the type given the number of components.
     *
     * @param numComponents The number of components in the
     * {@code ColorSpace}.
     * @exception IllegalArgumentException if {@code numComponents}
     * is less than 1.
     */
    private static int getType(int numComponents) {
        if(numComponents < 1) {
            throw new IllegalArgumentException("numComponents < 1!");
        }

        int type;
        switch(numComponents) {
        case 1:
            type = ColorSpace.TYPE_GRAY;
            break;
        default:
            // Based on the constant definitions TYPE_2CLR=12 through
            // TYPE_FCLR=25. This will return unknown types for
            // numComponents > 15.
            type = numComponents + 10;
        }

        return type;
    }

    /**
     * Constructs a bogus {@code ColorSpace}.
     *
     * @param numComponents The number of components in the
     * {@code ColorSpace}.
     * @exception IllegalArgumentException if {@code numComponents}
     * is less than 1.
     */
    public BogusColorSpace(int numComponents) {
        super(getType(numComponents), numComponents);
    }

    //
    // The following methods simply copy the input array to the
    // output array while otherwise attempting to adhere to the
    // specified behavior of the methods vis-a-vis exceptions.
    //

    public float[] toRGB(float[] colorvalue) {
        if(colorvalue.length < getNumComponents()) {
            throw new ArrayIndexOutOfBoundsException
                ("colorvalue.length < getNumComponents()");
        }

        float[] rgbvalue = new float[3];

        System.arraycopy(colorvalue, 0, rgbvalue, 0,
                         Math.min(3, getNumComponents()));

        return colorvalue;
    }

    public float[] fromRGB(float[] rgbvalue) {
        if(rgbvalue.length < 3) {
            throw new ArrayIndexOutOfBoundsException
                ("rgbvalue.length < 3");
        }

        float[] colorvalue = new float[getNumComponents()];

        System.arraycopy(rgbvalue, 0, colorvalue, 0,
                         Math.min(3, colorvalue.length));

        return rgbvalue;
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        if(colorvalue.length < getNumComponents()) {
            throw new ArrayIndexOutOfBoundsException
                ("colorvalue.length < getNumComponents()");
        }

        float[] xyzvalue = new float[3];

        System.arraycopy(colorvalue, 0, xyzvalue, 0,
                         Math.min(3, getNumComponents()));

        return colorvalue;
    }

    public float[] fromCIEXYZ(float[] xyzvalue) {
        if(xyzvalue.length < 3) {
            throw new ArrayIndexOutOfBoundsException
                ("xyzvalue.length < 3");
        }

        float[] colorvalue = new float[getNumComponents()];

        System.arraycopy(xyzvalue, 0, colorvalue, 0,
                         Math.min(3, colorvalue.length));

        return xyzvalue;
    }
}
