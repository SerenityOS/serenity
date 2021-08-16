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

package sun.tools.jstat;

import java.util.*;


/**
 * A typesafe enumeration for describing mathematical operators.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public abstract class Operator {

    private static int nextOrdinal = 0;
    private static HashMap<String, Operator> map = new HashMap<String, Operator>();

    private final String name;
    private final int ordinal = nextOrdinal++;

    private Operator(String name) {
        this.name = name;
        map.put(name, this);
    }

    protected abstract double eval(double x, double y);

    /* Operator '+' */
    public static final Operator PLUS = new Operator("+") {
        protected double eval(double x, double y) {
            return x + y;
        }
    };

    /* Operator '-' */
    public static final Operator MINUS = new Operator("-") {
        protected double eval(double x, double y) {
            return x - y;
        }
    };

    /* Operator '/' */
    public static final Operator DIVIDE = new Operator("/") {
        protected double eval(double x, double y) {
            if (y == 0) {
                return Double.NaN;
            }
            return x / y;
        }
    };

    /* Operator '*' */
    public static final Operator MULTIPLY = new Operator("*") {
        protected double eval(double x, double y) {
            return x * y;
        }
    };

    /**
     * Returns the string representation of this Operator object.
     *
     * @return  the string representation of this Operator object
     */
    public String toString() {
        return name;
    }

    /**
     * Maps a string to its corresponding Operator object.
     *
     * @param   s  an string to match against Operator objects.
     * @return     The Operator object matching the given string.
     */
    public static Operator toOperator(String s) {
        return map.get(s);
    }

    /**
     * Returns an enumeration of the keys for this enumerated type
     *
     * @param   s  an string to match against Operator objects.
     * @return     The Operator object matching the given string.
     */
    protected static Set<?> keySet() {
        return map.keySet();
    }
}
