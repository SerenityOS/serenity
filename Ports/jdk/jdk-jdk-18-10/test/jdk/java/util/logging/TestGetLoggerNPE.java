/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintStream;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jdk.internal.access.JavaAWTAccess;
import jdk.internal.access.SharedSecrets;

/*
 * @test
 * @bug 8025512
 *
 * @summary NPE with logging while launching webstart
 *
 * @modules java.base/jdk.internal.access
 *          java.logging
 * @build TestGetLoggerNPE
 * @run main/othervm -Djava.security.manager=allow TestGetLoggerNPE getLogger
 * @run main/othervm -Djava.security.manager=allow TestGetLoggerNPE getLogManager
 */
public class TestGetLoggerNPE {
    static volatile Throwable thrown = null;
    public static void main(String[] args) throws Exception {
        final String testCase = args.length == 0 ? "getLogger" : args[0];
        final JavaAWTAccessStub access = new JavaAWTAccessStub();
        SharedSecrets.setJavaAWTAccess(access);
        final ThreadGroup tg = new ThreadGroup("TestGroup");
        Thread t = new Thread(tg, "test") {
            public void run() {
                try {
                    access.setContext(Context.ONE);
                    final PrintStream out = System.out;
                    System.setOut(null);
                    try {
                        if ("getLogger".equals(testCase)) {
                           Logger.getLogger("sun.plugin");
                        } else {
                           LogManager.getLogManager();
                        }
                    } finally {
                        System.setOut(out);
                    }

                    System.out.println(Logger.global);
                } catch (Throwable x) {
                    x.printStackTrace();
                    thrown = x;
                }
            }
        };
        Policy.setPolicy(new Policy() {
             public boolean implies(ProtectionDomain domain,
                                    Permission permission) {
                 return true; // all permissions
             }
        });
        System.setSecurityManager(new SecurityManager());
        t.start();
        t.join();
        if (thrown == null) {
            System.out.println("PASSED: " + testCase);
        } else {
            System.err.println("FAILED: " + testCase);
            throw new Error("Test failed: " + testCase + " - " + thrown, thrown);
        }

    }

    static enum Context { ONE, TWO };

    static final class JavaAWTAccessStub implements JavaAWTAccess {
        private static final InheritableThreadLocal<Context> context = new InheritableThreadLocal<>();


        public void setContext(Context context) {
            JavaAWTAccessStub.context.set(context);
        }

        @Override
        public Object getAppletContext() {
            return context.get();
        }

     }

}
