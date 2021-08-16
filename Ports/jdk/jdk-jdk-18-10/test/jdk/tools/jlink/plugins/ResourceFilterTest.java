/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test ResourceFilter class
 * @modules jdk.jlink/jdk.tools.jlink.internal.plugins
 * @run main ResourceFilterTest
 */

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import jdk.tools.jlink.internal.plugins.ResourceFilter;

public class ResourceFilterTest {

    public static void main(String[] args) throws Exception {
        new ResourceFilterTest().test();
    }

    public void test() throws Exception {
        String[] samples = {"toto.jcov", "/module/META-INF/services/MyProvider"};
        String[] patterns = {"*.jcov", "**/META-INF/**",
                             "glob:*.jcov", "glob:**/META-INF/**",
                             "regex:.*\\.jcov", "regex:.*/META-INF/.*"};
        ResourceFilter rf = ResourceFilter.includeFilter(Arrays.asList(patterns));
        for (String s : samples) {
            if (!rf.test(s)) {
                throw new Exception("Sample " + s + "not accepted");
            }
        }
        ResourceFilter rf2 = ResourceFilter.excludeFilter(Arrays.asList(patterns));
        for (String s : samples) {
            if (rf2.test(s)) {
                throw new Exception("Sample " + s + " accepted");
            }
        }

        // Excluded resource list in a file
        File resources = new File("resources.exc");
        resources.createNewFile();
        StringBuilder builder = new StringBuilder();
        for (String p : patterns) {
            builder.append(p).append("\n");
        }
        Files.write(resources.toPath(), builder.toString().getBytes());

        String[] input = {"@" + resources.getAbsolutePath()};
        ResourceFilter rf3 = ResourceFilter.includeFilter(Arrays.asList(input));
        for (String s : samples) {
            if (!rf3.test(s)) {
                throw new Exception("Sample " + s + "not accepted");
            }
        }
        ResourceFilter rf4 = ResourceFilter.excludeFilter(Arrays.asList(input));
        for (String s : samples) {
            if (rf4.test(s)) {
                throw new Exception("Sample " + s + " accepted");
            }
        }
    }
}
