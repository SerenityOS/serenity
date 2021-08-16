/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Platform;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

/**
 * @test
 * @summary Tests that the SVML shared library is present in an image only when jdk.incubator.vector is present
 * @requires vm.compiler2.enabled
 * @requires os.arch == "x86_64" | os.arch == "amd64"
 * @requires os.family == "linux" | os.family == "windows"
 * @modules jdk.incubator.vector jdk.jlink
 * @library /test/lib
 * @run testng ImageTest
 */

public class ImageTest {
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() ->
                    new RuntimeException("jlink tool not found")
            );

    static final String SVML_LIBRARY_NAME = Platform.isWindows()
            ? "svml.dll"
            : "libsvml.so";

    static void link(String module, Path output) {
        int e = JLINK_TOOL.run(System.out, System.err,
                "--add-modules", module,
                "--output", output.toString()
        );
        if (e != 0) {
            throw new RuntimeException("Error running jlink");
        }
    }

    static void checkSVML(Path image, boolean shouldBepresent) {
        Path libsvml = Platform.libDir(image).resolve(SVML_LIBRARY_NAME);

        boolean exists = Files.exists(libsvml);
        if (shouldBepresent) {
            Assert.assertTrue(exists, libsvml + " should be present");
        } else {
            Assert.assertFalse(exists, libsvml + "should be absent");
        }
    }


    @Test
    public void withVectorModule() {
        Path output = Path.of("withVectorModuleImage");
        link("jdk.incubator.vector", output);
        checkSVML(output, true);
    }

    @Test
    public void withoutVectorModule() {
        Path output = Path.of("withoutVectorModuleImage");
        link("java.base", output);
        checkSVML(output, false);
    }
}
