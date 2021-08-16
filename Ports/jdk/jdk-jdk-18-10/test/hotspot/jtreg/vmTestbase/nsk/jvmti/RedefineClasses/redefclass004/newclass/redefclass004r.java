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
 * This is new version of the redefined class
 */
public class redefclass004r {
    public byte byteFld = 11;
    public short shortFld = 22;
    public int intFld = 33;
    public long longFld = 44L;
    public float floatFld = 55.5F;
    public double doubleFld = 66.6D;
    public char charFld = 'c';
    public boolean booleanFld = true;
    public String stringFld = "NEW redefclass004r";
// completely new instance fields are below
    public int intComplNewFld = 333;
    public long longComplNewFld = 444L;
    public String stringComplNewFld = "completely new String field";

    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        if (DEBUG_MODE)
            out.println("NEW redefclass004r: inside the checkIt()");
        return 73;
    }
}
