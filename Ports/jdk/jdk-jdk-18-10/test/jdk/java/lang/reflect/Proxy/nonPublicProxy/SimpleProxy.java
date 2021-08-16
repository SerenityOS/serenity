/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.security.*;
import java.util.Arrays;

/*
 * @test
 * @bug 8004260
 * @summary Test making a proxy instance that implements a non-public
 *          interface with and without security manager installed
 * @build p.Foo p.Bar
 * @run main/othervm -Djava.security.manager=allow SimpleProxy
 */
public class SimpleProxy {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = SimpleProxy.class.getClassLoader();
        Class<?> fooClass = Class.forName("p.Foo");
        Class<?> barClass = Class.forName("p.Bar");

        makeProxy(loader, fooClass);

        System.setSecurityManager(new SecurityManager());
        try {
            makeProxy(loader, barClass);
            throw new RuntimeException("should fail to new proxy instance of a non-public interface");
        } catch (AccessControlException e) {
            if (e.getPermission().getClass() != ReflectPermission.class ||
                    !e.getPermission().getName().equals("newProxyInPackage.p")) {
                throw e;
            }
        }
    }

    private static void makeProxy(ClassLoader loader, Class<?> cls) {
        Class<?>[] intfs = new Class<?>[] { cls };
        Proxy.newProxyInstance(loader, intfs, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                Class<?>[] intfs = proxy.getClass().getInterfaces();
                System.out.println("Proxy for " + Arrays.toString(intfs)
                        + " " + method.getName() + " is being invoked");
                return null;
            }
        });
    }
}
