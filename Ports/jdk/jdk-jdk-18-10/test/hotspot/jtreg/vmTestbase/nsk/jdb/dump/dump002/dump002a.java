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

package nsk.jdb.dump.dump002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class dump002a {
    static dump002a _dump002a = new dump002a();

    public static void main(String args[]) {
       System.exit(dump002.JCK_STATUS_BASE + _dump002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        lastBreak();

        log.display("Debuggee PASSED");
        return dump002.PASSED;
    }

    static     int iStatic    = 0;
    private    int iPrivate   = 1;
    protected  int iProtect   = 2;
    public     int iPublic    = 3;
    final      int iFinal     = 4;
    transient  int iTransient = 5;
    volatile   int iVolatile  = 6;

    static     int [] iArray = { 7 };

    static     String sStatic    = "zero";
    private    String sPrivate   = "one";
    protected  String sProtected = "two";
    public     String sPublic    = "three";
    final      String sFinal     = "four";
    transient  String sTransient = "five";
    volatile   String sVolatile  = "six";

    static     String [] sArray = { "seven" };

    boolean fBoolean = true;
    byte    fByte    = Byte.MAX_VALUE;
    char    fChar    = Character.MAX_VALUE;
    double  fDouble  = Double.MAX_VALUE;
    float   fFloat   = Float.MAX_VALUE;
    int     fInt     = Integer.MAX_VALUE;
    long    fLong    = Long.MAX_VALUE;
    short   fShort   = Short.MAX_VALUE;
}
