/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8221823
 * @key headful
 * @summary Verifies TOP level component's minimumSize using different DPI
 * @library /test/lib
 * @run main/othervm -Dsun.java2d.uiScale=1 MinimumSizeDPIVariation
 */
public final class MinimumSizeDPIVariation {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Dimension minimumSize = test(new Frame());
            checkAllDPI("frame", minimumSize.width, minimumSize.height);
            minimumSize = test(new Window(null));
            checkAllDPI("window", minimumSize.width, minimumSize.height);
            minimumSize = test(new Dialog((Frame) null));
            checkAllDPI("dialog", minimumSize.width, minimumSize.height);
        } else {
            String comp = args[0];
            int w = Integer.parseInt(args[1]);
            int h = Integer.parseInt(args[2]);
            double scale = Double.parseDouble(args[3]);

            System.err.println("comp = " + comp);
            System.err.println("scale = " + scale);

            Dimension minimumSize = switch (comp) {
                case "frame" -> test(new Frame());
                case "window" -> test(new Window(null));
                case "dialog" -> test(new Dialog((Frame) null));
                default -> throw new java.lang.IllegalStateException(
                        "Unexpected value: " + comp);
            };
            check(minimumSize.width, Math.max(w / scale, 1));
            check(minimumSize.height, Math.max(h / scale, 1));
        }
    }

    private static Dimension test(Window window) {
        try {
            window.setLayout(null); // trigger use the minimum size of the peer
            window.setSize(new Dimension(1, 1));
            window.pack();
            Dimension minimumSize = window.getMinimumSize();
            Dimension size = window.getSize();
            if (!minimumSize.equals(size)) {
                System.err.println(window);
                System.err.println("Expected: " + minimumSize);
                System.err.println("Actual: " + size);
                throw new RuntimeException("Wrong size");
            }
            return minimumSize;
        } finally {
            window.dispose();
        }
    }

    private static void check(int actual, double expected) {
        double i = 100 * (actual - expected) / expected;
        if (Math.abs(i) > 10) { // no more than 10% variation
            System.err.println("Expected: " + expected);
            System.err.println("Actual: " + actual);
            throw new RuntimeException("Difference is too big: " + i);
        }
    }

    private static void checkAllDPI(String comp, int w, int h)
            throws Exception {
        if (!Platform.isOSX()) {
            for (String dpi : List.of("1.5", "1.75", "2", "2.5")) {
                runProcess(dpi, comp, w, h);
            }
        }
    }

    private static void runProcess(String dpi, String comp, int w, int h)
            throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dsun.java2d.uiScale=" + dpi,
                MinimumSizeDPIVariation.class.getSimpleName(), comp,
                String.valueOf(w), String.valueOf(h), dpi);
        Process worker = ProcessTools.startProcess("Worker", pb, null, 20,
                TimeUnit.SECONDS);
        new OutputAnalyzer(worker).shouldHaveExitValue(0);
    }
}
