/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.tools.sjavac.comp.CompilationService;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The javac state class maintains the previous (prev) and the current (now)
 * build states and everything else that goes into the javac_state file.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavacState {
    // The arguments to the compile. If not identical, then it cannot
    // be an incremental build!
    String theArgs;
    // The number of cores limits how many threads are used for heavy concurrent work.
    int numCores;

    // The bin_dir/javac_state
    private File javacState;

    // The previous build state is loaded from javac_state
    private BuildState prev;
    // The current build state is constructed during the build,
    // then saved as the new javac_state.
    private BuildState now;

    // Something has changed in the javac_state. It needs to be saved!
    private boolean needsSaving;
    // If this is a new javac_state file, then do not print unnecessary messages.
    private boolean newJavacState;

    // These are packages where something has changed and the package
    // needs to be recompiled. Actions that trigger recompilation:
    // * source belonging to the package has changed
    // * artifact belonging to the package is lost, or its timestamp has been changed.
    // * an unknown artifact has appeared, we simply delete it, but we also trigger a recompilation.
    // * a package that is tainted, taints all packages that depend on it.
    private Set<String> taintedPackages;
    // After a compile, the pubapis are compared with the pubapis stored in the javac state file.
    // Any packages where the pubapi differ are added to this set.
    // Later we use this set and the dependency information to taint dependent packages.
    private Set<String> packagesWithChangedPublicApis;
    // When a module-info.java file is changed, taint the module,
    // then taint all modules that depend on that that module.
    // A module dependency can occur directly through a require, or
    // indirectly through a module that does a public export for the first tainted module.
    // When all modules are tainted, then taint all packages belonging to these modules.
    // Then rebuild. It is perhaps possible (and valuable?) to do a more fine-grained examination of the
    // change in module-info.java, but that will have to wait.
    private Set<String> taintedModules;
    // The set of all packages that has been recompiled.
    // Copy over the javac_state for the packages that did not need recompilation,
    // verbatim from the previous (prev) to the new (now) build state.
    private Set<String> recompiledPackages;

    // The output directories filled with tasty artifacts.
    private File binDir, gensrcDir, headerDir, stateDir;

    // The current status of the file system.
    private Set<File> binArtifacts;
    private Set<File> gensrcArtifacts;
    private Set<File> headerArtifacts;

    // The status of the sources.
    Set<Source> removedSources = null;
    Set<Source> addedSources = null;
    Set<Source> modifiedSources = null;

    // Visible sources for linking. These are the only
    // ones that -sourcepath is allowed to see.
    Set<URI> visibleSrcs;

    // Setup transform that always exist.
    private CompileJavaPackages compileJavaPackages = new CompileJavaPackages();

    // Command line options.
    private Options options;

    JavacState(Options op, boolean removeJavacState) {
        options = op;
        numCores = options.getNumCores();
        theArgs = options.getStateArgsString();
        binDir = Util.pathToFile(options.getDestDir());
        gensrcDir = Util.pathToFile(options.getGenSrcDir());
        headerDir = Util.pathToFile(options.getHeaderDir());
        stateDir = Util.pathToFile(options.getStateDir());
        javacState = new File(stateDir, "javac_state");
        if (removeJavacState && javacState.exists()) {
            javacState.delete();
        }
        newJavacState = false;
        if (!javacState.exists()) {
            newJavacState = true;
            // If there is no javac_state then delete the contents of all the artifact dirs!
            // We do not want to risk building a broken incremental build.
            // BUT since the makefiles still copy things straight into the bin_dir et al,
            // we avoid deleting files here, if the option --permit-unidentified-classes was supplied.
            if (!options.areUnidentifiedArtifactsPermitted()) {
                deleteContents(binDir);
                deleteContents(gensrcDir);
                deleteContents(headerDir);
            }
            needsSaving = true;
        }
        prev = new BuildState();
        now = new BuildState();
        taintedPackages = new HashSet<>();
        recompiledPackages = new HashSet<>();
        packagesWithChangedPublicApis = new HashSet<>();
    }

    public BuildState prev() { return prev; }
    public BuildState now() { return now; }

    /**
     * Remove args not affecting the state.
     */
    static String[] removeArgsNotAffectingState(String[] args) {
        String[] out = new String[args.length];
        int j = 0;
        for (int i = 0; i<args.length; ++i) {
            if (args[i].equals("-j")) {
                // Just skip it and skip following value
                i++;
            } else if (args[i].startsWith("--server:")) {
                // Just skip it.
            } else if (args[i].startsWith("--log=")) {
                // Just skip it.
            } else if (args[i].equals("--compare-found-sources")) {
                // Just skip it and skip verify file name
                i++;
            } else {
                // Copy argument.
                out[j] = args[i];
                j++;
            }
        }
        String[] ret = new String[j];
        System.arraycopy(out, 0, ret, 0, j);
        return ret;
    }

    /**
     * Specify which sources are visible to the compiler through -sourcepath.
     */
    public void setVisibleSources(Map<String,Source> vs) {
        visibleSrcs = new HashSet<>();
        for (String s : vs.keySet()) {
            Source src = vs.get(s);
            visibleSrcs.add(src.file().toURI());
        }
    }

    /**
     * Returns true if this is an incremental build.
     */
    public boolean isIncremental() {
        return !prev.sources().isEmpty();
    }

    /**
     * Find all artifacts that exists on disk.
     */
    public void findAllArtifacts() {
        binArtifacts = findAllFiles(binDir);
        gensrcArtifacts = findAllFiles(gensrcDir);
        headerArtifacts = findAllFiles(headerDir);
    }

    /**
     * Lookup the artifacts generated for this package in the previous build.
     */
    private Map<String,File> fetchPrevArtifacts(String pkg) {
        Package p = prev.packages().get(pkg);
        if (p != null) {
            return p.artifacts();
        }
        return new HashMap<>();
    }

    /**
     * Delete all prev artifacts in the currently tainted packages.
     */
    public void deleteClassArtifactsInTaintedPackages() {
        for (String pkg : taintedPackages) {
            Map<String,File> arts = fetchPrevArtifacts(pkg);
            for (File f : arts.values()) {
                if (f.exists() && f.getName().endsWith(".class")) {
                    f.delete();
                }
            }
        }
    }

    /**
     * Mark the javac_state file to be in need of saving and as a side effect,
     * it gets a new timestamp.
     */
    private void needsSaving() {
        needsSaving = true;
    }

    /**
     * Save the javac_state file.
     */
    public void save() throws IOException {
        if (!needsSaving)
            return;
        try (FileWriter out = new FileWriter(javacState)) {
            StringBuilder b = new StringBuilder();
            long millisNow = System.currentTimeMillis();
            Date d = new Date(millisNow);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            b.append("# javac_state ver 0.4 generated "+millisNow+" "+df.format(d)+"\n");
            b.append("# This format might change at any time. Please do not depend on it.\n");
            b.append("# R arguments\n");
            b.append("# M module\n");
            b.append("# P package\n");
            b.append("# S C source_tobe_compiled timestamp\n");
            b.append("# S L link_only_source timestamp\n");
            b.append("# G C generated_source timestamp\n");
            b.append("# A artifact timestamp\n");
            b.append("# D S dependant -> source dependency\n");
            b.append("# D C dependant -> classpath dependency\n");
            b.append("# I pubapi\n");
            b.append("R ").append(theArgs).append("\n");

            // Copy over the javac_state for the packages that did not need recompilation.
            now.copyPackagesExcept(prev, recompiledPackages, new HashSet<String>());
            // Save the packages, ie package names, dependencies, pubapis and artifacts!
            // I.e. the lot.
            Module.saveModules(now.modules(), b);

            String s = b.toString();
            out.write(s, 0, s.length());
        }
    }

    /**
     * Load a javac_state file.
     */
    public static JavacState load(Options options) {
        JavacState db = new JavacState(options, false);
        Module  lastModule = null;
        Package lastPackage = null;
        Source  lastSource = null;
        boolean noFileFound = false;
        boolean foundCorrectVerNr = false;
        boolean newCommandLine = false;
        boolean syntaxError = false;

        Log.debug("Loading javac state file: " + db.javacState);

        try (BufferedReader in = new BufferedReader(new FileReader(db.javacState))) {
            for (;;) {
                String l = in.readLine();
                if (l==null) break;
                if (l.length()>=3 && l.charAt(1) == ' ') {
                    char c = l.charAt(0);
                    if (c == 'M') {
                        lastModule = db.prev.loadModule(l);
                    } else
                    if (c == 'P') {
                        if (lastModule == null) { syntaxError = true; break; }
                        lastPackage = db.prev.loadPackage(lastModule, l);
                    } else
                    if (c == 'D') {
                        if (lastModule == null || lastPackage == null) { syntaxError = true; break; }
                        char depType = l.charAt(2);
                        if (depType != 'S' && depType != 'C')
                            throw new RuntimeException("Bad dependency string: " + l);
                        lastPackage.parseAndAddDependency(l.substring(4), depType == 'C');
                    } else
                    if (c == 'I') {
                        if (lastModule == null || lastPackage == null) { syntaxError = true; break; }
                        lastPackage.getPubApi().appendItem(l.substring(2)); // Strip "I "
                    } else
                    if (c == 'A') {
                        if (lastModule == null || lastPackage == null) { syntaxError = true; break; }
                        lastPackage.loadArtifact(l);
                    } else
                    if (c == 'S') {
                        if (lastModule == null || lastPackage == null) { syntaxError = true; break; }
                        lastSource = db.prev.loadSource(lastPackage, l, false);
                    } else
                    if (c == 'G') {
                        if (lastModule == null || lastPackage == null) { syntaxError = true; break; }
                        lastSource = db.prev.loadSource(lastPackage, l, true);
                    } else
                    if (c == 'R') {
                        String ncmdl = "R "+db.theArgs;
                        if (!l.equals(ncmdl)) {
                            newCommandLine = true;
                        }
                    } else
                         if (c == '#') {
                        if (l.startsWith("# javac_state ver ")) {
                            int sp = l.indexOf(" ", 18);
                            if (sp != -1) {
                                String ver = l.substring(18,sp);
                                if (!ver.equals("0.4")) {
                    break;
                                 }
                foundCorrectVerNr = true;
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException | NoSuchFileException e) {
            // Silently create a new javac_state file.
            noFileFound = true;
        } catch (IOException e) {
            Log.warn("Dropping old javac_state because of errors when reading it.");
            db = new JavacState(options, true);
            foundCorrectVerNr = true;
            newCommandLine = false;
            syntaxError = false;
    }
        if (foundCorrectVerNr == false && !noFileFound) {
            Log.debug("Dropping old javac_state since it is of an old version.");
            db = new JavacState(options, true);
        } else
        if (newCommandLine == true && !noFileFound) {
            Log.debug("Dropping old javac_state since a new command line is used!");
            db = new JavacState(options, true);
        } else
        if (syntaxError == true) {
            Log.warn("Dropping old javac_state since it contains syntax errors.");
            db = new JavacState(options, true);
        }
        db.prev.calculateDependents();
        return db;
    }

    /**
     * Mark a java package as tainted, ie it needs recompilation.
     */
    public void taintPackage(String name, String because) {
        if (!taintedPackages.contains(name)) {
            if (because != null) Log.debug("Tainting "+Util.justPackageName(name)+" because "+because);
            // It has not been tainted before.
            taintedPackages.add(name);
            needsSaving();
            Package nowp = now.packages().get(name);
            if (nowp != null) {
                for (String d : nowp.dependents()) {
                    taintPackage(d, because);
                }
            }
        }
    }

    /**
     * This packages need recompilation.
     */
    public Set<String> taintedPackages() {
        return taintedPackages;
    }

    /**
     * Clean out the tainted package set, used after the first round of compiles,
     * prior to propagating dependencies.
     */
    public void clearTaintedPackages() {
        taintedPackages = new HashSet<>();
    }

    /**
     * Go through all sources and check which have been removed, added or modified
     * and taint the corresponding packages.
     */
    public void checkSourceStatus(boolean check_gensrc) {
        removedSources = calculateRemovedSources();
        for (Source s : removedSources) {
            if (!s.isGenerated() || check_gensrc) {
                taintPackage(s.pkg().name(), "source "+s.name()+" was removed");
            }
        }

        addedSources = calculateAddedSources();
        for (Source s : addedSources) {
            String msg = null;
            if (isIncremental()) {
                // When building from scratch, there is no point
                // printing "was added" for every file since all files are added.
                // However for an incremental build it makes sense.
                msg = "source "+s.name()+" was added";
            }
            if (!s.isGenerated() || check_gensrc) {
                taintPackage(s.pkg().name(), msg);
            }
        }

        modifiedSources = calculateModifiedSources();
        for (Source s : modifiedSources) {
            if (!s.isGenerated() || check_gensrc) {
                taintPackage(s.pkg().name(), "source "+s.name()+" was modified");
            }
        }
    }

    /**
     * Acquire the compile_java_packages suffix rule for .java files.
     */
    public Map<String,Transformer> getJavaSuffixRule() {
        Map<String,Transformer> sr = new HashMap<>();
        sr.put(".java", compileJavaPackages);
        return sr;
    }


    /**
     * If artifacts have gone missing, force a recompile of the packages
     * they belong to.
     */
    public void taintPackagesThatMissArtifacts() {
        for (Package pkg : prev.packages().values()) {
            for (File f : pkg.artifacts().values()) {
                if (!f.exists()) {
                    // Hmm, the artifact on disk does not exist! Someone has removed it....
                    // Lets rebuild the package.
                    taintPackage(pkg.name(), ""+f+" is missing.");
                }
            }
        }
    }

    /**
     * Propagate recompilation through the dependency chains.
     * Avoid re-tainting packages that have already been compiled.
     */
    public void taintPackagesDependingOnChangedPackages(Set<String> pkgsWithChangedPubApi, Set<String> recentlyCompiled) {
        // For each to-be-recompiled-candidates...
        for (Package pkg : new HashSet<>(prev.packages().values())) {
            // Find out what it depends upon...
            Set<String> deps = pkg.typeDependencies()
                                  .values()
                                  .stream()
                                  .flatMap(Collection::stream)
                                  .collect(Collectors.toSet());
            for (String dep : deps) {
                String depPkg = ":" + dep.substring(0, dep.lastIndexOf('.'));
                if (depPkg.equals(pkg.name()))
                    continue;
                // Checking if that dependency has changed
                if (pkgsWithChangedPubApi.contains(depPkg) && !recentlyCompiled.contains(pkg.name())) {
                    taintPackage(pkg.name(), "its depending on " + depPkg);
                }
            }
        }
    }

    /**
     * Compare the javac_state recorded public apis of packages on the classpath
     * with the actual public apis on the classpath.
     */
    public void taintPackagesDependingOnChangedClasspathPackages() throws IOException {

        // 1. Collect fully qualified names of all interesting classpath dependencies
        Set<String> fqDependencies = new HashSet<>();
        for (Package pkg : prev.packages().values()) {
            // Check if this package was compiled. If it's presence is recorded
            // because it was on the class path and we needed to save it's
            // public api, it's not a candidate for tainting.
            if (pkg.sources().isEmpty())
                continue;

            pkg.typeClasspathDependencies().values().forEach(fqDependencies::addAll);
        }

        // 2. Extract the public APIs from the on disk .class files
        // (Reason for doing step 1 in a separate phase is to avoid extracting
        // public APIs of the same class twice.)
        PubApiExtractor pubApiExtractor = new PubApiExtractor(options);
        Map<String, PubApi> onDiskPubApi = new HashMap<>();
        for (String cpDep : fqDependencies) {
            onDiskPubApi.put(cpDep, pubApiExtractor.getPubApi(cpDep));
        }
        pubApiExtractor.close();

        // 3. Compare them with the public APIs as of last compilation (loaded from javac_state)
        nextPkg:
        for (Package pkg : prev.packages().values()) {
            // Check if this package was compiled. If it's presence is recorded
            // because it was on the class path and we needed to save it's
            // public api, it's not a candidate for tainting.
            if (pkg.sources().isEmpty())
                continue;

            Set<String> cpDepsOfThisPkg = new HashSet<>();
            for (Set<String> cpDeps : pkg.typeClasspathDependencies().values())
                cpDepsOfThisPkg.addAll(cpDeps);

            for (String fqDep : cpDepsOfThisPkg) {

                String depPkg = ":" + fqDep.substring(0, fqDep.lastIndexOf('.'));
                PubApi prevPkgApi = prev.packages().get(depPkg).getPubApi();

                // This PubApi directly lists the members of the class,
                // i.e. [ MEMBER1, MEMBER2, ... ]
                PubApi prevDepApi = prevPkgApi.types.get(fqDep).pubApi;

                // In order to dive *into* the class, we need to add
                // .types.get(fqDep).pubApi below.
                PubApi currentDepApi = onDiskPubApi.get(fqDep).types.get(fqDep).pubApi;

                if (!currentDepApi.isBackwardCompatibleWith(prevDepApi)) {
                    List<String> apiDiff = currentDepApi.diff(prevDepApi);
                    taintPackage(pkg.name(), "depends on classpath "
                                + "package which has an updated package api: "
                                + String.join("\n", apiDiff));
                    //Log.debug("========================================");
                    //Log.debug("------ PREV API ------------------------");
                    //prevDepApi.asListOfStrings().forEach(Log::debug);
                    //Log.debug("------ CURRENT API ---------------------");
                    //currentDepApi.asListOfStrings().forEach(Log::debug);
                    //Log.debug("========================================");
                    continue nextPkg;
                }
            }
        }
    }

    /**
     * Scan all output dirs for artifacts and remove those files (artifacts?)
     * that are not recognized as such, in the javac_state file.
     */
    public void removeUnidentifiedArtifacts() {
        Set<File> allKnownArtifacts = new HashSet<>();
        for (Package pkg : prev.packages().values()) {
            for (File f : pkg.artifacts().values()) {
                allKnownArtifacts.add(f);
            }
        }
        // Do not forget about javac_state....
        allKnownArtifacts.add(javacState);

        for (File f : binArtifacts) {
            if (!allKnownArtifacts.contains(f) &&
                !options.isUnidentifiedArtifactPermitted(f.getAbsolutePath())) {
                Log.debug("Removing "+f.getPath()+" since it is unknown to the javac_state.");
                f.delete();
            }
        }
        for (File f : headerArtifacts) {
            if (!allKnownArtifacts.contains(f)) {
                Log.debug("Removing "+f.getPath()+" since it is unknown to the javac_state.");
                f.delete();
            }
        }
        for (File f : gensrcArtifacts) {
            if (!allKnownArtifacts.contains(f)) {
                Log.debug("Removing "+f.getPath()+" since it is unknown to the javac_state.");
                f.delete();
            }
        }
    }

    /**
     * Remove artifacts that are no longer produced when compiling!
     */
    public void removeSuperfluousArtifacts(Set<String> recentlyCompiled) {
        // Nothing to do, if nothing was recompiled.
        if (recentlyCompiled.size() == 0) return;

        for (String pkg : now.packages().keySet()) {
            // If this package has not been recompiled, skip the check.
            if (!recentlyCompiled.contains(pkg)) continue;
            Collection<File> arts = now.artifacts().values();
            for (File f : fetchPrevArtifacts(pkg).values()) {
                if (!arts.contains(f)) {
                    Log.debug("Removing "+f.getPath()+" since it is now superfluous!");
                    if (f.exists()) f.delete();
                }
            }
        }
    }

    /**
     * Return those files belonging to prev, but not now.
     */
    private Set<Source> calculateRemovedSources() {
        Set<Source> removed = new HashSet<>();
        for (String src : prev.sources().keySet()) {
            if (now.sources().get(src) == null) {
                removed.add(prev.sources().get(src));
            }
        }
        return removed;
    }

    /**
     * Return those files belonging to now, but not prev.
     */
    private Set<Source> calculateAddedSources() {
        Set<Source> added = new HashSet<>();
        for (String src : now.sources().keySet()) {
            if (prev.sources().get(src) == null) {
                added.add(now.sources().get(src));
            }
        }
        return added;
    }

    /**
     * Return those files where the timestamp is newer.
     * If a source file timestamp suddenly is older than what is known
     * about it in javac_state, then consider it modified, but print
     * a warning!
     */
    private Set<Source> calculateModifiedSources() {
        Set<Source> modified = new HashSet<>();
        for (String src : now.sources().keySet()) {
            Source n = now.sources().get(src);
            Source t = prev.sources().get(src);
            if (prev.sources().get(src) != null) {
                if (t != null) {
                    if (n.lastModified() > t.lastModified()) {
                        modified.add(n);
                    } else if (n.lastModified() < t.lastModified()) {
                        modified.add(n);
                        Log.warn("The source file "+n.name()+" timestamp has moved backwards in time.");
                    }
                }
            }
        }
        return modified;
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private void deleteContents(File dir) {
        if (dir != null && dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    deleteContents(f);
                }
                if (!options.isUnidentifiedArtifactPermitted(f.getAbsolutePath())) {
                    Log.debug("Removing "+f.getAbsolutePath());
                    f.delete();
                }
            }
        }
    }

    /**
     * Run the copy translator only.
     */
    public void performCopying(File binDir, Map<String,Transformer> suffixRules) {
        Map<String,Transformer> sr = new HashMap<>();
        for (Map.Entry<String,Transformer> e : suffixRules.entrySet()) {
            if (e.getValue().getClass().equals(CopyFile.class)) {
                sr.put(e.getKey(), e.getValue());
            }
        }
        perform(null, binDir, sr);
    }

    /**
     * Run all the translators that translate into java source code.
     * I.e. all translators that are not copy nor compile_java_source.
     */
    public void performTranslation(File gensrcDir, Map<String,Transformer> suffixRules) {
        Map<String,Transformer> sr = new HashMap<>();
        for (Map.Entry<String,Transformer> e : suffixRules.entrySet()) {
            Class<?> trClass = e.getValue().getClass();
            if (trClass == CompileJavaPackages.class || trClass == CopyFile.class)
                continue;

            sr.put(e.getKey(), e.getValue());
        }
        perform(null, gensrcDir, sr);
    }

    /**
     * Compile all the java sources. Return true, if it needs to be called again!
     */
    public boolean performJavaCompilations(CompilationService sjavac,
                                           Options args,
                                           Set<String> recentlyCompiled,
                                           boolean[] rcValue) {
        Map<String,Transformer> suffixRules = new HashMap<>();
        suffixRules.put(".java", compileJavaPackages);
        compileJavaPackages.setExtra(args);
        rcValue[0] = perform(sjavac, binDir, suffixRules);
        recentlyCompiled.addAll(taintedPackages());
        clearTaintedPackages();
        boolean again = !packagesWithChangedPublicApis.isEmpty();
        taintPackagesDependingOnChangedPackages(packagesWithChangedPublicApis, recentlyCompiled);
        packagesWithChangedPublicApis = new HashSet<>();
        return again && rcValue[0];

        // TODO: Figure out why 'again' checks packagesWithChangedPublicAPis.
        // (It shouldn't matter if packages had changed pub apis as long as no
        // one depends on them. Wouldn't it make more sense to let 'again'
        // depend on taintedPackages?)
    }

    /**
     * Store the source into the set of sources belonging to the given transform.
     */
    private void addFileToTransform(Map<Transformer,Map<String,Set<URI>>> gs, Transformer t, Source s) {
        Map<String,Set<URI>> fs = gs.get(t);
        if (fs == null) {
            fs = new HashMap<>();
            gs.put(t, fs);
        }
        Set<URI> ss = fs.get(s.pkg().name());
        if (ss == null) {
            ss = new HashSet<>();
            fs.put(s.pkg().name(), ss);
        }
        ss.add(s.file().toURI());
    }

    /**
     * For all packages, find all sources belonging to the package, group the sources
     * based on their transformers and apply the transformers on each source code group.
     */
    private boolean perform(CompilationService sjavac,
                            File outputDir,
                            Map<String,Transformer> suffixRules) {
        boolean rc = true;
        // Group sources based on transforms. A source file can only belong to a single transform.
        Map<Transformer,Map<String,Set<URI>>> groupedSources = new HashMap<>();
        for (Source src : now.sources().values()) {
            Transformer t = suffixRules.get(src.suffix());
            if (t != null) {
                if (taintedPackages.contains(src.pkg().name()) && !src.isLinkedOnly()) {
                    addFileToTransform(groupedSources, t, src);
                }
            }
        }
        // Go through the transforms and transform them.
        for (Map.Entry<Transformer, Map<String, Set<URI>>> e : groupedSources.entrySet()) {
            Transformer t = e.getKey();
            Map<String, Set<URI>> srcs = e.getValue();
            // These maps need to be synchronized since multiple threads will be
            // writing results into them.
            Map<String, Set<URI>> packageArtifacts = Collections.synchronizedMap(new HashMap<>());
            Map<String, Map<String, Set<String>>> packageDependencies = Collections.synchronizedMap(new HashMap<>());
            Map<String, Map<String, Set<String>>> packageCpDependencies = Collections.synchronizedMap(new HashMap<>());
            Map<String, PubApi> packagePublicApis = Collections.synchronizedMap(new HashMap<>());
            Map<String, PubApi> dependencyPublicApis = Collections.synchronizedMap(new HashMap<>());

            boolean r = t.transform(sjavac,
                                    srcs,
                                    visibleSrcs,
                                    prev.dependents(),
                                    outputDir.toURI(),
                                    packageArtifacts,
                                    packageDependencies,
                                    packageCpDependencies,
                                    packagePublicApis,
                                    dependencyPublicApis,
                                    0,
                                    isIncremental(),
                                    numCores);
            if (!r)
                rc = false;

            for (String p : srcs.keySet()) {
                recompiledPackages.add(p);
            }
            // The transform is done! Extract all the artifacts and store the info into the Package objects.
            for (Map.Entry<String, Set<URI>> a : packageArtifacts.entrySet()) {
                Module mnow = now.findModuleFromPackageName(a.getKey());
                mnow.addArtifacts(a.getKey(), a.getValue());
            }
            // Extract all the dependencies and store the info into the Package objects.
            for (Map.Entry<String, Map<String, Set<String>>> a : packageDependencies.entrySet()) {
                Map<String, Set<String>> deps = a.getValue();
                Module mnow = now.findModuleFromPackageName(a.getKey());
                mnow.setDependencies(a.getKey(), deps, false);
            }
            for (Map.Entry<String, Map<String, Set<String>>> a : packageCpDependencies.entrySet()) {
                Map<String, Set<String>> deps = a.getValue();
                Module mnow = now.findModuleFromPackageName(a.getKey());
                mnow.setDependencies(a.getKey(), deps, true);
            }

            // This map contains the public api of the types that this
            // compilation depended upon. This means that it may not contain
            // full packages. In other words, we shouldn't remove knowledge of
            // public apis but merge these with what we already have.
            for (Map.Entry<String, PubApi> a : dependencyPublicApis.entrySet()) {
                String pkg = a.getKey();
                PubApi packagePartialPubApi = a.getValue();
                Package pkgNow = now.findModuleFromPackageName(pkg).lookupPackage(pkg);
                PubApi currentPubApi = pkgNow.getPubApi();
                PubApi newPubApi = PubApi.mergeTypes(currentPubApi, packagePartialPubApi);
                pkgNow.setPubapi(newPubApi);

                // See JDK-8071904
                if (now.packages().containsKey(pkg))
                    now.packages().get(pkg).setPubapi(newPubApi);
                else
                    now.packages().put(pkg, pkgNow);
            }

            // The packagePublicApis cover entire packages (since sjavac compiles
            // stuff on package level). This means that if a type is missing
            // in the public api of a given package, it means that it has been
            // removed. In other words, we should *set* the pubapi to whatever
            // this map contains, and not merge it with what we already have.
            for (Map.Entry<String, PubApi> a : packagePublicApis.entrySet()) {
                String pkg = a.getKey();
                PubApi newPubApi = a.getValue();
                Module mprev = prev.findModuleFromPackageName(pkg);
                Module mnow = now.findModuleFromPackageName(pkg);
                mnow.setPubapi(pkg, newPubApi);
                if (mprev.hasPubapiChanged(pkg, newPubApi)) {
                    // Aha! The pubapi of this package has changed!
                    // It can also be a new compile from scratch.
                    if (mprev.lookupPackage(pkg).existsInJavacState()) {
                        // This is an incremental compile! The pubapi
                        // did change. Trigger recompilation of dependents.
                        packagesWithChangedPublicApis.add(pkg);
                        Log.debug("The API of " + Util.justPackageName(pkg) + " has changed!");
                    }
                }
            }
        }
        return rc;
    }

    /**
     * Utility method to recursively find all files below a directory.
     */
    private static Set<File> findAllFiles(File dir) {
        Set<File> foundFiles = new HashSet<>();
        if (dir == null) {
            return foundFiles;
        }
        recurse(dir, foundFiles);
        return foundFiles;
    }

    private static void recurse(File dir, Set<File> foundFiles) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                foundFiles.add(f);
            } else if (f.isDirectory()) {
                recurse(f, foundFiles);
            }
        }
    }

    /**
     * Compare the calculate source list, with an explicit list, usually
     * supplied from the makefile. Used to detect bugs where the makefile and
     * sjavac have different opinions on which files should be compiled.
     */
    public void compareWithMakefileList(File makefileSourceList)
            throws ProblemException {
        // If we are building on win32 using for example cygwin the paths in the
        // makefile source list
        // might be /cygdrive/c/.... which does not match c:\....
        // We need to adjust our calculated sources to be identical, if
        // necessary.
        boolean mightNeedRewriting = File.pathSeparatorChar == ';';

        if (makefileSourceList == null)
            return;

        Set<String> calculatedSources = new HashSet<>();
        Set<String> listedSources = new HashSet<>();

        // Create a set of filenames with full paths.
        for (Source s : now.sources().values()) {
            // Don't include link only sources when comparing sources to compile
            if (!s.isLinkedOnly()) {
                String path = s.file().getPath();
                if (mightNeedRewriting)
                    path = Util.normalizeDriveLetter(path);
                calculatedSources.add(path);
            }
        }
        // Read in the file and create another set of filenames with full paths.
        try(BufferedReader in = new BufferedReader(new FileReader(makefileSourceList))) {
            for (;;) {
                String l = in.readLine();
                if (l==null) break;
                l = l.trim();
                if (mightNeedRewriting) {
                    if (l.indexOf(":") == 1 && l.indexOf("\\") == 2) {
                        // Everything a-ok, the format is already C:\foo\bar
                    } else if (l.indexOf(":") == 1 && l.indexOf("/") == 2) {
                        // The format is C:/foo/bar, rewrite into the above format.
                        l = l.replaceAll("/","\\\\");
                    } else if (l.charAt(0) == '/' && l.indexOf("/",1) != -1) {
                        // The format might be: /cygdrive/c/foo/bar, rewrite into the above format.
                        // Do not hardcode the name cygdrive here.
                        int slash = l.indexOf("/",1);
                        l = l.replaceAll("/","\\\\");
                        l = ""+l.charAt(slash+1)+":"+l.substring(slash+2);
                    }
                    if (Character.isLowerCase(l.charAt(0))) {
                        l = Character.toUpperCase(l.charAt(0))+l.substring(1);
                    }
                }
                listedSources.add(l);
            }
        } catch (FileNotFoundException | NoSuchFileException e) {
            throw new ProblemException("Could not open "+makefileSourceList.getPath()+" since it does not exist!");
        } catch (IOException e) {
            throw new ProblemException("Could not read "+makefileSourceList.getPath());
        }

        for (String s : listedSources) {
            if (!calculatedSources.contains(s)) {
                 throw new ProblemException("The makefile listed source "+s+" was not calculated by the smart javac wrapper!");
            }
        }

        for (String s : calculatedSources) {
            if (!listedSources.contains(s)) {
                throw new ProblemException("The smart javac wrapper calculated source "+s+" was not listed by the makefiles!");
            }
        }
    }
}
