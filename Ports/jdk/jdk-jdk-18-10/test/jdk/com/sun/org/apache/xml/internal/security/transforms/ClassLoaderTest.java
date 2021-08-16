/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @author Sean Mullan
 * @bug 6461674 8009217 7147060
 * @modules java.xml.crypto/com.sun.org.apache.xml.internal.security.exceptions
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.transforms
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.signature
 * @compile -XDignore.symbol.file ClassLoaderTest.java MyTransform.java
 * @run main/othervm ClassLoaderTest
 * @summary Ensure Transform.register works with transform implementations
 *   loaded by class loader other than system/boot class loader
 */
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import com.sun.org.apache.xml.internal.security.exceptions.AlgorithmAlreadyRegisteredException;
import com.sun.org.apache.xml.internal.security.transforms.Transform;

public class ClassLoaderTest {

    private final static String BASE = System.getProperty("test.src", "./");

    public static void main(String[] args) throws Exception {

        File file = new File(BASE);
        URL[] urls = new URL[1];
        urls[0] = file.toURI().toURL();
        URLClassLoader ucl = new URLClassLoader(urls);
        Class<?> c = ucl.loadClass("MyTransform");
        Constructor<?> cons = c.getConstructor(new Class[] {});
        Object o = cons.newInstance();
        // Apache code swallows the ClassNotFoundExc, so we need to
        // check if the Transform has already been registered by registering
        // it again and catching an AlgorithmAlreadyRegisteredExc
        try {
            Transform.register(MyTransform.URI, "MyTransform");
            throw new Exception("ClassLoaderTest failed");
        } catch (AlgorithmAlreadyRegisteredException e) {
            // test passed
        }
    }
}
