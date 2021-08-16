/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.sjavac;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/** A Source object maintains information about a source file.
 * For example which package it belongs to and kind of source it is.
 * The class also knows how to find source files (scanRoot) given include/exclude
 * patterns and a root.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Source implements Comparable<Source> {
    // The package the source belongs to.
   private Package pkg;
    // Name of this source file, relative its source root.
    // For example: java/lang/Object.java
    // Or if the source file is inside a module:
    // jdk.base/java/lang/Object.java
    private String name;
    // What kind of file is this.
    private String suffix;
    // When this source file was last_modified
    private long lastModified;
    // The source File.
    private File file;
    // If the source is generated.
    private boolean isGenerated;
    // If the source is only linked to, not compiled.
    private boolean linkedOnly;

    @Override
    public boolean equals(Object o) {
        return (o instanceof Source source) && name.equals(source.name);
    }

    @Override
    public int compareTo(Source o) {
        return name.compareTo(o.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Source(Module m, String n, File f) {
        name = n;
        int dp = n.lastIndexOf(".");
        if (dp != -1) {
            suffix = n.substring(dp);
        } else {
            suffix = "";
        }
        file = f;
        lastModified = f.lastModified();
        linkedOnly = false;
    }

    public Source(Package p, String n, long lm) {
        pkg = p;
        name = n;
        int dp = n.lastIndexOf(".");
        if (dp != -1) {
            suffix = n.substring(dp);
        } else {
            suffix = "";
        }
        file = null;
        lastModified = lm;
        linkedOnly = false;
        int ls = n.lastIndexOf('/');
    }

    public String name() { return name; }
    public String suffix() { return suffix; }
    public Package pkg() { return pkg; }
    public File   file() { return file; }
    public long lastModified() {
        return lastModified;
    }

    public void setPackage(Package p) {
        pkg = p;
    }

    public void markAsGenerated() {
        isGenerated = true;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public void markAsLinkedOnly() {
        linkedOnly = true;
    }

    public boolean isLinkedOnly() {
        return linkedOnly;
    }

    private void save(StringBuilder b) {
        String CL = linkedOnly?"L":"C";
        String GS = isGenerated?"G":"S";
        b.append(GS+" "+CL+" "+name+" "+file.lastModified()+"\n");
    }
    // Parse a line that looks like this:
    // S C /code/alfa/A.java 1357631228000
    public static Source load(Package lastPackage, String l, boolean isGenerated) {
        int sp = l.indexOf(' ',4);
        if (sp == -1) return null;
        String name = l.substring(4,sp);
        long last_modified = Long.parseLong(l.substring(sp+1));

        boolean isLinkedOnly = false;
        if (l.charAt(2) == 'L') {
            isLinkedOnly = true;
        } else if (l.charAt(2) == 'C') {
            isLinkedOnly = false;
        } else return null;

        Source s = new Source(lastPackage, name, last_modified);
        s.file = new File(name);
        if (isGenerated) s.markAsGenerated();
        if (isLinkedOnly) s.markAsLinkedOnly();
        return s;
    }

    public static void saveSources(Map<String,Source> sources, StringBuilder b) {
        List<String> sorted_sources = new ArrayList<>();
        for (String key : sources.keySet()) {
            sorted_sources.add(key);
        }
        Collections.sort(sorted_sources);
        for (String key : sorted_sources) {
            Source s = sources.get(key);
            s.save(b);
        }
    }

    /**
     * Recurse into the directory root and find all files matching the excl/incl/exclfiles/inclfiles rules.
     * Detects the existence of module-info.java files and presumes that the directory it resides in
     * is the name of the current module.
     */
    public static void scanRoot(File root,
                                Set<String> suffixes,
                                List<String> excludes,
                                List<String> includes,
                                Map<String,Source> foundFiles,
                                Map<String,Module> foundModules,
                                final Module currentModule,
                                boolean permitSourcesWithoutPackage,
                                boolean inGensrc,
                                boolean inLinksrc)
                                        throws IOException, ProblemException {

        if (root == null)
            return;

        FileSystem fs = root.toPath().getFileSystem();

        if (includes.isEmpty()) {
            includes = Collections.singletonList("**");
        }

        List<PathMatcher> includeMatchers = createPathMatchers(fs, includes);
        List<PathMatcher> excludeMatchers = createPathMatchers(fs, excludes);

        Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                Path relToRoot = root.toPath().relativize(file);

                if (includeMatchers.stream().anyMatch(im -> im.matches(relToRoot))
                        && excludeMatchers.stream().noneMatch(em -> em.matches(relToRoot))
                        && suffixes.contains(Util.fileSuffix(file))) {

                    // TODO: Test this.
                    Source existing = foundFiles.get(file);
                    if (existing != null) {
                        throw new IOException("You have already added the file "+file+" from "+existing.file().getPath());
                    }
                    existing = currentModule.lookupSource(file.toString());
                    if (existing != null) {

                            // Oops, the source is already added, could be ok, could be not, let's check.
                            if (inLinksrc) {
                                // So we are collecting sources for linking only.
                                if (existing.isLinkedOnly()) {
                                    // Ouch, this one is also for linking only. Bad.
                                    throw new IOException("You have already added the link only file " + file + " from " + existing.file().getPath());
                                }
                                // Ok, the existing source is to be compiled. Thus this link only is redundant
                                // since all compiled are also linked to. Continue to the next source.
                                // But we need to add the source, so that it will be visible to linking,
                                // if not the multi core compile will fail because a JavaCompiler cannot
                                // find the necessary dependencies for its part of the source.
                                foundFiles.put(file.toString(), existing);
                            } else {
                                // We are looking for sources to compile, if we find an existing to be compiled
                                // source with the same name, it is an internal error, since we must
                                // find the sources to be compiled before we find the sources to be linked to.
                                throw new IOException("Internal error: Double add of file " + file + " from " + existing.file().getPath());
                            }

                    } else {

                        //////////////////////////////////////////////////////////////
                        // Add source
                        Source s = new Source(currentModule, file.toString(), file.toFile());
                        if (inGensrc) {
                            s.markAsGenerated();
                        }
                        if (inLinksrc) {
                            s.markAsLinkedOnly();
                        }
                        String pkg = packageOfJavaFile(root.toPath(), file);
                        pkg = currentModule.name() + ":" + pkg;
                        foundFiles.put(file.toString(), s);
                        currentModule.addSource(pkg, s);
                        //////////////////////////////////////////////////////////////
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static List<PathMatcher> createPathMatchers(FileSystem fs, List<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                matchers.add(fs.getPathMatcher("glob:" + pattern));
            } catch (PatternSyntaxException e) {
                Log.error("Invalid pattern: " + pattern);
                throw e;
            }
        }
        return matchers;
    }

    private static String packageOfJavaFile(Path sourceRoot, Path javaFile) {
        Path javaFileDir = javaFile.getParent();
        Path packageDir = sourceRoot.relativize(javaFileDir);
        List<String> separateDirs = new ArrayList<>();
        for (Path pathElement : packageDir) {
            separateDirs.add(pathElement.getFileName().toString());
        }
        return String.join(".", separateDirs);
    }

    @Override
    public String toString() {
        return String.format("%s[pkg: %s, name: %s, suffix: %s, file: %s, isGenerated: %b, linkedOnly: %b]",
                             getClass().getSimpleName(),
                             pkg,
                             name,
                             suffix,
                             file,
                             isGenerated,
                             linkedOnly);
    }
}
