/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URL;
import java.net.URLClassLoader;
import sun.hotspot.WhiteBox;
import jdk.test.lib.classloader.ClassUnloadCommon;

public class HelloUnload {
    private static String className = "CustomLoadee";

    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
            throw new RuntimeException("Unexpected number of arguments: expected 3, actual " + args.length);
        }

        String path = args[0];
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};
        System.out.println(path);
        System.out.println(url);

        // unload the custom class loader
        boolean doUnload = false;
        if (args[1].equals("true")) {
            doUnload = true;
        } else if (args[1].equals("false")) {
            doUnload = false;
        } else {
            throw new RuntimeException("args[1] can only be either \"true\" or \"false\", actual " + args[1]);
        }

        // should the CustomLoadee class be in the shared archive
        boolean inArchive = false;
        if (args[2].equals("true")) {
            inArchive = true;
        } else if (args[2].equals("false")) {
            inArchive = false;
        } else {
            throw new RuntimeException("args[2] can only be either \"true\" or \"false\", actual " + args[1]);
        }

        URLClassLoader urlClassLoader =
            new URLClassLoader("HelloClassLoader", urls, null);
        Class c = Class.forName(className, true, urlClassLoader);
        System.out.println(c);
        System.out.println(c.getClassLoader());
        Object o = c.newInstance();

        // [1] Check that CustomLoadee is defined by the correct loader
        if (c.getClassLoader() != urlClassLoader) {
            throw new RuntimeException("c.getClassLoader() == " + c.getClassLoader() +
                                       ", expected == " + urlClassLoader);
        }

        // [2] Check that CustomLoadee is loaded from shared archive.
        WhiteBox wb = WhiteBox.getWhiteBox();
        if(wb.isSharedClass(HelloUnload.class)) {
            if (inArchive && !wb.isSharedClass(c)) {
                throw new RuntimeException("wb.isSharedClass(c) should be true");
            }
        }

        ClassUnloadCommon.failIf(!wb.isClassAlive(className), "should be live here");

        if (doUnload) {
            String loaderName = urlClassLoader.getName();
            int loadedRefcount = wb.getSymbolRefcount(loaderName);
            System.out.println("Refcount of symbol " + loaderName + " is " + loadedRefcount);

            urlClassLoader = null; c = null; o = null;
            ClassUnloadCommon.triggerUnloading();
            System.out.println("Is CustomLoadee alive? " + wb.isClassAlive(className));
            ClassUnloadCommon.failIf(wb.isClassAlive(className), "should have been unloaded");

            int unloadedRefcount = wb.getSymbolRefcount(loaderName);
            System.out.println("Refcount of symbol " + loaderName + " is " + unloadedRefcount);

            // refcount of a permanent symbol will not be decremented
            if (loadedRefcount != 65535) {
                ClassUnloadCommon.failIf(unloadedRefcount != (loadedRefcount - 1), "Refcount must be decremented");
            }
        }
    }
}
