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

/*
 * @test
 * @summary Same as HelloCustom, but add -XX:StartFlightRecording:dumponexit=true to the runtime
 *          options. This makes sure that the shared classes are compatible with both
 *          JFR and JVMTI ClassFileLoadHook.
 * @requires vm.hasJFR
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/HelloUnload.java test-classes/CustomLoadee.java
 * @build sun.hotspot.WhiteBox jdk.test.lib.classloader.ClassUnloadCommon
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar HelloUnload
 *                 jdk.test.lib.classloader.ClassUnloadCommon
 *                 jdk.test.lib.classloader.ClassUnloadCommon$1
 *                 jdk.test.lib.classloader.ClassUnloadCommon$TestFailure
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello_custom.jar CustomLoadee
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver HelloCustom_JFR
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class HelloCustom_JFR {
    public static void main(String[] args) throws Exception {
        HelloCustom.run("-XX:StartFlightRecording:dumponexit=true", "-Xlog:cds+jvmti=debug");
    }
}
