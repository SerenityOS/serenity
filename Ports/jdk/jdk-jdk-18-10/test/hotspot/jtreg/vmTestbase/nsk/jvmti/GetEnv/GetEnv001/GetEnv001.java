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
 * @summary converted from VM Testbase nsk/jvmti/GetEnv/GetEnv001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 * The test performs a check whether JNI GetEnv function returns a valid JVMTI
 * interface instance.
 * The check consists of the following steps:
 *     1) specific version (1.1) of the JVMTI interface is aquired
 *     2) RetransformClasses event (introduced in 1.1) handler is enabled, so
 *        if the JVMTI interface doesn't support it, the test will fail
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:GetEnv001 nsk.jvmti.GetEnv.GetEnv001.GetEnv001
 */

package nsk.jvmti.GetEnv.GetEnv001;

import nsk.share.Consts;
import java.io.PrintStream;

public class GetEnv001 {
    native private static int getLoadedClassesCount();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // JCK-compatible exit
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** run test from JCK-compatible environment */
    public static int run(String args[], PrintStream out) {
        int loadedClasses = getLoadedClassesCount();

        if (loadedClasses > 0) {
            out.println("TEST PASSED. The number of loaded classes: "+loadedClasses);
            return Consts.TEST_PASSED;
        } else {
            out.println("TEST FAILED. The number of loaded classes: "+loadedClasses);
            return Consts.TEST_FAILED;
        }
    }
}
