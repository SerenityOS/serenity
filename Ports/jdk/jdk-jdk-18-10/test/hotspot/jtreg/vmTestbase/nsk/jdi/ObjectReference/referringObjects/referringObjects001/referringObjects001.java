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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/referringObjects/referringObjects001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test checks behaviour of ReferenceType.instances, VirtualMachine.instanceCounts, ObjectReference.referringObjects
 *     in cases when object is reacheable via following types of references:
 *         - strong
 *         - soft
 *         - weak
 *         - phantom
 *         - jni local
 *         - jni global
 *         - jni weak
 *     Test is executed for following sublcasses of ObjectReference: StringReference, ThreadReference, ClassLoaderReference, ArrayReference.
 *     The test scenario is following:
 *     - Debugger VM
 *         for refererence_type in <Strong, JNI_Local_Ref, JNI_Global_Ref, JNI_Weak_Ref, PhantomReference, SoftReference, WeakReference>
 *         do
 *                 - initiate creation of referring objects of type 'refererence_type'
 *                 - check the number of referrers and instances is correct
 *                 - initiate deletion of some referreres(making them unreachable)
 *                 - check the number of referrers and instances is correct
 *         done
 *     - Debugger VM
 *         create references of all possible types to single object, ObjectReference.referringObjects should return only
 *         referrers with supported type(Strong, PhantomReference, SoftReference, WeakReference)
 *
 * @requires vm.gc != "Z"
 * @requires !vm.graal.enabled
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.referringObjects.referringObjects001.referringObjects001
 *        nsk.share.jdi.TestClass1
 * @run main/othervm/native
 *      nsk.jdi.ObjectReference.referringObjects.referringObjects001.referringObjects001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx128M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ObjectReference.referringObjects.referringObjects001;

import java.io.PrintStream;
import java.util.*;

import nsk.share.*;
import nsk.share.jdi.*;


import com.sun.jdi.*;
import nsk.share.jpda.AbstractDebuggeeTest;

