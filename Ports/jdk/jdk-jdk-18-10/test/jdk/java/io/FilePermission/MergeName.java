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

import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @test
 * @bug 8170364
 * @summary FilePermission path modified during merge
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main MergeName
 */

public class MergeName {

    public static final String[] ALL_ACTIONS
            = {"read", "write", "execute", "delete"};

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            test("p1", "read", "write", "delete", "execute");
            test("p2", "read,write", "delete,execute");
            test("p3", "read,write,delete", "execute");
            test("p4", "read,write,delete,execute");
        } else {
            SecurityManager sm = System.getSecurityManager();
            for (String arg : args) {
                // Use bits to create powerset of ALL_ACTIONS
                IntStream.range(1, 16)
                        .mapToObj(n -> IntStream.range(0, 4)
                                .filter(x -> (n & (1 << x)) != 0)
                                .mapToObj(x -> ALL_ACTIONS[x])
                                .collect(Collectors.joining(",")))
                        .forEach(a -> sm.checkPermission(
                                new FilePermission(arg, a)));
            }
        }
    }

    private static void test(String file, String... actions) throws Exception {
        List<String> content = new ArrayList<>();
        content.add("grant {");
        for (String action : actions) {
            content.add("   permission java.io.FilePermission " +
                    "\"x\", \"" +action + "\";");
        }
        content.add("};");
        Files.write(Paths.get(file), content);
        ProcessTools.executeTestJvm("-Djava.security.manager",
                "-Djava.security.policy=" + file,
                "MergeName",
                "x",
                new File(System.getProperty("user.dir"), "x").getPath())
            .shouldHaveExitValue(0);
    }
}
