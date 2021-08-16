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

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4429762
 * @summary Some instrument names in some soundbanks include bad extra characters
 */
public class ExtraCharInSoundbank {

    private static void printName(String loadedName)
    {
        System.out.println("Loaded Name: " + loadedName);
        byte[] theLoadedNameByteArray = loadedName.getBytes();

        System.out.print("Name Bytes: ");
        for(int i = 0; i < theLoadedNameByteArray.length; i++)
            System.out.print((Integer.toHexString((int)theLoadedNameByteArray[i]).toUpperCase()) + " ");
        System.out.println("");
        System.out.println("");
    }

    private static boolean containsControlChar(String name) {
        byte[] bytes = name.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < 32) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkInstrumentNames(Synthesizer theSynthesizer)
    {
        boolean containsControlCharacters = false;

        Instrument[] theLoadedInstruments = theSynthesizer.getLoadedInstruments();

        System.out.println("Checking soundbank...");
        for(int theInstrumentIndex = 0; theInstrumentIndex < theLoadedInstruments.length; theInstrumentIndex++) {
            String name = theLoadedInstruments[theInstrumentIndex].getName();
            if (containsControlChar(name)) {
                containsControlCharacters = true;
                System.out.print("Instrument[" + theInstrumentIndex + "] contains unexpected control characters: ");
                printName(name);
            }
        }
        return !containsControlCharacters;
    }

    public static void main(String[] args) throws Exception {
        // the internal synthesizer needs a soundcard to work properly
        if (!isSoundcardInstalled()) {
            return;
        }
        Synthesizer theSynth = MidiSystem.getSynthesizer();
        System.out.println("Got synth: "+theSynth);
        theSynth.open();
        try {
            Soundbank theSoundbank = theSynth.getDefaultSoundbank();
            System.out.println("Got soundbank: "+theSoundbank);
            theSynth.loadAllInstruments(theSoundbank);
            try {
                    if (!checkInstrumentNames(theSynth)) {
                            throw new Exception("Test failed");
                    }
            } finally {
                    theSynth.unloadAllInstruments(theSoundbank);
            }
        } finally {
            theSynth.close();
        }
        System.out.println("Test passed.");
    }

    /**
     * Returns true if at least one soundcard is correctly installed
     * on the system.
     */
    public static boolean isSoundcardInstalled() {
        boolean result = false;
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers.length > 0) {
                result = AudioSystem.getSourceDataLine(null) != null;
            }
        } catch (Exception e) {
            System.err.println("Exception occured: "+e);
        }
        if (!result) {
            System.err.println("Soundcard does not exist or sound drivers not installed!");
            System.err.println("This test requires sound drivers for execution.");
        }
        return result;
    }
}
