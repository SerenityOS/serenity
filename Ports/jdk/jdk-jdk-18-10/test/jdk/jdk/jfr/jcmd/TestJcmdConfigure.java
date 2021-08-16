/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.internal.Repository;
import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.jfr.internal.Options;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

/**
 * @test
 * @summary The test verifies JFR.configure command
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm -Xlog:jfr=info:file=jfr_info.txt jdk.jfr.jcmd.TestJcmdConfigure
 */
public class TestJcmdConfigure {

    private static final String DUMPPATH = "dumppath";
    private static final String STACK_DEPTH = "stackdepth";
    private static final String GLOBAL_BUFFER_COUNT = "globalbuffercount";
    private static final String GLOBAL_BUFFER_SIZE = "globalbuffersize";
    private static final String THREAD_BUFFER_SIZE = "thread_buffer_size";
    private static final String MAX_CHUNK_SIZE = "maxchunksize";
    private static final String SAMPLE_THREADS = "samplethreads";
    private static final String UNSUPPORTED_OPTION = "unsupportedoption";

    private static final String REPOSITORYPATH_1 = "." + File.pathSeparator + "repo1";
    private static final String REPOSITORYPATH_2 = "." + File.pathSeparator + "repo2";

    private static final String REPOSITORYPATH_SETTING_1 = "repositorypath="+REPOSITORYPATH_1;
    private static final String REPOSITORYPATH_SETTING_2 = "repositorypath="+REPOSITORYPATH_2;
    private static final String JFR_UNIFIED_LOG_FILE = "jfr_info.txt";

    public static void main(String[] args) throws Exception {
        //
        // Simple sanity tests against what is available in Java,
        // before Flight Recorder is loaded. To do:
        //
        // - set values when JFR is running, check for errors.
        // - validate against output from JFR.configure
        // - where feasible, check if they are respected
        //

        String dumpPath = Utils.createTempDirectory("dump-path-").toAbsolutePath().toString();

        test(DUMPPATH, dumpPath);
        test(STACK_DEPTH, 15);
        test(GLOBAL_BUFFER_COUNT, 7);
        test(GLOBAL_BUFFER_SIZE, 6);
        test(THREAD_BUFFER_SIZE, 5);
        test(MAX_CHUNK_SIZE, 14 * 1000 * 1000);
        test(SAMPLE_THREADS, false);
        test(SAMPLE_THREADS, true);
        testNegative(UNSUPPORTED_OPTION, 100000);
        testNegative(MAX_CHUNK_SIZE, -500);

        testRepository();

        if (!testExceptions.isEmpty()) {
            for (Exception e : testExceptions) {
                System.out.println("Error: " + e.getMessage());
            }
            throw testExceptions.get(0);
        }
    }

    private static List<Exception> testExceptions = new ArrayList<>();

    private static void test(String configName, Object value) {
        JcmdHelper.jcmd("JFR.configure", configName + "=" + value);
        Object actualValue = getOption(configName);
        System.out.format("Test param='%s', expected='%s', actual='%s'%n", configName, value, actualValue);
        try {
            // Need convert to string to compare Integer and Long
            Asserts.assertEquals(value.toString(), actualValue.toString(), "Wrong JFR.configure " + configName);
        } catch (Exception e) {
            testExceptions.add(e);
        }
    }

    private static void testNegative(String configName, Object value) {
        try {
            JcmdHelper.jcmd(1, "JFR.configure", configName + "=" + value);
        } catch(Exception e) {
            testExceptions.add(e);
        }
    }

    private static Object getOption(String name) {
        switch (name) {
            case DUMPPATH: return Options.getDumpPath().toString();
            case STACK_DEPTH: return Options.getStackDepth();
            case GLOBAL_BUFFER_COUNT: return Options.getGlobalBufferCount();
            case GLOBAL_BUFFER_SIZE: return Options.getGlobalBufferSize();
            case THREAD_BUFFER_SIZE: return Options.getThreadBufferSize();
            case MAX_CHUNK_SIZE: return Options.getMaxChunkSize();
            case SAMPLE_THREADS: return Options.getSampleThreads();
            default: throw new RuntimeException("Unknown option " + name);
        }
    }

    private static void testRepository(){
        final String findWhat = "[info][jfr] Same base repository path " + REPOSITORYPATH_1 + " is set";

        try {
            JcmdHelper.jcmd("JFR.configure", REPOSITORYPATH_SETTING_1);
            SafePath initialPath = Repository.getRepository().getRepositoryPath();

            JcmdHelper.jcmd("JFR.configure", REPOSITORYPATH_SETTING_1);
            SafePath samePath = Repository.getRepository().getRepositoryPath();
            Asserts.assertTrue(samePath.equals(initialPath));

            List<String> lines = Files.readAllLines(Paths.get(JFR_UNIFIED_LOG_FILE));
            Asserts.assertTrue(lines.stream().anyMatch(l->l.contains(findWhat)));

            JcmdHelper.jcmd("JFR.configure", REPOSITORYPATH_SETTING_2);
            SafePath changedPath = Repository.getRepository().getRepositoryPath();

            Asserts.assertFalse(changedPath.equals(initialPath));

        } catch(Exception e) {
            testExceptions.add(e);
        }
    }
}
