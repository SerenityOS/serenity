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

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

/**
 * @test
 * @bug 4291250
 * @summary Midi files with SMPTE time do not play properly
 */
public class MidiSMPTE {

    public static void main(String[] args) throws Exception {
        Sequence s = null;
        //File midiFile = new File("outsmpte.mid");
        //InputStream is = new FileInputStream(midiFile);
        //is = new BufferedInputStream(is);
        InputStream is = new ByteArrayInputStream(smptemidifile);
        s = MidiSystem.getSequence(is);
        long duration = s.getMicrosecondLength() / 1000000;
        System.out.println("Duration: "+duration+" seconds ");
        if (duration > 14) {
            throw new Exception("SMPTE time reader is broken! Test FAILED");
        }
        System.out.println("Test passed");
    }

    public static void printFile(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        String s = "";
        for (int i=0; i<data.length; i++) {
            s+=String.valueOf(data[i])+", ";
            if (s.length()>72) {
                System.out.println(s);
                s="";
            }
        }
        System.out.println(s);
    }

    // A MIDI file with SMPTE timing
    static byte[] smptemidifile = {
            77, 84, 104, 100, 0, 0, 0, 6, 0, 1, 0, 3, -30, 120, 77, 84, 114, 107, 0,
            0, 0, 123, 0, -112, 30, 100, -113, 49, -128, 50, 100, -114, 69, -112, 31,
            100, -114, 33, -128, 51, 100, -114, 55, -112, 32, 100, -114, 120, -128, 52,
            100, -114, 40, -112, 33, 100, -114, 26, -128, 53, 100, -114, 26, -112, 34,
            100, -114, 76, -128, 54, 100, -114, 12, -112, 35, 100, -114, 91, -128, 55,
            100, -114, 69, -112, 36, 100, -114, 33, -128, 56, 100, -114, 55, -112, 37,
            100, -114, 84, -128, 57, 100, -114, 40, -112, 38, 100, -114, 26, -128, 58,
            100, -114, 26, -112, 39, 100, -113, 24, -128, 59, 100, -113, 60, -112, 40,
            100, -113, 110, -128, 60, 100, -113, 96, -112, 41, 100, -113, 39, -128, 61,
            100, 0, -1, 47, 0, 77, 84, 114, 107, 0, 0, 0, 4, 0, -1, 47, 0, 77, 84, 114,
            107, 0, 0, 0, 4, 0, -1, 47, 0
    };

}
