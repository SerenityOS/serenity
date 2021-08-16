/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4315352
 * @summary disabling EventRequest expired with addCountFilter() throws
 * InternalException.
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetAdapter TargetListener
 * @run compile -g CountEvent.java
 * @run driver CountEvent
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

class CountEventTarg {

    static CountEventTarg first = new CountEventTarg();
    static CountEventTarg second = new CountEventTarg();
    static CountEventTarg third = new CountEventTarg();

    public static void main(String args[]) {
        start();
    }

    static void start() {
        first.go();
        second.go();
        third.go();
    }

    void go() {
        one();
        two();
        three();
    }

    void one() {
    }

    void two() {
    }

    void three() {
    }
}

public class CountEvent extends TestScaffold {

    public static void main(String args[]) throws Exception {
        new CountEvent(args).startTests();
    }

    CountEvent(String args[]) throws Exception {
        super(args);
    }

    protected void runTests() throws Exception {

        BreakpointEvent bpe = startToMain("CountEventTarg");
        ThreadReference thread = bpe.thread();

        StepEvent stepEvent = stepIntoLine(thread);

        ReferenceType clazz = thread.frame(0).location().declaringType();
        String className = clazz.name();

        // uses addCountFilter
        BreakpointEvent bpEvent = resumeTo(className, "go", "()V");

        bpEvent.request().disable();
        System.out.println("About to resume");
        resumeToVMDisconnect();

        if (!testFailed) {
            println("CountEvent: passed");
        } else {
            throw new Exception("CountEvent: failed");
        }
    }
}
