/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarBuilder {
    final private String name;
    final private Attributes attributes = new Attributes();
    final private List<Entry> entries = new ArrayList<>();

    public JarBuilder(String name) {
        this.name = name;
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Created-By", "1.9.0-internal (Oracle Corporation)");
    }

    public JarBuilder addAttribute(String name, String value) {
        attributes.putValue(name, value);
        return this;
    }

    public JarBuilder addEntry(String name, byte[] bytes) {
        entries.add(new Entry(name, bytes));
        return this;
    }

    public void build() throws IOException {
        try (OutputStream os = Files.newOutputStream(Paths.get(name));
             JarOutputStream jos = new JarOutputStream(os)) {
            JarEntry me = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(me);
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putAll(attributes);
            manifest.write(jos);
            jos.closeEntry();
            entries.forEach(e -> {
                JarEntry je = new JarEntry(e.name);
                try {
                    jos.putNextEntry(je);
                    jos.write(e.bytes);
                    jos.closeEntry();
                } catch (IOException iox) {
                    throw new RuntimeException(iox);
                }
            });
        } catch (RuntimeException x) {
            Throwable t = x.getCause();
            if (t instanceof IOException) {
                IOException iox = (IOException)t;
                throw iox;
            }
            throw x;
        }
    }

    private static class Entry {
        String name;
        byte[] bytes;

        Entry(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }
    }

    public static void main(String[] args) throws IOException {
        JarBuilder jb = new JarBuilder("version.jar");
        jb.addAttribute("Multi-Release", "true");
        String s = "something to say";
        byte[] bytes = s.getBytes();
        jb.addEntry("version/Version.class", bytes);
        jb.addEntry("README", bytes);
        jb.addEntry("version/Version.java", bytes);
        jb.build();
    }
}
