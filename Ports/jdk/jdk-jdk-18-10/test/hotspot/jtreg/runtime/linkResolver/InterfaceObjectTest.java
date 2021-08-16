/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026394 8251414
 * @summary test interface resolution when clone and finalize are declared abstract within
 *          an interface and when they are not
 * @compile InterfaceObj.jasm
 * @run main InterfaceObjectTest
 */
interface IClone extends Cloneable {
    void finalize() throws Throwable;
    Object clone();
}

interface ICloneExtend extends IClone { }

public class InterfaceObjectTest implements ICloneExtend {

    public Object clone() {
        System.out.println("In InterfaceObjectTest's clone() method\n");
        return null;
    }

    public void finalize() throws Throwable {
        try {
            System.out.println("In InterfaceObjectTest's finalize() method\n");
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void tryIt(ICloneExtend o1) {
        try {
            Object o2 = o1.clone();
            o1.finalize();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }


    public static void main(String[] args) throws Exception {
        // Test with abstract public clone() and finalize() methods.
        InterfaceObjectTest o1 = new InterfaceObjectTest();
        tryIt(o1);


        // Test with reflection without abstract public clone() and finalize() methods.
        Class cls = Class.forName("InterfaceObj");
        try {
            java.lang.reflect.Method m = cls.getMethod("testFinalize");
            m.invoke(cls);
            throw new RuntimeException("Failed to throw NoSuchMethodError for finalize()");
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (!e.getCause().toString().contains("NoSuchMethodError")) {
                throw new RuntimeException("wrong ITE: " + e.getCause().toString());
            }
        }

        try {
            java.lang.reflect.Method m = cls.getMethod("testClone");
            m.invoke(cls);
            throw new RuntimeException("Failed to throw NoSuchMethodError for clone()");
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (!e.getCause().toString().contains("NoSuchMethodError")) {
                throw new RuntimeException("wrong ITE: " + e.getCause().toString());
            }
        }

    }
}
