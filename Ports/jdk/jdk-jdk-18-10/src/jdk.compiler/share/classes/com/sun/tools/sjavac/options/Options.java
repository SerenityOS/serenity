/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.options;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.StringJoiner;

import com.sun.tools.sjavac.Transformer;
import com.sun.tools.sjavac.Util;

/**
 * Instances of this class represent values for sjavac command line options.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Options {

    // Output directories
    private Path destDir, genSrcDir, headerDir, stateDir;

    // Input directories
    private List<SourceLocation> sources = new ArrayList<>();
    private List<SourceLocation> sourceSearchPaths = new ArrayList<>();
    private List<SourceLocation> classSearchPaths = new ArrayList<>();
    private List<SourceLocation> moduleSearchPaths = new ArrayList<>();

    private String logLevel = "info";

    private Set<String> permitted_artifacts = new HashSet<>();
    private boolean permitUnidentifiedArtifacts = false;
    private boolean permitSourcesInDefaultPackage = false;

    private Path sourceReferenceList;
    private int numCores = 4;
    private String implicitPolicy = "none";
    private List<String> javacArgs = new ArrayList<>();

    private Map<String, Transformer> trRules = new HashMap<>();

    private boolean startServer = false;

    // Server configuration string
    private String serverConf;

    /** Get the policy for implicit classes */
    public String getImplicitPolicy() {
        return implicitPolicy;
    }

    /** Get the path for generated sources (or null if no such path is set) */
    public Path getGenSrcDir() {
        return genSrcDir;
    }

    /** Get the path for the destination directory */
    public Path getDestDir() {
        return destDir;
    }

    /** Get the path for the header directory (or null if no such path is set) */
    public Path getHeaderDir() {
        return headerDir;
    }

    /** Get the path for the state directory, defaults to destDir. */
    public Path getStateDir() {
        return stateDir;
    }

    /** Get all source locations for files to be compiled */
    public List<SourceLocation> getSources() {
        return sources;
    }

    /**
     * Get all paths to search for classes in .java format. (Java-files in
     * found here should not be compiled.
     */
    public List<SourceLocation> getSourceSearchPaths() {
        return sourceSearchPaths;
    }

    /** Get all paths to search for classes in. */
    public List<SourceLocation> getClassSearchPath() {
        return classSearchPaths;
    }

    /** Get all paths to search for modules in. */
    public List<SourceLocation> getModuleSearchPaths() {
        return moduleSearchPaths;
    }

    /** Get the log level. */
    public String getLogLevel() {
        return logLevel;
    }

    /** Returns true iff the artifact is permitted in the output dir. */
    public boolean isUnidentifiedArtifactPermitted(String f) {
        return permitted_artifacts.contains(f);
    }

    /** Returns true iff artifacts in the output directories should be kept,
     * even if they would not be generated in a clean build. */
    public boolean areUnidentifiedArtifactsPermitted() {
        return permitUnidentifiedArtifacts;
    }

    /** Returns true iff sources in the default package should be permitted. */
    public boolean isDefaultPackagePermitted() {
        return permitSourcesInDefaultPackage;
    }

    /** Get the path to the list of reference sources (or null if none is set) */
    public Path getSourceReferenceList() {
        return sourceReferenceList;
    }

    /** Get the number of cores to be used by sjavac */
    public int getNumCores() {
        return numCores;
    }

    /** Returns all arguments relevant to javac but irrelevant to sjavac. */
    public List<String> getJavacArgs() {
        return javacArgs;
    }

    /**
     * Get a map which maps suffixes to transformers (for example
     * ".java" {@literal ->} CompileJavaPackages)
     */
    public Map<String, Transformer> getTranslationRules() {
        return trRules;
    }

    /** Return true iff a new server should be started */
    public boolean startServerFlag() {
        return startServer;
    }

    /** Return the server configuration string. */
    public String getServerConf() {
        return serverConf;
    }

    /**
     * Parses the given argument array and returns a corresponding Options
     * instance.
     */
    public static Options parseArgs(String... args) {
        Options options = new Options();
        options.new ArgDecoderOptionHelper().traverse(args);
        return options;
    }

    /** Returns true iff a .java file is among the javac arguments */
    public boolean isJavaFilesAmongJavacArgs() {
        for (String javacArg : javacArgs)
            if (javacArg.endsWith(".java"))
                return true;
        return false;
    }

    /**
     * Returns a string representation of the options that affect the result of
     * the compilation. (Used for saving the state of the options used in a
     * previous compile.)
     */
    public String getStateArgsString() {

        // Local utility class for collecting the arguments
        class StateArgs {

            private List<String> args = new ArrayList<>();

            void addArg(Option opt) {
                args.add(opt.arg);
            }

            void addArg(Option opt, Object val) {
                addArg(opt);
                args.add(val.toString());
            }

            void addSourceLocations(Option opt, List<SourceLocation> locs) {
                for (SourceLocation sl : locs) {
                    for (String pkg : sl.includes) addArg(Option.I, pkg);
                    for (String pkg : sl.excludes) addArg(Option.X, pkg);
                    addArg(opt, sl.getPath());
                }
            }

            String getResult() {
                return String.join(" ", args);
            }

            public void addAll(Collection<String> toAdd) {
                args.addAll(toAdd);
            }
        }

        StateArgs args = new StateArgs();

        // Directories
        if (genSrcDir != null)
            args.addArg(Option.S, genSrcDir.normalize());

        if (headerDir != null)
            args.addArg(Option.H, headerDir.normalize());

        if (destDir != null)
            args.addArg(Option.D, destDir.normalize());

        if (stateDir != null)
            args.addArg(Option.STATE_DIR, stateDir.normalize());

        // Source roots
        args.addSourceLocations(Option.SRC, sources);
        args.addSourceLocations(Option.SOURCE_PATH, sourceSearchPaths);
        args.addSourceLocations(Option.CLASS_PATH,  classSearchPaths);
        args.addSourceLocations(Option.MODULE_PATH, moduleSearchPaths);

        // Boolean options
        if (permitSourcesInDefaultPackage)
            args.addArg(Option.PERMIT_SOURCES_WITHOUT_PACKAGE);

        for (String f : permitted_artifacts) {
            args.addArg(Option.PERMIT_ARTIFACT, f);
        }

        if (permitUnidentifiedArtifacts)
            args.addArg(Option.PERMIT_UNIDENTIFIED_ARTIFACTS);

        // Translation rules
        for (Map.Entry<String, Transformer> tr : trRules.entrySet()) {
            String val = tr.getKey() + "=" + tr.getValue().getClass().getName();
            args.addArg(Option.TR, val);
        }

        // Javac args
        args.addAll(javacArgs);

        return args.getResult();
    }


    /** Extract the arguments to be passed on to javac. */
    public String[] prepJavacArgs() {
        List<String> args = new ArrayList<>();

        // Output directories
        args.add("-d");
        args.add(destDir.toString());

        if (getGenSrcDir() != null) {
            args.add("-s");
            args.add(genSrcDir.toString());
        }

        if (headerDir != null) {
            args.add("-h");
            args.add(headerDir.toString());
        }

        // Prep sourcepath
        List<SourceLocation> sourcepath = new ArrayList<>();
        sourcepath.addAll(sources);
        sourcepath.addAll(sourceSearchPaths);
        if (sourcepath.size() > 0) {
            args.add("-sourcepath");
            args.add(concatenateSourceLocations(sourcepath));
        }

        // Prep classpath
        if (classSearchPaths.size() > 0) {
            args.add("-classpath");
            args.add(concatenateSourceLocations(classSearchPaths));
        }

        // Enable dependency generation
        args.add("--debug=completionDeps=source,class");

        // This can't be anything but 'none'. Enforced by sjavac main method.
        args.add("-implicit:" + implicitPolicy);

        // If this option is not used, Object for instance is erroneously
        // picked up from PLATFORM_CLASS_PATH instead of CLASS_PATH.
        //
        // Discussing this further led to the decision of letting bootclasspath
        // be a dummy (empty) directory when building the JDK.
        //args.add("-XXuserPathsFirst");

        // Append javac-options (i.e. pass through options not recognized by
        // sjavac to javac.)
        args.addAll(javacArgs);

        return args.toArray(new String[args.size()]);
    }

    // Helper method to join a list of source locations separated by
    // File.pathSeparator
    private static String concatenateSourceLocations(List<SourceLocation> locs) {
        StringJoiner joiner = new StringJoiner(java.io.File.pathSeparator);
        for (SourceLocation loc : locs) {
            joiner.add(loc.getPath().toString());
        }
        return joiner.toString();
    }

    // OptionHelper that records the traversed options in this Options instance.
    private class ArgDecoderOptionHelper extends OptionHelper {

        List<String> includes, excludes, includeFiles, excludeFiles;
        {
            resetFilters();
        }

        boolean headerProvided = false;
        boolean genSrcProvided = false;
        boolean stateProvided = false;

        @Override
        public void reportError(String msg) {
            throw new IllegalArgumentException(msg);
        }

        @Override
        public void sourceRoots(List<Path> paths) {
            sources.addAll(createSourceLocations(paths));
        }

        @Override
        public void exclude(String exclPattern) {
            exclPattern = Util.normalizeDriveLetter(exclPattern);
            excludes.add(exclPattern);
        }

        @Override
        public void include(String inclPattern) {
            inclPattern = Util.normalizeDriveLetter(inclPattern);
            includes.add(inclPattern);
        }

        @Override
        public void addTransformer(String suffix, Transformer tr) {
            if (trRules.containsKey(suffix)) {
                reportError("More than one transformer specified for " +
                            "suffix " + suffix + ".");
                return;
            }
            trRules.put(suffix, tr);
        }

        @Override
        public void sourcepath(List<Path> paths) {
            sourceSearchPaths.addAll(createSourceLocations(paths));
        }

        @Override
        public void modulepath(List<Path> paths) {
            moduleSearchPaths.addAll(createSourceLocations(paths));
        }

        @Override
        public void classpath(List<Path> paths) {
            classSearchPaths.addAll(createSourceLocations(paths));
        }

        @Override
        public void numCores(int n) {
            numCores = n;
        }

        @Override
        public void logLevel(String level) {
            logLevel = level;
        }

        @Override
        public void compareFoundSources(Path referenceList) {
            sourceReferenceList = referenceList;
        }

        @Override
        public void permitArtifact(String f) {
            permitted_artifacts.add(f);
        }

        @Override
        public void permitUnidentifiedArtifacts() {
            permitUnidentifiedArtifacts = true;
        }

        @Override
        public void permitDefaultPackage() {
            permitSourcesInDefaultPackage = true;
        }

        @Override
        public void serverConf(String conf) {
            if (serverConf != null)
                reportError("Can not specify more than one server configuration.");
            else
                serverConf = conf;
        }

        @Override
        public void implicit(String policy) {
            implicitPolicy = policy;
        }

        @Override
        public void startServerConf(String conf) {
            if (serverConf != null)
                reportError("Can not specify more than one server configuration.");
            else {
                startServer = true;
                serverConf = conf;
            }
        }

        @Override
        public void javacArg(String... arg) {
            javacArgs.addAll(Arrays.asList(arg));
        }

        @Override
        public void destDir(Path dir) {
            if (destDir != null) {
                reportError("Destination directory already specified.");
                return;
            }
            destDir = dir.toAbsolutePath();
        }

        @Override
        public void generatedSourcesDir(Path dir) {
            if (genSrcProvided) {
                reportError("Directory for generated sources already specified.");
                return;
            }
            genSrcProvided = true;
            genSrcDir = dir.toAbsolutePath();
        }

        @Override
        public void headerDir(Path dir) {
            if (headerProvided) {
                reportError("Header directory already specified.");
                return;
            }
            headerProvided = true;
            headerDir = dir.toAbsolutePath();
        }

        @Override
        public void stateDir(Path dir) {
            if (stateProvided) {
                reportError("State directory already specified.");
                return;
            }
            stateProvided = true;
            stateDir = dir.toAbsolutePath();
        }

        private List<SourceLocation> createSourceLocations(List<Path> paths) {
            List<SourceLocation> result = new ArrayList<>();
            for (Path path : paths) {
                result.add(new SourceLocation(
                        path,
                        includes,
                        excludes));
            }
            resetFilters();
            return result;
        }

        private void resetFilters() {
            includes = new ArrayList<>();
            excludes = new ArrayList<>();
            includeFiles = new ArrayList<>();
            excludeFiles = new ArrayList<>();
        }
    }

}
