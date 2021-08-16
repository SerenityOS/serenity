/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/instanceCounts/instancecounts003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 * This scenario in particular cover the situation described in CR 6376715.
 *      The test scenario is following:
 *       - the debugger gets the debuggee running on another JavaVM and
 *         establishes a pipe with the debuggee program
 *       - upon receiving corresponding command from the debugger process the debuggee
 *         do load set of classes. No instances of classes are created
 *       - the debugger process check that instanceCounts returns 0
 *       - the debuggee process drop all references to the class and forces VM
 *         to unload them.
 *       - the debuggee process will check that instanceCounts returns 0 and no
 *         com.sun.jdi.ObjectCollectedException or com.sun.jdi.ClassNotLoadedException
 *         exception is thrown.
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.instanceCounts.instancecounts003.instancecounts003
 *        nsk.share.jdi.TestClass1
 *        nsk.share.jdi.TestInterfaceImplementer1
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.instanceCounts.instancecounts003.instancecounts003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 */

package nsk.jdi.VirtualMachine.instanceCounts.instancecounts003;

import java.io.PrintStream;
import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jdi.*;
import nsk.share.jpda.AbstractDebuggeeTest;

public class instancecounts003 extends HeapwalkingDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instancecounts003().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        if (classpath == null) {
            throw new TestBug("Debugger requires 'testClassPath' parameter");
        }

        return AbstractJDIDebuggee.class.getName() + " -testClassPath " + classpath;
    }

    protected void doTest() {
        String testClassNames[] = { "nsk.share.jdi.TestClass1", "nsk.share.jdi.TestInterfaceImplementer1" };

        // load classes, don't create instances, check that instancCounts returns 0
        for (String className : testClassNames) {
            pipe.println(AbstractDebuggeeTest.COMMAND_LOAD_CLASS + ":" + className);
            checkDebugeeAnswer_instanceCounts(className, 0);
        }

        testInstanceCounts();

        // unload classes, check that instancCounts returns 0 and there are no unexpected exceptions
        for (String className : testClassNames) {
            pipe.println(AbstractDebuggeeTest.COMMAND_UNLOAD_CLASS + ":" + className);
            checkDebugeeAnswer_instanceCounts(className, 0);
        }

        testInstanceCounts();
    }
}
