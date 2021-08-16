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


import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.BeanProperty;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.JavaBean;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import javax.swing.SwingContainer;
import java.util.Arrays;

/**
 * @test
 * @bug 4058433 8131055
 * @summary Check if the user-defined bean info
 *          is not overridden with the annotated one.
 * @author a.stepanov
 */


public class TestBeanInfoPriority {

    // ========== test bean (annotations must be ignored!) ==========

    @JavaBean(
            description = "annotation-description",
            defaultProperty = "other",
            defaultEventSet = "mouse")
    @SwingContainer(value = false)
    public static class TestClass {

        private int    value;
        private double other;

        @BeanProperty(
                bound     = false,
                expert    = false,
                hidden    = false,
                preferred = false,
                required  = false,
                visualUpdate = false,
                description = "annotation-value",
                enumerationValues = {
                    "javax.swing.SwingConstants.NORTH"}
                )
        public void setValue(int v) { value = v; }
        public  int getValue()      { return value; }


        @BeanProperty(
            bound     = true,
            expert    = true,
            hidden    = true,
            preferred = true,
            required  = true,
            visualUpdate = true,
            description = "annotation-other",
            enumerationValues = {
                "javax.swing.SwingConstants.LEFT",
                "javax.swing.SwingConstants.RIGHT",
                "javax.swing.SwingConstants.CENTER"}
            )
        public   void setOther(double o) { other = o; }
        public double getOther()         { return other; }

        public void addActionListener(ActionListener l) {}
        public void removeActionListener(ActionListener l) {}

        public void addMouseListener(MouseListener l) {}
        public void removeMouseListener(MouseListener l) {}
    }

    // ========== user-defined bean info ==========

    public static class TestClassBeanInfo extends SimpleBeanInfo {

        private static final int iOther = 0;
        private static final int iValue = 1;

        private static final int iAction = 0;
        private static final int iMouse  = 1;


        @Override
        public BeanDescriptor getBeanDescriptor() {

            BeanDescriptor bd = new BeanDescriptor(TestClass.class, null);
            bd.setShortDescription("user-defined-description");
            bd.setValue("isContainer", true);
            bd.setValue("containerDelegate", "user-defined-delegate");

            return bd;
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {

            PropertyDescriptor[] p = new PropertyDescriptor[2];

            try {

                // value
                PropertyDescriptor pdValue = new PropertyDescriptor(
                    "value", TestClass.class, "getValue", "setValue");
                pdValue.setBound(true);
                pdValue.setConstrained(true);
                pdValue.setExpert(true);
                pdValue.setHidden(true);
                pdValue.setPreferred(true);
                pdValue.setValue("required", true);
                pdValue.setValue("visualUpdate", true);
                pdValue.setShortDescription("user-defined-value");
                pdValue.setValue("enumerationValues", new Object[]{
                        "EAST", 3, "javax.swing.SwingConstants.EAST",
                        "WEST", 7, "javax.swing.SwingConstants.WEST"});
                p[iValue] = pdValue;

                // other
                PropertyDescriptor pdOther = new PropertyDescriptor(
                        "other", TestClass.class, "getOther", "setOther");
                pdOther.setBound(false);
                pdOther.setConstrained(false);
                pdOther.setExpert(false);
                pdOther.setHidden(false);
                pdOther.setPreferred(false);
                pdOther.setValue("required", false);
                pdOther.setValue("visualUpdate", false);
                pdOther.setShortDescription("user-defined-other");
                pdOther.setValue("enumerationValues", new Object[]{
                        "TOP", 1, "javax.swing.SwingConstants.TOP"});
                p[iOther] = pdOther;

            } catch(IntrospectionException e) {
                e.printStackTrace();
            }

            return p;
        }

        @Override
        public EventSetDescriptor[] getEventSetDescriptors() {
            EventSetDescriptor[] es = new EventSetDescriptor[2];
            try {
                es[iAction] = new EventSetDescriptor(
                        TestClass.class,
                        "actionListener",
                        java.awt.event.ActionListener.class,
                        new String[] {"actionPerformed"},
                        "addActionListener",
                        "removeActionListener");
                es[iMouse] = new EventSetDescriptor(
                        TestClass.class,
                        "mouseListener",
                        java.awt.event.MouseListener.class,
                        new String[] {"mouseClicked", "mousePressed", "mouseReleased", "mouseEntered", "mouseExited"},
                        "addMouseListener",
                        "removeMouseListener");
            } catch(IntrospectionException e) {
                e.printStackTrace();
            }
            return es;
        }

        @Override
        public MethodDescriptor[] getMethodDescriptors() {
            MethodDescriptor[] m = new MethodDescriptor[0];
            return m;
        }

        @Override
        public int getDefaultPropertyIndex() { return iValue; } // default: value

        @Override
        public int getDefaultEventIndex() { return iAction; } // default: action

        @Override
        public java.awt.Image getIcon(int iconKind) { return null; }
    }

