/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.test.lib.jfr;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Custom class loader which will try to load the class via getResourceAsStream().
 * If there are any errors, the parent class loader will be used instead.
 */
public class TestClassLoader extends ClassLoader {
    static public final String CLASS_LOADER_NAME = "JFR TestClassLoader";

    public TestClassLoader() {
        super(CLASS_LOADER_NAME, ClassLoader.getSystemClassLoader());
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {

        InputStream is = null;
        DataInputStream dis = null;
        try {
            String resourceName = name.replace('.', '/') + ".class";
            is = getResourceAsStream(resourceName);
            if (is != null) {
                int i = is.available();
                byte buf[] = new byte[i];
                dis = new DataInputStream(is);
                dis.readFully(buf);
                dis.close();
                return defineClass(name, buf, 0, buf.length);
            }
        } catch (SecurityException e) {
            // This error will happen quite often (for example when loading
            // "java.lang...").
            // Ignore this error and use parent class loader.
        } catch (IOException e) {
            // Unexpected error. Use parent class loader.
            e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return super.loadClass(name);
    }

}
