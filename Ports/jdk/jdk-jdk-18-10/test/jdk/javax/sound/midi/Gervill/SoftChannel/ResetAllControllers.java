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
   @summary Test SoftChannel resetAllControllers method
   @modules java.desktop/com.sun.media.sound
*/

import javax.sound.midi.*;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class ResetAllControllers {

    public static boolean[] dontResetControls = new boolean[128];
    static {
        for (int i = 0; i < dontResetControls.length; i++)
            dontResetControls[i] = false;

        dontResetControls[0] = true;   // Bank Select (MSB)
        dontResetControls[32] = true;  // Bank Select (LSB)
        dontResetControls[7] = true;   // Channel Volume (MSB)
        dontResetControls[8] = true;   // Balance (MSB)
        dontResetControls[10] = true;  // Pan (MSB)
        dontResetControls[11] = true;  // Expression (MSB)
        dontResetControls[91] = true;  // Effects 1 Depth (default: Reverb Send)
        dontResetControls[92] = true;  // Effects 2 Depth (default: Tremolo Depth)
        dontResetControls[93] = true;  // Effects 3 Depth (default: Chorus Send)
        dontResetControls[94] = true;  // Effects 4 Depth (default: Celeste [Detune] Depth)
        dontResetControls[95] = true;  // Effects 5 Depth (default: Phaser Depth)
        dontResetControls[70] = true;  // Sound Controller 1 (default: Sound Variation)
        dontResetControls[71] = true;  // Sound Controller 2 (default: Timbre / Harmonic Quality)
        dontResetControls[72] = true;  // Sound Controller 3 (default: Release Time)
        dontResetControls[73] = true;  // Sound Controller 4 (default: Attack Time)
        dontResetControls[74] = true;  // Sound Controller 5 (default: Brightness)
        dontResetControls[75] = true;  // Sound Controller 6 (GM2 default: Decay Time)
        dontResetControls[76] = true;  // Sound Controller 7 (GM2 default: Vibrato Rate)
        dontResetControls[77] = true;  // Sound Controller 8 (GM2 default: Vibrato Depth)
        dontResetControls[78] = true;  // Sound Controller 9 (GM2 default: Vibrato Delay)
        dontResetControls[79] = true;  // Sound Controller 10 (GM2 default: Undefined)
        dontResetControls[120] = true; // All Sound Off
        dontResetControls[121] = true; // Reset All Controllers
        dontResetControls[122] = true; // Local Control On/Off
        dontResetControls[123] = true; // All Notes Off
        dontResetControls[124] = true; // Omni Mode Off
        dontResetControls[125] = true; // Omni Mode On
        dontResetControls[126] = true; // Poly Mode Off
        dontResetControls[127] = true; // Poly Mode On

        dontResetControls[6] = true;   // Data Entry (MSB)
        dontResetControls[38] = true;  // Data Entry (LSB)
        dontResetControls[96] = true;  // Data Increment
        dontResetControls[97] = true;  // Data Decrement
        dontResetControls[98] = true;  // Non-Registered Parameter Number (LSB)
        dontResetControls[99] = true;  // Non-Registered Parameter Number(MSB)
        dontResetControls[100] = true; // RPN = Null
        dontResetControls[101] = true; // RPN = Null
    }

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
        SoftTestUtils soft = new SoftTestUtils();
        MidiChannel channel = soft.synth.getChannels()[0];

        // First let all controls contain non-default values
        for (int i = 0; i < 128; i++)
            channel.setPolyPressure(i, 10);
        channel.setChannelPressure(10);
        channel.setPitchBend(2192);
        for (int i = 0; i < 120; i++)
            channel.controlChange(i, 1);
        channel.resetAllControllers();

        // Now check if resetAllControllers did what it was suppose to do

        for (int i = 0; i < 128; i++)
            assertEquals(channel.getPolyPressure(i), 0);
        assertEquals(channel.getChannelPressure(), 0);
        assertEquals(channel.getPitchBend(),8192);
        for (int i = 0; i < 120; i++)
            if(!dontResetControls[i])
                assertEquals(channel.getController(i), 0);
        assertEquals(channel.getController(71), 64); // Filter Resonance
        assertEquals(channel.getController(72), 64); // Release Time
        assertEquals(channel.getController(73), 64); // Attack Time
        assertEquals(channel.getController(74), 64); // Brightness
        assertEquals(channel.getController(75), 64); // Decay Time
        assertEquals(channel.getController(76), 64); // Vibrato Rate
        assertEquals(channel.getController(77), 64); // Vibrato Depth
        assertEquals(channel.getController(78), 64); // Vibrato Delay
        assertEquals(channel.getController(8), 64); // Balance
        assertEquals(channel.getController(11), 127); // Expression
        assertEquals(channel.getController(98), 127); // NRPN Null
        assertEquals(channel.getController(99), 127); // NRPN Null
        assertEquals(channel.getController(100), 127); // RPN = Null
        assertEquals(channel.getController(101), 127); // RPN = Null

        soft.close();
    }
}
