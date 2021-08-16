/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests jdeps --generate-open-module option
 * @library ../lib
 * @build CompilerUtils JdepsUtil JdepsRunner
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng GenOpenModule
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.stream.Stream;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GenOpenModule extends GenModuleInfo {
    private static final String MODULE_INFO = "module-info.class";

    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIBS_DIR = Paths.get("libs");
    private static final Path DEST_DIR = Paths.get("moduleinfosrc");
    private static final Path NEW_MODS_DIR = Paths.get("new_mods");

    @BeforeTest
    public void setup() throws Exception {
        compileAndCreateJars();
    }

    @Test
    public void test() throws IOException {
        Path dest = DEST_DIR.resolve("open");
        Path classes = NEW_MODS_DIR.resolve("open");
        Files.createDirectories(dest);
        Files.createDirectories(classes);

        Stream<String> files = MODULES.stream()
                .map(mn -> LIBS_DIR.resolve(mn + ".jar"))
                .map(Path::toString);

        Stream<String> options = Stream.concat(
            Stream.of("--generate-open-module", dest.toString()), files);
        JdepsRunner.run(options.toArray(String[]::new));

        // check file exists
        MODULES.stream()
             .map(mn -> dest.resolve(mn).resolve("module-info.java"))
             .forEach(f -> assertTrue(Files.exists(f)));

        // copy classes to a temporary directory
        // and then compile new module-info.java
        copyClasses(MODS_DIR, classes);
        compileNewGenModuleInfo(dest, classes);

        for (String mn : MODULES) {
            Path p1 = classes.resolve(mn).resolve(MODULE_INFO);
            Path p2 = MODS_DIR.resolve(mn).resolve(MODULE_INFO);
            try (InputStream in1 = Files.newInputStream(p1);
                 InputStream in2 = Files.newInputStream(p2)) {
                verify(ModuleDescriptor.read(in1),
                       ModuleDescriptor.read(in2));
            }
        }
    }

    /*
     * Verify the dependences
     */
    private void verify(ModuleDescriptor openModule, ModuleDescriptor md) {
        System.out.println("verifying: " + openModule.name());
        assertTrue(openModule.isOpen());
        assertTrue(!md.isOpen());
        assertEquals(openModule.name(), md.name());
        assertEquals(openModule.requires(), md.requires());
        assertTrue(openModule.exports().isEmpty());
        assertEquals(openModule.provides(), md.provides());
    }
}
