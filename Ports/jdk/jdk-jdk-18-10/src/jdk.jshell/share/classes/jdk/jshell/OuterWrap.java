/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import static jdk.jshell.Util.PARSED_LOCALE;
import static jdk.jshell.Util.REPL_CLASS_PREFIX;
import static jdk.jshell.Util.REPL_DOESNOTMATTER_CLASS_NAME;
import static jdk.jshell.Util.REPL_PACKAGE;
import static jdk.jshell.Util.expunge;

/**
 *
 * @author Robert Field
 */
class OuterWrap implements GeneralWrap {

    protected final Wrap w;

    OuterWrap(Wrap wrap) {
        this.w = wrap;
    }

    @Override
    public final String wrapped() {
        return w.wrapped();
    }

    @Override
    public int snippetIndexToWrapIndex(int ui) {
        return w.snippetIndexToWrapIndex(ui);
    }

    @Override
    public int wrapIndexToSnippetIndex(int si) {
        return w.wrapIndexToSnippetIndex(si);
    }

    @Override
    public int firstSnippetIndex() {
        return w.firstSnippetIndex();
    }

    @Override
    public int lastSnippetIndex() {
        return w.lastSnippetIndex();
    }

    @Override
    public int snippetLineToWrapLine(int snline) {
        return w.snippetLineToWrapLine(snline);
    }

    @Override
    public int wrapLineToSnippetLine(int wline) {
        return w.wrapLineToSnippetLine(wline);
    }

    @Override
    public int firstSnippetLine() {
        return w.firstSnippetLine();
    }

    @Override
    public int lastSnippetLine() {
        return w.lastSnippetLine();
    }

    public String className() {
        return REPL_DOESNOTMATTER_CLASS_NAME;
    }

    public String classFullName() {
        return REPL_PACKAGE + "." + className();
    }

    @Override
    public int hashCode() {
        return className().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof OuterWrap)
                ? className().equals(((OuterWrap) o).className())
                : false;
    }

    @Override
    public String toString() {
        return "OW(" + w + ")";
    }

    Diag wrapDiag(Diagnostic<? extends JavaFileObject> d) {
        return new WrappedDiagnostic(d);
    }

    class WrappedDiagnostic extends Diag {

        final Diagnostic<? extends JavaFileObject> diag;

        WrappedDiagnostic(Diagnostic<? extends JavaFileObject> diag) {
            this.diag = diag;
        }

        @Override
        public boolean isError() {
            return diag.getKind() == Diagnostic.Kind.ERROR;
        }

        @Override
        public long getPosition() {
            return wrapIndexToSnippetIndex(diag.getPosition());
        }

        @Override
        public long getStartPosition() {
            return wrapIndexToSnippetIndex(diag.getStartPosition());
        }

        @Override
        public long getEndPosition() {
            return wrapIndexToSnippetIndex(diag.getEndPosition());
        }

        @Override
        public String getCode() {
            return diag.getCode();
        }

        @Override
        public String getMessage(Locale locale) {
            return expunge(diag.getMessage(locale));
        }

        @Override
        boolean isResolutionError() {
            if (!super.isResolutionError()) {
                return false;
            }
            for (String line : diag.getMessage(PARSED_LOCALE).split("\\r?\\n")) {
                if (line.trim().startsWith("location:")) {
                    if (!line.contains(REPL_CLASS_PREFIX)) {
                        // Resolution error must occur within a REPL class or it is not resolvable
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "WrappedDiagnostic(" + getMessage(null) + ":" + getPosition() + ")";
        }
    }
}
