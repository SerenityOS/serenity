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
 *
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/instances/instances002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *      The test scenario is following:
 *       - Debugger VM
 *         - initiate creation a number of instances in debuggee VM using ClassType.newInstance
 *         - check the number of instances is correct
 *         - initiate creation a number of instances in debuggee VM using ArrayType.newInstance
 *         - check the number of instances is correct
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.instances.instances002.instances002
 *        nsk.jdi.ReferenceType.instances.instances002.instances002a
 *        nsk.share.jdi.TestClass1
 *        nsk.share.jdi.TestInterfaceImplementer1
 * @run main/othervm/native
 *      nsk.jdi.ReferenceType.instances.instances002.instances002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ReferenceType.instances.instances002;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import nsk.share.Consts;
import nsk.share.ObjectInstancesManager;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jpda.AbstractDebuggeeTest;

public class instances002 extends HeapwalkingDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instances002().runIt(argv, out);
    }

    protected boolean canRunTest() {
        return super.canRunTest() || (!vm.canBeModified());
    }

    protected String debuggeeClassName() {
        return nsk.jdi.ReferenceType.instances.instances002.instances002a.class.getName();
    }

    // create instance of ReferenceType
    public ReferenceType prepareReferenceType(String className, int instanceCount) {
        String referrerType = ObjectInstancesManager.STRONG_REFERENCE;

        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + className + ":" + instanceCount + ":" + "1" + ":" + referrerType);

        if (!isDebuggeeReady())
            return null;

        return debuggee.classByName(className);
    }

    // test method ArrayType.newInstance
    public void testArrayType(String className) {
        // create some instances in target VM, just to get ReferenceType object
        int baseInstances = 10;

        ReferenceType referenceType = prepareReferenceType(className, baseInstances);

        if (referenceType == null)
            return;

        if (!(referenceType instanceof ArrayType)) {
            setSuccess(false);
            log.complain("Unexpected reference type: " + referenceType.getClass().getName() + ", expected is ArrayType");
            return;
        }
        // There are potentially other non-test Java threads allocating objects and triggering GC's.
        debuggee.suspend();

        List<ObjectReference> baseReferences = new LinkedList<>();
        // We need to call disableCollection() on each object returned by referenceType.instances()
        // to deal with the case when GC was triggered before the suspend. Otherwise, these objects can
        // be potentially collected.
        for (ObjectReference objRef : referenceType.instances(0)) {
            try {
                objRef.disableCollection();
                baseReferences.add(objRef);
            } catch (ObjectCollectedException e) {
                // skip this reference
            }
        }
        baseInstances = baseReferences.size();

        int createInstanceCount = 100;
        int arraySize = 1;

        ArrayType arrayType = (ArrayType) referenceType;
        List<ArrayReference> objectReferences = new ArrayList<ArrayReference>();

        for (int i = 0; i < createInstanceCount; i++) {
            // instances created in this way aren't reachable for the purposes of garbage collection,
            // to make it reachable call disableCollection() for this objects
            ArrayReference arrayReference = arrayType.newInstance(arraySize);
            arrayReference.disableCollection();

            objectReferences.add(arrayReference);
        }

        checkDebugeeAnswer_instances(className, createInstanceCount + baseInstances);

        for (ArrayReference arrayReference : objectReferences) {
            arrayReference.enableCollection();
        }

        for (ObjectReference baseRef : baseReferences) {
            baseRef.enableCollection();
        }

        debuggee.resume();
    }

    // test method ClassType.newInstance
    public void testClassType(String className) {
        // create some instances in target VM, just to get ReferenceType object
        int baseInstances = 10;

        ReferenceType referenceType = prepareReferenceType(className, baseInstances);

        if (referenceType == null)
            return;

        if (!(referenceType instanceof ClassType)) {
            setSuccess(false);
            log.display("Unexpected reference type: " + referenceType.getClass().getName() + ", expected is ClassType");
            return;
        }

        baseInstances = referenceType.instances(0).size();

        ClassType classType = (ClassType) referenceType;

        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_BREAKPOINT);

        BreakpointEvent breakpointEvent = waitForBreakpoint(defaultBreakpointRequest);

        List<Method> methods = referenceType.allMethods();
        List<ObjectReference> objectReferences = new ArrayList<ObjectReference>();

        int createInstanceCount = 100;

        try {
            for (Method method : methods) {
                if (method.isConstructor()) {
                    for (int i = 0; i < createInstanceCount; i++) {
                        objectReferences.add(classType.newInstance(breakpointEvent.thread(), method, new ArrayList<Value>(), 0));
                    }

                    debuggee.resume();
                    checkDebugeeAnswer_instances(className, baseInstances);
                    debuggee.suspend();

                    break;
                }
            }
        } catch (Exception e) {
            setSuccess(false);
            log.display("Unexpected exception: ");
            e.printStackTrace(log.getOutStream());
        }

        debuggee.resume();

        // wait response for command 'COMMAND_FORCE_BREAKPOINT'
        if (!isDebuggeeReady())
            return;

        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + className + ":" + baseInstances);

        if (!isDebuggeeReady())
            return;

        checkDebugeeAnswer_instances(className, 0);
    }

    protected void doTest() {
        initDefaultBreakpoint();

        String[] testClasses = { "nsk.share.jdi.TestClass1", "nsk.share.jdi.TestInterfaceImplementer1" };

        for (String className : testClasses)
            testClassType(className);

        for (String className : ObjectInstancesManager.primitiveArrayClassNames)
            testArrayType(className);
    }

}
