/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4168475
 * @summary Tests that you can override BeanInfo for core classes
 * @author Graham Hamilton
 */

import infos.ComponentBeanInfo;

import java.awt.Component;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class Test4168475 {
    private static final Class CLASS = ComponentBeanInfo.class;
    private static final String[] PATH = {"infos"};

    public static void main(String[] args) throws IntrospectionException {
        Introspector.setBeanInfoSearchPath(PATH);
        BeanInfo info = Introspector.getBeanInfo(Component.class);
        PropertyDescriptor[] pds = info.getPropertyDescriptors();

        // The custom ComponentBeanInfo we deliver
        // only provides a single property.

        if (pds.length != 1) {
            throw new Error("wrong number of properties");
        }
        if (!pds[0].getName().equals("name")) {
            throw new Error("unexpected property name");
        }
    }
}
