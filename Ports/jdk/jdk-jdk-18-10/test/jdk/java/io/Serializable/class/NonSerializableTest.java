/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4075221
 * @library /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run testng/timeout=300 NonSerializableTest
 * @summary Enable serialize of nonSerializable Class descriptor.
 */

import java.nio.file.Paths;
import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;


public class NonSerializableTest {

    @BeforeClass
    public void setup() throws Exception {
        boolean b = CompilerUtils.compile(
                Paths.get(System.getProperty("test.src"), "TestEntry.java"),
                Paths.get(System.getProperty("user.dir")));
        assertTrue(b, "Compilation failed");
    }

    @DataProvider
    public Object[][] provider() {
        return new String[][][] {
            // Write NonSerial1, Read NonSerial1
            {{"NonSerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"NonSerialA_1", "-cp", ".", "TestEntry", "-d"}},

            // Write NonSerial1, Read NonSerial2
            {{"NonSerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"NonSerialA_2", "-cp", ".", "TestEntry", "-d"}},

            // Write NonSerial1, Read Serial1
            {{"NonSerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"SerialA_1", "-cp", ".", "TestEntry", "-d"}},

            // Write Serial1, Read NonSerial1
            {{"SerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"NonSerialA_1", "-cp", ".", "TestEntry", "-doe"}},

            // Write Serial1, Read Serial2
            {{"SerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"SerialA_2", "-cp", ".", "TestEntry", "-d"}},

            // Write Serial2, Read Serial1
            {{"SerialA_2", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"SerialA_1", "-cp", ".", "TestEntry", "-d"}},

            // Write Serial1, Read Serial3
            {{"SerialA_1", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"SerialA_3", "-cp", ".", "TestEntry", "-de"}},

            // Write Serial3, Read Serial1
            {{"SerialA_3", "-cp", ".", "TestEntry", "-s", "A"}},
            {{"SerialA_1", "-cp", ".", "TestEntry", "-de"}},
        };
    }

    @Test(dataProvider="provider")
    public void test(String[] args) throws Exception {
        boolean b = CompilerUtils.compile(Paths.get(System.getProperty("test.src"), args[0]),
                                          Paths.get(System.getProperty("user.dir")));
        assertTrue(b, "Compilation failed");
        String params[] = Arrays.copyOfRange(args, 1, args.length);
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(params);
        Process p = ProcessTools.startProcess("Serializable Test", pb);
        int exitValue = p.waitFor();
        assertEquals(exitValue, 0, "Test failed");
    }
}
