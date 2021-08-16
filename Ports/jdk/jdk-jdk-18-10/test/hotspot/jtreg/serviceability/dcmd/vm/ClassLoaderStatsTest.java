/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test of diagnostic command VM.classloader_stats
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng/othervm --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED ClassLoaderStatsTest
 */

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassLoaderStatsTest {

  // Expected output from VM.classloader_stats:
    // ClassLoader         Parent              CLD*               Classes   ChunkSz   BlockSz  Type
    // 0x0000000800bd3830  0x000000080037f468  0x00007f001c2ea170       1     10240      4672  ClassLoaderStatsTest$DummyClassLoader
    //                                                                  1       256       131   + hidden classes
    // 0x0000000000000000  0x0000000000000000  0x00007f00e852d190    1607   4628480   3931216  <boot class loader>
    //                                                                 38    124928     85856   + hidden classes
    // 0x00000008003b5508  0x0000000000000000  0x00007f001c2d4760       1      6144      4040  jdk.internal.reflect.DelegatingClassLoader
    // 0x000000080037f468  0x000000080037ee80  0x00007f00e868e3f0     228   1368064   1286672  jdk.internal.loader.ClassLoaders$AppClassLoader
    // ...

    static Pattern clLine = Pattern.compile("0x\\p{XDigit}*\\s*0x\\p{XDigit}*\\s*0x\\p{XDigit}*\\s*(\\d*)\\s*(\\d*)\\s*(\\d*)\\s*(.*)");
    static Pattern hiddenLine = Pattern.compile("\\s*(\\d*)\\s*(\\d*)\\s*(\\d*)\\s*.*");

    public static DummyClassLoader dummyloader;

    public void run(CommandExecutor executor) throws ClassNotFoundException {

        // create a classloader and load our special classes
        dummyloader = new DummyClassLoader();
        Class<?> c = Class.forName("TestClass", true, dummyloader);
        if (c.getClassLoader() != dummyloader) {
            Assert.fail("TestClass defined by wrong classloader: " + c.getClassLoader());
        }

        OutputAnalyzer output = executor.execute("VM.classloader_stats");
        Iterator<String> lines = output.asLines().iterator();
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = clLine.matcher(line);
            if (m.matches()) {
                // verify that DummyClassLoader has loaded 1 regular class and 2 hidden classes
                if (m.group(4).equals("ClassLoaderStatsTest$DummyClassLoader")) {
                    System.out.println("DummyClassLoader line: " + line);
                    if (!m.group(1).equals("1")) {
                        Assert.fail("Should have loaded 1 class: " + line);
                    }
                    checkPositiveInt(m.group(2));
                    checkPositiveInt(m.group(3));

                    String next = lines.next();
                    System.out.println("DummyClassLoader next: " + next);
                    if (!next.contains("hidden classes")) {
                        Assert.fail("Should have a hidden class");
                    }
                    Matcher m2 = hiddenLine.matcher(next);
                    m2.matches();
                    if (!m2.group(1).equals("1")) {
                        Assert.fail("Should have loaded 1 hidden class, but found : " + m2.group(1));
                    }
                    checkPositiveInt(m2.group(2));
                    checkPositiveInt(m2.group(3));
                }
            }
        }
    }

    private static void checkPositiveInt(String s) {
        if (Integer.parseInt(s) <= 0) {
            Assert.fail("Value should have been > 0: " + s);
        }
    }

    public static class DummyClassLoader extends ClassLoader {

        static ByteBuffer readClassFile(String name)
        {
            File f = new File(System.getProperty("test.classes", "."), name);
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
            if (!"TestClass".equals(name)) {
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
            if (!"TestClass".equals(name)) {
                throw new ClassNotFoundException("Unexpected class: " + name);
            }
            return defineClass(name, readClassFile(name + ".class"), null);
        }
    } /* DummyClassLoader */

    @Test
    public void jmx() throws ClassNotFoundException {
        run(new JMXExecutor());
    }
}

class HiddenClass { }

class TestClass {
    private static final String HCName = "HiddenClass.class";
    private static final String DIR = System.getProperty("test.classes");

    static {
        try {
            // Create a hidden non-strong class
            byte[] klassBuf = readClassFile(DIR + File.separator + HCName);
            Class<?> hc = defineHiddenClass(klassBuf);
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception in TestClass: " + e.getMessage());
        }
    }


    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }

    static Class<?> defineHiddenClass(byte[] bytes) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        Class<?> hc = lookup.defineHiddenClass(bytes, false, NESTMATE).lookupClass();
        return hc;
    }
}
