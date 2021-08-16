/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4528948
 * @summary Unable to finish a debugging in NetBeans IDE
 * @author jjh
 *
 * @library ..
 *
 * @run build  TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g DeleteAllBkptsTest.java
 * @run driver DeleteAllBkptsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class DeleteAllBkptsTarg {
    public void gus() {
    }

    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from DeleteAllBkptsTarg!");
    }
}

    /********** test program **********/

public class DeleteAllBkptsTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    DeleteAllBkptsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new DeleteAllBkptsTest(args).startTests();
    }


    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("DeleteAllBkptsTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();


        Method method = findMethod(targetClass, "gus", "()V");
        if (method == null) {
            throw new IllegalArgumentException("Bad method name/signature");
        }
        BreakpointRequest request = erm.createBreakpointRequest(
                                                   method.location());

        // This avoids the problem.
        //request.enable();

        try {
            erm.deleteAllBreakpoints();
        } catch (InternalException ee) {
            // We get a failure here if no bkpts exist because of
            // an un-init variable in BE function eventHandler_freeAll
            failure("FAIL: Unexpected Exception encountered: " + ee);
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("DeleteAllBkptsTest: passed");
        } else {
            throw new Exception("DeleteAllBkptsTest: failed");
        }
    }
}
