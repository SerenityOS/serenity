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
 *          java.management
 * @build sun.hotspot.WhiteBox Foo Bar
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox Foo Bar
 * @run driver ClassesDirTest prepare
 * @run driver ClassesDirTest compile classes
 * @run driver ClassesDirTest check
 * @summary testing of CompileTheWorld :: classes in directory
 * @author igor.ignatyev@oracle.com
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ClassesDirTest extends CtwTest {
    private static final String[] SHOULD_CONTAIN
            = {"# dir: classes", "Done (2 classes, 6 methods, "};

    private ClassesDirTest() {
        super(SHOULD_CONTAIN);
    }

    public static void main(String[] args) throws Exception {
        new ClassesDirTest().run(args);
    }

    protected void prepare() throws Exception {
        String path = "classes";
        Files.createDirectory(Paths.get(path));
        Files.move(Paths.get("Foo.class"), Paths.get(path, "Foo.class"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.move(Paths.get("Bar.class"), Paths.get(path, "Bar.class"),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
