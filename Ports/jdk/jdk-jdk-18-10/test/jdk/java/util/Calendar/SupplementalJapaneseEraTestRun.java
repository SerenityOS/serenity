/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048123 8054214 8173423
 * @summary Test for jdk.calendar.japanese.supplemental.era support
 * @library /test/lib
 * @build SupplementalJapaneseEraTest
 * @run testng/othervm SupplementalJapaneseEraTestRun
 */

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import static java.util.Calendar.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SupplementalJapaneseEraTestRun {
    @DataProvider(name = "validprop")
    Object[][] validPropertyData() {
        return new Object[][] {
                //Tests with valid property values
                {"name=SupEra,abbr=S.E.,since="},
                {"name = SupEra, abbr = S.E., since = "},
        };
    }

    @DataProvider(name = "invalidprop")
    Object[][] invalidPropertyData() {
        return new Object[][] {
                //Tests with invalid property values
                {"=SupEra,abbr=S.E.,since="},
                {"=,abbr=S.E.,since="},
                {"name,abbr=S.E.,since="},
                {"abbr=S.E.,since="},
                {"name=SupEra,since="},
                {"name=,abbr=S.E.,since"},
                {"name=SupEra,abbr=,since="},
                {"name=SupEra,abbr=S.E."},
                {"name=SupEra,abbr=S.E.,since=0"},
                //since=9223372036854775808 the number means Long.MAX_VALUE+1
                {"name=SupEra,abbr=S.E.,since=9223372036854775808"},
        };
    }

    @Test(dataProvider = "validprop")
    public void ValidPropertyValuesTest(String prop)
            throws Throwable {
        //get the start time of the fictional next era
        String startTime = getStartTime();
        testRun(prop + startTime, List.of("-t"));
    }

    @Test(dataProvider = "invalidprop")
    public void InvalidPropertyValuesTest(String prop)
            throws Throwable {
        //get the start time of the fictional next era
        String startTime = getStartTime();
        //get the name of the current era to be used to confirm that
        //invalid property values are ignored.
        String currentEra = getCurrentEra();
        testRun(prop + startTime, List.of("-b", currentEra));
    }

    private static void testRun(String property, List<String> javaParam)
            throws Throwable{
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-cp")
                .addToolArg(Utils.TEST_CLASS_PATH)
                .addToolArg("-Djdk.calendar.japanese.supplemental.era=" + property)
                .addToolArg("SupplementalJapaneseEraTest");
        for (String para: javaParam) {
            launcher.addToolArg(para);
        }
        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        System.out.println(property + ":pass");
        if (exitCode != 0) {
            System.out.println(property + ":fail");
            throw new RuntimeException("Unexpected exit code: " + exitCode);
        }
    }

    private static String getStartTime(){
        Calendar cal = new Calendar.Builder().setCalendarType("japanese")
                .setTimeZone(TimeZone.getTimeZone("GMT")).setFields(ERA, 5)
                .setDate(200, FEBRUARY, 11).build();
        return String.valueOf(cal.getTimeInMillis());
    }

    private static String getCurrentEra(){
        Calendar jcal = new Calendar.Builder()
                .setCalendarType("japanese")
                .setFields(YEAR, 1, DAY_OF_YEAR, 1)
                .build();
        return jcal.getDisplayName(ERA, LONG, Locale.US);
    }
}
