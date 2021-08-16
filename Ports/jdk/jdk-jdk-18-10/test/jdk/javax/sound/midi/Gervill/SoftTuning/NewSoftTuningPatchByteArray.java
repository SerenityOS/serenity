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
   @summary Test SoftTuning constructor
   @modules java.desktop/com.sun.media.sound
*/

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class NewSoftTuningPatchByteArray {

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
        // RealTime: Scale/Octave tuning in 1-byte format
        int[] msg = {0xf0,0x7f,0x7f,0x08,0x08,0x03,0x7f,0x7f,
                5,10,15,20,25,30,35,40,45,50,51,52,
                0xf7};
        int[] oct = {5,10,15,20,25,30,35,40,45,50,51,52};
        byte[] bmsg = new byte[msg.length];
        for (int i = 0; i < bmsg.length; i++)
            bmsg[i] = (byte)msg[i];
        SoftTuning tuning = new SoftTuning(new Patch(8,32),bmsg);
        double[] tunings = tuning.getTuning();
        for (int i = 0; i < tunings.length; i++)
            assertTrue(Math.abs(tunings[i]-(i*100 + (oct[i%12]-64))) < 0.00001);
        assertEquals(tuning.getPatch().getProgram(), 32);
        assertEquals(tuning.getPatch().getBank(), 8);
    }
}
