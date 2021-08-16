/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8269335
 * @summary Tests SVML shared library is loaded successfully.
 * @modules jdk.incubator.vector
 * @requires vm.compiler2.enabled
 * @requires os.arch == "x86_64" | os.arch == "amd64"
 * @requires os.family == "linux" | os.family == "windows"
 * @library /test/lib
 * @run main LoadSvmlTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorSpecies;


public class LoadSvmlTest {

    private static class VectorTest {

        static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

        public static void main(String[] args) throws Exception {
            float a[] = new float [1024];
            float r[] = new float [1024];

            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = FloatVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.SINH).intoArray(r, i);
            }
        }
    }

    public static void main(String... args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xmn8m", "-Xlog:library=info",
            "--add-modules=jdk.incubator.vector",
            VectorTest.class.getName());

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldMatch("Loaded library .*svml");
    }
}
