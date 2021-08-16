/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests Japanese Calendar.
 * @bug 4609228
 * @modules java.base/sun.util
 *          java.base/sun.util.calendar
 * @compile
 *    CalendarAdapter.java
 *    CalendarTestEngine.java
 *    CalendarTestException.java
 *    Exceptions.java
 *    GregorianAdapter.java
 *    Result.java
 *    Symbol.java
 *    Variable.java
 *    JapaneseRollDayOfWeekTestGenerator.java
 * @run main/timeout=120/othervm JapaneseRollTests
 */

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JapaneseRollTests {
    private static List<String> TZ_TEST_LIST = Arrays.asList(
        "tz_japan.cts", "tz_pst.cts");
    private static Path srcPath = Paths.get(System.getProperty("test.src"));
    private static Path testPath = Paths.get(System.getProperty("test.classes"));

    public static void main(String[] args) throws Throwable {
        List<String> modeList = getFileNameList("params");
        //Test only 3 years by default
        String[] jpyear ={"3"};
        JapaneseRollDayOfWeekTestGenerator.generateTestScript(jpyear);

        for (String tz : TZ_TEST_LIST) {
            for(String mode:modeList){
                String[] ts = { srcPath + "/timezones/" + tz,
                        srcPath + "/params/" + mode,
                        testPath + "/rolldow.cts"};

                CalendarTestEngine.main(ts);
            }
        }
    }

    private static List<String> getFileNameList(String type ){
        List<String> fileList = new ArrayList<>();
        File dir = new File(srcPath + "/"+ type);
        File[] testFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".cts");
            }
        });
        for (File f:testFiles) {
            fileList.add(f.getName());
        }

        return fileList;
    }
}
