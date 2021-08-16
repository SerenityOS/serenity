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
package jdk.test.lib.compiler;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Compiler {
    final private Map<String,String> input;
    private List<String> options;

    public Compiler(Map<String,String> input) {
        this.input = input;
    }

    public Compiler setRelease(int release) {
        // Setting the -release option does not work for some reason
        // so do it the old fashioned way
        // options = Arrays.asList("-release", String.valueOf(release));
        String target = String.valueOf(release);
        options = Arrays.asList("-source", target, "-target", target, "-classpath", "");
        return this;
    }

    public Map<String,byte[]> compile() {
        List<SourceFileObject> cunits = createCompilationUnits();
        Map<String,ClassFileObject> cfos = createClassFileObjects();
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        JavaFileManager jfm = new CustomFileManager(jc.getStandardFileManager(null, null, null), cfos);
        if(!jc.getTask(null, jfm, null, options, null, cunits).call()) {
            throw new RuntimeException("Compilation failed");
        }
        return createOutput(cfos);
    }

    private List<SourceFileObject> createCompilationUnits() {
        return input.entrySet().stream()
                .map(e -> new SourceFileObject(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private Map<String,ClassFileObject> createClassFileObjects() {
        return input.keySet().stream()
                .collect(Collectors.toMap(k -> k, k -> new ClassFileObject(k)));
    }

    private Map<String,byte[]> createOutput(Map<String,ClassFileObject> cfos) {
        return cfos.keySet().stream().collect(Collectors.toMap(k -> k, k -> cfos.get(k).getBytes()));
    }

    private static class SourceFileObject extends SimpleJavaFileObject {
        private final String source;

        SourceFileObject(String name, String source) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static class ClassFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ClassFileObject(String className) {
            super(URI.create(className), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return baos;
        }

        public byte[] getBytes() {
            return baos.toByteArray();
        }
    }

    private static class CustomFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String,ClassFileObject> cfos;

        CustomFileManager(JavaFileManager jfm, Map<String,ClassFileObject> cfos) {
            super(jfm);
            this.cfos = cfos;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location loc, String name,
                                                   JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            ClassFileObject cfo = cfos.get(name);
            return cfo;
        }
    }
}
