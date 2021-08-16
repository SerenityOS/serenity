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
 * @test 8080352
 * @summary Tests for hard errors, like syntax errors
 * @build KullaTesting
 * @run testng RejectedFailedTest
 */

import java.util.List;

import jdk.jshell.Snippet.SubKind;
import org.testng.annotations.Test;

import jdk.jshell.Diag;
import jdk.jshell.Snippet;
import jdk.jshell.Snippet.Kind;
import jdk.jshell.Snippet.Status;

import jdk.jshell.SnippetEvent;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class RejectedFailedTest extends KullaTesting {

    private String bad(String input, Kind kind, String prevId) {
        List<SnippetEvent> events = assertEvalFail(input);
        assertEquals(events.size(), 1, "Expected one event, got: " + events.size());
        SnippetEvent e = events.get(0);
        List<Diag> diagnostics = getState().diagnostics(e.snippet()).collect(toList());
        assertTrue(diagnostics.size() > 0, "Expected diagnostics, got none");
        assertEquals(e.exception(), null, "Expected exception to be null.");
        assertEquals(e.value(), null, "Expected value to be null.");

        Snippet key = e.snippet();
        assertTrue(key != null, "key must be non-null, but was null.");
        assertEquals(key.kind(), kind, "Expected kind: " + kind + ", got: " + key.kind());
        SubKind expectedSubKind = kind == Kind.ERRONEOUS ? SubKind.UNKNOWN_SUBKIND : SubKind.METHOD_SUBKIND;
        assertEquals(key.subKind(), expectedSubKind, "SubKind: ");
        assertTrue(key.id().compareTo(prevId) > 0, "Current id: " + key.id() + ", previous: " + prevId);
        assertEquals(getState().diagnostics(key).collect(toList()), diagnostics, "Expected retrieved diagnostics to match, but didn't.");
        assertEquals(key.source(), input, "Expected retrieved source: " +
                key.source() + " to match input: " + input);
        assertEquals(getState().status(key), Status.REJECTED, "Expected status of REJECTED, got: " + getState().status(key));
        return key.id();
    }

    private void checkByKind(String[] inputs, Kind kind) {
        String prevId = "";
        for (String in : inputs) {
            prevId = bad(in, kind, prevId);
        }
    }

    public void testErroneous() {
        String[] inputsErroneous = {
                "%&^%&",
                " a b c",
                ")",
                "class interface A",
                "package foo;"
        };
        checkByKind(inputsErroneous, Kind.ERRONEOUS);
    }

    public void testBadMethod() {
        String[] inputsMethod = {
                "transient int m() { return x; }",
                "int h() { }",
                "void l() { return 4; }",
                "int vv(void x) { return 2; }",
        };
        checkByKind(inputsMethod, Kind.METHOD);
    }
}
