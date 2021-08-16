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

package sun.jvmstat.perfdata.monitor.v1_0;

/**
 * A typesafe enumeration for describing Java basic types.
 *
 * <p> The enumeration values for this typesafe enumeration must be
 * kept in synchronization with the BasicType enum in the
 * globalsDefinitions.hpp file in the HotSpot source base.</p>
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class BasicType {

    private final String name;
    private final int value;

    public static final BasicType BOOLEAN = new BasicType("boolean",  4);
    public static final BasicType CHAR    = new BasicType("char",     5);
    public static final BasicType FLOAT   = new BasicType("float",    6);
    public static final BasicType DOUBLE  = new BasicType("double",   7);
    public static final BasicType BYTE    = new BasicType("byte",     8);
    public static final BasicType SHORT   = new BasicType("short",    9);
    public static final BasicType INT     = new BasicType("int",     10);
    public static final BasicType LONG    = new BasicType("long",    11);
    public static final BasicType OBJECT  = new BasicType("object",  12);
    public static final BasicType ARRAY   = new BasicType("array",   13);
    public static final BasicType VOID    = new BasicType("void",    14);
    public static final BasicType ADDRESS = new BasicType("address", 15);
    public static final BasicType ILLEGAL = new BasicType("illegal", 99);

    private static BasicType basicTypes[] = {
        BOOLEAN, CHAR, FLOAT, DOUBLE, BYTE, SHORT, INT, LONG, OBJECT,
        ARRAY, VOID, ADDRESS, ILLEGAL
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
     * Convert enumeration to an integer value.
     *
     * @return int - the integer representation for the enumeration.
     */
    public int intValue() {
        return value;
    }

    /**
     * Map an integer value to its corresponding BasicType object.
     *
     * @param i an integer representation of a BasicType
     * @return BasicType - The BasicType enumeration object matching
     *                     the value of <code>i</code>
     */
    public static BasicType toBasicType(int i) {
        for (int j = 0; j < basicTypes.length; j++) {
            if (basicTypes[j].intValue() == j) {
                return (basicTypes[j]);
            }
        }
        return ILLEGAL;
    }

    private BasicType(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
