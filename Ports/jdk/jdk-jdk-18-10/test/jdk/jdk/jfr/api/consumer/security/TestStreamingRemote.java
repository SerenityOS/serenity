/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.security;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary Test that a stream can be opened against a remote repository using
 *          only file permission
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.api.consumer.security.TestStreamingRemote
 */
public class TestStreamingRemote {

    private static final String SUCCESS = "Success!";

    public static class TestEvent extends Event {
    }

    public static class Test {
        public static void main(String... args) throws Exception {
            Path repo = Paths.get(args[0]);
            System.out.println("Repository: " + repo);
            try (EventStream es = EventStream.openRepository(repo)) {
                es.setStartTime(Instant.EPOCH);
                es.onEvent(e -> {
                    System.out.println(SUCCESS);
                    es.close();
                });
                es.start();
            }
        }
    }

    public static void main(String... args) throws Exception {
        try (Recording r = new Recording()) {
            r.start();
            String repository = System.getProperty("jdk.jfr.repository");
            Path policy = createPolicyFile(repository);
            TestEvent e = new TestEvent();
            e.commit();
            String[] c = new String[4];
            c[0] = "-Djava.security.manager";
            c[1] = "-Djava.security.policy=" + escapeBackslashes(policy.toString());
            c[2] = Test.class.getName();
            c[3] = repository;
            OutputAnalyzer oa = ProcessTools.executeTestJvm(c);
            oa.shouldContain(SUCCESS);
        }
    }

    private static Path createPolicyFile(String repository) throws IOException {
        Path policy = Paths.get("permission.policy").toAbsolutePath();
        try (PrintWriter pw = new PrintWriter(policy.toFile())) {
            pw.println("grant {");
            // All the files and directories the contained in path
            String dir = escapeBackslashes(repository);
            String contents = escapeBackslashes(repository + File.separatorChar + "-");
            pw.println("  permission java.io.FilePermission \"" + dir + "\", \"read\";");
            pw.println("  permission java.io.FilePermission \"" + contents + "\", \"read\";");
            pw.println("};");
            pw.println();
        }
        System.out.println("Permission file: " + policy);
        for (String line : Files.readAllLines(policy)) {
            System.out.println(line);
        }
        System.out.println();
        return policy;
    }

    // Needed for Windows
    private static String escapeBackslashes(String text) {
        return text.replace("\\", "\\\\");
    }
}
