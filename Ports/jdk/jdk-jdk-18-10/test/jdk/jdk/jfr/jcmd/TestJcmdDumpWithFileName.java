/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @bug 8220657
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestJcmdDumpWithFileName
 */
public class TestJcmdDumpWithFileName {

    public static void main(String[] args) throws Exception {
        testDumpAll();
        testDumpNamed();
        testDumpNamedWithFilename();
    }

    private static void testDumpAll() throws Exception {
        Path p = Path.of("testDumpAll.jfr").toAbsolutePath();
        try (Recording r = new Recording()) {
            r.setName("testDumpAll");
            r.setDestination(p);
            r.start();

            JcmdHelper.jcmd("JFR.dump");

            Asserts.assertFalse(namedFile(p), "Unexpected file: " + p.toString());
            Asserts.assertTrue(generatedFile(), "Expected generated file");
        }
        cleanup();
    }

    private static void testDumpNamed() throws Exception {
        Path p = Path.of("testDumpNamed.jfr").toAbsolutePath();
        try (Recording r = new Recording()) {
            r.setName("testDumpNamed");
            r.setDestination(p);
            r.start();

            JcmdHelper.jcmd("JFR.dump", "name=testDumpNamed");

            Asserts.assertTrue(namedFile(p), "Expected file: " + p.toString());
            Asserts.assertFalse(generatedFile(), "Unexpected generated file");
        }
        cleanup();
    }

    private static void testDumpNamedWithFilename() throws Exception {
        Path p = Path.of("testDumpNamedWithFilename.jfr").toAbsolutePath();
        Path override = Path.of("override.jfr").toAbsolutePath();
        try (Recording r = new Recording()) {
            r.setName("testDumpNamedWithFilename");
            r.setDestination(p);
            r.start();

            JcmdHelper.jcmd("JFR.dump", "name=testDumpNamedWithFilename", "filename=" + override.toString());

            Asserts.assertFalse(namedFile(p), "Unexpected file: " + p.toString());
            Asserts.assertTrue(namedFile(override), "Expected file: " + override.toString());
            Asserts.assertFalse(generatedFile(), "Unexpected generated file");
        }
        cleanup();
    }

    private static boolean namedFile(Path dumpFile) throws IOException {
        return Files.exists(dumpFile) && (Files.size(dumpFile) > 0);
    }

    private static boolean generatedFile() throws IOException {
        long pid = ProcessHandle.current().pid();
        Stream<Path> stream = Files.find(Path.of("."), 1, (p, a) -> p.toString()
                                                                     .matches("^.*hotspot-pid-" + pid + "-[0-9_]+\\.jfr$") && (a.size() > 0L));
        try (stream) {
            return stream.findAny()
                         .isPresent();
        }
    }

    private static void cleanup() throws IOException {
        Stream<Path> stream = Files.find(Path.of("."), 1, (p, a) -> p.toString().endsWith(".jfr"));
        try (stream) {
            stream.forEach(p -> p.toFile().delete());
        }
    }

}
