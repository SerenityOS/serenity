/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jshell;

import java.util.LinkedHashMap;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import jdk.jshell.Wrap.CompoundWrap;

/**
 * The outer wrap for a set of snippets wrapped in a generated class
 * @author Robert Field
 */
class OuterSnippetsClassWrap extends OuterWrap {

    private final String className;
    private final LinkedHashMap<Wrap, Snippet> wrapToSnippet;

    OuterSnippetsClassWrap(String className, CompoundWrap w, List<Snippet> snippets, List<Wrap> wraps) {
        super(w);
        assert snippets == null || snippets.size() == wraps.size();
        this.className = className;
        wrapToSnippet = new LinkedHashMap<>();
        for (int i = 0; i < snippets.size(); ++i) {
            wrapToSnippet.put(wraps.get(i), snippets.get(i));
        }
    }

    public Snippet wrapLineToSnippet(int wline) {
        Wrap wrap = ((CompoundWrap)w).wrapLineToWrap(wline);
        return wrapToSnippet.get(wrap);
    }

    @Override
    Diag wrapDiag(Diagnostic<? extends JavaFileObject> d) {
        return new WrappedDiagnostic(d) {

            @Override
            Snippet snippetOrNull() {
                Wrap wrap = ((CompoundWrap) w).wrapIndexToWrap(diag.getPosition());
                Snippet sn = wrapToSnippet.get(wrap);
                if (sn != null) {
                    return sn;
                } else {
                    return super.snippetOrNull();
                }
            }
        };
    }

    int ordinal(Snippet sn) {
        int i = 0;
        for (Snippet si : wrapToSnippet.values()) {
            if (si == sn) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    @Override
    public String className() {
        return className;
    }

    @Override
    public String toString() {
        return "OSCW(" + className + ":" + w + ")";
    }
}
