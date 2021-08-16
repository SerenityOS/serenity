/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.scenarios.hotswap.HS202.hs202t002;
public class MyThread extends Thread {
    private int val = 100;

    public void run() {
        playWithThis();
    }

    public void playWithThis() {
        try {
            display();
        } catch (Exception ex) {
        }
    }

    private synchronized void display() throws Exception {
        // increment val by 1 in the base version
        // of display(). Redefine classes will change this method to
        // increment val by 10.
        val++;
        // the upcoming athrow will fire off jvmti_post_exception_events as well as
        // jvmti_post_method_exit_events (will be issued by this thread).
        // In the jvmti_post_method_exit callback for this test,
        // a redefineClasses call will be made to redefine MyThread, effectively changing
        // this method, display() .
        // After the redefineClasses call completes, this thread will proceed
        // with suspending itself. At time of suspension, display() will still be at the top of the stack.
        // The main thread waits for the thread to suspend itself, it will then
        // give instructions to PopFrame the display() method and will also
        // resume the suspended thread (note also that the main thread disables
        // the notifications for jvmti_method_exit events in order to avoid recursion on PopFrame reinvoke).
        // Due to the PopFrame instruction, the method exit unwind will not happen,
        // instead, PopFrame will unlock the monitor associated with display(), as well
        // as discard the unwinding exception. The caller stack is restored, including objectref "this".
        // Bytecode for playWithThis() will be rewound to the invokevirtual instruction for
        // the invocation of the display() method, which has now been redefined, and the invoke is tried again.
        throw new Exception(" Dummy Exception...");
    }

    public int getValue() {
        return val;
    }
}
