/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6277246
 * @summary Tests problem with java.beans use of reflection
 * @modules java.base/sun.security.x509
 *          java.desktop
 * @run main/othervm -Djava.security.manager=allow Test6277246
 * @author Jeff Nisewanger
 */

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;

public class Test6277246 {
    public static void main(String[] args) throws IntrospectionException {
        Class type = sun.security.x509.X509CertInfo.class;
        System.setSecurityManager(new SecurityManager());
        BeanInfo info = Introspector.getBeanInfo(type);
        for (MethodDescriptor md : info.getMethodDescriptors()) {
            Method method = md.getMethod();
            System.out.println(method);

            String name = method.getDeclaringClass().getName();
            if (name.startsWith("sun.")) {
                throw new Error("found inaccessible method");
            }
        }
    }
}
