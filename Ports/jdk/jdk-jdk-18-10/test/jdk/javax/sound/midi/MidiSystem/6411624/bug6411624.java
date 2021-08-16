/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * This test should be run on specific environment (solaris or linux w/o
 * audio card installed).
 */
public class bug6411624 {

    public static void main(String args[]) throws Exception {
        log("This test should only be run on solaris or linux system");
        log("without audio card installed (to test on SunRay set");
        log("incorrect $AUDIODEV value).");
        readln();

        boolean testRecv = false;
        boolean testTrans = false;
        boolean testSeq = true;

        // print add info (midi device list)
        try {
            MidiDevice.Info[] midis = MidiSystem.getMidiDeviceInfo();
            log("MidiDevices (total " + midis.length + "):");
            for (int i=0; i<midis.length; i++) {
                log("" + i + ": " + midis[i].toString());
//                MidiDevice dev = MidiSystem.getMidiDevice(midis[i]);
//                log("    device: " + dev);
            }
        } catch (Exception ex) {
            log("!!!EXCEPTION:");
            ex.printStackTrace();
        }

        log("");
        log("getting default receiver...");
        try {
            Receiver rec = MidiSystem.getReceiver();
            log(" - OK: " + rec);
            testRecv = checkDevice(rec);
            rec.close();
        } catch (MidiUnavailableException e) {
            log("MidiUnavailableException has been thrown - OK");
            testRecv = true;
        }


        log("");
        log("getting default transmitter...");
        try {
            Transmitter trans = MidiSystem.getTransmitter();
            log(" - OK: " + trans);
            testTrans = checkDevice(trans);
            trans.close();
        } catch (MidiUnavailableException e) {
            log("MidiUnavailableException has been thrown - OK");
            testTrans = true;
        }


        // print default synthesizer
        log("");
        log("getting default synth...");
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            log(" - OK: " + synth);
            synth.close();
        } catch (MidiUnavailableException e) {
            log("MidiUnavailableException has been thrown - OK:");
            e.printStackTrace();
        }


        log("");
        log("getting default sequencer (connected)...");
        try {
            Sequencer seq = MidiSystem.getSequencer();
            log("OK: " + seq);

            // check that returned sequencer doesn't connected to another sequencer
            log("  receivers:");
            log("    max=" + seq.getMaxReceivers());
            List<Receiver> recvList = seq.getReceivers();
            log("    count=" + recvList.size());
            Iterator<Receiver> recvIter = recvList.iterator();
            int i = 0;
            while (recvIter.hasNext()) {
                Receiver recv = recvIter.next();
                log("    " + (++i) + ": " + recv);
            }

            log("  transmitters:");
            log("    max=" + seq.getMaxTransmitters());
            List<Transmitter> transList = seq.getTransmitters();
            log("    count=" + transList.size());
            Iterator<Transmitter> transIter = transList.iterator();
            i = 0;
            while (transIter.hasNext()) {
                Transmitter trans = transIter.next();
                log("    " + (++i) + ": " + trans);
                Receiver recv = trans.getReceiver();
                log("      recv: " + recv);
                if (!checkDevice(recv))
                    testSeq = false;
            }

            log("opening sequencer...");
            seq.open();
            log("OK.");

            log("closing...");
            seq.close();
            log("OK.");
        } catch (MidiUnavailableException e) {
            log("MidiUnavailableException has been thrown - OK:");
            e.printStackTrace();
        }


        // debug testing - non-connected sequencer
        log("");
        log("getting default sequencer (non-connected)...");
        try {
            Sequencer seq = MidiSystem.getSequencer(false);
            log("OK: " + seq);

            log("  receivers:");
            log("    max=" + seq.getMaxReceivers());
            List<Receiver> recvList = seq.getReceivers();
            log("    count=" + recvList.size());
            Iterator<Receiver> recvIter = recvList.iterator();
            int i = 0;
            while (recvIter.hasNext()) {
                Receiver recv = recvIter.next();
                log("    " + (++i) + ": " + recv);
            }

            log("  transmitters:");
            log("    max=" + seq.getMaxTransmitters());
            List<Transmitter> transList = seq.getTransmitters();
            log("    count=" + transList.size());
            Iterator<Transmitter> transIter = transList.iterator();
            i = 0;
            while (transIter.hasNext()) {
                Transmitter trans = transIter.next();
                log("    " + (++i) + ": " + trans);
                Receiver recv = trans.getReceiver();
                log("      recv: " + recv);
            }
            seq.close();
        } catch (MidiUnavailableException e) {
            log("MidiUnavailableException has been thrown (shouln't?):");
            e.printStackTrace();
        }

        log("");
        log("Test result:");
        // print results
        if (testRecv && testTrans && testSeq) {
            log("  All tests sucessfully passed.");
        } else {
            log("  Some tests failed:");
            log("    receiver test:    " + (testRecv ? "OK" : "FAILED"));
            log("    transmitter test: " + (testTrans ? "OK" : "FAILED"));
            log("    sequencer test:   " + (testSeq ? "OK" : "FAILED"));
        }
        log("\n\n\n");
    }

    // check that device is not sequencer
    static boolean checkDevice(Object dev) {
        String className = dev.getClass().toString().toLowerCase();
        boolean result = (className.indexOf("sequencer") < 0);
        if (!result)
            log("ERROR: inapropriate device");
        return result;
    }

    // helper routines
    static long startTime = currentTimeMillis();
    static long currentTimeMillis() {
        //return System.nanoTime() / 1000000L;
        return System.currentTimeMillis();
    }
    static void log(String s) {
        long time = currentTimeMillis() - startTime;
        long ms = time % 1000;
        time /= 1000;
        long sec = time % 60;
        time /= 60;
        long min = time % 60;
        time /= 60;
        System.out.println(""
                + (time < 10 ? "0" : "") + time
                + ":" + (min < 10 ? "0" : "") + min
                + ":" + (sec < 10 ? "0" : "") + sec
                + "." + (ms < 10 ? "00" : (ms < 100 ? "0" : "")) + ms
                + " (" + Thread.currentThread().getName() + ") " + s);
    }
    static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {}
    }
    static void readln() {
        log("");
        log("Press ENTER to continue...");
        try {
            while (System.in.read() != 10) ;
        } catch (IOException e) { }
        log("");
    }
}