public class referringObjects001 extends HeapwalkingDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new referringObjects001().runIt(argv, out);
        }

    protected String debuggeeClassName() {
        return nsk.share.jdi.HeapwalkingDebuggee.class.getName();
    }

    // test given class with all references types
    protected void testClass(String className) {
        log.display("Test ObjectReference for class: " + className);

        // force GC in debuggee VM to avoid collection of weak references during test execution
        forceGC();

        for (String referrerType : ObjectInstancesManager.allReferenceTypes) {
            testReferrerType(referrerType, className);
            if (ObjectInstancesManager.isWeak(referrerType)) {
                resetStatusIfGC();
            }
        }

        int createInstanceCount = 10;
        List<ObjectReference> objectsToFilter = getObjectReferences(className, vm);

        // create instances reachable via all types of references
        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_ALL_TYPE_REFERENCES + ":" + className + ":" + createInstanceCount);

        if (!isDebuggeeReady())
            return;

        // ObjectReference.referringObjects should include only supported types of references
        checkDebugeeAnswer_instances_referringObjects(objectsToFilter, className, createInstanceCount, true,
                HeapwalkingDebuggee.includedIntoReferrersCountTypes.size());

        for (int i = 0; i < ObjectInstancesManager.allReferenceTypes.size(); i++) {
            pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_REFERRERS + ":" + className + ":" + 1);

            if (!isDebuggeeReady())
                return;
        }
    }


    protected void testReferrerType(String referrerType, String className) {
        // can't check number of referrers for objects that was created in debugee VM
        // before debugger command (for example primitive type arrays), so we should filter such objects
        List<ObjectReference> objectsToFilter = HeapwalkingDebugger.getObjectReferences(className, vm);

        boolean includedInReferrersCount = HeapwalkingDebuggee.isIncludedIntoReferrersCount(referrerType);
        boolean includedInInstancesCount = HeapwalkingDebuggee.isIncludedIntoInstancesCount(referrerType);

        int createInstanceCount = 4;
        int referrerCount = 10;

        // create 'createInstanceCount' instances with 'referrerCount' referrers

        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + className + ":" + createInstanceCount + ":" + referrerCount + ":"
                + referrerType);

        int expectedInstanceCount;

        if (includedInInstancesCount)
            expectedInstanceCount = createInstanceCount;
        else
            expectedInstanceCount = 0;

        // Note! This test is broken, in the sense that it incorrectly assumes
        // that no GC can happen before it walks the heap. In practice, it seems
        // to only affect this test when using ZGC. However, this test will also
        // fail when using other GCs if an explicit GC is done here.

        checkDebugeeAnswer_instanceCounts(className, expectedInstanceCount, objectsToFilter);
        checkDebugeeAnswer_instances_referringObjects(objectsToFilter, className, expectedInstanceCount, includedInReferrersCount, referrerCount);

        // delete half of referrers

        int deleteCount = referrerCount / 2;

        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_REFERRERS + ":" + className + ":" + deleteCount);

        referrerCount -= deleteCount;

        if (includedInInstancesCount)
            expectedInstanceCount = createInstanceCount;
        else
            expectedInstanceCount = 0;

        checkDebugeeAnswer_instanceCounts(className, expectedInstanceCount, objectsToFilter);
        checkDebugeeAnswer_instances_referringObjects(objectsToFilter, className, expectedInstanceCount, includedInReferrersCount, referrerCount);

        // delete half of instances

        deleteCount = createInstanceCount / 2;
        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + className + ":" + deleteCount);

        createInstanceCount -= deleteCount;

        if (includedInInstancesCount)
            expectedInstanceCount = createInstanceCount;
        else
            expectedInstanceCount = 0;

        checkDebugeeAnswer_instanceCounts(className, expectedInstanceCount, objectsToFilter);
        checkDebugeeAnswer_instances_referringObjects(objectsToFilter, className, expectedInstanceCount, includedInReferrersCount, referrerCount);

        // delete all referrers (make object unreachable)

        deleteCount = referrerCount;

        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_REFERRERS + ":" + className + ":" + deleteCount);

        referrerCount = 0;
        createInstanceCount = 0;

        expectedInstanceCount = 0;

        checkDebugeeAnswer_instanceCounts(className, expectedInstanceCount, objectsToFilter);
        checkDebugeeAnswer_instances_referringObjects(objectsToFilter, className, expectedInstanceCount, includedInReferrersCount, referrerCount);
    }

    protected void checkDebugeeAnswer_instances_referringObjects(List<ObjectReference> objectsToFilter, String className, int expectedInstances,
            boolean checkReferrers, int expectedReferrers) {
        try {
            ReferenceType referenceType = debuggee.classByName(className);

            if (checkReferrers) {
                List<ObjectReference> objectReferrences = HeapwalkingDebugger.filterObjectReferrence(objectsToFilter, referenceType.instances(0));

                // used for non-strict check
                int correctObjectsFound = 0;

                for (ObjectReference objectReference : objectReferrences) {
                    int referrerCount = objectReference.referringObjects(0).size();

                    if (strictCheck(className)) {
                        if (referrerCount != expectedReferrers) {
                            setSuccess(false);
                            log.complain("List with wrong size was returned by ObjectReference.referringObjects: " + referrerCount + ", expected: "
                                    + expectedReferrers);
                        }
                    } else {
                        if (referrerCount == expectedReferrers) {
                            correctObjectsFound++;
                        }
                    }
                }

                if (!strictCheck(className)) {
                    if (correctObjectsFound != expectedInstances) {
                        setSuccess(false);
                        log.complain("List with wrong size was returned by ObjectReference.referringObjects, expected: " + expectedReferrers);
                    }
                }
            }
        } catch (Throwable t) {
            log.complain("Unexpected exception:");
            t.printStackTrace(log.getOutStream());
        }
    }

    protected void doTest() {
        String[] testClassNames = { "java.lang.String", "nsk.share.jdi.TestClass1", "boolean[]",
                "float[]" };

        for (String className : testClassNames) {
            testClass(className);
        }
    }
}
