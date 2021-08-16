/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JvmtiEnv::AddToBootstrapClassLoaderSearch and JvmtiEnv::AddToSystemClassLoaderSearch should disable AppCDS
 * @requires vm.cds
 * @bug 8060592
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile test-classes/Hello.java
 * @compile test-classes/JvmtiApp.java
 * @run driver JvmtiAddPath
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class JvmtiAddPath {
    static String use_whitebox_jar;
    static String[] no_extra_matches = {};
    static String[] check_appcds_enabled = {
        "[class,load] ExtraClass source: shared object"
    };
    static String[] check_appcds_disabled = {
        "[class,load] ExtraClass source: file:"
    };

    static void run(String cp, String... args) throws Exception {
        run(no_extra_matches, cp, args);
    }

    static void run(String[] extra_matches, String cp, String... args) throws Exception {
        String[] opts = {"-cp", cp, "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", use_whitebox_jar};
        opts = TestCommon.concat(opts, args);
        TestCommon.run(opts).assertNormalExit(extra_matches);
    }

    public static void main(String[] args) throws Exception {
        JarBuilder.build("jvmti_addboot", "Hello");
        JarBuilder.build("jvmti_addapp", "Hello");
        JarBuilder.build("jvmti_app", "JvmtiApp", "ExtraClass");
        JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");

        // In all the test cases below, appJar does not contain Hello.class. Instead, we
        // append JAR file(s) that contain Hello.class to the boot classpath, the app
        // classpath, or both, and verify that Hello.class is loaded by the expected ClassLoader.
        String appJar = TestCommon.getTestJar("jvmti_app.jar");         // contains JvmtiApp.class
        String addappJar = TestCommon.getTestJar("jvmti_addapp.jar");   // contains Hello.class
        String addbootJar = TestCommon.getTestJar("jvmti_addboot.jar"); // contains Hello.class
        String twoAppJars = appJar + File.pathSeparator + addappJar;
        String wbJar = TestCommon.getTestJar("WhiteBox.jar");
        use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        TestCommon.testDump(appJar, TestCommon.list("JvmtiApp", "ExtraClass"), use_whitebox_jar);

        System.out.println("Test case 1: not adding any paths - Hello.class should not be found");
        run(check_appcds_enabled, appJar, "-Xlog:class+load", "JvmtiApp", "noadd"); // appcds should be enabled

        System.out.println("Test case 2: add to boot classpath only - should find Hello.class in boot loader");
        String[] toCheck = (TestCommon.isDynamicArchive()) ? check_appcds_enabled
                                                           : check_appcds_disabled;
        run(toCheck, appJar, "-Xlog:class+load", "JvmtiApp", "bootonly", addbootJar); // appcds should be disabled

        System.out.println("Test case 3: add to app classpath only - should find Hello.class in app loader");
        run(appJar, "JvmtiApp", "apponly", addappJar);

        System.out.println("Test case 4: add to boot and app paths - should find Hello.class in boot loader");
        run(appJar, "JvmtiApp", "appandboot", addbootJar, addappJar);

        System.out.println("Test case 5: add to app using -cp, but add to boot using JVMTI - should find Hello.class in boot loader");
        run(twoAppJars, "JvmtiApp", "bootonly", addappJar);

        System.out.println("Test case 6: add to app using AppCDS, but add to boot using JVMTI - should find Hello.class in boot loader");
        TestCommon.testDump(twoAppJars, TestCommon.list("JvmtiApp", "ExtraClass", "Hello"), use_whitebox_jar);
        if (!TestCommon.isDynamicArchive()) {
            // skip for dynamic archive, the Hello class will be loaded from
            // the dynamic archive
            run(twoAppJars, "JvmtiApp", "bootonly", addappJar);
        }

        System.out.println("Test case 7: add to app using AppCDS, no JVMTI calls - should find Hello.class in app loader");
        run(twoAppJars, "JvmtiApp", "noadd-appcds");
    }
}
