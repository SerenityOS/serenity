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

package nsk.jdb.ignore.ignore001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class ignore001a {

    /* TEST DEPENDANT VARIABLES AND CONSTANTS */
    static final String PACKAGE_NAME = "nsk.jdb.ignore.ignore001";
    static final String JAVA_EXCEPTION = "java.lang.NumberFormatException";
    static final String USER_EXCEPTION1 = PACKAGE_NAME + ".ignore001a$Exception1";
    static final String USER_EXCEPTION2 = PACKAGE_NAME + ".Exception2";

    static JdbArgumentHandler argumentHandler;
    static Log log;

    public static void main(String args[]) {
       ignore001a _ignore001a = new ignore001a();
       System.exit(ignore001.JCK_STATUS_BASE + _ignore001a.runIt(args, System.out));
    }

//    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        for (int i = 0; i < 6; i++) {
            a(i);
        }

        log.display("Debuggee PASSED");
        return ignore001.PASSED;
    }

    private void a (int i) {
        try {
            switch (i) {
            case 0: case 3:
                log.display("Throwing NumberFormatException, i = " + i);
                throw new java.lang.NumberFormatException();
            case 1: case 4:
                log.display("Throwing Exception1, i = " + i);
                throw new Exception1();
            case 2: case 5:
                log.display("Throwing Exception2, i = " + i);
                throw new Exception2();
            }
        } catch (java.lang.NumberFormatException e0) {
        } catch (Exception1 e1) {
        } catch (Exception2 e2) {
        }
//        lastBreak();
    }

    class Exception1 extends Exception {}
}

class Exception2 extends Exception {}
