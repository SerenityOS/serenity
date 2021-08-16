/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.*;

import tests.Helper;

/* @test
 * @bug 8232080
 * @summary Test the --vendor-version --vendor-url-bug plugins
 * @library ../../lib
 * @library /test/lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main VendorInfoPluginsTest
 */

public class VendorInfoPluginsTest {

    public static class Crasher {

        public static void main(String ... args) throws Exception {
            var uc = Class.forName("sun.misc.Unsafe");
            var f = uc.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            var u = (sun.misc.Unsafe)f.get(null);
            for (long a = 0; a < Long.MAX_VALUE; a += 8)
                u.putLong(a, -1L);
        }

    }

    private static final String VERSION = "XyzzyVM 3.14.15";
    private static final String BUG_URL = "https://bugs.xyzzy.com/";
    private static final String VM_BUG_URL = "https://bugs.xyzzy.com/crash/";

    public static void main(String[] args) throws Throwable {

        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }

        var module = "vendorinfo";
        helper.generateDefaultJModule(module);
        var image = helper.generateDefaultImage(new String[] {
                "--add-modules", "jdk.unsupported",
                "--vendor-version", VERSION,
                "--vendor-bug-url", BUG_URL,
                "--vendor-vm-bug-url", VM_BUG_URL },
            module).assertSuccess();
        helper.checkImage(image, module, null, null);

        // Properties and --version
        var launcher
            = image.resolve("bin/java"
                            + (System.getProperty("os.name").startsWith("Windows")
                               ? ".exe" : "")).toString();
        var oa = ProcessTools.executeProcess(launcher,
                                             "-Xmx64m",
                                             "-XshowSettings:properties",
                                             "--version");
        oa.stderrShouldMatch("^ +java.vendor.url.bug = " + BUG_URL + "$");
        oa.stderrShouldMatch("^ +java.vendor.version = " + VERSION + "$");
        oa.stdoutShouldMatch("^.*Runtime Environment " + VERSION + " \\(.*build.*$");
        oa.stdoutShouldMatch("^.*VM " + VERSION + " \\(.*build.*$");

        // VM error log
        oa = ProcessTools.executeProcess(launcher,
                                         "-Xmx64m",
                                         "-XX:-CreateCoredumpOnCrash",
                                         "--class-path",
                                         System.getProperty("test.classes"),
                                         "VendorInfoPluginsTest$Crasher");
        oa.stdoutShouldMatch("^# +" + VM_BUG_URL + "$");
        oa.stdoutShouldMatch("^.*Runtime Environment " + VERSION + " \\(.*$");
        oa.stdoutShouldMatch("^.*VM " + VERSION + " \\(.*$");

    }

}
