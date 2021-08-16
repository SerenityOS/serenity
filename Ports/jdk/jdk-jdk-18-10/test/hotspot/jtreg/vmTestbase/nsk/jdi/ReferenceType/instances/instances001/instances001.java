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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/instances/instances001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test checks behaviour of ReferenceType.instances, VirtualMachine.instanceCounts
 *     in cases when object is reacheable via following types of references:
 *         - strong
 *         - soft
 *         - weak
 *         - phantom
 *         - jni local
 *         - jni global
 *         - jni weak
 *     Test is executed for following subclasses of ReferenceType:
 *         - ArrayType
 *         - InterfaceType(for InterfaceType methof instances() should always return empty list)
 *     The test scenario is following:
 *     - Debugger VM
 *         - inititate creation instances of sublcass of tested class to ensure that instances of subclasses are not returned by ReferenceType.instances()
 *      - Debugger VM
 *         for refererence_type in <Strong, JNI_Local_Ref, JNI_Global_Ref, JNI_Weak_Ref, PhantomReference, SoftReference, WeakReference>
 *         do
 *             - initiate creation test class instances  of type 'refererence_type'
 *             - check the number of instances is correct
 *             - initiate deletion of some referreres(making them unreachable)
 *             - check the number of instances is correct
 *         done
 *
 * @requires !vm.graal.enabled
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.instances.instances001.instances001
 *        nsk.share.jdi.TestClass1
 *        nsk.share.jdi.TestClass2
 *        nsk.share.jdi.TestInterfaceImplementer1
 * @run main/othervm/native
 *      nsk.jdi.ReferenceType.instances.instances001.instances001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ReferenceType.instances.instances001;

import java.io.PrintStream;
import java.util.*;
import com.sun.jdi.*;

import nsk.share.Consts;
import nsk.share.ObjectInstancesManager;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jpda.AbstractDebuggeeTest;

public class instances001 extends HeapwalkingDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instances001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return nsk.share.jdi.HeapwalkingDebuggee.class.getName();
    }

    // test given class with all references types
    protected void testClass(String className) {
        // force GC in debugee VM to avoid collection of weak references during test execution
        forceGC();

        for (String referrerType : ObjectInstancesManager.allReferenceTypes) {
            testReferrerType(referrerType, className);
            if (ObjectInstancesManager.isWeak(referrerType)) {
                // if GC occurs during test the results should be ignored
                resetStatusIfGC();
           }
        }
    }

    protected void testReferrerType(String referrerType, String className) {
        int createInstanceCount = 50;
        int baseCount = 0;

        ReferenceType referenceType = debuggee.classByName(className);

        // object number created in target VM before debugger command should be filtered
        if (referenceType != null) {
            List<ObjectReference> objectReferences = referenceType.instances(0);

            // disable GC because of uncontrolled object deletion may break test checks
            for (ObjectReference objectReference : objectReferences)
                objectReference.disableCollection();

            baseCount = objectReferences.size();
        }

        boolean isIncludedIntoInstancesCount = HeapwalkingDebuggee.isIncludedIntoInstancesCount(referrerType);

        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + className + ":" + createInstanceCount + ":" + 1 + ":" + referrerType);

        int expectedCount;

        if (isIncludedIntoInstancesCount)
            expectedCount = baseCount + createInstanceCount;
        else
            expectedCount = baseCount;

        checkDebugeeAnswer_instanceCounts(className, expectedCount);
        checkDebugeeAnswer_instances(className, expectedCount);

        //delete half of instances

        int deleteCount = createInstanceCount / 2;
        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + className + ":" + deleteCount);

        createInstanceCount -= deleteCount;

        if (isIncludedIntoInstancesCount)
            expectedCount = baseCount + createInstanceCount;
        else
            expectedCount = baseCount;

        checkDebugeeAnswer_instanceCounts(className, expectedCount);
        checkDebugeeAnswer_instances(className, expectedCount);

        //delete all instances

        deleteCount = createInstanceCount;

        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + className + ":" + deleteCount);

        if (isIncludedIntoInstancesCount)
            expectedCount = baseCount;
        else
            expectedCount = baseCount;

        checkDebugeeAnswer_instanceCounts(className, expectedCount);
        checkDebugeeAnswer_instances(className, expectedCount);
    }

    // also check number of instanes for InterfaceType
    protected void checkDebugeeAnswer_instances(String className, int expectedInstances) {
        super.checkDebugeeAnswer_instances(className, expectedInstances);

        ReferenceType referenceType = debuggee.classByName(className);

        // check number of instances for InterfaceType(should be 0)
        if (referenceType instanceof ClassType) {
            ClassType classType = (ClassType) referenceType;

            for (InterfaceType interfaceType : classType.interfaces()) {
                checkDebugeeAnswer_InterfaceType_instances(interfaceType, 0);
            }
        }
    }

    // ArrayType, ClassType and InterfaceType are tested here
    protected void doTest() {
        String testClassName = "nsk.share.jdi.TestClass1";
        String testClassSubclassName = "nsk.share.jdi.TestClass2";

        int subclassInstanceCount = 10;

        String testClasses[] = { testClassName, "nsk.share.jdi.TestInterfaceImplementer1", "boolean[]", "float[]" };

        // create instances of 'nsk.share.jdi.TestClass1' subclass to ensure that instances of subclasses are not returned by ReferenceType.instances()
        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + testClassSubclassName + ":" + subclassInstanceCount);

        checkDebugeeAnswer_instanceCounts(testClassSubclassName, subclassInstanceCount);
        checkDebugeeAnswer_instances(testClassSubclassName, subclassInstanceCount);

        for (String className : testClasses) {
            testClass(className);
        }
    }
}
