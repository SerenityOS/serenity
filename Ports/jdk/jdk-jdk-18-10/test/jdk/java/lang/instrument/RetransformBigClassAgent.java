/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.*;
import java.util.*;

public class RetransformBigClassAgent {
    private static int N_RETRANSFORMS = 1000;
    public static Class clz;
    public static volatile boolean doneRetransforming = false;

    // just retransform the class in a loop via a Timer
    public static void premain(String agentArgs, final Instrumentation inst) throws Exception {
        String s = agentArgs.substring(0, agentArgs.indexOf(".class"));
        clz = Class.forName(s.replace('/', '.'));

        ClassFileTransformer trans = new SimpleIdentityTransformer();
        inst.addTransformer(trans, true /* canRetransform */);

        System.gc();  // throw away anything we can before we start testing

        new Timer(true).schedule(new TimerTask() {
            public void run() {
                try {
                    int i;
                    System.out.println("Retransforming");
                    for (i = 0; i < N_RETRANSFORMS; i++) {
                        inst.retransformClasses(clz);
                        System.gc();  // throw away anything we can
                    }
                    System.out.println("Retransformed " + i + " times.");
                    RetransformBigClassAgent.doneRetransforming = true;
                }
                catch (Exception e) { e.printStackTrace(); }
            }
        }, 500);
    }
}
