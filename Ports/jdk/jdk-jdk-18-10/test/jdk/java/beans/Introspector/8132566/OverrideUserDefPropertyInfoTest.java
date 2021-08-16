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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.BeanProperty;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.JavaBean;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * @test
 * @bug 8132566 8132669 8131939
 * @summary Check if the derived class overrides
 *          the parent's user-defined property info.
 * @author a.stepanov
 */

public class OverrideUserDefPropertyInfoTest {

    private static final boolean baseFlag  = true;
    private static final boolean childFlag = false;

    @JavaBean(description = "CHILD")
    public static class C extends Base {

        private int x;

        @BeanProperty(
                bound        = childFlag,
                expert       = childFlag,
                hidden       = childFlag,
                preferred    = childFlag,
                required     = childFlag,
                visualUpdate = childFlag,
                description = "CHILDPROPERTY",
                enumerationValues = {"javax.swing.SwingConstants.BOTTOM"}
                )
        @Override
        public void setX(int v) { x = v; }
        @Override
        public  int getX() { return x; }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class Base {
        private int x;
        public void setX(int v) { x = v; }
        public  int getX() { return x; }
        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class BaseBeanInfo extends SimpleBeanInfo {

        @Override
        public BeanDescriptor getBeanDescriptor() {
            BeanDescriptor d = new BeanDescriptor(Base.class, null);
            d.setShortDescription("BASE");
            return d;
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            PropertyDescriptor[] p = new PropertyDescriptor[1];

            try {
                p[0] = new PropertyDescriptor ("x", Base.class, "getX", null);
                p[0].setShortDescription("BASEPROPERTY");
                p[0].setBound (baseFlag);
                p[0].setExpert(baseFlag);
                p[0].setHidden(baseFlag);
                p[0].setPreferred(baseFlag);
                p[0].setValue("required", baseFlag);
                p[0].setValue("visualUpdate", baseFlag);
                p[0].setValue("enumerationValues",
                    new Object[]{"TOP", 1, "javax.swing.SwingConstants.TOP"});
            } catch(IntrospectionException e) { e.printStackTrace(); }

            return p;
        }

        @Override
        public EventSetDescriptor[] getEventSetDescriptors() { return new EventSetDescriptor[0]; }
        @Override
        public MethodDescriptor[] getMethodDescriptors() {
            MethodDescriptor[] m = new MethodDescriptor[1];
            try {
                m[0] = new MethodDescriptor(Base.class.getMethod("setX", new Class[] {int.class}));
                m[0].setDisplayName("");
            }
            catch( NoSuchMethodException | SecurityException e) {}
            return m;
        }

        @Override
        public int getDefaultPropertyIndex() { return -1; }
        @Override
        public int getDefaultEventIndex() { return -1; }
        @Override
        public java.awt.Image getIcon(int iconKind) { return null; }
    }

    public static void main(String[] args) throws Exception {

        BeanInfo i = Introspector.getBeanInfo(C.class, Object.class);
        Checker.checkEq("description",
            i.getBeanDescriptor().getShortDescription(), "CHILD");

        PropertyDescriptor[] pds = i.getPropertyDescriptors();
        Checker.checkEq("number of properties", pds.length, 1);
        PropertyDescriptor p = pds[0];

        Checker.checkEq("property description", p.getShortDescription(), "CHILDPROPERTY");
        Checker.checkEq("isBound",  p.isBound(),  childFlag);
        Checker.checkEq("isExpert", p.isExpert(), childFlag);
        Checker.checkEq("isHidden", p.isHidden(), childFlag);
        Checker.checkEq("isPreferred", p.isPreferred(), childFlag);
        Checker.checkEq("required", p.getValue("required"), childFlag);
        Checker.checkEq("visualUpdate", p.getValue("visualUpdate"), childFlag);

        Checker.checkEnumEq("enumerationValues", p.getValue("enumerationValues"),
            new Object[]{"BOTTOM", 3, "javax.swing.SwingConstants.BOTTOM"});
    }
}
