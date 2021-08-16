/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8130937 8131347
 * @summary Tests the booleans properties of the BeanProperty annotation
 * @library ..
 */
public final class TestBooleanBeanProperties {

    public static void main(final String[] args) {
        test(Empty.class, false, false, false, false, false, false);
        test(BoundTrue.class, false, false, false, false, false, false);
        test(BoundFalse.class, false, false, false, false, false, false);
        test(BoundListener.class, true, false, false, false, false, false);
        test(BoundFalseListener.class, false, false, false, false, false, false);
        test(BoundTrueListener.class, true, false, false, false, false, false);
        test(ExpertTrue.class, false, true, false, false, false, false);
        test(ExpertFalse.class, false, false, false, false, false, false);
        test(HiddenTrue.class, false, false, true, false, false, false);
        test(HiddenFalse.class, false, false, false, false, false, false);
        test(PreferredTrue.class, false, false, false, true, false, false);
        test(PreferredFalse.class, false, false, false, false, false, false);
        test(RequiredTrue.class, false, false, false, false, true, false);
        test(RequiredFalse.class, false, false, false, false, false, false);
        test(VisualUpdateTrue.class, false, false, false, false, false, true);
        test(VisualUpdateFalse.class, false, false, false, false, false, false);
        test(All.class, true, true, true, true, true, true);
    }

    private static void test(Class<?> cls, boolean isBound, boolean isExpert,
                             boolean isHidden, boolean isPref, boolean isReq,
                             boolean isVS) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(cls, "value");
        if (pd.isBound() != isBound) {
            throw new RuntimeException("isBound should be: " + isBound);
        }
        if (pd.isExpert() != isExpert || getValue(pd, "expert") != isExpert) {
            throw new RuntimeException("isExpert should be:" + isExpert);
        }
        if (pd.isHidden() != isHidden || getValue(pd, "hidden") != isHidden) {
            throw new RuntimeException("isHidden should be: " + isHidden);
        }
        if (pd.isPreferred() != isPref) {
            throw new RuntimeException("isPreferred should be: " + isPref);
        }
        if (getValue(pd, "required") != isReq) {
            throw new RuntimeException("required should be: " + isReq);
        }
        if (getValue(pd, "visualUpdate") != isVS) {
            throw new RuntimeException("required should be: " + isVS);
        }
        if (pd.getValue("enumerationValues") == null) {
            throw new RuntimeException("enumerationValues should be empty array");
        }
    }

    private static boolean getValue(PropertyDescriptor pd, String value) {
        return (boolean) pd.getValue(value);
    }
    ////////////////////////////////////////////////////////////////////////////

    public static final class Empty {

        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class All {

        private int value;

        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        public int getValue() {
            return value;
        }

        @BeanProperty(bound = true, expert = true, hidden = true,
                      preferred = true, required = true, visualUpdate = true,
                      enumerationValues = {})
        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // bound property
    ////////////////////////////////////////////////////////////////////////////

    public static final class BoundTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(bound = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class BoundFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(bound = false)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class BoundListener {

        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }
    }

    public static final class BoundFalseListener {

        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(bound = false)
        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }
    }

    public static final class BoundTrueListener {

        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(bound = true)
        public void setValue(int value) {
            this.value = value;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // expert property
    ////////////////////////////////////////////////////////////////////////////

    public static final class ExpertTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(expert = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class ExpertFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(expert = false)
        public void setValue(int value) {
            this.value = value;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // hidden property
    ////////////////////////////////////////////////////////////////////////////

    public static final class HiddenTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(hidden = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class HiddenFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(hidden = false)
        public void setValue(int value) {
            this.value = value;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // preferred property
    ////////////////////////////////////////////////////////////////////////////

    public static final class PreferredTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(preferred = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class PreferredFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(preferred = false)
        public void setValue(int value) {
            this.value = value;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // required property
    ////////////////////////////////////////////////////////////////////////////

    public static final class RequiredTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(required = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class RequiredFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(required = false)
        public void setValue(int value) {
            this.value = value;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // visualUpdate property
    ////////////////////////////////////////////////////////////////////////////

    public static final class VisualUpdateTrue {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(visualUpdate = true)
        public void setValue(int value) {
            this.value = value;
        }
    }

    public static final class VisualUpdateFalse {

        private int value;

        public int getValue() {
            return value;
        }

        @BeanProperty(visualUpdate = false)
        public void setValue(int value) {
            this.value = value;
        }
    }
}
