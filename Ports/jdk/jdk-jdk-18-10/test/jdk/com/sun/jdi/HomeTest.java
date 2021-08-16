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
 * @bug 4446294
 * @summary JDI spec/impl: default "home" for CommandLineLaunch isn't java.home
 * @author Tim Bell (based on "HomeTest.java" by Eugene I. Latkin)
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g HomeTest.java
 * @run driver HomeTest
 */
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class HomeTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from HomeTarg!");
    }
}

    /********** test program **********/

public class HomeTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    HomeTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new HomeTest(args).startTests();
    }

    /********** test core **********/
    private static String getValue(Map arguments, String key) {
        Connector.Argument x = (Connector.StringArgument) arguments.get(key);
        return x.value();
    }
    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("HomeTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        Connector defaultConnector = vmm.defaultConnector();
        Map arguments = defaultConnector.defaultArguments();
        String argsHome = getValue(arguments,"home");
        String javaHome = System.getProperty("java.home");
        if (!argsHome.equals(javaHome)) {
            failure("FAILURE: Value for \"home\" does not match value for \"java.home\"");
            failure("     home is: " + argsHome);
        }
        println("java.home is: " + javaHome);
        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("HomeTest: passed");
        } else {
            throw new Exception("HomeTest: failed");
        }
    }
}
