/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164742
 * @summary Test that jdk.compiler can materialize a service loader for arbitrary services
 * @run main FileManagerGetServiceLoaderTest
 */

import javax.tools.*;

public class FileManagerGetServiceLoaderTest {

    public static void main(String... args) throws Exception {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);

        /* FileManagerGetServiceLoaderTest.class is not really a service, but that is
           immaterial to the test which just verifies addUses would have been called
           so module boundary is not an issue for a class outside of jdk.compiler
        */
        java.util.ServiceLoader<?> loader = fm.getServiceLoader(StandardLocation.CLASS_PATH,
                                     FileManagerGetServiceLoaderTest.class);
        if (loader == null) {
            throw new AssertionError("Could not obtain service loader");
        }
    }
}
