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

package nsk.jdb.where.where005;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class where005a {
    public static void main(String args[]) {
       where005a _where005a = new where005a();
       lastBreak();
       System.exit(where005.JCK_STATUS_BASE + _where005a.runIt(args, System.out)); // where005.FRAMES[7]
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        int i = func1(10); // where005.FRAMES[6]

        log.display("Debuggee PASSED");
        return where005.PASSED;
    }

    public int func1(int i) {
        return func2(i) + 1; // where005.FRAMES[5]
    }

    public int func2(int i) {
        return func3(i) + 1; // where005.FRAMES[4]
    }

    public int func3(int i) {
        return func4(i) + 1; // where005.FRAMES[3]
    }

    public int func4(int i) {
       return func5(i) + 1; // where005.FRAMES[2]
    }

    public int func5(int i) {
       return func6(i) + 1; // where005.FRAMES[1]
    }

    public int func6(int i) {
        String s = null;
        s.concat("null");  // throws NullPointerException // where005.FRAMES[0]
        return i-5;
    }
}
