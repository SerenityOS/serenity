/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8021897
 * @summary Test getting the value for an uninitialized String object
 * @author Staffan Larsen
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g GetUninitializedStringValue.java
 * @run driver GetUninitializedStringValue
 */
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;

 /********** target program **********/

class GetUninitializedStringValueTarg {
    public static void main(String[] args) {
        new String("foo");
        System.out.println("Goodbye from GetUninitializedStringValueTarg!");
    }
}

 /********** test program **********/

public class GetUninitializedStringValue extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    GetUninitializedStringValue (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new GetUninitializedStringValue (args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {
        /*
         * Run to String.<init>
         */
        startUp("GetUninitializedStringValueTarg");
        BreakpointEvent bpe = resumeTo("java.lang.String", "<init>", "(Ljava/lang/String;)V");

        /*
         * We've arrived. Look at 'this' - it will be uninitialized (the value field will not be set yet).
         */
        StackFrame frame = bpe.thread().frame(0);
        StringReference sr = (StringReference)frame.thisObject();
        if (!sr.value().equals("")) {
            throw new Exception("Unexpected value for the uninitialized String");
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();
    }
}
