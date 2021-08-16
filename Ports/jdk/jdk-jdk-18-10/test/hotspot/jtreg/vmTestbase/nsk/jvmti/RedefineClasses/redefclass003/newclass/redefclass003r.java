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

public class redefclass003r {
    static public byte byteFld = 10;
    static public short shortFld = 20;
    static public int intFld = 30;
    static public long longFld = 40L;
    static public float floatFld = 50.2F;
    static public double doubleFld = 60.3D;
    static public char charFld = 'b';
    static public boolean booleanFld = true;
    static public String stringFld = "NEW redefclass003r";
// completely new static variables are below
    static public byte byteComplNewFld = 11;
    static public short shortComplNewFld = 22;
    static public int intComplNewFld = 33;
    static public long longComplNewFld = 44L;
    static public float floatComplNewFld = 55.5F;
    static public double doubleComplNewFld = 66.6D;
    static public char charComplNewFld = 'c';
    static public boolean booleanComplNewFld = false;
    static public String stringComplNewFld = "completely new field";

    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        if (DEBUG_MODE)
            out.println("NEW redefclass003r: inside the checkIt()");
        return 73;
    }
}
