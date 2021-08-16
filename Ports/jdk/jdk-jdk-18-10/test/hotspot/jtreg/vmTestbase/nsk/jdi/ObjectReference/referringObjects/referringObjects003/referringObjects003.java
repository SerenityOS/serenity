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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/referringObjects/referringObjects003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test check behaviour of ObjectReference.referringObjects for ThreadReference and ThreadGroupReference
 *     and ObjectReference.disableCollection/ObjectReference.enableCollection for ThreadGroupReference
 *     The test scenario is following:
 *       - Debugger VM
 *         - initiate in target VM thread execution:
 *                 - Debugee VM
 *                 - start several threads included in thread group
 *                 - create references of all possible types to all started threads and thread group
 *       - Debugger VM
 *         - check that threads and thread group have correct number of referrers:
 *         (thread referrers should include thread group and references with supported types,
 *          thread group referrers should include group's threads, parent thread group and references with supported types)
 *       - Debugger VM
 *         - initiate in target VM thread stop:
 *         - Debugee VM
 *             - stop all threads and remove all references to threads and thread group
 *       - Debugger VM
 *         - check that thread group have only 1 referrer: parent thread group
 *         - check that threre are no references to test threads in target VM
 *       - Debugger VM
 *         - test ObjectReference.disableCollection, ObjectReference.enableCollection for ThreadGroupReference:
 *         can't force collection of thread group because of thread group always has 1 referrer - parent thread group, so
 *         just test disableCollection/enableCollection don't throw any unexpected exceptions
 *
 * @requires !vm.graal.enabled
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.referringObjects.referringObjects003.referringObjects003
 *        nsk.jdi.ObjectReference.referringObjects.referringObjects003.referringObjects003a
 * @run main/othervm/native
 *      nsk.jdi.ObjectReference.referringObjects.referringObjects003.referringObjects003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ObjectReference.referringObjects.referringObjects003;

import java.io.PrintStream;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jpda.AbstractDebuggeeTest;

public class referringObjects003 extends HeapwalkingDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new referringObjects003().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return "nsk.jdi.ObjectReference.referringObjects.referringObjects003.referringObjects003a";
    }

    // check ObjectReference type and number of referrers
    public void checkThreadGroupReferrersCount(List<ObjectReference> objectReferences, int expectedCount) {
        for (ObjectReference objectReference : objectReferences) {
            if (!(objectReference instanceof ThreadGroupReference)) {
                setSuccess(false);
                log.complain("Unexpected type of ObjectReference: " + objectReference.getClass().getName()
                        + ", expected ThreadGroupReference");
            }

            int referrerCount = objectReference.referringObjects(0).size();

            if (referrerCount != expectedCount) {
                setSuccess(false);
                log
                        .complain("List with wrong size was returned by ObjectReference.referringObjects(ThreadGroupReference): "
                                + referrerCount + ", expected: " + expectedCount);
            }
        }
    }

    // check ObjectReference type and number of referrers
    public void checkThreadReferrersCount(List<ObjectReference> objectReferences, int expectedCount) {
        for (ObjectReference objectReference : objectReferences) {
            if (!(objectReference instanceof ThreadReference)) {
                setSuccess(false);
                log.complain("Unexpected type of ObjectReference: " + objectReference.getClass().getName()
                        + ", expected ThreadReference");
            }

            if (((ThreadReference)objectReference).name().contains("Test thread") == false)
                continue;

            int referrerCount = objectReference.referringObjects(0).size();

            if (referrerCount != expectedCount) {
                setSuccess(false);
                log.complain("List with wrong size was returned by ObjectReference.referringObjects(ThreadReferrence): "
                             + referrerCount + ", expected: " + expectedCount);
            }
        }
    }

    public void doTest() {
        int threadCount = 15;

        // threads and thread groups loaded before debugger command should be
        // filtered
        List<ObjectReference> threadGroupsToFilter = HeapwalkingDebugger.getObjectReferences(
                "java.lang.ThreadGroup",
                vm);
        List<ObjectReference> threadsToFilter = HeapwalkingDebugger.getObjectReferences("java.lang.Thread", vm);

        pipe.println(referringObjects003a.COMMAND_START_THREADS + ":" + threadCount);

        if (!isDebuggeeReady())
            return;

        // check instance count
        checkDebugeeAnswer_instances("java.lang.ThreadGroup", threadGroupsToFilter.size() + 1);
        checkDebugeeAnswer_instances("java.lang.Thread", threadsToFilter.size() + threadCount);

        List<ObjectReference> threadGroups = HeapwalkingDebugger.filterObjectReferrence(
                threadGroupsToFilter,
                HeapwalkingDebugger.getObjectReferences("java.lang.ThreadGroup", vm));

        // thread group has 'threadCount' referrers + 1 referrer is parent
        // thread group
        // + 'includedIntoReferrersCountTypes.size()' referrers was additionally
        // created
        int expectedCount = threadCount + 1 + HeapwalkingDebuggee.includedIntoReferrersCountTypes.size();

        checkThreadGroupReferrersCount(threadGroups, expectedCount);

        List<ObjectReference> threads = HeapwalkingDebugger.filterObjectReferrence(threadsToFilter, HeapwalkingDebugger
                .getObjectReferences("java.lang.Thread", vm));

        expectedCount = 2 + HeapwalkingDebuggee.includedIntoReferrersCountTypes.size();

        // 1 referrer is debugee object + 1 referrer is thread group
        // + 'includedIntoReferrersCountTypes.size()' referrers was additionally
        // created
        checkThreadReferrersCount(threads, expectedCount);

        pipe.println(referringObjects003a.COMMAND_STOP_THREADS);

        if (!isDebuggeeReady())
            return;

        checkDebugeeAnswer_instances("java.lang.ThreadGroup", threadGroupsToFilter.size() + 1);
        checkDebugeeAnswer_instances("java.lang.Thread", threadsToFilter.size());

        threadGroups = HeapwalkingDebugger.filterObjectReferrence(threadGroupsToFilter, HeapwalkingDebugger
                .getObjectReferences("java.lang.ThreadGroup", vm));

        // 1 referrer(parent thread group) is left
        checkThreadGroupReferrersCount(threadGroups, 1);

        threads = HeapwalkingDebugger.filterObjectReferrence(threadsToFilter, HeapwalkingDebugger.getObjectReferences(
                "java.lang.Thread",
                vm));

        if (threads.size() != 0) {
            log.complain("All test threads should be removed");
            log.complain("Unexpected threads:");
            for (ObjectReference objectReference : threads) {
                log.complain(objectReference.toString());
            }
        }

        checkThreadGroupDisableCollection(threadGroups);
    }

    // can't force collection of thread group because of 1 reference is always
    // left in parent tread group
    public void checkThreadGroupDisableCollection(List<ObjectReference> objectReferences) {
        try {
            for (ObjectReference objectReference : objectReferences)
                objectReference.disableCollection();
        } catch (Throwable t) {
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        }

        forceGC();
        try {
            for (ObjectReference objectReference : objectReferences)
                objectReference.enableCollection();
        } catch (Throwable t) {
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        }

        forceGC();
        try {
            for (ObjectReference objectReference : objectReferences)
                objectReference.referringObjects(0);
        } catch (Throwable t) {
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        }
    }
}
