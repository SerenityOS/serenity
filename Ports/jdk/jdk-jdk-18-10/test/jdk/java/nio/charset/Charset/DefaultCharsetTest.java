/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4772857
 * @summary Unit test for Charset.defaultCharset
 * @requires os.family == "linux"
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        Default
 * @run testng DefaultCharsetTest
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class DefaultCharsetTest {

    private static final ProcessBuilder pb
            = ProcessTools.createTestJvm(Default.class.getName());
    private static final Map<String, String> env = pb.environment();
    private static String UNSUPPORTED = null;

    @BeforeClass
    public static void checkSupports() throws Exception {
        UNSUPPORTED = runWithLocale("nonexist");
    }

    @DataProvider
    public static Iterator<Object[]> locales() {
        List<Object[]> data = new ArrayList<>();
        data.add(new String[]{"en_US", "iso-8859-1"});
        data.add(new String[]{"ja_JP.utf8", "utf-8"});
        data.add(new String[]{"tr_TR", "iso-8859-9"});
        data.add(new String[]{"C", "us-ascii"});
        data.add(new String[]{"ja_JP", "x-euc-jp-linux"});
        data.add(new String[]{"ja_JP.eucjp", "x-euc-jp-linux"});
        data.add(new String[]{"ja_JP.ujis", "x-euc-jp-linux"});
        data.add(new String[]{"ja_JP.utf8", "utf-8"});
        return data.iterator();
    }

    @Test(dataProvider = "locales")
    public void testDefaultCharset(String locale, String expectedCharset)
            throws Exception {
        String actual = runWithLocale(locale);
        if (UNSUPPORTED.equals(actual)) {
            System.out.println(locale + ": Locale not supported, skipping...");
        } else {
            assertTrue(actual.equalsIgnoreCase(expectedCharset),
                       String.format("LC_ALL = %s, got defaultCharset = %s, "
                               + "NOT as expected %s",
                               locale, actual, expectedCharset));
        }
    }

    private static String runWithLocale(String locale) throws Exception {
        env.remove("LC_ALL");
        env.put("LC_ALL", locale);
        return ProcessTools.executeProcess(pb)
                           .shouldHaveExitValue(0)
                           .getStdout()
                           .replace(System.lineSeparator(), "");
    }
}
