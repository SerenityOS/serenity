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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import jdk.tools.jlink.internal.Archive;
import jdk.tools.jlink.internal.ImageFileCreator;
import jdk.tools.jlink.internal.ImagePluginStack;
import jdk.tools.jlink.internal.ExecutableImage;
import jdk.tools.jlink.builder.ImageBuilder;
import jdk.tools.jlink.plugin.ResourcePool;


/*
 * @test
 * @summary ImageFileCreator class test
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.builder
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          java.base/jdk.internal.jimage
 * @run main/othervm -verbose:gc -Xmx1g ImageFileCreatorTest
 */
public class ImageFileCreatorTest {

    private static class TestArchive implements Archive {

        private final String name;
        private final List<Entry> entries = new ArrayList<>();

        private TestArchive(String name, List<String> entries) {
            this.name = name;
            for (String p : entries) {
                this.entries.add(new TestEntry(p, p));
            }
        }

        @Override
        public String moduleName() {
            return name;
        }

        @Override
        public Stream<Entry> entries() {
            return entries.stream();
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public void open() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        private class TestEntry extends Entry {

            TestEntry(String path, String name) {
                super(TestArchive.this, path, name, Entry.EntryType.CLASS_OR_RESOURCE);
            }

            @Override
            public long size() {
                return 0;
            }

            @Override
            public InputStream stream() throws IOException {
                return new ByteArrayInputStream(new byte[0]);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        {
            List<String> entries = new ArrayList<>();
            entries.add("classes/class");
            test(entries);
        }

        {
            // Add an entry that is a directory, that is wrong
            List<String> entries = new ArrayList<>();
            entries.add("classes");
            entries.add("classes/class");
            test(entries);
        }

        {
            // Add an entry that is wrongly prefixed by /
            // /bad//classes/class is the resource added
            // /bad/classes/class is the metadata node built.
            List<String> entries = new ArrayList<>();
            entries.add("/classes/class");
            test(entries);
        }

        {
            // Trailing '/' is wrong
            List<String> entries = new ArrayList<>();
            entries.add("classes/class/");
            test(entries);
        }

        {
            // Too much '/' characters
            List<String> entries = new ArrayList<>();
            entries.add("classes//class/");
            test(entries);
        }

        {
            // Too much '/' characters
            List<String> entries = new ArrayList<>();
            entries.add("classes/////class/");
            test(entries);
        }

        {
            // Single '/' character
            List<String> entries = new ArrayList<>();
            entries.add("/");
            test(entries);
        }

        {
            // 2 '/' characters
            List<String> entries = new ArrayList<>();
            entries.add("//");
            test(entries);
        }

        {
            // 3 '/' characters
            List<String> entries = new ArrayList<>();
            entries.add("///");
            test(entries);
        }

        {
            // no character
            List<String> entries = new ArrayList<>();
            entries.add("");
            test(entries);
        }

        {
            // all together
            List<String> entries = new ArrayList<>();
            entries.add("");
            entries.add("///");
            entries.add("//");
            entries.add("/");
            entries.add("classes/////class/");
            entries.add("classes//class/");
            entries.add("classes/class/");
            entries.add("/classes/class");
            entries.add("classes");
            entries.add("classes/class");
            test(entries);
        }

    }

    private static void test(List<String> entries) throws Exception {
        TestArchive arch = new TestArchive("bad", entries);
        Set<Archive> archives = new HashSet<>();
        archives.add(arch);
        ImageBuilder noopBuilder = new ImageBuilder() {

            @Override
            public DataOutputStream getJImageOutputStream() {
                return new DataOutputStream(new ByteArrayOutputStream());
            }

            @Override
            public ExecutableImage getExecutableImage() {
                return null;
            }

            @Override
            public void storeFiles(ResourcePool content) {
            }
        };

        ImagePluginStack stack = new ImagePluginStack(noopBuilder, Collections.emptyList(),
                null, false);

        ImageFileCreator.create(archives, ByteOrder.nativeOrder(), stack);
    }
}
