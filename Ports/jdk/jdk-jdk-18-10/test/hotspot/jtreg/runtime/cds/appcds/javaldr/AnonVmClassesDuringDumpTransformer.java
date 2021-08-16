/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class AnonVmClassesDuringDumpTransformer implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String name, Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] buffer) throws IllegalClassFormatException {
        return null;
    }

    private static Instrumentation savedInstrumentation;

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        System.out.println("ClassFileTransformer.premain() is called");
        instrumentation.addTransformer(new AnonVmClassesDuringDumpTransformer(), /*canRetransform=*/true);
        savedInstrumentation = instrumentation;

        // This will create a Lambda, which will result in some Anonymous VM Classes
        // being generated.
        //
        // Look for something like these in the STDOUT:
        // ----------------
        // ClassFileTransformer.premain() is called
        // Dumping class files to DUMP_CLASS_FILES/...
        // dump: DUMP_CLASS_FILES/java/lang/invoke/LambdaForm$MH000.class
        // dump: DUMP_CLASS_FILES/java/lang/invoke/LambdaForm$MH001.class
        // Invoked inside a Lambda
        // ----------------
        Runnable r = () -> {
            System.out.println("Invoked inside a Lambda");
        };
        r.run();
    }

    public static Instrumentation getInstrumentation() {
        return savedInstrumentation;
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        premain(args, inst);
    }
}
