/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.image.ColorModel;

/**
 * @test
 * @bug 8168307
 * @run main/othervm IsToolkitUseTheMainScreen
 * @run main/othervm -Djava.awt.headless=true IsToolkitUseTheMainScreen
 */
public final class IsToolkitUseTheMainScreen {

    public static void main(final String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            testHeadless();
        } else {
            testHeadful();
        }
    }

    private static void testHeadless() {
        try {
            Toolkit.getDefaultToolkit().getScreenSize();
            throw new RuntimeException("HeadlessException is not thrown");
        } catch (final HeadlessException ignored) {
            // expected exception
        }
        try {
            Toolkit.getDefaultToolkit().getColorModel();
            throw new RuntimeException("HeadlessException is not thrown");
        } catch (final HeadlessException ignored) {
            // expected exception
        }
    }

    private static void testHeadful() {
        GraphicsEnvironment ge
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc
                = ge.getDefaultScreenDevice().getDefaultConfiguration();
        Dimension gcSize = gc.getBounds().getSize();
        ColorModel gcCM = gc.getColorModel();

        Dimension toolkitSize = Toolkit.getDefaultToolkit().getScreenSize();
        ColorModel toolkitCM = Toolkit.getDefaultToolkit().getColorModel();

        if (!gcSize.equals(toolkitSize)) {
            System.err.println("Toolkit size = " + toolkitSize);
            System.err.println("GraphicsConfiguration size = " + gcSize);
            throw new RuntimeException("Incorrect size");
        }
        if (!gcCM.equals(toolkitCM)) {
            System.err.println("Toolkit color model = " + toolkitCM);
            System.err.println("GraphicsConfiguration color model = " + gcCM);
            throw new RuntimeException("Incorrect color model");
        }
    }
}
