/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;

/**
 * @test
 * @bug 4629810
 * @summary MidiSystem.getSoundbank() throws unexpected IOException
 */
public class GetSoundBankIOException {

    public static void main(String args[]) throws Exception {
        boolean failed = false;
        try {
            String filename = "GetSoundBankIOException.java";
            System.out.println("Opening "+filename+" as soundbank...");
            File midiFile = new File(System.getProperty("test.src", "."), filename);
            MidiSystem.getSoundbank(midiFile);
            //Soundbank sBank = MidiSystem.getSoundbank(new NonMarkableIS());
            System.err.println("InvalidMidiDataException was not thrown!");
            failed = true;
        } catch (InvalidMidiDataException invMidiEx) {
            System.err.println("InvalidMidiDataException was thrown. OK.");
        } catch (IOException ioEx) {
            System.err.println("Unexpected IOException was caught!");
            System.err.println(ioEx.getMessage());
            ioEx.printStackTrace();
            failed = true;
        }

        if (failed) throw new Exception("Test FAILED!");
        System.out.println("Test passed.");
    }

    private static class NonMarkableIS extends InputStream {
        int counter = 0;

        public NonMarkableIS() {
        }

        public int read() throws IOException {
            if (counter > 1000) return -1;
            return (++counter) % 256;
        }

        public synchronized void mark(int readlimit) {
            System.out.println("Called mark with readlimit= "+readlimit);
        }

        public synchronized void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }

        public boolean markSupported() {
            return false;
        }

    }
}