    // ========== auxiliary functions ==========

    static void checkEq(String what, Object v, Object ref) throws Exception {

        if ((v != null) && v.equals(ref)) {
            System.out.println(what + ": ok (" + ref.toString() + ")");
        } else {
            throw new Exception(
                "invalid " + what + ", expected: \"" + ref + "\", got: \"" + v + "\"");
        }
    }

    static void checkEnumEq(String what, Object v, Object ref[]) throws Exception {

        what = "\"" + what + "\"";
        if (v == null) {
            throw new Exception("null " + what + " enumeration values");
        }

        String msg = "invalid " + what + " enumeration values";
        if (!(v instanceof Object[])) { throw new Exception(msg); }

        if (Arrays.equals((Object []) v, ref)) {
            System.out.println(what + " enumeration values: ok");
        } else { throw new Exception(msg); }
    }


    // ========== test ==========


    public static void main(String[] args) throws Exception {

        BeanInfo i = Introspector.getBeanInfo(TestClass.class, Object.class);
        BeanDescriptor bd = i.getBeanDescriptor();

        checkEq("description", bd.getShortDescription(), "user-defined-description");
        checkEq("default property index", i.getDefaultPropertyIndex(), 1);
        checkEq("default event index", i.getDefaultEventIndex(), 0);

        checkEq("isContainer", i.getBeanDescriptor().getValue("isContainer"), true);
        checkEq("containerDelegate",
            i.getBeanDescriptor().getValue("containerDelegate"), "user-defined-delegate");
        System.out.println("");

        PropertyDescriptor[] pds = i.getPropertyDescriptors();
        for (PropertyDescriptor pd: pds) {
            String name = pd.getName();
            switch (name) {
                case "value":
                    checkEq("\"value\" isBound",       pd.isBound(),       true);
                    checkEq("\"value\" isConstrained", pd.isConstrained(), true);
                    checkEq("\"value\" isExpert",      pd.isExpert(),      true);
                    checkEq("\"value\" isHidden",      pd.isHidden(),      true);
                    checkEq("\"value\" isPreferred",   pd.isPreferred(),   true);
                    checkEq("\"value\" required",      pd.getValue("required"),     true);
                    checkEq("\"value\" visualUpdate",  pd.getValue("visualUpdate"), true);

                    checkEq("\"value\" description",   pd.getShortDescription(), "user-defined-value");

                    checkEnumEq(pd.getName(), pd.getValue("enumerationValues"),
                        new Object[]{
                        "EAST", 3, "javax.swing.SwingConstants.EAST",
                        "WEST", 7, "javax.swing.SwingConstants.WEST"});
                    System.out.println("");
                    break;
                case "other":
                    checkEq("\"other\" isBound",       pd.isBound(),       false);
                    checkEq("\"other\" isConstrained", pd.isConstrained(), false);
                    checkEq("\"other\" isExpert",      pd.isExpert(),      false);
                    checkEq("\"other\" isHidden",      pd.isHidden(),      false);
                    checkEq("\"other\" isPreferred",   pd.isPreferred(),   false);
                    checkEq("\"other\" required",      pd.getValue("required"),     false);
                    checkEq("\"other\" visualUpdate",  pd.getValue("visualUpdate"), false);

                    checkEq("\"other\" description",   pd.getShortDescription(), "user-defined-other");

                    checkEnumEq(pd.getName(), pd.getValue("enumerationValues"),
                        new Object[]{"TOP", 1, "javax.swing.SwingConstants.TOP"});
                    System.out.println("");
                    break;
                default:
                    throw new Exception("invalid property descriptor: " + name);
            }
        }
    }
}
