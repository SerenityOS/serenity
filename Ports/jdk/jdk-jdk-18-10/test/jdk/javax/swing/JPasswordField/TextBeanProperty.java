/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * @test
 * @bug 8258373
 * @summary The "text" property should not be "bound"
 */
public final class TextBeanProperty {

    public static void main(String[] args) throws Exception {
        test(JTextComponent.class);
        test(JTextField.class);
        test(JTextArea.class);
        test(JPasswordField.class);
    }

    private static void test(Class<?> beanClass) throws Exception {
        BeanInfo info = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] pd = info.getPropertyDescriptors();
        int i;
        for (i = 0; i < pd.length; i++) {
            if (pd[i].getName().equals("text")) {
                break;
            }
        }
        if (pd[i].isBound()) {
            System.err.println("Property: " + pd[i]);
            throw new RuntimeException("text property is flagged as bound");
        }
    }
}
