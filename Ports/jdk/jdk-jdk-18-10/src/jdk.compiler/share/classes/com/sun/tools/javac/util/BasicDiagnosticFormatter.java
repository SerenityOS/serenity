/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.util.AbstractDiagnosticFormatter.SimpleConfiguration;
import com.sun.tools.javac.util.BasicDiagnosticFormatter.BasicConfiguration;

import static com.sun.tools.javac.api.DiagnosticFormatter.PositionKind.*;
import static com.sun.tools.javac.util.BasicDiagnosticFormatter.BasicConfiguration.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;

/**
 * A basic formatter for diagnostic messages.
 * The basic formatter will format a diagnostic according to one of three format patterns, depending on whether
 * or not the source name and position are set. The formatter supports a printf-like string for patterns
 * with the following special characters:
 * <ul>
 * <li>%b: the base of the source name
 * <li>%f: the source name (full absolute path)
 * <li>%l: the line number of the diagnostic, derived from the character offset
 * <li>%c: the column number of the diagnostic, derived from the character offset
 * <li>%o: the character offset of the diagnostic if set
 * <li>%p: the prefix for the diagnostic, derived from the diagnostic type
 * <li>%t: the prefix as it normally appears in standard diagnostics. In this case, no prefix is
 *        shown if the type is ERROR and if a source name is set
 * <li>%m: the text or the diagnostic, including any appropriate arguments
 * <li>%_: space delimiter, useful for formatting purposes
 * </ul>
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class BasicDiagnosticFormatter extends AbstractDiagnosticFormatter {

    /**
     * Create a basic formatter based on the supplied options.
     *
     * @param options list of command-line options
     * @param msgs JavacMessages object used for i18n
     */
    public BasicDiagnosticFormatter(Options options, JavacMessages msgs) {
        super(msgs, new BasicConfiguration(options));
    }

    /**
     * Create a standard basic formatter
     *
     * @param msgs JavacMessages object used for i18n
     */
    public BasicDiagnosticFormatter(JavacMessages msgs) {
        super(msgs, new BasicConfiguration());
    }

    public String formatDiagnostic(JCDiagnostic d, Locale l) {
        if (l == null)
            l = messages.getCurrentLocale();
        String format = selectFormat(d);
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            boolean meta = false;
            if (c == '%' && i < format.length() - 1) {
                meta = true;
                c = format.charAt(++i);
            }
            buf.append(meta ? formatMeta(c, d, l) : String.valueOf(c));
        }
        if (depth == 0)
            return addSourceLineIfNeeded(d, buf.toString());
        else
            return buf.toString();
    }

    public String formatMessage(JCDiagnostic d, Locale l) {
        int currentIndentation = 0;
        StringBuilder buf = new StringBuilder();
        Collection<String> args = formatArguments(d, l);
        String msg = localize(l, d.getCode(), args.toArray());
        String[] lines = msg.split("\n");
        if (lines.length == 0) // will happen when msg only contains one or more separators: "\n", "\n\n", etc.
            lines = new String[] { "" };
        if (getConfiguration().getVisible().contains(DiagnosticPart.SUMMARY)) {
            currentIndentation += getConfiguration().getIndentation(DiagnosticPart.SUMMARY);
            buf.append(indent(lines[0], currentIndentation)); //summary
        }
        if (lines.length > 1 && getConfiguration().getVisible().contains(DiagnosticPart.DETAILS)) {
            currentIndentation += getConfiguration().getIndentation(DiagnosticPart.DETAILS);
            for (int i = 1;i < lines.length; i++) {
                buf.append("\n" + indent(lines[i], currentIndentation));
            }
        }
        if (d.isMultiline() && getConfiguration().getVisible().contains(DiagnosticPart.SUBDIAGNOSTICS)) {
            currentIndentation += getConfiguration().getIndentation(DiagnosticPart.SUBDIAGNOSTICS);
                for (String sub : formatSubdiagnostics(d, l)) {
                    buf.append("\n" + indent(sub, currentIndentation));
            }
        }
        return buf.toString();
    }

    protected String addSourceLineIfNeeded(JCDiagnostic d, String msg) {
        if (!displaySource(d))
            return msg;
        else {
            BasicConfiguration conf = getConfiguration();
            int indentSource = conf.getIndentation(DiagnosticPart.SOURCE);
            String sourceLine = "\n" + formatSourceLine(d, indentSource);
            boolean singleLine = !msg.contains("\n");
            if (singleLine || getConfiguration().getSourcePosition() == SourcePosition.BOTTOM)
                return msg + sourceLine;
            else
                return msg.replaceFirst("\n", Matcher.quoteReplacement(sourceLine) + "\n");
        }
    }

    protected String formatMeta(char c, JCDiagnostic d, Locale l) {
        switch (c) {
            case 'b':
                return formatSource(d, false, l);
            case 'e':
                return formatPosition(d, END, l);
            case 'f':
                return formatSource(d, true, l);
            case 'l':
                return formatPosition(d, LINE, l);
            case 'c':
                return formatPosition(d, COLUMN, l);
            case 'o':
                return formatPosition(d, OFFSET, l);
            case 'p':
                return formatKind(d, l);
            case 's':
                return formatPosition(d, START, l);
            case 't': {
                boolean usePrefix;
                switch (d.getType()) {
                case FRAGMENT:
                    usePrefix = false;
                    break;
                case ERROR:
                    usePrefix = (d.getIntPosition() == Position.NOPOS);
                    break;
                default:
                    usePrefix = true;
                }
                if (usePrefix)
                    return formatKind(d, l);
                else
                    return "";
            }
            case 'm':
                return formatMessage(d, l);
            case 'L':
                return formatLintCategory(d, l);
            case '_':
                return " ";
            case '%':
                return "%";
            default:
                return String.valueOf(c);
        }
    }

    private String selectFormat(JCDiagnostic d) {
        DiagnosticSource source = d.getDiagnosticSource();
        String format = getConfiguration().getFormat(BasicFormatKind.DEFAULT_NO_POS_FORMAT);
        if (source != null && source != DiagnosticSource.NO_SOURCE) {
            if (d.getIntPosition() != Position.NOPOS) {
                format = getConfiguration().getFormat(BasicFormatKind.DEFAULT_POS_FORMAT);
            } else if (source.getFile() != null &&
                       source.getFile().getKind() == JavaFileObject.Kind.CLASS) {
                format = getConfiguration().getFormat(BasicFormatKind.DEFAULT_CLASS_FORMAT);
            }
        }
        return format;
    }

    @Override
    public BasicConfiguration getConfiguration() {
        //the following cast is always safe - see init
        return (BasicConfiguration)super.getConfiguration();
    }

    public static class BasicConfiguration extends SimpleConfiguration {

        protected Map<DiagnosticPart, Integer> indentationLevels;
        protected Map<BasicFormatKind, String> availableFormats;
        protected SourcePosition sourcePosition;

        @SuppressWarnings("fallthrough")
        public BasicConfiguration(Options options) {
            super(options, EnumSet.of(DiagnosticPart.SUMMARY,
                            DiagnosticPart.DETAILS,
                            DiagnosticPart.SUBDIAGNOSTICS,
                            DiagnosticPart.SOURCE));
            initFormat();
            initIndentation();
            if (options.isSet("diags.legacy"))
                initOldFormat();
            String fmt = options.get("diags.layout");
            if (fmt != null) {
                if (fmt.equals("OLD"))
                    initOldFormat();
                else
                    initFormats(fmt);
            }
            String srcPos = null;
            if ((((srcPos = options.get("diags.sourcePosition")) != null)) &&
                    srcPos.equals("bottom"))
                    setSourcePosition(SourcePosition.BOTTOM);
            else
                setSourcePosition(SourcePosition.AFTER_SUMMARY);
            String indent = options.get("diags.indent");
            if (indent != null) {
                String[] levels = indent.split("\\|");
                try {
                    switch (levels.length) {
                        case 5:
                            setIndentation(DiagnosticPart.JLS,
                                    Integer.parseInt(levels[4]));
                        case 4:
                            setIndentation(DiagnosticPart.SUBDIAGNOSTICS,
                                    Integer.parseInt(levels[3]));
                        case 3:
                            setIndentation(DiagnosticPart.SOURCE,
                                    Integer.parseInt(levels[2]));
                        case 2:
                            setIndentation(DiagnosticPart.DETAILS,
                                    Integer.parseInt(levels[1]));
                        default:
                            setIndentation(DiagnosticPart.SUMMARY,
                                    Integer.parseInt(levels[0]));
                    }
                }
                catch (NumberFormatException ex) {
                    initIndentation();
                }
            }
        }

        public BasicConfiguration() {
            super(EnumSet.of(DiagnosticPart.SUMMARY,
                  DiagnosticPart.DETAILS,
                  DiagnosticPart.SUBDIAGNOSTICS,
                  DiagnosticPart.SOURCE));
            initFormat();
            initIndentation();
        }

        private void initFormat() {
            initFormats("%f:%l:%_%p%L%m", "%p%L%m", "%f:%_%p%L%m");
        }

        private void initOldFormat() {
            initFormats("%f:%l:%_%t%L%m", "%p%L%m", "%f:%_%t%L%m");
        }

        private void initFormats(String pos, String nopos, String clazz) {
            availableFormats = new EnumMap<>(BasicFormatKind.class);
            setFormat(BasicFormatKind.DEFAULT_POS_FORMAT,    pos);
            setFormat(BasicFormatKind.DEFAULT_NO_POS_FORMAT, nopos);
            setFormat(BasicFormatKind.DEFAULT_CLASS_FORMAT,  clazz);
        }

        @SuppressWarnings("fallthrough")
        private void initFormats(String fmt) {
            String[] formats = fmt.split("\\|");
            switch (formats.length) {
                case 3:
                    setFormat(BasicFormatKind.DEFAULT_CLASS_FORMAT, formats[2]);
                case 2:
                    setFormat(BasicFormatKind.DEFAULT_NO_POS_FORMAT, formats[1]);
                default:
                    setFormat(BasicFormatKind.DEFAULT_POS_FORMAT, formats[0]);
            }
        }

        private void initIndentation() {
            indentationLevels = new HashMap<>();
            setIndentation(DiagnosticPart.SUMMARY, 0);
            setIndentation(DiagnosticPart.DETAILS, DetailsInc);
            setIndentation(DiagnosticPart.SUBDIAGNOSTICS, DiagInc);
            setIndentation(DiagnosticPart.SOURCE, 0);
        }

        /**
         * Get the amount of spaces for a given indentation kind
         * @param diagPart the diagnostic part for which the indentation is
         * to be retrieved
         * @return the amount of spaces used for the specified indentation kind
         */
        public int getIndentation(DiagnosticPart diagPart) {
            return indentationLevels.get(diagPart);
        }

        /**
         * Set the indentation level for various element of a given diagnostic -
         * this might lead to more readable diagnostics
         *
         * @param diagPart
         * @param nSpaces amount of spaces for the specified diagnostic part
         */
        public void setIndentation(DiagnosticPart diagPart, int nSpaces) {
            indentationLevels.put(diagPart, nSpaces);
        }

        /**
         * Set the source line positioning used by this formatter
         *
         * @param sourcePos a positioning value for source line
         */
        public void setSourcePosition(SourcePosition sourcePos) {
            sourcePosition = sourcePos;
        }

        /**
         * Get the source line positioning used by this formatter
         *
         * @return the positioning value used by this formatter
         */
        public SourcePosition getSourcePosition() {
            return sourcePosition;
        }
        //where
        /**
         * A source positioning value controls the position (within a given
         * diagnostic message) in which the source line the diagnostic refers to
         * should be displayed (if applicable)
         */
        public enum SourcePosition {
            /**
             * Source line is displayed after the diagnostic message
             */
            BOTTOM,
            /**
             * Source line is displayed after the first line of the diagnostic
             * message
             */
            AFTER_SUMMARY
        }

        /**
         * Set a metachar string for a specific format
         *
         * @param kind the format kind to be set
         * @param s the metachar string specifying the format
         */
        public void setFormat(BasicFormatKind kind, String s) {
            availableFormats.put(kind, s);
        }

        /**
         * Get a metachar string for a specific format
         *
         * @param kind the format kind for which to get the metachar string
         */
        public String getFormat(BasicFormatKind kind) {
            return availableFormats.get(kind);
        }
        //where
        /**
         * This enum contains all the kinds of formatting patterns supported
         * by a basic diagnostic formatter.
         */
        public enum BasicFormatKind {
            /**
            * A format string to be used for diagnostics with a given position.
            */
            DEFAULT_POS_FORMAT,
            /**
            * A format string to be used for diagnostics without a given position.
            */
            DEFAULT_NO_POS_FORMAT,
            /**
            * A format string to be used for diagnostics regarding classfiles
            */
            DEFAULT_CLASS_FORMAT
        }
    }
}
