/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import static javax.sound.midi.SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
import static javax.sound.midi.SysexMessage.SYSTEM_EXCLUSIVE;

/**
 * @test
 * @bug 8237495
 * @summary fail with a dereferenced memory error when asked to send a raw 0xF7
 */
public final class SendRawSysexMessage {

    private static final class RawMidiMessage extends MidiMessage {
        @Override
        public RawMidiMessage clone() {
            return new RawMidiMessage(getMessage());
        }
        @Override
        public int getStatus() {
            return SYSTEM_EXCLUSIVE; // not that this really matters
        }

        RawMidiMessage(byte[] data) {
            super(data.clone());
        }
    }

    public static void main(String[] args) throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        System.err.println("List of devices to test:");
        for (int i = 0; i < infos.length; i++) {
            System.err.printf("\t%d.\t%s%n", i, infos[i]);
        }
        for (int i = 0; i < infos.length; i++) {
            System.err.printf("%d.\t%s%n", i, infos[i]);
            try {
                test(infos[i]);
            } catch (MidiUnavailableException ignored){
                // try next
            }
        }
    }

    private static void test(MidiDevice.Info info) throws Exception {
        try (MidiDevice device = MidiSystem.getMidiDevice(info)) {
            System.err.println("Sending to " + device + " (" + info + ")");
            if (!device.isOpen())
                device.open();
            try (Receiver r = device.getReceiver()) {
                System.err.println("note on");
                r.send(new ShortMessage(ShortMessage.NOTE_ON, 5, 5), -1);
                System.err.println("sysex");
                r.send(new SysexMessage(new byte[]{
                        (byte) SYSTEM_EXCLUSIVE, 0x0, 0x03, 0x04,
                        (byte) SPECIAL_SYSTEM_EXCLUSIVE}, 5), -1);
                System.err.println("raw 1");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) 0x02, 0x02, 0x03, 0x04}), -1);
                System.err.println("raw 2");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) 0x09, 0x02, 0x03, 0x04}), -1);
                System.err.println("raw 3");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) SYSTEM_EXCLUSIVE, 0x02, 0x03, 0x04}), -1);
                System.err.println("raw 4");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) 0x02, 0x02, 0x03, 0x04}), -1);
                System.err.println("raw 5");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) 0x02, 0x02, 0x03,
                        (byte) SPECIAL_SYSTEM_EXCLUSIVE}), -1);
                System.err.println("raw 6");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) SYSTEM_EXCLUSIVE, 0x02, 0x03, 0x04}), -1);
                System.err.println("sleep");
                Thread.sleep(1000);
                System.err.println("raw 7");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) 0x02, 0x02, 0x03, 0x04}), -1);
                System.err.println("sleep");
                Thread.sleep(1000);
                System.err.println("raw 8");
                r.send(new RawMidiMessage(new byte[]{
                        (byte) SPECIAL_SYSTEM_EXCLUSIVE}), -1);
                System.err.println("note off");
                r.send(new ShortMessage(ShortMessage.NOTE_OFF, 5, 5), -1);
                System.err.println("done, should quit");
                System.err.println();
            }
        }
    }
}
