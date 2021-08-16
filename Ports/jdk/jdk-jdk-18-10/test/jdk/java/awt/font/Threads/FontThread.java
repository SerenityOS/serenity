/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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


/* @test
 * @summary verify thread interruption doesn't affect font file reading.
 * @bug 6640532
 */

import java.awt.*;

public class FontThread extends Thread {

    String fontName = "Dialog";
    static FontThread thread1;
    static FontThread thread2;
    static FontThread thread3;

    public static void main(String args[]) throws Exception {
        thread1 = new FontThread("SansSerif");
        thread2 = new FontThread("Serif");
        thread3 = new FontThread("Monospaced");
        thread1.dometrics(60); // load classes first
        thread1.start();
        thread2.start();
        thread3.start();
        InterruptThread ithread = new InterruptThread();
        ithread.setDaemon(true);
        ithread.start();
        thread1.join();
        thread2.join();
        thread3.join();
    }

    FontThread(String font) {
        super();
        this.fontName = font;
    }

    public void run() {
        System.out.println("started "+fontName); System.out.flush();
        dometrics(4000);
        System.out.println("done "+fontName); System.out.flush();
    }

    private void dometrics(int max) {
        Font f = new Font(fontName, Font.PLAIN, 12);
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
        for (char i=0;i<max;i++) {
            if (f.canDisplay(i)) fm.charWidth(i);
        }
    }

    static class InterruptThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                thread1.interrupt();
                thread2.interrupt();
                thread3.interrupt();
            }
        }
    }
}
