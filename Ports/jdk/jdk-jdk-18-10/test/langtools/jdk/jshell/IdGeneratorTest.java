/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test custom id generators
 * @build KullaTesting TestingInputStream
 * @run testng IdGeneratorTest
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.UnresolvedReferenceException;
import jdk.jshell.VarSnippet;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class IdGeneratorTest {

    public JShell.Builder getBuilder() {
        TestingInputStream inStream = new TestingInputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        return JShell.builder()
                .in(inStream)
                .out(new PrintStream(outStream))
                .err(new PrintStream(errStream));
    }

    public void testTempNameGenerator() {
        JShell.Builder builder = getBuilder().tempVariableNameGenerator(new Supplier<String>() {
            int count = 0;

            @Override
            public String get() {
                return "temp" + ++count;
            }
        });
        try (JShell jShell = builder.build()) {
            for (int i = 0; i < 3; ++i) {
                VarSnippet v = (VarSnippet) jShell.eval("2 + " + (i + 1)).get(0).snippet();
                assertEquals("temp" + (i + 1), v.name(), "Custom id: ");
            }
        }
    }

    public void testResetTempNameGenerator() {
        JShell.Builder builder = getBuilder().tempVariableNameGenerator(() -> {
            throw new AssertionError("Should not be called");
        });
        try (JShell jShell = builder.tempVariableNameGenerator(null).build()) {
            jShell.eval("2 + 2");
        }
    }

    public void testIdGenerator() {
        JShell.Builder builder = getBuilder().idGenerator(((snippet, id) -> "custom" + id));
        try (JShell jShell = builder.build()) {
            List<SnippetEvent> eval = jShell.eval("int a, b;");
            checkIds(eval);
            checkIds(jShell.drop(eval.get(0).snippet()));
        }
    }

    private void checkIds(List<SnippetEvent> events) {
        for (SnippetEvent event : events) {
            assertTrue(event.snippet().id().startsWith("custom"), "Not started with \"custom\": "
                    + event.snippet().id());
        }
    }

    public void testIdInException() {
        JShell.Builder builder = getBuilder().idGenerator(((snippet, id) -> "custom" + id));
        try (JShell jShell = builder.build()) {
            EvalException evalException = (EvalException) jShell.eval("throw new Error();").get(0).exception();
            for (StackTraceElement ste : evalException.getStackTrace()) {
                assertTrue(ste.getFileName().startsWith("#custom"), "Not started with \"#custom\": "
                        + ste.getFileName());
            }
            jShell.eval("void f() { g(); }");
            UnresolvedReferenceException unresolvedException = (UnresolvedReferenceException) jShell.eval("f();").get(0).exception();
            for (StackTraceElement ste : unresolvedException.getStackTrace()) {
                assertTrue(ste.getFileName().startsWith("#custom"), "Not started with \"#custom\": "
                        + ste.getFileName());
            }
        }
    }

    public void testResetIdGenerator() {
        JShell.Builder builder = getBuilder().idGenerator((sn, id) -> {
            throw new AssertionError("Should not be called");
        });
        try (JShell jShell = builder.idGenerator(null).build()) {
            jShell.eval("2 + 2");
        }
    }
}
