/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;

/**
 * TestProvider2 extends TestProvider with very similar behavior, but
 * with its own static fields, to allow a test to distinguish between
 * an installed TestProvider2 being used, or a property-specified
 * TestProvider being used.
 */
public class TestProvider2 extends TestProvider {

    public static final Class loadClassReturn =
        (new Object() { }).getClass();
    public static final Class loadProxyClassReturn =
        (new Object() { }).getClass();
    public static final ClassLoader getClassLoaderReturn =
        URLClassLoader.newInstance(new URL[0]);
    public static final String getClassAnnotationReturn = new String();

    public static List invocations =
        Collections.synchronizedList(new ArrayList(1));

    public TestProvider2() {
        System.err.println("TestProvider2()");
    }

    public Class loadClass(String codebase, String name,
                           ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException
    {
        invocations.add(new Invocation(loadClassMethod,
            new Object[] { codebase, name, defaultLoader }));

        return loadClassReturn;
    }

    public Class loadProxyClass(String codebase, String[] interfaces,
                                ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException
    {
        invocations.add(new Invocation(loadProxyClassMethod,
            new Object[] { codebase, interfaces, defaultLoader }));

        return loadProxyClassReturn;
    }

    public ClassLoader getClassLoader(String codebase)
        throws MalformedURLException
    {
        invocations.add(new Invocation(
            getClassLoaderMethod, new Object[] { codebase }));

        return getClassLoaderReturn;
    }

    public String getClassAnnotation(Class<?> cl) {
        invocations.add(new Invocation(
            getClassAnnotationMethod, new Object[] { cl }));

        return getClassAnnotationReturn;
    }
}
