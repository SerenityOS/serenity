/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/*
 * @test
 * @bug 4135031
 * @summary Test bootstrap problem when a URLStreamHandlerFactory is loaded
 *          by the application class loader.
 * @modules java.base/sun.net.www.protocol.file
 * @run main/othervm HandlerLoop
 */
public class HandlerLoop {

    public static void main(String args[]) throws Exception {
        URL.setURLStreamHandlerFactory(
            new HandlerFactory("sun.net.www.protocol"));
        URL url = new URL("file:///bogus/index.html");
        System.out.println("url = " + url);
        url.openConnection();
    }

    private static class HandlerFactory implements URLStreamHandlerFactory {
        private String pkg;

        HandlerFactory(String pkg) {
            this.pkg = pkg;
        }

        public URLStreamHandler createURLStreamHandler(String protocol) {
            String name = pkg + "." + protocol + ".Handler";
            System.out.println("Loading handler class: " + name);
            // Loading this dummy class demonstrates the bootstrap
            // problem as the stream handler factory will be reentered
            // over and over again if the application class loader
            // shares the same stream handler factory.
            new Dummy();
            try {
                Class<?> c = Class.forName(name);
                return (URLStreamHandler)c.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException |
                    IllegalAccessException |
                    InstantiationException |
                    NoSuchMethodException |
                    InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class Dummy {
    }
}
