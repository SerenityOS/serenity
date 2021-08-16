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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/referringObjects/referringObjects004.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test checks behavior of ObjectReference.referringObjects() in case when there are
 *     many (several thousands) referring objects for corresponding ObjectReference.
 *     Debugger forces debuggee create instance of 'nsk.share.jdi.TestClass1' with 100000 (this value can be changed through parameter -referrerCount)
 *     referrers, obtains ObjectReference for this instance and checks that ObjectReference.referringObjects()
 *     returns collection with correct size and returned instances doesn't throw any exception when call following methods:
 *         referenceType()
 *         isCollected()
 *         uniqueID()
 *         hashCode()
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.referringObjects.referringObjects004.referringObjects004
 * @run main/othervm/native
 *      nsk.jdi.ObjectReference.referringObjects.referringObjects004.referringObjects004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ObjectReference.referringObjects.referringObjects004;

import java.io.PrintStream;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.ObjectInstancesManager;
import nsk.share.ReferringObject;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jdi.TestClass1;

public class referringObjects004 extends HeapwalkingDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new referringObjects004().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return HeapwalkingDebuggee.class.getName();
    }

    private int referrerCount = 100000;

    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-referrerCount") && (i < args.length - 1)) {
                referrerCount = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    public void doTest() {
        String className = TestClass1.class.getName();

        pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + className + ":" + 1 + ":" + referrerCount
                + ":" + ObjectInstancesManager.STRONG_REFERENCE);

        if (!isDebuggeeReady())
            return;

        ObjectReference objectReference = findSingleObjectReference(className);

        if (objectReference.referringObjects(0).size() != referrerCount) {
            setSuccess(false);
            log.complain("List with wrong size was returned by ObjectReference.referringObjects: "
                    + objectReference.referringObjects(0).size() + ", expected: " + referrerCount);
        }

        List<ObjectReference> referrers = objectReference.referringObjects(0);

        ReferenceType referrerReferenceType = debuggee.classByName(ReferringObject.class.getName());

        vm.suspend();

        for (ObjectReference referrer : referrers) {
            try {
                if (!referrerReferenceType.equals(referrer.referenceType())) {
                    setSuccess(false);
                    log.complain("Referrer's ReferenceType " + referrer.referenceType() + " doesn't equal "
                            + referrerReferenceType);
                }
                if (referrer.isCollected()) {
                    setSuccess(false);
                    log.complain("isCollected() returns 'true' for " + referrer);
                }
                referrer.uniqueID();
                referrer.hashCode();
            } catch (Throwable t) {
                setSuccess(false);
                t.printStackTrace(log.getOutStream());
                log.complain("Unexpected exception: " + t);
            }
        }

        vm.resume();
    }
}
