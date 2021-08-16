/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.ReferenceType.GetValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

public class getvalues001a {

    public static void main(String args[]) {
        getvalues001a _getvalues001a = new getvalues001a();
        System.exit(getvalues001.JCK_STATUS_BASE + _getvalues001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        // meke communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // ensure tested class loaded
        log.display("Creating object of tested class");
        TestedClass foo = new TestedClass();

        // send debugger signal READY
        log.display("Sending signal to debugger: " + getvalues001.READY);
        pipe.println(getvalues001.READY);

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + getvalues001.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (! signal.equals(getvalues001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + getvalues001.QUIT + ")");
            log.display("Debugee FAILED");
            return getvalues001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return getvalues001.PASSED;
    }

    // tested class with own static fields values
    public static class TestedClass {
        private   static boolean       booleanValue = true;
        private   static final byte    byteValue    = (byte)0x0F;
        protected static char          charValue    = 'Z';
        protected static final int     intValue     = 100;
        public    static short         shortValue   = (short)10;
        public    static final long    longValue    = (long)1000000;
                  static float         floatValue   = (float)3.14;
                  static final double  doubleValue  = (double)2.8e-12;
                  static TestedClass   objectValue  = null;
    }
}
