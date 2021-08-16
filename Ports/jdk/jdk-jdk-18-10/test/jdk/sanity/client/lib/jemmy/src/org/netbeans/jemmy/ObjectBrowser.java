/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * Class to display information about object: fields, methods, ancestors and so
 * on.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ObjectBrowser implements Outputable {

    private Object object;

    private TestOut output;

    /**
     * Constructor.
     */
    public ObjectBrowser() {
    }

    /**
     * Defines print output streams or writers.
     *
     * @param out Identify the streams or writers used for print output.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut out) {
        output = out;
    }

    /**
     * Returns print output streams or writers.
     *
     * @return an object that contains references to objects for printing to
     * output and err streams.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Specifies the object value.
     *
     * @param obj Object to work with.
     * @see #getObject
     */
    public void setObject(Object obj) {
        object = obj;
    }

    /**
     * Returns the object value.
     *
     * @return Current object.
     * @see #setObject
     */
    public Object getObject() {
        return object;
    }

    /**
     * Prints {@code toString()} information.
     */
    public void printToString() {
        output.printLine(object.toString());
    }

    /**
     * Prints object fields names and values.
     */
    public void printFields() {
        Class<?> cl = object.getClass();
        output.printLine("Class: " + cl.getName());
        output.printLine("Fields: ");
        Field[] fields = cl.getFields();
        for (Field field : fields) {
            output.printLine(Modifier.toString(field.getModifiers()) + " "
                    + field.getType().getName() + " "
                    + field.getName());
            Object value = "Inaccessible";
            try {
                value = field.get(object);
            } catch (IllegalAccessException ignored) {
            }
            output.printLine("    Value: " + value.toString());
        }
    }

    /**
     * Prints object methods names and parameters.
     */
    public void printMethods() {
        Class<?> cl = object.getClass();
        output.printLine("Class: " + cl.getName());
        output.printLine("Methods: ");
        Method[] methods = cl.getMethods();
        for (Method method : methods) {
            output.printLine(Modifier.toString(method.getModifiers()) + " "
                    + method.getReturnType().getName() + " "
                    + method.getName());
            Class<?>[] params = method.getParameterTypes();
            for (Class<?> param : params) {
                output.printLine("    " + param.getName());
            }
        }
    }

    /**
     * Prints allsuperclasses names.
     */
    public void printClasses() {
        Class<?> cl = object.getClass();
        do {
            output.printLine(cl.getName());
        } while ((cl = cl.getSuperclass()) != null);
    }

    /**
     * Prints everything.
     */
    public void printFull() {
        printFields();
        printMethods();
    }
}
