/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4369044
 * @summary javax.sound.sampled.AudioSystem.getAudioInputStream() works wrong
 *          with Cp037
 */
public class AIFFCp037 {

    public static void main(String args[]) throws Exception {
        System.setProperty("file.encoding", "Cp037");
        // try to read this file with Cp037 encoding
        AudioSystem.getAudioInputStream(new ByteArrayInputStream(SHORT_AIFF));
        System.out.println("  test passed.");
    }

    public static String getString(byte b) {
        //String res = Integer.toHexString(b & 0xFF).toUpperCase();
        //while (res.length()<2) res="0"+res;
        //return res;
        return String.valueOf(b);
    }


    public static void printFile(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        String s = "";
        for (int i=0; i<data.length; i++) {
            s+=getString(data[i])+", ";
            if (s.length()>72) {
                System.out.println(s);
                s="";
            }
        }
        System.out.println(s);
    }

    public static byte[] SHORT_AIFF = {
        70, 79, 82, 77, 0, 0, 4, -54, 65, 73, 70, 70, 67, 79, 77, 77, 0, 0, 0, 18,
        0, 1, 0, 0, 2, 78, 0, 16, 64, 12, -84, 68, 0, 0, 0, 0, 0, 0, 83, 83, 78,
        68, 0, 0, 4, -92, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -2, 0, -2, 0, 0, 0, 0, 0,
        -3, 0, -5, 0, -2, 0, 3, 0, 1, 0, -3, 0, -5, 0, -6, 0, -6, 0, -5, 0, -2, 0,
        -2, 0, -5, 0, -6, 0, -3, 0, 0, 0, 0, 0, -3, 0, -5, 0, -6, 0, -8, 0, -5, 0,
        1, 0, 4, 0, 1, 0, -5, 0, -8, 0, -3, 0, 3, 0, 4, 0, 0, 0, -8, 0, -11, 0, -8,
        0, -3, 0, 0, 0, 0, 0, 1, 0, 0, 0, -5, 0, -9, 0, -8, 0, 0, 0, 6, 0, 7, 0,
        0, 0, -8, 0, -11, 0, -8, 0, 0, 0, 4, 0, 6, 0, 3, 0, -2, 0, -5, 0, -5, 0,
        0, 0, 6, 0, 6, 0, 1, 0, -5, 0, -3, 0, 1, 0, 6, 0, 6, 0, 1, 0, -3, 0, -3,
        0, 0, 0, 3, 0, 3, 0, 0, 0, -3, 0, -2, 0, 3, 0, 6, 0, 4, 0, 0, 0, -2, 0, -2,
        0, 1, 0, 1, 0, 3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -3, 0, 1, 0, 6,
        0, 6, 0, 1, 0, 0, 0, 1, 0, 0, 0, -2, 0, -2, 0, 3, 0, 4, 0, 0, 0, -5, 0, -3,
        0, 1, 0, 4, 0, 4, 0, 0, 0, -2, 0, 1, 0, 1, 0, -2, 0, -6, 0, -6, 0, -2, 0,
        6, 0, 7, 0, 4, 0, 0, 0, -5, 0, -6, 0, -5, 0, 0, 0, 4, 0, 4, 0, 0, 0, -5,
        0, -8, 0, -8, 0, -5, 0, 1, 0, 3, 0, 1, 0, -3, 0, -6, 0, -6, 0, -5, 0, -5,
        0, -3, 0, 1, 0, 3, 0, 1, 0, -2, 0, -5, 0, -5, 0, -5, 0, -3, 0, 3, 0, 6, 0,
        6, 0, 0, 0, -3, 0, -3, 0, 0, 0, 1, 0, 0, 0, 3, 0, 4, 0, 0, 0, -3, 0, -5,
        0, -2, 0, 1, 0, -2, 0, -2, 0, 1, 0, 4, 0, 1, 0, -3, 0, -2, 0, 0, 0, 0, 0,
        -3, 0, -6, 0, -5, 0, 0, 0, 3, 0, 0, 0, -3, 0, -5, 0, -5, 0, -2, 0, -2, 0,
        -5, 0, -6, 0, -3, 0, 1, 0, 1, 0, -2, 0, -8, 0, -8, 0, -3, 0, 1, 0, 3, 0,
        1, 0, -5, 0, -8, 0, -6, 0, -2, 0, 3, 0, 4, 0, -2, 0, -5, 0, -6, 0, -3, 0,
        -2, 0, -2, 0, -2, 0, -2, 0, -3, 0, -5, 0, -6, 0, -5, 0, -2, 0, 0, 0, 0, 0,
        -2, 0, -3, 0, -5, 0, -5, 0, -3, 0, -3, 0, -2, 0, -2, 0, 0, 0, 1, 0, 1, 0,
        0, 0, 1, 0, 1, 0, -2, 0, -5, 0, -3, 0, 1, 0, 4, 0, 6, 0, 4, 0, 1, 0, 0, 0,
        0, 0, 3, 0, 4, 0, 1, 0, -3, 0, -3, 0, 1, 0, 6, 0, 4, 0, 1, 0, -3, 0, -5,
        0, -2, 0, 3, 0, 6, 0, 7, 0, 1, 0, -5, 0, -5, 0, 1, 0, 7, 0, 6, 0, 3, 0, 1,
        0, -2, 0, -5, 0, -5, 0, -2, 0, 3, 0, 3, 0, 3, 0, 0, 0, -3, 0, -5, 0, -3,
        0, 0, 0, 6, 0, 9, 0, 4, 0, -2, 0, -6, 0, -5, 0, -2, 0, 3, 0, 4, 0, 3, 0,
        -2, 0, -6, 0, -3, 0, 1, 0, 3, 0, -3, 0, -6, 0, 0, 0, 4, 0, 1, 0, -6, 0, -9,
        0, -5, 0, 1, 0, 1, 0, 0, 0, -2, 0, -3, 0, -5, 0, -6, 0, -5, 0, 0, 0, 3, 0,
        3, 0, -2, 0, -6, 0, -6, 0, -3, 0, -2, 0, -2, 0, -5, 0, -6, 0, -5, 0, -2,
        0, 0, 0, -2, 0, -3, 0, -3, 0, -3, 0, -2, 0, 0, 0, 1, 0, 0, 0, -2, 0, -2,
        0, -3, 0, -5, 0, -5, 0, -2, 0, 0, 0, 3, 0, 3, 0, 0, 0, -3, 0, -3, 0, 0, 0,
        -2, 0, -5, 0, -3, 0, 0, 0, 1, 0, -3, 0, -8, 0, -6, 0, -2, 0, -2, 0, -6, 0,
        -6, 0, -5, 0, -3, 0, -3, 0, -6, 0, -6, 0, -3, 0, -3, 0, -3, 0, -3, 0, 0,
        0, 1, 0, -3, 0, -6, 0, -3, 0, 1, 0, 0, 0, -6, 0, -9, 0, -9, 0, -6, 0, -2,
        0, 1, 0, 3, 0, -3, 0, -5, 0, -3, 0, -2, 0, -3, 0, -6, 0, -5, 0, -2, 0, -2,
        0, -5, 0, -5, 0, -2, 0, -2, 0, -5, 0, -6, 0, -6, 0, -2, 0, -2, 0, -2, 0,
        -3, 0, -5, 0, -5, 0, -3, 0, 1, 0, 0, 0, 1, 0, 1, 0, 3, 0, 6, 0, 6, 0, 3,
        0, -6, 0, -12, 0, -8, 0, 1, 0, 9, 0, 7, 0, 1, 0, -3, 0, 0, 0, 3, 0, 0, 0,
        0, 0, 3, 0, 6, 0, 7, 0, 3, 0, -3, 0, -5, 0, 0, 0, 4, 0, 4, 0, 3, 0, 1, 0,
        3, 0, 3, 0, 0, 0, -2, 0, 0, 0, 1, 0, 1, 0, 1, 0, 3, 0, 3, 0, 1, 0, 0, 0,
        0, 0, 3, 0, 1, 0, -2, 0, -2, 0, 1, 0, 1, 0, -3, 0, -3, 0, 0, 0, 4, 0, 6,
        0, 6, 0, 3, 0, -3, 0, -8, 0, -5, 0, 1, 0, 3, 0, 1, 0, 1, 0, 0, 0, -3, 0,
        -6, 0, -5, 0, 1, 0, 3, 0, -2, 0, -3, 0, 0, 0, 1, 0, 1, 0, -3, 0, -5, 0, -2,
        0, -2, 0, -2, 0, 0, 0, 1, 0, 1, 0, -2, 0, -5, 0, -8, 0, -6, 0, -5, 0, -2,
        0, 1, 0, 0, 0, -5, 0, -6, 0, 0, 0, 4, 0, 1, 0, -5, 0, -5, 0, -3, 0, -2, 0,
        -3, 0, -3, 0, 0, 0, 0, 0, -2, 0, -3, 0, -2, 0, 1, 0, -2, 0, -5, 0, -3, 0,
        0, 0, 3, 0, 0, 0, -3, 0, -3, 0, -3, 0, -3, 0, -3, 0, 0, 0, 3, 0, 4, 0, -2,
        0, -8, 0, -8, 0, -5, 0, 3, 0, 3, 0, -2, 0, -6, 0, -8, 0, -3, 0, 1, 0, 0,
        0, -5, 0, -5, 0, -2, 0, -2, 0, -3, 0, -5, 0, -3, 0, 0, 0, 0, 0, -2, 0, -5,
        0, -6, 0, -5, 0, -3, 0, 0, 0, 0, 0, -3, 0, -3, 0, -5, 0, -5, 0, -6, 0, -6,
        0, -6, 0, -5, 0, 0, 0, 1, 0, 0, 0, -5, 0, -6, 0, -5, 0, -2, 0, -2, 0, -3,
        0, -5, 0, -8, 0, -9, 0, -6, 0, -2, 0, 0, 0, -2, 0, -5, 0, -5, 0, -2, 0, 3,
        0, 4, 0, 0, 0, -2, 0, -2, 0, 1, 0, 1, 0, 1, 0, 3, 0
    };

}
