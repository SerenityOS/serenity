/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5102804
 * @summary Tests memory leak
 * @author Sergey Malenkov
 * @run main/othervm -ms16m -mx16m Test5102804
 */

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;

public class Test5102804 {
    private static final String BEAN_NAME = "Test5102804$Example";
    private static final String BEAN_INFO_NAME = BEAN_NAME + "BeanInfo";

    public static void main(String[] args) {
        if (!isCollectible(getReference()))
            throw new Error("Reference is not collected");
    }

    private static Reference getReference() {
        try {
            ClassLoader loader = new Loader();
            Class type = Class.forName(BEAN_NAME, true, loader);
            if (!type.getClassLoader().equals(loader)) {
                throw new Error("Wrong class loader");
            }
            BeanInfo info = Introspector.getBeanInfo(type);
            if (0 != info.getDefaultPropertyIndex()) {
                throw new Error("Wrong bean info found");
            }
            return new WeakReference<Class>(type);
        }
        catch (IntrospectionException exception) {
            throw new Error("Introspection Error", exception);
        }
        catch (ClassNotFoundException exception) {
            throw new Error("Class Not Found", exception);
        }
    }

    private static boolean isCollectible(Reference reference) {
        int[] array = new int[10];
        while (true) {
            try {
                array = new int[array.length + array.length / 3];
            }
            catch (OutOfMemoryError error) {
                return null == reference.get();
            }
        }
    }

    /**
     * Custom class loader to load the Example class by itself.
     * Could also load it from a different code source, but this is easier to set up.
     */
    private static final class Loader extends URLClassLoader {
        Loader() {
            super(new URL[] {
                    Test5102804.class.getProtectionDomain().getCodeSource().getLocation()
            });
        }

        @Override
        protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class c = findLoadedClass(name);
            if (c == null) {
                if (BEAN_NAME.equals(name) || BEAN_INFO_NAME.equals(name)) {
                    c = findClass(name);
                }
                else try {
                    c = getParent().loadClass(name);
                }
                catch (ClassNotFoundException exception) {
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    /**
     * A simple bean to load from the Loader class, not main class loader.
     */
    public static final class Example {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * The BeanInfo for the Example class.
     * It is also loaded from the Loader class.
     */
    public static final class ExampleBeanInfo extends SimpleBeanInfo {
        @Override
        public int getDefaultPropertyIndex() {
            return 0;
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                return new PropertyDescriptor[] {
                        new PropertyDescriptor("value", Class.forName(BEAN_NAME))
                };
            }
            catch (ClassNotFoundException exception) {
                return null;
            }
            catch (IntrospectionException exception) {
                return null;
            }
        }
    }
}
