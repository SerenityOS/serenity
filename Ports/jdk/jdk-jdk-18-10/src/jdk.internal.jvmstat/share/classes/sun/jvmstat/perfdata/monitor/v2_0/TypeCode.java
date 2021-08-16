/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor.v2_0;

/**
 * A typesafe enumeration for describing standard Java type codes.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class TypeCode {

    private final String name;
    private final char value;

    public static final TypeCode BOOLEAN = new TypeCode("boolean", 'Z');
    public static final TypeCode CHAR    = new TypeCode("char",    'C');
    public static final TypeCode FLOAT   = new TypeCode("float",   'F');
    public static final TypeCode DOUBLE  = new TypeCode("double",  'D');
    public static final TypeCode BYTE    = new TypeCode("byte",    'B');
    public static final TypeCode SHORT   = new TypeCode("short",   'S');
    public static final TypeCode INT     = new TypeCode("int",     'I');
    public static final TypeCode LONG    = new TypeCode("long",    'J');
    public static final TypeCode OBJECT  = new TypeCode("object",  'L');
    public static final TypeCode ARRAY   = new TypeCode("array",   '[');
    public static final TypeCode VOID    = new TypeCode("void",    'V');

    private static TypeCode basicTypes[] = {
        LONG, BYTE, BOOLEAN, CHAR, FLOAT, DOUBLE,
        SHORT, INT, OBJECT, ARRAY, VOID
    };

    /**
     * Convert enumeration value to a String.
     *
     * @return String - the string representation for the enumeration.
     */
    public String toString() {
        return name;
    }

    /**
     * Convert enumeration to its character representation.
     *
     * @return int - the integer representation for the enumeration.
     */
    public int toChar() {
        return value;
    }

    /**
     * Map a character value to its corresponding TypeCode object.
     *
     * @param c an character representing a Java TypeCode
     * @return TypeCode - The TypeCode enumeration object for the given
     *                    character.
     * @throws IllegalArgumentException Thrown if <code>c</code> is not
     *                                  a valid Java TypeCode.
     */
    public static TypeCode toTypeCode(char c) {
        for (int j = 0; j < basicTypes.length; j++) {
            if (basicTypes[j].value == c) {
                return (basicTypes[j]);
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Map a character value to its corresponding TypeCode object.
     *
     * @param b a byte representing a Java TypeCode. This value is
     *          converted into a char and used to find the corresponding
     *          TypeCode.
     * @return TypeCode - The TypeCode enumeration object for the given byte.
     * @throws IllegalArgumentException Thrown if <code>v</code> is not
     *                                  a valid Java TypeCode.
     */
    public static TypeCode toTypeCode(byte b) {
        return toTypeCode((char)b);
    }

    private TypeCode(String name, char value) {
        this.name = name;
        this.value = value;
    }
}
