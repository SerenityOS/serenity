/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.RedefineClasses;

import java.io.PrintStream;

/**
 * This is the old version of a redefined class.
 * Note that in accordance with 2.12 Constructors from the JVM spec, 2nd ed.
 * default constructor must be automatically provided:
 * <p>
 * "If a class declares no constructors then a <i>default constructor</i>, which
 * takes no arguments, is automatically provided. If the class being declared
 * is Object, then the default constructor has an empty body. ... If the class
 * is declared public, then the default constructor is implicitly given the
 * access modifier public."
 */
public class redefclass009r {
    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        int i = 234; // dummy local var

        if (DEBUG_MODE)
            out.println("OLD redefclass009r: inside the checkIt()");
        return 19;
    }

// dummy methods are below
    static double statMethod(int x, int y, int z) {
        return 19.73D;
    }

    final void finMethod(char c, long i, int j, long k) {}
}
