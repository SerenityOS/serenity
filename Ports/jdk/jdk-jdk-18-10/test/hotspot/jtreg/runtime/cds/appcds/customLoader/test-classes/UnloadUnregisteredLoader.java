/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

public class UnloadUnregisteredLoader {
    public static void main(String args[]) throws Exception {
        String path = args[0];
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};
        WhiteBox wb = WhiteBox.getWhiteBox();
        String className = "CustomLoadee";

        for (int i=0; i<5; i++) {
            doit(urls, className, (i == 0));

            ClassUnloadCommon.triggerUnloading();
            ClassUnloadCommon.failIf(wb.isClassAlive(className), "should have been unloaded");
        }
    }

  public static void doit(URL urls[], String className, boolean isFirstTime) throws Exception {
        ClassLoader appLoader = UnloadUnregisteredLoader.class.getClassLoader();
        URLClassLoader custLoader = new URLClassLoader(urls, appLoader);

        Class klass = custLoader.loadClass(className);
        WhiteBox wb = WhiteBox.getWhiteBox();
        if (wb.isSharedClass(UnloadUnregisteredLoader.class)) {
            if (isFirstTime) {
                // First time: we should be able to load the class from the CDS archive
                if (!wb.isSharedClass(klass)) {
                    throw new RuntimeException("wb.isSharedClass(klass) should be true for first time");
                }
            } else {
                // Second time:  the class in the CDS archive is not available, because it has not been cleaned
                // up (see bug 8140287), so we must load the class dynamically.
                //
                // FIXME: after 8140287 is fixed, class should be shard regardless of isFirstTime.
                if (wb.isSharedClass(klass)) {
                    throw new RuntimeException("wb.isSharedClass(klass) should be false for second time");
                }
            }
        }
    }
}
