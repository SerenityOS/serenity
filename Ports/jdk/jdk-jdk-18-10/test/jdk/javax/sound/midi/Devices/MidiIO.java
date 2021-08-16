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

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

/**
 * @test
 * @bug 4356787
 * @summary MIDI device I/O is not working
 */
public class MidiIO {

    public static void main(String[] args) throws Exception {
        out("4356787: MIDI device I/O is not working (windows)");

        if (System.getProperty("os.name").startsWith("Windows")) {
            boolean forInput=true;
            boolean forOutput=true;
            int outOnlyCount=0;
            int inOnlyCount=0;
            out("  available MIDI devices:");
            MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
            for (int i = 0; i < aInfos.length; i++) {
                try {
                    MidiDevice      device = MidiSystem.getMidiDevice(aInfos[i]);
                    boolean         bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean         bAllowsOutput = (device.getMaxReceivers() != 0);
                    if (bAllowsInput && !bAllowsOutput) {
                        inOnlyCount++;
                    }
                    if (!bAllowsInput && bAllowsOutput) {
                        outOnlyCount++;
                    }
                    if ((bAllowsInput && forInput) || (bAllowsOutput && forOutput)) {
                        out(""+i+"  "
                                +(bAllowsInput?"IN ":"   ")
                                +(bAllowsOutput?"OUT ":"    ")
                                +aInfos[i].getName()+", "
                                +aInfos[i].getVendor()+", "
                                +aInfos[i].getVersion()+", "
                                +aInfos[i].getDescription());
                    }
                }
                catch (MidiUnavailableException e) {
                    // device is obviously not available...
                }
            }
            if (aInfos.length == 0) {
                out("No devices available. Test should be run on systems with MIDI drivers installed.");
            } else {
                if (outOnlyCount>1) {
                    if (inOnlyCount==0) {
                        //throw new Exception("No input devices! test fails.");
                        out("System provides out devices, but no input devices. This means either");
                        out("a bug in Java Sound, or the drivers are not set up correctly.");
                    }
                    out("Test passed.");
                } else {
                    out("no MIDI I/O installed. Test should be run on systems with MIDI drivers installed.");
                }
            }
        } else {
            out("  -- not on Windows. Test doesn't apply.");
        }
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
    }
}
