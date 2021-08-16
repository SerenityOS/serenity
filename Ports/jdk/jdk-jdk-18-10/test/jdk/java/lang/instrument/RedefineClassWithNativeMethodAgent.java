/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Timer;
import java.util.TimerTask;

public class RedefineClassWithNativeMethodAgent {
    static Class clz;

    // just read the original class and redefine it via a Timer
    public static void premain(String agentArgs, final Instrumentation inst) throws Exception {
        String s = agentArgs.substring(0, agentArgs.indexOf(".class"));
        clz = Class.forName(s.replace('/', '.'));
        InputStream in;
        Module m = clz.getModule();
        if (m != null) {
            in = m.getResourceAsStream(agentArgs);
        } else {
            ClassLoader loader =
                RedefineClassWithNativeMethodAgent.class.getClassLoader();
            in = loader.getResourceAsStream(agentArgs);
        }
        if (in == null) {
            throw new Exception("Cannot find class: " + agentArgs);
        }
        byte[] buffer = in.readAllBytes();

        new Timer(true).schedule(new TimerTask() {
            public void run() {
                try {
                    System.out.println("Instrumenting");
                    ClassDefinition cld = new ClassDefinition(clz, buffer);
                    inst.redefineClasses(new ClassDefinition[] { cld });
                }
                catch (Exception e) { e.printStackTrace(); }
            }
        }, 500);
    }
}
