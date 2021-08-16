/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.bmp;

public interface BMPConstants {
    // bmp versions
    static final String VERSION_2 = "BMP v. 2.x";
    static final String VERSION_3 = "BMP v. 3.x";
    static final String VERSION_3_NT = "BMP v. 3.x NT";
    static final String VERSION_3_EXT = "BMP V2/V3 INFO";
    static final String VERSION_4 = "BMP v. 4.x";
    static final String VERSION_5 = "BMP v. 5.x";

    // Color space types
    static final int LCS_CALIBRATED_RGB = 0;
    static final int LCS_sRGB = 1;
    static final int LCS_WINDOWS_COLOR_SPACE = 2;
    static final int PROFILE_LINKED = 3;
    static final int PROFILE_EMBEDDED = 4;

    // Compression Types
    static final int BI_RGB = 0;
    static final int BI_RLE8 = 1;
    static final int BI_RLE4 = 2;
    static final int BI_BITFIELDS = 3;
    static final int BI_JPEG = 4;
    static final int BI_PNG = 5;
}

