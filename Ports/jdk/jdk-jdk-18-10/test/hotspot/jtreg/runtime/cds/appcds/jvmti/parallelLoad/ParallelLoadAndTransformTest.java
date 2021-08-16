/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Load app classes from CDS archive in parallel threads,
 * use initial transformation (CFLH)
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *     /test/hotspot/jtreg/runtime/cds/appcds/test-classes /test/hotspot/jtreg/runtime/cds/appcds/jvmti
 *     /test/hotspot/jtreg/testlibrary/jvmti
 * @requires vm.cds
 * @requires !vm.graal.enabled
 * @requires vm.jvmti
 * @build TransformUtil TransformerAgent ParallelLoad
 * @run driver ParallelLoadAndTransformTest
 */

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jdk.test.lib.helpers.ClassFileInstaller;

public class ParallelLoadAndTransformTest {

    public static void main(String[] args) throws Exception {
        String prop = "-Dappcds.parallel.transform.mode=cflh";
        String appJar = ClassFileInstaller.writeJar("parallel_load.jar",
                            getClassList(true));
        String agentJar = prepareAgent();

        TestCommon.test(appJar, getClassList(false),
                        "-javaagent:" + agentJar + "=ParallelClassTr.*",
                        prop, "ParallelLoad");
    }


    private static String[] getClassList(boolean includeWatchdog) {
        List<String> classList =
            IntStream.range(0, ParallelClassesTransform.NUMBER_OF_CLASSES)
            .mapToObj(i -> "ParallelClassTr" + i)
            .collect(Collectors.toList());

        classList.add("ParallelLoad");
        classList.add("ParallelLoadThread");
        if (includeWatchdog)
            classList.add("ParallelLoadWatchdog");

        return classList.toArray(new String[0]);
    }


    // Agent is the same for all test cases
    private static String prepareAgent() throws Exception {
        String agentClasses[] = {
            "TransformerAgent",
            "TransformerAgent$SimpleTransformer",
            "TransformUtil"
        };

        String manifest = "../../../../../testlibrary/jvmti/TransformerAgent.mf";

        return ClassFileInstaller.writeJar("TransformerAgent.jar",
            ClassFileInstaller.Manifest.fromSourceFile(manifest),
                                        agentClasses);
    }

}
