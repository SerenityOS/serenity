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

/**
 * @test
 * @bug 4513488
 * @summary Test for JDI: Internal JDI helper threads should setDaemon(true)
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g DebuggerThreadTest.java
 * @run driver DebuggerThreadTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class DebuggerThreadTarg {
    public void ready() {
    }
    public static void main(String[] args){
        System.out.println("Howdy!");
        DebuggerThreadTarg targ = new DebuggerThreadTarg();
        targ.ready();
        System.out.println("Goodbye from DebuggerThreadTarg!");
    }
}

    /********** test program **********/

public class DebuggerThreadTest extends TestScaffold {

    DebuggerThreadTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new DebuggerThreadTest(args).startTests();
    }

    /*
     * Move to top ThreadGroup and dump all threads.
     */
    public void dumpThreads() {
        int finishedThreads = 0;
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = tg.getParent();
        while (parent != null) {
            tg = parent;
            parent = tg.getParent();
        }
        int listThreads = tg.activeCount();
        Thread list[] = new Thread[listThreads * 2];
        int gotThreads = tg.enumerate(list, true);
        for (int i = 0; i < Math.min(gotThreads, list.length); i++){
            Thread t = list[i];
            ThreadGroup tga = t.getThreadGroup();
            String groupName;
            if (tga == null) {
                groupName = "<completed>";
                finishedThreads++ ;
            } else {
                groupName = tga.getName();
            }

            System.out.println("Thread [" + i + "] group = '" +
                               groupName +
                               "' name = '" + t.getName() +
                               "' daemon = " + t.isDaemon());

            if (groupName.startsWith("JDI ") &&
                (! t.isDaemon())) {
                failure("FAIL: non-daemon thread '" + t.getName() +
                        "' found in ThreadGroup '" + groupName + "'");
            }
        }
        if (finishedThreads > 0 ) {
            failure("FAIL: " + finishedThreads +
                    " threads completed while VM suspended.");
        }
    }

    protected void runTests() throws Exception {
        try {
            /*
             * Get to the top of ready()
             */
            startTo("DebuggerThreadTarg", "ready", "()V");

            dumpThreads();

            listenUntilVMDisconnect();

        } finally {
            /*
             * deal with results of test
             * if anything has called failure("foo") testFailed will be true
             */
            if (!testFailed) {
                println("DebuggerThreadTest: passed");
            } else {
                throw new Exception("DebuggerThreadTest: failed");
            }
        }
        return;
    }
}
