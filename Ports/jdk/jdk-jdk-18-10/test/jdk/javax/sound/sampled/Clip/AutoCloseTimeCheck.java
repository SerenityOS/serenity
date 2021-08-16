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

import java.applet.AudioClip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

/**
 * @test
 * @bug 8202264
 */
public final class AutoCloseTimeCheck {

    public static void main(final String[] args) throws Exception {
        // Prepare the audio file
        File file = new File("audio.wav");
        try {
            AudioFormat format =
                    new AudioFormat(PCM_SIGNED, 44100, 8, 1, 1, 44100, false);
            AudioSystem.write(getStream(format), Type.WAVE, file);
        } catch (final Exception ignored) {
            return; // the test is not applicable
        }
        try {
            testSmallDelay(file);
            testBigDelay(file);
        } finally {
            Files.delete(file.toPath());
        }
    }

    /**
     * Checks that after a big period of non-activity the clip will be closed
     * and the "Direct Clip" thread will stop.
     */
    private static void testBigDelay(final File file) throws Exception {
        AudioClip clip = (AudioClip) file.toURL().getContent();
        clip.loop();
        clip.stop();
        sleep(20000); // 20 sec for slow systems
        if (count() != 0) {
            throw new RuntimeException("Thread was found");
        }
    }

    /**
     * Checks that after small period of non-activity the clip will not be
     * closed and the "Direct Clip" thread will alive.
     */
    private static void testSmallDelay(final File file) throws IOException {
        AudioClip clip = (AudioClip) file.toURL().getContent();
        long threadID = 0;
        // Will run the test no more than 15 seconds
        long endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (endtime - System.nanoTime() > 0) {
            clip.loop();
            sleep(500);

            long data = count();
            if (data != threadID) {
                System.out.println("Playing on new thread: " + data + " at "
                                           + new java.util.Date());
                if (threadID == 0) {
                    threadID = data;
                } else {
                    throw new RuntimeException("Thread was changed");
                }
            }

            clip.stop();
            sleep(500);
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static long count() {
        for (final Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals("Direct Clip")) {
                return t.getId();
            }
        }
        return 0;
    }

    private static AudioInputStream getStream(final AudioFormat format) {
        final int dataSize = 5000 * format.getFrameSize();
        final InputStream in = new ByteArrayInputStream(new byte[dataSize]);
        return new AudioInputStream(in, format, NOT_SPECIFIED);
    }
}
