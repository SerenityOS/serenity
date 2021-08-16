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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.tools.ToolProvider;

/**
 * @test
 * @bug 8154190
 * @summary Test javax.tools.ToolProvider running with security manager
 * @modules java.compiler
 *          jdk.compiler
 * @run main/othervm -Djava.security.manager=allow ToolProviderTest
 */

// run in other vm to ensure the initialization code path is exercised.
public class ToolProviderTest {
    public static void main(String... args) {
        // The following code allows the test to be skipped when run on
        // an exploded image.
        // See https://bugs.openjdk.java.net/browse/JDK-8155858
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path image = javaHome.resolve("lib").resolve("modules");
        Path modules = javaHome.resolve("modules");
        if (!Files.exists(image) && Files.exists(modules)) {
            System.err.println("Test running on exploded image");
            System.err.println("Test skipped!");
            return;
        }

        System.setSecurityManager(new SecurityManager());

        Objects.requireNonNull(ToolProvider.getSystemDocumentationTool());
        Objects.requireNonNull(ToolProvider.getSystemJavaCompiler());
        if (ToolProvider.getSystemToolClassLoader() != null) {
            throw new AssertionError("unexpected value for getSystemToolClassLoader");
        }
    }
}
