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
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * @test
 * @bug 8231924 8233091 8233272
 * @summary Confirm load (but not link) behavior of Class.forName()
 * @library /test/lib
 *
 * @compile MissingClass.java Container.java
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar classes.jar Container Container$1
 *
 * @run main/othervm NonLinking init
 * @run main/othervm NonLinking load
 */
/*
 * The @compile and '@main ClassFileInstaller' tasks above create a classes.jar
 * file containing the .class file for Container, but not MissingClass.
 */

public class NonLinking {
    public static void main(String[] args) throws Throwable {
        Path jarPath = Paths.get("classes.jar");
        URL url = jarPath.toUri().toURL();
        URLClassLoader ucl1 = new URLClassLoader("UCL1",
                                                 new URL[] { url },
                                                 null); // Don't delegate
        switch(args[0]) {
            case "init":
                try {
                    // Trying to initialize Container without MissingClass -> NCDFE
                    Class.forName("Container", true, ucl1);
                    throw new RuntimeException("Missed expected NoClassDefFoundError");
                } catch (NoClassDefFoundError expected) {
                    final String CLASSNAME = "MissingClass";
                    Throwable cause = expected.getCause();
                    if (!cause.getMessage().contains(CLASSNAME)) {
                        throw new RuntimeException("Cause of NoClassDefFoundError does not contain \"" + CLASSNAME + "\"", cause);
                    }
                }
                break;
            case "load":
                // Loading (but not linking) Container will succeed.
                // Before 8233091, this fails with NCDFE due to linking.
                Class.forName("Container", false, ucl1);
                break;
            default:
                throw new RuntimeException("Unknown command: " + args[0]);
        }
    }
}
