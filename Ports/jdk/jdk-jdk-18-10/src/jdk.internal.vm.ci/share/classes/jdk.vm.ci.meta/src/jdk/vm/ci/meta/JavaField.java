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
 * Represents a reference to a Java field, either resolved or unresolved fields. Fields, like
 * methods and types, are resolved through {@link ConstantPool constant pools}.
 */
public interface JavaField {

    /**
     * Returns the name of this field.
     */
    String getName();

    /**
     * Returns a {@link JavaType} object that identifies the declared type for this field.
     */
    JavaType getType();

    /**
     * Returns the kind of this field. This is the same as calling {@link #getType}.
     * {@link JavaType#getJavaKind getJavaKind}.
     */
    default JavaKind getJavaKind() {
        return getType().getJavaKind();
    }

    /**
     * Returns the {@link JavaType} object representing the class or interface that declares this
     * field.
     */
    JavaType getDeclaringClass();

    /**
     * Gets a string for this field formatted according to a given format specification. A format
     * specification is composed of characters that are to be copied verbatim to the result and
     * specifiers that denote an attribute of this field that is to be copied to the result. A
     * specifier is a single character preceded by a '%' character. The accepted specifiers and the
     * field attributes they denote are described below:
     *
     * <pre>
     *     Specifier | Description                                          | Example(s)
     *     ----------+------------------------------------------------------------------------------------------
     *     'T'       | Qualified type                                       | "int" "java.lang.String"
     *     't'       | Unqualified type                                     | "int" "String"
     *     'H'       | Qualified holder                                     | "java.util.Map.Entry"
     *     'h'       | Unqualified holder                                   | "Entry"
     *     'n'       | Field name                                           | "age"
     *     'f'       | Indicator if field is unresolved, static or instance | "unresolved" "static" "instance"
     *     '%'       | A '%' character                                      | "%"
     * </pre>
     *
     * @param format a format specification
     * @return the result of formatting this field according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    default String format(String format) throws IllegalFormatException {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        JavaType type = getType();
        while (index < format.length()) {
            char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new UnknownFormatConversionException("An unquoted '%' character cannot terminate a field format specification");
                }
                char specifier = format.charAt(index++);
                switch (specifier) {
                    case 'T':
                    case 't': {
                        sb.append(type.toJavaName(specifier == 'T'));
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
                    case 'f': {
                        sb.append(!(this instanceof ResolvedJavaField) ? "unresolved" : ((ResolvedJavaField) this).isStatic() ? "static" : "instance");
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
