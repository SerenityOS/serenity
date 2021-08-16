/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


// This is a handy class for running an application inside a custom class loader. This
// is used for testing CDS handling of unregistered classes (i.e., archived classes loaded
// by custom class loaders).
//
// See test/hotspot/jtreg/runtime/cds/appcds/loaderConstraints/LoaderConstraintsTest.java
// for an example.
public class CustomAppLoader {
    // args[0] = App JAR file
    // args[1] = App main class
    // args[2...] = arguments for the main class
    public static void main(String args[]) throws Throwable {
        File f = new File(args[0]);
        URL[] classLoaderUrls = new URL[] {new URL("file://" + f.getCanonicalPath())};
        URLClassLoader loader = new URLClassLoader(classLoaderUrls, CustomAppLoader.class.getClassLoader());
        Class k = Class.forName(args[1], true, loader);
        Class parameterTypes[] = new Class[] {String[].class};
        Method mainMethod = k.getDeclaredMethod("main", parameterTypes);
        String appArgs[] = new String[args.length - 2];
        Object invokeArgs[] = new Object[] {appArgs};
        for (int i = 0; i < appArgs.length; i++) {
            appArgs[i] = args[i + 2];
        }
        mainMethod.invoke(null, invokeArgs);
    }
}