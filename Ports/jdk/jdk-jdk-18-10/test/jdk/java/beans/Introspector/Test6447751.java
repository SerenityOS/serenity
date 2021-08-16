/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6447751
 * @summary Tests automatic search for customizers
 * @author Sergey Malenkov
 */

import java.awt.Component;
import java.beans.Customizer;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.SimpleBeanInfo;
import java.beans.BeanDescriptor;
import java.beans.PropertyChangeListener;

public class Test6447751 {

    public static void main(String[] args) {
        test(Manual.class, AutomaticCustomizer.class);
        test(Illegal.class, null);
        test(Automatic.class, AutomaticCustomizer.class);
    }

    private static void test(Class<?> type, Class<?> expected) {
        Class<?> actual;
        try {
            actual = Introspector.getBeanInfo(type).getBeanDescriptor().getCustomizerClass();
        }
        catch (IntrospectionException exception) {
            throw new Error("unexpected error", exception);
        }
        if (actual != expected) {
            StringBuilder sb = new StringBuilder();
            sb.append("bean ").append(type).append(": ");
            if (expected != null) {
                sb.append("expected ").append(expected);
                if (actual != null) {
                    sb.append(", but ");
                }
            }
            if (actual != null) {
                sb.append("found ").append(actual);
            }
            throw new Error(sb.toString());
        }
    }

    public static class Automatic {
    }
    public static class AutomaticCustomizer extends Component implements Customizer {
        public void setObject(Object bean) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Illegal {
    }
    public static class IllegalCustomizer implements Customizer {
        public void setObject(Object bean) {
            throw new UnsupportedOperationException();
        }
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Manual {
    }
    public static class ManualBeanInfo extends SimpleBeanInfo {
        public BeanDescriptor getBeanDescriptor() {
            return new BeanDescriptor(Manual.class, AutomaticCustomizer.class);
        }
    }
}
