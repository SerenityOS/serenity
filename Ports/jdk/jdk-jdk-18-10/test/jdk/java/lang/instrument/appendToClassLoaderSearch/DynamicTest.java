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
 * Unit test for Instrumentation appendToBootstrapClassLoaderSearch method.
 * The test works as follows:
 *
 * 1. Load class "Application" and execute the doSomething() method
 *
 * 2. Append Tracer.jar to the boot class path - Tracer.jar has a single
 *    class org.tools.Tracer.
 *
 * 3. Redefine class "Application" - the redefined version has an instrumented
 *    version of doSomething() that invokes a method in org.tools.Tracer.
 *
 * 4. Re-execute doSomething() - this should provoke the loading of org.tools.Tracer
 *    from the jar file. If updated version of doSomething() executes then test
 *    passes.
 */
import java.lang.instrument.*;
import java.util.jar.JarFile;
import java.io.*;

public class DynamicTest {

    public static void main(String args[]) throws Exception {

        // Load Application
        Application app = new Application();
        if (app.doSomething() != 1) {
            throw new RuntimeException("Test configuration error - doSomething should return 1");
        }

        // Add org.tools.Tracer package to the boot class path
        JarFile jf = new JarFile("Tracer.jar");
        Agent.getInstrumentation().appendToBootstrapClassLoaderSearch(jf);

        // Redefine Application with the instrumented version
        File f = new File("InstrumentedApplication.bytes");
        int len = (int)f.length();
        byte[] def = new byte[len];

        FileInputStream fis = new FileInputStream(f);
        int nread = 0;
        do {
            int n = fis.read(def, nread, len-nread);
            if (n > 0) {
                nread += n;
            }
        } while (nread < len);

        ClassDefinition classDefs = new ClassDefinition(Application.class, def);
        Agent.getInstrumentation().redefineClasses(new ClassDefinition[] { classDefs } );

        // Re-execute doSomething() - should get 3 messages printed
        int res = app.doSomething();
        if (res != 3) {
            throw new RuntimeException("FAIL: redefined Application returned: " + res);
        }
        System.out.println("PASS: Test passed.");
    }
}
