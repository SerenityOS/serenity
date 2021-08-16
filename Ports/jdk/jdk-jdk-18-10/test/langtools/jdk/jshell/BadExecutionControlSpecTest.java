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
 * @bug 8168615
 * @summary Test bad input to ExecutionControl.generate
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @run testng BadExecutionControlSpecTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionEnv;
import static org.testng.Assert.fail;

@Test
public class BadExecutionControlSpecTest {
    private static void assertIllegal(String spec) throws Throwable {
        try {
            ExecutionEnv env = new ExecutionEnv() {
                @Override
                public InputStream userIn() {
                    return new ByteArrayInputStream(new byte[0]);
                }

                @Override
                public PrintStream userOut() {
                    return new PrintStream(new ByteArrayOutputStream());
                }

                @Override
                public PrintStream userErr() {
                    return new PrintStream(new ByteArrayOutputStream());
                }

                @Override
                public List<String> extraRemoteVMOptions() {
                    return Collections.emptyList();
                }

                @Override
                public void closeDown() {
                }

            };
            ExecutionControl.generate(env, spec);
            fail("Expected exception -- " + spec);
        } catch (IllegalArgumentException ex) {
            // The expected happened
        }
    }

    public void syntaxTest() throws Throwable {
        assertIllegal(":launch(true)");
        assertIllegal("jdi:launch(true");
        assertIllegal("jdi:launch(true)$");
        assertIllegal("jdi:,");
    }

    public void notFoundTest() throws Throwable {
        assertIllegal("fruitbats");
        assertIllegal("jdi:baz(true)");
        assertIllegal("random:launch(true)");
        assertIllegal("jdi:,");
    }
}
