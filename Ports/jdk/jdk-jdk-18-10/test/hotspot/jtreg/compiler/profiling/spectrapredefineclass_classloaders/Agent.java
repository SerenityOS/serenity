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

package compiler.profiling.spectrapredefineclass_classloaders;

import com.sun.tools.attach.VirtualMachine;
import jdk.test.lib.Utils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

public class Agent implements ClassFileTransformer {
    public static final String AGENT_JAR = Paths.get(Utils.TEST_CLASSES, "agent.jar").toString();
    public static ClassLoader newClassLoader() {
        try {
            return new URLClassLoader(new URL[] {
                    Paths.get(Utils.TEST_CLASSES).toUri().toURL(),
            }, null);
        } catch (MalformedURLException e){
            throw new RuntimeException("Unexpected URL conversion failure", e);
        }
    }

    static public Class Test_class;

    static public void main(String[] args) throws Exception {

        // loader2 must be first on the list so loader 1 must be used first
        ClassLoader loader1 = newClassLoader();
        String packageName = Agent.class.getPackage().getName();
        Class dummy = loader1.loadClass(packageName + ".Test");

        ClassLoader loader2 = newClassLoader();

        Test_class = loader2.loadClass(packageName + ".Test");
        Method m3 = Test_class.getMethod("m3", ClassLoader.class);
        // Add speculative trap in m2() (loaded by loader1) that
        // references m4() (loaded by loader2).
        m3.invoke(Test_class.newInstance(), loader1);

        String pid = Long.toString(ProcessHandle.current().pid());

        // Make the nmethod go away
        for (int i = 0; i < 10; i++) {
            System.gc();
        }

        // Redefine class Test loaded by loader2
        for (int i = 0; i < 2; i++) {
            try {
                VirtualMachine vm = VirtualMachine.attach(pid);
                vm.loadAgent(AGENT_JAR, "");
                vm.detach();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // Will process loader2 first, find m4() is redefined and
        // needs to be freed then process loader1, check the
        // speculative trap in m2() and try to access m4() which was
        // freed already.
        for (int i = 0; i < 10; i++) {
            System.gc();
        }
    }

    public synchronized byte[] transform(final ClassLoader classLoader,
                                         final String className,
                                         Class<?> classBeingRedefined,
                                         ProtectionDomain protectionDomain,
                                         byte[] classfileBuffer) {
        System.out.println("Transforming class " + className + " "+ classLoader);
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

        redefine(agentArgs, instrumentation, Test_class);
    }
}
