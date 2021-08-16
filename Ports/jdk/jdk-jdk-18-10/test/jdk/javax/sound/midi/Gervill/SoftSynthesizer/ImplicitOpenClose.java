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
   @summary Test SoftSynthesizer implicit open/close using getReceiver.
   @modules java.desktop/com.sun.media.sound:+open
*/

import java.lang.reflect.Field;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.*;
import javax.sound.midi.MidiDevice.Info;

import com.sun.media.sound.*;

public class ImplicitOpenClose {

    public static void main(String[] args) throws Exception {
        Field f = SoftSynthesizer.class.getDeclaredField("testline");
        f.setAccessible(true);
        f.set(null, new DummySourceDataLine());

        Synthesizer synth = new SoftSynthesizer();

        ReferenceCountingDevice rcd = (ReferenceCountingDevice)synth;

        // Test single open/close cycle

        Receiver recv = rcd.getReceiverReferenceCounting();
        if(!synth.isOpen())
            throw new Exception("Synthesizer not open!");
        recv.close();
        if(synth.isOpen())
            throw new Exception("Synthesizer not closed!");

        // Test using 2 receiver cycle

        Receiver recv1 = rcd.getReceiverReferenceCounting();
        if(!synth.isOpen())
            throw new Exception("Synthesizer not open!");
        Receiver recv2 = rcd.getReceiverReferenceCounting();
        if(!synth.isOpen())
            throw new Exception("Synthesizer not open!");

        recv2.close();
        if(!synth.isOpen())
            throw new Exception("Synthesizer was closed!");
        recv1.close();
        if(synth.isOpen())
            throw new Exception("Synthesizer not closed!");

        // Test for explicit,implicit conflict

        synth.open();
        Receiver recv3 = rcd.getReceiverReferenceCounting();
        if(!synth.isOpen())
            throw new Exception("Synthesizer not open!");
        recv3.close();
        if(!synth.isOpen())
            throw new Exception("Synthesizer was closed!");
        synth.close();
        if(synth.isOpen())
            throw new Exception("Synthesizer not closed!");

        // Test for implicit,explicit conflict

        recv3 = rcd.getReceiverReferenceCounting();
        synth.open();
        if(!synth.isOpen())
            throw new Exception("Synthesizer not open!");
        recv3.close();
        if(!synth.isOpen())
            throw new Exception("Synthesizer was closed!");
        synth.close();
        if(synth.isOpen())
            throw new Exception("Synthesizer not closed!");

    }
}
