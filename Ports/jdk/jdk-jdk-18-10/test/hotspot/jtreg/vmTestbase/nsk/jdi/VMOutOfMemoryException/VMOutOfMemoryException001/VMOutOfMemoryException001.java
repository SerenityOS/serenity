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
 * @summary converted from VM Testbase nsk/jdi/VMOutOfMemoryException/VMOutOfMemoryException001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test provokes 'VMOutOfMemoryException'.
 *     Test scenario:
 *     Debugger obtains 'ArrayType' instance for int array and in endless loop instantiates arrays
 *     in debuggee VM through 'ArrayType.newInstance()' till VMOutOfMemoryException. Any other exception
 *     thrown by 'ArrayType.newInstance()' is treated as product bug.
 *
 * @requires !vm.graal.enabled
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VMOutOfMemoryException.VMOutOfMemoryException001.VMOutOfMemoryException001
 *        nsk.jdi.VMOutOfMemoryException.VMOutOfMemoryException001.VMOutOfMemoryException001t
 * @run main/othervm
 *      nsk.jdi.VMOutOfMemoryException.VMOutOfMemoryException001.VMOutOfMemoryException001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.VMOutOfMemoryException.VMOutOfMemoryException001;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.*;

public class VMOutOfMemoryException001 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new VMOutOfMemoryException001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return VMOutOfMemoryException001t.class.getName();
    }

    public void doTest() {
        ArrayType referenceType = (ArrayType) debuggee.classByName("int[]");

        try {
            List<ArrayReference> objects = new ArrayList<ArrayReference>();

            // create array in debuggee VM till VMOutOfMemoryException
            while (true) {
                ArrayReference array = referenceType.newInstance(100000);
                try {
                    // Since the VM is not suspended, the object may have been collected
                    // before disableCollection() could be called on it. Just ignore and
                    // continue doing allocations until we run out of memory.
                    array.disableCollection();
                } catch (ObjectCollectedException e) {
                    continue;
                }
                objects.add(array);
            }
        } catch (VMOutOfMemoryException e) {
            // expected exception
            log.display("Got expected exception: " + e.toString());
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception(VMOutOfMemoryException was expected): " + t);
            t.printStackTrace(log.getOutStream());
        }
    }
}
