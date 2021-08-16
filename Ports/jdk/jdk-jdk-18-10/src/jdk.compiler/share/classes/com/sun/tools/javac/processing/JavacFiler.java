/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.processing;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FilterWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Element;
import javax.tools.*;
import javax.tools.JavaFileManager.Location;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;

import static com.sun.tools.javac.code.Lint.LintCategory.PROCESSING;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.main.Option;

/**
 * The FilerImplementation class must maintain a number of
 * constraints.  First, multiple attempts to open the same path within
 * the same invocation of the tool results in an IOException being
 * thrown.  For example, trying to open the same source file twice:
 *
 * <pre>
 * createSourceFile("foo.Bar")
 * ...
 * createSourceFile("foo.Bar")
 * </pre>
 *
 * is disallowed as is opening a text file that happens to have
 * the same name as a source file:
 *
 * <pre>
 * createSourceFile("foo.Bar")
 * ...
 * createTextFile(SOURCE_TREE, "foo", new File("Bar"), null)
 * </pre>
 *
 * <p>Additionally, creating a source file that corresponds to an
 * already created class file (or vice versa) also results in an
 * IOException since each type can only be created once.  However, if
 * the Filer is used to create a text file named *.java that happens
 * to correspond to an existing class file, a warning is *not*
 * generated.  Similarly, a warning is not generated for a binary file
 * named *.class and an existing source file.
 *
 * <p>The reason for this difference is that source files and class
 * files are registered with the tool and can get passed on as
 * declarations to the next round of processing.  Files that are just
 * named *.java and *.class are not processed in that manner; although
 * having extra source files and class files on the source path and
 * class path can alter the behavior of the tool and any final
 * compile.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class JavacFiler implements Filer, Closeable {
    // TODO: Implement different transaction model for updating the
    // Filer's record keeping on file close.

    private static final String ALREADY_OPENED =
        "Output stream or writer has already been opened.";
    private static final String NOT_FOR_READING =
        "FileObject was not opened for reading.";
    private static final String NOT_FOR_WRITING =
        "FileObject was not opened for writing.";

    /**
     * Wrap a JavaFileObject to manage writing by the Filer.
     */
    private class FilerOutputFileObject extends ForwardingFileObject<FileObject> {
        private boolean opened = false;
        private ModuleSymbol mod;
        private String name;

        FilerOutputFileObject(ModuleSymbol mod, String name, FileObject fileObject) {
            super(fileObject);
            this.mod = mod;
            this.name = name;
        }

        @Override @DefinedBy(Api.COMPILER)
        public synchronized OutputStream openOutputStream() throws IOException {
            if (opened)
                throw new IOException(ALREADY_OPENED);
            opened = true;
            return new FilerOutputStream(mod, name, fileObject);
        }

        @Override @DefinedBy(Api.COMPILER)
        public synchronized Writer openWriter() throws IOException {
            if (opened)
                throw new IOException(ALREADY_OPENED);
            opened = true;
            return new FilerWriter(mod, name, fileObject);
        }

        // Three anti-literacy methods
        @Override @DefinedBy(Api.COMPILER)
        public InputStream openInputStream() throws IOException {
            throw new IllegalStateException(NOT_FOR_READING);
        }

        @Override @DefinedBy(Api.COMPILER)
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            throw new IllegalStateException(NOT_FOR_READING);
        }

        @Override @DefinedBy(Api.COMPILER)
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            throw new IllegalStateException(NOT_FOR_READING);
        }

        @Override @DefinedBy(Api.COMPILER)
        public boolean delete() {
            return false;
        }
    }

    private class FilerOutputJavaFileObject extends FilerOutputFileObject implements JavaFileObject {
        private final JavaFileObject javaFileObject;
        FilerOutputJavaFileObject(ModuleSymbol mod, String name, JavaFileObject javaFileObject) {
            super(mod, name, javaFileObject);
            this.javaFileObject = javaFileObject;
        }

        @DefinedBy(Api.COMPILER)
        public JavaFileObject.Kind getKind() {
            return javaFileObject.getKind();
        }

        @DefinedBy(Api.COMPILER)
        public boolean isNameCompatible(String simpleName,
                                        JavaFileObject.Kind kind) {
            return javaFileObject.isNameCompatible(simpleName, kind);
        }

        @DefinedBy(Api.COMPILER)
        public NestingKind getNestingKind() {
            return javaFileObject.getNestingKind();
        }

        @DefinedBy(Api.COMPILER)
        public Modifier getAccessLevel() {
            return javaFileObject.getAccessLevel();
        }
    }

    /**
     * Wrap a JavaFileObject to manage reading by the Filer.
     */
    private class FilerInputFileObject extends ForwardingFileObject<FileObject> {
        FilerInputFileObject(FileObject fileObject) {
            super(fileObject);
        }

        @Override @DefinedBy(Api.COMPILER)
        public OutputStream openOutputStream() throws IOException {
            throw new IllegalStateException(NOT_FOR_WRITING);
        }

        @Override @DefinedBy(Api.COMPILER)
        public Writer openWriter() throws IOException {
            throw new IllegalStateException(NOT_FOR_WRITING);
        }

        @Override @DefinedBy(Api.COMPILER)
        public boolean delete() {
            return false;
        }
    }

    private class FilerInputJavaFileObject extends FilerInputFileObject implements JavaFileObject {
        private final JavaFileObject javaFileObject;
        FilerInputJavaFileObject(JavaFileObject javaFileObject) {
            super(javaFileObject);
            this.javaFileObject = javaFileObject;
        }

        @DefinedBy(Api.COMPILER)
        public JavaFileObject.Kind getKind() {
            return javaFileObject.getKind();
        }

        @DefinedBy(Api.COMPILER)
        public boolean isNameCompatible(String simpleName,
                                        JavaFileObject.Kind kind) {
            return javaFileObject.isNameCompatible(simpleName, kind);
        }

        @DefinedBy(Api.COMPILER)
        public NestingKind getNestingKind() {
            return javaFileObject.getNestingKind();
        }

        @DefinedBy(Api.COMPILER)
        public Modifier getAccessLevel() {
            return javaFileObject.getAccessLevel();
        }
    }


    /**
     * Wrap a {@code OutputStream} returned from the {@code
     * JavaFileManager} to properly register source or class files
     * when they are closed.
     */
    private class FilerOutputStream extends FilterOutputStream {
        ModuleSymbol mod;
        String typeName;
        FileObject fileObject;
        boolean closed = false;

        /**
         * @param typeName name of class or {@code null} if just a
         * binary file
         */
        FilerOutputStream(ModuleSymbol mod, String typeName, FileObject fileObject) throws IOException {
            super(fileObject.openOutputStream());
            this.mod = mod;
            this.typeName = typeName;
            this.fileObject = fileObject;
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            out.write(b, off, len);
        }

        @Override
        public synchronized void close() throws IOException {
            if (!closed) {
                closed = true;
                /*
                 * If an IOException occurs when closing the underlying
                 * stream, still try to process the file.
                 */

                closeFileObject(mod, typeName, fileObject);
                out.close();
            }
        }
    }

    /**
     * Wrap a {@code Writer} returned from the {@code JavaFileManager}
     * to properly register source or class files when they are
     * closed.
     */
    private class FilerWriter extends FilterWriter {
        ModuleSymbol mod;
        String typeName;
        FileObject fileObject;
        boolean closed = false;

        /**
         * @param fileObject the fileObject to be written to
         * @param typeName name of source file or {@code null} if just a
         * text file
         */
        FilerWriter(ModuleSymbol mod, String typeName, FileObject fileObject) throws IOException {
            super(fileObject.openWriter());
            this.mod = mod;
            this.typeName = typeName;
            this.fileObject = fileObject;
        }

        @Override
        public synchronized void close() throws IOException {
            if (!closed) {
                closed = true;
                /*
                 * If an IOException occurs when closing the underlying
                 * Writer, still try to process the file.
                 */

                closeFileObject(mod, typeName, fileObject);
                out.close();
            }
        }
    }

    JavaFileManager fileManager;
    JavacElements elementUtils;
    Log log;
    Modules modules;
    Names names;
    Symtab syms;
    Context context;
    boolean lastRound;

    private final boolean lint;

    /**
     * Initial inputs passed to the tool.  This set must be
     * synchronized.
     */
    private final Set<FileObject> initialInputs;

    /**
     * Logical names of all created files.  This set must be
     * synchronized.
     */
    private final Set<FileObject> fileObjectHistory;

    /**
     * Names of types that have had files created but not closed.
     */
    private final Set<String> openTypeNames;

    /**
     * Names of source files closed in this round.  This set must be
     * synchronized.  Its iterators should preserve insertion order.
     */
    private Set<String> generatedSourceNames;

    /**
     * Names and class files of the class files closed in this round.
     * This set must be synchronized.  Its iterators should preserve
     * insertion order.
     */
    private final Map<ModuleSymbol, Map<String, JavaFileObject>> generatedClasses;

    /**
     * JavaFileObjects for source files closed in this round.  This
     * set must be synchronized.  Its iterators should preserve
     * insertion order.
     */
    private Set<JavaFileObject> generatedSourceFileObjects;

    /**
     * Names of all created source files.  Its iterators should
     * preserve insertion order.
     */
    private final Set<Pair<ModuleSymbol, String>> aggregateGeneratedSourceNames;

    /**
     * Names of all created class files.  Its iterators should
     * preserve insertion order.
     */
    private final Set<Pair<ModuleSymbol, String>> aggregateGeneratedClassNames;

    private final Set<String> initialClassNames;

    private final String defaultTargetModule;

    JavacFiler(Context context) {
        this.context = context;
        fileManager = context.get(JavaFileManager.class);
        elementUtils = JavacElements.instance(context);

        log = Log.instance(context);
        modules = Modules.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);

        initialInputs = synchronizedSet(new LinkedHashSet<>());
        fileObjectHistory = synchronizedSet(new LinkedHashSet<>());
        generatedSourceNames = synchronizedSet(new LinkedHashSet<>());
        generatedSourceFileObjects = synchronizedSet(new LinkedHashSet<>());

        generatedClasses = synchronizedMap(new LinkedHashMap<>());

        openTypeNames  = synchronizedSet(new LinkedHashSet<>());

        aggregateGeneratedSourceNames = new LinkedHashSet<>();
        aggregateGeneratedClassNames  = new LinkedHashSet<>();
        initialClassNames  = new LinkedHashSet<>();

        lint = (Lint.instance(context)).isEnabled(PROCESSING);

        Options options = Options.instance(context);

        defaultTargetModule = options.get(Option.DEFAULT_MODULE_FOR_CREATED_FILES);
    }

    @Override @DefinedBy(Api.ANNOTATION_PROCESSING)
    public JavaFileObject createSourceFile(CharSequence nameAndModule,
                                           Element... originatingElements) throws IOException {
        Pair<ModuleSymbol, String> moduleAndClass = checkOrInferModule(nameAndModule);
        return createSourceOrClassFile(moduleAndClass.fst, true, moduleAndClass.snd);
    }

    @Override @DefinedBy(Api.ANNOTATION_PROCESSING)
    public JavaFileObject createClassFile(CharSequence nameAndModule,
                                          Element... originatingElements) throws IOException {
        Pair<ModuleSymbol, String> moduleAndClass = checkOrInferModule(nameAndModule);
        return createSourceOrClassFile(moduleAndClass.fst, false, moduleAndClass.snd);
    }

    private Pair<ModuleSymbol, String> checkOrInferModule(CharSequence moduleAndPkg) throws FilerException {
        String moduleAndPkgString = moduleAndPkg.toString();
        int slash = moduleAndPkgString.indexOf('/');
        String module;
        String pkg;

        if (slash == (-1)) {
            //module name not specified:
            int lastDot = moduleAndPkgString.lastIndexOf('.');
            String pack = lastDot != (-1) ? moduleAndPkgString.substring(0, lastDot) : "";
            ModuleSymbol msym = inferModule(pack);

            if (msym != null) {
                return Pair.of(msym, moduleAndPkgString);
            }

            if (defaultTargetModule == null) {
                throw new FilerException("Cannot determine target module.");
            }

            module = defaultTargetModule;
            pkg = moduleAndPkgString;
        } else {
            //module name specified:
            module = moduleAndPkgString.substring(0, slash);
            pkg = moduleAndPkgString.substring(slash + 1);
        }

        ModuleSymbol explicitModule = syms.getModule(names.fromString(module));

        if (explicitModule == null) {
            throw new FilerException("Module: " + module + " does not exist.");
        }

        if (!modules.isRootModule(explicitModule)) {
            throw new FilerException("Cannot write to the given module.");
        }

        return Pair.of(explicitModule, pkg);
    }

    private JavaFileObject createSourceOrClassFile(ModuleSymbol mod, boolean isSourceFile, String name) throws IOException {
        Assert.checkNonNull(mod);

        if (lint) {
            int periodIndex = name.lastIndexOf(".");
            if (periodIndex != -1) {
                String base = name.substring(periodIndex);
                String extn = (isSourceFile ? ".java" : ".class");
                if (base.equals(extn))
                    log.warning(Warnings.ProcSuspiciousClassName(name, extn));
            }
        }
        checkNameAndExistence(mod, name, isSourceFile);
        Location loc = (isSourceFile ? SOURCE_OUTPUT : CLASS_OUTPUT);

        if (modules.multiModuleMode) {
            loc = this.fileManager.getLocationForModule(loc, mod.name.toString());
        }
        JavaFileObject.Kind kind = (isSourceFile ?
                                    JavaFileObject.Kind.SOURCE :
                                    JavaFileObject.Kind.CLASS);

        JavaFileObject fileObject =
            fileManager.getJavaFileForOutput(loc, name, kind, null);
        checkFileReopening(fileObject, true);

        if (lastRound)
            log.warning(Warnings.ProcFileCreateLastRound(name));

        if (isSourceFile)
            aggregateGeneratedSourceNames.add(Pair.of(mod, name));
        else
            aggregateGeneratedClassNames.add(Pair.of(mod, name));
        openTypeNames.add(name);

        return new FilerOutputJavaFileObject(mod, name, fileObject);
    }

    @Override @DefinedBy(Api.ANNOTATION_PROCESSING)
    public FileObject createResource(JavaFileManager.Location location,
                                     CharSequence moduleAndPkg,
                                     CharSequence relativeName,
                                     Element... originatingElements) throws IOException {
        Tuple3<Location, ModuleSymbol, String> locationModuleAndPackage = checkOrInferModule(location, moduleAndPkg, true);
        location = locationModuleAndPackage.a;
        ModuleSymbol msym = locationModuleAndPackage.b;
        String pkg = locationModuleAndPackage.c;

        locationCheck(location);

        String strPkg = pkg.toString();
        if (strPkg.length() > 0)
            checkName(strPkg);

        FileObject fileObject =
            fileManager.getFileForOutput(location, strPkg,
                                         relativeName.toString(), null);
        checkFileReopening(fileObject, true);

        if (fileObject instanceof JavaFileObject javaFileObject)
            return new FilerOutputJavaFileObject(msym, null, javaFileObject);
        else
            return new FilerOutputFileObject(msym, null, fileObject);
    }

    private void locationCheck(JavaFileManager.Location location) {
        if (location instanceof StandardLocation standardLocation) {
            if (!standardLocation.isOutputLocation())
                throw new IllegalArgumentException("Resource creation not supported in location " +
                                                    standardLocation);
        }
    }

    @Override @DefinedBy(Api.ANNOTATION_PROCESSING)
    public FileObject getResource(JavaFileManager.Location location,
                                  CharSequence moduleAndPkg,
                                  CharSequence relativeName) throws IOException {
        Tuple3<Location, ModuleSymbol, String> locationModuleAndPackage = checkOrInferModule(location, moduleAndPkg, false);
        location = locationModuleAndPackage.a;
        String pkg = locationModuleAndPackage.c;

        if (pkg.length() > 0)
            checkName(pkg);

        // TODO: Only support reading resources in selected output
        // locations?  Only allow reading of non-source, non-class
        // files from the supported input locations?

        // In the following, getFileForInput is the "obvious" method
        // to use, but it does not have the "obvious" semantics for
        // SOURCE_OUTPUT and CLASS_OUTPUT. Conversely, getFileForOutput
        // does not have the correct semantics for any "path" location
        // with more than one component. So, for now, we use a hybrid
        // invocation.
        FileObject fileObject;
        if (location.isOutputLocation()) {
            fileObject = fileManager.getFileForOutput(location,
                    pkg,
                    relativeName.toString(),
                    null);
        } else {
            fileObject = fileManager.getFileForInput(location,
                    pkg,
                    relativeName.toString());
        }
        if (fileObject == null) {
            String name = (pkg.length() == 0)
                    ? relativeName.toString() : (pkg + "/" + relativeName);
            throw new FileNotFoundException(name);
        }

        // If the path was already opened for writing, throw an exception.
        checkFileReopening(fileObject, false);
        return new FilerInputFileObject(fileObject);
    }

    private Tuple3<JavaFileManager.Location, ModuleSymbol, String> checkOrInferModule(JavaFileManager.Location location,
                                                           CharSequence moduleAndPkg,
                                                           boolean write) throws IOException {
        String moduleAndPkgString = moduleAndPkg.toString();
        int slash = moduleAndPkgString.indexOf('/');
        boolean multiModuleLocation = location.isModuleOrientedLocation() ||
                                      (modules.multiModuleMode && location.isOutputLocation());
        String module;
        String pkg;

        if (slash == (-1)) {
            //module name not specified:
            if (!multiModuleLocation) {
                //package oriented location:
                return new Tuple3<>(location, modules.getDefaultModule(), moduleAndPkgString);
            }

            if (location.isOutputLocation()) {
                ModuleSymbol msym = inferModule(moduleAndPkgString);

                if (msym != null) {
                    Location moduleLoc =
                            fileManager.getLocationForModule(location, msym.name.toString());
                    return new Tuple3<>(moduleLoc, msym, moduleAndPkgString);
                }
            }

            if (defaultTargetModule == null) {
                throw new FilerException("No module specified and the location is either " +
                                         "a module-oriented location, or a multi-module " +
                                         "output location.");
            }

            module = defaultTargetModule;
            pkg = moduleAndPkgString;
        } else {
            //module name specified:
            module = moduleAndPkgString.substring(0, slash);
            pkg = moduleAndPkgString.substring(slash + 1);
        }

        if (multiModuleLocation) {
            ModuleSymbol explicitModule = syms.getModule(names.fromString(module));

            if (explicitModule == null) {
                throw new FilerException("Module: " + module + " does not exist.");
            }

            if (write && !modules.isRootModule(explicitModule)) {
                throw new FilerException("Cannot write to the given module.");
            }

            Location moduleLoc = fileManager.getLocationForModule(location, module);

            return new Tuple3<>(moduleLoc, explicitModule, pkg);
        } else {
            throw new FilerException("Module specified but the location is neither " +
                                     "a module-oriented location, nor a multi-module " +
                                     "output location.");
        }
    }

    static final class Tuple3<A, B, C> {
        final A a;
        final B b;
        final C c;

        public Tuple3(A a, B b, C c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private ModuleSymbol inferModule(String pkg) {
        if (modules.getDefaultModule() == syms.noModule)
            return modules.getDefaultModule();

        Set<ModuleSymbol> rootModules = modules.getRootModules();

        if (rootModules.size() == 1) {
            return rootModules.iterator().next();
        }

        PackageSymbol pack = elementUtils.getPackageElement(pkg);

        if (pack != null && pack.modle != syms.unnamedModule) {
            return pack.modle;
        }

        return null;
    }

    private void checkName(String name) throws FilerException {
        checkName(name, false);
    }

    private void checkName(String name, boolean allowUnnamedPackageInfo) throws FilerException {
        if (!SourceVersion.isName(name) && !isPackageInfo(name, allowUnnamedPackageInfo)) {
            if (lint)
                log.warning(Warnings.ProcIllegalFileName(name));
            throw new FilerException("Illegal name " + name);
        }
    }

    private boolean isPackageInfo(String name, boolean allowUnnamedPackageInfo) {
        // Is the name of the form "package-info" or
        // "foo.bar.package-info"?
        final String PKG_INFO = "package-info";
        int periodIndex = name.lastIndexOf(".");
        if (periodIndex == -1) {
            return allowUnnamedPackageInfo ? name.equals(PKG_INFO) : false;
        } else {
            // "foo.bar.package-info." illegal
            String prefix = name.substring(0, periodIndex);
            String simple = name.substring(periodIndex+1);
            return SourceVersion.isName(prefix) && simple.equals(PKG_INFO);
        }
    }

    private void checkNameAndExistence(ModuleSymbol mod, String typename, boolean allowUnnamedPackageInfo) throws FilerException {
        checkName(typename, allowUnnamedPackageInfo);
        ClassSymbol existing = elementUtils.getTypeElement(typename);
        boolean alreadySeen = aggregateGeneratedSourceNames.contains(Pair.of(mod, typename)) ||
                              aggregateGeneratedClassNames.contains(Pair.of(mod, typename)) ||
                              initialClassNames.contains(typename) ||
                              containedInInitialInputs(typename);
        if (alreadySeen) {
            if (lint)
                log.warning(Warnings.ProcTypeRecreate(typename));
            throw new FilerException("Attempt to recreate a file for type " + typename);
        }
        if (lint && existing != null) {
            log.warning(Warnings.ProcTypeAlreadyExists(typename));
        }
        if (!mod.isUnnamed() && !typename.contains(".")) {
            throw new FilerException("Attempt to create a type in unnamed package of a named module: " + typename);
        }
    }

    private boolean containedInInitialInputs(String typename) {
        // Name could be a type name or the name of a package-info file
        JavaFileObject sourceFile = null;

        ClassSymbol existingClass = elementUtils.getTypeElement(typename);
        if (existingClass != null) {
            sourceFile = existingClass.sourcefile;
        } else if (typename.endsWith(".package-info")) {
            String targetName = typename.substring(0, typename.length() - ".package-info".length());
            PackageSymbol existingPackage = elementUtils.getPackageElement(targetName);
            if (existingPackage != null)
                sourceFile = existingPackage.sourcefile;
        }
        return (sourceFile == null) ? false : initialInputs.contains(sourceFile);
    }

    /**
     * Check to see if the file has already been opened; if so, throw
     * an exception, otherwise add it to the set of files.
     */
    private void checkFileReopening(FileObject fileObject, boolean forWriting) throws FilerException {
        if (isInFileObjectHistory(fileObject, forWriting)) {
            if (lint)
                log.warning(Warnings.ProcFileReopening(fileObject.getName()));
            throw new FilerException("Attempt to reopen a file for path " + fileObject.getName());
        }
        if (forWriting)
            fileObjectHistory.add(fileObject);
    }

    private boolean isInFileObjectHistory(FileObject fileObject, boolean forWriting) {
        if (forWriting) {
            for(FileObject veteran : initialInputs) {
                try {
                    if (fileManager.isSameFile(veteran, fileObject)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    //ignore...
                }
            }
            for (String className : initialClassNames) {
                try {
                    ClassSymbol existing = elementUtils.getTypeElement(className);
                    if (   existing != null
                        && (   (existing.sourcefile != null && fileManager.isSameFile(existing.sourcefile, fileObject))
                            || (existing.classfile != null && fileManager.isSameFile(existing.classfile, fileObject)))) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    //ignore...
                }
            }
        }

        for(FileObject veteran : fileObjectHistory) {
            if (fileManager.isSameFile(veteran, fileObject)) {
                return true;
            }
        }

        return false;
    }

    public boolean newFiles() {
        return (!generatedSourceNames.isEmpty())
            || (!generatedClasses.isEmpty());
    }

    public Set<String> getGeneratedSourceNames() {
        return generatedSourceNames;
    }

    public Set<JavaFileObject> getGeneratedSourceFileObjects() {
        return generatedSourceFileObjects;
    }

    public Map<ModuleSymbol, Map<String, JavaFileObject>> getGeneratedClasses() {
        return generatedClasses;
    }

    public void warnIfUnclosedFiles() {
        if (!openTypeNames.isEmpty())
            log.warning(Warnings.ProcUnclosedTypeFiles(openTypeNames));
    }

    /**
     * Update internal state for a new round.
     */
    public void newRound() {
        clearRoundState();
    }

    void setLastRound(boolean lastRound) {
        this.lastRound = lastRound;
    }

    public void setInitialState(Collection<? extends JavaFileObject> initialInputs,
                                Collection<String> initialClassNames) {
        this.initialInputs.addAll(initialInputs);
        this.initialClassNames.addAll(initialClassNames);
    }

    public void close() {
        clearRoundState();
        // Cross-round state
        initialClassNames.clear();
        initialInputs.clear();
        fileObjectHistory.clear();
        openTypeNames.clear();
        aggregateGeneratedSourceNames.clear();
        aggregateGeneratedClassNames.clear();
    }

    private void clearRoundState() {
        generatedSourceNames.clear();
        generatedSourceFileObjects.clear();
        generatedClasses.clear();
    }

    /**
     * Debugging function to display internal state.
     */
    public void displayState() {
        PrintWriter xout = context.get(Log.logKey).getWriter(Log.WriterKind.STDERR);
        xout.println("File Object History : " +  fileObjectHistory);
        xout.println("Open Type Names     : " +  openTypeNames);
        xout.println("Gen. Src Names      : " +  generatedSourceNames);
        xout.println("Gen. Cls Names      : " +  generatedClasses.keySet());
        xout.println("Agg. Gen. Src Names : " +  aggregateGeneratedSourceNames);
        xout.println("Agg. Gen. Cls Names : " +  aggregateGeneratedClassNames);
    }

    public String toString() {
        return "javac Filer";
    }

    /**
     * Upon close, register files opened by create{Source, Class}File
     * for annotation processing.
     */
    private void closeFileObject(ModuleSymbol mod, String typeName, FileObject fileObject) {
        /*
         * If typeName is non-null, the file object was opened as a
         * source or class file by the user.  If a file was opened as
         * a resource, typeName will be null and the file is *not*
         * subject to annotation processing.
         */
        if ((typeName != null)) {
            if (!(fileObject instanceof JavaFileObject javaFileObject))
                throw new AssertionError("JavaFileObject not found for " + fileObject);
            switch(javaFileObject.getKind()) {
            case SOURCE:
                generatedSourceNames.add(typeName);
                generatedSourceFileObjects.add(javaFileObject);
                openTypeNames.remove(typeName);
                break;

            case CLASS:
                generatedClasses.computeIfAbsent(mod, m -> Collections.synchronizedMap(new LinkedHashMap<>())).put(typeName, javaFileObject);
                openTypeNames.remove(typeName);
                break;

            default:
                break;
            }
        }
    }

}
