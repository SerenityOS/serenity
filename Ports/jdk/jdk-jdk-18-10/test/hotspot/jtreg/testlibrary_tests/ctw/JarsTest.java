/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8012447
 * @library /test/lib /testlibrary/ctw/src
 * @modules java.base/jdk.internal.access
 *          java.base/jdk.internal.jimage
 *          java.base/jdk.internal.misc
 *          java.base/jdk.internal.reflect
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build sun.hotspot.WhiteBox Foo Bar
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox Foo Bar
 * @run driver JarsTest prepare
 * @run driver JarsTest compile foo.jar bar.jar
 * @run driver JarsTest check
 * @summary testing of CompileTheWorld :: jars
 * @author igor.ignatyev@oracle.com
 */

import jdk.test.lib.process.OutputAnalyzer;

public class JarsTest extends CtwTest {
    private static final String[] SHOULD_CONTAIN
            = {"# jar: foo.jar", "# jar: bar.jar",
                    "Done (4 classes, 12 methods, "};

    private JarsTest() {
        super(SHOULD_CONTAIN);
    }

    public static void main(String[] args) throws Exception {
        new JarsTest().run(args);
    }

    protected void prepare() throws Exception {
        ProcessBuilder pb = createJarProcessBuilder("cf", "foo.jar",
                "Foo.class", "Bar.class");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        dump(output, "ctw-foo.jar");
        output.shouldHaveExitValue(0);

        pb = createJarProcessBuilder("cf", "bar.jar", "Foo.class", "Bar.class");
        output = new OutputAnalyzer(pb.start());
        dump(output, "ctw-bar.jar");
        output.shouldHaveExitValue(0);
    }

}
