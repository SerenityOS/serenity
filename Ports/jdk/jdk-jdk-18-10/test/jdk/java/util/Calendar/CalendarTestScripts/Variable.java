/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Variable {
    private static Map<String,Variable> vars = new HashMap<String,Variable>();

    private String name;
    private long value;

    Variable(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int intValue() {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new RuntimeException("Overflow/Underflow: " + value);
        }
        return (int) value;
    }

    public long longValue() {
        return value;
    }

    public void setValue(int v) { value = v; }
    public void setValue(long v) { value = v; }

    // static methods

    public static Variable newVar(String name, long value) {
        if (name.charAt(0) != '$') {
            throw new RuntimeException("wrong var name: " + name);
        }
        String s = name.toLowerCase(Locale.ROOT);
        Variable v = new Variable(name, value);
        put(name, v);
        return v;
    }

    static void put(String s, Variable var) {
        vars.put(s, var);
    }

    public static Variable get(String name) {
        return vars.get(name);
    }
}
