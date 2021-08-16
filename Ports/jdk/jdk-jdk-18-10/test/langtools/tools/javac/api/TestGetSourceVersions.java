/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6395981 6458819 7025784 8028543 8028544 8193291 8193292 8193292 8205393 8245585 8245585 8245585
 * @summary JavaCompilerTool and Tool must specify version of JLS and JVMS
 * @author  Peter von der Ah\u00e9
 * @modules java.compiler
 *          jdk.compiler
 * @run main/fail TestGetSourceVersions
 * @run main/fail TestGetSourceVersions RELEASE_3  RELEASE_5  RELEASE_6
 * @run main/fail TestGetSourceVersions RELEASE_0  RELEASE_1  RELEASE_2  RELEASE_3  RELEASE_4
 *                                      RELEASE_5  RELEASE_6
 * @run main TestGetSourceVersions      RELEASE_3  RELEASE_4  RELEASE_5  RELEASE_6  RELEASE_7
 *                                      RELEASE_8  RELEASE_9  RELEASE_10 RELEASE_11 RELEASE_12
 *                                      RELEASE_13 RELEASE_14 RELEASE_15 RELEASE_16 RELEASE_17
 *                                      RELEASE_18
 */

import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.Tool;
import javax.tools.ToolProvider;
import static javax.lang.model.SourceVersion.*;

public class TestGetSourceVersions {
    public static void main(String... args) {
        Tool compiler = ToolProvider.getSystemJavaCompiler();
        Set<SourceVersion> expected = EnumSet.noneOf(SourceVersion.class);
        for (String arg : args)
            expected.add(SourceVersion.valueOf(arg));
        Set<SourceVersion> found = compiler.getSourceVersions();
        Set<SourceVersion> notExpected = EnumSet.copyOf(found);
        for (SourceVersion version : expected) {
            if (!found.contains(version))
                throw new AssertionError("Expected source version not found: " + version);
            else
                notExpected.remove(version);
        }
        if (!notExpected.isEmpty())
            throw new AssertionError("Unexpected source versions: " + notExpected);
    }
}
