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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4629669
 * @summary AU file reader: problems with empty files
 */
public class AuZeroLength {

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

    public static void test(byte[] file) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(file);
        AudioFileFormat aff = AudioSystem.getAudioFileFormat(inputStream);

        if (aff.getFrameLength() != 0) {
            throw new Exception("File length is "+aff.getFrameLength()+" instead of 0. test FAILED");
        }
        System.out.println(aff.getType()+" file length is 0.");
    }

    public static void main(String[] args) throws Exception {
        test(ZERO_AU);
        test(ZERO_WAV);
        test(ZERO_AIFF);

        System.out.println("Test passed.");
    }

    public static byte[] ZERO_AU = {
        46, 115, 110, 100, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, -84, 68, 0,
        0, 0, 1, 116, 101, 115, 116, 46, 119, 97, 118
    };

    public static byte[] ZERO_WAV = {
        82, 73, 70, 70, 36, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32, 16, 0, 0,
        0, 1, 0, 1, 0, 68, -84, 0, 0, -120, 88, 1, 0, 2, 0, 16, 0, 100, 97, 116,
        97, 0, 0, 0, 0
    };

    public static byte[] ZERO_AIFF = {
        70, 79, 82, 77, 0, 0, 0, 46, 65, 73, 70, 70, 67, 79, 77, 77, 0, 0, 0, 18,
        0, 1, 0, 0, 0, 0, 0, 16, 64, 14, -84, 68, 0, 0, 0, 0, 0, 0, 83, 83, 78, 68,
        0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0
    };

}
