/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4750368
 * @summary Tests for class name collision in Introspector
 * @author Mark Davidson
 */

import java.awt.Component;
import java.awt.List;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class Test4750368 {
    public static void main(String[] args) throws IntrospectionException {
        if (getLength(com.foo.test.Component.class) == getLength(Component.class)) {
            throw new Error("test failed for Component");
        }
        if (getLength(java.util.List.class) == getLength(List.class)) {
            throw new Error("test failed for List");
        }
    }

    private static int getLength(Class type) throws IntrospectionException {
        PropertyDescriptor[]  pds = Introspector.getBeanInfo(type).getPropertyDescriptors();
        System.out.println(type + ": " + pds.length);
        for (PropertyDescriptor pd : pds) {
            System.out.println(" - " + pd.getName());
        }
        return pds.length;
    }
}
