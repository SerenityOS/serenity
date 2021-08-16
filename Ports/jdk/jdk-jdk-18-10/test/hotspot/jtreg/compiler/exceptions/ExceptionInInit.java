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
 */

/*
 * @test
 * @bug 4165973
 * @summary Attempt to read inaccessible property can produce
 *          exception of the wrong type.
 * @author Tom Rodriguez
 *
 * @modules java.rmi
 * @run main/othervm -Djava.security.manager=allow compiler.exceptions.ExceptionInInit
 */

package compiler.exceptions;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class ExceptionInInit {

    public static void main(String[] args) {

        Test test = null;

        try {
            System.setSecurityManager(new java.rmi.RMISecurityManager());
            Test.showTest();
        } catch (ExceptionInInitializerError e) {
        }
    }

    public static class FooBar {
        static String test = "test";
        FooBar(String test) {
            this.test = test;
        }
    }

    public static class Test extends FooBar {

        /*
         * An AccessControlException is thrown in the static initializer of the
         * class FooBar. This exception should produce an ExceptionInInitializer
         * error. Instead it causes a more cryptic ClassNotFound error.
         *
         * The following is an excerpt from the output from java.security.debug=all
         *
         * access: access denied (java.util.PropertyPermission test.src read)
         * java.lang.Exception: Stack trace
         *         at java.lang.Thread.dumpStack(Thread.java:938)
         *         at java.security.AccessControlContext.checkPermission(AccessControlContext.java:184)
         *         at java.security.AccessController.checkPermission(AccessController.java:402)
         *         at java.lang.SecurityManager.checkPermission(SecurityManager.java:516)
         *         at java.lang.SecurityManager.checkPropertyAccess(SecurityManager.java:1035)
         *         at java.lang.System.getProperty(System.java:441)
         *         at sun.security.action.GetPropertyAction.run(GetPropertyAction.java:73)
         *         at java.security.AccessController.doPrivileged(Native Method)
         *         at ExceptionInInit$Test.&#60clinit>(ExceptionInInit.java:33)
         *         at ExceptionInInit.main(ExceptionInInit.java:18)
         * access: domain that failed ProtectionDomain (file:/tmp/exceptionInInit/<no certificates>)
         *
         * The following exception is occurring when this test program tries
         * to access the test.src property.
         */
        private static String test =
            AccessController.doPrivileged((PrivilegedAction<String>)() -> System.getProperty("test.src", "."));

        Test(String test) {
            super(test);
        }
        public static void showTest() {
            System.err.println(test);
        }
    }
}
