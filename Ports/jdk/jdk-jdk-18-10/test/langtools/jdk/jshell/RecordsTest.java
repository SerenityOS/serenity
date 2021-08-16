/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8235474 8236715 8246774
 * @summary Tests for evalution of records
 * @modules jdk.jshell
 * @build KullaTesting TestingInputStream ExpectedDiagnostic
 * @run testng RecordsTest
 */

import org.testng.annotations.Test;

import javax.lang.model.SourceVersion;
import jdk.jshell.Snippet.Status;
import jdk.jshell.UnresolvedReferenceException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;

@Test
public class RecordsTest extends KullaTesting {

    public void testRecordClass() {
        assertEval("record R(String s, int i) { }");
        assertEquals(varKey(assertEval("R r = new R(\"r\", 42);")).name(), "r");
        assertEval("r.s()", "\"r\"");
        assertEval("r.i()", "42");
    }

    public void testRecordCorralling() {
        //simple record with a mistake that can be fixed by corralling:
        assertEval("record R1(int i) { int g() { return j; } }", ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_DEFINED, true, null));
        assertEval("R1 r1 = new R1(1);", null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(Status.VALID));
        //record with a concise constructor and a mistake take can be fixed by corralling:
        assertEval("record R2(int i) { public R2 {} int g() { return j; } }", ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_DEFINED, true, null));
        assertEval("R2 r2 = new R2(1);", null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(Status.VALID));
        //record with a full constructor and a mistake take can be fixed by corralling:
        assertEval("record R3(int i) { public R3(int i) {this.i = i;} int g() { return j; } }", ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_DEFINED, true, null));
        assertEval("R3 r3 = new R3(1);", null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(Status.VALID));
        //record with an accessor and a mistake take can be fixed by corralling:
        assertEval("record R4(int i) { public int i() { return i; } int g() { return j; } }", ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_DEFINED, true, null));
        assertEval("R4 r4 = new R4(1);", null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(Status.VALID));
        //record with an accessor with a mistake take can be fixed by corralling:
        assertEval("record R5(int i) { public int i() { return j; } }", ste(MAIN_SNIPPET, Status.NONEXISTENT, Status.RECOVERABLE_DEFINED, true, null));
        assertEval("R5 r5 = new R5(1);", null, UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, added(Status.VALID));
    }

    public void testRecordField() {
        assertEquals(varKey(assertEval("String record = \"\";")).name(), "record");
        assertEval("record.length()", "0");
    }

    public void testRecordMethod() {
        assertEquals(methodKey(assertEval("String record(String record) { return record + record; }")).name(), "record");
        assertEval("record(\"r\")", "\"rr\"");
        assertEval("record(\"r\").length()", "2");
    }
}
