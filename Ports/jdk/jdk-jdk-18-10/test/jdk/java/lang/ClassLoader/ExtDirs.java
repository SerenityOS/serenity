/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8060206 8067366
 * @library /test/lib
 * @summary Extension mechanism is removed
 */

import jdk.test.lib.process.ProcessTools;

import java.lang.Integer;
import java.util.stream.Stream;

public class ExtDirs {
    private static String TEST_CLASSES = System.getProperty("test.classes", ".");

    private static String[] VALUES = new String[] {
            null,
            "",
            "\"\""
    };

    public static void main(String... args) throws Exception {
        String value = System.getProperty("java.ext.dirs");
        System.out.format("java.ext.dirs = '%s'%n", value);
        if (args.length > 0) {
            int index = Integer.valueOf(args[0]);
            String expectedValue = VALUES[index];
            if (!(expectedValue == value ||
                    (value != null && value.isEmpty()) ||
                    (expectedValue != null & expectedValue.equals(value)))) {
                throw new RuntimeException("java.ext.dirs (" +
                        value + ") != " + expectedValue);
            }
            // launched by subprocess.
            return;
        }

        if (value != null) {
            throw new RuntimeException("java.ext.dirs not removed: " + value);
        }

        fatalError(0, "-Djava.ext.dirs=foo");
        start(0);
        start(1, "-Djava.ext.dirs=");
        start(2, "-Djava.ext.dirs=\"\"");
    }

    static String[] launchOptions(int testParam, String... args) {
        return Stream.concat(Stream.of(args),
                             Stream.of("-cp", TEST_CLASSES, "ExtDirs",
                                       String.valueOf(testParam)))
                     .toArray(String[]::new);
    }

    static void start(int testParam, String... args) throws Exception {
        ProcessTools.executeTestJava(launchOptions(testParam, args))
                    .shouldHaveExitValue(0);
    }

    static void fatalError(int testParam, String... args) throws Exception {
        ProcessTools.executeTestJava(launchOptions(testParam, args))
                    .stderrShouldContain("Could not create the Java Virtual Machine");
    }
}
