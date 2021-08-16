/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Unit test for Instrumentation appendToSystemClassLoaderSearch. This
 * test does the following:
 *
 * 1. Creates a class loader to load class Foo. Execute Foo.doSomething
 *    which references a missing class Bar. The doSomething method
 *    should fail with NoClassDefFoundError.
 *
 * 2. Add Bar.jar to the system class path. Bar.jar contains Bar.
 *
 * 3. Create another class loader to load Foo. Execute Foo.doSomething.
 *    doSomething will load Bar.
 *
 * 4. Re-execute the first Foo's doSomething - it should fail a second
 *    time because the attempt to resolve Bar must fail with the same
 *    error as the first attempt.
 *
 * 5. De-reference both class loaders and execute System.gc(). We can't
 *    assert that the Foo classes will be unloaded but it serves to
 *    exercise the unload code path in HotSpot.
 */
import java.lang.instrument.Instrumentation;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarFile;

public class ClassUnloadTest {

    static Instrumentation ins;

    public static void main(String args[]) throws Exception {
        String dir = args[0] + File.separator;
        String jar = dir + args[1];

        System.out.println(jar);

        URL u = (new File(dir)).toURL();
        URL urls[] = { u };

        // This should fail as Bar is not available
        Invoker i1 = new Invoker(urls, "Foo", "doSomething");
        Boolean result = (Boolean)i1.invoke((Object)null);
        if (result.booleanValue()) {
            throw new RuntimeException("Test configuration error - doSomething should not succeed");
        }

        // put Bar on the system class path
        ins.appendToSystemClassLoaderSearch( new JarFile(jar) );

        // This should fail even though Bar is now available
        result = (Boolean)i1.invoke((Object)null);
        if (result.booleanValue()) {
            throw new RuntimeException("Test configuration error - doSomething should not succeed");
        }

        // This should succeed because this is a different Foo
        Invoker i2 = new Invoker(urls, "Foo", "doSomething");
        result = (Boolean)i2.invoke((Object)null);
        if (!result.booleanValue()) {
            throw new RuntimeException("Test configuration error - doSomething did not succeed");
        }

        // Exercise some class unloading
        i1 = i2 = null;
        System.gc();
    }

    static class Invoker {

        URLClassLoader cl;
        Method m;

        public Invoker(URL urls[], String cn, String mn, Class ... params)
            throws ClassNotFoundException, NoSuchMethodException
        {
            cl = new URLClassLoader(urls);
            Class c = Class.forName("Foo", true, cl);
            m = c.getDeclaredMethod(mn, params);
        }

        public Object invoke(Object ... args)
            throws IllegalAccessException, InvocationTargetException
        {
            return m.invoke(args);
        }
    }

    public static void premain(String args, Instrumentation i) {
        ins = i;
    }
}
