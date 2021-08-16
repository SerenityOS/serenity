/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package b;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A public class in b which is a subclass of public class in a.
 */
public class PublicSub extends a.PublicSuper {

    // fields
    private static int privateStatic;
    private int privateInstance;
    static int packageStatic;
    int packageInstance;
    protected static int protectedStatic;
    protected int protectedInstance;
    public static int publicStatic;
    public int publicInstance;

    // methods
    private static int privateStatic() { return 42; }
    private int privateInstance() { return 42; }
    static int packageStatic() { return 42; }
    int packageInstance() { return 42; }
    protected static int protectedStatic() { return 42; }
    protected int protectedInstance() { return 42; }
    public static int publicStatic() { return 42; }
    public int publicInstance() { return 42; }

    // constructors
    private PublicSub(Void _1, Void _2, Void _3) {}
    PublicSub(Void _1, Void _2) {}
    protected PublicSub(Void _1) {}
    public PublicSub() {}


    // testing method
    public static void checkAccess(AccessibleObject accessibleObject, Object obj)
        throws IllegalAccessException,
               InvocationTargetException,
               InstantiationException
    {
        if (accessibleObject instanceof Field) {
            Field field = (Field) accessibleObject;
            field.set(obj, 42);
            field.get(obj);
        } else if (accessibleObject instanceof Method) {
            Method method = (Method) accessibleObject;
            method.invoke(obj);
        } else if (accessibleObject instanceof Constructor) {
            Constructor<?> constructor = (Constructor<?>) accessibleObject;
            Object[] params = new Object[constructor.getParameterCount()];
            constructor.newInstance(params);
        }
    }
}
