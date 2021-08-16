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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * @test
 * @bug 8159696
 * @library /javax/swing/regtesthelpers
 * @compile ./stub/Stub.java
 * @run main/othervm -mx32M UnloadClassBeanInfo
 */
public class UnloadClassBeanInfo {

    private static URLClassLoader loader;

    public static void main(final String[] args) throws Exception {
        Class cl = getStub();
        System.out.println("cl.getClassLoader() = " + cl.getClassLoader());
        final BeanInfo beanInfo = Introspector.getBeanInfo(cl, Object.class);
        MethodDescriptor[] mds = beanInfo.getMethodDescriptors();
        System.out.println("mds = " + Arrays.toString(mds));
        loader.close();
        loader=null;
        cl=null;
        Util.generateOOME();
        mds = beanInfo.getMethodDescriptors();
        System.out.println("mds = " + Arrays.toString(mds));
    }

    /**
     * The Stub class is compiled by jtreg, but we want to move it so it is not
     * on the application classpath. We want to load it through a separate
     * classloader.
     */
    static Class<?> getStub() throws Exception {
        final String testclasses = System.getProperty("test.classes");
        final File subdir = new File(testclasses, "stub");
        subdir.mkdir();

        final Path src = Paths.get(testclasses, "Stub.class");
        final Path dest = subdir.toPath().resolve("Stub.class");
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);

        loader = new URLClassLoader(new URL[]{subdir.toURL()});
        return Class.forName("Stub", true, loader);
    }
}
