/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;

/**
 * @test
 * @bug 6186488
 * @summary Tests that software Java Syntesizer processed
 *          non-ShortMessage-derived messages
 * @run main/manual=yesno bug6186488
 */
public class bug6186488 {
    public static void main(String[] args) throws Exception {
        MidiDevice/*Synthesizer*/ synth = null;

        try {
            synth = MidiSystem.getSynthesizer();
            //synth = MidiSystem.getMidiDevice(infos[0]);

            System.out.println("Synthesizer: " + synth.getDeviceInfo());
            synth.open();
            MidiMessage msg = new GenericMidiMessage(0x90, 0x3C, 0x40);
            //ShortMessage msg = new ShortMessage();
            //msg.setMessage(0x90, 0x3C, 0x40);

            synth.getReceiver().send(msg, 0);
            Thread.sleep(2000);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            if (synth != null && synth.isOpen())
                synth.close();
        }
        System.out.print("Did you heard a note? (enter 'y' or 'n') ");
        int result = System.in.read();
        System.in.skip(1000);
        if (result == 'y' || result == 'Y')
        {
            System.out.println("Test passed sucessfully.");
        }
        else
        {
            System.out.println("Test FAILED.");
            throw new RuntimeException("Test failed.");
        }
    }

    private static class GenericMidiMessage extends MidiMessage {
        GenericMidiMessage(int... message) {
            super(new byte[message.length]);
            for (int i=0; i<data.length; i++) {
                data[i] = (byte)(0xFF & message[i]);
            }
        }

        GenericMidiMessage(byte... message) {
            super(message);
        }

        public Object clone() {
            return new GenericMidiMessage((byte[])data.clone());
        }
    }
}
