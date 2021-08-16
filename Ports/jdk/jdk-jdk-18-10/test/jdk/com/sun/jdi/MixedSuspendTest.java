/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6224859
 * @summary JDWP: Mixing application suspends and debugger suspends can cause hangs
 * @comment converted from test/jdk/com/sun/jdi/MixedSuspendTest.sh
 *
 * @library /test/lib
 * @run main/othervm MixedSuspendTest
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class MixedSuspendTarg extends Thread {

    static volatile boolean started = true;
    static String lock = "startLock";

    public static void main(String[] args){
        System.out.println("Howdy from MixedSuspendTarg");

        MixedSuspendTarg mytarg = new MixedSuspendTarg();

        synchronized(lock) {
            mytarg.start();
            try {
                lock.wait();
            } catch(InterruptedException ee) {
            }
        }
        mytarg.suspend();
        bkpt();
        System.out.println("Debuggee: resuming thread");

        // If the bug occurs, this resume hangs in the back-end
        mytarg.resume();
        System.out.println("Debuggee: resumed thread");
        synchronized(lock) {
            started = false;
        }
        System.out.println("Debuggee: exitting, started = " + started);
    }

    public void run() {
        synchronized(lock) {
            lock.notifyAll();
        }
        while (true) {
            synchronized(lock) {
                if (!started) {
                    break;
                }
                int i = 0;
            }
        }

        System.out.println("Debuggee: end of thread");
    }

    static void bkpt() {
        //System.out.println("bkpt reached, thread = " + this.getName());
        int i = 0;   // @1 breakpoint
    }
}

public class MixedSuspendTest extends JdbTest {
    public static void main(String argv[]) {
        new MixedSuspendTest().run();
    }

    private MixedSuspendTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = MixedSuspendTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("MixedSuspendTest.java", 1);
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.cont().allowExit());

        // This test fails by timing out.
    }
}
