/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8267430
 * @key headful
 * @summary verify setting a display mode with unknow refresh rate works
 */

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class UnknownRefrshRateTest {

    public static void main(String[] args) throws Exception {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        for (GraphicsDevice d : devices) {

            if (!d.isDisplayChangeSupported()) {
                continue;
            }
            DisplayMode odm = d.getDisplayMode();
            System.out.println("device=" + d + " original mode=" + odm);

            DisplayMode[] modes = d.getDisplayModes();
            System.out.println("There are " + modes.length + " modes.");
            try {
                for (int i=0; i<modes.length; i++) {
                    DisplayMode mode = modes[i];
                    System.out.println("copying from mode " + i + " : " + mode);
                    int w = mode.getWidth();
                    int h = mode.getHeight();
                    int bpp = mode.getBitDepth();
                    int refRate = DisplayMode.REFRESH_RATE_UNKNOWN;
                    DisplayMode newMode = new DisplayMode(w, h, bpp, refRate);
                    d.setDisplayMode(newMode);
                    Thread.sleep(2000);
                    System.out.println("set " + d.getDisplayMode());
                 }
             } finally {
                 System.out.println("restoring original mode"+odm);
                 d.setDisplayMode(odm);
                 Thread.sleep(10000);
             }
       }
    }
}
