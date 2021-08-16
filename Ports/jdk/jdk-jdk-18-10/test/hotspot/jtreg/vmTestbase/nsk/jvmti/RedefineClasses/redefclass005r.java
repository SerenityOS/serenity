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

public class redefclass005r {
/* final fields */
    static final int intStatFinFld = 10;
    static final long longStatFinFld = 200L;
/* public static fields */
    static public byte bytePubStatFld = 1;
    static public short shortPubStatFld = 2;
    static public int intPubStatFld = 3;
    static public long longPubStatFld = 4L;
    static public float floatPubStatFld = 5.1F;
    static public double doublePubStatFld = 6.2D;
    static public char charPubStatFld = 'a';
    static public boolean booleanPubStatFld = false;
    static public String strPubStatFld = "static field";
/* instance fields */
    byte byteFld = 1;
    short shortFld = 2;
    int intFld = 3;
    long longFld = 4L;
    float floatFld = 5.1F;
    double doubleFld = 6.2D;
    char charFld = 'a';
    boolean booleanFld = false;
    String strFld = "instance field";
/* public instance fields */
    public byte bytePubFld = 1;
    public short shortPubFld = 2;
    public int intPubFld = 3;
    public long longPubFld = 4L;
    public float floatPubFld = 5.1F;
    public double doublePubFld = 6.2D;
    public char charPubFld = 'a';
    public boolean booleanPubFld = false;
    public String strPubFld = "public instance field";

    public redefclass005r() {
        this.intFld = -2;
    }

    public redefclass005r(int par) {
        this.intFld = par;
    }

    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        if (DEBUG_MODE)
            out.println("redefclass005r: inside the checkIt()");
        return 19;
    }

    public static int staticMethod(int j) {
        int i = 1 + j;
        return i;
    }
}
