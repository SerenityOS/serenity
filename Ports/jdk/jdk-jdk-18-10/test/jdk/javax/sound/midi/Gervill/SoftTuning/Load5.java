/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @summary Test SoftTuning load method
   @modules java.desktop/com.sun.media.sound
*/

import java.io.UnsupportedEncodingException;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class Load5 {

    private static void assertEquals(Object a, Object b) throws Exception
    {
        if(!a.equals(b))
            throw new RuntimeException("assertEquals fails!");
    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        // http://www.midi.org/about-midi/tuning_extens.shtml
        // 0x05 SCALE/OCTAVE TUNING DUMP, 1 byte format
        SoftTuning tuning = new SoftTuning();

        byte[] name;
        name = "Testing123      ".getBytes("ascii");
        int[] msg = {0xf0,0x7e,0x7f,0x08,0x05,0,0,
                name[0],name[1],name[2],name[3],name[4],name[5],name[6],
                name[7],name[8],name[9],name[10],name[11],name[12],name[13],
                name[14],name[15],
                5,10,15,20,25,30,35,40,45,50,51,52,0,
                0xf7};
        // Calc checksum
        int x = msg[1] & 0xFF;
        for (int i = 2; i < msg.length - 2; i++)
            x = x ^ (msg[i] & 0xFF);
        msg[msg.length-2] = (x & 127);

        int[] oct = {5,10,15,20,25,30,35,40,45,50,51,52};
        byte[] bmsg = new byte[msg.length];
        for (int i = 0; i < bmsg.length; i++)
            bmsg[i] = (byte)msg[i];
        tuning.load(bmsg);
        double[] tunings = tuning.getTuning();
        for (int i = 0; i < tunings.length; i++)
            assertTrue(Math.abs(tunings[i]-(i*100 + (oct[i%12]-64))) < 0.00001);

        // Check if tuning fails if checksum is wrong
        msg[msg.length - 2] += 10;
        for (int i = 0; i < bmsg.length; i++)
            bmsg[i] = (byte)msg[i];
        tuning = new SoftTuning();
        tuning.load(bmsg);
        assertTrue(!tuning.getName().equals("Testing123      "));
    }
}
