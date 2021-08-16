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
 * @bug 8185108
 * @summary Test exception().getMessage() in events returned by eval()
 * @run testng ExceptionMessageTest
 */

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.execution.JdiExecutionControlProvider;
import jdk.jshell.execution.LocalExecutionControlProvider;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class ExceptionMessageTest {

    public void testDefaultEC() {
        doTestCases(new JdiExecutionControlProvider(), "default");
    }

    public void testLocalEC() {
        doTestCases(new LocalExecutionControlProvider(), "local");
    }

    public void testDirectEC() {
        doTestCases(new ExecutionControlProvider() {
            public ExecutionControl generate(ExecutionEnv env, Map<String, String> param) throws Throwable {
                return new DirectExecutionControl();
            }

            public String name() {
                return "direct";
            }

        }, "direct");
    }

    private JShell shell(ExecutionControlProvider ec) {
        return JShell.builder().executionEngine(ec, new HashMap<>()).build();
    }

    private void doTestCases(ExecutionControlProvider ec, String label) {
        JShell jshell = shell(ec);
        doTest(jshell, label, "throw new java.io.IOException();", null);
        doTest(jshell, label, "throw new java.io.IOException((String)null);", null);
        doTest(jshell, label, "throw new java.io.IOException(\"\");", "");
        doTest(jshell, label, "throw new java.io.IOException(\"test\");", "test");
    }

    private void doTest(JShell jshell, String label, String code, String expected) {
        List<SnippetEvent> result = jshell.eval(code);
        assertEquals(result.size(), 1, "Expected only one event");
        SnippetEvent evt = result.get(0);
        Exception exc = evt.exception();
        String out = exc.getMessage();
        assertEquals(out, expected, "Exception message not as expected: " +
                label + " -- " + code);
    }
}
