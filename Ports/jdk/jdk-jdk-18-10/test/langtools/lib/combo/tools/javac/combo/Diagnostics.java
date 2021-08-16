/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package tools.javac.combo;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
* A container for compiler diagnostics, separated into errors and warnings,
 * used by JavacTemplateTestBase.
 *
 * @author Brian Goetz
*/
public class Diagnostics implements javax.tools.DiagnosticListener<JavaFileObject> {

    protected List<Diagnostic<? extends JavaFileObject>> diags = new ArrayList<>();

    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        diags.add(diagnostic);
    }

    /** Were there any errors found? */
    public boolean errorsFound() {
        return diags.stream()
                    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
    }

    /** Get all diagnostic keys */
    public List<String> keys() {
        return diags.stream()
                    .map(Diagnostic::getCode)
                    .collect(toList());
    }

    public Diagnostic<?> getDiagWithKey(String key) {
        for (Diagnostic<?> d : diags) {
            if (d.getCode().equals(key)) {
                return d;
            }
        }
        return null;
    }

    public List<Diagnostic<?>> getAllDiags() {
        return diags.stream().map(d -> (Diagnostic<?>)d).collect(toList());
    }

    /** Do the diagnostics contain the specified error key? */
    public boolean containsErrorKey(String key) {
        return diags.stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .anyMatch(d -> d.getCode().equals(key));
    }

    /** Do the diagnostics contain the specified warning key? */
    public boolean containsWarningKey(String key) {
        return diags.stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.WARNING || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING)
                    .anyMatch(d -> d.getCode().equals(key));
    }

    /** Get the error keys */
    public List<String> errorKeys() {
        return diags.stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(Diagnostic::getCode)
                    .collect(toList());
    }

    public String toString() { return keys().toString(); }

    /** Clear all diagnostic state */
    public void reset() {
        diags.clear();
    }
}
