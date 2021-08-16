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

package nsk.jdb.pop.pop001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class pop001a {

    static final String MYTHREAD  = "MyThread";

    static JdbArgumentHandler argumentHandler;
    static Log log;

    static pop001a _pop001a = new pop001a();

    public static void main(String args[]) {
       System.exit(pop001.JCK_STATUS_BASE + _pop001a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        MyThread myThread = new MyThread(MYTHREAD);
        myThread.start();

        if (myThread.isAlive()) {
            try {
                myThread.join();
            } catch (InterruptedException e) {
                log.complain("Main thread was interrupted while waiting for finish of " + MYTHREAD);
                return pop001.FAILED;
            }
        }

        log.display("Debuggee PASSED");
        return pop001.PASSED;
    }
}

class MyThread extends Thread {

    public MyThread (String name) {
        super(name);
    }

    public void run() {
        pop001a.log.display("run() started");
        int runLocal;
        int j = func1(0);
        pop001a.log.display("run() finished");
    }

    public int func1(int intParam) {
        pop001a.log.display("func1() started");
        int func1Local;
        intParam++;
        return func2(intParam);
    }

    public int func2(int intParam) {
        pop001a.log.display("func2() started");
        int func2Local;
        intParam++;
        return func3(intParam);
    }

    public int func3(int intParam) {
        pop001a.log.display("func3() started");
        int func3Local;
        intParam++;
        return func4(intParam);
    }

    public int func4(int intParam) {
        pop001a.log.display("func4() started");
        int func4Local;
        intParam++;
        return func5(intParam);
    }

    public int func5(int intParam) {
        pop001a.log.display("func5() started");
        int func5Local;
        intParam++;
        pop001a.lastBreak();
        return intParam;
    }
}
