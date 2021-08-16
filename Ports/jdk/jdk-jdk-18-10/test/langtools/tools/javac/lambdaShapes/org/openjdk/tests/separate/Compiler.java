/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.separate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.URI;
import javax.tools.*;

import com.sun.source.util.JavacTask;

import static org.openjdk.tests.separate.SourceModel.Type;
import static org.openjdk.tests.separate.SourceModel.Class;
import static org.openjdk.tests.separate.SourceModel.Extends;
import static org.openjdk.tests.separate.SourceModel.SourceProcessor;

public class Compiler {

    public enum Flags {
        VERBOSE, // Prints out files as they are compiled
        USECACHE // Keeps results around for reuse.  Only use this is
                 // you're sure that each compilation name maps to the
                 // same source code
    }

    private static final AtomicInteger counter = new AtomicInteger();
    private static final String targetDir = "gen-separate";
    private static final File root = new File(targetDir);
    private static ConcurrentHashMap<String,File> cache =
            new ConcurrentHashMap<>();

    Set<Flags> flags;

    private JavaCompiler systemJavaCompiler;
    private StandardJavaFileManager fm;
    private List<File> tempDirs;
    private List<ClassFilePreprocessor> postprocessors;

    private static class SourceFile extends SimpleJavaFileObject {
        private final String content;

        public SourceFile(String name, String content) {
            super(URI.create("myfo:/" + name + ".java"), Kind.SOURCE);
            this.content = content;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return toString();
        }

        public String toString() { return this.content; }
    }

    public Compiler(Flags ... flags) {
        setFlags(flags);
        this.tempDirs = new ArrayList<>();
        this.postprocessors = new ArrayList<>();
        this.systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
        this.fm = systemJavaCompiler.getStandardFileManager(null, null, null);
    }

    public void setFlags(Flags ... flags) {
        this.flags = new HashSet<>(Arrays.asList(flags));
    }

    public void addPostprocessor(ClassFilePreprocessor cfp) {
        this.postprocessors.add(cfp);
    }

    /**
     * Compile hierarchies starting with each of the 'types' and return
     * a ClassLoader that can be used to load the compiled classes.
     */
    public ClassLoader compile(Type ... types) {
        ClassFilePreprocessor[] cfps = this.postprocessors.toArray(
            new ClassFilePreprocessor[0]);

        DirectedClassLoader dcl = new DirectedClassLoader(cfps);

        for (Type t : types) {
            for (Map.Entry<String,File> each : compileHierarchy(t).entrySet()) {
                dcl.setLocationFor(each.getKey(), each.getValue());
            }
        }
        return dcl;
    }

    /**
     * Compiles and loads a hierarchy, starting at 'type'
     */
    public java.lang.Class<?> compileAndLoad(Type type)
            throws ClassNotFoundException {

        ClassLoader loader = compile(type);
        return java.lang.Class.forName(type.getName(), false, loader);
    }

    /**
     * Compiles a hierarchy, starting at 'type' and return a mapping of the
     * name to the location where the classfile for that type resides.
     */
    private Map<String,File> compileHierarchy(Type type) {
        HashMap<String,File> outputDirs = new HashMap<>();

        File outDir = compileOne(type);
        outputDirs.put(type.getName(), outDir);

        Class superClass = type.getSuperclass();
        if (superClass != null)
            outputDirs.putAll(compileHierarchy(superClass));
        for (Extends ext : type.getSupertypes())
            outputDirs.putAll(compileHierarchy(ext.getType()));

        return outputDirs;
    }

    private File compileOne(Type type) {
        if (this.flags.contains(Flags.USECACHE)) {
            File dir = cache.get(type.getName());
            if (dir != null) {
                return dir;
            }
        }
        List<JavaFileObject> files = new ArrayList<>();
        SourceProcessor accum =
            (name, src) -> { files.add(new SourceFile(name, src)); };

        Collection<Type> deps = type.typeDependencies(type.isFullCompilation());
        for (Type dep : deps) {
            if (type.isFullCompilation())
                dep.generate(accum);
            else
                dep.generateAsDependency(accum, type.methodDependencies());
        }

        type.generate(accum);

        JavacTask ct = (JavacTask)this.systemJavaCompiler.getTask(
            null, this.fm, null, null, null, files);
        File destDir = null;
        do {
            int value = counter.incrementAndGet();
            destDir = new File(root, Integer.toString(value));
        } while (destDir.exists());

        if (this.flags.contains(Flags.VERBOSE)) {
            System.out.println("Compilation unit for " + type.getName() +
                " : compiled into " + destDir);
            for (JavaFileObject jfo : files) {
                System.out.println(jfo.toString());
            }
        }

        try {
            destDir.mkdirs();
            this.fm.setLocation(
                StandardLocation.CLASS_OUTPUT, Arrays.asList(destDir));
        } catch (IOException e) {
            throw new RuntimeException(
                "IOException encountered during compilation", e);
        }
        Boolean result = ct.call();
        if (result == Boolean.FALSE) {
            throw new RuntimeException(
                "Compilation failure in " + type.getName() + " unit");
        }
        if (this.flags.contains(Flags.USECACHE)) {
            File existing = cache.putIfAbsent(type.getName(), destDir);
            if (existing != null) {
                deleteDir(destDir);
                return existing;
            }
        } else {
        this.tempDirs.add(destDir);
        }
        return destDir;
    }

    private static void deleteDir(File dir) {
        for (File f : dir.listFiles()) {
            f.delete();
        };
        dir.delete();
    }

    public void cleanup() {
        if (!this.flags.contains(Flags.USECACHE)) {
            for (File d : tempDirs) {
                deleteDir(d);
            };
            tempDirs = new ArrayList<>();
        }
    }

    // Removes all of the elements in the cache and deletes the associated
    // output directories.  This may not actually empty the cache if there
    // are concurrent users of it.
    public static void purgeCache() {
        for (Map.Entry<String,File> entry : cache.entrySet()) {
            cache.remove(entry.getKey());
            deleteDir(entry.getValue());
        }
    }
}
