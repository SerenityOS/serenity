/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

import java.util.IllegalFormatException;
import java.util.UnknownFormatConversionException;

/**
 * Represents a reference to a Java method, either resolved or unresolved. Methods, like fields and
 * types, are resolved through {@link ConstantPool constant pools}.
 */
public interface JavaMethod {

    /**
     * Returns the name of this method.
     */
    String getName();

    /**
     * Returns the {@link JavaType} object representing the class or interface that declares this
     * method.
     */
    JavaType getDeclaringClass();

    /**
     * Returns the signature of this method.
     */
    Signature getSignature();

    /**
     * Gets a string for this method formatted according to a given format specification. A format
     * specification is composed of characters that are to be copied verbatim to the result and
     * specifiers that denote an attribute of this method that is to be copied to the result. A
     * specifier is a single character preceded by a '%' character. The accepted specifiers and the
     * method attributes they denote are described below:
     *
     * <pre>
     *     Specifier | Description                                          | Example(s)
     *     ----------+------------------------------------------------------------------------------------------
     *     'R'       | Qualified return type                                | "int" "java.lang.String"
     *     'r'       | Unqualified return type                              | "int" "String"
     *     'H'       | Qualified holder                                     | "java.util.Map.Entry"
     *     'h'       | Unqualified holder                                   | "Entry"
     *     'n'       | Method name                                          | "add"
     *     'P'       | Qualified parameter types, separated by ', '         | "int, java.lang.String"
     *     'p'       | Unqualified parameter types, separated by ', '       | "int, String"
     *     'f'       | Indicator if method is unresolved, static or virtual | "unresolved" "static" "virtual"
     *     '%'       | A '%' character                                      | "%"
     * </pre>
     *
     * @param format a format specification
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    default String format(String format) throws IllegalFormatException {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        Signature sig = null;
        while (index < format.length()) {
            char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new UnknownFormatConversionException("An unquoted '%' character cannot terminate a method format specification");
                }
                char specifier = format.charAt(index++);
                switch (specifier) {
                    case 'R':
                    case 'r': {
                        if (sig == null) {
                            sig = getSignature();
                        }
                        sb.append(sig.getReturnType(null).toJavaName(specifier == 'R'));
                        break;
                    }
                    case 'H':
                    case 'h': {
                        sb.append(getDeclaringClass().toJavaName(specifier == 'H'));
                        break;
                    }
                    case 'n': {
                        sb.append(getName());
                        break;
                    }
                    case 'P':
                    case 'p': {
                        if (sig == null) {
                            sig = getSignature();
                        }
                        for (int i = 0; i < sig.getParameterCount(false); i++) {
                            if (i != 0) {
                                sb.append(", ");
                            }
                            sb.append(sig.getParameterType(i, null).toJavaName(specifier == 'P'));
                        }
                        break;
                    }
                    case 'f': {
                        sb.append(!(this instanceof ResolvedJavaMethod) ? "unresolved" : ((ResolvedJavaMethod) this).isStatic() ? "static" : "virtual");
                        break;
                    }
                    case '%': {
                        sb.append('%');
                        break;
                    }
                    default: {
                        throw new UnknownFormatConversionException(String.valueOf(specifier));
                    }
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
