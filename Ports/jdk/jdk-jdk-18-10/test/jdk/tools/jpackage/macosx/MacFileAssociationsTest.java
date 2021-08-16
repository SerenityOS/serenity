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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.MacHelper;
import jdk.jpackage.test.MacHelper.PListWrapper;
import jdk.jpackage.test.Annotations.Test;

/**
 * Tests generation of app image with --file-associations and mac additional file
 * association arguments. Test will verify that arguments correctly propagated to
 * Info.plist.
 */

/*
 * @test
 * @summary jpackage with --file-associations and mac specific file association args
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @build MacFileAssociationsTest
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @requires (os.family == "mac")
 * @run main/othervm -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=MacFileAssociationsTest
 */
public class MacFileAssociationsTest {

    @Test
    public static void test() throws Exception {
        final Path propFile = TKit.workDir().resolve("fa.properties");
        Map<String, String> map = Map.ofEntries(
                entry("mime-type", "application/x-jpackage-foo"),
                entry("extension", "foo"),
                entry("description", "bar"),
                entry("mac.CFBundleTypeRole", "Viewer"),
                entry("mac.LSHandlerRank", "Default"),
                entry("mac.NSDocumentClass", "SomeClass"),
                entry("mac.LSTypeIsPackage", "true"),
                entry("mac.LSSupportsOpeningDocumentsInPlace", "false"),
                entry("mac.UISupportsDocumentBrowser", "false"),
                entry("mac.NSExportableTypes", "public.png, public.jpg"),
                entry("mac.UTTypeConformsTo", "public.image, public.data"));
        TKit.createPropertiesFile(propFile, map);

        JPackageCommand cmd = JPackageCommand.helloAppImage();
        cmd.addArguments("--file-associations", propFile);
        cmd.executeAndAssertHelloAppImageCreated();

        Path appImage = cmd.outputBundle();
        verifyPList(appImage);
    }

    private static void checkStringValue(PListWrapper plist, String key, String value) {
        String result = plist.queryValue(key);
        TKit.assertEquals(value, result, String.format(
                "Check value of %s plist key", key));
    }

    private static void checkBoolValue(PListWrapper plist, String key, Boolean value) {
        Boolean result = plist.queryBoolValue(key);
        TKit.assertEquals(value.toString(), result.toString(), String.format(
                "Check value of %s plist key", key));
    }

    private static void checkArrayValue(PListWrapper plist, String key,
            List<String> values) {
        List<String> result = plist.queryArrayValue(key);
        TKit.assertStringListEquals(values, result, String.format(
                "Check value of %s plist key", key));
    }

    private static void verifyPList(Path appImage) throws Exception {
        PListWrapper plist = MacHelper.readPListFromAppImage(appImage);
        checkStringValue(plist, "CFBundleTypeRole", "Viewer");
        checkStringValue(plist, "LSHandlerRank", "Default");
        checkStringValue(plist, "NSDocumentClass", "SomeClass");

        checkBoolValue(plist, "LSTypeIsPackage", true);
        checkBoolValue(plist, "LSSupportsOpeningDocumentsInPlace", false);
        checkBoolValue(plist, "UISupportsDocumentBrowser", false);

        checkArrayValue(plist, "NSExportableTypes", List.of("public.png",
                                                            "public.jpg"));

        checkArrayValue(plist, "UTTypeConformsTo", List.of("public.image",
                                                           "public.data"));
    }
}
