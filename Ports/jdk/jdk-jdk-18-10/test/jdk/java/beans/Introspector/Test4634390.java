/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4634390
 * @summary Tests contract for equals and hashCode methods
 * @author Mark Davidson
 */

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import javax.swing.*;

public class Test4634390 {

    private static final Class[] CLASSES = {
            JButton.class,
            JDialog.class,
            JMenu.class,
            JPanel.class,
            JProgressBar.class,
            JSlider.class,
            JTable.class,
            JTree.class,
            JComboBox.class,
            JToolBar.class,
            JTabbedPane.class,
    };

    public static void main(String[] args) {
        for (Class type : CLASSES) {
            test(type);
        }
    }

    private static void test(Class type) {
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(type)) {
            PropertyDescriptor pdCopy = create(pd);
            if (pdCopy != null) {
                // XXX - hack! The Introspector will set the bound property
                // since it assumes that propertyChange event set descriptors
                // infers that all the properties are bound
                pdCopy.setBound(pd.isBound());

                String name = pd.getName();
                System.out.println(" - " + name);

                if (!compare(pd, pdCopy))
                    throw new Error("property delegates are not equal");

                if (!pd.equals(pdCopy))
                    throw new Error("equals() failed");

                if (pd.hashCode() != pdCopy.hashCode())
                    throw new Error("hashCode() failed");
            }
        }
    }

    private static PropertyDescriptor create(PropertyDescriptor pd) {
        try {
            if (pd instanceof IndexedPropertyDescriptor) {
                IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
                return new IndexedPropertyDescriptor(
                        ipd.getName(),
                        ipd.getReadMethod(),
                        ipd.getWriteMethod(),
                        ipd.getIndexedReadMethod(),
                        ipd.getIndexedWriteMethod());
            } else {
                return new PropertyDescriptor(
                        pd.getName(),
                        pd.getReadMethod(),
                        pd.getWriteMethod());
            }
        }
        catch (IntrospectionException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static boolean compare(PropertyDescriptor pd1, PropertyDescriptor pd2) {
        if (!compare(pd1.getReadMethod(), pd2.getReadMethod())) {
            System.out.println("read methods not equal");
            return false;
        }
        if (!compare(pd1.getWriteMethod(), pd2.getWriteMethod())) {
            System.out.println("write methods not equal");
            return false;
        }
        if (pd1.getPropertyType() != pd2.getPropertyType()) {
            System.out.println("property type not equal");
            return false;
        }
        if (pd1.getPropertyEditorClass() != pd2.getPropertyEditorClass()) {
            System.out.println("property editor class not equal");
            return false;
        }
        if (pd1.isBound() != pd2.isBound()) {
            System.out.println("bound value not equal");
            return false;
        }
        if (pd1.isConstrained() != pd2.isConstrained()) {
            System.out.println("constrained value not equal");
            return false;
        }
        return true;
    }

    private static boolean compare(Method m1, Method m2) {
        if ((m1 == null) && (m2 == null)) {
            return true;
        }
        if ((m1 == null) || (m2 == null)) {
            return false;
        }
        return m1.equals(m2);
    }
}
