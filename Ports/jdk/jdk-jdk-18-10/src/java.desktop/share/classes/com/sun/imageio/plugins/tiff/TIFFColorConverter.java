/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

/**
 * An abstract class that performs simple color conversion on 3-banded source
 * images, for use with the TIFF Image I/O plug-in.
 */
public abstract class TIFFColorConverter {

    /**
     * Constructs an instance of a {@code TIFFColorConverter}.
     */
    public TIFFColorConverter() {}

    /**
     * Converts an RGB triple into the native color space of this
     * TIFFColorConverter, and stores the result in the first three
     * entries of the {@code result} array.
     *
     * @param r the red value.
     * @param g the green value.
     * @param b the blue value.
     * @param result an array of {@code float}s containing three elements.
     * @throws NullPointerException if {@code result} is
     * {@code null}.
     * @throws ArrayIndexOutOfBoundsException if
     * {@code result.length < 3}.
     */
    public abstract void fromRGB(float r, float g, float b, float[] result);

    /**
     * Converts  a   triple  in  the   native  color  space   of  this
     * TIFFColorConverter into an RGB triple, and stores the result in
     * the first three entries of the {@code rgb} array.
     *
     * @param x0 the value of channel 0.
     * @param x1 the value of channel 1.
     * @param x2 the value of channel 2.
     * @param rgb an array of {@code float}s containing three elements.
     * @throws NullPointerException if {@code rgb} is
     * {@code null}.
     * @throws ArrayIndexOutOfBoundsException if
     * {@code rgb.length < 3}.
     */
    public abstract void toRGB(float x0, float x1, float x2, float[] rgb);
}
