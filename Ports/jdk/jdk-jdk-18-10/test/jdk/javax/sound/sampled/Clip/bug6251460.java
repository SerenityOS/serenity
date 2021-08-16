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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

/**
 * @test
 * @bug 6251460 8047222
 * @requires (os.family == "windows" | os.family == "mac")
 * @summary Tests that JavaSound plays short sounds (less then 1 second)
 */
public class bug6251460 {
    private static final class MutableBoolean {
        public boolean value;

        public MutableBoolean(boolean initialValue) {
            value = initialValue;
        }
    }

    // static helper routines
    static long startTime = currentTimeMillis();
    static long currentTimeMillis() {
        return System.nanoTime() / 1000000L;
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
            + " " + s);
    }


    static private int countErrors = 0;
    static private final int LOOP_COUNT = 30;

    static AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
    // create a 250-ms clip
    static byte[] soundData = new byte[(int) (format.getFrameRate() * format.getFrameSize() * 0.25)];

    static protected void test()
            throws LineUnavailableException, InterruptedException {
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Clip clip = (Clip)AudioSystem.getLine(info);
        final MutableBoolean clipStoppedEvent = new MutableBoolean(false);
        clip.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType() == LineEvent.Type.STOP) {
                    synchronized (clipStoppedEvent) {
                        clipStoppedEvent.value = true;
                        clipStoppedEvent.notifyAll();
                    }
                }
            }
        });
        clip.open(format, soundData, 0, soundData.length);

        long lengthClip = clip.getMicrosecondLength() / 1000;
        log("Clip length " + lengthClip + " ms");
        log("Playing...");
        for (int i=1; i<=LOOP_COUNT; i++) {
            long startTime = currentTimeMillis();
            log(" Loop " + i);
            clip.start();

            synchronized (clipStoppedEvent) {
                while (!clipStoppedEvent.value) {
                    clipStoppedEvent.wait();
                }
                clipStoppedEvent.value = false;
            }

            long endTime = currentTimeMillis();
            long lengthPlayed = endTime - startTime;

            if (lengthClip > lengthPlayed + 20) {
                log(" ERR: Looks like sound didn't play: played " + lengthPlayed + " ms instead " + lengthClip);
                countErrors++;
            } else {
                log(" OK: played " + lengthPlayed + " ms");
            }
            clip.setFramePosition(0);

        }
        log("Played " + LOOP_COUNT + " times, " + countErrors + " errors detected.");
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            test();
        } catch (LineUnavailableException | IllegalArgumentException
                | IllegalStateException ignored) {
            System.out.println("Test is not applicable. Automatically passed");
            return;
        }
        if (countErrors > 0) {
            throw new RuntimeException(
                    "Test FAILED: " + countErrors + " error detected (total "
                            + LOOP_COUNT + ")");
        }
    }
}
