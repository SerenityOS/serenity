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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * @test
 * @bug 4661602
 * @summary Buffersize is checked when re-opening line
 */
public class BufferSizeCheck {

    public static void main(String[] args) throws Exception {
        boolean realTest = false;
        if (!isSoundcardInstalled()) {
            return;
        }

        try {
            out("4661602: Buffersize is checked when re-opening line");
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
            out("Opening with buffersize 12000...");
            sdl.open(format, 12000);
            out("Opening with buffersize 11000...");
            realTest=true;
            sdl.open(format, 11000);
            try {
                sdl.close();
            } catch(Throwable t) {}
        } catch (Exception e) {
            e.printStackTrace();
            // do not fail if no audio device installed - bug 4742021
            if (realTest || !(e instanceof LineUnavailableException)) {
                throw e;
            }
        }
        out("Test passed");
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
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
