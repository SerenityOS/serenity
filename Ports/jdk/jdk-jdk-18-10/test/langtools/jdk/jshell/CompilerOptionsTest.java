/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159635
 * @summary Test setting compiler options
 * @build KullaTesting TestingInputStream
 * @run testng CompilerOptionsTest
 */

import javax.tools.Diagnostic;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static jdk.jshell.Snippet.Status.VALID;

@Test
public class CompilerOptionsTest extends KullaTesting {

    @BeforeMethod
    @Override
    public void setUp() {
        setUp(b -> b.compilerOptions("-source", "7", "-Xlint:cast,-options"));
    }

    public void testLint() {
        assertDeclareWarn1("String s = (String)\"hello\";",
                new ExpectedDiagnostic("compiler.warn.redundant.cast", 11, 26, 11, -1, -1, Diagnostic.Kind.WARNING));
    }

    public void testSourceVersion() {
        assertEval("import java.util.function.*;", added(VALID));
        assertDeclareFail("Function<Integer,Integer> f = x -> x*2;",
                new ExpectedDiagnostic("compiler.err.feature.not.supported.in.source.plural", 32, 32, 32, -1, -1, Diagnostic.Kind.ERROR));
    }
}
