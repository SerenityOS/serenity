/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary -XX:DumpLoadedClassList should support custom loaders
 * @bug 8265602
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib
 *          /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 *          test-classes
 * @build CustomLoaderApp OldClass CustomLoadee CustomLoadee2
 *        CustomLoadee3Child CustomLoadee4WithLambda
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar app.jar CustomLoaderApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar custom.jar
 *             OldClass CustomLoadee
 *             CustomLoadee2 CustomInterface2_ia CustomInterface2_ib
 *             CustomLoadee3 CustomLoadee3Child
 *             CustomLoadee4WithLambda
 * @run driver CustomClassListDump
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.helpers.ClassFileInstaller;

public class CustomClassListDump {
    private static String appJar = ClassFileInstaller.getJarPath("app.jar");
    private static String customJar = ClassFileInstaller.getJarPath("custom.jar");
    private static String classList = "app.list";
    private static String commandLine[] = {
        "-cp", appJar,
        "CustomLoaderApp",
        customJar,
        "unregistered",
        "CustomLoadee",
        "CustomLoadee2",
        "CustomLoadee3Child",
        "CustomLoadee4WithLambda",
        "OldClass",
    };

    public static void main(String[] args) throws Exception {
        // Dump the classlist and check that custom-loader classes are in there.
        CDSTestUtils.dumpClassList(classList, commandLine)
            .assertNormalExit();

        String listData = new String(Files.readAllBytes(Paths.get(classList)));
        check(listData, true, "CustomLoaderApp id: [0-9]+");
        check(listData, true, "CustomLoadee id: [0-9]+ super: [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomInterface2_ia id: [0-9]+ super: [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomInterface2_ib id: [0-9]+ super: [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomLoadee2 id: [0-9]+ super: [0-9]+ interfaces: [0-9]+ [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomLoadee3 id: [0-9]+ super: [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomLoadee3Child id: [0-9]+ super: [0-9]+ source: .*/custom.jar");
        check(listData, true, "CustomLoadee4WithLambda id: [0-9]+ super: [0-9]+ source: .*/custom.jar");

        // We don't support archiving of Lambda proxies for custom loaders.
        check(listData, false, "@lambda-proxy.*CustomLoadee4WithLambda");

        // Dump the static archive
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar,
                       "-Xlog:cds+class=debug",
                       "-XX:SharedClassListFile=" + classList);
        CDSTestUtils.createArchiveAndCheck(opts)
            .shouldContain("unreg CustomLoadee")
            .shouldContain("unreg CustomLoadee2")
            .shouldContain("unreg CustomLoadee3Child")
            .shouldContain("unreg OldClass ** unlinked");

        // Use the dumped static archive
        opts = (new CDSOptions())
            .setUseVersion(false)
            .addPrefix("-cp", appJar)
            .addSuffix("-Xlog:class+load,verification")
            .addSuffix(commandLine);
        CDSTestUtils.run(opts)
            .assertNormalExit("CustomLoadee source: shared objects file",
                              "CustomLoadee2 source: shared objects file",
                              "CustomLoadee3Child source: shared objects file",
                              "OldClass source: shared objects file",
                              "Verifying class OldClass with old format");
    }

    static void check(String listData, boolean mustMatch, String regexp) throws Exception {
        Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(listData);
        boolean found = matcher.find();
        if (mustMatch && !found) {
            System.out.println(listData);
            throw new RuntimeException("Pattern \"" + regexp + "\" not found in classlist");
        }

        if (!mustMatch && found) {
            throw new RuntimeException("Pattern \"" + regexp + "\" found in in classlist: \""
                                       + matcher.group() + "\"");
        }
    }
}
