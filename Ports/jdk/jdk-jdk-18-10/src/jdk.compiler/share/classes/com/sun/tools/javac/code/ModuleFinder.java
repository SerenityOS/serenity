/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.javac.code;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.jvm.ModuleNameReader;
import com.sun.tools.javac.jvm.ModuleNameReader.BadClassFile;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.Fragment;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.code.Kinds.Kind.*;

/**
 *  This class provides operations to locate module definitions
 *  from the source and class files on the paths provided to javac.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ModuleFinder {
    /** The context key for the module finder. */
    protected static final Context.Key<ModuleFinder> moduleFinderKey = new Context.Key<>();

    /** The log to use for verbose output. */
    private final Log log;

    /** The symbol table. */
    private final Symtab syms;

    /** The name table. */
    private final Names names;

    private final ClassFinder classFinder;

    /** Access to files
     */
    private final JavaFileManager fileManager;

    private final JCDiagnostic.Factory diags;

    private final DeferredCompletionFailureHandler dcfh;

    private ModuleNameReader moduleNameReader;

    public ModuleNameFromSourceReader moduleNameFromSourceReader;

    /** Get the ModuleFinder instance for this invocation. */
    public static ModuleFinder instance(Context context) {
        ModuleFinder instance = context.get(moduleFinderKey);
        if (instance == null)
            instance = new ModuleFinder(context);
        return instance;
    }

    /** Construct a new module finder. */
    protected ModuleFinder(Context context) {
        context.put(moduleFinderKey, this);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        fileManager = context.get(JavaFileManager.class);
        log = Log.instance(context);
        classFinder = ClassFinder.instance(context);

        diags = JCDiagnostic.Factory.instance(context);
        dcfh = DeferredCompletionFailureHandler.instance(context);
    }

    class ModuleLocationIterator implements Iterator<Set<Location>> {
        StandardLocation outer;
        Set<Location> next = null;

        Iterator<StandardLocation> outerIter = Arrays.asList(
                StandardLocation.MODULE_SOURCE_PATH,
                StandardLocation.UPGRADE_MODULE_PATH,
                StandardLocation.SYSTEM_MODULES,
                StandardLocation.MODULE_PATH
        ).iterator();
        Iterator<Set<Location>> innerIter = null;

        @Override
        public boolean hasNext() {
            while (next == null) {
                while (innerIter == null || !innerIter.hasNext()) {
                    if (outerIter.hasNext()) {
                        outer = outerIter.next();
                        try {
                            innerIter = fileManager.listLocationsForModules(outer).iterator();
                        } catch (IOException e) {
                            System.err.println("error listing module locations for " + outer + ": " + e);  // FIXME
                        }
                    } else
                        return false;
                }

                if (innerIter.hasNext())
                    next = innerIter.next();
            }
            return true;
        }

        @Override
        public Set<Location> next() {
            hasNext();
            if (next != null) {
                Set<Location> result = next;
                next = null;
                return result;
            }
            throw new NoSuchElementException();
        }

    }

    ModuleLocationIterator moduleLocationIterator = new ModuleLocationIterator();

    public ModuleSymbol findModule(Name name) {
        return findModule(syms.enterModule(name));
    }

    public ModuleSymbol findModule(ModuleSymbol msym) {
        if (msym.kind != ERR && msym.sourceLocation == null && msym.classLocation == null) {
            // fill in location
            List<ModuleSymbol> list = scanModulePath(msym);
            if (list.isEmpty()) {
                msym.kind = ERR;
            }
        }
        if (msym.kind != ERR && msym.module_info.sourcefile == null && msym.module_info.classfile == null) {
            // fill in module-info
            findModuleInfo(msym);
        }
        return msym;
    }

    public List<ModuleSymbol> findAllModules() {
        List<ModuleSymbol> list = scanModulePath(null);
        for (ModuleSymbol msym: list) {
            if (msym.kind != ERR && msym.module_info.sourcefile == null && msym.module_info.classfile == null) {
                // fill in module-info
                findModuleInfo(msym);
            }
        }
        return list;
    }

    public ModuleSymbol findSingleModule() {
        try {
            JavaFileObject src_fo = getModuleInfoFromLocation(StandardLocation.SOURCE_PATH, Kind.SOURCE);
            JavaFileObject class_fo = getModuleInfoFromLocation(StandardLocation.CLASS_OUTPUT, Kind.CLASS);
            JavaFileObject fo = (src_fo == null) ? class_fo
                    : (class_fo == null) ? src_fo
                            : classFinder.preferredFileObject(src_fo, class_fo);

            ModuleSymbol msym;
            if (fo == null) {
                msym = syms.unnamedModule;
            } else {
                msym = readModule(fo);
            }

            if (msym.patchLocation == null) {
                msym.classLocation = StandardLocation.CLASS_OUTPUT;
            } else {
                msym.patchOutputLocation = StandardLocation.CLASS_OUTPUT;
            }
            return msym;

        } catch (IOException e) {
            throw new Error(e); // FIXME
        }
    }

    private ModuleSymbol readModule(JavaFileObject fo) throws IOException {
        Name name;
        switch (fo.getKind()) {
            case SOURCE:
                name = moduleNameFromSourceReader.readModuleName(fo);
                if (name == null) {
                    JCDiagnostic diag =
                        diags.fragment(Fragments.FileDoesNotContainModule);
                    ClassSymbol errModuleInfo = syms.defineClass(names.module_info, syms.errModule);
                    throw new ClassFinder.BadClassFile(errModuleInfo, fo, diag, diags, dcfh);
                }
                break;
            case CLASS:
                try {
                    name = names.fromString(readModuleName(fo));
                } catch (BadClassFile | IOException ex) {
                    //fillIn will report proper errors:
                    name = names.error;
                }
                break;
            default:
                Assert.error();
                name = names.error;
                break;
        }

        ModuleSymbol msym = syms.enterModule(name);

        if (msym.module_info.classfile == null) {
            msym.module_info.classfile = fo;
            if (fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH) && name != names.error) {
                msym.patchLocation = fileManager.getLocationForModule(StandardLocation.PATCH_MODULE_PATH, name.toString());

                if (msym.patchLocation != null) {
                    JavaFileObject patchFO = getModuleInfoFromLocation(StandardLocation.CLASS_OUTPUT, Kind.CLASS);
                    patchFO = preferredFileObject(getModuleInfoFromLocation(msym.patchLocation, Kind.CLASS), patchFO);
                    patchFO = preferredFileObject(getModuleInfoFromLocation(msym.patchLocation, Kind.SOURCE), patchFO);

                    if (patchFO != null) {
                        msym.module_info.classfile = patchFO;
                    }
                }
            }

            msym.completer = Completer.NULL_COMPLETER;
            classFinder.fillIn(msym.module_info);
        }

        return msym;
    }

    private String readModuleName(JavaFileObject jfo) throws IOException, ModuleNameReader.BadClassFile {
        if (moduleNameReader == null)
            moduleNameReader = new ModuleNameReader();
        return moduleNameReader.readModuleName(jfo);
    }

    private JavaFileObject getModuleInfoFromLocation(Location location, Kind kind) throws IOException {
        if (location == null || !fileManager.hasLocation(location))
            return null;

        return fileManager.getJavaFileForInput(location,
                                               names.module_info.toString(),
                                               kind);
    }

    private List<ModuleSymbol> scanModulePath(ModuleSymbol toFind) {
        ListBuffer<ModuleSymbol> results = new ListBuffer<>();
        Map<Name, Location> namesInSet = new HashMap<>();
        boolean multiModuleMode = fileManager.hasLocation(StandardLocation.MODULE_SOURCE_PATH);
        while (moduleLocationIterator.hasNext()) {
            Set<Location> locns = (moduleLocationIterator.next());
            namesInSet.clear();
            for (Location l: locns) {
                try {
                    Name n = names.fromString(fileManager.inferModuleName(l));
                    if (namesInSet.put(n, l) == null) {
                        ModuleSymbol msym = syms.enterModule(n);
                        if (msym.sourceLocation != null || msym.classLocation != null) {
                            // module has already been found, so ignore this instance
                            continue;
                        }
                        if (fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH) &&
                            msym.patchLocation == null) {
                            msym.patchLocation =
                                    fileManager.getLocationForModule(StandardLocation.PATCH_MODULE_PATH,
                                                                     msym.name.toString());
                            if (msym.patchLocation != null &&
                                multiModuleMode &&
                                fileManager.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                                msym.patchOutputLocation =
                                        fileManager.getLocationForModule(StandardLocation.CLASS_OUTPUT,
                                                                         msym.name.toString());
                            }
                        }
                        if (moduleLocationIterator.outer == StandardLocation.MODULE_SOURCE_PATH) {
                            msym.sourceLocation = l;
                            if (fileManager.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                                msym.classLocation =
                                        fileManager.getLocationForModule(StandardLocation.CLASS_OUTPUT,
                                                                         msym.name.toString());
                            }
                        } else {
                            msym.classLocation = l;
                        }
                        if (moduleLocationIterator.outer == StandardLocation.SYSTEM_MODULES ||
                            moduleLocationIterator.outer == StandardLocation.UPGRADE_MODULE_PATH) {
                            msym.flags_field |= Flags.SYSTEM_MODULE;
                        }
                        if (toFind == null ||
                            (toFind == msym && (msym.sourceLocation != null || msym.classLocation != null))) {
                            // Note: cannot return msym directly, because we must finish
                            // processing this set first
                            results.add(msym);
                        }
                    } else {
                        log.error(Errors.DuplicateModuleOnPath(
                                getDescription(moduleLocationIterator.outer), n));
                    }
                } catch (IOException e) {
                    // skip location for now?  log error?
                }
            }
            if (toFind != null && results.nonEmpty())
                return results.toList();
        }

        return results.toList();
    }

    private void findModuleInfo(ModuleSymbol msym) {
        try {
            JavaFileObject fo;

            fo = getModuleInfoFromLocation(msym.patchOutputLocation, Kind.CLASS);
            fo = preferredFileObject(getModuleInfoFromLocation(msym.patchLocation, Kind.CLASS), fo);
            fo = preferredFileObject(getModuleInfoFromLocation(msym.patchLocation, Kind.SOURCE), fo);

            if (fo == null) {
                fo = getModuleInfoFromLocation(msym.classLocation, Kind.CLASS);
                fo = preferredFileObject(getModuleInfoFromLocation(msym.sourceLocation, Kind.SOURCE), fo);
            }

            if (fo == null) {
                String moduleName = msym.sourceLocation == null && msym.classLocation != null ?
                    fileManager.inferModuleName(msym.classLocation) : null;
                if (moduleName != null) {
                    msym.module_info.classfile = null;
                    msym.flags_field |= Flags.AUTOMATIC_MODULE;
                } else {
                    msym.kind = ERR;
                }
            } else {
                msym.module_info.classfile = fo;
                msym.module_info.completer = new Symbol.Completer() {
                    @Override
                    public void complete(Symbol sym) throws CompletionFailure {
                        classFinder.fillIn(msym.module_info);
                    }
                    @Override
                    public String toString() {
                        return "ModuleInfoCompleter";
                    }
                };
            }
        } catch (IOException e) {
            msym.kind = ERR;
        }
    }

    private JavaFileObject preferredFileObject(JavaFileObject fo1, JavaFileObject fo2) {
        if (fo1 == null) return fo2;
        if (fo2 == null) return fo1;
        return classFinder.preferredFileObject(fo1, fo2);
    }

    Fragment getDescription(StandardLocation l) {
        switch (l) {
            case MODULE_PATH: return Fragments.LocnModule_path;
            case MODULE_SOURCE_PATH: return Fragments.LocnModule_source_path;
            case SYSTEM_MODULES: return Fragments.LocnSystem_modules;
            case UPGRADE_MODULE_PATH: return Fragments.LocnUpgrade_module_path;
            default:
                throw new AssertionError();
        }
    }

    public interface ModuleNameFromSourceReader {
        public Name readModuleName(JavaFileObject file);
    }

}
