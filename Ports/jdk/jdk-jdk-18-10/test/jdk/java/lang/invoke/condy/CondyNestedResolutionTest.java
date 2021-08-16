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
 * @bug 8203435
 * @summary Test JVMs 5.4.3.6 with respect to a dynamically-computed constant and circularity.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile CondyNestedResolution.jcod
 * @run main/othervm CondyNestedResolutionTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

/*
 * JVMs section 5.4.3.6 Dynamically-Computed Constant and Call Site Resolution
 *     "Let X be the symbolic reference currently being resolved, and let Y be a static argument of X
 *      to be resolved as described above. If X and Y are both dynamically-computed constants, and if Y
 *      is either the same as X or has a static argument that references X through its static arguments,
 *      directly or indirectly, resolution fails with a StackOverflowError.
 */
public class CondyNestedResolutionTest {
    public static void main(String args[]) throws Throwable {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("CondyNestedResolution");
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("StackOverflowError");
        oa.shouldContain("bsm1arg");
        oa.shouldContain("hello1");
        oa.shouldContain("hello2");
        oa.shouldContain("hello4");
        oa.shouldContain("hello6");
        oa.shouldNotContain("hello3");
        oa.shouldNotContain("hello5");
        oa.shouldNotContain("bsm2arg");
        oa.shouldNotContain("bsm3arg");
        oa.shouldNotContain("bsm4arg");
        oa.shouldHaveExitValue(1);
    }
}
