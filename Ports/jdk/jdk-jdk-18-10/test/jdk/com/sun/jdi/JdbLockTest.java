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

/*
 * @test
 * @bug 4847812
 * @summary TTY: jdb lock command displays incorrect data
 * @comment converted from test/jdk/com/sun/jdi/JdbLockTest.sh
 *
 * @library /test/lib
 * @compile -g JdbLockTest.java
 * @run main/othervm JdbLockTest
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class JdbLockTestTarg {
    static String jj = "jj";
    public static void main(String args[]) {
        synchronized(jj) {
            sleeper xx = new sleeper();
            xx.start();
            // Give the sleeper a chance to run and get to
            // the synchronized statement.
            while(sleeper.started == 0) {
                try {
                    Thread.sleep(100);
                } catch(InterruptedException ee) {
                }
            }
            // At this bkpt, sleeper should be waiting on $classname.jj
            System.out.println("Hello sailor");    // @1 breakpoint
        }
    }
}

class sleeper extends Thread {
    public static int started = 0;
    public void run() {
        started = 1;
        System.out.println("     sleeper starts sleeping");
        synchronized(JdbLockTestTarg.jj) {
            System.out.println("     sleeper got the lock");
        }
        System.out.println("     sleeper awakes");
    }
}

public class JdbLockTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbLockTest().run();
    }

    private JdbLockTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbLockTestTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("JdbLockTest.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        // This should say that main owns the lock
        // and the sleeper thread is waiting for it.
        execCommand(JdbCommand.lock("JdbLockTestTarg.jj"))
                .shouldNotContain("Waiting thread: main");
    }
}
