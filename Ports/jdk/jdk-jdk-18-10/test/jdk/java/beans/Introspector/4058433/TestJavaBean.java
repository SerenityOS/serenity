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

import java.awt.event.ActionListener;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.JavaBean;

/*
 * @test
 * @bug 4058433
 * @summary Tests the JavaBean annotation
 * @author Sergey Malenkov
 */
public class TestJavaBean {

    static final String DSCR = "description";
    static final String PRP = "value";
    static final String ACT = "action";

    public static void main(final String[] args) throws Exception {
        test(X.class, "TestJavaBean$X", "TestJavaBean$X", null, null);
        test(D.class, "TestJavaBean$D", DSCR, null, null);
        test(DP.class, "TestJavaBean$DP", "TestJavaBean$DP", PRP, null);
        test(DES.class, "TestJavaBean$DES", "TestJavaBean$DES", null, ACT);
        test(DDP.class, "TestJavaBean$DDP", DSCR, PRP, null);
        test(DDES.class, "TestJavaBean$DDES", DSCR, null, ACT);
        test(DPDES.class, "TestJavaBean$DPDES", "TestJavaBean$DPDES", PRP, ACT);
        test(DDPDES.class, "TestJavaBean$DDPDES", DSCR, PRP, ACT);
    }

    private static void test(Class<?> type, String name, String descr,
                             String prop, String event) throws Exception {
        BeanInfo info = Introspector.getBeanInfo(type);
        BeanDescriptor bd = info.getBeanDescriptor();

        if (!bd.getName().equals(name)) {
            throw new Error("unexpected name of the bean");
        }

        if (!bd.getShortDescription().equals(descr)) {
            throw new Error("unexpected description of the bean");
        }

        int dp = info.getDefaultPropertyIndex();
        if (dp < 0 && prop != null) {
            throw new Error("unexpected index of the default property");
        }
        if (dp >= 0) {
            if (!info.getPropertyDescriptors()[dp].getName().equals(prop)) {
                throw new Error("unexpected default property");
            }
        }
        int des = info.getDefaultEventIndex();
        if (des < 0 && event != null) {
            throw new Error("unexpected index of the default event set");
        }
        if (des >= 0) {
            if (!info.getEventSetDescriptors()[des].getName().equals(event)) {
                throw new Error("unexpected default event set");
            }
        }
    }

    public static class X {
    }

    @JavaBean(description = DSCR)
    public static class D {
    }

    @JavaBean(defaultProperty = PRP)
    public static class DP {
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @JavaBean(defaultEventSet = ACT)
    public static class DES {
        public void addActionListener(ActionListener listener) {
        }

        public void removeActionListener(ActionListener listener) {
        }
    }

    @JavaBean(description = DSCR, defaultProperty = PRP)
    public static class DDP {
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @JavaBean(description = DSCR, defaultEventSet = ACT)
    public static class DDES {
        public void addActionListener(ActionListener listener) {
        }

        public void removeActionListener(ActionListener listener) {
        }
    }

    @JavaBean(defaultProperty = PRP, defaultEventSet = ACT)
    public static class DPDES {
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void addActionListener(ActionListener listener) {
        }

        public void removeActionListener(ActionListener listener) {
        }
    }

    @JavaBean(description = DSCR, defaultProperty = PRP, defaultEventSet = ACT)
    public static class DDPDES {
        private int value;

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void addActionListener(ActionListener listener) {
        }

        public void removeActionListener(ActionListener listener) {
        }
    }
}
