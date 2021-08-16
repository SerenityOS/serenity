/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4933700
 * @summary Tests that default devices return MidiDeviceTransmitter/Receiver and returned objects return correct MidiDevice
 * @compile TestAllDevices.java
 * @run main TestAllDevices
 * @author Alex Menkov
 */

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiDeviceTransmitter;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;


public class TestAllDevices {

    static boolean failed = false;

    public static void main(String[] args) throws Exception {
        out("default receiver:");
        try {
            Receiver recv = MidiSystem.getReceiver();
            out("  receiver: " + recv);
            if (recv instanceof MidiDeviceReceiver) {
                out("    OK");
            } else {
                out("    ERROR: not an instance of MidiDeviceReceiver");
                failed = true;
            }
        } catch (MidiUnavailableException ex) {
            // this is not an error
            out("  receiver: MidiUnavailableException (test NOT failed)");
        }

        out("default transmitter:");
        try {
            Transmitter tran = MidiSystem.getTransmitter();
            out("  transmitter: " + tran);
            if (tran instanceof MidiDeviceTransmitter) {
                out("    OK");
            } else {
                out("    ERROR: not an instance of MidiDeviceTransmitter");
                failed = true;
            }
        } catch (MidiUnavailableException ex) {
            // this is not an error
            out("  transmitter: MidiUnavailableException (test NOT failed)");
        }

        MidiDevice.Info[] infos = MidiSystem .getMidiDeviceInfo();
        for (MidiDevice.Info info: infos) {
            out(info.toString() + ":");
            try {
                MidiDevice dev = MidiSystem.getMidiDevice(info);
                dev.open();

                try {
                    Receiver recv = dev.getReceiver();
                    out("  receiver: " + recv);
                    if (recv instanceof MidiDeviceReceiver) {
                        MidiDeviceReceiver devRecv = (MidiDeviceReceiver)recv;
                        MidiDevice retDev = devRecv.getMidiDevice();
                        if (retDev == dev) {
                            out("    OK");
                        } else {
                            out("    ERROR: getMidiDevice returned incorrect device: " + retDev);
                            failed = true;
                        }
                    } else {
                        out("    ERROR: not an instance of MidiDeviceReceiver");
                        failed = true;
                    }
                } catch (MidiUnavailableException ex) {
                    // this is not an error
                    out("  receiver: MidiUnavailableException (test NOT failed)");
                }

                try {
                    Transmitter tran = dev.getTransmitter();
                    out("  transmitter: " + tran);
                    if (tran instanceof MidiDeviceTransmitter) {
                        MidiDeviceTransmitter devTran = (MidiDeviceTransmitter)tran;
                        MidiDevice retDev = devTran.getMidiDevice();
                        if (retDev == dev) {
                            out("    OK");
                        } else {
                            out("    ERROR: getMidiDevice retur4ned incorrect device: " + retDev);
                            failed = true;
                        }
                    } else {
                        out("    ERROR: not an instance of MidiDeviceTransmitter");
                        failed = true;
                    }
                } catch (MidiUnavailableException ex) {
                    // this is not an error
                    out("  transmitter: MidiUnavailableException (test NOT failed)");
                }

                dev.close();
            } catch (MidiUnavailableException ex) {
                out("  device: MidiUnavailableException (test NOT failed)");
            }
        }

        if (failed) {
            throw new Exception("Test failed.");
        }
    }

    static void out(String s) {
        System.out.println(s);
    }

}
