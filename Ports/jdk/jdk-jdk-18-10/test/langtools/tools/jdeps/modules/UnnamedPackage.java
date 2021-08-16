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

/*
 * @test
 * @bug 8166846
 * @summary Tests --generate-module-info on invalid JAR file
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @library ../lib
 * @build JdepsRunner JdepsUtil
 * @run main UnnamedPackage
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class UnnamedPackage {
    private static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes"));
    private static final Path FOO_JAR_FILE = Paths.get("foo.jar");

    public static void main(String... args) throws Exception {
        // create foo.jar with unnamed package
        Path name = TEST_CLASSES.resolve("UnnamedPackage.class");
        JdepsUtil.createJar(FOO_JAR_FILE, TEST_CLASSES, Stream.of(name));

        // run jdeps --generate-module-info
        JdepsRunner jdeps = new JdepsRunner("--generate-module-info",
                                            "tmp", FOO_JAR_FILE.toString());
        // should fail to generate module-info.java
        int exitValue = jdeps.run();
        if (exitValue == 0) {
            throw new RuntimeException("expected non-zero exitValue");
        }
        if (!jdeps.outputContains("foo.jar contains an unnamed package")) {
            jdeps.printStdout(System.out);
            throw new RuntimeException("expected error message not found");
        }
    }
}
