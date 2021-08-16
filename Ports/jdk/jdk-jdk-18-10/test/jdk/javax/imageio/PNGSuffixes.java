/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4394924
 * @summary Checks for spurious leading "." in PNG file suffixes
 * @modules java.desktop/com.sun.imageio.plugins.png
 */

import com.sun.imageio.plugins.png.PNGImageWriterSpi;

public class PNGSuffixes {

    public static void main(String[] args) {
        String[] suffixes = new PNGImageWriterSpi().getFileSuffixes();
        for (int i = 0; i < suffixes.length; i++) {
            if (suffixes[i].startsWith(".")) {
                throw new RuntimeException("Found a \".\" in a suffix!");
            }
        }
    }
}
