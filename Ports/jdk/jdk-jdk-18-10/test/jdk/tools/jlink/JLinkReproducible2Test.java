/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.spi.ToolProvider;

/*
 * @test
 * @summary Make sure that jimages are consistent when created by jlink.
 * @bug 8241602
 * @modules jdk.jlink
 *          java.se
 *          jdk.management
 *          jdk.unsupported
 *          jdk.charsets
 * @run main JLinkReproducible2Test
 */
public class JLinkReproducible2Test {
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() ->
                    new RuntimeException("jlink tool not found")
            );

    public static void main(String[] args) throws Exception {
        Path image1 = Paths.get("./image1");
        Path image2 = Paths.get("./image2");

        JLINK_TOOL.run(System.out, System.err, "--add-modules", "java.se", "--output", image1.toString());
        JLINK_TOOL.run(System.out, System.err, "--add-modules", "java.se", "--output", image2.toString());

        if (Files.mismatch(image1.resolve("lib").resolve("modules"), image2.resolve("lib").resolve("modules")) != -1L) {
            throw new RuntimeException("jlink producing inconsistent result");
        }

        Path image3 = Paths.get("./image3");
        Path image4 = Paths.get("./image4");

        JLINK_TOOL.run(System.out, System.err, "--add-modules", "java.base,jdk.management,jdk.unsupported,jdk.charsets", "--output", image3.toString());
        JLINK_TOOL.run(System.out, System.err, "--add-modules", "java.base,jdk.management,jdk.unsupported,jdk.charsets", "--output", image4.toString());

        if (Files.mismatch(image3.resolve("lib").resolve("modules"), image4.resolve("lib").resolve("modules")) != -1L) {
            throw new RuntimeException("jlink producing inconsistent result with multiple named modules");
        }
    }
}
