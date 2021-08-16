/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import bean.Derived;
import java.lang.reflect.Method;
/*
 * @test
 * @bug 7084904
 * @summary Compares reflection and bean introspection
 * @library ..
 * @run main/othervm -Djava.security.manager=allow Test7084904
 * @author Sergey Malenkov
 */
public class Test7084904 {
    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new SecurityManager());
        Derived bean = new Derived();
        Class<?> type = bean.getClass();
        Method method1 = test("reflection", bean, type.getMethod("isAllowed"));
        Method method2 = test("bean introspection", bean, BeanUtils.getPropertyDescriptor(type, "allowed").getReadMethod());
        if (!method1.equals(method2)) {
            throw new Error("first method is not equal to the second one");
        }
    }

    private static Method test(String name, Object bean, Method method) throws Exception {
        System.out.println("\n === use " + name + " ===");
        System.out.println(method);
        System.out.println("declaring " + method.getDeclaringClass());
        System.out.println("invocation result: " + method.invoke(bean));
        return method;
    }
}
