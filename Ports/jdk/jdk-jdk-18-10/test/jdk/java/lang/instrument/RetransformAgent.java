/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6274264 6274241 5070281
 * @summary test retransformClasses
 * @author Robert Field, Sun Microsystems
 *
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.instrument
 * @run shell/timeout=240 MakeJAR2.sh RetransformAgent RetransformApp 'Can-Retransform-Classes: true'
 * @run main/othervm -javaagent:RetransformAgent.jar RetransformApp
 */

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.io.*;
import asmlib.*;

class RetransformAgent {

    static ClassFileTransformer t1, t2, t3, t4;
    static Instrumentation inst;
    static boolean succeeded = true;
    static int markCount = 0;
    static int[] markGolden = {30, 40, 20, 30, 40, 20, 30, 40, 20, 30, 40, 20,
                               11, 40, 20, 11, 40, 20, 11, 40, 20, 11, 40, 20};

    static class Tr implements ClassFileTransformer {
        final String trname;
        final boolean onLoad;
        final int loadIndex;
        final boolean onRedef;
        final int redefIndex;
        final String cname;
        final String nname;

        Tr(String trname, boolean onLoad, int loadIndex, boolean onRedef, int redefIndex,
           String cname, String nname) {
            this.trname = trname;
            this.onLoad = onLoad;
            this.loadIndex = loadIndex;
            this.onRedef = onRedef;
            this.redefIndex = redefIndex;
            this.cname = cname;
            this.nname = nname;
        }

        public byte[] transform(Module module,
                                ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain    protectionDomain,
                                byte[] classfileBuffer) {
            boolean redef = classBeingRedefined != null;

            // System.err.println("hook " + trname + ": " + className +
            //                    (redef? " REDEF" : " LOAD"));
            if ((redef? onRedef : onLoad) && className != null && className.equals(cname)) {
                int fixedIndex = redef ? redefIndex : loadIndex;
                try {
                    byte[] newcf = Instrumentor.instrFor(classfileBuffer)
                                   .addMethodEntryInjection(
                                        nname,
                                        (h)->{
                                           h.push(fixedIndex);
                                           h.invokeStatic("RetransformAgent", "callTracker", "(I)V", false);
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
                    System.err.println(trname + ": " + className + " index: " + fixedIndex +
                                       (redef? " REDEF" : " LOAD") +
                                       " len before: " + classfileBuffer.length +
                                       " after: " + newcf.length);
                    return newcf;
                } catch (Throwable ex) {
                    System.err.println("Injection failure: " + ex);
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

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

    public static void premain (String agentArgs, Instrumentation instArg) {
        inst = instArg;
        System.err.println("Premain");

        t1 = new Tr("TR1", false, 10, true, 11, "RetransformApp", "foo");
        inst.addTransformer(t1, true);
        t2 = new Tr("TN2", true,  20, true, 21, "RetransformApp", "foo");
        inst.addTransformer(t2, false);
        t3 = new Tr("TR3", true,  30, true, 31, "RetransformApp", "foo");
        inst.addTransformer(t3, true);
        t4 = new Tr("TN4", true,  40, true, 41, "RetransformApp", "foo");
        inst.addTransformer(t4, false);
    }

    public static void undo() {
        inst.removeTransformer(t3);
        try {
            System.err.println("RETRANSFORM");
            inst.retransformClasses(new RetransformApp().getClass());
        } catch (Exception ex) {
            System.err.println("EXCEPTION on undo: " + ex);
        }
    }

    public static boolean succeeded() {
        return succeeded && markCount == markGolden.length;
    }

    public static void callTracker(int mark) {
        System.err.println("got mark " + mark);
        if (markCount >= markGolden.length || mark != markGolden[markCount++]) {
            succeeded = false;
        }
    }
}
