/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @bug 8189134
 * @summary Tests the system properties
 * @modules jdk.localedata
 * @build DefaultLocaleTest
 * @run testng/othervm SystemPropertyTests
 */

import static jdk.test.lib.process.ProcessTools.executeTestJava;
import static org.testng.Assert.assertTrue;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Locale.getDefault() reflects the system property. Note that the
 * result may change depending on the CLDR releases.
 */
@Test
public class SystemPropertyTests {

    private static String LANGPROP = "-Duser.language=en";
    private static String SCPTPROP = "-Duser.script=";
    private static String CTRYPROP = "-Duser.country=US";

    @DataProvider(name="data")
    Object[][] data() {
        return new Object[][] {
            // system property, expected default, expected format, expected display
            {"-Duser.extensions=u-ca-japanese",
             "en_US_#u-ca-japanese",
             "en_US_#u-ca-japanese",
             "en_US_#u-ca-japanese",
            },

            {"-Duser.extensions=u-ca-japanese-nu-thai",
             "en_US_#u-ca-japanese-nu-thai",
             "en_US_#u-ca-japanese-nu-thai",
             "en_US_#u-ca-japanese-nu-thai",
            },

            {"-Duser.extensions=foo",
             "en_US",
             "en_US",
             "en_US",
            },

            {"-Duser.extensions.format=u-ca-japanese",
             "en_US",
             "en_US_#u-ca-japanese",
             "en_US",
            },

            {"-Duser.extensions.display=u-ca-japanese",
             "en_US",
             "en_US",
             "en_US_#u-ca-japanese",
            },
        };
    }

    @Test(dataProvider="data")
    public void runTest(String extprop, String defLoc,
                        String defFmtLoc, String defDspLoc) throws Exception {
        int exitValue = executeTestJava(LANGPROP, SCPTPROP, CTRYPROP,
                                    extprop, "DefaultLocaleTest", defLoc, defFmtLoc, defDspLoc)
                            .outputTo(System.out)
                            .errorTo(System.out)
                            .getExitValue();

        assertTrue(exitValue == 0);
    }
}
