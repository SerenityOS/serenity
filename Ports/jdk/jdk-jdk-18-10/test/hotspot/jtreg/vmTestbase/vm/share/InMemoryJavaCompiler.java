/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;


public class InMemoryJavaCompiler {

    public static Map<String, byte[]> compile(Map<String, ? extends CharSequence> inputMap) {
        Collection<JavaFileObject> sourceFiles = new LinkedList<JavaFileObject>();
        for (Entry<String, ? extends CharSequence> entry : inputMap.entrySet()) {
            sourceFiles.add(new SourceFile(entry.getKey(), entry.getValue()));
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        FileManager fileManager = new FileManager(compiler.getStandardFileManager(null, null, null));

        Writer writer = new StringWriter();
        Boolean exitCode = compiler.getTask(writer, fileManager, null, null, null, sourceFiles).call();
        if (!exitCode) {
            System.out.println("*********** javac output begin ***********");
            System.out.println(writer.toString());
            System.out.println("*********** javac output end ***********");
            if (writer.toString().contains("java.lang.OutOfMemoryError")) {
                System.out.println("Got OOME while performing in memory compilation. It happens on weak hosts and there is nothing we can do. ");
                throw new OutOfMemoryError("Got OOME while performing in memory compilation.");
            }
            throw new RuntimeException("Test bug: in memory compilation failed.");
        }
        return fileManager.getByteCode();
    }

    // Wraper for class file
    static class ClassFile extends SimpleJavaFileObject {

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        protected ClassFile(String name) {
            super(URI.create("memo:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public ByteArrayOutputStream openOutputStream() { return this.baos; }

        byte[] toByteArray() { return baos.toByteArray(); }
    }

    // File manager which spawns ClassFile instances by demand
    static class FileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private Map<String, ClassFile> classesMap = new HashMap<String, ClassFile>();

        protected FileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public ClassFile getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject source) {
            ClassFile classFile = new ClassFile(name);
            classesMap.put(name, classFile);
            return classFile;
        }

        public Map<String, byte[]> getByteCode() {
            Map<String, byte[]> result = new HashMap<String, byte[]>();
            for (Entry<String, ClassFile> entry : classesMap.entrySet()) {
                result.put(entry.getKey(), entry.getValue().toByteArray());
            }
            return result;
        }
    }

    // Wrapper for source file
    static class SourceFile extends SimpleJavaFileObject {

        private CharSequence sourceCode;

        public SourceFile(String name, CharSequence sourceCode) {
            super(URI.create("memo:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignore) {
            return this.sourceCode;
        }
    }

}
