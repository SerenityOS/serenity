/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test id=default-cl
 * @requires vm.cds
 * @summary Test class loader constraint checks for archived classes
 * @library /test/lib
 *          /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @modules java.base/jdk.internal.misc
 *          jdk.httpserver
 * @run driver LoaderConstraintsTest
 */

/**
 * @test id=custom-cl
 * @requires vm.cds.custom.loaders
 * @summary Test class loader constraint checks for archived classes with custom class loader
 * @bug 8267347
 * @library /test/lib
 *          /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @modules java.base/jdk.internal.misc
 *          jdk.httpserver
 * @run driver LoaderConstraintsTest custom
 */


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jdk.test.lib.Asserts;
import jdk.test.lib.helpers.ClassFileInstaller;
import jdk.test.lib.Platform;

public class LoaderConstraintsTest  {
    static String mainClass = LoaderConstraintsApp.class.getName();
    static String httpHandlerClass = HttpHandler.class.getName().replace(".", "/");
    static String httpExchangeClass = HttpExchange.class.getName().replace(".", "/");
    static String appJar = null;
    static String appClasses[] = {
        mainClass,
        httpHandlerClass,
        httpExchangeClass,
        Asserts.class.getName(),
        MyHttpHandler.class.getName(),
        MyHttpHandlerB.class.getName(),
        MyHttpHandlerC.class.getName(),
        MyClassLoader.class.getName()
    };

    static String loaderMainClass = CustomAppLoader.class.getName();
    static String loaderJar = null;
    static String loaderClasses[] = {
        loaderMainClass
    };

    static void doTest() throws Exception  {
        TestCommon.dump(appJar, appClasses, "-Xlog:cds");
        String cmdLine[] =
            TestCommon.concat("-cp", appJar,
                              "-Xlog:cds",
                              "-Xlog:class+loader+constraints=debug",
                              "--add-exports",
                              "java.base/jdk.internal.misc=ALL-UNNAMED",
                              mainClass);
        runWithArchive(cmdLine, "1");
        runWithArchive(cmdLine, "2");
        runWithArchive(cmdLine, "3");
    }

    // Same as doTest, except that LoaderConstraintsApp and MyHttpHandler* are loaded
    // by a custom loader. This is test case for JDK-8267347.
    static void doTestCustomLoader() throws Exception  {
        String src = " source: " + appJar;
        String classList[] =
            TestCommon.concat(loaderClasses,
                              "java/lang/Object id: 1",
                              mainClass + " id: 2 super: 1" + src,
                              httpHandlerClass + " id: 3",
                              "MyHttpHandler id: 5 super: 1 interfaces: 3" + src,
                              "MyHttpHandlerB id: 6 super: 1 interfaces: 3" + src,
                              "MyHttpHandlerC id: 7 super: 1 interfaces: 3" + src);
        TestCommon.dump(loaderJar, classList, "-Xlog:cds");

        String cmdLine[] =
            TestCommon.concat("-cp", loaderJar,
                              "-Xlog:cds",
                              "-Xlog:class+loader+constraints=debug",
                              "--add-exports",
                              "java.base/jdk.internal.misc=ALL-UNNAMED",
                              loaderMainClass, appJar, mainClass);
        runWithArchive(cmdLine, "1");
        runWithArchive(cmdLine, "2");
        runWithArchive(cmdLine, "3");
    }

    static void runWithArchive(String[] optsMain, String arg) throws Exception {
        String cmd[] = TestCommon.concat(optsMain, arg);
        TestCommon.run(cmd).assertNormalExit();
    }

    public static void main(String... args) throws Exception {
        appJar = ClassFileInstaller.writeJar("loader_constraints.jar", appClasses);
        if (args.length == 0) {
            doTest();
        } else {
            loaderJar = ClassFileInstaller.writeJar("custom_app_loader.jar", loaderClasses);
            doTestCustomLoader();
        }
    }
}

