/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4676532
 * @summary Tests XMLDecoder.setClassLoader()
 * @author Mark Davidson
 */

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Tests the XMLDecoder with an alternative classloader.
 * Specicifically, the URLClassLoader.
 * The test.jar file should be in the same directory as the classes.
 * The Encode class will create the test.xml test file.
 */
public class Test4676532 {
    private static final String DATA
            = "<java>\n"
            + " <object class=\"test.Test\">\n"
            + "  <void property=\"message\">\n"
            + "   <string>Hello, world</string>\n"
            + "  </void>\n"
            + " </object>\n"
            + "</java> ";

    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder(256);
        sb.append("file:");
        sb.append(System.getProperty("test.src", "."));
        sb.append(File.separatorChar);
        sb.append("test.jar");

        URL[] url = {new URL(sb.toString())};
        URLClassLoader cl = new URLClassLoader(url);

        Class type = cl.loadClass("test.Test");
        if (type == null) {
            throw new Error("could not find class test.Test");
        }


        InputStream stream = new ByteArrayInputStream(DATA.getBytes());

        ExceptionListener el = new ExceptionListener() {
            public void exceptionThrown(Exception exception) {
                throw new Error("unexpected exception", exception);
            }
        };

        XMLDecoder decoder = new XMLDecoder(stream, null, el, cl);
        Object object = decoder.readObject();
        decoder.close();

        if (!type.equals(object.getClass())) {
            throw new Error("unexpected " + object.getClass());
        }
    }
}
