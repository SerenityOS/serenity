/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.pop_exception.pop_exception001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * debugee application
 */
public class pop_exception001a {
    public static void main(String args[]) {
       pop_exception001a _pop_exception001a = new pop_exception001a();
       System.exit(Consts.JCK_STATUS_BASE + _pop_exception001a.runIt(args, System.out));
    }

    static JdbArgumentHandler argumentHandler;
    static Log log;

    public int runIt(String args[], PrintStream out) {
        int i = 0;
        Object item = null;
        try {
            item = new Object();
            i = 5; // expectedFinish
            item = getItem(i);
        } finally {
            System.out.println("item = "+item);
        }
        return Consts.TEST_PASSED;
    }

    private Object getItem(int i) {
        if (i == 5) {
            throw new NullPointerException("Something went wrong");
        } else {
            return Integer.valueOf(i);
        }
    }

    public final static String expectedFinish = "51";
}
