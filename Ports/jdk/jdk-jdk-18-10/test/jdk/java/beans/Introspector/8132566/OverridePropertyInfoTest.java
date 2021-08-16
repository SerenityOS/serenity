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

import java.beans.BeanInfo;
import java.beans.BeanProperty;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

/**
 * @test
 * @bug 8132566 8132565 8131939
 * @summary Check if the derived class overrides
 *           the parent's property info.
 * @author a.stepanov
 */

public class OverridePropertyInfoTest {

    public static class C extends CBase {

        private int value;

        @BeanProperty(
                bound        = false,
                expert       = false,
                hidden       = false,
                preferred    = false,
                required     = false,
                visualUpdate = false,
                description = "CHILD",
                enumerationValues = {"javax.swing.SwingConstants.BOTTOM"}
                )
        @Override
        public void setValue(int v) { value = v; }
        @Override
        public  int getValue() { return value; }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static void main(String[] args) throws Exception {

        BeanInfo i = Introspector.getBeanInfo(C.class, Object.class);
        PropertyDescriptor[] pds = i.getPropertyDescriptors();

        Checker.checkEq("number of properties", pds.length, 1);
        PropertyDescriptor p = pds[0];
        Checker.checkEq("property description", p.getShortDescription(), "CHILD");

        Checker.checkEq("isBound",  p.isBound(),  false);
        Checker.checkEq("isExpert", p.isExpert(), false);
        Checker.checkEq("isHidden", p.isHidden(), false);
        Checker.checkEq("isPreferred", p.isPreferred(), false);
        Checker.checkEq("required", p.getValue("required"), false);
        Checker.checkEq("visualUpdate", p.getValue("visualUpdate"), false);

        Checker.checkEnumEq("enumerationValues", p.getValue("enumerationValues"),
            new Object[]{"BOTTOM", 3, "javax.swing.SwingConstants.BOTTOM"});
    }
}
