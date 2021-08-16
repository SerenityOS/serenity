/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.symbolgenerator;

import build.tools.symbolgenerator.CreateSymbols
                                  .ModuleHeaderDescription
                                  .ProvidesDescription;
import build.tools.symbolgenerator.CreateSymbols
                                  .ModuleHeaderDescription
                                  .RequiresDescription;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Annotation.Annotation_element_value;
import com.sun.tools.classfile.Annotation.Array_element_value;
import com.sun.tools.classfile.Annotation.Class_element_value;
import com.sun.tools.classfile.Annotation.Enum_element_value;
import com.sun.tools.classfile.Annotation.Primitive_element_value;
import com.sun.tools.classfile.Annotation.element_value;
import com.sun.tools.classfile.Annotation.element_value_pair;
import com.sun.tools.classfile.AnnotationDefault_attribute;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Double_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Float_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Integer_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Long_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Module_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Package_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_String_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.ConstantValue_attribute;
import com.sun.tools.classfile.Deprecated_attribute;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Exceptions_attribute;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.InnerClasses_attribute;
import com.sun.tools.classfile.InnerClasses_attribute.Info;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.MethodParameters_attribute;
import com.sun.tools.classfile.ModuleResolution_attribute;
import com.sun.tools.classfile.ModuleTarget_attribute;
import com.sun.tools.classfile.Module_attribute;
import com.sun.tools.classfile.Module_attribute.ExportsEntry;
import com.sun.tools.classfile.Module_attribute.OpensEntry;
import com.sun.tools.classfile.Module_attribute.ProvidesEntry;
import com.sun.tools.classfile.Module_attribute.RequiresEntry;
import com.sun.tools.classfile.NestHost_attribute;
import com.sun.tools.classfile.NestMembers_attribute;
import com.sun.tools.classfile.PermittedSubclasses_attribute;
import com.sun.tools.classfile.Record_attribute;
import com.sun.tools.classfile.Record_attribute.ComponentInfo;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;
import com.sun.tools.classfile.RuntimeInvisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeInvisibleParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleParameterAnnotations_attribute;
import com.sun.tools.classfile.Signature_attribute;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;
import java.util.Optional;

/**
 * A tool for processing the .sym.txt files.
 *
 * To add historical data for JDK N, N >= 11, do the following:
 *  * cd <open-jdk-checkout>/make/data/symbols
 *  * <jdk-N>/bin/java --add-exports jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED \
 *                     --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 *                     --add-exports jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
 *                     --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
 *                     --add-modules jdk.jdeps \
 *                     ../../../make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java \
 *                     build-description-incremental symbols include.list
 *  * sanity-check the new and updates files in make/data/symbols and commit them
 *
 * The tools allows to:
 *  * convert the .sym.txt into class/sig files for ct.sym
 *  * in cooperation with the adjacent history Probe, construct .sym.txt files for previous platforms
 *  * enhance existing .sym.txt files with a a new set .sym.txt for the current platform
 *
 * To convert the .sym.txt files to class/sig files from ct.sym, run:
 *     java build.tool.symbolgenerator.CreateSymbols build-ctsym <platform-description-file> <target-directory>
 *
 * The <platform-description-file> is a file of this format:
 *     generate platforms <platform-ids-to-generate separate with ':'>
 *     platform version <platform-id1> files <.sym.txt files containing history data for given platform, separate with ':'>
 *     platform version <platform-id2> base <base-platform-id> files <.sym.txt files containing history data for given platform, separate with ':'>
 *
 * The content of platform "<base-platform-id>" is also automatically added to the content of
 * platform "<platform-id2>", unless explicitly excluded in "<platform-id2>"'s .sym.txt files.
 *
 * To create the .sym.txt files, first run the history Probe for all the previous platforms:
 *     <jdk-N>/bin/java build.tools.symbolgenerator.Probe <classes-for-N>
 *
 * Where <classes-for-N> is a name of a file into which the classes from the bootclasspath of <jdk-N>
 * will be written.
 *
 * Then create the <platform-description-file> file and the .sym.txt files like this:
 *     java build.tools.symbolgenerator.CreateSymbols build-description <target-directory> <path-to-a-JDK-root> <include-list-file>
 *                                                    <platform-id1> <target-file-for-platform1> "<none>"
 *                                                    <platform-id2> <target-file-for-platform2> <diff-against-platform2>
 *                                                    <platform-id3> <target-file-for-platform3> <diff-against-platform3>
 *                                                    ...
 *
 * The <include-list-file> is a file that specifies classes that should be included/excluded.
 * Lines that start with '+' represent class or package that should be included, '-' class or package
 * that should be excluded. '/' should be used as package name delimiter, packages should end with '/'.
 * Several include list files may be specified, separated by File.pathSeparator.
 *
 * When <diff-against-platformN> is specified, the .sym.txt files for platform N will only contain
 * differences between platform N and the specified platform. The first platform (denoted F further)
 * that is specified should use literal value "<none>", to have all the APIs of the platform written to
 * the .sym.txt files. If there is an existing platform with full .sym.txt files in the repository,
 * that platform should be used as the first platform to avoid unnecessary changes to the .sym.txt
 * files. The <diff-against-platformN> for platform N should be determined as follows: if N < F, then
 * <diff-against-platformN> should be N + 1. If F < N, then <diff-against-platformN> should be N - 1.
 * If N is a custom/specialized sub-version of another platform N', then <diff-against-platformN> should be N'.
 *
 * To generate the .sym.txt files for OpenJDK 7 and 8:
 *     <jdk-7>/bin/java build.tools.symbolgenerator.Probe OpenJDK7.classes
 *     <jdk-8>/bin/java build.tools.symbolgenerator.Probe OpenJDK8.classes
 *     java build.tools.symbolgenerator.CreateSymbols build-description make/data/symbols $TOPDIR make/data/symbols/include.list
 *                                                    8 OpenJDK8.classes '<none>'
 *                                                    7 OpenJDK7.classes 8
 *
 * Note: the versions are expected to be a single character.
 *
 */
public class CreateSymbols {

    //<editor-fold defaultstate="collapsed" desc="ct.sym construction">
    /**Create sig files for ct.sym reading the classes description from the directory that contains
     * {@code ctDescriptionFile}, using the file as a recipe to create the sigfiles.
     */
    @SuppressWarnings("unchecked")
    public void createSymbols(String ctDescriptionFileExtra, String ctDescriptionFile, String ctSymLocation,
                              long timestamp, String currentVersion, String systemModules) throws IOException {
        LoadDescriptions data = load(ctDescriptionFileExtra != null ? Paths.get(ctDescriptionFileExtra)
                                                                    : null,
                                     Paths.get(ctDescriptionFile));

        stripNonExistentAnnotations(data);
        splitHeaders(data.classes);

        Map<String, Map<Character, String>> package2Version2Module = new HashMap<>();
        Map<String, Set<FileData>> directory2FileData = new TreeMap<>();

        for (ModuleDescription md : data.modules.values()) {
            for (ModuleHeaderDescription mhd : md.header) {
                List<String> versionsList =
                        Collections.singletonList(mhd.versions);
                writeModulesForVersions(directory2FileData,
                                        md,
                                        mhd,
                                        versionsList);
                mhd.exports.stream().forEach(pkg -> {
                    for (char v : mhd.versions.toCharArray()) {
                        package2Version2Module.computeIfAbsent(pkg, dummy -> new HashMap<>()).put(v, md.name);
                    }
                });
            }
        }

        for (ClassDescription classDescription : data.classes) {
            Map<Character, String> version2Module = package2Version2Module.getOrDefault(classDescription.packge().replace('.', '/'), Collections.emptyMap());
            for (ClassHeaderDescription header : classDescription.header) {
                Set<String> jointVersions = new HashSet<>();
                jointVersions.add(header.versions);
                limitJointVersion(jointVersions, classDescription.fields);
                limitJointVersion(jointVersions, classDescription.methods);
                Map<String, StringBuilder> module2Versions = new HashMap<>();
                for (char v : header.versions.toCharArray()) {
                    String module = version2Module.get(v);
                    if (module == null) {
                        if (v >= '9') {
                            throw new AssertionError("No module for " + classDescription.name +
                                                     " and version " + v);
                        }
                        module = version2Module.get('9');
                        if (module == null) {
                            module = "java.base";
                        }
                    }
                    module2Versions.computeIfAbsent(module, dummy -> new StringBuilder()).append(v);
                }
                for (Entry<String, StringBuilder> e : module2Versions.entrySet()) {
                    Set<String> currentVersions = new HashSet<>(jointVersions);
                    limitJointVersion(currentVersions, e.getValue().toString());
                    currentVersions = currentVersions.stream().filter(vers -> !disjoint(vers, e.getValue().toString())).collect(Collectors.toSet());
                    writeClassesForVersions(directory2FileData, classDescription, header, e.getKey(), currentVersions);
                }
            }
        }

        currentVersion = Integer.toString(Integer.parseInt(currentVersion), Character.MAX_RADIX);
        currentVersion = currentVersion.toUpperCase(Locale.ROOT);

        openDirectory(directory2FileData, currentVersion + "/")
                .add(new FileData(currentVersion + "/system-modules",
                                  Files.readAllBytes(Paths.get(systemModules))));

        try (OutputStream fos = new FileOutputStream(ctSymLocation);
             OutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream jos = new ZipOutputStream(bos)) {
            for (Entry<String, Set<FileData>> e : directory2FileData.entrySet()) {
                jos.putNextEntry(createZipEntry(e.getKey(), timestamp));
                for (FileData fd : e.getValue()) {
                    jos.putNextEntry(createZipEntry(fd.fileName, timestamp));
                    jos.write(fd.fileData);
                }
            }
        }
    }

    private static final String PREVIEW_FEATURE_ANNOTATION_OLD =
            "Ljdk/internal/PreviewFeature;";
    private static final String PREVIEW_FEATURE_ANNOTATION_NEW =
            "Ljdk/internal/javac/PreviewFeature;";
    private static final String PREVIEW_FEATURE_ANNOTATION_INTERNAL =
            "Ljdk/internal/PreviewFeature+Annotation;";
    private static final String VALUE_BASED_ANNOTATION =
            "Ljdk/internal/ValueBased;";
    private static final String VALUE_BASED_ANNOTATION_INTERNAL =
            "Ljdk/internal/ValueBased+Annotation;";
    public static final Set<String> HARDCODED_ANNOTATIONS = new HashSet<>(
            List.of("Ljdk/Profile+Annotation;",
                    "Lsun/Proprietary+Annotation;",
                    PREVIEW_FEATURE_ANNOTATION_OLD,
                    PREVIEW_FEATURE_ANNOTATION_NEW,
                    VALUE_BASED_ANNOTATION));

    private void stripNonExistentAnnotations(LoadDescriptions data) {
        Set<String> allClasses = data.classes.name2Class.keySet();
        data.modules.values().forEach(mod -> {
            stripNonExistentAnnotations(allClasses, mod.header);
        });
        data.classes.classes.forEach(clazz -> {
            stripNonExistentAnnotations(allClasses, clazz.header);
            stripNonExistentAnnotations(allClasses, clazz.fields);
            stripNonExistentAnnotations(allClasses, clazz.methods);
        });
    }

    private void stripNonExistentAnnotations(Set<String> allClasses, Iterable<? extends FeatureDescription> descs) {
        descs.forEach(d -> stripNonExistentAnnotations(allClasses, d));
    }

    private void stripNonExistentAnnotations(Set<String> allClasses, FeatureDescription d) {
        stripNonExistentAnnotations(allClasses, d.classAnnotations);
        stripNonExistentAnnotations(allClasses, d.runtimeAnnotations);
    }

    private void stripNonExistentAnnotations(Set<String> allClasses, List<AnnotationDescription> annotations) {
        if (annotations != null)
            annotations.removeIf(ann -> !HARDCODED_ANNOTATIONS.contains(ann.annotationType) &&
                                        !allClasses.contains(ann.annotationType.substring(1, ann.annotationType.length() - 1)));
    }

    private ZipEntry createZipEntry(String name, long timestamp) {
        ZipEntry ze = new ZipEntry(name);

        ze.setTime(timestamp);
        return ze;
    }

    public static String EXTENSION = ".sig";

    LoadDescriptions load(Path ctDescriptionWithExtraContent, Path ctDescriptionOpen) throws IOException {
        Map<String, PlatformInput> platforms = new LinkedHashMap<>();

        if (ctDescriptionWithExtraContent != null && Files.isRegularFile(ctDescriptionWithExtraContent)) {
            try (LineBasedReader reader = new LineBasedReader(ctDescriptionWithExtraContent)) {
                while (reader.hasNext()) {
                    switch (reader.lineKey) {
                        case "generate":
                            //ignore
                            reader.moveNext();
                            break;
                        case "platform":
                            PlatformInput platform = PlatformInput.load(ctDescriptionWithExtraContent,
                                                                        reader);
                            platforms.put(platform.version, platform);
                            reader.moveNext();
                            break;
                        default:
                            throw new IllegalStateException("Unknown key: " + reader.lineKey);
                    }
                }
            }
        }

        Set<String> generatePlatforms = null;

        try (LineBasedReader reader = new LineBasedReader(ctDescriptionOpen)) {
            while (reader.hasNext()) {
                switch (reader.lineKey) {
                    case "generate":
                        String[] platformsAttr = reader.attributes.get("platforms").split(":");
                        generatePlatforms = new HashSet<>(List.of(platformsAttr));
                        reader.moveNext();
                        break;
                    case "platform":
                        PlatformInput platform = PlatformInput.load(ctDescriptionOpen, reader);
                        if (!platforms.containsKey(platform.version))
                            platforms.put(platform.version, platform);
                        reader.moveNext();
                        break;
                    default:
                        throw new IllegalStateException("Unknown key: " + reader.lineKey);
                }
            }
        }

        Map<String, ClassDescription> classes = new LinkedHashMap<>();
        Map<String, ModuleDescription> modules = new LinkedHashMap<>();

        for (PlatformInput platform : platforms.values()) {
            for (ClassDescription cd : classes.values()) {
                addNewVersion(cd.header, platform.basePlatform, platform.version);
                addNewVersion(cd.fields, platform.basePlatform, platform.version);
                addNewVersion(cd.methods, platform.basePlatform, platform.version);
            }
            for (ModuleDescription md : modules.values()) {
                addNewVersion(md.header, platform.basePlatform, platform.version);
            }
            for (String input : platform.files) {
                Path inputFile = platform.ctDescription.getParent().resolve(input);
                try (LineBasedReader reader = new LineBasedReader(inputFile)) {
                    while (reader.hasNext()) {
                        String nameAttr = reader.attributes.get("name");
                        switch (reader.lineKey) {
                            case "class": case "-class":
                                ClassDescription cd =
                                        classes.computeIfAbsent(nameAttr,
                                                n -> new ClassDescription());
                                if ("-class".equals(reader.lineKey)) {
                                    removeVersion(cd.header, h -> true,
                                                  platform.version);
                                    reader.moveNext();
                                    continue;
                                }
                                cd.read(reader, platform.basePlatform,
                                        platform.version);
                                break;
                            case "module": {
                                ModuleDescription md =
                                        modules.computeIfAbsent(nameAttr,
                                                n -> new ModuleDescription());
                                md.read(reader, platform.basePlatform,
                                        platform.version);
                                break;
                            }
                            case "-module": {
                                ModuleDescription md =
                                        modules.computeIfAbsent(nameAttr,
                                                n -> new ModuleDescription());
                                removeVersion(md.header, h -> true,
                                              platform.version);
                                reader.moveNext();
                                break;
                            }
                        }
                    }
                }
            }
        }

        ClassList result = new ClassList();

        classes.values().forEach(result::add);
        return new LoadDescriptions(result,
                                    modules,
                                    new ArrayList<>(platforms.values()));
    }

