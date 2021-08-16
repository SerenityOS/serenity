/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246353
 * @summary Test sealed class in jshell
 * @modules jdk.jshell
 * @build KullaTesting TestingInputStream ExpectedDiagnostic
 * @run testng SealedClassesTest
 */

import javax.lang.model.SourceVersion;

import jdk.jshell.TypeDeclSnippet;
import jdk.jshell.Snippet.Status;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jdk.jshell.Snippet.Status.VALID;

@Test
public class SealedClassesTest extends KullaTesting {

    public void testSealed() {
        TypeDeclSnippet base = classKey(
                assertEval("sealed class B permits I {}",
                           ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_NOT_DEFINED, false, null)));
        assertEval("final class I extends B {}",
                   added(VALID),
                   ste(base, Status.RECOVERABLE_NOT_DEFINED, Status.VALID, true, null));
        assertEval("new I()");
    }

    public void testNonSealed() {
        TypeDeclSnippet base = classKey(
                assertEval("sealed class B permits I {}",
                           ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_NOT_DEFINED, false, null)));
        assertEval("non-sealed class I extends B {}",
                   added(VALID),
                   ste(base, Status.RECOVERABLE_NOT_DEFINED, Status.VALID, true, null));
        assertEval("class I2 extends I {}");
        assertEval("new I2()");
    }
}
