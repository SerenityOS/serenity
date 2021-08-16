/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFileFormat;

/**
 * @test
 * @bug 4300529
 * @summary Filename extension test. The filename extensions for file types
 *          AIFF-C, SND, and WAVE should not include a ".".
 */
public class FileTypeExtensionTest {

    public static void main(String[] args) throws Exception {

        AudioFileFormat.Type[] types = { AudioFileFormat.Type.AIFC,
                                         AudioFileFormat.Type.AIFF,
                                         AudioFileFormat.Type.AU,
                                         AudioFileFormat.Type.SND,
                                         AudioFileFormat.Type.WAVE };

        boolean failed = false;

        System.out.println("\nDefined file types and extensions:");

        for (int i = 0; i < types.length; i++) {
            System.out.println("\n");
            System.out.println("  file type: " + types[i]);
            System.out.println("  extension: " + types[i].getExtension());
            if( types[i].getExtension().charAt(0) == '.' ) {
                failed = true;
            }
        }

        if (failed) {
            System.err.println("Failed!");
            throw new Exception("File type extensions begin with .");
        } else {
            System.err.println("Passed!");
        }
    }
}
