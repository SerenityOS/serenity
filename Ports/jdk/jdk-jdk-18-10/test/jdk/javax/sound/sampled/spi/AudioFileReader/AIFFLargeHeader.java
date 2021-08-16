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
 * @bug 4399551
 * @summary Repost of bug candidate: cannot replay aif file. AIFF headers were
 *          checked for certain size also tests that ulaw encoded AIFC files can
 *          be read.
 */
public class AIFFLargeHeader {

    public static void main(String args[]) throws Exception {
        System.out.println();
        System.out.println();
        System.out.println("4399551: Repost of bug candidate: cannot replay aif file (Review ID: 108108)");
        // try to read this file
        AudioSystem.getAudioInputStream(new ByteArrayInputStream(SHORT_AIFC_ULAW));
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

    public static byte[] SHORT_AIFC_ULAW = {
        70, 79, 82, 77, 0, 0, 2, 50, 65, 73, 70, 67, 70, 86, 69, 82, 0, 0, 0, 4,
        -94, -128, 81, 64, 67, 79, 77, 77, 0, 0, 0, 30, 0, 1, 0, 1, 118, -9, 0, 16,
        64, 12, -84, 68, 0, 0, 0, 0, 0, 0, 117, 108, 97, 119, 7, 117, 110, 107, 110,
        111, 119, 110, 83, 83, 78, 68, 0, 0, 1, -13, 0, 0, 0, 0, 0, 0, 0, 0, 103,
        103, 103, -1, -1, 91, 77, 103, -45, -25, 91, 77, 73, 73, 77, 103, 103, 77,
        73, 91, -1, -1, 91, 77, 73, 65, 77, -25, -51, -25, 77, 65, 91, -45, -51,
        -1, 65, 58, 65, 91, -1, -1, -25, -1, 77, 62, 65, -1, -59, -63, -1, 65, 58,
        65, -1, -51, -59, -45, 103, 77, 77, -1, -59, -59, -25, 77, 91, -25, -59,
        -59, -25, 91, 91, -1, -45, -45, -1, 91, 103, -45, -59, -51, -1, 103, 103,
        -25, -25, -45, -45, -1, -1, -1, -1, 103, 91, -25, -59, -59, -25, -1, -25,
        -1, 103, 103, -45, -51, -1, 77, 91, -25, -51, -51, -1, 103, -25, -25, 103,
        73, 73, 103, -59, -63, -51, -1, 77, 73, 77, -1, -51, -51, -1, 77, 65, 65,
        77, -25, -45, -25, 91, 73, 73, 77, 77, 91, -25, -45, -25, 103, 77, 77, 77,
        91, -45, -59, -59, -1, 91, 91, -1, -25, -1, -45, -51, -1, 91, 77, 103, -25,
        103, 103, -25, -51, -25, 91, 103, -1, -1, 91, 73, 77, -1, -45, -1, 91, 77,
        77, 103, 103, 77, 73, 91, -25, -25, 103, 65, 65, 91, -25, -45, -25, 77, 65,
        73, 103, -45, -51, 103, 77, 73, 91, 103, 103, 103, 103, 91, 77, 73, 77, 103,
        -1, -1, 103, 91, 77, 77, 91, 91, 103, 103, -1, -25, -25, -1, -25, -25, 103,
        77, 91, -25, -51, -59, -51, -25, -1, -1, -45, -51, -25, 91, 91, -25, -59,
        -51, -25, 91, 77, 103, -45, -59, -63, -25, 77, 77, -25, -63, -59, -45, -25,
        103, 77, 77, 103, -45, -45, -45, -1, 91, 77, 91, -1, -59, -68, -51, 103,
        73, 77, 103, -45, -51, -45, 103, 73, 91, -25, -45, 91, 73, -1, -51, -25,
        73, 62, 77, -25, -25, -1, 103, 91, 77, 73, 77, -1, -45, -45, 103, 73, 73,
        91, 103, 103, 77, 73, 77, 103, -1, 103, 91, 91, 91, 103, -1, -25, -1, 103,
        103, 91, 77, 77, 103, -1, -45, -45, -1, 91, 91, -1, 103, 77, 91, -1, -25,
        91, 65, 73, 103, 103, 73, 73, 77, 91, 91, 73, 73, 91, 91, 91, 91, -1, -25,
        91, 73, 91, -25, -1, 73, 62, 62, 73, 103, -25, -45, 91, 77, 91, 103, 91,
        73, 77, 103, 103, 77, 77, 103, 103, 77, 73, 73, 103, 103, 103, 91, 77, 77,
        91, -25, -1, -25, -25, -45, -59, -59, -45, 73, 56, 65, -25, -68, -63, -25,
        91, -1, -45, -1, -1, -45, -59, -63, -1, 103, 103, -25, -25, 103, 91, -1,
        -45, -51, -25, 103, 91, 91, 103, -25, -25, -25, 103, 73, 77, -1, -51, -45,
        103, 91, 103, -25, -1, 91, 91, 91, -1, -45, -51, -25, 91, 77, 103, -1, -1,
        91, -1, -1, -1, 103, 91, 91, 73, 77, 103, -25, -25, 103, 91, 103, 103, 103,
        -1, -45, -1, 77, 77, -1
    };

}
