/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8022810
 * @summary Device.getDisplayMode() doesn't report refresh rate on Linux in case
 *          of dual screen
 * @run main CurrentDisplayModeTest
 */

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CurrentDisplayModeTest {
    public static void main(String[] args) {
        GraphicsDevice[] screenDevices = GraphicsEnvironment.
                               getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice screenDevice : screenDevices) {
            DisplayMode currentMode = screenDevice.getDisplayMode();
            System.out.println("current mode " + currentMode);
            Set<DisplayMode> set = new HashSet<>(
                                 Arrays.asList(screenDevice.getDisplayModes()));
            if (!set.contains(currentMode)) {
                throw new RuntimeException("Mode " + currentMode +
                        " is not found in the modes list " + set);
            }
        }
    }
}
