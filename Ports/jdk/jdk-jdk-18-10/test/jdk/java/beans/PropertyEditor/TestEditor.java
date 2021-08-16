/*
 * Copyright (c) 2006, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

final class TestEditor {
    private final PropertyEditor editor;

    TestEditor(Class type) {
        System.out.println("Property class: " + type);

        this.editor = PropertyEditorManager.findEditor(type);
        if (this.editor == null)
            throw new Error("could not find editor for " + type);

        System.out.println("PropertyEditor class: " + this.editor.getClass());
        validate(null, null);
    }

    void testJava(Object value) {
        this.editor.setValue(value);

        Object object = execute("Executor", "execute", this.editor.getJavaInitializationString());

        System.out.println("Property value before: " + value);
        System.out.println("Property value after: " + object);

        if (!areEqual(value, object))
            throw new Error("values are not equal");
    }

    void testValue(Object value, String text) {
        this.editor.setValue(value);
        validate(value, text);
    }

    void testText(String text, Object value) {
        this.editor.setAsText(text);
        validate(value, text);
    }

    private void validate(Object value, String text) {
        if (!areEqual(value, this.editor.getValue()))
            throw new Error("value should be " + value);

        if (!areEqual(text, this.editor.getAsText()))
            throw new Error("text should be " + text);
    }

    private static boolean areEqual(Object object1, Object object2) {
        return (object1 == null)
                ? object2 == null
                : object1.equals(object2);
    }

    private static Object execute(String classname, String methodname, String value) {
        String content
                = "public class " + classname + " {"
                + "    public static Object " + methodname + "() throws Exception {"
                + "        return " + value + ";"
                + "    }"
                + "}";

        try {
            MemoryClassLoader loader = new MemoryClassLoader();
            Class type = loader.compile(classname, content);
            return type.getMethod(methodname).invoke(null);
        }
        catch (Exception exception) {
            throw new Error(exception);
        }
    }
}
