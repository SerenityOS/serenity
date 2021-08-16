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
 * @bug 8248157
 * @summary test Unicode characters in Snippets
 * @build KullaTesting TestingInputStream
 * @run testng UnicodeTest
 */

import jdk.jshell.Snippet;
import jdk.jshell.DeclarationSnippet;
import org.testng.annotations.Test;

import jdk.jshell.Snippet.Status;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Snippet.SubKind.*;

@Test
public class UnicodeTest extends KullaTesting {

    public void testVarDeclarationKey() {
        assertVarKeyMatch("int \\u00aa;", true, "\u00aa", VAR_DECLARATION_SUBKIND, "int", added(VALID));
        assertEval("\\u00aa", "0");
    }

    public void testVarDeclarationWithInitializerKey() {
        assertVarKeyMatch("double \\u00ba\\u0044\\u0577 = 9.4;", true, "\u00ba\u0044\u0577",
                          VAR_DECLARATION_WITH_INITIALIZER_SUBKIND, "double", added(VALID));
        assertEval("\\u00ba\\u0044\\u0577", "9.4");
    }
}
