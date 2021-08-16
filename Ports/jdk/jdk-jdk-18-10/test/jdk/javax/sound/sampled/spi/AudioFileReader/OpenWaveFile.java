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
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4489272
 * @summary AudioSystem.getAudioFileFormat() fails for InputStream, but works
 *          for URL
 */
public class OpenWaveFile {

    static void check(Object source) throws Exception {
         AudioFileFormat aff2 = null;
         if (source instanceof File) {
            aff2 = AudioSystem.getAudioFileFormat((File) source);
         }
         else if (source instanceof InputStream) {
            aff2 = AudioSystem.getAudioFileFormat((InputStream) source);
         }
         else if (source instanceof URL) {
            aff2 = AudioSystem.getAudioFileFormat((URL) source);
         } else throw new Exception("wrong source. Test FAILED");
         System.out.println("Got: "+aff2);
         if (aff2.getFormat().getSampleSizeInBits()==-1) {
            throw new Exception("wrong audio format. Test FAILED");
         }
    }

    public static void main(String args[]) throws Exception {
         //check(new File(args[0]));
         //check(new URL("file", "", args[0]));
         check(new ByteArrayInputStream(SHORT_AU));
         check(new ByteArrayInputStream(SHORT_WAVE));
         check(new ByteArrayInputStream(SHORT_AIFF));
         System.out.println("Test passed.");

         //printFile(args[0]);
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

    public static byte[] SHORT_WAVE = {
        82, 73, 70, 70, -120, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32, 16, 0,
        0, 0, 1, 0, 1, 0, 34, 86, 0, 0, 34, 86, 0, 0, 1, 0, 8, 0, 100, 97, 116, 97,
        100, 0, 0, 0, -128, -128, -128, -128, -128, -128, -128, -128, -128, -128,
        -128, -128, -128, -128, -128, -128, -128, -128, -128, 127, 127, -128, 127,
        127, 127, -128, -128, -128, -128, 127, 127, -128, -128, 127, -128, -128,
        -128, 127, 127, 127, -128, -128, -128, 127, 127, 127, 127, -128, -128, -128,
        -128, -128, -128, 127, 127, 127, -128, -128, -128, -128, -128, 127, -128,
        -128, 127, -128, -128, 127, 127, -128, -128, 127, 127, -128, -128, -128,
        -128, -128, 127, 127, -128, -128, -128, 127, 127, 127, -128, 127, -128, -128,
        127, 127, 127, -128, -128, -128, 127, 127, -128, -128,
    };

    public static byte[] SHORT_AU = {
        46, 115, 110, 100, 0, 0, 0, 24, 0, 0, 0, 100, 0, 0, 0, 2, 0, 0, 86, 34, 0,
        0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1,
        0, -1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0, -1, 0, 0, 0, -1, -1, -1, 0, 0, 0,
        -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, -1, 0, 0, -1,
        0, 0, -1, -1, 0, 0, -1, -1, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, -1, -1, -1, 0,
        -1, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1, 0, 0,
    };

    public static byte[] SHORT_AIFF = {
        70, 79, 82, 77, 0, 0, 0, -110, 65, 73, 70, 70, 67, 79, 77, 77, 0, 0, 0, 18,
        0, 1, 0, 0, 0, 100, 0, 8, 64, 13, -84, 68, 0, 0, 0, 0, 0, 0, 83, 83, 78,
        68, 0, 0, 0, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, -1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 0,
        -1, 0, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, -1, -1,
        -1, 0, 0, 0, 0, 0, -1, 0, 0, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, 0, 0,
        0, -1, -1, 0, 0, 0, -1, -1, -1, 0, -1, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1,
        0, 0,
    };
}
