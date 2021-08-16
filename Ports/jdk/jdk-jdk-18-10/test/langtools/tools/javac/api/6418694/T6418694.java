/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6418694
 * @summary JSR 199: JavaFileManager.hasLocation(Location)
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile T6418694.java
 * @run main T6418694
 */

import java.io.IOException;
import javax.tools.StandardLocation;

public class T6418694 extends ToolTester {
    void test(String... args) {
        for (StandardLocation loc : StandardLocation.values()) {
            switch (loc) {
            case CLASS_PATH:
            case SOURCE_PATH:
            case CLASS_OUTPUT:
            case PLATFORM_CLASS_PATH:
            case SYSTEM_MODULES:
                if (!fm.hasLocation(loc))
                    throw new AssertionError("Missing location " + loc);
                break;
            default:
                if (fm.hasLocation(loc))
                    throw new AssertionError("Extra location " + loc);
                break;
            }
        }
    }
    public static void main(String... args) throws IOException {
        try (T6418694 t = new T6418694()) {
            t.test(args);
        }
    }
}
