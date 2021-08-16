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
 * @bug 4991526 6514993 7197662
 * @summary Unit test for Preferences jar providers
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils jdk.test.lib.process.*
 *        PrefsSpi StubPreferencesFactory StubPreferences
 * @run testng PrefsSpiTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Arrays.asList;

import static jdk.test.lib.Utils.TEST_CLASSES;
import static jdk.test.lib.Utils.TEST_CLASS_PATH;

public class PrefsSpiTest {

    private static final Path SPIJAR = Path.of("extDir", "PrefsSpi.jar");
    private static final String SPIJAR_CP = TEST_CLASS_PATH
            + File.pathSeparator + SPIJAR.toString();

    @BeforeClass
    public void initialize() throws Exception {
        Path xdir = Path.of("jarDir");

        Path config = xdir.resolve("META-INF/services/java.util.prefs.PreferencesFactory");
        Files.createDirectories(config.getParent());
        Files.write(config, "StubPreferencesFactory".getBytes(), CREATE);

        String[] files = {"StubPreferencesFactory.class", "StubPreferences.class"};
        for (String f : files) {
            Path source = Path.of(TEST_CLASSES, f);
            Path target = xdir.resolve(source.getFileName());
            Files.copy(source, target, REPLACE_EXISTING);
        }

        JarUtils.createJarFile(SPIJAR, xdir);
    }

    @DataProvider
    public Object[][] testCases() {
        return new Object[][]{
            // CLI options,                        runtime arguments
            {List.of("-cp", SPIJAR_CP,
                     "-Djava.util.prefs.PreferencesFactory=StubPreferencesFactory"),
                                                   "StubPreferences"},
            {List.of("-cp", TEST_CLASS_PATH),      "java.util.prefs.*"},
            {List.of("-cp", SPIJAR_CP),            "StubPreferences"}
        };
    }

    @Test(dataProvider = "testCases")
    public void testProvider(List<String> opts, String pattern) throws Throwable {
        List<String> args = new ArrayList<>();
        args.add(JDKToolFinder.getJDKTool("java"));
        args.addAll(asList(Utils.getTestJavaOpts()));
        args.addAll(opts);
        args.add("-Djava.util.prefs.userRoot=.");
        args.add(PrefsSpi.class.getName());
        args.add(pattern);

        ProcessTools.executeCommand(args.stream()
                                        .filter(t -> !t.isEmpty())
                                        .toArray(String[]::new))
                    .shouldHaveExitValue(0);
    }

}
