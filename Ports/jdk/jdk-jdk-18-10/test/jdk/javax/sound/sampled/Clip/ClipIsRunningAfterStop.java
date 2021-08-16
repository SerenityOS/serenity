/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

/**
 * @test
 * @bug 8207150
 * @summary Clip.isRunning() may return true after Clip.stop() was called
 */
public final class ClipIsRunningAfterStop {

    private static volatile Exception failed;

    public static void main(final String[] args) throws Exception {
        final Runnable r = () -> {
            try {
                test();
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // the test is not applicable
            } catch (Exception ex) {
                failed = ex;
            }
        };
        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        Thread t3 = new Thread(r);
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
        if (failed != null) {
            throw new RuntimeException(failed);
        }
    }

    private static void test() throws Exception {
        // Will run the test no more than 15 seconds
        long endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (failed == null && endtime - System.nanoTime() > 0) {
            Clip clip = createClip();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.stop();
            if (clip.isRunning()) {
                if (clip.isRunning()) {
                    throw new RuntimeException("Clip is running");
                }
            }
            if (clip.isActive()) {
                if (clip.isActive()) {
                    throw new RuntimeException("Clip is active");
                }
            }
            clip.close();
        }
    }

    private static Clip createClip() throws LineUnavailableException {
        AudioFormat format =
                new AudioFormat(PCM_SIGNED, 44100, 8, 1, 1, 44100, false);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(format, new byte[2], 0, 2);
        return clip;
    }
}