    private static void removeVersion(LoadDescriptions load, String deletePlatform) {
        for (Iterator<ClassDescription> it = load.classes.iterator(); it.hasNext();) {
            ClassDescription desc = it.next();
            Iterator<ClassHeaderDescription> chdIt = desc.header.iterator();

            while (chdIt.hasNext()) {
                ClassHeaderDescription chd = chdIt.next();

                chd.versions = removeVersion(chd.versions, deletePlatform);
                if (chd.versions.isEmpty()) {
                    chdIt.remove();
                }
            }

            if (desc.header.isEmpty()) {
                it.remove();
                continue;
            }

            Iterator<MethodDescription> methodIt = desc.methods.iterator();

            while (methodIt.hasNext()) {
                MethodDescription method = methodIt.next();

                method.versions = removeVersion(method.versions, deletePlatform);
                if (method.versions.isEmpty())
                    methodIt.remove();
            }

            Iterator<FieldDescription> fieldIt = desc.fields.iterator();

            while (fieldIt.hasNext()) {
                FieldDescription field = fieldIt.next();

                field.versions = removeVersion(field.versions, deletePlatform);
                if (field.versions.isEmpty())
                    fieldIt.remove();
            }
        }

        for (Iterator<ModuleDescription> it = load.modules.values().iterator(); it.hasNext();) {
            ModuleDescription desc = it.next();
            Iterator<ModuleHeaderDescription> mhdIt = desc.header.iterator();

            while (mhdIt.hasNext()) {
                ModuleHeaderDescription mhd = mhdIt.next();

                mhd.versions = removeVersion(mhd.versions, deletePlatform);
                if (mhd.versions.isEmpty())
                    mhdIt.remove();
            }

            if (desc.header.isEmpty()) {
                it.remove();
                continue;
            }
        }
    }

    static final class LoadDescriptions {
        public final ClassList classes;
        public final Map<String, ModuleDescription> modules;
        public final List<PlatformInput> versions;

        public LoadDescriptions(ClassList classes,
                                Map<String, ModuleDescription>  modules,
                                List<PlatformInput> versions) {
            this.classes = classes;
            this.modules = modules;
            this.versions = versions;
        }

    }

    static final class LineBasedReader implements AutoCloseable {
        private final BufferedReader input;
        public String lineKey;
        public Map<String, String> attributes = new HashMap<>();

        public LineBasedReader(Path input) throws IOException {
            this.input = Files.newBufferedReader(input);
            moveNext();
        }

        public void moveNext() throws IOException {
            String line = input.readLine();

            if (line == null) {
                lineKey = null;
                return ;
            }

            if (line.trim().isEmpty() || line.startsWith("#")) {
                moveNext();
                return ;
            }

            String[] parts = line.split(" ");

            lineKey = parts[0];
            attributes.clear();

            for (int i = 1; i < parts.length; i += 2) {
                attributes.put(parts[i], unquote(parts[i + 1]));
            }
        }

        public boolean hasNext() {
            return lineKey != null;
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }

    private static String reduce(String original, String other) {
        Set<String> otherSet = new HashSet<>();

        for (char v : other.toCharArray()) {
            otherSet.add("" + v);
        }

        return reduce(original, otherSet);
    }

    private static String reduce(String original, Set<String> generate) {
        StringBuilder sb = new StringBuilder();

        for (char v : original.toCharArray()) {
            if (generate.contains("" + v)) {
                sb.append(v);
            }
        }
        return sb.toString();
    }

    private static String removeVersion(String original, String remove) {
        StringBuilder sb = new StringBuilder();

        for (char v : original.toCharArray()) {
            if (v != remove.charAt(0)) {
                sb.append(v);
            }
        }
        return sb.toString();
    }

    private static class PlatformInput {
        public final String version;
        public final String basePlatform;
        public final List<String> files;
        public final Path ctDescription;
        public PlatformInput(Path ctDescription, String version, String basePlatform, List<String> files) {
            this.ctDescription = ctDescription;
            this.version = version;
            this.basePlatform = basePlatform;
            this.files = files;
        }

        public static PlatformInput load(Path ctDescription, LineBasedReader in) throws IOException {
            return new PlatformInput(ctDescription,
                                     in.attributes.get("version"),
                                     in.attributes.get("base"),
                                     List.of(in.attributes.get("files").split(":")));
        }
    }

    static void addNewVersion(Collection<? extends FeatureDescription> features,
                       String baselineVersion,
                       String version) {
        features.stream()
                .filter(f -> f.versions.contains(baselineVersion))
                .forEach(f -> f.versions += version);
    }

    static <T extends FeatureDescription> void removeVersion(Collection<T> features,
                                                             Predicate<T> shouldRemove,
                                                             String version) {
        for (T existing : features) {
            if (shouldRemove.test(existing) && existing.versions.endsWith(version)) {
                existing.versions = existing.versions.replace(version, "");
                return;
            }
        }
    }

    /**Changes to class header of an outer class (like adding a new type parameter) may affect
     * its innerclasses. So if the outer class's header is different for versions A and B, need to
     * split its innerclasses headers to also be different for versions A and B.
     */
    static void splitHeaders(ClassList classes) {
        Set<String> ctVersions = new HashSet<>();

        for (ClassDescription cd : classes) {
            for (ClassHeaderDescription header : cd.header) {
                for (char c : header.versions.toCharArray()) {
                    ctVersions.add("" + c);
                }
            }
        }

        classes.sort();

        for (ClassDescription cd : classes) {
            Map<String, String> outerSignatures2Version = new HashMap<>();

            for (String version : ctVersions) { //XXX
                ClassDescription outer = cd;
                String outerSignatures = "";

                while ((outer = classes.enclosingClass(outer)) != null) {
                    for (ClassHeaderDescription outerHeader : outer.header) {
                        if (outerHeader.versions.contains(version)) {
                            outerSignatures += outerHeader.signature;
                        }
                    }
                }

                outerSignatures2Version.compute(outerSignatures,
                                                 (key, value) -> value != null ? value + version : version);
            }

            List<ClassHeaderDescription> newHeaders = new ArrayList<>();

            HEADER_LOOP: for (ClassHeaderDescription header : cd.header) {
                for (String versions : outerSignatures2Version.values()) {
                    if (containsAll(versions, header.versions)) {
                        newHeaders.add(header);
                        continue HEADER_LOOP;
                    }
                    if (disjoint(versions, header.versions)) {
                        continue;
                    }
                    ClassHeaderDescription newHeader = new ClassHeaderDescription();
                    newHeader.classAnnotations = header.classAnnotations;
                    newHeader.deprecated = header.deprecated;
                    newHeader.extendsAttr = header.extendsAttr;
                    newHeader.flags = header.flags;
                    newHeader.implementsAttr = header.implementsAttr;
                    newHeader.innerClasses = header.innerClasses;
                    newHeader.runtimeAnnotations = header.runtimeAnnotations;
                    newHeader.signature = header.signature;
                    newHeader.versions = reduce(header.versions, versions);

                    newHeaders.add(newHeader);
                }
            }

            cd.header = newHeaders;
        }
    }

    void limitJointVersion(Set<String> jointVersions, List<? extends FeatureDescription> features) {
        for (FeatureDescription feature : features) {
            limitJointVersion(jointVersions, feature.versions);
        }
    }

    void limitJointVersion(Set<String> jointVersions, String versions) {
        for (String version : jointVersions) {
            if (!containsAll(versions, version) &&
                !disjoint(versions, version)) {
                StringBuilder featurePart = new StringBuilder();
                StringBuilder otherPart = new StringBuilder();
                for (char v : version.toCharArray()) {
                    if (versions.indexOf(v) != (-1)) {
                        featurePart.append(v);
                    } else {
                        otherPart.append(v);
                    }
                }
                jointVersions.remove(version);
                if (featurePart.length() == 0 || otherPart.length() == 0) {
                    throw new AssertionError();
                }
                jointVersions.add(featurePart.toString());
                jointVersions.add(otherPart.toString());
                break;
            }
        }
    }

    private static boolean containsAll(String versions, String subVersions) {
        for (char c : subVersions.toCharArray()) {
            if (versions.indexOf(c) == (-1))
                return false;
        }
        return true;
    }

    private static boolean disjoint(String version1, String version2) {
        for (char c : version2.toCharArray()) {
            if (version1.indexOf(c) != (-1))
                return false;
        }
        return true;
    }

    void writeClassesForVersions(Map<String, Set<FileData>> directory2FileData,
                                 ClassDescription classDescription,
                                 ClassHeaderDescription header,
                                 String module,
                                 Iterable<String> versions)
            throws IOException {
        for (String ver : versions) {
            writeClass(directory2FileData, classDescription, header, module, ver);
        }
    }

