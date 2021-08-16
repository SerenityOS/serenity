/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

/**
 * @test
 * @bug 4685396
 * @summary Tests that Synthesizer.remapInstrument works
 * @run main bug4685396
 */
public class bug4685396 {

    static Synthesizer synth = null;

    public static boolean isInstrumentExist(Instrument inst, Instrument[] insts) {
        for (int i = 0; i < insts.length; i++) {
            if (inst.equals(insts[i]))
                return true;
        }
        return false;
    }

    static boolean test(
            boolean reloadInstr,    // reload all instruments?
            boolean unloadFrom,     // unload "from" instrument?
            boolean unloadTo        // unload "to" instrument?
            ) throws MidiUnavailableException {
        log("Starting test: reloadInstr=" + reloadInstr
                + ", unloadFrom=" + unloadFrom
                + ", unloadTo=" + unloadTo
                + "");

        log("  creating synthesizer...");
        synth = MidiSystem.getSynthesizer();
        log("  opening synthesizer...");
        synth.open();

        Soundbank sbank = synth.getDefaultSoundbank();
        if (sbank == null)
            throw new RuntimeException("ERROR: Could not get default soundbank");

        if (reloadInstr) {
            synth.unloadAllInstruments(sbank);
            synth.loadAllInstruments(sbank);
        }

        Instrument[] instrs = synth.getLoadedInstruments();

        log("  " + instrs.length + " instruments loaded.");

        if (instrs.length < 2)
            throw new RuntimeException("ERROR: need at least 2 loaded instruments");

        Instrument from = instrs[0];
        Instrument to = instrs[instrs.length - 1];

        if (unloadFrom)
            synth.unloadInstrument(from);
        if (unloadTo)
            synth.unloadInstrument(to);

        log("  from instrument (" + (unloadFrom ? "UNLOADED" : "LOADED")
                                + "): " + from.toString());
        log("  to instrument (" + (unloadTo ? "UNLOADED" : "LOADED")
                                + "): " + to.toString());

        boolean result = false;
        boolean excepted = false;
        try {
            result = synth.remapInstrument(from, to);
            log("  remapInstrument(from, to) returns " + result);
        } catch (IllegalArgumentException ex) {
            excepted = true;
            log("  EXCEPTION:");
            ex.printStackTrace(System.out);
        }

        instrs = synth.getLoadedInstruments();
        log("  " + instrs.length + " instruments remains loaded.");

        boolean toUnloaded = !isInstrumentExist(to, instrs);
        boolean fromUnloaded = !isInstrumentExist(from, instrs);

        log("  from instrument is " + (fromUnloaded ? "UNLOADED" : "LOADED"));
        log("  to instrument is " + (toUnloaded ? "UNLOADED" : "LOADED"));

        boolean bOK = true;
        if (result) {
            if (unloadTo) {
                bOK = false;
                log("ERROR: unloaded to, but sucessfull remap");
            }
            if (!fromUnloaded) {
                bOK = false;
                log("ERROR: sucessfull remap, but from hasn't been unloaded");
            }
            if (toUnloaded) {
                bOK = false;
                log("ERROR: to has been unloaded!");
            }
        } else {
            if (!excepted) {
                bOK = false;
                log("ERROR: remap returns false, exception hasn't been thrown");
            }
            if (!unloadTo) {
                bOK = false;
                log("ERROR: to is loaded, but remap returns false");
            }
            if (unloadFrom != fromUnloaded) {
                bOK = false;
                log("ERROR: remap returns false, but status of from has been changed");
            }
        }

        if (bOK) {
            log("Test result: OK\n");
        } else {
            log("Test result: FAIL\n");
        }

        return bOK;
    }

    static void cleanup() {
        if (synth != null) {
            synth.close();
            synth = null;
        }
    }

    static boolean runTest(
            boolean reloadInstr,    // reload all instruments?
            boolean unloadTo,       // unload "to" instrument?
            boolean unloadFrom      // unload "from" instrument?
            )
    {
        boolean success = false;
        try {
            success = test(reloadInstr, unloadFrom, unloadTo);
        } catch (final MidiUnavailableException ignored) {
            // the test is not applicable
            success = true;
        } catch (Exception ex) {
            log("Exception: " + ex.toString());
        }
        cleanup();
        return success;
    }

    public static void main(String args[]) {
        boolean failed = false;
        if (!runTest(true, false, false))
            failed = true;
        if (!runTest(true, false, true))
            failed = true;
        if (!runTest(true, true, false))
            failed = true;
        if (!runTest(true, true, true))
            failed = true;

        if (failed) {
            throw new RuntimeException("Test FAILED.");
        }
        log("Test sucessfully passed.");
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
}
