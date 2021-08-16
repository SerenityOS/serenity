/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jdb.locals.locals002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class locals002a {
    static locals002a _locals002a = new locals002a();

    public static void main(String args[]) {
       System.exit(locals002.JCK_STATUS_BASE + _locals002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        int arr[] = new int[3];

        for (int i = 0 ; i < 3 ; i++) arr[i] = i*i;

        allKindsOfVars (
            false,
            (byte)12,
            'A',
            (short)327,
            3647,
            (long)65789,
            (float)4.852,
            (double)3.8976,
            "objArgString",
            arr
                       );

        allKindsOfLocals();

        log.display("Debuggee PASSED");
        return locals002.PASSED;
    }

   public void allKindsOfVars (
       boolean boolVar,
       byte    byteVar,
       char    charVar,
       short   shortVar,
       int     intVar,
       long    longVar,
       float   floatVar,
       double  doubleVar,
       Object  objVar,
       int[]   arrVar
                              )
   {
       int x = 3; // locals002.BREAKPOINT_LINE1
   }

   static void allKindsOfLocals()  {
       boolean boolVar   = true;
       byte    byteVar   = 27;
       char    charVar   = 'V';
       short   shortVar  = (short)767;
       int     intVar    = 1474;
       long    longVar   = (long)21345;
       float   floatVar  = (float)3.141;
       double  doubleVar = (double)2.578;
       Object  objVar    = "objVarString";
       int[]   arrVar    = new int[5];

       for (int j = 0; j < 5 ; j++) arrVar[j] = j;
       int x = 4; // locals002.BREAKPOINT_LINE2
   }
}
