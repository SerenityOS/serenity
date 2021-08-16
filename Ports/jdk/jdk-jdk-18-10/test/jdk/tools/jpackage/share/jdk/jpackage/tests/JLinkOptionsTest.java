/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import java.util.Collection;
import java.util.List;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;

/*
 * @test
 * @summary jpackage application version testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile JLinkOptionsTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.JLinkOptionsTest
 */

public final class JLinkOptionsTest {

    @Parameters
    public static Collection input() {
        return List.of(new Object[][]{
            // default but with strip-native-commands removed
            {"Hello", new String[]{
                    "--jlink-options",
                    "--strip-debug --no-man-pages --no-header-files",
                    },
                    // non modular should have everything
                    new String[]{"jdk.jartool", "jdk.unsupported"},
                    null,
                    },
            // multiple jlink-options
            {"com.other/com.other.Hello", new String[]{
                    "--jlink-options",
                    "--strip-debug --no-man-pages --no-header-files",
                    "--jlink-options",
                    "--bind-services",
                    },
                    // with bind-services should have some services
                    new String[]{"java.smartcardio", "jdk.crypto.ec"},
                    null,
                    },
            // bind-services
            {"Hello", new String[]{
                    "--jlink-options", "--bind-services",
                    },
                    // non modular should have everything
                    new String[]{"jdk.jartool", "jdk.unsupported"},
                    null,
                    },

            // jlink-options --bind-services
            {"com.other/com.other.Hello", new String[]{
                    "--jlink-options", "--bind-services",
                    },
                    // with bind-services should have some services
                    new String[]{"java.smartcardio", "jdk.crypto.ec"},
                    null,
                    },

            // limit modules
            {"com.other/com.other.Hello", new String[]{
                    "--jlink-options",
                    "--limit-modules java.base,java.datatransfer,java.xml,java.prefs,java.desktop,com.other",
                    },
                    // should have whatever it needs
                    new String[]{"java.base", "com.other"},
                    // should not have whatever it doesn't need
                    new String[]{"jdk.jpackage"},
                    },

            // bind-services and limit-options
            {"com.other/com.other.Hello", new String[]{
                    "--jlink-options",
                    "--bind-services",
                    "--jlink-options",
                    "--limit-modules java.base,java.datatransfer,java.xml,java.prefs,java.desktop,com.other,java.smartcardio",
                    },
                    // with bind-services should have some services
                    new String[]{"java.smartcardio"},
                    // but not limited
                    new String[]{"jdk.crypto.ec"},
                    },

        });
    }

    public JLinkOptionsTest(String javaAppDesc, String[] jpackageArgs, String[] required, String[] prohibited) {
        this.required = required;
        this.prohibited = prohibited;
        cmd = JPackageCommand
                .helloAppImage(javaAppDesc)
                .ignoreDefaultRuntime(true)
                .addArguments(jpackageArgs);
    }

    @Test
    public void test() {
        cmd.executeAndAssertHelloAppImageCreated();

        List<String> release = cmd.readRuntimeReleaseFile();
        List<String> mods = List.of(release.get(1));
        if (required != null) {
            for (String s : required) {
                TKit.assertTextStream(s).label("mods").apply(mods.stream());
            }
        }
        if (prohibited != null) {
            for (String s : prohibited) {
                TKit.assertTextStream(s).label("mods").negate().apply(mods.stream());
            }
        }
    }

    private final String[] required;
    private final String[] prohibited;
    private final JPackageCommand cmd;
}
