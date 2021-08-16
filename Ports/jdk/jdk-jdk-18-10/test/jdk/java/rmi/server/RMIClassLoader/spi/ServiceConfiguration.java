/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.io.File;
import java.io.IOException;

public class ServiceConfiguration {
    public static void installServiceConfigurationFile() {
        String filename = "java.rmi.server.RMIClassLoaderSpi";

        File dstDir = new File(TestLibrary.getProperty("test.classes", "."),
                               "META-INF/services");
        if (!dstDir.exists()) {
            if (!dstDir.mkdirs()) {
                throw new RuntimeException(
                    "could not create META-INF/services directory " + dstDir);
            }
        }
        File dstFile = new File(dstDir, filename);

        File srcDir = new File(TestLibrary.getProperty("test.src", "."));
        File srcFile = new File(srcDir, filename);

        try {
            TestLibrary.copyFile(srcFile, dstFile);
        } catch (IOException e) {
            throw new RuntimeException("could not install " + dstFile, e);
        }
    }
}
