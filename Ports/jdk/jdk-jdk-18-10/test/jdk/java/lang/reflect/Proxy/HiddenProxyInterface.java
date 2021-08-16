/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8250219
 * @run testng HiddenProxyInterface
 */

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class HiddenProxyInterface {
    private static final Path CLASSES_DIR = Paths.get(System.getProperty("test.classes"));

    interface Intf {
        void m();
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testHiddenInterface() throws Exception {
        Path classFile = CLASSES_DIR.resolve("HiddenProxyInterface$Intf.class");
        byte[] bytes = Files.readAllBytes(classFile);
        Class<?> hiddenIntf = MethodHandles.lookup().defineHiddenClass(bytes, false).lookupClass();
        Proxy.newProxyInstance(HiddenProxyInterface.class.getClassLoader(),
                new Class<?>[]{ hiddenIntf },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });
    }
}