    void writeModulesForVersions(Map<String, Set<FileData>> directory2FileData,
                                 ModuleDescription moduleDescription,
                                 ModuleHeaderDescription header,
                                 Iterable<String> versions)
            throws IOException {
        for (String ver : versions) {
            writeModule(directory2FileData, moduleDescription, header, ver);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Class Writing">
    void writeModule(Map<String, Set<FileData>> directory2FileData,
                    ModuleDescription moduleDescription,
                    ModuleHeaderDescription header,
                    String version) throws IOException {
        List<CPInfo> constantPool = new ArrayList<>();
        constantPool.add(null);
        int currentClass = addClass(constantPool, "module-info");
        int superclass = 0;
        int[] interfaces = new int[0];
        AccessFlags flags = new AccessFlags(header.flags);
        Map<String, Attribute> attributesMap = new HashMap<>();
        addAttributes(moduleDescription, header, constantPool, attributesMap);
        Attributes attributes = new Attributes(attributesMap);
        CPInfo[] cpData = constantPool.toArray(new CPInfo[constantPool.size()]);
        ConstantPool cp = new ConstantPool(cpData);
        ClassFile classFile = new ClassFile(0xCAFEBABE,
                Target.DEFAULT.minorVersion,
                Target.DEFAULT.majorVersion,
                cp,
                flags,
                currentClass,
                superclass,
                interfaces,
                new Field[0],
                new Method[0],
                attributes);

        doWrite(directory2FileData, version, moduleDescription.name, "module-info" + EXTENSION, classFile);
    }

    void writeClass(Map<String, Set<FileData>> directory2FileData,
                    ClassDescription classDescription,
                    ClassHeaderDescription header,
                    String module,
                    String version) throws IOException {
        List<CPInfo> constantPool = new ArrayList<>();
        constantPool.add(null);
        List<Method> methods = new ArrayList<>();
        for (MethodDescription methDesc : classDescription.methods) {
            if (disjoint(methDesc.versions, version))
                continue;
            Descriptor descriptor = new Descriptor(addString(constantPool, methDesc.descriptor));
            //TODO: LinkedHashMap to avoid param annotations vs. Signature problem in javac's ClassReader:
            Map<String, Attribute> attributesMap = new LinkedHashMap<>();
            addAttributes(methDesc, constantPool, attributesMap);
            Attributes attributes = new Attributes(attributesMap);
            AccessFlags flags = new AccessFlags(methDesc.flags);
            int nameString = addString(constantPool, methDesc.name);
            methods.add(new Method(flags, nameString, descriptor, attributes));
        }
        List<Field> fields = new ArrayList<>();
        for (FieldDescription fieldDesc : classDescription.fields) {
            if (disjoint(fieldDesc.versions, version))
                continue;
            Descriptor descriptor = new Descriptor(addString(constantPool, fieldDesc.descriptor));
            Map<String, Attribute> attributesMap = new HashMap<>();
            addAttributes(fieldDesc, constantPool, attributesMap);
            Attributes attributes = new Attributes(attributesMap);
            AccessFlags flags = new AccessFlags(fieldDesc.flags);
            int nameString = addString(constantPool, fieldDesc.name);
            fields.add(new Field(flags, nameString, descriptor, attributes));
        }
        int currentClass = addClass(constantPool, classDescription.name);
        int superclass = header.extendsAttr != null ? addClass(constantPool, header.extendsAttr) : 0;
        int[] interfaces = new int[header.implementsAttr.size()];
        int i = 0;
        for (String intf : header.implementsAttr) {
            interfaces[i++] = addClass(constantPool, intf);
        }
        AccessFlags flags = new AccessFlags(header.flags);
        Map<String, Attribute> attributesMap = new HashMap<>();
        addAttributes(header, constantPool, attributesMap);
        Attributes attributes = new Attributes(attributesMap);
        ConstantPool cp = new ConstantPool(constantPool.toArray(new CPInfo[constantPool.size()]));
        ClassFile classFile = new ClassFile(0xCAFEBABE,
                Target.DEFAULT.minorVersion,
                Target.DEFAULT.majorVersion,
                cp,
                flags,
                currentClass,
                superclass,
                interfaces,
                fields.toArray(new Field[0]),
                methods.toArray(new Method[0]),
                attributes);

        doWrite(directory2FileData, version, module, classDescription.name + EXTENSION, classFile);
    }

    private void doWrite(Map<String, Set<FileData>> directory2FileData,
                         String version,
                         String moduleName,
                         String fileName,
                         ClassFile classFile) throws IOException {
        int lastSlash = fileName.lastIndexOf('/');
        String pack = lastSlash != (-1) ? fileName.substring(0, lastSlash + 1) : "/";
        String directory = version + "/" + moduleName + "/" + pack;
        String fullFileName = version + "/" + moduleName + "/" + fileName;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ClassWriter w = new ClassWriter();

            w.write(classFile, out);

            openDirectory(directory2FileData, directory)
                .add(new FileData(fullFileName, out.toByteArray()));
        }
    }

    private Set<FileData> openDirectory(Map<String, Set<FileData>> directory2FileData,
                               String directory) {
        Comparator<FileData> fileCompare = (fd1, fd2) -> fd1.fileName.compareTo(fd2.fileName);
        return directory2FileData.computeIfAbsent(directory, d -> new TreeSet<>(fileCompare));
    }

    private static class FileData {
        public final String fileName;
        public final byte[] fileData;

        public FileData(String fileName, byte[] fileData) {
            this.fileName = fileName;
            this.fileData = fileData;
        }

    }

    private void addAttributes(ModuleDescription md,
                               ModuleHeaderDescription header,
                               List<CPInfo> cp,
                               Map<String, Attribute> attributes) {
        addGenericAttributes(header, cp, attributes);
        if (header.moduleResolution != null) {
            int attrIdx = addString(cp, Attribute.ModuleResolution);
            final ModuleResolution_attribute resIdx =
                    new ModuleResolution_attribute(attrIdx,
                                                   header.moduleResolution);
            attributes.put(Attribute.ModuleResolution, resIdx);
        }
        if (header.moduleTarget != null) {
            int attrIdx = addString(cp, Attribute.ModuleTarget);
            int targetIdx = addString(cp, header.moduleTarget);
            attributes.put(Attribute.ModuleTarget,
                           new ModuleTarget_attribute(attrIdx, targetIdx));
        }
        int attrIdx = addString(cp, Attribute.Module);
        attributes.put(Attribute.Module,
                       new Module_attribute(attrIdx,
                             addModuleName(cp, md.name),
                             0,
                             0,
                             header.requires
                                   .stream()
                                   .map(r -> createRequiresEntry(cp, r))
                                   .collect(Collectors.toList())
                                   .toArray(new RequiresEntry[0]),
                             header.exports
                                   .stream()
                                   .map(e -> createExportsEntry(cp, e))
                                   .collect(Collectors.toList())
                                   .toArray(new ExportsEntry[0]),
                             header.opens
                                   .stream()
                                   .map(e -> createOpensEntry(cp, e))
                                   .collect(Collectors.toList())
                                   .toArray(new OpensEntry[0]),
                             header.uses
                                   .stream()
                                   .mapToInt(u -> addClassName(cp, u))
                                   .toArray(),
                             header.provides
                                   .stream()
                                   .map(p -> createProvidesEntry(cp, p))
                                   .collect(Collectors.toList())
                                   .toArray(new ProvidesEntry[0])));
        addInnerClassesAttribute(header, cp, attributes);
    }

    private static RequiresEntry createRequiresEntry(List<CPInfo> cp,
            RequiresDescription r) {
        final int idx = addModuleName(cp, r.moduleName);
        return new RequiresEntry(idx,
                                 r.flags,
                                 r.version != null
                                         ? addInt(cp, r.version)
                                         : 0);
    }

    private static ExportsEntry createExportsEntry(List<CPInfo> cp,
                                                   String e) {
        return new ExportsEntry(addPackageName(cp, e), 0, new int[0]);
    }

    private static OpensEntry createOpensEntry(List<CPInfo> cp, String e) {
        return new OpensEntry(addPackageName(cp, e), 0, new int[0]);
    }

    private static ProvidesEntry createProvidesEntry(List<CPInfo> cp,
            ModuleHeaderDescription.ProvidesDescription p) {
        final int idx = addClassName(cp, p.interfaceName);
        return new ProvidesEntry(idx, p.implNames
                                       .stream()
                                       .mapToInt(i -> addClassName(cp, i))
                                       .toArray());
    }

    private void addAttributes(ClassHeaderDescription header,
            List<CPInfo> constantPool, Map<String, Attribute> attributes) {
        addGenericAttributes(header, constantPool, attributes);
        if (header.nestHost != null) {
            int attributeString = addString(constantPool, Attribute.NestHost);
            int nestHost = addClass(constantPool, header.nestHost);
            attributes.put(Attribute.NestHost,
                           new NestHost_attribute(attributeString, nestHost));
        }
        if (header.nestMembers != null && !header.nestMembers.isEmpty()) {
            int attributeString = addString(constantPool, Attribute.NestMembers);
            int[] nestMembers = new int[header.nestMembers.size()];
            int i = 0;
            for (String intf : header.nestMembers) {
                nestMembers[i++] = addClass(constantPool, intf);
            }
            attributes.put(Attribute.NestMembers,
                           new NestMembers_attribute(attributeString, nestMembers));
        }
        if (header.isRecord) {
            assert header.recordComponents != null;
            int attributeString = addString(constantPool, Attribute.Record);
            ComponentInfo[] recordComponents = new ComponentInfo[header.recordComponents.size()];
            int i = 0;
            for (RecordComponentDescription rcd : header.recordComponents) {
                int name = addString(constantPool, rcd.name);
                Descriptor desc = new Descriptor(addString(constantPool, rcd.descriptor));
                Map<String, Attribute> nestedAttrs = new HashMap<>();
                addGenericAttributes(rcd, constantPool, nestedAttrs);
                Attributes attrs = new Attributes(nestedAttrs);
                recordComponents[i++] = new ComponentInfo(name, desc, attrs);
            }
            attributes.put(Attribute.Record,
                           new Record_attribute(attributeString, recordComponents));
        }
        if (header.isSealed) {
            int attributeString = addString(constantPool, Attribute.PermittedSubclasses);
            int[] subclasses = new int[header.permittedSubclasses.size()];
            int i = 0;
            for (String intf : header.permittedSubclasses) {
                subclasses[i++] = addClass(constantPool, intf);
            }
            attributes.put(Attribute.PermittedSubclasses,
                    new PermittedSubclasses_attribute(attributeString, subclasses));
        }
        addInnerClassesAttribute(header, constantPool, attributes);
    }

    private void addInnerClassesAttribute(HeaderDescription header,
            List<CPInfo> constantPool, Map<String, Attribute> attributes) {
        if (header.innerClasses != null && !header.innerClasses.isEmpty()) {
            Info[] innerClasses = new Info[header.innerClasses.size()];
            int i = 0;
            for (InnerClassInfo info : header.innerClasses) {
                innerClasses[i++] =
                        new Info(info.innerClass == null ? 0 : addClass(constantPool, info.innerClass),
                                 info.outerClass == null ? 0 : addClass(constantPool, info.outerClass),
                                 info.innerClassName == null ? 0 : addString(constantPool, info.innerClassName),
                                 new AccessFlags(info.innerClassFlags));
            }
            int attributeString = addString(constantPool, Attribute.InnerClasses);
            attributes.put(Attribute.InnerClasses,
                           new InnerClasses_attribute(attributeString, innerClasses));
        }
    }

    private void addAttributes(MethodDescription desc, List<CPInfo> constantPool, Map<String, Attribute> attributes) {
        addGenericAttributes(desc, constantPool, attributes);
        if (desc.thrownTypes != null) {
            int[] exceptions = new int[desc.thrownTypes.size()];
            int i = 0;
            for (String exc : desc.thrownTypes) {
                exceptions[i++] = addClass(constantPool, exc);
            }
            int attributeString = addString(constantPool, Attribute.Exceptions);
            attributes.put(Attribute.Exceptions,
                           new Exceptions_attribute(attributeString, exceptions));
        }
        if (desc.annotationDefaultValue != null) {
            int attributeString = addString(constantPool, Attribute.AnnotationDefault);
            element_value attributeValue = createAttributeValue(constantPool,
                                                                desc.annotationDefaultValue);
            attributes.put(Attribute.AnnotationDefault,
                           new AnnotationDefault_attribute(attributeString, attributeValue));
        }
        if (desc.classParameterAnnotations != null && !desc.classParameterAnnotations.isEmpty()) {
            int attributeString =
                    addString(constantPool, Attribute.RuntimeInvisibleParameterAnnotations);
            Annotation[][] annotations =
                    createParameterAnnotations(constantPool, desc.classParameterAnnotations);
            attributes.put(Attribute.RuntimeInvisibleParameterAnnotations,
                           new RuntimeInvisibleParameterAnnotations_attribute(attributeString,
                                   annotations));
        }
        if (desc.runtimeParameterAnnotations != null && !desc.runtimeParameterAnnotations.isEmpty()) {
            int attributeString =
                    addString(constantPool, Attribute.RuntimeVisibleParameterAnnotations);
            Annotation[][] annotations =
                    createParameterAnnotations(constantPool, desc.runtimeParameterAnnotations);
            attributes.put(Attribute.RuntimeVisibleParameterAnnotations,
                           new RuntimeVisibleParameterAnnotations_attribute(attributeString,
                                   annotations));
        }
        if (desc.methodParameters != null && !desc.methodParameters.isEmpty()) {
            int attributeString =
                    addString(constantPool, Attribute.MethodParameters);
            MethodParameters_attribute.Entry[] entries =
                    desc.methodParameters
                        .stream()
                        .map(p -> new MethodParameters_attribute.Entry(addString(constantPool, p.name),
                                                                        p.flags))
                        .toArray(s -> new MethodParameters_attribute.Entry[s]);
            attributes.put(Attribute.MethodParameters,
                           new MethodParameters_attribute(attributeString, entries));
        }
    }

    private void addAttributes(FieldDescription desc, List<CPInfo> constantPool, Map<String, Attribute> attributes) {
        addGenericAttributes(desc, constantPool, attributes);
        if (desc.constantValue != null) {
            Pair<Integer, Character> constantPoolEntry =
                    addConstant(constantPool, desc.constantValue, false);
            Assert.checkNonNull(constantPoolEntry);
            int constantValueString = addString(constantPool, Attribute.ConstantValue);
            attributes.put(Attribute.ConstantValue,
                           new ConstantValue_attribute(constantValueString, constantPoolEntry.fst));
        }
    }

    private void addGenericAttributes(FeatureDescription desc, List<CPInfo> constantPool, Map<String, Attribute> attributes) {
        if (desc.deprecated) {
            int attributeString = addString(constantPool, Attribute.Deprecated);
            attributes.put(Attribute.Deprecated,
                           new Deprecated_attribute(attributeString));
        }
        if (desc.signature != null) {
            int attributeString = addString(constantPool, Attribute.Signature);
            int signatureString = addString(constantPool, desc.signature);
            attributes.put(Attribute.Signature,
                           new Signature_attribute(attributeString, signatureString));
        }
        if (desc.classAnnotations != null && !desc.classAnnotations.isEmpty()) {
            int attributeString = addString(constantPool, Attribute.RuntimeInvisibleAnnotations);
            Annotation[] annotations = createAnnotations(constantPool, desc.classAnnotations);
            attributes.put(Attribute.RuntimeInvisibleAnnotations,
                           new RuntimeInvisibleAnnotations_attribute(attributeString, annotations));
        }
        if (desc.runtimeAnnotations != null && !desc.runtimeAnnotations.isEmpty()) {
            int attributeString = addString(constantPool, Attribute.RuntimeVisibleAnnotations);
            Annotation[] annotations = createAnnotations(constantPool, desc.runtimeAnnotations);
            attributes.put(Attribute.RuntimeVisibleAnnotations,
                           new RuntimeVisibleAnnotations_attribute(attributeString, annotations));
        }
    }

    private Annotation[] createAnnotations(List<CPInfo> constantPool, List<AnnotationDescription> desc) {
        Annotation[] result = new Annotation[desc.size()];
        int i = 0;

        for (AnnotationDescription ad : desc) {
            result[i++] = createAnnotation(constantPool, ad);
        }

        return result;
    }

    private Annotation[][] createParameterAnnotations(List<CPInfo> constantPool, List<List<AnnotationDescription>> desc) {
        Annotation[][] result = new Annotation[desc.size()][];
        int i = 0;

        for (List<AnnotationDescription> paramAnnos : desc) {
            result[i++] = createAnnotations(constantPool, paramAnnos);
        }

        return result;
    }

    private Annotation createAnnotation(List<CPInfo> constantPool, AnnotationDescription desc) {
        String annotationType = desc.annotationType;
        Map<String, Object> values = desc.values;

        if (PREVIEW_FEATURE_ANNOTATION_NEW.equals(annotationType)) {
            //the non-public PreviewFeature annotation will not be available in ct.sym,
            //replace with purely synthetic javac-internal annotation:
            annotationType = PREVIEW_FEATURE_ANNOTATION_INTERNAL;
        }

        if (PREVIEW_FEATURE_ANNOTATION_OLD.equals(annotationType)) {
            //the non-public PreviewFeature annotation will not be available in ct.sym,
            //replace with purely synthetic javac-internal annotation:
            annotationType = PREVIEW_FEATURE_ANNOTATION_INTERNAL;
            values = new HashMap<>(values);
            Boolean essentialAPI = (Boolean) values.remove("essentialAPI");
            values.put("reflective", essentialAPI != null && !essentialAPI);
        }

        if (VALUE_BASED_ANNOTATION.equals(annotationType)) {
            //the non-public ValueBased annotation will not be available in ct.sym,
            //replace with purely synthetic javac-internal annotation:
            annotationType = VALUE_BASED_ANNOTATION_INTERNAL;
        }

        return new Annotation(null,
                              addString(constantPool, annotationType),
                              createElementPairs(constantPool, values));
    }

    private element_value_pair[] createElementPairs(List<CPInfo> constantPool, Map<String, Object> annotationAttributes) {
        element_value_pair[] pairs = new element_value_pair[annotationAttributes.size()];
        int i = 0;

        for (Entry<String, Object> e : annotationAttributes.entrySet()) {
            int elementNameString = addString(constantPool, e.getKey());
            element_value value = createAttributeValue(constantPool, e.getValue());
            pairs[i++] = new element_value_pair(elementNameString, value);
        }

        return pairs;
    }

    private element_value createAttributeValue(List<CPInfo> constantPool, Object value) {
        Pair<Integer, Character> constantPoolEntry = addConstant(constantPool, value, true);
        if (constantPoolEntry != null) {
            return new Primitive_element_value(constantPoolEntry.fst, constantPoolEntry.snd);
        } else if (value instanceof EnumConstant) {
            EnumConstant ec = (EnumConstant) value;
            return new Enum_element_value(addString(constantPool, ec.type),
                                          addString(constantPool, ec.constant),
                                          'e');
        } else if (value instanceof ClassConstant) {
            ClassConstant cc = (ClassConstant) value;
            return new Class_element_value(addString(constantPool, cc.type), 'c');
        } else if (value instanceof AnnotationDescription) {
            Annotation annotation = createAnnotation(constantPool, ((AnnotationDescription) value));
            return new Annotation_element_value(annotation, '@');
        } else if (value instanceof Collection) {
            @SuppressWarnings("unchecked")
                    Collection<Object> array = (Collection<Object>) value;
            element_value[] values = new element_value[array.size()];
            int i = 0;

            for (Object elem : array) {
                values[i++] = createAttributeValue(constantPool, elem);
            }

            return new Array_element_value(values, '[');
        }
        throw new IllegalStateException(value.getClass().getName());
    }

    private static Pair<Integer, Character> addConstant(List<CPInfo> constantPool, Object value, boolean annotation) {
        if (value instanceof Boolean) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Integer_info(((Boolean) value) ? 1 : 0)), 'Z');
        } else if (value instanceof Byte) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Integer_info((byte) value)), 'B');
        } else if (value instanceof Character) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Integer_info((char) value)), 'C');
        } else if (value instanceof Short) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Integer_info((short) value)), 'S');
        } else if (value instanceof Integer) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Integer_info((int) value)), 'I');
        } else if (value instanceof Long) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Long_info((long) value)), 'J');
        } else if (value instanceof Float) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Float_info((float) value)), 'F');
        } else if (value instanceof Double) {
            return Pair.of(addToCP(constantPool, new CONSTANT_Double_info((double) value)), 'D');
        } else if (value instanceof String) {
            int stringIndex = addString(constantPool, (String) value);
            if (annotation) {
                return Pair.of(stringIndex, 's');
            } else {
                return Pair.of(addToCP(constantPool, new CONSTANT_String_info(null, stringIndex)), 's');
            }
        }

        return null;
    }

    private static int addString(List<CPInfo> constantPool, String string) {
        Assert.checkNonNull(string);

        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Utf8_info) {
                if (((CONSTANT_Utf8_info) info).value.equals(string)) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Utf8_info(string));
    }

    private static int addInt(List<CPInfo> constantPool, int value) {
        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Integer_info) {
                if (((CONSTANT_Integer_info) info).value == value) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Integer_info(value));
    }

    private static int addModuleName(List<CPInfo> constantPool, String moduleName) {
        int nameIdx = addString(constantPool, moduleName);
        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Module_info) {
                if (((CONSTANT_Module_info) info).name_index == nameIdx) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Module_info(null, nameIdx));
    }

    private static int addPackageName(List<CPInfo> constantPool, String packageName) {
        int nameIdx = addString(constantPool, packageName);
        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Package_info) {
                if (((CONSTANT_Package_info) info).name_index == nameIdx) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Package_info(null, nameIdx));
    }

    private static int addClassName(List<CPInfo> constantPool, String className) {
        int nameIdx = addString(constantPool, className);
        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Class_info) {
                if (((CONSTANT_Class_info) info).name_index == nameIdx) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Class_info(null, nameIdx));
    }

    private static int addToCP(List<CPInfo> constantPool, CPInfo entry) {
        int result = constantPool.size();

        constantPool.add(entry);

        if (entry.size() > 1) {
            constantPool.add(null);
        }

        return result;
    }

    private static int addClass(List<CPInfo> constantPool, String className) {
        int classNameIndex = addString(constantPool, className);

        int i = 0;
        for (CPInfo info : constantPool) {
            if (info instanceof CONSTANT_Class_info) {
                if (((CONSTANT_Class_info) info).name_index == classNameIndex) {
                    return i;
                }
            }
            i++;
        }

        return addToCP(constantPool, new CONSTANT_Class_info(null, classNameIndex));
    }
    //</editor-fold>
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Create Symbol Description">
    public void createBaseLine(List<VersionDescription> versions,
                               ExcludeIncludeList excludesIncludes,
                               Path descDest,
                               String[] args) throws IOException {
        ClassList classes = new ClassList();
        Map<String, ModuleDescription> modules = new HashMap<>();

        for (VersionDescription desc : versions) {
            Iterable<byte[]> classFileData = loadClassData(desc.classes);

            loadVersionClasses(classes, modules, classFileData, excludesIncludes, desc.version, null);
        }

        List<PlatformInput> platforms =
                versions.stream()
                        .map(desc -> new PlatformInput(null,
                                                       desc.version,
                                                       desc.primaryBaseline,
                                                       null))
                        .collect(Collectors.toList());

        dumpDescriptions(classes, modules, platforms, Set.of(), descDest.resolve("symbols"), args);
    }
    //where:
        private static final String DO_NO_MODIFY =
            "#\n" +
            "# Copyright (c) {YEAR}, Oracle and/or its affiliates. All rights reserved.\n" +
            "# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n" +
            "#\n" +
            "# This code is free software; you can redistribute it and/or modify it\n" +
            "# under the terms of the GNU General Public License version 2 only, as\n" +
            "# published by the Free Software Foundation.  Oracle designates this\n" +
            "# particular file as subject to the \"Classpath\" exception as provided\n" +
            "# by Oracle in the LICENSE file that accompanied this code.\n" +
            "#\n" +
            "# This code is distributed in the hope that it will be useful, but WITHOUT\n" +
            "# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n" +
            "# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n" +
            "# version 2 for more details (a copy is included in the LICENSE file that\n" +
            "# accompanied this code).\n" +
            "#\n" +
            "# You should have received a copy of the GNU General Public License version\n" +
            "# 2 along with this work; if not, write to the Free Software Foundation,\n" +
            "# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n" +
            "#\n" +
            "# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA\n" +
            "# or visit www.oracle.com if you need additional information or have any\n" +
            "# questions.\n" +
            "#\n" +
            "# ##########################################################\n" +
            "# ### THIS FILE IS AUTOMATICALLY GENERATED. DO NOT EDIT. ###\n" +
            "# ##########################################################\n" +
            "#\n";

        private Iterable<byte[]> loadClassData(String path) {
            List<byte[]> classFileData = new ArrayList<>();

            try (BufferedReader descIn =
                    Files.newBufferedReader(Paths.get(path))) {
                String line;
                while ((line = descIn.readLine()) != null) {
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    for (int i = 0; i < line.length(); i += 2) {
                        String hex = line.substring(i, i + 2);
                        data.write(Integer.parseInt(hex, 16));
                    }
                    classFileData.add(data.toByteArray());
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }

            return classFileData;
        }

    private void loadVersionClasses(ClassList classes,
                                    Map<String, ModuleDescription> modules,
                                    Iterable<byte[]> classData,
                                    ExcludeIncludeList excludesIncludes,
                                    String version,
                                    String baseline) {
        Map<String, ModuleDescription> currentVersionModules =
                new HashMap<>();

        for (byte[] classFileData : classData) {
            try (InputStream in = new ByteArrayInputStream(classFileData)) {
                inspectModuleInfoClassFile(in,
                                           currentVersionModules, version);
            } catch (IOException | ConstantPoolException ex) {
                throw new IllegalStateException(ex);
            }
        }

        ExcludeIncludeList currentEIList = excludesIncludes;

        if (!currentVersionModules.isEmpty()) {
            Set<String> includes = new HashSet<>();

            for (ModuleDescription md : currentVersionModules.values()) {
                md.header.get(0).exports.stream().map(e -> e + '/')
                                        .forEach(includes::add);
            }

            currentEIList = new ExcludeIncludeList(includes,
                                                   Collections.emptySet());
        }

        ClassList currentVersionClasses = new ClassList();

        for (byte[] classFileData : classData) {
            try (InputStream in = new ByteArrayInputStream(classFileData)) {
                inspectClassFile(in, currentVersionClasses,
                                 currentEIList, version);
            } catch (IOException | ConstantPoolException ex) {
                throw new IllegalStateException(ex);
            }
        }

        ModuleDescription unsupported =
                currentVersionModules.get("jdk.unsupported");

        if (unsupported != null) {
            for (ClassDescription cd : currentVersionClasses.classes) {
                if (unsupported.header
                               .get(0)
                               .exports
                               .contains(cd.packge().replace('.', '/'))) {
                    ClassHeaderDescription ch = cd.header.get(0);
                    if (ch.classAnnotations == null) {
                        ch.classAnnotations = new ArrayList<>();
                    }
                    AnnotationDescription ad;
                    ad = new AnnotationDescription(PROPERITARY_ANNOTATION,
                                                   Collections.emptyMap());
                    ch.classAnnotations.add(ad);
                }
            }
        }

        Set<String> includedClasses = new HashSet<>();
        boolean modified;

        do {
            modified = false;

            for (ClassDescription clazz : currentVersionClasses) {
                ClassHeaderDescription header = clazz.header.get(0);

                if (includeEffectiveAccess(currentVersionClasses, clazz)) {
                    modified |= include(includedClasses, currentVersionClasses, clazz.name);
                }

                if (includedClasses.contains(clazz.name)) {
                    modified |= include(includedClasses, currentVersionClasses, header.extendsAttr);
                    for (String i : header.implementsAttr) {
                        modified |= include(includedClasses, currentVersionClasses, i);
                    }

                    modified |= includeOutputType(Collections.singleton(header),
                                                  h -> "",
                                                  includedClasses,
                                                  currentVersionClasses);
                    modified |= includeOutputType(clazz.fields,
                                                  f -> f.descriptor,
                                                  includedClasses,
                                                  currentVersionClasses);
                    modified |= includeOutputType(clazz.methods,
                                                  m -> m.descriptor,
                                                  includedClasses,
                                                  currentVersionClasses);
                }
            }
        } while (modified);

        for (ClassDescription clazz : currentVersionClasses) {
            if (!includedClasses.contains(clazz.name)) {
                continue;
            }

            ClassHeaderDescription header = clazz.header.get(0);

            if (header.nestMembers != null) {
                Iterator<String> nestMemberIt = header.nestMembers.iterator();

                while(nestMemberIt.hasNext()) {
                    String member = nestMemberIt.next();
                    if (!includedClasses.contains(member))
                        nestMemberIt.remove();
                }
            }

            if (header.innerClasses != null) {
                Iterator<InnerClassInfo> innerClassIt = header.innerClasses.iterator();

                while(innerClassIt.hasNext()) {
                    InnerClassInfo ici = innerClassIt.next();
                    if (!includedClasses.contains(ici.innerClass))
                        innerClassIt.remove();
                }
            }

            ClassDescription existing = classes.find(clazz.name, true);

            if (existing != null) {
                addClassHeader(existing, header, version, baseline);
                for (MethodDescription currentMethod : clazz.methods) {
                    addMethod(existing, currentMethod, version, baseline);
                }
                for (FieldDescription currentField : clazz.fields) {
                    addField(existing, currentField, version, baseline);
                }
            } else {
                classes.add(clazz);
            }
        }

        for (ModuleDescription module : currentVersionModules.values()) {
            ModuleHeaderDescription header = module.header.get(0);

            if (header.innerClasses != null) {
                Iterator<InnerClassInfo> innerClassIt =
                        header.innerClasses.iterator();

                while(innerClassIt.hasNext()) {
                    InnerClassInfo ici = innerClassIt.next();
                    if (!includedClasses.contains(ici.innerClass))
                        innerClassIt.remove();
                }
            }

            ModuleDescription existing = modules.get(module.name);

            if (existing != null) {
                addModuleHeader(existing, header, version);
            } else {
                modules.put(module.name, module);
            }
        }
    }
    //where:
        private static final String PROPERITARY_ANNOTATION =
                "Lsun/Proprietary+Annotation;";

    private void dumpDescriptions(ClassList classes,
                                  Map<String, ModuleDescription> modules,
                                  List<PlatformInput> versions,
                                  Set<String> forceWriteVersions,
                                  Path ctDescriptionFile,
                                  String[] args) throws IOException {
        classes.sort();

        Map<String, String> package2Modules = new HashMap<>();

        versions.stream()
                .filter(v -> "9".compareTo(v.version) <= 0)
                .sorted((v1, v2) -> v1.version.compareTo(v2.version))
                .forEach(v -> {
            for (ModuleDescription md : modules.values()) {
                md.header
                  .stream()
                  .filter(h -> h.versions.contains(v.version))
                  .flatMap(h -> h.exports.stream())
                  .map(p -> p.replace('/', '.'))
                  .forEach(p -> package2Modules.putIfAbsent(p, md.name));
            }
        });

        package2Modules.put("java.awt.dnd.peer", "java.desktop");
        package2Modules.put("java.awt.peer", "java.desktop");
        package2Modules.put("jdk", "java.base");

        Map<String, List<ClassDescription>> module2Classes = new HashMap<>();

        for (ClassDescription clazz : classes) {
            String pack = clazz.packge();
            String module = package2Modules.get(pack);

            if (module == null) {
                module = "java.base";

                OUTER: while (!pack.isEmpty()) {
                    for (Entry<String, String> p2M : package2Modules.entrySet()) {
                        if (p2M.getKey().startsWith(pack)) {
                            module = p2M.getValue();
                            break OUTER;
                        }
                    }
                    int dot = pack.lastIndexOf('.');
                    if (dot == (-1))
                        break;
                    pack = pack.substring(0, dot);
                }
            }
            module2Classes.computeIfAbsent(module, m -> new ArrayList<>())
                    .add(clazz);
        }

        modules.keySet()
               .stream()
               .filter(m -> !module2Classes.containsKey(m))
               .forEach(m -> module2Classes.put(m, Collections.emptyList()));

        Files.createDirectories(ctDescriptionFile.getParent());

        int year = Calendar.getInstance(TimeZone.getTimeZone("UTF"), Locale.ROOT)
                           .get(Calendar.YEAR);

        try (Writer symbolsOut = Files.newBufferedWriter(ctDescriptionFile)) {
            Map<PlatformInput, List<String>> outputFiles = new LinkedHashMap<>();

            for (PlatformInput desc : versions) {
                List<String> files = desc.files;

                if (files == null || forceWriteVersions.contains(desc.version)) {
                    files = new ArrayList<>();
                    for (Entry<String, List<ClassDescription>> e : module2Classes.entrySet()) {
                        StringWriter data = new StringWriter();
                        ModuleDescription module = modules.get(e.getKey());

                        if (module != null) { //module == null should only be in tests.
                            module.write(data, desc.basePlatform, desc.version);
                        }

                        for (ClassDescription clazz : e.getValue()) {
                            clazz.write(data, desc.basePlatform, desc.version);
                        }

                        String fileName = e.getKey() + "-" + desc.version + ".sym.txt";
                        Path f = ctDescriptionFile.getParent().resolve(fileName);

                        String dataString = data.toString();

                        if (!dataString.isEmpty()) {
                            String existingYear = null;
                            boolean hasChange = true;
                            if (Files.isReadable(f)) {
                                String oldContent = Files.readString(f, StandardCharsets.UTF_8);
                                int yearPos = DO_NO_MODIFY.indexOf("{YEAR}");
                                String headerPattern =
                                        Pattern.quote(DO_NO_MODIFY.substring(0, yearPos)) +
                                        "([0-9]+)(, [0-9]+)?" +
                                        Pattern.quote(DO_NO_MODIFY.substring(yearPos + "{YEAR}".length()));
                                String pattern = headerPattern +
                                                 Pattern.quote(dataString);
                                Matcher m = Pattern.compile(pattern, Pattern.MULTILINE).matcher(oldContent);
                                if (m.matches()) {
                                    hasChange = false;
                                } else {
                                    m = Pattern.compile(headerPattern).matcher(oldContent);
                                    if (m.find()) {
                                        existingYear = m.group(1);
                                    }
                                }
                            }
                            if (hasChange) {
                                try (Writer out = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                                    String currentYear = String.valueOf(year);
                                    String yearSpec = (existingYear != null && !currentYear.equals(existingYear) ? existingYear + ", " : "") + currentYear;
                                    out.append(DO_NO_MODIFY.replace("{YEAR}", yearSpec));
                                    out.write(dataString);
                                }
                            }
                            files.add(f.getFileName().toString());
                        }
                    }
                }

                outputFiles.put(desc, files);
            }
            symbolsOut.append(DO_NO_MODIFY.replace("{YEAR}", "2015, " + year));
            symbolsOut.append("#command used to generate this file:\n");
            symbolsOut.append("#")
                      .append(CreateSymbols.class.getName())
                      .append(" ")
                      .append(Arrays.stream(args)
                                    .collect(Collectors.joining(" ")))
                      .append("\n");
            symbolsOut.append("#\n");
            symbolsOut.append("generate platforms ")
                      .append(versions.stream()
                                      .map(v -> v.version)
                                      .sorted()
                                      .collect(Collectors.joining(":")))
                      .append("\n");
            for (Entry<PlatformInput, List<String>> versionFileEntry : outputFiles.entrySet()) {
                symbolsOut.append("platform version ")
                          .append(versionFileEntry.getKey().version);
                if (versionFileEntry.getKey().basePlatform != null) {
                    symbolsOut.append(" base ")
                              .append(versionFileEntry.getKey().basePlatform);
                }
                symbolsOut.append(" files ")
                          .append(versionFileEntry.getValue()
                                                  .stream()
                                                  .map(p -> p)
                                                  .sorted()
                                                  .collect(Collectors.joining(":")))
                          .append("\n");
            }
        }
    }

    private void incrementalUpdate(String ctDescriptionFile,
                                   String excludeFile,
                                   String platformVersion,
                                   Iterable<byte[]> classBytes,
                                   Function<LoadDescriptions, String> baseline,
                                   String[] args) throws IOException {
        String currentVersion =
                Integer.toString(Integer.parseInt(platformVersion), Character.MAX_RADIX);
        String version = currentVersion.toUpperCase(Locale.ROOT);
        Path ctDescriptionPath = Paths.get(ctDescriptionFile).toAbsolutePath();
        LoadDescriptions data = load(null, ctDescriptionPath);

        ClassList classes = data.classes;
        Map<String, ModuleDescription> modules = data.modules;
        List<PlatformInput> versions = data.versions;

        ExcludeIncludeList excludeList =
                ExcludeIncludeList.create(excludeFile);

        loadVersionClasses(classes, modules, classBytes, excludeList, "$", version);

        removeVersion(data, version);

        for (ModuleDescription md : data.modules.values()) {
            for (ModuleHeaderDescription header : md.header) {
                header.versions = header.versions.replace("$", version);
            }
        }

        for (ClassDescription clazzDesc : data.classes) {
            for (ClassHeaderDescription header : clazzDesc.header) {
                header.versions = header.versions.replace("$", version);
            }
            for (MethodDescription method : clazzDesc.methods) {
                method.versions = method.versions.replace("$", version);
            }
            for (FieldDescription field : clazzDesc.fields) {
                field.versions = field.versions.replace("$", version);
            }
        }

        if (versions.stream().noneMatch(inp -> version.equals(inp.version))) {
            versions.add(new PlatformInput(null, version, baseline.apply(data), null));
        }

        Set<String> writeVersions = new HashSet<>();

        writeVersions.add(version);

        //re-write all platforms that have version as their basline:
        versions.stream()
                .filter(inp -> version.equals(inp.basePlatform))
                .map(inp -> inp.version)
                .forEach(writeVersions::add);
        dumpDescriptions(classes, modules, versions, writeVersions, ctDescriptionPath, args);
    }

    public void createIncrementalBaseLineFromDataFile(String ctDescriptionFile,
                                                      String excludeFile,
                                                      String version,
                                                      String dataFile,
                                                      String baseline,
                                                      String[] args) throws IOException {
        incrementalUpdate(ctDescriptionFile, excludeFile, version, loadClassData(dataFile), x -> baseline, args);
    }

    public void createIncrementalBaseLine(String ctDescriptionFile,
                                          String excludeFile,
                                          String[] args) throws IOException {
        String specVersion = System.getProperty("java.specification.version");
        Iterable<byte[]> classBytes = dumpCurrentClasses();
        Function<LoadDescriptions, String> baseline = data -> {
            if (data.versions.isEmpty()) {
                return null;
            } else {
                return data.versions.stream()
                                    .sorted((v1, v2) -> v2.version.compareTo(v1.version))
                                    .findFirst()
                                    .get()
                                    .version;
            }
        };
        incrementalUpdate(ctDescriptionFile, excludeFile, specVersion, classBytes, baseline, args);
    }

    private List<byte[]> dumpCurrentClasses() throws IOException {
        JavacTool tool = JavacTool.create();
        Context ctx = new Context();
        String version = System.getProperty("java.specification.version");
        JavacTask task = tool.getTask(null, null, null,
                                      List.of("--release", version),
                                      null, null, ctx);
        task.getElements().getTypeElement("java.lang.Object");
        JavaFileManager fm = ctx.get(JavaFileManager.class);

        List<byte[]> data = new ArrayList<>();
        for (Location modLoc : LOCATIONS) {
            for (Set<JavaFileManager.Location> module :
                    fm.listLocationsForModules(modLoc)) {
                for (JavaFileManager.Location loc : module) {
                    Iterable<JavaFileObject> files =
                            fm.list(loc,
                                    "",
                                    EnumSet.of(Kind.CLASS),
                                    true);

                    for (JavaFileObject jfo : files) {
                        try (InputStream is = jfo.openInputStream();
                             InputStream in =
                                     new BufferedInputStream(is)) {
                            ByteArrayOutputStream baos =
                                    new ByteArrayOutputStream();

                            in.transferTo(baos);
                            data.add(baos.toByteArray());
                        }
                    }
                }
            }
        }

        return data;
    }
    //where:
        private static final List<StandardLocation> LOCATIONS =
                List.of(StandardLocation.SYSTEM_MODULES,
                        StandardLocation.UPGRADE_MODULE_PATH);

    //<editor-fold defaultstate="collapsed" desc="Class Reading">
    //non-final for tests:
    public static String PROFILE_ANNOTATION = "Ljdk/Profile+Annotation;";
    public static boolean ALLOW_NON_EXISTING_CLASSES = false;

    private void inspectClassFile(InputStream in, ClassList classes, ExcludeIncludeList excludesIncludes, String version) throws IOException, ConstantPoolException {
        ClassFile cf = ClassFile.read(in);

        if (cf.access_flags.is(AccessFlags.ACC_MODULE)) {
            return ;
        }

        if (!excludesIncludes.accepts(cf.getName())) {
            return ;
        }

        ClassHeaderDescription headerDesc = new ClassHeaderDescription();

        headerDesc.flags = cf.access_flags.flags;

        if (cf.super_class != 0) {
            headerDesc.extendsAttr = cf.getSuperclassName();
        }
        List<String> interfaces = new ArrayList<>();
        for (int i = 0; i < cf.interfaces.length; i++) {
            interfaces.add(cf.getInterfaceName(i));
        }
        headerDesc.implementsAttr = interfaces;
        for (Attribute attr : cf.attributes) {
            if (!readAttribute(cf, headerDesc, attr))
                return ;
        }

        ClassDescription clazzDesc = null;

        for (ClassDescription cd : classes) {
            if (cd.name.equals(cf.getName())) {
                clazzDesc = cd;
                break;
            }
        }

        if (clazzDesc == null) {
            clazzDesc = new ClassDescription();
            clazzDesc.name = cf.getName();
            classes.add(clazzDesc);
        }

        addClassHeader(clazzDesc, headerDesc, version, null);

        for (Method m : cf.methods) {
            if (!include(m.access_flags.flags))
                continue;
            MethodDescription methDesc = new MethodDescription();
            methDesc.flags = m.access_flags.flags;
            methDesc.name = m.getName(cf.constant_pool);
            methDesc.descriptor = m.descriptor.getValue(cf.constant_pool);
            for (Attribute attr : m.attributes) {
                readAttribute(cf, methDesc, attr);
            }
            addMethod(clazzDesc, methDesc, version, null);
        }
        for (Field f : cf.fields) {
            if (!include(f.access_flags.flags))
                continue;
            FieldDescription fieldDesc = new FieldDescription();
            fieldDesc.flags = f.access_flags.flags;
            fieldDesc.name = f.getName(cf.constant_pool);
            fieldDesc.descriptor = f.descriptor.getValue(cf.constant_pool);
            for (Attribute attr : f.attributes) {
                readAttribute(cf, fieldDesc, attr);
            }
            addField(clazzDesc, fieldDesc, version, null);
        }
    }

    private void inspectModuleInfoClassFile(InputStream in,
            Map<String, ModuleDescription> modules,
            String version) throws IOException, ConstantPoolException {
        ClassFile cf = ClassFile.read(in);

        if (!cf.access_flags.is(AccessFlags.ACC_MODULE)) {
            return ;
        }

        ModuleHeaderDescription headerDesc = new ModuleHeaderDescription();

        headerDesc.versions = version;
        headerDesc.flags = cf.access_flags.flags;

        for (Attribute attr : cf.attributes) {
            if (!readAttribute(cf, headerDesc, attr))
                return ;
        }

        String name = headerDesc.name;

        ModuleDescription moduleDesc = modules.get(name);

        if (moduleDesc == null) {
            moduleDesc = new ModuleDescription();
            moduleDesc.name = name;
            modules.put(moduleDesc.name, moduleDesc);
        }

        addModuleHeader(moduleDesc, headerDesc, version);
    }

    private void addModuleHeader(ModuleDescription moduleDesc,
                                 ModuleHeaderDescription headerDesc,
                                 String version) {
        //normalize:
        boolean existed = false;
        for (ModuleHeaderDescription existing : moduleDesc.header) {
            if (existing.equals(headerDesc)) {
                headerDesc = existing;
                existed = true;
            }
        }

        headerDesc.versions += version;

        if (!existed) {
            moduleDesc.header.add(headerDesc);
        }
    }

    private boolean include(int accessFlags) {
        return (accessFlags & (AccessFlags.ACC_PUBLIC | AccessFlags.ACC_PROTECTED)) != 0;
    }

    private void addClassHeader(ClassDescription clazzDesc, ClassHeaderDescription headerDesc, String version, String baseline) {
        //normalize:
        boolean existed = false;
        for (ClassHeaderDescription existing : clazzDesc.header) {
            if (existing.equals(headerDesc) && (!existed || (baseline != null && existing.versions.contains(baseline)))) {
                headerDesc = existing;
                existed = true;
            }
        }

        if (!existed) {
            //check if the only difference between the 7 and 8 version is the Profile annotation
            //if so, copy it to the pre-8 version, so save space
            for (ClassHeaderDescription existing : clazzDesc.header) {
                List<AnnotationDescription> annots = existing.classAnnotations;

                if (annots != null) {
                    for (AnnotationDescription ad : annots) {
                        if (PROFILE_ANNOTATION.equals(ad.annotationType)) {
                            existing.classAnnotations = new ArrayList<>(annots);
                            existing.classAnnotations.remove(ad);
                            if (existing.equals(headerDesc)) {
                                headerDesc = existing;
                                existed = true;
                            }
                            existing.classAnnotations = annots;
                            break;
                        }
                    }
                }
            }
        }

        headerDesc.versions += version;

        if (!existed) {
            clazzDesc.header.add(headerDesc);
        }
    }

    private void addMethod(ClassDescription clazzDesc, MethodDescription methDesc, String version, String baseline) {
        //normalize:
        boolean methodExisted = false;
        for (MethodDescription existing : clazzDesc.methods) {
            if (existing.equals(methDesc) && (!methodExisted || (baseline != null && existing.versions.contains(baseline)))) {
                methodExisted = true;
                methDesc = existing;
            }
        }
        methDesc.versions += version;
        if (!methodExisted) {
            clazzDesc.methods.add(methDesc);
        }
    }

    private void addField(ClassDescription clazzDesc, FieldDescription fieldDesc, String version, String baseline) {
        boolean fieldExisted = false;
        for (FieldDescription existing : clazzDesc.fields) {
            if (existing.equals(fieldDesc) && (!fieldExisted || (baseline != null && existing.versions.contains(baseline)))) {
                fieldExisted = true;
                fieldDesc = existing;
            }
        }
        fieldDesc.versions += version;
        if (!fieldExisted) {
            clazzDesc.fields.add(fieldDesc);
        }
    }

    private boolean readAttribute(ClassFile cf, FeatureDescription feature, Attribute attr) throws ConstantPoolException {
        String attrName = attr.getName(cf.constant_pool);
        switch (attrName) {
            case Attribute.AnnotationDefault:
                assert feature instanceof MethodDescription;
                element_value defaultValue = ((AnnotationDefault_attribute) attr).default_value;
                ((MethodDescription) feature).annotationDefaultValue =
                        convertElementValue(cf.constant_pool, defaultValue);
                break;
            case "Deprecated":
                feature.deprecated = true;
                break;
            case "Exceptions":
                assert feature instanceof MethodDescription;
                List<String> thrownTypes = new ArrayList<>();
                Exceptions_attribute exceptionAttr = (Exceptions_attribute) attr;
                for (int i = 0; i < exceptionAttr.exception_index_table.length; i++) {
                    thrownTypes.add(exceptionAttr.getException(i, cf.constant_pool));
                }
                ((MethodDescription) feature).thrownTypes = thrownTypes;
                break;
            case Attribute.InnerClasses:
                if (feature instanceof ModuleHeaderDescription)
                    break; //XXX
                assert feature instanceof ClassHeaderDescription;
                List<InnerClassInfo> innerClasses = new ArrayList<>();
                InnerClasses_attribute innerClassesAttr = (InnerClasses_attribute) attr;
                for (int i = 0; i < innerClassesAttr.number_of_classes; i++) {
                    CONSTANT_Class_info outerClassInfo =
                            innerClassesAttr.classes[i].getOuterClassInfo(cf.constant_pool);
                    InnerClassInfo info = new InnerClassInfo();
                    CONSTANT_Class_info innerClassInfo =
                            innerClassesAttr.classes[i].getInnerClassInfo(cf.constant_pool);
                    info.innerClass = innerClassInfo != null ? innerClassInfo.getName() : null;
                    info.outerClass = outerClassInfo != null ? outerClassInfo.getName() : null;
                    info.innerClassName = innerClassesAttr.classes[i].getInnerName(cf.constant_pool);
                    info.innerClassFlags = innerClassesAttr.classes[i].inner_class_access_flags.flags;
                    innerClasses.add(info);
                }
                ((ClassHeaderDescription) feature).innerClasses = innerClasses;
                break;
            case "RuntimeInvisibleAnnotations":
                feature.classAnnotations = annotations2Description(cf.constant_pool, attr);
                break;
            case "RuntimeVisibleAnnotations":
                feature.runtimeAnnotations = annotations2Description(cf.constant_pool, attr);
                break;
            case "Signature":
                feature.signature = ((Signature_attribute) attr).getSignature(cf.constant_pool);
                break;
            case "ConstantValue":
                assert feature instanceof FieldDescription;
                Object value = convertConstantValue(cf.constant_pool.get(((ConstantValue_attribute) attr).constantvalue_index), ((FieldDescription) feature).descriptor);
                if (((FieldDescription) feature).descriptor.equals("C")) {
                    value = (char) (int) value;
                }
                ((FieldDescription) feature).constantValue = value;
                break;
            case "SourceFile":
                //ignore, not needed
                break;
            case "BootstrapMethods":
                //ignore, not needed
                break;
            case "Code":
                //ignore, not needed
                break;
            case "EnclosingMethod":
                return false;
            case "Synthetic":
                break;
            case "RuntimeVisibleParameterAnnotations":
                assert feature instanceof MethodDescription;
                ((MethodDescription) feature).runtimeParameterAnnotations =
                        parameterAnnotations2Description(cf.constant_pool, attr);
                break;
            case "RuntimeInvisibleParameterAnnotations":
                assert feature instanceof MethodDescription;
                ((MethodDescription) feature).classParameterAnnotations =
                        parameterAnnotations2Description(cf.constant_pool, attr);
                break;
            case Attribute.Module: {
                assert feature instanceof ModuleHeaderDescription;
                ModuleHeaderDescription header =
                        (ModuleHeaderDescription) feature;
                Module_attribute mod = (Module_attribute) attr;

                header.name = cf.constant_pool
                                .getModuleInfo(mod.module_name)
                                .getName();

                header.exports =
                        Arrays.stream(mod.exports)
                              .filter(ee -> ee.exports_to_count == 0)
                              .map(ee -> getPackageName(cf, ee.exports_index))
                              .collect(Collectors.toList());
                header.requires =
                        Arrays.stream(mod.requires)
                              .map(r -> RequiresDescription.create(cf, r))
                              .collect(Collectors.toList());
                header.uses = Arrays.stream(mod.uses_index)
                                    .mapToObj(use -> getClassName(cf, use))
                                    .collect(Collectors.toList());
                header.provides =
                        Arrays.stream(mod.provides)
                              .map(p -> ProvidesDescription.create(cf, p))
                              .collect(Collectors.toList());
                break;
            }
            case Attribute.ModuleTarget: {
                assert feature instanceof ModuleHeaderDescription;
                ModuleHeaderDescription header =
                        (ModuleHeaderDescription) feature;
                ModuleTarget_attribute mod = (ModuleTarget_attribute) attr;
                if (mod.target_platform_index != 0) {
                    header.moduleTarget =
                            cf.constant_pool
                              .getUTF8Value(mod.target_platform_index);
                }
                break;
            }
            case Attribute.ModuleResolution: {
                assert feature instanceof ModuleHeaderDescription;
                ModuleHeaderDescription header =
                        (ModuleHeaderDescription) feature;
                ModuleResolution_attribute mod =
                        (ModuleResolution_attribute) attr;
                header.moduleResolution = mod.resolution_flags;
                break;
            }
            case Attribute.ModulePackages:
            case Attribute.ModuleHashes:
                break;
            case Attribute.NestHost: {
                assert feature instanceof ClassHeaderDescription;
                NestHost_attribute nestHost = (NestHost_attribute) attr;
                ClassHeaderDescription chd = (ClassHeaderDescription) feature;
                chd.nestHost = nestHost.getNestTop(cf.constant_pool).getName();
                break;
            }
            case Attribute.NestMembers: {
                assert feature instanceof ClassHeaderDescription;
                NestMembers_attribute nestMembers = (NestMembers_attribute) attr;
                ClassHeaderDescription chd = (ClassHeaderDescription) feature;
                chd.nestMembers = Arrays.stream(nestMembers.members_indexes)
                                        .mapToObj(i -> getClassName(cf, i))
                                        .collect(Collectors.toList());
                break;
            }
            case Attribute.Record: {
                assert feature instanceof ClassHeaderDescription;
                Record_attribute record = (Record_attribute) attr;
                List<RecordComponentDescription> components = new ArrayList<>();
                for (ComponentInfo info : record.component_info_arr) {
                    RecordComponentDescription rcd = new RecordComponentDescription();
                    rcd.name = info.getName(cf.constant_pool);
                    rcd.descriptor = info.descriptor.getValue(cf.constant_pool);
                    for (Attribute nestedAttr : info.attributes) {
                        readAttribute(cf, rcd, nestedAttr);
                    }
                    components.add(rcd);
                }
                ClassHeaderDescription chd = (ClassHeaderDescription) feature;
                chd.isRecord = true;
                chd.recordComponents = components;
                break;
            }
            case Attribute.MethodParameters: {
                assert feature instanceof MethodDescription;
                MethodParameters_attribute params = (MethodParameters_attribute) attr;
                MethodDescription method = (MethodDescription) feature;
                method.methodParameters = new ArrayList<>();
                for (MethodParameters_attribute.Entry e : params.method_parameter_table) {
                    String name = cf.constant_pool.getUTF8Value(e.name_index);
                    MethodDescription.MethodParam param =
                            new MethodDescription.MethodParam(e.flags, name);
                    method.methodParameters.add(param);
                }
                break;
            }
            case Attribute.PermittedSubclasses: {
                assert feature instanceof ClassHeaderDescription;
                PermittedSubclasses_attribute permittedSubclasses = (PermittedSubclasses_attribute) attr;
                ClassHeaderDescription chd = (ClassHeaderDescription) feature;
                chd.permittedSubclasses = Arrays.stream(permittedSubclasses.subtypes)
                        .mapToObj(i -> getClassName(cf, i))
                        .collect(Collectors.toList());
                chd.isSealed = true;
                break;
            }
            default:
                throw new IllegalStateException("Unhandled attribute: " +
                                                attrName);
        }

        return true;
    }

    private static String getClassName(ClassFile cf, int idx) {
        try {
            return cf.constant_pool.getClassInfo(idx).getName();
        } catch (InvalidIndex ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPool.UnexpectedEntry ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPoolException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String getPackageName(ClassFile cf, int idx) {
        try {
            return cf.constant_pool.getPackageInfo(idx).getName();
        } catch (InvalidIndex ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPool.UnexpectedEntry ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPoolException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String getModuleName(ClassFile cf, int idx) {
        try {
            return cf.constant_pool.getModuleInfo(idx).getName();
        } catch (InvalidIndex ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPool.UnexpectedEntry ex) {
            throw new IllegalStateException(ex);
        } catch (ConstantPoolException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Integer getVersion(ClassFile cf, int idx) {
        if (idx == 0)
            return null;
        try {
            return ((CONSTANT_Integer_info) cf.constant_pool.get(idx)).value;
        } catch (InvalidIndex ex) {
            throw new IllegalStateException(ex);
        }
    }

    Object convertConstantValue(CPInfo info, String descriptor) throws ConstantPoolException {
        if (info instanceof CONSTANT_Integer_info) {
            if ("Z".equals(descriptor))
                return ((CONSTANT_Integer_info) info).value == 1;
            else
                return ((CONSTANT_Integer_info) info).value;
        } else if (info instanceof CONSTANT_Long_info) {
            return ((CONSTANT_Long_info) info).value;
        } else if (info instanceof CONSTANT_Float_info) {
            return ((CONSTANT_Float_info) info).value;
        } else if (info instanceof CONSTANT_Double_info) {
            return ((CONSTANT_Double_info) info).value;
        } else if (info instanceof CONSTANT_String_info) {
            return ((CONSTANT_String_info) info).getString();
        }
        throw new IllegalStateException(info.getClass().getName());
    }

    Object convertElementValue(ConstantPool cp, element_value val) throws InvalidIndex, ConstantPoolException {
        switch (val.tag) {
            case 'Z':
                return ((CONSTANT_Integer_info) cp.get(((Primitive_element_value) val).const_value_index)).value != 0;
            case 'B':
                return (byte) ((CONSTANT_Integer_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'C':
                return (char) ((CONSTANT_Integer_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'S':
                return (short) ((CONSTANT_Integer_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'I':
                return ((CONSTANT_Integer_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'J':
                return ((CONSTANT_Long_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'F':
                return ((CONSTANT_Float_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 'D':
                return ((CONSTANT_Double_info) cp.get(((Primitive_element_value) val).const_value_index)).value;
            case 's':
                return ((CONSTANT_Utf8_info) cp.get(((Primitive_element_value) val).const_value_index)).value;

            case 'e':
                return new EnumConstant(cp.getUTF8Value(((Enum_element_value) val).type_name_index),
                        cp.getUTF8Value(((Enum_element_value) val).const_name_index));
            case 'c':
                return new ClassConstant(cp.getUTF8Value(((Class_element_value) val).class_info_index));

            case '@':
                return annotation2Description(cp, ((Annotation_element_value) val).annotation_value);

            case '[':
                List<Object> values = new ArrayList<>();
                for (element_value elem : ((Array_element_value) val).values) {
                    values.add(convertElementValue(cp, elem));
                }
                return values;
            default:
                throw new IllegalStateException("Currently unhandled tag: " + val.tag);
        }
    }

    private List<AnnotationDescription> annotations2Description(ConstantPool cp, Attribute attr) throws ConstantPoolException {
        RuntimeAnnotations_attribute annotationsAttr = (RuntimeAnnotations_attribute) attr;
        List<AnnotationDescription> descs = new ArrayList<>();
        for (Annotation a : annotationsAttr.annotations) {
            descs.add(annotation2Description(cp, a));
        }
        return descs;
    }

    private List<List<AnnotationDescription>> parameterAnnotations2Description(ConstantPool cp, Attribute attr) throws ConstantPoolException {
        RuntimeParameterAnnotations_attribute annotationsAttr =
                (RuntimeParameterAnnotations_attribute) attr;
        List<List<AnnotationDescription>> descs = new ArrayList<>();
        for (Annotation[] attrAnnos : annotationsAttr.parameter_annotations) {
            List<AnnotationDescription> paramDescs = new ArrayList<>();
            for (Annotation ann : attrAnnos) {
                paramDescs.add(annotation2Description(cp, ann));
            }
            descs.add(paramDescs);
        }
        return descs;
    }

    private AnnotationDescription annotation2Description(ConstantPool cp, Annotation a) throws ConstantPoolException {
        String annotationType = cp.getUTF8Value(a.type_index);
        Map<String, Object> values = new HashMap<>();

        for (element_value_pair e : a.element_value_pairs) {
            values.put(cp.getUTF8Value(e.element_name_index), convertElementValue(cp, e.value));
        }

        return new AnnotationDescription(annotationType, values);
    }
    //</editor-fold>

    protected boolean includeEffectiveAccess(ClassList classes, ClassDescription clazz) {
        if (!include(clazz.header.get(0).flags))
            return false;
        for (ClassDescription outer : classes.enclosingClasses(clazz)) {
            if (!include(outer.header.get(0).flags))
                return false;
        }
        return true;
    }

    boolean include(Set<String> includedClasses, ClassList classes, String clazzName) {
        if (clazzName == null)
            return false;

        boolean modified = includedClasses.add(clazzName);

        for (ClassDescription outer : classes.enclosingClasses(classes.find(clazzName, true))) {
            modified |= includedClasses.add(outer.name);
        }

        return modified;
    }

    <T extends FeatureDescription> boolean includeOutputType(Iterable<T> features,
                                                             Function<T, String> feature2Descriptor,
                                                             Set<String> includedClasses,
                                                             ClassList classes) {
        boolean modified = false;

        for (T feature : features) {
            CharSequence sig =
                    feature.signature != null ? feature.signature : feature2Descriptor.apply(feature);
            Matcher m = OUTPUT_TYPE_PATTERN.matcher(sig);
            while (m.find()) {
                modified |= include(includedClasses, classes, m.group(1));
            }
        }

        return modified;
    }

    static final Pattern OUTPUT_TYPE_PATTERN = Pattern.compile("L([^;<]+)(;|<)");

    public static class VersionDescription {
        public final String classes;
        public final String version;
        public final String primaryBaseline;

        public VersionDescription(String classes, String version, String primaryBaseline) {
            this.classes = classes;
            this.version = version;
            this.primaryBaseline = "<none>".equals(primaryBaseline) ? null : primaryBaseline;
        }

    }

    public static class ExcludeIncludeList {
        public final Set<String> includeList;
        public final Set<String> excludeList;

        protected ExcludeIncludeList(Set<String> includeList, Set<String> excludeList) {
            this.includeList = includeList;
            this.excludeList = excludeList;
        }

        public static ExcludeIncludeList create(String files) throws IOException {
            Set<String> includeList = new HashSet<>();
            Set<String> excludeList = new HashSet<>();
            for (String file : files.split(File.pathSeparator)) {
                try (Stream<String> lines = Files.lines(Paths.get(file))) {
                    lines.map(l -> l.substring(0, l.indexOf('#') != (-1) ? l.indexOf('#') : l.length()))
                         .filter(l -> !l.trim().isEmpty())
                         .forEach(l -> {
                             Set<String> target = l.startsWith("+") ? includeList : excludeList;
                             target.add(l.substring(1));
                         });
                }
            }
            return new ExcludeIncludeList(includeList, excludeList);
        }

        public boolean accepts(String className) {
            return matches(includeList, className) && !matches(excludeList, className);
        }

        private static boolean matches(Set<String> list, String className) {
            if (list.contains(className))
                return true;
            String pack = className.substring(0, className.lastIndexOf('/') + 1);
            return list.contains(pack);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Class Data Structures">
    static boolean checkChange(String versions, String version,
                               String baselineVersion) {
        return versions.contains(version) ^
               (baselineVersion != null &&
                versions.contains(baselineVersion));
    }

    static abstract class FeatureDescription {
        int flagsNormalization = ~0;
        int flags;
        boolean deprecated;
        String signature;
        String versions = "";
        List<AnnotationDescription> classAnnotations;
        List<AnnotationDescription> runtimeAnnotations;

        protected void writeAttributes(Appendable output) throws IOException {
            if (flags != 0)
                output.append(" flags " + Integer.toHexString(flags));
            if (deprecated) {
                output.append(" deprecated true");
            }
            if (signature != null) {
                output.append(" signature " + quote(signature, false));
            }
            if (classAnnotations != null && !classAnnotations.isEmpty()) {
                output.append(" classAnnotations ");
                for (AnnotationDescription a : classAnnotations) {
                    output.append(quote(a.toString(), false));
                }
            }
            if (runtimeAnnotations != null && !runtimeAnnotations.isEmpty()) {
                output.append(" runtimeAnnotations ");
                for (AnnotationDescription a : runtimeAnnotations) {
                    output.append(quote(a.toString(), false));
                }
            }
        }

        protected boolean shouldIgnore(String baselineVersion, String version) {
            return (!versions.contains(version) &&
                    (baselineVersion == null || !versions.contains(baselineVersion))) ||
                   (baselineVersion != null &&
                    versions.contains(baselineVersion) && versions.contains(version));
        }

        public abstract void write(Appendable output, String baselineVersion, String version) throws IOException;

        protected void readAttributes(LineBasedReader reader) {
            String inFlags = reader.attributes.get("flags");
            if (inFlags != null && !inFlags.isEmpty()) {
                flags = Integer.parseInt(inFlags, 16);
            }
            String inDeprecated = reader.attributes.get("deprecated");
            if ("true".equals(inDeprecated)) {
                deprecated = true;
            }
            signature = reader.attributes.get("signature");
            String inClassAnnotations = reader.attributes.get("classAnnotations");
            if (inClassAnnotations != null) {
                classAnnotations = parseAnnotations(inClassAnnotations, new int[1]);
            }
            String inRuntimeAnnotations = reader.attributes.get("runtimeAnnotations");
            if (inRuntimeAnnotations != null) {
                runtimeAnnotations = parseAnnotations(inRuntimeAnnotations, new int[1]);
            }
        }

        public abstract boolean read(LineBasedReader reader) throws IOException;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + (this.flags & flagsNormalization);
            hash = 89 * hash + (this.deprecated ? 1 : 0);
            hash = 89 * hash + Objects.hashCode(this.signature);
            hash = 89 * hash + listHashCode(this.classAnnotations);
            hash = 89 * hash + listHashCode(this.runtimeAnnotations);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FeatureDescription other = (FeatureDescription) obj;
            if ((this.flags & flagsNormalization) != (other.flags & flagsNormalization)) {
                return false;
            }
            if (this.deprecated != other.deprecated) {
                return false;
            }
            if (!Objects.equals(this.signature, other.signature)) {
                return false;
            }
            if (!listEquals(this.classAnnotations, other.classAnnotations)) {
                return false;
            }
            if (!listEquals(this.runtimeAnnotations, other.runtimeAnnotations)) {
                return false;
            }
            return true;
        }

    }

    public static class ModuleDescription {
        String name;
        List<ModuleHeaderDescription> header = new ArrayList<>();

        public void write(Appendable output, String baselineVersion,
                          String version) throws IOException {
            boolean inBaseline = false;
            boolean inVersion = false;
            for (ModuleHeaderDescription mhd : header) {
                if (baselineVersion != null &&
                    mhd.versions.contains(baselineVersion)) {
                    inBaseline = true;
                }
                if (mhd.versions.contains(version)) {
                    inVersion = true;
                }
            }
            if (!inVersion && !inBaseline)
                return ;
            if (!inVersion) {
                output.append("-module name " + name + "\n\n");
                return;
            }
            boolean hasChange = hasChange(header, version, baselineVersion);
            if (!hasChange)
                return;

            output.append("module name " + name + "\n");
            for (ModuleHeaderDescription header : header) {
                header.write(output, baselineVersion, version);
            }
            output.append("\n");
        }

        boolean hasChange(List<? extends FeatureDescription> hasChange,
                          String version, String baseline) {
            return hasChange.stream()
                            .map(fd -> fd.versions)
                            .anyMatch(versions -> checkChange(versions,
                                                              version,
                                                              baseline));
        }

        public void read(LineBasedReader reader, String baselineVersion,
                         String version) throws IOException {
            if (!"module".equals(reader.lineKey))
                return ;

            name = reader.attributes.get("name");

            reader.moveNext();

            OUTER: while (reader.hasNext()) {
                switch (reader.lineKey) {
                    case "header":
                        removeVersion(header, h -> true, version);
                        ModuleHeaderDescription mhd =
                                new ModuleHeaderDescription();
                        mhd.read(reader);
                        mhd.name = name;
                        mhd.versions = version;
                        header.add(mhd);
                        break;
                    case "class":
                    case "-class":
                    case "module":
                    case "-module":
                        break OUTER;
                    default:
                        throw new IllegalStateException(reader.lineKey);
                }
            }
        }
    }

    static class ModuleHeaderDescription extends HeaderDescription {
        String name;
        List<String> exports = new ArrayList<>();
        List<String> opens = new ArrayList<>();
        List<RequiresDescription> requires = new ArrayList<>();
        List<String> uses = new ArrayList<>();
        List<ProvidesDescription> provides = new ArrayList<>();
        Integer moduleResolution;
        String moduleTarget;

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 83 * hash + Objects.hashCode(this.name);
            hash = 83 * hash + Objects.hashCode(this.exports);
            hash = 83 * hash + Objects.hashCode(this.opens);
            hash = 83 * hash + Objects.hashCode(this.requires);
            hash = 83 * hash + Objects.hashCode(this.uses);
            hash = 83 * hash + Objects.hashCode(this.provides);
            hash = 83 * hash + Objects.hashCode(this.moduleResolution);
            hash = 83 * hash + Objects.hashCode(this.moduleTarget);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final ModuleHeaderDescription other =
                    (ModuleHeaderDescription) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!listEquals(this.exports, other.exports)) {
                return false;
            }
            if (!listEquals(this.opens, other.opens)) {
                return false;
            }
            if (!listEquals(this.requires, other.requires)) {
                return false;
            }
            if (!listEquals(this.uses, other.uses)) {
                return false;
            }
            if (!listEquals(this.provides, other.provides)) {
                return false;
            }
            if (!Objects.equals(this.moduleTarget, other.moduleTarget)) {
                return false;
            }
            if (!Objects.equals(this.moduleResolution,
                                other.moduleResolution)) {
                return false;
            }
            return true;
        }

        @Override
        public void write(Appendable output, String baselineVersion,
                          String version) throws IOException {
            if (!versions.contains(version) ||
                (baselineVersion != null && versions.contains(baselineVersion)
                 && versions.contains(version)))
                return ;
            output.append("header");
            if (exports != null && !exports.isEmpty())
                output.append(" exports " + serializeList(exports));
            if (opens != null && !opens.isEmpty())
                output.append(" opens " + serializeList(opens));
            if (requires != null && !requires.isEmpty()) {
                List<String> requiresList =
                        requires.stream()
                                .map(req -> req.serialize())
                                .collect(Collectors.toList());
                output.append(" requires " + serializeList(requiresList));
            }
            if (uses != null && !uses.isEmpty())
                output.append(" uses " + serializeList(uses));
            if (provides != null && !provides.isEmpty()) {
                List<String> providesList =
                        provides.stream()
                                .map(p -> p.serialize())
                                .collect(Collectors.toList());
                output.append(" provides " + serializeList(providesList));
            }
            if (moduleTarget != null)
                output.append(" target " + quote(moduleTarget, true));
            if (moduleResolution != null)
                output.append(" resolution " +
                              quote(Integer.toHexString(moduleResolution),
                                    true));
            writeAttributes(output);
            output.append("\n");
            writeInnerClasses(output, baselineVersion, version);
        }

        private static Map<String, String> splitAttributes(String data) {
            String[] parts = data.split(" ");

            Map<String, String> attributes = new HashMap<>();

            for (int i = 0; i < parts.length; i += 2) {
                attributes.put(parts[i], unquote(parts[i + 1]));
            }

            return attributes;
        }

        @Override
        public boolean read(LineBasedReader reader) throws IOException {
            if (!"header".equals(reader.lineKey))
                return false;

            exports = deserializeList(reader.attributes.get("exports"));
            opens = deserializeList(reader.attributes.get("opens"));
            List<String> requiresList =
                    deserializeList(reader.attributes.get("requires"));
            requires = requiresList.stream()
                                   .map(RequiresDescription::deserialize)
                                   .collect(Collectors.toList());
            uses = deserializeList(reader.attributes.get("uses"));
            List<String> providesList =
                    deserializeList(reader.attributes.get("provides"), false);
            provides = providesList.stream()
                                   .map(ProvidesDescription::deserialize)
                                   .collect(Collectors.toList());

            moduleTarget = reader.attributes.get("target");

            if (reader.attributes.containsKey("resolution")) {
                final String resolutionFlags =
                        reader.attributes.get("resolution");
                moduleResolution = Integer.parseInt(resolutionFlags, 16);
            }

            readAttributes(reader);
            reader.moveNext();
            readInnerClasses(reader);

            return true;
        }

        static class RequiresDescription {
            final String moduleName;
            final int flags;
            final Integer version;

            public RequiresDescription(String moduleName, int flags,
                                       Integer version) {
                this.moduleName = moduleName;
                this.flags = flags;
                this.version = version;
            }

            public String serialize() {
                String versionKeyValue = version != null
                        ? " version " + quote(String.valueOf(version), true)
                        : "";
                return "name " + quote(moduleName, true) +
                       " flags " + quote(Integer.toHexString(flags), true) +
                       versionKeyValue;
            }

            public static RequiresDescription deserialize(String data) {
                Map<String, String> attributes = splitAttributes(data);

                Integer ver = attributes.containsKey("version")
                        ? Integer.parseInt(attributes.get("version"))
                        : null;
                int flags = Integer.parseInt(attributes.get("flags"), 16);
                return new RequiresDescription(attributes.get("name"),
                                               flags,
                                               ver);
            }

            public static RequiresDescription create(ClassFile cf,
                                                     RequiresEntry req) {
                String mod = getModuleName(cf, req.requires_index);
                Integer ver = getVersion(cf, req.requires_version_index);
                return new RequiresDescription(mod,
                                               req.requires_flags,
                                               ver);
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 53 * hash + Objects.hashCode(this.moduleName);
                hash = 53 * hash + this.flags;
                hash = 53 * hash + Objects.hashCode(this.version);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final RequiresDescription other = (RequiresDescription) obj;
                if (this.flags != other.flags) {
                    return false;
                }
                if (!Objects.equals(this.moduleName, other.moduleName)) {
                    return false;
                }
                if (!Objects.equals(this.version, other.version)) {
                    return false;
                }
                return true;
            }

        }

        static class ProvidesDescription {
            final String interfaceName;
            final List<String> implNames;

            public ProvidesDescription(String interfaceName,
                                       List<String> implNames) {
                this.interfaceName = interfaceName;
                this.implNames = implNames;
            }

            public String serialize() {
                return "interface " + quote(interfaceName, true) +
                       " impls " + quote(serializeList(implNames), true, true);
            }

            public static ProvidesDescription deserialize(String data) {
                Map<String, String> attributes = splitAttributes(data);
                List<String> implsList =
                        deserializeList(attributes.get("impls"),
                                        false);
                return new ProvidesDescription(attributes.get("interface"),
                                               implsList);
            }

            public static ProvidesDescription create(ClassFile cf,
                                                     ProvidesEntry prov) {
                String api = getClassName(cf, prov.provides_index);
                List<String> impls =
                        Arrays.stream(prov.with_index)
                              .mapToObj(wi -> getClassName(cf, wi))
                              .collect(Collectors.toList());
                return new ProvidesDescription(api, impls);
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 53 * hash + Objects.hashCode(this.interfaceName);
                hash = 53 * hash + Objects.hashCode(this.implNames);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final ProvidesDescription other = (ProvidesDescription) obj;
                if (!Objects.equals(this.interfaceName, other.interfaceName)) {
                    return false;
                }
                if (!Objects.equals(this.implNames, other.implNames)) {
                    return false;
                }
                return true;
            }
        }
    }

    public static class ClassDescription {
        String name;
        List<ClassHeaderDescription> header = new ArrayList<>();
        List<MethodDescription> methods = new ArrayList<>();
        List<FieldDescription> fields = new ArrayList<>();

        public void write(Appendable output, String baselineVersion,
                          String version) throws IOException {
            boolean inBaseline = false;
            boolean inVersion = false;
            for (ClassHeaderDescription chd : header) {
                if (baselineVersion != null &&
                    chd.versions.contains(baselineVersion)) {
                    inBaseline = true;
                }
                if (chd.versions.contains(version)) {
                    inVersion = true;
                }
            }
            if (!inVersion && !inBaseline)
                return ;
            if (!inVersion) {
                output.append("-class name " + name + "\n\n");
                return;
            }
            boolean hasChange = hasChange(header, version, baselineVersion) ||
                                hasChange(fields, version, baselineVersion) ||
                                hasChange(methods, version, baselineVersion);
            if (!hasChange)
                return;

            output.append("class name " + name + "\n");
            for (ClassHeaderDescription header : header) {
                header.write(output, baselineVersion, version);
            }
            for (FieldDescription field : fields) {
                field.write(output, baselineVersion, version);
            }
            for (MethodDescription method : methods) {
                method.write(output, baselineVersion, version);
            }
            output.append("\n");
        }

        boolean hasChange(List<? extends FeatureDescription> hasChange,
                          String version,
                          String baseline) {
            return hasChange.stream()
                            .map(fd -> fd.versions)
                            .anyMatch(versions -> checkChange(versions,
                                                              version,
                                                              baseline));
        }

        public void read(LineBasedReader reader, String baselineVersion,
                         String version) throws IOException {
            if (!"class".equals(reader.lineKey))
                return ;

            name = reader.attributes.get("name");

            reader.moveNext();

            OUTER: while (reader.hasNext()) {
                switch (reader.lineKey) {
                    case "header":
                        removeVersion(header, h -> true, version);
                        ClassHeaderDescription chd = new ClassHeaderDescription();
                        chd.read(reader);
                        chd.versions = version;
                        header.add(chd);
                        break;
                    case "field":
                        FieldDescription field = new FieldDescription();
                        field.read(reader);
                        field.versions += version;
                        fields.add(field);
                        break;
                    case "-field": {
                        removeVersion(fields,
                                      f -> Objects.equals(f.name, reader.attributes.get("name")) &&
                                           Objects.equals(f.descriptor, reader.attributes.get("descriptor")),
                                      version);
                        reader.moveNext();
                        break;
                    }
                    case "method":
                        MethodDescription method = new MethodDescription();
                        method.read(reader);
                        method.versions += version;
                        methods.add(method);
                        break;
                    case "-method": {
                        removeVersion(methods,
                                      m -> Objects.equals(m.name, reader.attributes.get("name")) &&
                                           Objects.equals(m.descriptor, reader.attributes.get("descriptor")),
                                      version);
                        reader.moveNext();
                        break;
                    }
                    case "class":
                    case "-class":
                    case "module":
                    case "-module":
                        break OUTER;
                    default:
                        throw new IllegalStateException(reader.lineKey);
                }
            }
        }

        public String packge() {
            String pack;
            int lastSlash = name.lastIndexOf('/');
            if (lastSlash != (-1)) {
                pack = name.substring(0, lastSlash).replace('/', '.');
            } else {
                pack = "";
            }

            return pack;
        }
    }

    static class ClassHeaderDescription extends HeaderDescription {
        String extendsAttr;
        List<String> implementsAttr;
        String nestHost;
        List<String> nestMembers;
        boolean isRecord;
        List<RecordComponentDescription> recordComponents;
        boolean isSealed;
        List<String> permittedSubclasses;

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 17 * hash + Objects.hashCode(this.extendsAttr);
            hash = 17 * hash + Objects.hashCode(this.implementsAttr);
            hash = 17 * hash + Objects.hashCode(this.nestHost);
            hash = 17 * hash + Objects.hashCode(this.nestMembers);
            hash = 17 * hash + Objects.hashCode(this.isRecord);
            hash = 17 * hash + Objects.hashCode(this.recordComponents);
            hash = 17 * hash + Objects.hashCode(this.isSealed);
            hash = 17 * hash + Objects.hashCode(this.permittedSubclasses);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final ClassHeaderDescription other = (ClassHeaderDescription) obj;
            if (!Objects.equals(this.extendsAttr, other.extendsAttr)) {
                return false;
            }
            if (!Objects.equals(this.implementsAttr, other.implementsAttr)) {
                return false;
            }
            if (!Objects.equals(this.nestHost, other.nestHost)) {
                return false;
            }
            if (!listEquals(this.nestMembers, other.nestMembers)) {
                return false;
            }
            if (this.isRecord != other.isRecord) {
                return false;
            }
            if (!listEquals(this.recordComponents, other.recordComponents)) {
                return false;
            }
            if (this.isSealed != other.isSealed) {
                return false;
            }
            if (!listEquals(this.permittedSubclasses, other.permittedSubclasses)) {
                return false;
            }
            return true;
        }

        @Override
        public void write(Appendable output, String baselineVersion, String version) throws IOException {
            if (!versions.contains(version) ||
                (baselineVersion != null && versions.contains(baselineVersion) && versions.contains(version)))
                return ;
            output.append("header");
            if (extendsAttr != null)
                output.append(" extends " + extendsAttr);
            if (implementsAttr != null && !implementsAttr.isEmpty())
                output.append(" implements " + serializeList(implementsAttr));
            if (nestHost != null)
                output.append(" nestHost " + nestHost);
            if (nestMembers != null && !nestMembers.isEmpty())
                output.append(" nestMembers " + serializeList(nestMembers));
            if (isRecord) {
                output.append(" record true");
            }
            if (isSealed) {
                output.append(" sealed true");
            }
            writeAttributes(output);
            output.append("\n");
            writeRecordComponents(output, baselineVersion, version);
            writeInnerClasses(output, baselineVersion, version);
        }

        @Override
        public boolean read(LineBasedReader reader) throws IOException {
            if (!"header".equals(reader.lineKey))
                return false;

            extendsAttr = reader.attributes.get("extends");
            String elementsList = reader.attributes.get("implements");
            implementsAttr = deserializeList(elementsList);

            nestHost = reader.attributes.get("nestHost");
            String nestMembersList = reader.attributes.get("nestMembers");
            nestMembers = deserializeList(nestMembersList);
            isRecord = reader.attributes.containsKey("record");

            readAttributes(reader);
            reader.moveNext();
            if (isRecord) {
                readRecordComponents(reader);
            }
            readInnerClasses(reader);
            isSealed = reader.attributes.containsKey("permittedSubclasses");
            if (isSealed) {
                String subclassesList = reader.attributes.get("permittedSubclasses");
                permittedSubclasses = deserializeList(subclassesList);
            }

            return true;
        }

        protected void writeRecordComponents(Appendable output,
                                              String baselineVersion,
                                              String version) throws IOException {
            if (recordComponents != null) {
                for (RecordComponentDescription rcd : recordComponents) {
                    rcd.write(output, "", "");
                }
            }
        }

        protected void readRecordComponents(LineBasedReader reader) throws IOException {
            recordComponents = new ArrayList<>();

            while ("recordcomponent".equals(reader.lineKey)) {
                RecordComponentDescription rcd = new RecordComponentDescription();
                rcd.read(reader);
                recordComponents.add(rcd);
            }
        }
    }

    static abstract class HeaderDescription extends FeatureDescription {
        List<InnerClassInfo> innerClasses;

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 19 * hash + Objects.hashCode(this.innerClasses);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final HeaderDescription other = (HeaderDescription) obj;
            if (!listEquals(this.innerClasses, other.innerClasses)) {
                return false;
            }
            return true;
        }

        protected void writeInnerClasses(Appendable output,
                                         String baselineVersion,
                                         String version) throws IOException {
            if (innerClasses != null && !innerClasses.isEmpty()) {
                for (InnerClassInfo ici : innerClasses) {
                    output.append("innerclass");
                    output.append(" innerClass " + ici.innerClass);
                    output.append(" outerClass " + ici.outerClass);
                    output.append(" innerClassName " + ici.innerClassName);
                    output.append(" flags " + Integer.toHexString(ici.innerClassFlags));
                    output.append("\n");
                }
            }
        }

        protected void readInnerClasses(LineBasedReader reader) throws IOException {
            innerClasses = new ArrayList<>();

            while ("innerclass".equals(reader.lineKey)) {
                InnerClassInfo info = new InnerClassInfo();

                info.innerClass = reader.attributes.get("innerClass");
                info.outerClass = reader.attributes.get("outerClass");
                info.innerClassName = reader.attributes.get("innerClassName");

                String inFlags = reader.attributes.get("flags");
                if (inFlags != null && !inFlags.isEmpty())
                    info.innerClassFlags = Integer.parseInt(inFlags, 16);

                innerClasses.add(info);

                reader.moveNext();
            }
        }

    }

    static class MethodDescription extends FeatureDescription {
        static int METHODS_FLAGS_NORMALIZATION = ~0;
        String name;
        String descriptor;
        List<String> thrownTypes;
        Object annotationDefaultValue;
        List<List<AnnotationDescription>> classParameterAnnotations;
        List<List<AnnotationDescription>> runtimeParameterAnnotations;
        List<MethodParam> methodParameters;

        public MethodDescription() {
            flagsNormalization = METHODS_FLAGS_NORMALIZATION;
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 59 * hash + Objects.hashCode(this.name);
            hash = 59 * hash + Objects.hashCode(this.descriptor);
            hash = 59 * hash + Objects.hashCode(this.thrownTypes);
            hash = 59 * hash + Objects.hashCode(this.annotationDefaultValue);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final MethodDescription other = (MethodDescription) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.descriptor, other.descriptor)) {
                return false;
            }
            if (!Objects.equals(this.thrownTypes, other.thrownTypes)) {
                return false;
            }
            if (!Objects.equals(this.annotationDefaultValue, other.annotationDefaultValue)) {
                return false;
            }
            return true;
        }

        @Override
        public void write(Appendable output, String baselineVersion, String version) throws IOException {
            if (shouldIgnore(baselineVersion, version))
                return ;
            if (!versions.contains(version)) {
                output.append("-method");
                output.append(" name " + quote(name, false));
                output.append(" descriptor " + quote(descriptor, false));
                output.append("\n");
                return ;
            }
            output.append("method");
            output.append(" name " + quote(name, false));
            output.append(" descriptor " + quote(descriptor, false));
            if (thrownTypes != null)
                output.append(" thrownTypes " + serializeList(thrownTypes));
            if (annotationDefaultValue != null)
                output.append(" annotationDefaultValue " + quote(AnnotationDescription.dumpAnnotationValue(annotationDefaultValue), false));
            writeAttributes(output);
            if (classParameterAnnotations != null && !classParameterAnnotations.isEmpty()) {
                output.append(" classParameterAnnotations ");
                for (List<AnnotationDescription> pa : classParameterAnnotations) {
                    for (AnnotationDescription a : pa) {
                        output.append(quote(a.toString(), false));
                    }
                    output.append(";");
                }
            }
            if (runtimeParameterAnnotations != null && !runtimeParameterAnnotations.isEmpty()) {
                output.append(" runtimeParameterAnnotations ");
                for (List<AnnotationDescription> pa : runtimeParameterAnnotations) {
                    for (AnnotationDescription a : pa) {
                        output.append(quote(a.toString(), false));
                    }
                    output.append(";");
                }
            }
            if (methodParameters != null && !methodParameters.isEmpty()) {
                Function<MethodParam, String> param2String =
                        p -> Integer.toHexString(p.flags) + ":" + p.name;
                List<String> paramsAsStrings =
                        methodParameters.stream()
                                         .map(param2String)
                                         .collect(Collectors.toList());
                output.append(" methodParameters " + serializeList(paramsAsStrings));
            }
            output.append("\n");
        }

        @Override
        public boolean read(LineBasedReader reader) throws IOException {
            if (!"method".equals(reader.lineKey))
                return false;

            name = reader.attributes.get("name");
            descriptor = reader.attributes.get("descriptor");

            String thrownTypesValue = reader.attributes.get("thrownTypes");

            if (thrownTypesValue != null) {
                thrownTypes = deserializeList(thrownTypesValue);
            }

            String inAnnotationDefaultValue = reader.attributes.get("annotationDefaultValue");

            if (inAnnotationDefaultValue != null) {
                annotationDefaultValue = parseAnnotationValue(inAnnotationDefaultValue, new int[1]);
            }

            readAttributes(reader);

            String inClassParamAnnotations = reader.attributes.get("classParameterAnnotations");
            if (inClassParamAnnotations != null) {
                List<List<AnnotationDescription>> annos = new ArrayList<>();
                int[] pointer = new int[1];
                do {
                    annos.add(parseAnnotations(inClassParamAnnotations, pointer));
                    assert pointer[0] == inClassParamAnnotations.length() || inClassParamAnnotations.charAt(pointer[0]) == ';';
                } while (++pointer[0] < inClassParamAnnotations.length());
                classParameterAnnotations = annos;
            }

            String inRuntimeParamAnnotations = reader.attributes.get("runtimeParameterAnnotations");
            if (inRuntimeParamAnnotations != null) {
                List<List<AnnotationDescription>> annos = new ArrayList<>();
                int[] pointer = new int[1];
                do {
                    annos.add(parseAnnotations(inRuntimeParamAnnotations, pointer));
                    assert pointer[0] == inRuntimeParamAnnotations.length() || inRuntimeParamAnnotations.charAt(pointer[0]) == ';';
                } while (++pointer[0] < inRuntimeParamAnnotations.length());
                runtimeParameterAnnotations = annos;
            }

            String inMethodParameters = reader.attributes.get("methodParameters");
            if (inMethodParameters != null) {
                Function<String, MethodParam> string2Param =
                        p -> {
                            int sep = p.indexOf(':');
                            return new MethodParam(Integer.parseInt(p.substring(0, sep)),
                                                    p.substring(sep + 1));
                        };
                methodParameters =
                        deserializeList(inMethodParameters).stream()
                                                          .map(string2Param)
                                                          .collect(Collectors.toList());
            }

            reader.moveNext();

            return true;
        }

        public static class MethodParam {
            public final int flags;
            public final String name;

            public MethodParam(int flags, String name) {
                this.flags = flags;
                this.name = name;
            }
        }
    }

    static class FieldDescription extends FeatureDescription {
        String name;
        String descriptor;
        Object constantValue;
        String keyName = "field";

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 59 * hash + Objects.hashCode(this.name);
            hash = 59 * hash + Objects.hashCode(this.descriptor);
            hash = 59 * hash + Objects.hashCode(this.constantValue);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final FieldDescription other = (FieldDescription) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.descriptor, other.descriptor)) {
                return false;
            }
            if (!Objects.equals(this.constantValue, other.constantValue)) {
                return false;
            }
            return true;
        }

        @Override
        public void write(Appendable output, String baselineVersion, String version) throws IOException {
            if (shouldIgnore(baselineVersion, version))
                return ;
            if (!versions.contains(version)) {
                output.append("-" + keyName);
                output.append(" name " + quote(name, false));
                output.append(" descriptor " + quote(descriptor, false));
                output.append("\n");
                return ;
            }
            output.append(keyName);
            output.append(" name " + name);
            output.append(" descriptor " + descriptor);
            if (constantValue != null) {
                output.append(" constantValue " + quote(constantValue.toString(), false));
            }
            writeAttributes(output);
            output.append("\n");
        }

        @Override
        public boolean read(LineBasedReader reader) throws IOException {
            if (!keyName.equals(reader.lineKey))
                return false;

            name = reader.attributes.get("name");
            descriptor = reader.attributes.get("descriptor");

            String inConstantValue = reader.attributes.get("constantValue");

            if (inConstantValue != null) {
                switch (descriptor) {
                    case "Z": constantValue = "true".equals(inConstantValue); break;
                    case "B": constantValue = Integer.parseInt(inConstantValue); break;
                    case "C": constantValue = inConstantValue.charAt(0); break;
                    case "S": constantValue = Integer.parseInt(inConstantValue); break;
                    case "I": constantValue = Integer.parseInt(inConstantValue); break;
                    case "J": constantValue = Long.parseLong(inConstantValue); break;
                    case "F": constantValue = Float.parseFloat(inConstantValue); break;
                    case "D": constantValue = Double.parseDouble(inConstantValue); break;
                    case "Ljava/lang/String;": constantValue = inConstantValue; break;
                    default:
                        throw new IllegalStateException("Unrecognized field type: " + descriptor);
                }
            }

            readAttributes(reader);

            reader.moveNext();

            return true;
        }

    }

    static final class RecordComponentDescription extends FieldDescription {

        public RecordComponentDescription() {
            this.keyName = "recordcomponent";
        }

        @Override
        protected boolean shouldIgnore(String baselineVersion, String version) {
            return false;
        }

    }

    static final class AnnotationDescription {
        String annotationType;
        Map<String, Object> values;

        public AnnotationDescription(String annotationType, Map<String, Object> values) {
            this.annotationType = annotationType;
            this.values = values;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + Objects.hashCode(this.annotationType);
            hash = 47 * hash + Objects.hashCode(this.values);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AnnotationDescription other = (AnnotationDescription) obj;
            if (!Objects.equals(this.annotationType, other.annotationType)) {
                return false;
            }
            if (!Objects.equals(this.values, other.values)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("@" + annotationType);
            if (!values.isEmpty()) {
                result.append("(");
                boolean first = true;
                for (Entry<String, Object> e : values.entrySet()) {
                    if (!first) {
                        result.append(",");
                    }
                    first = false;
                    result.append(e.getKey());
                    result.append("=");
                    result.append(dumpAnnotationValue(e.getValue()));
                    result.append("");
                }
                result.append(")");
            }
            return result.toString();
        }

        private static String dumpAnnotationValue(Object value) {
            if (value instanceof List) {
                StringBuilder result = new StringBuilder();

                result.append("{");

                for (Object element : ((List) value)) {
                    result.append(dumpAnnotationValue(element));
                }

                result.append("}");

                return result.toString();
            }

            if (value instanceof String) {
                return "\"" + quote((String) value, true) + "\"";
            } else if (value instanceof Boolean) {
                return "Z" + value;
            } else if (value instanceof Byte) {
                return "B" + value;
            } if (value instanceof Character) {
                return "C" + value;
            } if (value instanceof Short) {
                return "S" + value;
            } if (value instanceof Integer) {
                return "I" + value;
            } if (value instanceof Long) {
                return "J" + value;
            } if (value instanceof Float) {
                return "F" + value;
            } if (value instanceof Double) {
                return "D" + value;
            } else {
                return value.toString();
            }
        }
    }

    static final class EnumConstant {
        String type;
        String constant;

        public EnumConstant(String type, String constant) {
            this.type = type;
            this.constant = constant;
        }

        @Override
        public String toString() {
            return "e" + type + constant + ";";
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 19 * hash + Objects.hashCode(this.type);
            hash = 19 * hash + Objects.hashCode(this.constant);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EnumConstant other = (EnumConstant) obj;
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            if (!Objects.equals(this.constant, other.constant)) {
                return false;
            }
            return true;
        }

    }

    static final class ClassConstant {
        String type;

        public ClassConstant(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "c" + type;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ClassConstant other = (ClassConstant) obj;
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            return true;
        }

    }

    static final class InnerClassInfo {
        String innerClass;
        String outerClass;
        String innerClassName;
        int    innerClassFlags;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + Objects.hashCode(this.innerClass);
            hash = 11 * hash + Objects.hashCode(this.outerClass);
            hash = 11 * hash + Objects.hashCode(this.innerClassName);
            hash = 11 * hash + Objects.hashCode(this.innerClassFlags);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final InnerClassInfo other = (InnerClassInfo) obj;
            if (!Objects.equals(this.innerClass, other.innerClass)) {
                return false;
            }
            if (!Objects.equals(this.outerClass, other.outerClass)) {
                return false;
            }
            if (!Objects.equals(this.innerClassName, other.innerClassName)) {
                return false;
            }
            if (!Objects.equals(this.innerClassFlags, other.innerClassFlags)) {
                return false;
            }
            return true;
        }

    }

    public static final class ClassList implements Iterable<ClassDescription> {
        private final List<ClassDescription> classes = new ArrayList<>();
        private final Map<String, ClassDescription> name2Class = new HashMap<>();
        private final Map<ClassDescription, ClassDescription> inner2Outter = new HashMap<>();

        @Override
        public Iterator<ClassDescription> iterator() {
            return classes.iterator();
        }

        public void add(ClassDescription desc) {
            classes.add(desc);
            name2Class.put(desc.name, desc);
        }

        public ClassDescription find(String name) {
            return find(name, ALLOW_NON_EXISTING_CLASSES);
        }

        public ClassDescription find(String name, boolean allowNull) {
            ClassDescription desc = name2Class.get(name);

            if (desc != null || allowNull)
                return desc;

            throw new IllegalStateException("Cannot find: " + name);
        }

        private static final ClassDescription NONE = new ClassDescription();

        public ClassDescription enclosingClass(ClassDescription clazz) {
            if (clazz == null)
                return null;
            ClassDescription desc = inner2Outter.computeIfAbsent(clazz, c -> {
                ClassHeaderDescription header = clazz.header.get(0);

                if (header.innerClasses != null) {
                    for (InnerClassInfo ici : header.innerClasses) {
                        if (ici.innerClass.equals(clazz.name)) {
                            return find(ici.outerClass);
                        }
                    }
                }

                return NONE;
            });

            return desc != NONE ? desc : null;
        }

        public Iterable<ClassDescription> enclosingClasses(ClassDescription clazz) {
            List<ClassDescription> result = new ArrayList<>();
            ClassDescription outer = enclosingClass(clazz);

            while (outer != null) {
                result.add(outer);
                outer = enclosingClass(outer);
            }

            return result;
        }

        public void sort() {
            Collections.sort(classes, (cd1, cd2) -> cd1.name.compareTo(cd2.name));
        }
    }

    private static int listHashCode(Collection<?> c) {
        return c == null || c.isEmpty() ? 0 : c.hashCode();
    }

    private static boolean listEquals(Collection<?> c1, Collection<?> c2) {
        if (c1 == c2) return true;
        if (c1 == null && c2.isEmpty()) return true;
        if (c2 == null && c1.isEmpty()) return true;
        return Objects.equals(c1, c2);
    }

    private static String serializeList(List<String> list) {
        StringBuilder result = new StringBuilder();
        String sep = "";

        for (Object o : list) {
            result.append(sep);
            result.append(o);
            sep = ",";
        }

        return quote(result.toString(), false);
    }

    private static List<String> deserializeList(String serialized) {
        return deserializeList(serialized, true);
    }

    private static List<String> deserializeList(String serialized,
                                                boolean unquote) {
        serialized = unquote ? unquote(serialized) : serialized;
        if (serialized == null)
            return new ArrayList<>();
        return new ArrayList<>(List.of(serialized.split(",")));
    }

    private static String quote(String value, boolean quoteQuotes) {
        return quote(value, quoteQuotes, false);
    }

    private static String quote(String value, boolean quoteQuotes,
                                boolean quoteCommas) {
        StringBuilder result = new StringBuilder();

        for (char c : value.toCharArray()) {
            if (c <= 32 || c >= 127 || c == '\\' ||
                (quoteQuotes && c == '"') || (quoteCommas && c == ',')) {
                result.append("\\u" + String.format("%04X", (int) c) + ";");
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static final Pattern unicodePattern =
            Pattern.compile("\\\\u([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])");

    private static String unquote(String value) {
        if (value == null)
            return null;

        StringBuilder result = new StringBuilder();
        Matcher m = unicodePattern.matcher(value);
        int lastStart = 0;

        while (m.find(lastStart)) {
            result.append(value.substring(lastStart, m.start()));
            result.append((char) Integer.parseInt(m.group(1), 16));
            lastStart = m.end() + 1;
        }

        result.append(value.substring(lastStart, value.length()));

        return result.toString();
    }

    private static String readDigits(String value, int[] valuePointer) {
        int start = valuePointer[0];

        if (value.charAt(valuePointer[0]) == '-')
            valuePointer[0]++;

        while (valuePointer[0] < value.length() && Character.isDigit(value.charAt(valuePointer[0])))
            valuePointer[0]++;

        return value.substring(start, valuePointer[0]);
    }

    private static String className(String value, int[] valuePointer) {
        int start = valuePointer[0];
        while (value.charAt(valuePointer[0]++) != ';')
            ;
        return value.substring(start, valuePointer[0]);
    }

    private static Object parseAnnotationValue(String value, int[] valuePointer) {
        switch (value.charAt(valuePointer[0]++)) {
            case 'Z':
                if ("true".equals(value.substring(valuePointer[0], valuePointer[0] + 4))) {
                    valuePointer[0] += 4;
                    return true;
                } else if ("false".equals(value.substring(valuePointer[0], valuePointer[0] + 5))) {
                    valuePointer[0] += 5;
                    return false;
                } else {
                    throw new IllegalStateException("Unrecognized boolean structure: " + value);
                }
            case 'B': return Byte.parseByte(readDigits(value, valuePointer));
            case 'C': return value.charAt(valuePointer[0]++);
            case 'S': return Short.parseShort(readDigits(value, valuePointer));
            case 'I': return Integer.parseInt(readDigits(value, valuePointer));
            case 'J': return Long.parseLong(readDigits(value, valuePointer));
            case 'F': return Float.parseFloat(readDigits(value, valuePointer));
            case 'D': return Double.parseDouble(readDigits(value, valuePointer));
            case 'c':
                return new ClassConstant(className(value, valuePointer));
            case 'e':
                return new EnumConstant(className(value, valuePointer), className(value, valuePointer).replaceFirst(";$", ""));
            case '{':
                List<Object> elements = new ArrayList<>(); //TODO: a good test for this would be highly desirable
                while (value.charAt(valuePointer[0]) != '}') {
                    elements.add(parseAnnotationValue(value, valuePointer));
                }
                valuePointer[0]++;
                return elements;
            case '"':
                int start = valuePointer[0];
                while (value.charAt(valuePointer[0]) != '"')
                    valuePointer[0]++;
                return unquote(value.substring(start, valuePointer[0]++));
            case '@':
                return parseAnnotation(value, valuePointer);
            default:
                throw new IllegalStateException("Unrecognized signature type: " + value.charAt(valuePointer[0] - 1) + "; value=" + value);
        }
    }

    public static List<AnnotationDescription> parseAnnotations(String encoded, int[] pointer) {
        ArrayList<AnnotationDescription> result = new ArrayList<>();

        while (pointer[0] < encoded.length() && encoded.charAt(pointer[0]) == '@') {
            pointer[0]++;
            result.add(parseAnnotation(encoded, pointer));
        }

        return result;
    }

    private static AnnotationDescription parseAnnotation(String value, int[] valuePointer) {
        String className = className(value, valuePointer);
        Map<String, Object> attribute2Value = new HashMap<>();

        if (valuePointer[0] < value.length() && value.charAt(valuePointer[0]) == '(') {
            while (value.charAt(valuePointer[0]) != ')') {
                int nameStart = ++valuePointer[0];

                while (value.charAt(valuePointer[0]++) != '=');

                String name = value.substring(nameStart, valuePointer[0] - 1);

                attribute2Value.put(name, parseAnnotationValue(value, valuePointer));
            }

            valuePointer[0]++;
        }

        return new AnnotationDescription(className, attribute2Value);
    }
    //</editor-fold>

    /**Create sig files for ct.sym reading the classes description from the directory that contains
     * {@code ctDescriptionFile}, using the file as a recipe to create the sigfiles.
     */
    @SuppressWarnings("unchecked")
    public void createJavadocData(String ctDescriptionFileExtra, String ctDescriptionFile,
                                  String targetDir, int startVersion) throws IOException {
        LoadDescriptions data = load(ctDescriptionFileExtra != null ? Paths.get(ctDescriptionFileExtra)
                                                                    : null,
                                     Paths.get(ctDescriptionFile));

        Path target = Paths.get(targetDir);

        for (PlatformInput version : data.versions) {
            int versionNumber = Integer.parseInt(version.version, Character.MAX_RADIX);
            if (versionNumber < startVersion) {
                continue;
            }
            Path outputFile = target.resolve("element-list-" + versionNumber + ".txt");
            Files.createDirectories(outputFile.getParent());
            try (Writer w = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                Set<ModuleDescription> modules = new TreeSet<>((m1, m2) -> m1.name.compareTo(m2.name));
                modules.addAll(data.modules.values());
                for (ModuleDescription module : modules) {
                    if ("jdk.unsupported".equals(module.name)) {
                        continue;
                    }
                    Optional<ModuleHeaderDescription> header = module.header.stream().filter(h -> h.versions.contains(version.version)).findAny();
                    if (header.isEmpty()) {
                        continue;
                    }
                    w.write("module:" + module.name);
                    w.write("\n");
                    for (String pack : header.get().exports) {
                        w.write(pack.replace('/', '.'));
                        w.write("\n");
                    }
                }
            }
        }
    }

    private static void help() {
        System.err.println("Help...");
    }

    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            help();
            return ;
        }

        switch (args[0]) {
            case "build-description": {
                if (args.length < 3) {
                    help();
                    return ;
                }

                Path descDest = Paths.get(args[1]);
                List<VersionDescription> versions = new ArrayList<>();

                for (int i = 3; i + 2 < args.length; i += 3) {
                    versions.add(new VersionDescription(args[i + 1], args[i], args[i + 2]));
                }

                Files.walkFileTree(descDest, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });

                ExcludeIncludeList excludeList =
                        ExcludeIncludeList.create(args[2]);

                new CreateSymbols().createBaseLine(versions,
                                                   excludeList,
                                                   descDest,
                                                   args);
                break;
            }
            case "build-description-incremental-file": {
                if (args.length != 6 && args.length != 7) {
                    help();
                    return ;
                }

                if (args.length == 7) {
                    if ("--normalize-method-flags".equals(args[6])) {
                        MethodDescription.METHODS_FLAGS_NORMALIZATION = ~(0x100 | 0x20);
                    } else {
                        help();
                        return ;
                    }
                }

                new CreateSymbols().createIncrementalBaseLineFromDataFile(args[1], args[2], args[3], args[4], "<none>".equals(args[5]) ? null : args[5], args);
                break;
            }
            case "build-description-incremental": {
                if (args.length != 3) {
                    help();
                    return ;
                }

                new CreateSymbols().createIncrementalBaseLine(args[1], args[2], args);
                break;
            }
            case "build-ctsym": {
                String ctDescriptionFileExtra;
                String ctDescriptionFile;
                String ctSymLocation;
                String timestampSpec;
                String currentVersion;
                String systemModules;

                if (args.length == 6) {
                    ctDescriptionFileExtra = null;
                    ctDescriptionFile = args[1];
                    ctSymLocation = args[2];
                    timestampSpec = args[3];
                    currentVersion = args[4];
                    systemModules = args[5];
                } else if (args.length == 7) {
                    ctDescriptionFileExtra = args[1];
                    ctDescriptionFile = args[2];
                    ctSymLocation = args[3];
                    timestampSpec = args[4];
                    currentVersion = args[5];
                    systemModules = args[6];
                } else {
                    help();
                    return ;
                }

                long timestamp = Long.parseLong(timestampSpec);

                //SOURCE_DATE_EPOCH is in seconds, convert to milliseconds:
                timestamp *= 1000;

                new CreateSymbols().createSymbols(ctDescriptionFileExtra,
                                                  ctDescriptionFile,
                                                  ctSymLocation,
                                                  timestamp,
                                                  currentVersion,
                                                  systemModules);
                break;
            }
            case "build-javadoc-data": {
                String ctDescriptionFileExtra;
                String ctDescriptionFile;
                String targetDir;
                int startVersion;

                if (args.length == 4) {
                    ctDescriptionFileExtra = null;
                    ctDescriptionFile = args[1];
                    targetDir = args[2];
                    startVersion = Integer.parseInt(args[3]);
                } else if (args.length == 5) {
                    ctDescriptionFileExtra = args[1];
                    ctDescriptionFile = args[2];
                    targetDir = args[3];
                    startVersion = Integer.parseInt(args[4]);
                } else {
                    help();
                    return ;
                }

                if (startVersion < 9) {
                    System.err.println("The start version must be at least 9!");
                    return ;
                }

                new CreateSymbols().createJavadocData(ctDescriptionFileExtra,
                                                      ctDescriptionFile,
                                                      targetDir,
                                                      startVersion);
                break;
            }
        }
    }

}
