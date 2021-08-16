/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6263319
 * @requires ((vm.opt.StartFlightRecording == null) | (vm.opt.StartFlightRecording == false)) & ((vm.opt.FlightRecorder == null) | (vm.opt.FlightRecorder == false))
 * @summary test setNativeMethodPrefix
 * @author Robert Field, Sun Microsystems
 *
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.management
 *          java.instrument
 * @run shell/timeout=240 MakeJAR2.sh NativeMethodPrefixAgent NativeMethodPrefixApp 'Can-Retransform-Classes: true' 'Can-Set-Native-Method-Prefix: true'
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-CheckIntrinsics -javaagent:NativeMethodPrefixAgent.jar NativeMethodPrefixApp
 */

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.io.*;

import asmlib.*;

class NativeMethodPrefixAgent {

    static ClassFileTransformer t0, t1, t2;
    static Instrumentation inst;

    static class Tr implements ClassFileTransformer {
        final String trname;
        final int transformId;

        Tr(int transformId) {
            this.trname = "tr" + transformId;
            this.transformId = transformId;
        }

        public byte[]
        transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[] classfileBuffer) {
            boolean redef = classBeingRedefined != null;
            System.out.println(trname + ": " +
                               (redef? "Retransforming " : "Loading ") + className);
            if (className != null) {
                try {
                    byte[] newcf = Instrumentor.instrFor(classfileBuffer)
                                   .addNativeMethodTrackingInjection(
                                        "wrapped_" + trname + "_",
                                        (h)->{
                                            h.push(h.getName());
                                            h.push(transformId);
                                            h.invokeStatic("bootreporter/StringIdCallbackReporter", "tracker", "(Ljava/lang/String;I)V", false);
                                        })
                                   .apply();
                    /*** debugging ...
                    if (newcf != null) {
                        String fname = trname + (redef?"_redef" : "") + "/" + className;
                        System.err.println("dumping to: " + fname);
                        write_buffer(fname + "_before.class", classfileBuffer);
                        write_buffer(fname + "_instr.class", newcf);
                    }
                    ***/

                    return redef? null : newcf;
                } catch (Throwable ex) {
                    System.err.println("ERROR: Injection failure: " + ex);
                    ex.printStackTrace();
                    System.err.println("Returning bad class file, to cause test failure");
                    return new byte[0];
                }
            }
            return null;
        }

    }

    // for debugging
    static void write_buffer(String fname, byte[]buffer) {
        try {
            File f = new File(fname);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            try (FileOutputStream outStream = new FileOutputStream(f)) {
                outStream.write(buffer, 0, buffer.length);
            }
        } catch (IOException ex) {
            System.err.println("EXCEPTION in write_buffer: " + ex);
        }
    }

    public static void
    premain (String agentArgs, Instrumentation instArg)
        throws IOException, IllegalClassFormatException,
        ClassNotFoundException, UnmodifiableClassException {
        inst = instArg;
        System.out.println("Premain");

        t1 = new Tr(1);
        t2 = new Tr(2);
        t0 = new Tr(0);
        inst.addTransformer(t1, true);
        inst.addTransformer(t2, false);
        inst.addTransformer(t0, true);
        instArg.setNativeMethodPrefix(t0, "wrapped_tr0_");
        instArg.setNativeMethodPrefix(t1, "wrapped_tr1_");
        instArg.setNativeMethodPrefix(t2, "wrapped_tr2_");

        // warm up: cause load of transformer classes before used during class load
        instArg.retransformClasses(Runtime.class);
    }
}
