/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.BeanProperty;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

/**
 * @test
 * @bug 4058433
 * @summary Tests the BeanProperty annotation
 * @author Sergey Malenkov
 * @library ..
 */
public class TestBeanProperty {
    public static void main(String[] args) throws Exception {
        Class<?>[] types =
                {B.class, BL.class, BLF.class, E.class, H.class, P.class,
                 VU.class, D.class, EVD.class, EVE.class, EV.class, EVL.class,
                 EVX.class, R.class};
        for (Class<?> type : types) {
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, "value");
            if (((B.class == type) || (BLF.class == type)) && pd.isBound()) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("not bound");
            }
            if ((BL.class == type) == !pd.isBound()) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("bound");
            }
            if ((E.class == type) == !pd.isExpert()) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("expert");
            }
            if ((H.class == type) == !pd.isHidden()) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("hidden");
            }
            if ((P.class == type) == !pd.isPreferred()) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("preferred");
            }
            if ((R.class == type) == !Boolean.TRUE.equals(pd.getValue("required"))) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("required");
            }
            if ((D.class == type) == !"getter".equals(pd.getShortDescription())) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("shortDescription");
            }
            if ((VU.class == type) == !Boolean.TRUE.equals(pd.getValue("visualUpdate"))) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("visualUpdate");
            }
            if ((EV.class == type) == !isEV(pd, "LEFT", 2, "javax.swing.SwingConstants.LEFT", "RIGHT", 4, "javax.swing.SwingConstants.RIGHT")) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("enumerationValues from another package");
            }
            if ((EVL.class == type) == !isEV(pd, "ZERO", 0, "ZERO", "ONE", 1, "ONE")) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("enumerationValues from another package");
            }
            if ((EVX.class == type) == !isEV(pd, "ZERO", 0, "X.ZERO", "ONE", 1, "X.ONE")) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("enumerationValues from another package");
            }
            if (EVD.class == type && !isEV(pd)) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("EV:"+ pd.getValue("enumerationValues"));
            }
            if (EVE.class == type && !isEV(pd)) {
                BeanUtils.reportPropertyDescriptor(pd);
                throw new Error("EV:"+ pd.getValue("enumerationValues"));
            }
        }
    }

    private static boolean isEV(PropertyDescriptor pd, Object... expected) {
        Object value = pd.getValue("enumerationValues");
        return value instanceof Object[] && Arrays.equals((Object[]) value, expected);
    }

    public static class B {
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class BL {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
        }
    }

    public static class BLF {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(bound = false)
        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
        }
    }

    public static class E {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(expert = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class H {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(hidden = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class P {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(preferred = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class R {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(required = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class VU {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(visualUpdate = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class D {
        private int value;

        @BeanProperty(description = "getter")
        public int getValue() {
            return this.value;
        }

        @BeanProperty(description = "setter")
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class EVD {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty()
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class EVE {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(enumerationValues = {})
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class EV {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(enumerationValues = {
                "javax.swing.SwingConstants.LEFT",
                "javax.swing.SwingConstants.RIGHT"})
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class EVL {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(enumerationValues = {"ZERO", "ONE"})
        public void setValue(int value) {
            this.value = value;
        }

        public static int ZERO = 0;
        public static int ONE = 1;
    }

    public static class EVX {
        private int value;

        public int getValue() {
            return this.value;
        }

        @BeanProperty(enumerationValues = {
                "X.ZERO",
                "X.ONE"})
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static interface X {
        int ZERO = 0;
        int ONE = 1;
    }
}
