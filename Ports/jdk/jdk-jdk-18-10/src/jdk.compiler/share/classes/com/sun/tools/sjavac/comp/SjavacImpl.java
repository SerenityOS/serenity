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

package com.sun.tools.sjavac.comp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
import com.sun.tools.sjavac.JavacState;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Module;
import com.sun.tools.sjavac.ProblemException;
import com.sun.tools.sjavac.Source;
import com.sun.tools.sjavac.Transformer;
import com.sun.tools.sjavac.Util;
import com.sun.tools.sjavac.options.Option;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.options.SourceLocation;
import com.sun.tools.sjavac.server.Sjavac;
import java.io.UncheckedIOException;

import javax.tools.JavaFileManager;

/**
 * The sjavac implementation that interacts with javac and performs the actual
 * compilation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SjavacImpl implements Sjavac {

    @Override
    public Result compile(String[] args) {
        Options options;
        try {
            options = Options.parseArgs(args);
        } catch (IllegalArgumentException e) {
            Log.error(e.getMessage());
            return Result.CMDERR;
        }

        if (!validateOptions(options))
            return Result.CMDERR;

        if (srcDstOverlap(options.getSources(), options.getDestDir())) {
            return Result.CMDERR;
        }

        if (!createIfMissing(options.getDestDir()))
            return Result.ERROR;

        Path stateDir = options.getStateDir();
        if (stateDir != null && !createIfMissing(options.getStateDir()))
            return Result.ERROR;

        Path gensrc = options.getGenSrcDir();
        if (gensrc != null && !createIfMissing(gensrc))
            return Result.ERROR;

        Path hdrdir = options.getHeaderDir();
        if (hdrdir != null && !createIfMissing(hdrdir))
            return Result.ERROR;

        if (stateDir == null) {
            // Prepare context. Direct logging to our byte array stream.
            Context context = new Context();
            StringWriter strWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(strWriter);
            com.sun.tools.javac.util.Log.preRegister(context, printWriter);
            JavacFileManager.preRegister(context);

            // Prepare arguments
            String[] passThroughArgs = Stream.of(args)
                                             .filter(arg -> !arg.startsWith(Option.SERVER.arg))
                                             .toArray(String[]::new);
            // Compile
            Result result = new Main("javac", printWriter).compile(passThroughArgs, context);

            // Process compiler output (which is always errors)
            printWriter.flush();
            Util.getLines(strWriter.toString()).forEach(Log::error);

            // Clean up
            JavaFileManager fileManager = context.get(JavaFileManager.class);
            if (fileManager instanceof JavacFileManager javacFileManager) {
                try {
                    javacFileManager.close();
                } catch (IOException es) {
                    throw new UncheckedIOException(es);
                }
            }
            return result;

        } else {
            // Load the prev build state database.
            JavacState javac_state = JavacState.load(options);

            // Setup the suffix rules from the command line.
            Map<String, Transformer> suffixRules = new HashMap<>();

            // Handling of .java-compilation
            suffixRules.putAll(javac_state.getJavaSuffixRule());

            // Handling of -copy and -tr
            suffixRules.putAll(options.getTranslationRules());

            // All found modules are put here.
            Map<String,Module> modules = new HashMap<>();
            // We start out in the legacy empty no-name module.
            // As soon as we stumble on a module-info.java file we change to that module.
            Module current_module = new Module("", "");
            modules.put("", current_module);

            try {
                // Find all sources, use the suffix rules to know which files are sources.
                Map<String,Source> sources = new HashMap<>();

                // Find the files, this will automatically populate the found modules
                // with found packages where the sources are found!
                findSourceFiles(options.getSources(),
                                suffixRules.keySet(),
                                sources,
                                modules,
                                current_module,
                                options.isDefaultPackagePermitted(),
                                false);

                if (sources.isEmpty()) {
                    Log.error("Found nothing to compile!");
                    return Result.ERROR;
                }


                // Create a map of all source files that are available for linking. Both -src and
                // -sourcepath point to such files. It is possible to specify multiple
                // -sourcepath options to enable different filtering rules. If the
                // filters are the same for multiple sourcepaths, they may be concatenated
                // using :(;). Before sending the list of sourcepaths to javac, they are
                // all concatenated. The list created here is used by the SmartFileWrapper to
                // make sure only the correct sources are actually available.
                // We might find more modules here as well.
                Map<String,Source> sources_to_link_to = new HashMap<>();

                List<SourceLocation> sourceResolutionLocations = new ArrayList<>();
                sourceResolutionLocations.addAll(options.getSources());
                sourceResolutionLocations.addAll(options.getSourceSearchPaths());
                findSourceFiles(sourceResolutionLocations,
                                Collections.singleton(".java"),
                                sources_to_link_to,
                                modules,
                                current_module,
                                options.isDefaultPackagePermitted(),
                                true);

                // Add the set of sources to the build database.
                javac_state.now().flattenPackagesSourcesAndArtifacts(modules);
                javac_state.now().checkInternalState("checking sources", false, sources);
                javac_state.now().checkInternalState("checking linked sources", true, sources_to_link_to);
                javac_state.setVisibleSources(sources_to_link_to);

                int round = 0;
                printRound(round);

                // If there is any change in the source files, taint packages
                // and mark the database in need of saving.
                javac_state.checkSourceStatus(false);

                // Find all existing artifacts. Their timestamp will match the last modified timestamps stored
                // in javac_state, simply because loading of the JavacState will clean out all artifacts
                // that do not match the javac_state database.
                javac_state.findAllArtifacts();

                // Remove unidentified artifacts from the bin, gensrc and header dirs.
                // (Unless we allow them to be there.)
                // I.e. artifacts that are not known according to the build database (javac_state).
                // For examples, files that have been manually copied into these dirs.
                // Artifacts with bad timestamps (ie the on disk timestamp does not match the timestamp
                // in javac_state) have already been removed when the javac_state was loaded.
                if (!options.areUnidentifiedArtifactsPermitted()) {
                    javac_state.removeUnidentifiedArtifacts();
                }
                // Go through all sources and taint all packages that miss artifacts.
                javac_state.taintPackagesThatMissArtifacts();

                // Check recorded classpath public apis. Taint packages that depend on
                // classpath classes whose public apis have changed.
                javac_state.taintPackagesDependingOnChangedClasspathPackages();

                // Now clean out all known artifacts belonging to tainted packages.
                javac_state.deleteClassArtifactsInTaintedPackages();
                // Copy files, for example property files, images files, xml files etc etc.
                javac_state.performCopying(Util.pathToFile(options.getDestDir()), suffixRules);
                // Translate files, for example compile properties or compile idls.
                javac_state.performTranslation(Util.pathToFile(gensrc), suffixRules);
                // Add any potentially generated java sources to the tobe compiled list.
                // (Generated sources must always have a package.)
                Map<String,Source> generated_sources = new HashMap<>();

                Source.scanRoot(Util.pathToFile(options.getGenSrcDir()),
                                Util.set(".java"),
                                Collections.emptyList(),
                                Collections.emptyList(),
                                generated_sources,
                                modules,
                                current_module,
                                false,
                                true,
                                false);
                javac_state.now().flattenPackagesSourcesAndArtifacts(modules);
                // Recheck the the source files and their timestamps again.
                javac_state.checkSourceStatus(true);

                // Now do a safety check that the list of source files is identical
                // to the list Make believes we are compiling. If we do not get this
                // right, then incremental builds will fail with subtility.
                // If any difference is detected, then we will fail hard here.
                // This is an important safety net.
                javac_state.compareWithMakefileList(Util.pathToFile(options.getSourceReferenceList()));

                // Do the compilations, repeatedly until no tainted packages exist.
                boolean again;
                // Collect the name of all compiled packages.
                Set<String> recently_compiled = new HashSet<>();
                boolean[] rc = new boolean[1];

                CompilationService compilationService = new CompilationService();
                do {
                    if (round > 0)
                        printRound(round);
                    // Clean out artifacts in tainted packages.
                    javac_state.deleteClassArtifactsInTaintedPackages();
                    again = javac_state.performJavaCompilations(compilationService,
                                                                options,
                                                                recently_compiled,
                                                                rc);
                    if (!rc[0]) {
                        Log.debug("Compilation failed.");
                        break;
                    }
                    if (!again) {
                        Log.debug("Nothing left to do.");
                    }
                    round++;
                } while (again);
                Log.debug("No need to do another round.");

                // Only update the state if the compile went well.
                if (rc[0]) {
                    javac_state.save();
                    // Reflatten only the artifacts.
                    javac_state.now().flattenArtifacts(modules);
                    // Remove artifacts that were generated during the last compile, but not this one.
                    javac_state.removeSuperfluousArtifacts(recently_compiled);
                }

                return rc[0] ? Result.OK : Result.ERROR;
            } catch (ProblemException e) {
                // For instance make file list mismatch.
                Log.error(e.getMessage());
                Log.debug(e);
                return Result.ERROR;
            } catch (Exception e) {
                Log.error(e);
                return Result.ERROR;
            }
        }
    }

    @Override
    public void shutdown() {
        // Nothing to clean up
    }

    private static boolean validateOptions(Options options) {

        String err = null;

        if (options.getDestDir() == null) {
            err = "Please specify output directory.";
        } else if (options.isJavaFilesAmongJavacArgs()) {
            err = "Sjavac does not handle explicit compilation of single .java files.";
        } else if (!options.getImplicitPolicy().equals("none")) {
            err = "The only allowed setting for sjavac is -implicit:none";
        } else if (options.getSources().isEmpty() && options.getStateDir() != null) {
            err = "You have to specify -src when using --state-dir.";
        } else if (options.getTranslationRules().size() > 1
                && options.getGenSrcDir() == null) {
            err = "You have translators but no gensrc dir (-s) specified!";
        }

        if (err != null)
            Log.error(err);

        return err == null;

    }

    private static boolean srcDstOverlap(List<SourceLocation> locs, Path dest) {
        for (SourceLocation loc : locs) {
            if (isOverlapping(loc.getPath(), dest)) {
                Log.error("Source location " + loc.getPath() + " overlaps with destination " + dest);
                return true;
            }
        }
        return false;
    }

    private static boolean isOverlapping(Path p1, Path p2) {
        p1 = p1.toAbsolutePath().normalize();
        p2 = p2.toAbsolutePath().normalize();
        return p1.startsWith(p2) || p2.startsWith(p1);
    }

    private static boolean createIfMissing(Path dir) {

        if (Files.isDirectory(dir))
            return true;

        if (Files.exists(dir)) {
            Log.error(dir + " is not a directory.");
            return false;
        }

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Log.error("Could not create directory: " + e.getMessage());
            return false;
        }

        return true;
    }

    /** Find source files in the given source locations. */
    public static void findSourceFiles(List<SourceLocation> sourceLocations,
                                       Set<String> sourceTypes,
                                       Map<String,Source> foundFiles,
                                       Map<String, Module> foundModules,
                                       Module currentModule,
                                       boolean permitSourcesInDefaultPackage,
                                       boolean inLinksrc)
                                               throws IOException {

        for (SourceLocation source : sourceLocations) {
            source.findSourceFiles(sourceTypes,
                                   foundFiles,
                                   foundModules,
                                   currentModule,
                                   permitSourcesInDefaultPackage,
                                   inLinksrc);
        }
    }

    private static void printRound(int round) {
        Log.debug("****************************************");
        Log.debug("* Round " + round + "                              *");
        Log.debug("****************************************");
    }
}
