/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8193879 8193801 8129348
 * @summary Invokes static and instance methods when debugger trace
 * mode is on.
 * @library /test/lib
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g MethodInvokeWithTraceOnTest.java
 * @run driver MethodInvokeWithTraceOnTest
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

import static lib.jdb.JdbTest.*;

/********** target program **********/

class MethodInvokeWithTraceOnTestTarg {
    public static void main(String[] args) {
        new MethodInvokeWithTraceOnTestTarg().test();
    }

    private void test() {
        Thread thread = Thread.currentThread();
        print(thread); // @1 breakpoint
        String str = "test";
        printStatic(str); // @2 breakpoint

    }

    public void print(Object obj) {
        System.out.println(obj);
    }

    public static void printStatic(Object obj) {
        System.out.println(obj);
    }

}


/********** test program **********/

public class MethodInvokeWithTraceOnTest extends TestScaffold {

    MethodInvokeWithTraceOnTest(String args[]) {
        super(args);
    }

    public static void main(String[] args)
            throws Exception {
        new MethodInvokeWithTraceOnTest(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        init();

        // Test with suspend policy set to SUSPEND_EVENT_THREAD
        BreakpointEvent be = resumeToBreakpoint(true, 1);
        System.out.println("Breakpoint 1 is hit, suspendPolicy:" + be.request().suspendPolicy());
        testMethods(be);

        // Test with suspend policy set to SUSPEND_ALL
        be = resumeToBreakpoint(false, 2);
        System.out.println("Breakpoint 2 is hit, suspendPolicy:" + be.request().suspendPolicy());
        testMethods(be);

        listenUntilVMDisconnect();
    }

    private void init() throws Exception {
        startToMain("MethodInvokeWithTraceOnTestTarg");
        vm().setDebugTraceMode(VirtualMachine.TRACE_ALL);
    }

    private BreakpointEvent resumeToBreakpoint(boolean suspendThread, int breakpointId) throws Exception {
        int bkpLine = parseBreakpoints(getTestSourcePath("MethodInvokeWithTraceOnTest.java"), breakpointId).get(0);
        System.out.println("Running to line: " + bkpLine);
        return resumeTo("MethodInvokeWithTraceOnTestTarg", bkpLine, suspendThread);
    }

    private void testMethods(BreakpointEvent be) throws Exception {
        System.out.println("Testing  methods...");
        ThreadReference thread = be.thread();
        StackFrame frame = thread.frame(0);
        ObjectReference thisObj = frame.thisObject();
        LocalVariable threadVar = frame.visibleVariableByName("thread");
        ThreadReference threadObj = (ThreadReference) frame.getValue(threadVar);
        StringReference stringObj = vm().mirrorOf("test string");
        int invokeOptions = getMethodInvokeOptions(be);

        testInstanceMethod1(thread, thisObj, stringObj, threadObj, invokeOptions);
        testStaticMethod1(thread, thisObj, stringObj, threadObj, invokeOptions);
        testStaticMethod2(thread, invokeOptions);
    }

    private void testInstanceMethod1(ThreadReference thread, ObjectReference thisObj, StringReference stringObj,
                                     ThreadReference threadObj, int invokeOptions) throws Exception {
        ClassType classType = (ClassType) thisObj.referenceType();
        Method printMethod = classType.methodsByName("print",
                "(Ljava/lang/Object;)V").get(0);

        System.out.println("Passing StringReference to instance method...");
        thisObj.invokeMethod(thread, printMethod, Collections.singletonList(stringObj), invokeOptions);

        System.out.println("Passing ThreadReference to instance method...");
        thisObj.invokeMethod(thread, printMethod, Collections.singletonList(threadObj), invokeOptions);
    }

    private void testStaticMethod1(ThreadReference thread, ObjectReference thisObj, StringReference stringObj,
                                   ThreadReference threadObj, int invokeOptions) throws Exception {
        ClassType classType = (ClassType) thisObj.referenceType();
        Method printMethod = classType.methodsByName("printStatic",
                "(Ljava/lang/Object;)V").get(0);

        System.out.println("Passing StringReference to static method...");
        classType.invokeMethod(thread, printMethod, Collections.singletonList(stringObj), invokeOptions);

        System.out.println("Passing ThreadReference to static method...");
        classType.invokeMethod(thread, printMethod, Collections.singletonList(threadObj), invokeOptions);
    }

    private void testStaticMethod2(ThreadReference thread, int invokeOptions) throws Exception {
        ClassType classType = getClassType("java.lang.Class");
        Method forNameMethod = classType.methodsByName("forName",
                "(Ljava/lang/String;)Ljava/lang/Class;").get(0);
        StringReference classNameParam = vm().mirrorOf("java.lang.String");
        classType.invokeMethod(thread, forNameMethod, Collections.singletonList(classNameParam), invokeOptions);
    }

    private ClassType getClassType(String className) {
        List classes = vm().classesByName(className);
        return (ClassType) classes.get(0);
    }

    private int getMethodInvokeOptions(BreakpointEvent be) {
        return be.request().suspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD ?
                ObjectReference.INVOKE_SINGLE_THREADED : 0;
    }
}
