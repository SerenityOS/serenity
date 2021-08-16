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

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;

/**
 * @test
 * @bug 6415669
 * @summary Tests that terminating thread which got transmitter doesn't cause
 *          JVM crash (windows)
 * @run main bug6415669
 */
public class bug6415669 {

    public static void main(String args[]) throws Exception {
        String osStr = System.getProperty("os.name");
        boolean isWin = osStr.toLowerCase().startsWith("windows");
        log("OS: " + osStr);
        log("Arch: " + System.getProperty("os.arch"));
        if (!isWin) {
            log("The test is for Windows only");
            return;
        }

        bug6415669 This = new bug6415669();
        if (This.test()) {
            log("Test sucessfully passed.");
        } else {
            log("Test FAILED!");
            throw new RuntimeException("Test FAILED!");
        }
    }

    volatile Transmitter transmitter = null;
    Thread openThread = null;
    boolean test() {
        openThread = new Thread(new Runnable() {
            public void run() {
                try {
                    log("openThread: getting transmitter...");
                    transmitter = MidiSystem.getTransmitter();
                    log("openThread:   - OK: " + transmitter);
                } catch (MidiUnavailableException ex) {
                    log("openThread:   - Exception: ");
                    ex.printStackTrace(System.out);
                    log("openThread: skipping...");
                }
                log("openThread: exiting...");
            }
        });
        log("starting openThread...");
        openThread.start();

        while (openThread.isAlive())
            delay(500);
        // make additional delay
        delay(500);

        if (transmitter == null) {
            return true;   // midi is not available, just ignore
        }

        log("closing transmitter");
        transmitter.close();
        log("  - OK");

        return true;
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
