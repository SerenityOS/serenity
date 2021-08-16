/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.tools.Diagnostic;

import jdk.jshell.Diag;
import static org.testng.Assert.assertEquals;

public class ExpectedDiagnostic {

    private final String code;
    private final long startPosition;
    private final long endPosition;
    private final long position;
    private final long lineNumber;
    private final long columnNumber;
    private final Diagnostic.Kind kind;

    public ExpectedDiagnostic(
            String code, long startPosition, long endPosition,
            long position, long lineNumber, long columnNumber,
            Diagnostic.Kind kind) {
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.position = position;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.kind = kind;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public String getCode() {
        return code;
    }

    public long getEndPosition() {
        return endPosition;
    }

    public long getPosition() {
        return position;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

    public Diagnostic.Kind getKind() {
        return kind;
    }

    public void assertDiagnostic(Diag diagnostic) {
        String code = diagnostic.getCode();
        assertEquals(code, this.code, "Expected error: " + this.code + ", got: " + code);
        assertEquals(diagnostic.isError(), kind == Diagnostic.Kind.ERROR);
        if (startPosition != -1) {
            assertEquals(diagnostic.getStartPosition(), startPosition, "Start position");
        }
        if (endPosition != -1) {
            assertEquals(diagnostic.getEndPosition(), endPosition, "End position");
        }
        if (position != -1) {
            assertEquals(diagnostic.getPosition(), position, "Position");
        }
    }
}
