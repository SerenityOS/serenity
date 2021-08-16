/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @summary Test of diagnostic command VM.classloaders
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng ClassLoaderHierarchyTest
 */

import org.testng.Assert;
import org.testng.annotations.Test;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ClassLoaderHierarchyTest {

//+-- <bootstrap>
//      |
//      +-- "platform", jdk.internal.loader.ClassLoaders$PlatformClassLoader
//      |     |
//      |     +-- "app", jdk.internal.loader.ClassLoaders$AppClassLoader
//      |
//      +-- jdk.internal.reflect.DelegatingClassLoader
//      |
//      +-- "Kevin", ClassLoaderHierarchyTest$TestClassLoader
//      |
//      +-- ClassLoaderHierarchyTest$TestClassLoader
//            |
//            +-- "Bill", ClassLoaderHierarchyTest$TestClassLoader

    public void run(CommandExecutor executor) throws ClassNotFoundException {

        ClassLoader unnamed_cl = new TestClassLoader(null, null);
        Class<?> c1 = Class.forName("TestClass2", true, unnamed_cl);
        if (c1.getClassLoader() != unnamed_cl) {
            Assert.fail("TestClass defined by wrong classloader: " + c1.getClassLoader());
        }

        ClassLoader named_cl = new TestClassLoader("Kevin", null);
        Class<?> c2 = Class.forName("TestClass2", true, named_cl);
        if (c2.getClassLoader() != named_cl) {
            Assert.fail("TestClass defined by wrong classloader: " + c2.getClassLoader());
        }

        ClassLoader named_child_cl = new TestClassLoader("Bill", unnamed_cl);
        Class<?> c3 = Class.forName("TestClass2", true, named_child_cl);
        if (c3.getClassLoader() != named_child_cl) {
            Assert.fail("TestClass defined by wrong classloader: " + c3.getClassLoader());
        }

        // First test: simple output, no classes displayed
        OutputAnalyzer output = executor.execute("VM.classloaders");
        output.shouldContain("<bootstrap>");
        output.shouldMatch(".*TestClassLoader");
        output.shouldMatch("Kevin.*TestClassLoader");
        output.shouldMatch("Bill.*TestClassLoader");

        // Second test: print with classes.
        output = executor.execute("VM.classloaders show-classes");
        output.shouldContain("<bootstrap>");
        output.shouldContain("java.lang.Object");
        output.shouldMatch(".*TestClassLoader");
        output.shouldMatch("Kevin.*TestClassLoader");
        output.shouldMatch("Bill.*TestClassLoader");
        output.shouldContain("TestClass2");
        output.shouldContain("Hidden Classes:");
    }

    static class TestClassLoader extends ClassLoader {

        public TestClassLoader() {
            super();
        }

        public TestClassLoader(String name, ClassLoader parent) {
            super(name, parent);
        }

        public static final String CLASS_NAME = "TestClass2";

        static ByteBuffer readClassFile(String name)
        {
            File f = new File(System.getProperty("test.classes", "."),
                              name);
            try (FileInputStream fin = new FileInputStream(f);
                 FileChannel fc = fin.getChannel())
            {
                return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            } catch (IOException e) {
                Assert.fail("Can't open file: " + name, e);
            }

            /* Will not reach here as Assert.fail() throws exception */
            return null;
        }

        protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
            Class<?> c;
            if (!CLASS_NAME.equals(name)) {
                c = super.loadClass(name, resolve);
            } else {
                // should not delegate to the system class loader
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
            }
            return c;
        }

        protected Class<?> findClass(String name)
            throws ClassNotFoundException
        {
            if (!CLASS_NAME.equals(name)) {
                throw new ClassNotFoundException("Unexpected class: " + name);
            }
            return defineClass(name, readClassFile(name + ".class"), null);
        }

    }

    @Test
    public void jmx() throws ClassNotFoundException {
        run(new JMXExecutor());
    }

}

class TestClass2 {
    static {
        Runnable r = () -> System.out.println("Hello");
        r.run();
    }
}

