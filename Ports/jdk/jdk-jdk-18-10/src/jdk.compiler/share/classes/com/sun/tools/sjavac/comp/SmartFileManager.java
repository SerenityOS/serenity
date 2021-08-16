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

package com.sun.tools.sjavac.comp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Intercepts reads and writes to the file system to gather
 * information about what artifacts are generated.
 *
 * Traps writes to certain files, if the content written is identical
 * to the existing file.
 *
 * Can also blind out the file manager from seeing certain files in the file system.
 * Necessary to prevent javac from seeing some sources where the source path points.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@com.sun.tools.javac.api.ClientCodeWrapper.Trusted
public class SmartFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // Set of sources that can be seen by javac.
    Set<URI> visibleSources = new HashSet<>();
    // Map from modulename:packagename to artifacts.
    Map<String,Set<URI>> packageArtifacts = new HashMap<>();

    public SmartFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    public void setVisibleSources(Set<URI> s) {
        visibleSources = s;
    }

    public void cleanArtifacts() {
        packageArtifacts = new HashMap<>();
    }

    /**
     * Set whether or not to use ct.sym as an alternate to rt.jar.
     */
    public void setSymbolFileEnabled(boolean b) {
        if (!(fileManager instanceof JavacFileManager javacFileManager))
            throw new IllegalStateException();
        javacFileManager.setSymbolFileEnabled(b);
    }

    @DefinedBy(Api.COMPILER)
    public String inferBinaryName(Location location, JavaFileObject file) {
        return super.inferBinaryName(location, locUnwrap(file));
    }


    public Map<String,Set<URI>> getPackageArtifacts() {
        return packageArtifacts;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<JavaFileObject> list(Location location,
                                         String packageName,
                                         Set<Kind> kinds,
                                         boolean recurse) throws IOException {
        // TODO: Do this lazily by returning an iterable with a filtering Iterator
        // Acquire the list of files.
        Iterable<JavaFileObject> files = super.list(location, packageName, kinds, recurse);
        if (visibleSources.isEmpty()) {
            return locWrapMany(files, location);
        }
        // Now filter!
        ListBuffer<JavaFileObject> filteredFiles = new ListBuffer<>();
        for (JavaFileObject f : files) {
            URI uri = f.toUri();
            String t = uri.toString();
            if (t.startsWith("jar:")
                || t.endsWith(".class")
                || visibleSources.contains(uri)) {
                filteredFiles.add(f);
            }
        }

        return locWrapMany(filteredFiles, location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavaFileObject getJavaFileForInput(Location location,
                                              String className,
                                              Kind kind) throws IOException {
        JavaFileObject file = super.getJavaFileForInput(location, className, kind);
        file = locWrap(file, location);
        if (file == null || visibleSources.isEmpty()) {
            return file;
        }

        if (visibleSources.contains(file.toUri()) || isModuleInfo(file)) {
            return file;
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavaFileObject getJavaFileForOutput(Location location,
                                               String className,
                                               Kind kind,
                                               FileObject sibling) throws IOException {
        JavaFileObject file = super.getJavaFileForOutput(location, className, kind, sibling);
        file = locWrap(file, location);
        if (file == null) return file;
        int dp = className.lastIndexOf('.');
        String pkg_name = "";
        if (dp != -1) {
            pkg_name = className.substring(0, dp);
        }
        // When modules are in use, then the mod_name might be something like "jdk_base"
        String mod_name = "";
        addArtifact(mod_name+":"+pkg_name, file.toUri());
        return file;
    }

    @Override @DefinedBy(Api.COMPILER)
    public FileObject getFileForInput(Location location,
                                      String packageName,
                                      String relativeName) throws IOException {
        FileObject file =  super.getFileForInput(location, packageName, relativeName);
        file = locWrap(file, location);
        if (file == null || visibleSources.isEmpty()) {
            return file;
        }

        if (visibleSources.contains(file.toUri()) || isModuleInfo(file)) {
            return file;
        }
        return null;
    }

    private boolean isModuleInfo(FileObject fo) {
        return (fo instanceof JavaFileObject javaFileObject)
                && (javaFileObject.isNameCompatible("module-info", Kind.SOURCE)
                    || javaFileObject.isNameCompatible("module-info", Kind.CLASS));
    }

    @Override @DefinedBy(Api.COMPILER)
    public FileObject getFileForOutput(Location location,
                                       String packageName,
                                       String relativeName,
                                       FileObject sibling) throws IOException {
        FileObject superFile = super.getFileForOutput(location, packageName, relativeName, sibling);
        FileObject file = locWrap(superFile, location);
        if (file == null) return file;

        if (location.equals(StandardLocation.NATIVE_HEADER_OUTPUT) && superFile instanceof JavaFileObject) {
           file = new SmartFileObject((JavaFileObject) file);
           packageName = ":" + packageNameFromFileName(relativeName);
        }
        if (packageName.equals("")) {
            packageName = ":";
        }
        addArtifact(packageName, file.toUri());
        return file;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        return super.getLocationForModule(location, locUnwrap(fo));
    }

    private static String packageNameFromFileName(String fn) {
        StringBuilder sb = new StringBuilder();
        int p = fn.indexOf('_'), pp = 0;
        while (p != -1) {
            if (sb.length() > 0) sb.append('.');
            sb.append(fn.substring(pp,p));
            if (p == fn.length()-1) break;
            pp = p+1;
            p = fn.indexOf('_',pp);
        }
        return sb.toString();
    }

    void addArtifact(String pkgName, URI art) {
        Set<URI> s = packageArtifacts.get(pkgName);
        if (s == null) {
            s = new HashSet<>();
            packageArtifacts.put(pkgName, s);
        }
        s.add(art);
    }

    public static JavaFileObject locWrap(JavaFileObject jfo, Location loc) {

        // From sjavac's perspective platform classes are not interesting and
        // there is no need to track the location for these file objects.
        // Also, there exists some jfo instanceof checks which breaks if
        // the jfos for platform classes are wrapped.
        if (loc == StandardLocation.PLATFORM_CLASS_PATH)
            return jfo;

        return jfo == null ? null : new JavaFileObjectWithLocation<>(jfo, loc);
    }

    private static FileObject locWrap(FileObject fo, Location loc) {
        if (fo instanceof JavaFileObject javaFileObject)
            return locWrap(javaFileObject, loc);
        return fo == null ? null : new FileObjectWithLocation<>(fo, loc);
    }

    @DefinedBy(Api.COMPILER)
    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return super.isSameFile(locUnwrap(a), locUnwrap(b));
    }

    private static ListBuffer<JavaFileObject> locWrapMany(Iterable<JavaFileObject> jfos,
                                                          Location loc) {
        ListBuffer<JavaFileObject> locWrapped = new ListBuffer<>();
        for (JavaFileObject f : jfos)
            locWrapped.add(locWrap(f, loc));
        return locWrapped;
    }

    private static FileObject locUnwrap(FileObject fo) {
        if (fo instanceof FileObjectWithLocation<?> fileObjectWithLocation)
            return fileObjectWithLocation.getDelegate();
        if (fo instanceof JavaFileObjectWithLocation<?> javaFileObjectWithLocation)
            return javaFileObjectWithLocation.getDelegate();
        return fo;
    }

    private static JavaFileObject locUnwrap(JavaFileObject fo) {
        if (fo instanceof JavaFileObjectWithLocation<?> javaFileObjectWithLocation)
            return javaFileObjectWithLocation.getDelegate();
        return fo;
    }
}
