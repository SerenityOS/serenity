/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4168475 4520754
 * @summary Tests for the removal of some of the exception based control flow
 * @author Mark Davidson
 */

import infos.ComponentBeanInfo;

import java.awt.Button;
import java.awt.Component;
import java.awt.List;
import java.awt.Menu;
import java.awt.Panel;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;

public class Test4520754 {
    /**
     * This is here to force the BeanInfo classes to be compiled
     */
    private static final Class[] COMPILE = {
            ComponentBeanInfo.class,
            FooBarBeanInfo.class,
            WombatBeanInfo.class,
    };

    public static void main(String[] args) {
        // ensure that 4168475 does not regress
        test4168475(Component.class);
        // AWT classes (com.sun.beans.infos.ComponentBeanInfo)
        test(null, Button.class, Component.class, List.class, Menu.class, Panel.class);
        // Swing classes (dt.jar)
        test(null, JApplet.class, JButton.class, JCheckBox.class);
        // user defined classes
        test(Boolean.TRUE, Wombat.class, Foo.class, FooBar.class);
    }

    private static void test(Boolean mark, Class... types) {
        for (Class type : types) {
            BeanInfo info = getBeanInfo(mark, type);
            if (info == null) {
                throw new Error("could not find BeanInfo for " + type);
            }
            if (mark != info.getBeanDescriptor().getValue("test")) {
                throw new Error("could not find marked BeanInfo for " + type);
            }
        }
        Introspector.flushCaches();
    }

    private static BeanInfo getBeanInfo(Boolean mark, Class type) {
        System.out.println("test=" + mark + " for " + type);
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type);
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
        if (info == null) {
            throw new Error("could not find BeanInfo for " + type);
        }
        if (mark != info.getBeanDescriptor().getValue("test")) {
            throw new Error("could not find marked BeanInfo for " + type);
        }
        return info;
    }

    /**
     * This is a regression test to ensure that 4168475 does not regress.
     */
    private static void test4168475(Class type) {
        String[] newPath = {"infos"};
        String[] oldPath = Introspector.getBeanInfoSearchPath();

        Introspector.setBeanInfoSearchPath(newPath);
        BeanInfo info = getBeanInfo(Boolean.TRUE, type);
        Introspector.setBeanInfoSearchPath(oldPath);

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        if (pds.length != 1) {
            throw new Error("could not find custom BeanInfo for " + type);
        }
        Introspector.flushCaches();
    }
}
