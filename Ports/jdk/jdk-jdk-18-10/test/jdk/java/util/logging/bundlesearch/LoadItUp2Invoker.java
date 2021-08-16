/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is loaded by a class loader that can see the resource. It creates
 * a new classloader for LoadItUp2 which cannot see the resource.  So, 2 levels
 * up the call chain we have a class/classloader that can see the resource, but
 * 1 level up the class/classloader cannot.
 *
 * @author Jim Gish
 */
public class LoadItUp2Invoker {
    private URLClassLoader cl;
    private String rbName;
    private Object loadItUp2;
    private Method testMethod;

    public void setup(URL[] urls, String rbName) throws
                       ReflectiveOperationException {
        this.cl = new URLClassLoader(urls, null);
        this.rbName = rbName;
        // Using this new classloader, load the actual test class
        // which is now two levels removed from the original caller
        Class<?> loadItUp2Clazz = Class.forName("LoadItUp2", true , cl);
        this.loadItUp2 = loadItUp2Clazz.newInstance();
        this.testMethod = loadItUp2Clazz.getMethod("test", String.class);
    }

    public Boolean test() throws Throwable {
        try {
            return (Boolean) testMethod.invoke(loadItUp2, rbName);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
