/*
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @bug 8212928
 * @summary With NeedsDeoptSuspend == true the assertion in compiledVFrame::update_deferred_value() fails, because the frame is not deoptimized.
 * @author Richard Reingruber richard DOT reingruber AT sap DOT com
 *
 * @library /test/lib
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run main jdk.test.lib.FileInstaller compilerDirectives.json compilerDirectives.json
 * @run compile -g SetLocalWhileThreadInNative.java
 * @run driver SetLocalWhileThreadInNative -Xbatch -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:+UnlockDiagnosticVMOptions -XX:CompilerDirectivesFile=compilerDirectives.json
 */

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.sun.jdi.*;
import com.sun.jdi.event.*;

import jdk.test.lib.Asserts;

/********** target program **********/

class SetLocalWhileThreadInNativeTarget {

    public static final String name = SetLocalWhileThreadInNativeTarget.class.getName();
    public static FileInputStream fis;
    public static int count;
    public static int bytesRead;


    // Let D (debugger) be a JVMTI agent that updates a local of a compiled frame F owned by
    // thread T. In this test F corresponds to dontinline_testMethod.
    //
    // Issue: The VM thread executes an VM op on behalf of D to set the local in F. Doing so it
    // requests the deoptimization of F and then calls compiledVFrame::update_deferred_value(),
    // where frame::is_deoptimized_frame() returns false causing an assertion failure on SPARC where
    // NeedsDeoptSuspend is true.
    //
    // Analysis: The deoptimization of F is requested, while T is in the native method
    // java.io.FileInputStream::read0() and F is the direct caller of read0(). This is a special
    // case with NeedsDeoptSuspend in frame::deoptimize(). Effectively the deoptimization is not
    // done synchronously, instead T deoptimizes F at a later point upon return from the native
    // method.
    public static int dontinline_testMethod() {
        int zero = 0;
        int val = 0;
        try {
            val = fis.read(); // Will be inlined. Calls native method java.io.FileInputStream::read0()
            count++;
        } catch (IOException e) { /* ignored */ }
        return val + zero;
    }

    public static void main(String[] args) {
        System.out.println(name + " is up and running.");
        fis = new FileInputStream(FileDescriptor.in);
        bytesRead=0;
        while (true) {
            int val = dontinline_testMethod();
            if (val == SetLocalWhileThreadInNative.STOP) {
                System.out.println("Debuggee: received STOP message");
                System.exit(0);
            }
            bytesRead++;
            if ((bytesRead & ((1L << 14)-1)) == 0) {
                System.out.println("Called test method " + bytesRead + " times");
            }
        }
    }
}

 /********** test program **********/

public class SetLocalWhileThreadInNative extends TestScaffold {
    public static final int MESSAGE_COUNT = 10000;
    public static final String MESSAGE    = "0123456789";
    public static final int MESSAGE_SIZE  = MESSAGE.length();
    public static final int TOTAL_BYTES   = MESSAGE_COUNT * MESSAGE_SIZE;
    public static final int STOP = 255;

    ReferenceType mainClass;
    ThreadReference mainThread;

    SetLocalWhileThreadInNative (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new SetLocalWhileThreadInNative (args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {
        String targetProgName = SetLocalWhileThreadInNativeTarget.class.getName();
        String testName = getClass().getSimpleName();

        // Start debuggee and obtain reference to main thread an main class
        BreakpointEvent bpe = startToMain(targetProgName);
        mainClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        // Resume debuggee send some bytes
        vm().resume();
        OutputStream os = vm().process().getOutputStream();
        byte[] ba = MESSAGE.getBytes();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            os.write(ba);
        }
        os.flush();

        // Wait for the debugee to read all the bytes.
        int bytesRead = 0;
        Field bytesReadField = mainClass.fieldByName("bytesRead");
        do {
            bytesRead = ((PrimitiveValue)mainClass.getValue(bytesReadField)).intValue();
            System.out.println("debugee has read " + bytesRead + " of " + TOTAL_BYTES);
            Thread.sleep(500);
        } while (bytesRead < TOTAL_BYTES);

        // By now  dontinline_testMethod() will be compiled. The debugee will be blocked in java.io.FileInputStream::read0().
        // Now set local variable in dontinline_testMethod().
        vm().suspend();
        System.out.println("Debuggee Stack:");
        List<StackFrame> stack_frames = mainThread.frames();
        int i = 0;
        for (StackFrame ff : stack_frames) {
            System.out.println("frame[" + i++ +"]: " + ff.location().method());
        }
        StackFrame frame = mainThread.frame(2);
        Asserts.assertEQ(frame.location().method().toString(), "SetLocalWhileThreadInNativeTarget.dontinline_testMethod()");
        List<LocalVariable> localVars = frame.visibleVariables();
        boolean changedLocal = false;
        for (LocalVariable lv : localVars) {
            if (lv.name().equals("zero")) {
                frame.setValue(lv, vm().mirrorOf(0)); // triggers deoptimization!
                changedLocal = true;
            }
        }
        Asserts.assertTrue(changedLocal);

        // signal stop
        os.write(STOP);
        os.flush();


        // resume the target listening for events
        listenUntilVMDisconnect();


        // deal with results of test if anything has called failure("foo")
        // testFailed will be true
        if (!testFailed) {
            println(testName + ": passed");
        } else {
            throw new Exception(testName + ": failed");
        }
    }
}
