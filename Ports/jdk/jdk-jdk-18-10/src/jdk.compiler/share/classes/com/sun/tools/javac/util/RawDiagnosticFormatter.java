/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.api.DiagnosticFormatter.Configuration.*;
import com.sun.tools.javac.api.Formattable;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.file.PathFileObject;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.api.DiagnosticFormatter.PositionKind.*;

/**
 * A raw formatter for diagnostic messages.
 * The raw formatter will format a diagnostic according to one of two format patterns, depending on whether
 * or not the source name and position are set. This formatter provides a standardized, localize-independent
 * implementation of a diagnostic formatter; as such, this formatter is best suited for testing purposes.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public final class RawDiagnosticFormatter extends AbstractDiagnosticFormatter {

    /**
     * The raw diagnostic helper.
     */
    RawDiagnosticPosHelper rawDiagnosticPosHelper;

    /**
     * Helper class to generate stable positions for AST nodes occurring in diagnostic arguments.
     * If the AST node appears in the same line number as the main diagnostic, the line is information is omitted.
     * Otherwise both line and column information is included, using the format @{code line:col}".
     *
     * Note: since subdiagnostics can be created without a diagnostic source, a position helper
     * should always refer to the toplevel diagnostic source.
     */
    static class RawDiagnosticPosHelper {
        private final JCDiagnostic diag;

        RawDiagnosticPosHelper(JCDiagnostic diag) {
            this.diag = diag;
        }

        String getPosition(JCExpression exp) {
            DiagnosticSource diagSource = diag.getDiagnosticSource();
            long diagLine = diag.getLineNumber();
            long expLine = diagSource.getLineNumber(exp.pos);
            long expCol = diagSource.getColumnNumber(exp.pos, false);
            return (expLine == diagLine) ?
                    String.valueOf(expCol) :
                    expLine + ":" + expCol;
        }
    }

    /**
     * Create a formatter based on the supplied options.
     * @param options
     */
    public RawDiagnosticFormatter(Options options) {
        super(null, new SimpleConfiguration(options,
                EnumSet.of(DiagnosticPart.SUMMARY,
                        DiagnosticPart.DETAILS,
                        DiagnosticPart.SUBDIAGNOSTICS)));
    }

    //provide common default formats
    public String formatDiagnostic(JCDiagnostic d, Locale l) {
        try {
            rawDiagnosticPosHelper = new RawDiagnosticPosHelper(d);
            StringBuilder buf = new StringBuilder();
            if (d.getPosition() != Position.NOPOS) {
                buf.append(formatSource(d, false, null));
                buf.append(':');
                buf.append(formatPosition(d, LINE, null));
                buf.append(':');
                buf.append(formatPosition(d, COLUMN, null));
                buf.append(':');
            }
            else if (d.getSource() != null && d.getSource().getKind() == JavaFileObject.Kind.CLASS) {
                buf.append(formatSource(d, false, null));
                buf.append(":-:-:");
            }
            else
                buf.append('-');
            buf.append(' ');
            buf.append(formatMessage(d, null));
            if (displaySource(d)) {
                buf.append("\n");
                buf.append(formatSourceLine(d, 0));
            }
            return buf.toString();
        }
        catch (Exception e) {
            return null;
        } finally {
            rawDiagnosticPosHelper = null;
        }
    }

    public String formatMessage(JCDiagnostic d, Locale l) {
        StringBuilder buf = new StringBuilder();
        Collection<String> args = formatArguments(d, l);
        buf.append(localize(null, d.getCode(), args.toArray()));
        if (d.isMultiline() && getConfiguration().getVisible().contains(DiagnosticPart.SUBDIAGNOSTICS)) {
            List<String> subDiags = formatSubdiagnostics(d, null);
            if (subDiags.nonEmpty()) {
                String sep = "";
                buf.append(",{");
                for (String sub : formatSubdiagnostics(d, null)) {
                    buf.append(sep);
                    buf.append("(");
                    buf.append(sub);
                    buf.append(")");
                    sep = ",";
                }
                buf.append('}');
            }
        }
        return buf.toString();
    }

    @Override
    protected String formatArgument(JCDiagnostic diag, Object arg, Locale l) {
        String s;
        if (arg instanceof Formattable) {
            s = arg.toString();
        } else if (arg instanceof JCExpression expression) {
            Assert.checkNonNull(rawDiagnosticPosHelper);
            s = "@" + rawDiagnosticPosHelper.getPosition(expression);
        } else if (arg instanceof PathFileObject pathFileObject) {
            s = pathFileObject.getShortName();
        } else if (arg instanceof Tag tag) {
            s = "compiler.misc.tree.tag." + StringUtils.toLowerCase(tag.name());
        } else if (arg instanceof Source && arg == Source.DEFAULT &&
                CODES_NEEDING_SOURCE_NORMALIZATION.contains(diag.getCode())) {
            s = "DEFAULT";
        } else {
            s = super.formatArgument(diag, arg, null);
        }
        return (arg instanceof JCDiagnostic) ? "(" + s + ")" : s;
    }
    //where:
        private static final Set<String> CODES_NEEDING_SOURCE_NORMALIZATION = Set.of(
                "compiler.note.preview.filename", "compiler.note.preview.plural");

    @Override
    protected String localize(Locale l, String key, Object... args) {
        StringBuilder buf = new StringBuilder();
        buf.append(key);
        String sep = ": ";
        for (Object o : args) {
            buf.append(sep);
            buf.append(o);
            sep = ", ";
        }
        return buf.toString();
    }

    @Override
    public boolean isRaw() {
        return true;
    }
}
