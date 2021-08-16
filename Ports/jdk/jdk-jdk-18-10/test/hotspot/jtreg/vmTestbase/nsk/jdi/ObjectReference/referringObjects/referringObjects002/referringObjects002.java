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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/referringObjects/referringObjects002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Test check behaviour of ObjectReference.referringObjects, ObjectReference.disableCollection and
 *         ObjectReference.enableCollection for ClassObjectReference
 *      The test scenario is following:
 *       - Debugger VM
 *                 - initiate in target VM loading of test class:
 *                 - Debugee VM
 *                 - create several instances of loaded class
 *                 - create references of all possible types to loaded class
 *       - Debugger VM
 *                 - check test class have correct number of referrers
 *                 (class object referrers should include class instances and references with supported types: Strong, PhantomReference, SoftReference, WeakReference)
 *       - Debugger VM
 *                 - initiate in debuggee removing of class instances and class object references
 *                 - check test class have correct number of referrers
 *       - Debugger VM
 *                 - prevent collection of class object using ObjectReference.disableCollection
 *                 - initiate test class unloading in debugee VM
 *                 - check class object was not collected
 *                 - enable collection of class object using ObjectReference.enableCollection
 *                 - check class object was collected
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.referringObjects.referringObjects002.referringObjects002
 *        nsk.jdi.ObjectReference.referringObjects.referringObjects002.referringObjects002a
 *        nsk.share.jdi.TestClass1
 * @run main/othervm/native
 *      nsk.jdi.ObjectReference.referringObjects.referringObjects002.referringObjects002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 */

package nsk.jdi.ObjectReference.referringObjects.referringObjects002;

import java.io.PrintStream;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;

public class referringObjects002 extends HeapwalkingDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new referringObjects002().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        if (classpath == null) {
            throw new TestBug("Debugger requires 'testClassPath' parameter");
        }

        return nsk.jdi.ObjectReference.referringObjects.referringObjects002.referringObjects002a.class.getName() +
            " -testClassPath " + classpath;
    }

    public void checkClassObjectReferrersCount(ClassObjectReference classObjectReference, int expectedCount) {
        int referrersCount = classObjectReference.referringObjects(0).size();

        if (referrersCount != expectedCount) {
            setSuccess(false);
            log.complain("Unexpected size of ClassLoaderReference.referringObjects: " + referrersCount + ", expected: " + expectedCount);
        }
    }

    protected void doTest() {
        String className = "nsk.share.jdi.TestClass1";

        int createInstances = 10;

        pipe.println(HeapwalkingDebuggee.COMMAND_LOAD_CLASS + ":" + className + ":" + createInstances);

        if (!isDebuggeeReady())
            return;

        // each class instances has reference to class object +
        // + 'includedIntoReferrersCountTypes.size()' referrers was additionally created
        // +1 referrer is classloader
        // +1 referrer is debugee class unloader
        // +1 self-reference from this_class index
        int expectedReferrersCount = createInstances + HeapwalkingDebuggee.includedIntoReferrersCountTypes.size() + 3;

        ClassObjectReference classObjectReference = debuggee.classByName(className).classObject();

        checkClassObjectReferrersCount(classObjectReference, expectedReferrersCount);

        pipe.println(referringObjects002a.COMMAND_DELETE_CLASS_OBJECT_REFERRERS);

        if (!isDebuggeeReady())
            return;

        // Only this referrers should left:
        // 1 referrer is classloader
        // 1 referrer is debugee class unloader
        // 1 self-reference from this_class index
        expectedReferrersCount = 3;

        checkClassObjectReferrersCount(classObjectReference, expectedReferrersCount);

        // disable collection and try unload class object
        classObjectReference.disableCollection();

        pipe.println(HeapwalkingDebuggee.COMMAND_UNLOAD_CLASS + ":" + className + ":" + HeapwalkingDebuggee.UNLOAD_RESULT_FALSE);

        if (!isDebuggeeReady())
            return;

        try {
            classObjectReference.referringObjects(0);
        } catch (ObjectCollectedException e) {
            setSuccess(false);
            log.complain("Class object was collected after disableCollection");
            return;
        }

        // enable collection and try unload class object
        classObjectReference.enableCollection();

        pipe.println(HeapwalkingDebuggee.COMMAND_UNLOAD_CLASS + ":" + className);

        if (!isDebuggeeReady())
            return;

        try {
            classObjectReference.referringObjects(0);
        } catch (ObjectCollectedException expectedException) {
            // expected exception
            return;
        }

        setSuccess(false);
        log.complain("Class object was not collected after enableCollection");
    }
}
