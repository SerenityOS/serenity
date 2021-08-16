/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/RetransformClasses/retransform003.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 * This test is designed to check, whether the invocation order of retransformation agents
 * are correct or not. Transformation capable and incapable agents are mixed together.
 * The specification defines the following behavior as correct:
 * When classes are initially loaded or when they are redefined, the initial class file
 * bytes can be transformed with the ClassFileLoadHook event. This function reruns the
 * transformation process (whether or not a transformation has previously occurred).
 * This retransformation follows these steps:
 *     * starting from the initial class file bytes
 *     * for each retransformation incapable agent which received a ClassFileLoadHook
 *       event during the previous load or redefine, the bytes it returned (via the
 *       new_class_data parameter) are reused as the output of the transformation;
 *       note that this is equivalent to reapplying the previous transformation,
 *       unaltered. except that the ClassFileLoadHook event is not sent to these agents
 *       * for each retransformation capable agent, the ClassFileLoadHook event is sent, allowing a new transformation to be applied
 *      * the transformed class file bytes are installed as the new definition of the class
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.jvmti.RetransformClasses.LinearHierarchy.Class1
 *
 * @comment duplicate retransform003 in current directory
 * @run driver nsk.jvmti.NativeLibraryCopier
 *      retransform003
 *      retransform003-01
 *      retransform003-02
 *      retransform003-03
 *
 * @run main/othervm/native TestDriver
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.util.Arrays;

public class TestDriver {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-agentlib:retransform003-01=id=1 can_retransform_classes=1",
                "-agentlib:retransform003-02=id=2 can_retransform_classes=0",
                "-agentlib:retransform003-03=id=3 can_retransform_classes=1",
                nsk.jvmti.RetransformClasses.retransform003.class.getName()
        );

        String envName = Platform.sharedLibraryPathVariableName();
        pb.environment()
          .merge(envName, ".", (x, y) -> y + File.pathSeparator + x);

        String command = Arrays.toString(args);
        System.out.println("exec " + command);
        pb.inheritIO();

        int exitCode = pb.start().waitFor();

        if (exitCode != 95 && exitCode !=0 ) {
            throw new AssertionError(command + " exit code is " + exitCode);
        }
    }
}

