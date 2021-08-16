/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/*
 * @test
 * @summary Basic test of proxy module mapping and the access to Proxy class
 * @modules java.base/sun.invoke
 */

public class ProxyModuleMapping {
    public static void main(String... args) throws Exception {
        ClassLoader ld = ProxyModuleMapping.class.getClassLoader();
        Module unnamed = ld.getUnnamedModule();
        new ProxyModuleMapping(Runnable.class).test();

        // unnamed module gets access to sun.invoke package (e.g. via --add-exports)
        new ProxyModuleMapping(sun.invoke.WrapperInstance.class).test();

        Class<?> modulePrivateIntf = Class.forName("sun.net.ProgressListener");
        new ProxyModuleMapping(modulePrivateIntf).test();
    }

    final Module target;
    final ClassLoader loader;
    final Class<?>[] interfaces;
    ProxyModuleMapping(Module m, Class<?>... interfaces) {
        this.target = m;
        this.loader = m.getClassLoader();
        this.interfaces = interfaces;
    }

    ProxyModuleMapping(Class<?>... interfaces) {
        this.target = null;  // expected to be dynamic module
        this.loader = interfaces[0].getClassLoader();   // same class loader
        this.interfaces = interfaces;
    }

    void test() throws Exception {
        verifyProxyClass();
        verifyNewProxyInstance();
    }

    void verifyProxyClass() throws Exception {
        Class<?> c = Proxy.getProxyClass(loader, interfaces);
        Module m = c.getModule();
        if (target != null && m != target) {
            throw new RuntimeException(c.getModule() + " not expected: " + target);
        }
        // expect dynamic module
        if (target == null && (!m.isNamed() || !m.getName().startsWith("jdk.proxy"))) {
            throw new RuntimeException("Unexpected:" + m);
        }

        Module module = c.getModule();
        try {
            Constructor<?> cons = c.getConstructor(InvocationHandler.class);
            cons.newInstance(ih);
            // the exported package name is same as the module name
            if (!c.getPackageName().equals(module.getName())) {
                throw new RuntimeException("expected IAE not thrown");
            }
        } catch (IllegalAccessException e) {
            // non-exported package from the dynamic module
            if (c.getPackageName().equals(module.getName())) {
                throw e;
            }
        }
    }

    void verifyNewProxyInstance() throws Exception {
        Object o = Proxy.newProxyInstance(loader, interfaces, ih);
        Module m = o.getClass().getModule();
        if (target != null && m != target) {
            throw new RuntimeException(m + " not expected: " + target);
        }
        if (target == null && (!m.isNamed() || !m.getName().startsWith("jdk.proxy"))) {
            throw new RuntimeException(m + " not expected: dynamic module");
        }
    }
    private final static InvocationHandler ih =
        (proxy, m, params) -> { System.out.println(m); return null; };
}
