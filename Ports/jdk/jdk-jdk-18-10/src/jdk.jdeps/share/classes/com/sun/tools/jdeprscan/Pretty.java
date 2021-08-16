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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for pretty-printing various bits of API syntax.
 */
public class Pretty {
    /**
     * Converts deprecation information into an {@code @Deprecated} annotation.
     * The output is minimized: an empty since string is omitted, a forRemoval
     * value of false is omitted; and if both are omitted, the trailing parentheses
     * are also omitted.
     *
     * @param since the since value
     * @param forRemoval the forRemoval value
     * @return string containing an annotation
     */
    static String depr(String since, boolean forRemoval) {
        String d = "@Deprecated";

        if (since.isEmpty() && !forRemoval) {
            return d;
        }

        StringBuilder sb = new StringBuilder(d).append('(');

        if (!since.isEmpty()) {
            sb.append("since=\"")
              .append(since.replace("\"", "\\\""))
              .append('"');
        }

        if (forRemoval) {
            if (!since.isEmpty()) {
                sb.append(", ");
            }
            sb.append("forRemoval=true");
        }

        sb.append(')');

        return sb.toString();
    }

    /**
     * Converts a slash-$ style name into a dot-separated name.
     *
     * @param n the input name
     * @return the result name
     */
    static String unslashify(String n) {
        return n.replace("/", ".")
                .replace("$", ".");
    }

    /**
     * Converts a type descriptor to a readable string.
     *
     * @param desc the input descriptor
     * @return the result string
     */
    static String desc(String desc) {
        return desc(desc, new int[] { 0 });
    }

    /**
     * Converts one type descriptor to a readable string, starting
     * from position {@code pos_inout[0]}, and updating it to the
     * location following the descriptor just parsed. A type descriptor
     * mostly corresponds to a FieldType in JVMS 4.3.2. It can be one of a
     * BaseType (a single character denoting a primitive, plus void),
     * an object type ("Lname;"), or an array type (one more more '[' followed
     * by a base or object type).
     *
     * @param desc a string possibly containing several descriptors
     * @param pos_inout on input, the start position; on return, the position
     *                  following the just-parsed descriptor
     * @return the result string
     */
    static String desc(String desc, int[] pos_inout) {
        int dims = 0;
        int pos = pos_inout[0];
        final int len = desc.length();

        while (pos < len && desc.charAt(pos) == '[') {
            pos++;
            dims++;
        }

        String name;

        if (pos >= len) {
            return null;
        }

        char c = desc.charAt(pos++);
        switch (c) {
            case 'Z':
                name = "boolean";
                break;
            case 'B':
                name = "byte";
                break;
            case 'S':
                name = "short";
                break;
            case 'C':
                name = "char";
                break;
            case 'I':
                name = "int";
                break;
            case 'J':
                name = "long";
                break;
            case 'F':
                name = "float";
                break;
            case 'D':
                name = "double";
                break;
            case 'V':
                name = "void";
                break;
            case 'L':
                int semi = desc.indexOf(';', pos);
                if (semi == -1) {
                    return null;
                }
                name = unslashify(desc.substring(pos, semi));
                pos = semi + 1;
                break;
            default:
                return null;
        }

        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < dims; i++) {
            sb.append("[]");
        }
        pos_inout[0] = pos;
        return sb.toString();
    }

    /**
     * Converts a series of type descriptors into a comma-separated,
     * readable string. This is used for the parameter types of a
     * method descriptor.
     *
     * @param types the parameter types
     * @return the readable string
     */
    static String parms(String types) {
        int[] pos = new int[] { 0 };
        StringBuilder sb = new StringBuilder();

        boolean first = true;

        String t;

        while ((t = desc(types, pos)) != null) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(t);
        }

        return sb.toString();
    }

    /**
     * Pattern for matching a method descriptor. Match results can
     * be retrieved from named capture groups as follows: "name(params)return".
     */
    static final Pattern DESC_PAT = Pattern.compile("(?<name>.*)\\((?<args>.*)\\)(?<return>.*)");

    /**
     * Pretty-prints the data contained in the given DeprData object.
     *
     * @param dd the deprecation data object
     * @return the formatted string
     */
    public static String print(DeprData dd) {
        StringBuilder sb = new StringBuilder();
        sb.append(depr(dd.since, dd.forRemoval))
          .append(' ');

        switch (dd.kind) {
            case ANNOTATION_TYPE:
                sb.append("@interface ");
                sb.append(unslashify(dd.typeName));
                break;
            case CLASS:
                sb.append("class ");
                sb.append(unslashify(dd.typeName));
                break;
            case ENUM:
                sb.append("enum ");
                sb.append(unslashify(dd.typeName));
                break;
            case INTERFACE:
                sb.append("interface ");
                sb.append(unslashify(dd.typeName));
                break;

            case ENUM_CONSTANT:
            case FIELD:
                sb.append(unslashify(dd.typeName))
                  .append('.')
                  .append(dd.nameSig);
                break;
            case CONSTRUCTOR:
                Matcher cons = DESC_PAT.matcher(dd.nameSig);
                sb.append(unslashify(dd.typeName));
                if (cons.matches()) {
                    sb.append('(')
                      .append(parms(cons.group("args")))
                      .append(')');
                } else {
                    sb.append('.')
                      .append(dd.nameSig);
                }
                break;
            case METHOD:
                Matcher meth = DESC_PAT.matcher(dd.nameSig);
                if (meth.matches()) {
                    sb.append(desc(meth.group("return")))
                      .append(' ')
                      .append(unslashify(dd.typeName))
                      .append('.')
                      .append(meth.group("name"))
                      .append('(')
                      .append(parms(meth.group("args")))
                      .append(')');
                } else {
                    sb.append(unslashify(dd.typeName))
                      .append('.')
                      .append(dd.nameSig);
                }
                break;
        }

        return sb.toString();
    }
}
