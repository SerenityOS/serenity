/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.platform;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Processor;
import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.util.Plugin;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.StringUtils;

/** PlatformProvider for JDK N.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JDKPlatformProvider implements PlatformProvider {

    @Override
    public Iterable<String> getSupportedPlatformNames() {
        return SUPPORTED_JAVA_PLATFORM_VERSIONS;
    }

    @Override
    public PlatformDescription getPlatform(String platformName, String options) {
        return new PlatformDescriptionImpl(platformName);
    }

    private static final String[] symbolFileLocation = { "lib", "ct.sym" };

    private static final Set<String> SUPPORTED_JAVA_PLATFORM_VERSIONS;
    public static final Comparator<String> NUMERICAL_COMPARATOR = (s1, s2) -> {
        int i1;
        try {
            i1 = Integer.parseInt(s1);
        } catch (NumberFormatException ex) {
            i1 = Integer.MAX_VALUE;
        }
        int i2;
        try {
            i2 = Integer.parseInt(s2);
        } catch (NumberFormatException ex) {
            i2 = Integer.MAX_VALUE;
        }
        return i1 != i2 ? i1 - i2 : s1.compareTo(s2);
    };

    static {
        SUPPORTED_JAVA_PLATFORM_VERSIONS = new TreeSet<>(NUMERICAL_COMPARATOR);
        Path ctSymFile = findCtSym();
        if (Files.exists(ctSymFile)) {
            try (FileSystem fs = FileSystems.newFileSystem(ctSymFile, (ClassLoader)null);
                 DirectoryStream<Path> dir =
                         Files.newDirectoryStream(fs.getRootDirectories().iterator().next())) {
                for (Path section : dir) {
                    if (section.getFileName().toString().contains("-"))
                        continue;
                    for (char ver : section.getFileName().toString().toCharArray()) {
                        String verString = Character.toString(ver);
                        Target t = Target.lookup("" + Integer.parseInt(verString, Character.MAX_RADIX));

                        if (t != null) {
                            SUPPORTED_JAVA_PLATFORM_VERSIONS.add(targetNumericVersion(t));
                        }
                    }
                }
            } catch (IOException | ProviderNotFoundException ex) {
            }
        }
    }

    private static String targetNumericVersion(Target target) {
        return Integer.toString(target.ordinal() - Target.JDK1_1.ordinal() + 1);
    }

    static class PlatformDescriptionImpl implements PlatformDescription {

        private final Map<Path, FileSystem> ctSym2FileSystem = new HashMap<>();
        private final String sourceVersion;
        private final String ctSymVersion;

        PlatformDescriptionImpl(String sourceVersion) {
            this.sourceVersion = sourceVersion;
            this.ctSymVersion =
                    StringUtils.toUpperCase(Integer.toString(Integer.parseInt(sourceVersion), Character.MAX_RADIX));
        }

        @Override
        public JavaFileManager getFileManager() {
            Context context = new Context();
            PrintWriter pw = new PrintWriter(System.err, true);
            context.put(Log.errKey, pw);
            CacheFSInfo.preRegister(context);
            JavacFileManager fm = new JavacFileManager(context, true, null) {
                @Override
                public boolean hasLocation(Location location) {
                    return super.hasExplicitLocation(location);
                }

                @Override
                public JavaFileObject getJavaFileForInput(Location location, String className,
                                                          Kind kind) throws IOException {
                    if (kind == Kind.CLASS) {
                        String fileName = className.replace('.', '/');
                        JavaFileObject result =
                                (JavaFileObject) getFileForInput(location,
                                                                 "",
                                                                 fileName + ".sig");

                        if (result == null) {
                            //in jrt://, the classfile may have the .class extension:
                            result = (JavaFileObject) getFileForInput(location,
                                                                      "",
                                                                      fileName + ".class");
                        }

                        if (result != null) {
                            return new SigJavaFileObject(result);
                        } else {
                            return null;
                        }
                    }

                    return super.getJavaFileForInput(location, className, kind);
                }

                @Override
                public Iterable<JavaFileObject> list(Location location,
                                                     String packageName,
                                                     Set<Kind> kinds,
                                                     boolean recurse) throws IOException {
                    Set<Kind> enhancedKinds = EnumSet.copyOf(kinds);

                    enhancedKinds.add(Kind.OTHER);

                    Iterable<JavaFileObject> listed = super.list(location, packageName,
                                                                 enhancedKinds, recurse);

                    return () -> new Iterator<JavaFileObject>() {
                        private final Iterator<JavaFileObject> original = listed.iterator();
                        private JavaFileObject next;
                        @Override
                        public boolean hasNext() {
                            if (next == null) {
                                while (original.hasNext()) {
                                    JavaFileObject fo = original.next();

                                    if (fo.getKind() == Kind.OTHER &&
                                        fo.getName().endsWith(".sig")) {
                                        next = new SigJavaFileObject(fo);
                                        break;
                                    }

                                    if (kinds.contains(fo.getKind())) {
                                        next = fo;
                                        break;
                                    }
                                }
                            }
                            return next != null;
                        }

                        @Override
                        public JavaFileObject next() {
                            if (!hasNext())
                                throw new NoSuchElementException();
                            JavaFileObject result = next;
                            next = null;
                            return result;
                        }

                    };
                }

                @Override
                public String inferBinaryName(Location location, JavaFileObject file) {
                    if (file instanceof SigJavaFileObject sigJavaFileObject) {
                        file = sigJavaFileObject.getDelegate();
                    }
                    return super.inferBinaryName(location, file);
                }

            };

            fm.handleOption(Option.MULTIRELEASE, sourceVersion);

            Path file = findCtSym();
            // file == ${jdk.home}/lib/ct.sym
            if (Files.exists(file)) {
                try {
                    FileSystem fs = ctSym2FileSystem.get(file);
                    if (fs == null) {
                        ctSym2FileSystem.put(file, fs = FileSystems.newFileSystem(file, (ClassLoader)null));
                    }

                    Path root = fs.getRootDirectories().iterator().next();
                    boolean hasModules =
                            Feature.MODULES.allowedInSource(Source.lookup(sourceVersion));
                    Path systemModules = root.resolve(ctSymVersion).resolve("system-modules");
                    Charset utf8 = Charset.forName("UTF-8");

                    if (!hasModules) {
                        List<Path> paths = new ArrayList<>();

                        try (DirectoryStream<Path> dir = Files.newDirectoryStream(root)) {
                            for (Path section : dir) {
                                if (section.getFileName().toString().contains(ctSymVersion) &&
                                    !section.getFileName().toString().contains("-")) {
                                    try (DirectoryStream<Path> modules = Files.newDirectoryStream(section)) {
                                        for (Path module : modules) {
                                            paths.add(module);
                                        }
                                    }
                                }
                            }
                        }

                        fm.setLocationFromPaths(StandardLocation.PLATFORM_CLASS_PATH, paths);
                    } else if (Files.isRegularFile(systemModules)) {
                        fm.handleOption("--system", Arrays.asList("none").iterator());

                        Path jrtModules =
                                FileSystems.getFileSystem(URI.create("jrt:/"))
                                           .getPath("modules");
                        try (Stream<String> lines =
                                Files.lines(systemModules, utf8)) {
                            lines.map(line -> jrtModules.resolve(line))
                                 .filter(mod -> Files.exists(mod))
                                 .forEach(mod -> setModule(fm, mod));
                        }
                    } else {
                        Map<String, List<Path>> module2Paths = new HashMap<>();

                        try (DirectoryStream<Path> dir = Files.newDirectoryStream(root)) {
                            for (Path section : dir) {
                                if (section.getFileName().toString().contains(ctSymVersion) &&
                                    !section.getFileName().toString().contains("-")) {
                                    try (DirectoryStream<Path> modules = Files.newDirectoryStream(section)) {
                                        for (Path module : modules) {
                                            module2Paths.computeIfAbsent(module.getFileName().toString(), dummy -> new ArrayList<>()).add(module);
                                        }
                                    }
                                }
                            }
                        }

                        fm.handleOption("--system", Arrays.asList("none").iterator());

                        for (Entry<String, List<Path>> e : module2Paths.entrySet()) {
                            fm.setLocationForModule(StandardLocation.SYSTEM_MODULES,
                                                    e.getKey(),
                                                    e.getValue());
                        }
                    }

                    return fm;
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                throw new IllegalStateException("Cannot find ct.sym!");
            }
        }

        private static void setModule(StandardJavaFileManager fm, Path mod) {
            try {
                fm.setLocationForModule(StandardLocation.SYSTEM_MODULES,
                                        mod.getFileName().toString(),
                                        Collections.singleton(mod));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static class SigJavaFileObject extends ForwardingJavaFileObject<JavaFileObject> {

            public SigJavaFileObject(JavaFileObject fileObject) {
                super(fileObject);
            }

            @Override
            public Kind getKind() {
                return Kind.CLASS;
            }

            @Override
            public boolean isNameCompatible(String simpleName, Kind kind) {
                return super.isNameCompatible(simpleName + ".sig", Kind.OTHER);
            }

            public JavaFileObject getDelegate() {
                return fileObject;
            }
        }

        @Override
        public String getSourceVersion() {
            return sourceVersion;
        }

        @Override
        public String getTargetVersion() {
            return sourceVersion;
        }

        @Override
        public List<PluginInfo<Processor>> getAnnotationProcessors() {
            return Collections.emptyList();
        }

        @Override
        public List<PluginInfo<Plugin>> getPlugins() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getAdditionalOptions() {
            return Collections.emptyList();
        }

        @Override
        public void close() throws IOException {
            for (FileSystem fs : ctSym2FileSystem.values()) {
                fs.close();
            }
            ctSym2FileSystem.clear();
        }

    }

    static Path findCtSym() {
        String javaHome = System.getProperty("java.home");
        Path file = Paths.get(javaHome);
        // file == ${jdk.home}
        for (String name : symbolFileLocation)
            file = file.resolve(name);
        return file;
    }

}
