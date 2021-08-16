/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049238
 * @summary Checks Signature attribute for methods which throw exceptions.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build ExceptionTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver ExceptionTest
 */

import java.io.IOError;
import java.io.IOException;

@ExpectedSignature(descriptor = "ExceptionTest",
        signature = "<Exc:Ljava/lang/RuntimeException;:Ljava/lang/Runnable;>Ljava/lang/Object;")
public class ExceptionTest<Exc extends RuntimeException & Runnable> {

    @ExpectedSignature(descriptor = "<init>()", signature = "<E:Ljava/lang/Exception;>()V^TE;")
    <E extends Exception> ExceptionTest() throws E {
    }

    @ExpectedSignature(descriptor = "<init>(int)",
            signature = "<E:Ljava/lang/Exception;>(I)V^Ljava/io/IOException;^TE;^Ljava/io/IOError;")
    <E extends Exception> ExceptionTest(int a) throws IOException, E, IOError {
    }

    @ExpectedSignature(descriptor = "<init>(long)", signature = "(J)V^TExc;")
    ExceptionTest(long a) throws Exc {
    }

    @ExpectedSignature(descriptor = "<init>(byte)", signature = "(B)V^Ljava/io/IOError;^TExc;^Ljava/io/IOException;")
    ExceptionTest(byte a) throws IOError, Exc, IOException {
    }

    @ExpectedSignature(descriptor = "<init>(java.lang.RuntimeException)", signature = "(TExc;)V")
    ExceptionTest(Exc a) throws IOException {
    }

    // no Signature attribute
    ExceptionTest(String a) throws IOError {
    }

    void noSignatureAttributeMethod() throws IOException {
    }

    @ExpectedSignature(descriptor = "genericMethod(int)", signature = "(I)V^TExc;")
    void genericMethod(int a) throws Exc {
    }

    @ExpectedSignature(descriptor = "genericMethod(long)", signature = "(J)V^TExc;^Ljava/io/IOException;")
    void genericMethod(long a) throws Exc, IOException {
    }

    @ExpectedSignature(descriptor = "genericMethod(java.lang.RuntimeException)", signature = "(TExc;)V")
    void genericMethod(Exc a) throws IOError {
    }

    static void staticNoSignatureAttributeMethod() throws IOException {
    }

    @ExpectedSignature(descriptor = "staticGenericMethod(int)",
            signature = "<E:Ljava/lang/Exception;:Ljava/lang/Runnable;>(I)V^TE;")
    static <E extends Exception & Runnable> void staticGenericMethod(int a) throws E {
    }

    @ExpectedSignature(descriptor = "staticGenericMethod(long)",
            signature = "<E:Ljava/lang/Exception;>(J)V^Ljava/io/IOError;^TE;^Ljava/io/IOException;")
    static <E extends Exception> void staticGenericMethod(long a) throws IOError, E, IOException {
    }

    @ExpectedSignature(descriptor = "staticGenericMethod(java.lang.Exception)",
            signature = "<E:Ljava/lang/Exception;>(TE;)V")
    static <E extends Exception> void staticGenericMethod(E a) throws IOError {
    }
}
