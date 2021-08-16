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

/*
 * @test
 * @bug 6400879 7100140
 * @summary Tests that Start/Stop sequence doesn't hang
 * @author Alexey Menkov
 * @run main/othervm bug6400879
 */

import java.util.concurrent.TimeUnit;

import javax.sound.sampled.*;

public class bug6400879 extends Thread {

    public static void main(String args[]) throws Exception {
        bug6400879 pThis = new bug6400879();
        //pThis.init();
        pThis.setDaemon(true);
        pThis.start();
        monitor(pThis);
    }

    static final long BLOCK_TIMEOUT = 5000;    // 5 sec

    // monitors that pThis doesn't hang
    public static void monitor(bug6400879 pThis) throws Exception {
        long prevLoop = -1;
        long prevTime = currentTimeMillis();
        while (pThis.isAlive()) {
            if (pThis.loopCounter == prevLoop) {
                long delay = currentTimeMillis() - prevTime;
                if (delay > BLOCK_TIMEOUT) {
                    // blocked?
                    log("The test is slow, delay = " + delay);
                }
            } else {
                prevLoop = pThis.loopCounter;
                prevTime = currentTimeMillis();
            }
            delay(1000);    // sleep for 1 sec
        }
        log("Test sucessfully passed.");
    }

    volatile long loopCounter = 0;
    final long LOOPS_PER_LINE = 100;

    public void run() {
        SourceDataLine line = null;

        DataLine.Info line_info = new DataLine.Info(SourceDataLine.class, null);
        Line.Info infos[] = AudioSystem.getSourceLineInfo(line_info);

        log("total " + infos.length + " lines");

        for (int lineNum = 0; lineNum < infos.length; lineNum++) {
            try {
                line = (SourceDataLine)AudioSystem.getLine(infos[lineNum]);
                log("testing line: " + line);
                line.open(line.getFormat());
                for (int i=0; i<LOOPS_PER_LINE; i++) {
                    log("start->stop (" + i + ")");
                    line.start();
                    line.stop();
                    log(" - OK");
                    loopCounter++;
                }
                line.close();
                line = null;
            } catch (LineUnavailableException e1) {
                log("LineUnavailableException caught, test okay.");
                log(e1.getMessage());
            } catch (SecurityException e2) {
                log("SecurityException caught, test okay.");
                log(e2.getMessage());
            } catch (IllegalArgumentException e3) {
                log("IllegalArgumentException caught, test okay.");
                log(e3.getMessage());
            }
            if (line != null) {
                line.close();
                line = null;
            }
        }

    }


    // helper routines
    static long startTime = currentTimeMillis();
    static long currentTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
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
