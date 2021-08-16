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
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

/**
 * @test
 * @bug 8132566 8131939
 * @summary Check if the derived class inherits the property info
 *          from the base class.
 * @author a.stepanov
 */

public class InheritPropertyInfoTest {

    public static class C extends CBase {}

    public static void main(String[] args) throws Exception {

        BeanInfo i = Introspector.getBeanInfo(C.class, Object.class);
        PropertyDescriptor[] pds = i.getPropertyDescriptors();

        Checker.checkEq("number of properties", pds.length, 1);
        PropertyDescriptor p = pds[0];

        Checker.checkEq("property description", p.getShortDescription(), "BASE");

        Checker.checkEq("isBound",  p.isBound(),  true);
        Checker.checkEq("isExpert", p.isExpert(), true);
        Checker.checkEq("isHidden", p.isHidden(), true);
        Checker.checkEq("isPreferred", p.isPreferred(), true);
        Checker.checkEq("required", p.getValue("required"), true);
        Checker.checkEq("visualUpdate", p.getValue("visualUpdate"), true);

        Checker.checkEnumEq("enumerationValues", p.getValue("enumerationValues"),
            new Object[]{"TOP", 1, "javax.swing.SwingConstants.TOP"});
    }
}
