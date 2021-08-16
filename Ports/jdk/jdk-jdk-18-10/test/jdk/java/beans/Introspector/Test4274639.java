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
 * @bug 4274639 4305280
 * @summary Tests PropertyDescriptor/PropertyEditorSupport improvements
 * @author Mark Davidson
 */

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.beans.SimpleBeanInfo;

public class Test4274639 {
    private static String STRING_PROPERTY = "string";
    private static String INTEGER_PROPERTY = "integer";

    private static String STRING_VALUE = "Test Text";
    private static Integer INTEGER_VALUE = 261074;

    public static void main(String[] args) {
        TestBean bean = new TestBean(STRING_VALUE);
        if (!STRING_VALUE.equals(bean.getString()))
            throw new Error("unexpected string property: " + bean.getString());

        boolean string = false;
        boolean integer = false;
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(bean.getClass())) {
            String name = pd.getName();
            System.out.println(" - " + name);
            if (name.equals(STRING_PROPERTY)) {
                // This tests createPropertyEditor such that the PropertyEditor
                // returned will have the bean as the source object.
                Class type = pd.getPropertyEditorClass();
                if (!StringEditor.class.equals(type))
                    throw new Error("unexpected property editor type: " + type);

                PropertyEditor editor = pd.createPropertyEditor(bean);
                if (editor == null)
                    throw new Error("property editor cannot be created");

                if (STRING_VALUE != editor.getValue())
                    throw new Error("unexpected value: " + editor.getValue());

                Object source = ((PropertyEditorSupport) editor).getSource();
                if (source != bean)
                    throw new Error("unexpected source: " + source);

                string = true;
            }
            if (name.equals(INTEGER_PROPERTY)) {
                // This tests createPropertyEditor such that the PropertyEditor
                // returned will be just a new instance
                Class type = pd.getPropertyEditorClass();
                if (!IntegerEditor.class.equals(type))
                    throw new Error("unexpected property editor type: " + type);

                PropertyEditor editor = pd.createPropertyEditor(bean);
                if (editor == null)
                    throw new Error("property editor cannot be created");

                if (INTEGER_VALUE != editor.getValue())
                    throw new Error("unexpected value: " + editor.getValue());

                Object source = ((PropertyEditorSupport) editor).getSource();
                if (source != editor)
                    throw new Error("unexpected source: " + source);

                integer = true;
            }
        }
        if (!string)
            throw new Error("string property is not tested");

        if (!integer)
            throw new Error("integer property is not tested");
    }

    public static final class TestBean {
        private String string;
        private int integer;

        public TestBean() {
            this.string = "default";
            this.integer = 0;
        }

        public TestBean(String string) {
            setString(string);
        }

        public String getString() {
            return this.string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public int getInteger() {
            return this.integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }
    }

    public static final class TestBeanBeanInfo extends SimpleBeanInfo {
        public PropertyDescriptor[] getPropertyDescriptors() {
            PropertyDescriptor[] pds = new PropertyDescriptor[2];
            try {
                pds[0] = new PropertyDescriptor(STRING_PROPERTY, TestBean.class);
                pds[0].setPropertyEditorClass(StringEditor.class);

                pds[1] = new PropertyDescriptor(INTEGER_PROPERTY, TestBean.class);
                pds[1].setPropertyEditorClass(IntegerEditor.class);

            }
            catch (IntrospectionException exception) {
                throw new Error("unexpected error", exception);
            }
            return pds;
        }
    }

    // Public constructor was added for 4305280
    public static final class StringEditor extends PropertyEditorSupport {
        public StringEditor(Object source) {
            super(source);

            if (source instanceof TestBean) {
                TestBean test = (TestBean) source;
                setValue(test.getString());
            }
        }
    }

    // Will use the default public constructor
    // that uses this property editor as the source.
    public static final class IntegerEditor extends PropertyEditorSupport {
        public Object getValue() {
            return INTEGER_VALUE; // default value is hard coded
        }
    }
}
