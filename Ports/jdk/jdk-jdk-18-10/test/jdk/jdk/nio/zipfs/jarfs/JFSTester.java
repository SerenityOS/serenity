/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164389 8222440
 * @summary walk entries in a multi-release jar file via jdk.zipfs
 * @modules jdk.jartool
 *          jdk.zipfs
 * @library /test/lib/
 * @build jdk.test.lib.util.JarBuilder
 *        jdk.test.lib.compiler.Compiler
 * @run testng JFSTester
 */

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.test.lib.util.JarBuilder;

public class JFSTester {
    private URI jarURI;

    final private String root_dir1_leaf1_txt = "This is leaf 1." + System.lineSeparator();
    final private String root_dir1_leaf2_txt = "This is leaf 2." + System.lineSeparator();
    final private String root_dir2_leaf3_txt = "This is leaf 3." + System.lineSeparator();
    final private String root_dir2_leaf4_txt = "This is leaf 4." + System.lineSeparator();
    final private String v9_root_dir2_leaf3_txt = "This is version 9 leaf 3." + System.lineSeparator();
    final private String v9_root_dir2_leaf4_txt = "This is version 9 leaf 4." + System.lineSeparator();
    final private String v9_root_dir3_leaf5_txt = "This is version 9 leaf 5." + System.lineSeparator();
    final private String v9_root_dir3_leaf6_txt = "This is version 9 leaf 6." + System.lineSeparator();
    final private String v10_root_dir3_leaf5_txt = "This is version 10 leaf 5." + System.lineSeparator();
    final private String v10_root_dir3_leaf6_txt = "This is version 10 leaf 6." + System.lineSeparator();

    @BeforeClass
    public void initialize() throws Exception {
        Path jarfile = Paths.get("test.jar");
        JarBuilder jb = new JarBuilder(jarfile.toString());
        jb.addAttribute("Multi-Release", "true");
        jb.addEntry("root/dir1/leaf1.txt", root_dir1_leaf1_txt.getBytes());
        jb.addEntry("root/dir1/leaf2.txt", root_dir1_leaf2_txt.getBytes());
        jb.addEntry("root/dir2/leaf3.txt", root_dir2_leaf3_txt.getBytes());
        jb.addEntry("root/dir2/leaf4.txt", root_dir2_leaf4_txt.getBytes());
        jb.addEntry("META-INF/versions/9/root/dir2/leaf3.txt", v9_root_dir2_leaf3_txt.getBytes());
        jb.addEntry("META-INF/versions/9/root/dir2/leaf4.txt", v9_root_dir2_leaf4_txt.getBytes());
        jb.addEntry("META-INF/versions/9/root/dir3/leaf5.txt", v9_root_dir3_leaf5_txt.getBytes());
        jb.addEntry("META-INF/versions/9/root/dir3/leaf6.txt", v9_root_dir3_leaf6_txt.getBytes());
        jb.addEntry("META-INF/versions/10/root/dir3/leaf5.txt", v10_root_dir3_leaf5_txt.getBytes());
        jb.addEntry("META-INF/versions/10/root/dir3/leaf6.txt", v10_root_dir3_leaf6_txt.getBytes());
        jb.build();
        System.out.println("Created " + jarfile + ": " + Files.exists(jarfile));
        jarURI = new URI("jar", jarfile.toUri().toString(), null);
    }

    @Test
    public void testWalk() throws IOException {
        // treat multi-release jar as unversioned
        Map<String, String> env = new HashMap<>();
        Set<String> contents = doTest(env);
        Set<String> expectedContents = Set.of(
            root_dir1_leaf1_txt,
            root_dir1_leaf2_txt,
            root_dir2_leaf3_txt,
            root_dir2_leaf4_txt
        );
        Assert.assertEquals(contents, expectedContents);

        // open file as multi-release for version 9
        env.put("multi-release", "9");
        contents = doTest(env);
        expectedContents = Set.of(
            root_dir1_leaf1_txt,
            root_dir1_leaf2_txt,
            v9_root_dir2_leaf3_txt,
            v9_root_dir2_leaf4_txt,
            v9_root_dir3_leaf5_txt,
            v9_root_dir3_leaf6_txt
        );
        Assert.assertEquals(contents, expectedContents);

        // open file as multi-release for version 10
        env.put("multi-release", "10");
        contents = doTest(env);
        expectedContents = Set.of(
            root_dir1_leaf1_txt,
            root_dir1_leaf2_txt,
            v9_root_dir2_leaf3_txt,
            v9_root_dir2_leaf4_txt,
            v10_root_dir3_leaf5_txt,
            v10_root_dir3_leaf6_txt
        );
        Assert.assertEquals(contents, expectedContents);
    }

    private Set<String> doTest(Map<String,String> env) throws IOException {
        Set<String> contents;
        try (FileSystem fs = FileSystems.newFileSystem(jarURI, env)) {
            Path root = fs.getPath("root");
            contents = Files.walk(root)
                .filter(p -> !Files.isDirectory(p))
                .map(this::pathToContents)
                .sorted()
                .collect(Collectors.toSet());
        }
        return contents;
    }

    private String pathToContents(Path path) {
        try {
            return new String(Files.readAllBytes(path));
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }
}
