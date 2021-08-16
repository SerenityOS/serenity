/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.profiling.spectrapredefineclass;

import com.sun.tools.attach.VirtualMachine;
import jdk.test.lib.Utils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

class A {
    void m() {
    }
}

class B extends A {
    void m() {
    }
}

class C extends A {
    void m() {
    }
}

class Test {

    static public void m() throws Exception {
        for (int i = 0; i < 20000; i++) {
            m1(a);
        }
        for (int i = 0; i < 4; i++) {
            m1(b);
        }
    }

    static boolean m1(A a) {
        boolean res =  Agent.m2(a);
        return res;
    }

    static public A a = new A();
    static public B b = new B();
    static public C c = new C();
}

public class Agent implements ClassFileTransformer {
    public static final String AGENT_JAR = Paths.get(Utils.TEST_CLASSES, "agent.jar").toString();
    static public boolean m2(A a) {
        boolean res = false;
        if (a.getClass() == B.class) {
            a.m();
        } else {
            res = true;
        }
        return res;
    }

    static public void main(String[] args) throws Exception {
        // Create speculative trap entries
        Test.m();

        String pid = Long.toString(ProcessHandle.current().pid());

        // Make the nmethod go away
        for (int i = 0; i < 10; i++) {
            System.gc();
        }

        // Redefine class
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(AGENT_JAR, "");
            vm.detach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Test.m();
        // GC will hit dead method pointer
        for (int i = 0; i < 10; i++) {
            System.gc();
        }
    }

    public synchronized byte[] transform(final ClassLoader classLoader,
                                         final String className,
                                         Class<?> classBeingRedefined,
                                         ProtectionDomain protectionDomain,
                                         byte[] classfileBuffer) {
        System.out.println("Transforming class " + className);
        return classfileBuffer;
    }

    public static void redefine(String agentArgs, Instrumentation instrumentation, Class to_redefine) {

        try {
            instrumentation.retransformClasses(to_redefine);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        Agent transformer = new Agent();
        instrumentation.addTransformer(transformer, true);

        redefine(agentArgs, instrumentation, Test.class);
    }
}
