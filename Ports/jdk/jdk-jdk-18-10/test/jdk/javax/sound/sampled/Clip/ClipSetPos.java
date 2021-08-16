/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6801206
 * @summary Tests that Clip sets frame position
 * @author Alex Menkov
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

public class ClipSetPos {

    final static AudioFormat audioFormat = new AudioFormat(44100f, 16, 2, true, true);
    final static int frameLength = 44100 * 2; // 2 seconds
    final static byte[] dataBuffer
            = new byte[frameLength * (audioFormat.getSampleSizeInBits()/8)
                       * audioFormat.getChannels()];
    final static int MAX_FRAME_DELTA = 20;

    public static void main(String[] args) {
        boolean testPassed = true;
        Clip clip = null;
        try {
            clip = (Clip)AudioSystem.getLine(new DataLine.Info(Clip.class, audioFormat));
            clip.open(audioFormat, dataBuffer, 0, dataBuffer.length);
        } catch (LineUnavailableException ex) {
            log(ex);
            log("Cannot test (this is not failure)");
            return;
        } catch (IllegalArgumentException ex) {
            log(ex);
            log("Cannot test (this is not failure)");
            return;
        }

        log("clip: " + clip.getClass().getName());

        int len = clip.getFrameLength();
        for (int pos=0; pos < len; pos += (len /100)) {
            clip.setFramePosition(pos);
            int curPos = clip.getFramePosition();
            if (Math.abs(pos - curPos) > MAX_FRAME_DELTA) {
                log("Tried to set pos to " + pos + ", but got back " + curPos);
                testPassed = false;
            } else {
                log("Sucessfully set pos to " + pos);
            }
        }
        clip.close();

        if (testPassed) {
            log("Test PASSED.");
        } else {
            log("Test FAILED.");
            throw new RuntimeException("Test FAILED (see log)");
        }
    }

    static void log(String s) {
        System.out.println(s);
    }

    static void log(Exception ex) {
        ex.printStackTrace(System.out);
    }

}
