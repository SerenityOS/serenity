/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that stack tracing isn't broken if an exception is thrown
 *          in a hidden class.
 * DESCRIPTION
 *     An exception is thrown by a hidden class.  Verify that the exception's
 *     stack trace contains the name of the current test class (i.e., verify
 *     that the stack trace is not broken).
 * @library /test/lib
 * @modules jdk.compiler
 * @run main HiddenClassStack
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class HiddenClassStack {

    static byte klassbuf[] = InMemoryJavaCompiler.compile("TestClass",
        "public class TestClass { " +
        "    public TestClass() { " +
        "        throw new RuntimeException(\"boom\"); " +
        " } } ");

    public static void main(String[] args) throws Throwable {

        // An exception is thrown by class loaded by lookup.defineHiddenClass().
        // Verify that the exception's stack trace contains name of the current
        // test class.
        try {
            Lookup lookup = MethodHandles.lookup();
            Class<?> cl = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
            Object obj = cl.newInstance();
            throw new Exception("Expected RuntimeException not thrown");
        } catch (RuntimeException e) {
            if (!e.getMessage().contains("boom")) {
                throw new RuntimeException("Wrong RuntimeException, e: " + e.toString());
            }
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(byteOS);
            e.printStackTrace(printStream);
            printStream.close();
            String stackTrace = byteOS.toString("ASCII");
            if (!stackTrace.contains(HiddenClassStack.class.getName())) {
                throw new RuntimeException("HiddenClassStack missing from stacktrace: " +
                                           stackTrace);
            }
        }
    }
}
