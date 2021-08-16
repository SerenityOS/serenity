/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.Iterator;

/**
 * Helper to create and manipulate type descriptors, where type descriptors
 * are represented as JVM type descriptor strings and symbols are represented
 * as name strings
 */
public class BasicTypeHelper implements TypeHelper<String, String> {

    @Override
    public String elemtype(String s) {
        if (!s.startsWith("[")) {
            throw new IllegalStateException();
        }
        return s.substring(1);
    }

    @Override
    public String arrayOf(String s) {
        return "[" + s;
    }

    @Override
    public String type(String s) {
        return "L" + s + ";";
    }

    @Override
    public TypeTag tag(String s) {
        switch (s.charAt(0)) {
            case '[':
            case 'L':
                return TypeTag.A;
            case 'B':
            case 'C':
            case 'Z':
            case 'S':
            case 'I':
                return TypeTag.I;
            case 'F':
                return TypeTag.F;
            case 'J':
                return TypeTag.J;
            case 'D':
                return TypeTag.D;
            case 'V':
                return TypeTag.V;
            case 'Q':
                return TypeTag.Q;
            default:
                throw new IllegalStateException("Bad type: " + s);
        }
    }

    @Override
    public String nullType() {
        // Needed in TypedCodeBuilder; ACONST_NULL pushes a 'null' onto the stack,
        // and stack maps handle null differently
        return "<null>";
    }

    @Override
    public String commonSupertype(String t1, String t2) {
        if (t1.equals(t2)) {
            return t1;
        } else {
            try {
                Class<?> c1 = from(t1);
                Class<?> c2 = from(t2);
                if (c1.isAssignableFrom(c2)) {
                    return t1;
                } else if (c2.isAssignableFrom(c1)) {
                    return t2;
                } else {
                    return "Ljava/lang/Object;";
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

    public Class<?> from(String desc) throws ReflectiveOperationException {
        if (desc.startsWith("[")) {
            return Class.forName(desc.replaceAll("/", "."));
        } else {
            return Class.forName(symbol(desc).replaceAll("/", "."));
        }
    }

    @Override
    public Iterator<String> parameterTypes(String s) {
        //TODO: gracefully non-method types
        return new Iterator<String>() {
            int ch = 1;

            @Override
            public boolean hasNext() {
                return s.charAt(ch) != ')';
            }

            @Override
            public String next() {
                char curr = s.charAt(ch);
                switch (curr) {
                    case 'C':
                    case 'B':
                    case 'S':
                    case 'I':
                    case 'J':
                    case 'F':
                    case 'D':
                    case 'Z':
                        ch++;
                        return String.valueOf(curr);
                    case '[':
                        ch++;
                        return "[" + next();
                    case 'L':
                    case 'Q':
                        StringBuilder builder = new StringBuilder();
                        while (curr != ';') {
                            builder.append(curr);
                            curr = s.charAt(++ch);
                        }
                        builder.append(';');
                        ch++;
                        return builder.toString();
                    default:
                        throw new AssertionError("cannot parse string: " + s);
                }
            }
        };
    }

    @Override
    public String symbolFrom(String s) {
        return s;
    }

    @Override
    public String fromTag(TypeTag tag) {
        return tag.name();
    }

    @Override
    public String symbol(String type) {
        return (type.startsWith("L") || type.startsWith("Q")) ? type.substring(1, type.length() - 1) : type;
    }

    @Override
    public String returnType(String s) {
        return s.substring(s.indexOf(')') + 1, s.length());
    }

}
