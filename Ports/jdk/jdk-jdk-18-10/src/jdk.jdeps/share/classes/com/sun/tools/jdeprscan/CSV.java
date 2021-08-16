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

package com.sun.tools.jdeprscan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for manipulating comma-separated-value (CSV) data.
 */
public class CSV {
    static String quote(String input) {
        String result;
        boolean needQuote = input.contains(",");

        if (input.contains("\"")) {
            needQuote = true;
            result = input.replace("\"", "\"\"");
        } else {
            result = input;
        }

        if (needQuote) {
            return "\"" + result + "\"";
        } else {
            return result;
        }
    }

    /**
     * Writes the objects' string representations to the output as a line of CSV.
     * The objects are converted to String, quoted if necessary, joined with commas,
     * and are written to the output followed by the line separator string.
     *
     * @param out the output destination
     * @param objs the objects to write
     */
    public static void write(PrintStream out, Object... objs) {
        out.println(Arrays.stream(objs)
                          .map(Object::toString)
                          .map(CSV::quote)
                          .collect(Collectors.joining(",")));
    }

    /**
     * The CSV parser state.
     */
    enum State {
        START_FIELD, // the start of a field
        IN_FIELD,    // within an unquoted field
        IN_QFIELD,   // within a quoted field
        END_QFIELD   // after the end of a quoted field
    }

    /**
     * Splits an input line into a list of strings, handling quoting.
     *
     * @param input the input line
     * @return the resulting list of strings
     */
    public static List<String> split(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        State state = State.START_FIELD;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case ',':
                    switch (state) {
                        case IN_QFIELD:
                            cur.append(',');
                            break;
                        default:
                            result.add(cur.toString());
                            cur.setLength(0);
                            state = State.START_FIELD;
                            break;
                    }
                    break;
                case '"':
                    switch (state) {
                        case START_FIELD:
                            state = State.IN_QFIELD;
                            break;
                        case IN_QFIELD:
                            state = State.END_QFIELD;
                            break;
                        case IN_FIELD:
                            throw new CSVParseException("unexpected quote", input, i);
                        case END_QFIELD:
                            cur.append('"');
                            state = State.IN_QFIELD;
                            break;
                    }
                    break;
                default:
                    switch (state) {
                        case START_FIELD:
                            state = State.IN_FIELD;
                            break;
                        case IN_FIELD:
                        case IN_QFIELD:
                            break;
                        case END_QFIELD:
                            throw new CSVParseException("extra character after quoted string",
                                                        input, i);
                    }
                    cur.append(ch);
                    break;
            }
        }

        if (state == State.IN_QFIELD) {
            throw new CSVParseException("unclosed quote", input, input.length());
        }

        result.add(cur.toString());
        return result;
    }
}
