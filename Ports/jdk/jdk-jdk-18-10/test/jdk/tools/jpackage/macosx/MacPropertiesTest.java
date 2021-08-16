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

import jdk.jpackage.test.TKit;
import jdk.jpackage.test.MacHelper;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;


/**
 * Test --mac-package-name, --mac-package-identifier parameters.
 */

/*
 * @test
 * @summary jpackage with --mac-package-name, --mac-package-identifier
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @requires (os.family == "mac")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile MacPropertiesTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=MacPropertiesTest
 */
public class MacPropertiesTest {
    @Test
    @Parameter("MacPackageNameTest")
    public void testPackageName(String packageName) {
        testParameterInAppImage("--mac-package-name", "CFBundleName",
                packageName);
    }

    @Test
    @Parameter("Foo")
    public void testPackageIdetifier(String packageId) {
        testParameterInAppImage("--mac-package-identifier", "CFBundleIdentifier",
                packageId);
    }

    private static void testParameterInAppImage(String jpackageParameterName,
            String plistKeyName, String value) {
        JPackageCommand cmd = JPackageCommand.helloAppImage()
                .addArguments(jpackageParameterName, value);

        cmd.executeAndAssertHelloAppImageCreated();

        var plist = MacHelper.readPListFromAppImage(cmd.outputBundle());

        TKit.assertEquals(value, plist.queryValue(plistKeyName), String.format(
                "Check value of %s plist key", plistKeyName));
    }
}
