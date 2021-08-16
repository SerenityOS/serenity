/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6400207
 * @summary JSR 199: JavaFileManager.list and unset location
 * @author  Peter von der Ah\u00e9
 * @modules java.compiler
 *          jdk.compiler
 */

import java.util.*;
import javax.tools.*;
import static javax.tools.StandardLocation.*;
import static javax.tools.JavaFileObject.Kind.*;

public class T6400207 {
    static void testList(JavaFileManager fm,
                    JavaFileManager.Location location,
                    String packageName,
                    Set<JavaFileObject.Kind> kinds) throws Exception {
        boolean expectNpe =
            location == null ||
            packageName == null ||
            kinds == null;
        try {
            fm.list(location, packageName, kinds, false);
            if (expectNpe) {
                String message = "NullPointerException not thrown";
                if (location == null)
                    throw new AssertionError(message + " (location is null)");
                if (packageName == null)
                    throw new AssertionError(message + " (packageName is null)");
                if (kinds == null)
                    throw new AssertionError(message + " (kinds is null)");
                throw new AssertionError(message);
            }
        } catch (NullPointerException e) {
            if (!expectNpe)
                throw e;
        }
    }

    public static void main(String... args) throws Exception {
        try (JavaFileManager fm =
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            JavaFileManager.Location bogusLocation = locationFor("bogus");
            JavaFileManager.Location knownLocation = CLASS_PATH;
            String packageName = "java.lang";
            Set<JavaFileObject.Kind> kinds = EnumSet.of(CLASS);

            for (StandardLocation location : StandardLocation.values()) {
                if (location != locationFor(location.getName()))
                    throw new AssertionError(location + " != locationFor(" +
                                             location.getName() + ")");
            }

            testList(fm, null, null, null);
            testList(fm, bogusLocation, packageName, kinds);
            testList(fm, knownLocation, packageName, kinds);
            testList(fm, null, packageName, kinds);
            testList(fm, knownLocation, null, kinds);
            testList(fm, knownLocation, packageName, null);
            testList(fm, bogusLocation, null, kinds);
            testList(fm, bogusLocation, packageName, null);

            System.err.println("Test PASSED.");
        }
    }
}
