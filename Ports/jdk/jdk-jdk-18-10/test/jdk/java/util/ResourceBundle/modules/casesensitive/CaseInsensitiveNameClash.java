/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177980
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.ProcessTools CaseInsensitiveNameClash
 * @run testng CaseInsensitiveNameClash
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CaseInsensitiveNameClash {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String MODULE = "resbundle";
    private static final String MAIN_CLASS = MODULE + "/jdk.test.Main";

    /**
     * Compiles the module used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        Path msrc = SRC_DIR.resolve(MODULE);
        assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                                         "--module-source-path", SRC_DIR.toString()));
        Path propsFile = Paths.get("jdk", "test", "main.properties");
        Files.copy(msrc.resolve(propsFile), MODS_DIR.resolve(MODULE).resolve(propsFile));
    }

    @Test
    public void test() throws Exception {
        assertTrue(ProcessTools.executeTestJava("--module-path", MODS_DIR.toString(),
                                                "-m", MAIN_CLASS)
                               .outputTo(System.out)
                               .errorTo(System.out)
                               .getExitValue() == 0);
    }
}
