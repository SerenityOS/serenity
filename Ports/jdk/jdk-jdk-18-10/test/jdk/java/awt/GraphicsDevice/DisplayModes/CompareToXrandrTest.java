/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8022810 8196616
 * @summary Cannot list all the available display modes on Ubuntu linux in case
 *          of two screen devices
 * @run main CompareToXrandrTest
 */

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CompareToXrandrTest {

    public static void main(String[] args) throws Exception {
        if (!new File("/usr/bin/xrandr").exists()) {
            System.out.println("No xrandr tool to compare");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Runtime.getRuntime().exec("/usr/bin/xrandr").getInputStream()))) {
            Pattern pattern = Pattern.compile("^\\s*(\\d+x\\d+)");

            for (GraphicsDevice d : GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getScreenDevices()) {

                Set<String> xrandrModes = reader.lines().map(pattern::matcher)
                        .filter(Matcher::find).map(m -> m.group(1))
                        .collect(Collectors.toSet());

                Set<String> javaModes = Arrays.stream(d.getDisplayModes())
                        .map(m -> m.getWidth() + "x" + m.getHeight())
                        .collect(Collectors.toSet());

                if (!xrandrModes.equals(javaModes)) {
                    throw new RuntimeException("Failed");
                } else {
                    System.out.println("Device " + d + ": " + javaModes.size() +
                            " modes found.");
                }
            }
        }
    }
}
