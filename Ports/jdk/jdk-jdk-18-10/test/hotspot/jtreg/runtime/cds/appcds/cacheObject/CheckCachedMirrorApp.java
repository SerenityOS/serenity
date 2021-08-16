/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

//
// Test class mirror objects are cached when open archive heap objects are mapped:
//  - Well-known shared library classes:
//      java.lang.Object
//      java.lang.String
//  - Shared application class loaded by the system class loader
//  - Shared application class loaded user defined class loader
//
public class CheckCachedMirrorApp {
    static WhiteBox wb;
    public static void main(String args[]) throws Exception {
        String path = args[0];
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};

        URLClassLoader loader = new URLClassLoader(urls);
        Class hello = loader.loadClass("Hello");
        System.out.println("Loaded " + hello + " from " + url + " using loader " + loader);

        wb = WhiteBox.getWhiteBox();

        if (!wb.areOpenArchiveHeapObjectsMapped()) {
            System.out.println("Archived open_archive_heap objects are not mapped.");
            System.out.println("This may happen during normal operation. Test Skipped.");
            return;
        }

        // Well-known shared library classes
        Class object_class = Object.class;
        checkMirror(object_class, true);
        Class string_class = String.class;
        checkMirror(string_class, true);

        // Shared app class
        Class app_class = CheckCachedMirrorApp.class;
        checkMirror(app_class, true);

        // Hello is shared class and loaded by the 'loader' defined in current app.
        // It should not have cached resolved_references.
        Class class_with_user_defined_loader = hello;
        checkMirror(class_with_user_defined_loader, false);
    }

    static void checkMirror(Class c, boolean mirrorShouldBeArchived) {
        System.out.print("Check cached mirror for " + c);
        if (wb.isSharedClass(c)) {
            // Check if the Class object is cached
            if (mirrorShouldBeArchived && wb.isShared(c)) {
                System.out.println(c + " mirror is cached. Expected.");
            } else if (!mirrorShouldBeArchived && !wb.isShared(c)) {
                System.out.println(c + " mirror is not cached. Expected.");
            } else if (mirrorShouldBeArchived && !wb.isShared(c)) {
                throw new RuntimeException(
                    "FAILED. " + c + " mirror is not cached.");
            } else {
                throw new RuntimeException(
                    "FAILED. " + c + " mirror should not be cached.");
            }
        } else {
          System.out.println("Class " + c + "is not shared, skipping the check for mirror");
        }
    }
}
