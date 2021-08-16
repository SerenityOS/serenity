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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8243925
 * @key headful
 * @requires (os.family == "windows")
 * @summary Verifies Toolkit.getScreenInsets using different DPI
 * @library /test/lib
 * @run main/othervm -Dsun.java2d.uiScale=1 ScreenInsetsDPIVariation
 */
public final class ScreenInsetsDPIVariation {

    public static void main(String[] args) throws Exception {
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screenDevices = ge.getScreenDevices();
        if (args.length == 0) {
            for (int i = 0; i < screenDevices.length; i++) {
                var gd = screenDevices[i];
                var gc = gd.getDefaultConfiguration();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                checkAllDPI(i, insets);
            }
        } else {
            int screen = Integer.parseInt(args[0]);
            int left = Integer.parseInt(args[1]);
            int right = Integer.parseInt(args[2]);
            int top = Integer.parseInt(args[3]);
            int bottom = Integer.parseInt(args[4]);
            double scale = Double.parseDouble(args[5]);

            System.err.println("screen = " + screen);
            System.err.println("scale = " + scale);
            if (screen >= screenDevices.length) {
                return; // devices were changed, skip
            }
            var gc = screenDevices[screen].getDefaultConfiguration();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            check(insets.left, left / scale);
            check(insets.right, right / scale);
            check(insets.top, top / scale);
            check(insets.bottom, bottom / scale);
        }
    }

    private static void check(int actual, double expected) {
        if (actual != clipRound(expected)) {
            System.err.println("Expected: " + expected);
            System.err.println("Actual: " + actual);
            throw new RuntimeException("Wrong size");
        }
    }

    private static void checkAllDPI(int screen, Insets insets)
            throws Exception {
        for (String dpi : List.of("1", "1.5", "1.75", "2", "2.5", "3", "3.1")) {
            runProcess(dpi, screen, insets);
        }
    }

    public static int clipRound(double coordinate) {
        double newv = coordinate - 0.5;
        if (newv < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (newv > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(newv);
    }

    private static void runProcess(String dpi, int screen, Insets insets)
            throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dsun.java2d.uiScale=" + dpi,
                ScreenInsetsDPIVariation.class.getSimpleName(),
                String.valueOf(screen), String.valueOf(insets.left),
                String.valueOf(insets.right), String.valueOf(insets.top),
                String.valueOf(insets.bottom), dpi);
        Process worker = ProcessTools.startProcess("Worker", pb, null, 20,
                TimeUnit.SECONDS);
        new OutputAnalyzer(worker).shouldHaveExitValue(0);
    }
}
