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

package com.sun.tools.jdeprscan.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method's signature, that is, its parameter types
 * and its return type.
 */
public class MethodSig {
    final List<String> parameters;
    final String returnType;

    /**
     * Parses the method descriptor and returns a MethodSig instance.
     *
     * @param desc the descriptor to parse
     * @return the new MethodSig instance
     */
    public static MethodSig fromDesc(String desc) {
        return parse(desc, 0, desc.length());
    }

    /**
     * Returns this method's return type.
     *
     * @return the return type
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Returns a list of parameters of this method.
     *
     * @return the parameter list
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Returns a string describing this method.
     *
     * @return the string description
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("parameters");
        if (parameters.isEmpty()) {
            sb.append(" none");
        } else {
            int i = 0;
            for (String p : parameters) {
                sb.append(String.format(" %d=%s", i++, p));
            }
        }
        sb.append(String.format(" return %s", returnType));
        return sb.toString();
    }

    private MethodSig(List<String> parameters, String returnType) {
        this.parameters = Collections.unmodifiableList(parameters);
        this.returnType = returnType;
    }

    private static IllegalArgumentException ex(String desc, int pos) {
        return new IllegalArgumentException(String.format(
            "illegal descriptor \"%s\" at position %d", desc, pos));
    }

    private static MethodSig parse(String desc, int start, int end)
            throws IllegalArgumentException {
        int p = start;
        int dims = 0;
        boolean inReturnType = false;
        String returnType = null;
        List<String> parameters = new ArrayList<>();

        while (p < end) {
            String type;
            char ch;
            switch (ch = desc.charAt(p)) {
                case '(':
                    p++;
                    continue;

                case ')':
                    p++;
                    inReturnType = true;
                    continue;

                case '[':
                    p++;
                    dims++;
                    continue;

                case 'B': // byte
                case 'C': // char
                case 'D': // double
                case 'F': // float
                case 'I': // int
                case 'J': // long
                case 'S': // short
                case 'Z': // boolean
                case 'V': // void
                    type = Character.toString(ch);
                    p++;
                    break;

                case 'L':
                    int sep = desc.indexOf(';', p);
                    if (sep == -1 || sep >= end)
                        throw ex(desc, p);
                    type = desc.substring(p, ++sep);
                    p = sep;
                    break;

                default:
                    throw ex(desc, p);
            }

            StringBuilder sb = new StringBuilder();
            for ( ; dims > 0; dims-- )
                sb.append("[");
            sb.append(type);
            if (inReturnType) {
                returnType = sb.toString();
            } else {
                parameters.add(sb.toString());
            }
        }

        if (returnType == null) {
            throw ex(desc, end);
        }

        return new MethodSig(parameters, returnType);
    }
}
