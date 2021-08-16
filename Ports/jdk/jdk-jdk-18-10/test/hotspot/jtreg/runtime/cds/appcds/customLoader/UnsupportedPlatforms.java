/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Ensure that support for AppCDS custom class loaders are not enabled on unsupported platforms.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/SimpleHello.java
 * @run driver UnsupportedPlatforms
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

public class UnsupportedPlatforms {
    public static String PLATFORM_NOT_SUPPORTED_WARNING =
        "AppCDS custom class loaders not supported on this platform";

    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.build("UnsupportedPlatforms", "SimpleHello");

        // Dump the archive
        String classlist[] = new String[] {
            "SimpleHello",
            "java/lang/Object id: 1",
            "CustomLoadee id: 2 super: 1 source: " + appJar
        };

        OutputAnalyzer out = TestCommon.dump(appJar, classlist);

        if (Platform.areCustomLoadersSupportedForCDS()) {
            out.shouldNotContain(PLATFORM_NOT_SUPPORTED_WARNING);
            out.shouldHaveExitValue(0);
        } else {
            out.shouldContain(PLATFORM_NOT_SUPPORTED_WARNING);
            out.shouldHaveExitValue(1);
        }
    }
}
