/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase nsk/jdi/stress/ClassPrepareEvents/ClassPrepareEvents001.
 * VM Testbase keywords: [stress, quick, jpda, jdi, feature_jdk6_jpda, vm6, quarantine]
 * VM Testbase comments: 6426321
 * VM Testbase readme:
 * DESCRIPTION
 *         Test covers bug 6426321. Test stress event queue forcing loading of 3000 classes in debuggee.
 *         Debugger in loop sends command to debuggee to load class and waits READY answer.
 *         Test passes if no hangs or any other errors occur in debuggee (if debuggee successfully sends answers for all commands).
 *         (number of classes to load can be changed through parameter -classesToLoad (for example -classesToLoad 1000)).
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.stress.ClassPrepareEvents.ClassPrepareEvents001.ClassPrepareEvents001
 * @run main/othervm
 *      nsk.jdi.stress.ClassPrepareEvents.ClassPrepareEvents001.ClassPrepareEvents001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 */

package nsk.jdi.stress.ClassPrepareEvents.ClassPrepareEvents001;

import java.io.*;
import java.util.ArrayList;
import nsk.share.*;
import nsk.share.jdi.*;
import nsk.share.jpda.AbstractDebuggeeTest;

public class ClassPrepareEvents001 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ClassPrepareEvents001().runIt(argv, out);
    }

    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-classesToLoad") && (i < args.length - 1)) {
                classesToLoad = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    protected String debuggeeClassName() {
        if (classpath == null)
            throw new TestBug("Debugger requires 'testClassPath' parameter");

        return AbstractJDIDebuggee.class.getName() + " -testClassPath " + classpath;
    }

    private int classesToLoad = 3000;

    public void doTest() {
        log.display("Debugger forces debuggee to load " + classesToLoad + " classes");

        // force loading of 'classesToLoad' classes in debuggee VM
        for (int i = 0; i < classesToLoad; i++) {
            pipe.println(AbstractDebuggeeTest.COMMAND_LOAD_CLASS + ":" + TestClass1.class.getName());
            if (!isDebuggeeReady())
                return;
        }
    }
}
