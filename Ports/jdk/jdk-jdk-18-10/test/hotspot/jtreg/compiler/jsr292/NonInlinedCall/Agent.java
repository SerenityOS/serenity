/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jsr292.NonInlinedCall;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

public class Agent {
    public static void main(String[] args) throws Exception {
        String jarName = args[0];
        String className = args[1];
        String manifestName = "manifest.mf";

        System.out.println("Creating " + manifestName);
        try (PrintStream out = new PrintStream(new File(manifestName))) {
            out.println("Premain-Class: " + className);
            out.println("Can-Redefine-Classes: true");
        }

        System.out.println("Building " + jarName);
        JDKToolLauncher jar = JDKToolLauncher
                .create("jar")
                .addToolArg("-cfm")
                .addToolArg(jarName)
                .addToolArg(manifestName);

        System.out.println("Running jar " + Arrays.toString(jar.getCommand()));

        ProcessBuilder pb = new ProcessBuilder(jar.getCommand());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}
