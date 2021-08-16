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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/instances/instances003.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *      The test scenario is following:
 *         - Debugger VM
 *         for refererence_type in <Strong, JNI_Local_Ref, JNI_Global_Ref, JNI_Weak_Ref, PhantomReference, SoftReference, WeakReference>
 *                 - initiate creation test class instances  of type 'refererence_type' in debuggee VM
 *                 - prevent some instances from being garbage collected using ObjectReference.disableCollection
 *                 - initiate GarbageCollection in Debuggee VM
 *                 - check the number of instances is left is correct
 *                 - enables Garbage Collection for instances for which it were previously disabled using ObjectReference.enableCollection
 *                 - initiate GarbageCollection in Debuggee VM
 *                 - check the number of instances is 0
 *         done
 *         Test is executed for following sublcasses of ObjectReference: StringReference, ThreadReference, ClassLoaderReference
 *
 * @requires vm.gc != "Z"
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.instances.instances003.instances003
 * @run main/othervm/native
 *      nsk.jdi.ReferenceType.instances.instances003.instances003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx128M ${test.vm.opts} ${test.java.opts}"
 *      -testClassNames nsk.jdi.ReferenceType.instances.instances003.instances003$TestClassLoader:java.lang.String:java.lang.Thread
 */

package nsk.jdi.ReferenceType.instances.instances003;

import java.io.PrintStream;
import java.util.*;

import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

import nsk.share.Consts;
import nsk.share.ObjectInstancesManager;
import nsk.share.TestBug;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jpda.AbstractDebuggeeTest;

public class instances003 extends HeapwalkingDebugger {
    // use subclass of java.lang.ClassLoader to be sure that there are no its instances in debuggee VM
    public static class TestClassLoader extends ClassLoader {

    }

    private String testClasses[];

    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testClassNames") && (i < args.length - 1)) {
                testClasses = args[i + 1].split(":");

                i++;
            } else
                standardArgs.add(args[i]);
        }

        if ((testClasses == null) || (testClasses.length == 0))
            throw new TestBug("Test class names was not specified, use test parameter '-testClassNames'");

        return standardArgs.toArray(new String[] {});
    }

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instances003().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return nsk.share.jdi.HeapwalkingDebuggee.class.getName();
    }

    protected void testClass(String className, String referrerType) {
        final int createInstanceCount = 50;
        final int referrerCount = 1;

        List<ObjectReference> objectsToFilter = HeapwalkingDebugger.getObjectReferences(className, vm);

        // create 'createInstanceCount' instances of test class

        // create temporary strong references to prevent the weakly referred instances being GCed
        // during the time between creating them and disabling collection on them
        boolean useTempStrongReference = needTempStongReference(referrerType);
        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + className + ":" + createInstanceCount +
                ":" + referrerCount + ":" + referrerType +
                (useTempStrongReference ? "|" + ObjectInstancesManager.STRONG_REFERENCE : ""));

        // Note! This test is broken, in the sense that it incorrectly assumes
        // that no GC can happen before it walks the heap. In practice, it seems
        // to only affect this test when using ZGC. However, this test will also
        // fail when using other GCs if an explicit GC is done here.

        // the instance counts should not be affected by creating multiple references
        checkDebugeeAnswer_instanceCounts(className, createInstanceCount, objectsToFilter);

        ReferenceType referenceType = debuggee.classByName(className);
        List<ObjectReference> allInstances = HeapwalkingDebugger.filterObjectReferrence(objectsToFilter, referenceType.instances(0));

        // There are potentially other non-test Java threads allocating objects and triggering GC's.
        // We need to call disableCollection() on each object returned by referenceType.instances()
        // to deal with the case when GC was triggered. Otherwise, these objects can
        // be potentially collected.
        List<ObjectReference> instances = new LinkedList<>();
        for (ObjectReference objRef : allInstances) {
            try {
                objRef.disableCollection();
                instances.add(objRef);
            } catch (ObjectCollectedException ex) {
                // skip this references
            }
        }

        // remove the temporary strong references so the weak references can be properly tested
        if (useTempStrongReference) {
            pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_REFERRERS + ":" + className + ":" + referrerCount + ":" + ObjectInstancesManager.STRONG_REFERENCE);
            if (!isDebuggeeReady()) {
                return;
            }
        }

        // prevent half of instances from GC, delete references and force GC

        int preventGCCount = createInstanceCount / 2;

        for (int i = 0; i < preventGCCount; i++) {
            instances.get(i).enableCollection();
        }


        pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + className + ":" + createInstanceCount);

        if (!isDebuggeeReady())
            return;

        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_GC);

        checkDebugeeAnswer_instanceCounts(className, createInstanceCount - preventGCCount, objectsToFilter);

        // enable garbage collection for all instances and force GC

        for (ObjectReference reference : referenceType.instances(0)) {
            reference.enableCollection();
        }

        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_GC);

        checkDebugeeAnswer_instanceCounts(className, 0, objectsToFilter);
    }

    protected void doTest() {
        for (String referenceType : HeapwalkingDebuggee.includedIntoInstancesCountTypes) {
            for (String className : testClasses)
                testClass(className, referenceType);
        }
    }


    private static boolean needTempStongReference(String referenceType) {
        return ObjectInstancesManager.WEAK_REFERENCE.equals(referenceType) ||
                ObjectInstancesManager.JNI_WEAK_REFERENCE.equals(referenceType) ||
                ObjectInstancesManager.PHANTOM_REFERENCE.equals(referenceType) ||
                ObjectInstancesManager.SOFT_REFERENCE.equals(referenceType);

    }
}
