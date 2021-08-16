/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
/*
 * @test
 * @bug 8080972
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 * @run main/othervm -Djava.security.manager=allow TestProxyLazyValue
 */

public class TestProxyLazyValue {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestProxyLazyValue::testUserProxyLazyValue);
        SwingUtilities.invokeAndWait(TestProxyLazyValue::testProxyLazyValue);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestProxyLazyValue::testUserProxyLazyValue);
        SwingUtilities.invokeAndWait(TestProxyLazyValue::testProxyLazyValue);
    }

    private static void testUserProxyLazyValue() {

        Object obj = new UserProxyLazyValue(
                UserLazyClass.class.getName()).createValue(null);

        if (!(obj instanceof UserLazyClass)) {
            throw new RuntimeException("Object is not UserLazyClass!");
        }

        obj = new UserProxyLazyValue(UserLazyClass.class.getName(),
                new Object[]{UserLazyClass.CONSTRUCTOR_ARG}).createValue(null);

        if (!(obj instanceof UserLazyClass)) {
            throw new RuntimeException("Object is not UserLazyClass!");
        }

        if (((UserLazyClass) obj).arg != UserLazyClass.CONSTRUCTOR_ARG) {
            throw new RuntimeException("Constructt argument is wrong!");
        }

        obj = new UserProxyLazyValue(UserLazyClass.class.getName(),
                "method1").createValue(null);

        if (!UserLazyClass.RESULT_1.equals(obj)) {
            throw new RuntimeException("Result is wrong!");
        }

        obj = new UserProxyLazyValue(UserLazyClass.class.getName(),
                "method2", new Object[]{UserLazyClass.RESULT_2}).createValue(null);

        if (!UserLazyClass.RESULT_2.equals(obj)) {
            throw new RuntimeException("Result is wrong!");
        }
    }

    private static void testProxyLazyValue() {

        Object obj = new UIDefaults.ProxyLazyValue(
                UserLazyClass.class.getName()).createValue(null);

        if (!(obj instanceof UserLazyClass)) {
            throw new RuntimeException("Object is not UserLazyClass!");
        }

        obj = new UIDefaults.ProxyLazyValue(UserLazyClass.class.getName(),
                new Object[]{UserLazyClass.CONSTRUCTOR_ARG}).createValue(null);

        if (!(obj instanceof UserLazyClass)) {
            throw new RuntimeException("Object is not UserLazyClass!");
        }

        if (((UserLazyClass) obj).arg != UserLazyClass.CONSTRUCTOR_ARG) {
            throw new RuntimeException("Constructt argument is wrong!");
        }

        obj = new UIDefaults.ProxyLazyValue(UserLazyClass.class.getName(),
                "method1").createValue(null);

        if (!UserLazyClass.RESULT_1.equals(obj)) {
            throw new RuntimeException("Result is wrong!");
        }

        obj = new UIDefaults.ProxyLazyValue(UserLazyClass.class.getName(),
                "method2", new Object[]{UserLazyClass.RESULT_2}).createValue(null);

        if (!UserLazyClass.RESULT_2.equals(obj)) {
            throw new RuntimeException("Result is wrong!");
        }
    }

    public static class UserLazyClass {

        static final int CONSTRUCTOR_ARG = 100;
        static final String RESULT_1 = "1";
        static final String RESULT_2 = "2";

        int arg;

        public UserLazyClass() {
        }

        public UserLazyClass(int arg) {
            this.arg = arg;
        }

        public static String method1() {
            return RESULT_1;
        }

        public static String method2(String arg) {
            return arg;
        }
    }

    public static class UserProxyLazyValue extends UIDefaults.ProxyLazyValue {

        public UserProxyLazyValue(String className) {
            super(className);
        }

        public UserProxyLazyValue(String className, Object[] constructorArgs) {
            super(className, constructorArgs);
        }

        public UserProxyLazyValue(String className, String methodName) {
            super(className, methodName);
        }

        public UserProxyLazyValue(String className, String methodName,
                Object[] methodArgs) {
            super(className, methodName, methodArgs);
        }
    }
}
