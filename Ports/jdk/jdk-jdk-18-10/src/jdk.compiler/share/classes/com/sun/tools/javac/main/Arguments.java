/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.tools.doclint.DocLint;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.file.BaseFileManager;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Profile;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.OptionHelper.GrumpyHelper;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformUtils;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticInfo;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.PrefixKind;
import com.sun.tools.javac.util.Log.WriterKind;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.PropagatedException;

/**
 * Shared option and argument handling for command line and API usage of javac.
 */
public class Arguments {

    /**
     * The context key for the arguments.
     */
    public static final Context.Key<Arguments> argsKey = new Context.Key<>();

    private String ownName;
    private Set<String> classNames;
    private Set<Path> files;
    private Map<Option, String> deferredFileManagerOptions;
    private Set<JavaFileObject> fileObjects;
    private boolean emptyAllowed;
    private final Options options;

    private JavaFileManager fileManager;
    private final Log log;
    private final Context context;

    private enum ErrorMode { ILLEGAL_ARGUMENT, ILLEGAL_STATE, LOG };
    private ErrorMode errorMode;
    private boolean errors;

    /**
     * Gets the Arguments instance for this context.
     *
     * @param context the content
     * @return the Arguments instance for this context.
     */
    public static Arguments instance(Context context) {
        Arguments instance = context.get(argsKey);
        if (instance == null) {
            instance = new Arguments(context);
        }
        return instance;
    }

    protected Arguments(Context context) {
        context.put(argsKey, this);
        options = Options.instance(context);
        log = Log.instance(context);
        this.context = context;

        // Ideally, we could init this here and update/configure it as
        // needed, but right now, initializing a file manager triggers
        // initialization of other items in the context, such as Lint
        // and FSInfo, which should not be initialized until after
        // processArgs
        //        fileManager = context.get(JavaFileManager.class);
    }

    private final OptionHelper cmdLineHelper = new OptionHelper() {
        @Override
        public String get(Option option) {
            return options.get(option);
        }

        @Override
        public void put(String name, String value) {
            options.put(name, value);
        }

        @Override
        public void remove(String name) {
            options.remove(name);
        }

        @Override
        public boolean handleFileManagerOption(Option option, String value) {
            options.put(option, value);
            deferredFileManagerOptions.put(option, value);
            return true;
        }

        @Override
        public Log getLog() {
            return log;
        }

        @Override
        public String getOwnName() {
            return ownName;
        }

        @Override
        public void addFile(Path p) {
            files.add(p);
        }

        @Override
        public void addClassName(String s) {
            classNames.add(s);
        }

    };

    /**
     * Initializes this Args instance with a set of command line args.
     * The args will be processed in conjunction with the full set of
     * command line options, including -help, -version etc.
     * The args may also contain class names and filenames.
     * Any errors during this call, and later during validate, will be reported
     * to the log.
     * @param ownName the name of this tool; used to prefix messages
     * @param args the args to be processed
     */
    public void init(String ownName, Iterable<String> args) {
        this.ownName = ownName;
        errorMode = ErrorMode.LOG;
        files = new LinkedHashSet<>();
        deferredFileManagerOptions = new LinkedHashMap<>();
        fileObjects = null;
        classNames = new LinkedHashSet<>();
        processArgs(args, Option.getJavaCompilerOptions(), cmdLineHelper, true, false);
        if (errors) {
            log.printLines(PrefixKind.JAVAC, "msg.usage", ownName);
        }
    }

    private final OptionHelper apiHelper = new GrumpyHelper(null) {
        @Override
        public String get(Option option) {
            return options.get(option);
        }

        @Override
        public void put(String name, String value) {
            options.put(name, value);
        }

        @Override
        public void remove(String name) {
            options.remove(name);
        }

        @Override
        public Log getLog() {
            return Arguments.this.log;
        }
    };

    /**
     * Initializes this Args instance with the parameters for a JavacTask.
     * The options will be processed in conjunction with the restricted set
     * of tool options, which does not include -help, -version, etc,
     * nor does it include classes and filenames, which should be specified
     * separately.
     * File manager options are handled directly by the file manager.
     * Any errors found while processing individual args will be reported
     * via IllegalArgumentException.
     * Any subsequent errors during validate will be reported via IllegalStateException.
     * @param ownName the name of this tool; used to prefix messages
     * @param options the options to be processed
     * @param classNames the classes to be subject to annotation processing
     * @param files the files to be compiled
     */
    public void init(String ownName,
            Iterable<String> options,
            Iterable<String> classNames,
            Iterable<? extends JavaFileObject> files) {
        this.ownName = ownName;
        this.classNames = toSet(classNames);
        this.fileObjects = toSet(files);
        this.files = null;
        errorMode = ErrorMode.ILLEGAL_ARGUMENT;
        if (options != null) {
            processArgs(toList(options), Option.getJavacToolOptions(), apiHelper, false, true);
        }
        errorMode = ErrorMode.ILLEGAL_STATE;
    }

    /**
     * Minimal initialization for tools, like javadoc,
     * to be able to process javac options for themselves,
     * and then call validate.
     * @param ownName  the name of this tool; used to prefix messages
     */
    public void init(String ownName) {
        this.ownName = ownName;
        errorMode = ErrorMode.LOG;
    }

    /**
     * Gets the files to be compiled.
     * @return the files to be compiled
     */
    public Set<JavaFileObject> getFileObjects() {
        if (fileObjects == null) {
            fileObjects = new LinkedHashSet<>();
        }
        if (files != null) {
            JavacFileManager jfm = (JavacFileManager) getFileManager();
            for (JavaFileObject fo: jfm.getJavaFileObjectsFromPaths(files))
                fileObjects.add(fo);
        }
        return fileObjects;
    }

    /**
     * Gets the classes to be subject to annotation processing.
     * @return the classes to be subject to annotation processing
     */
    public Set<String> getClassNames() {
        return classNames;
    }

    /**
     * Handles the {@code --release} option.
     *
     * @param additionalOptions a predicate to handle additional options implied by the
     * {@code --release} option. The predicate should return true if all the additional
     * options were processed successfully.
     * @return true if successful, false otherwise
     */
    public boolean handleReleaseOptions(Predicate<Iterable<String>> additionalOptions) {
        String platformString = options.get(Option.RELEASE);

        checkOptionAllowed(platformString == null,
                option -> reportDiag(Errors.ReleaseBootclasspathConflict(option)),
                Option.BOOT_CLASS_PATH, Option.XBOOTCLASSPATH, Option.XBOOTCLASSPATH_APPEND,
                Option.XBOOTCLASSPATH_PREPEND,
                Option.ENDORSEDDIRS, Option.DJAVA_ENDORSED_DIRS,
                Option.EXTDIRS, Option.DJAVA_EXT_DIRS,
                Option.SOURCE, Option.TARGET,
                Option.SYSTEM, Option.UPGRADE_MODULE_PATH);

        if (platformString != null) {
            PlatformDescription platformDescription =
                    PlatformUtils.lookupPlatformDescription(platformString);

            if (platformDescription == null) {
                reportDiag(Errors.UnsupportedReleaseVersion(platformString));
                return false;
            }

            options.put(Option.SOURCE, platformDescription.getSourceVersion());
            options.put(Option.TARGET, platformDescription.getTargetVersion());

            context.put(PlatformDescription.class, platformDescription);

            if (!additionalOptions.test(platformDescription.getAdditionalOptions()))
                return false;

            JavaFileManager platformFM = platformDescription.getFileManager();
            DelegatingJavaFileManager.installReleaseFileManager(context,
                                                                platformFM,
                                                                getFileManager());
        }

        return true;
    }

    /**
     * Processes strings containing options and operands.
     * @param args the strings to be processed
     * @param allowableOpts the set of option declarations that are applicable
     * @param helper a help for use by Option.process
     * @param allowOperands whether or not to check for files and classes
     * @param checkFileManager whether or not to check if the file manager can handle
     *      options which are not recognized by any of allowableOpts
     * @return true if all the strings were successfully processed; false otherwise
     * @throws IllegalArgumentException if a problem occurs and errorMode is set to
     *      ILLEGAL_ARGUMENT
     */
    private boolean processArgs(Iterable<String> args,
            Set<Option> allowableOpts, OptionHelper helper,
            boolean allowOperands, boolean checkFileManager) {
        if (!doProcessArgs(args, allowableOpts, helper, allowOperands, checkFileManager))
            return false;

        if (!handleReleaseOptions(extra -> doProcessArgs(extra, allowableOpts, helper, allowOperands, checkFileManager)))
            return false;

        options.notifyListeners();

        return true;
    }

    private boolean doProcessArgs(Iterable<String> args,
            Set<Option> allowableOpts, OptionHelper helper,
            boolean allowOperands, boolean checkFileManager) {
        JavaFileManager fm = checkFileManager ? getFileManager() : null;
        Iterator<String> argIter = args.iterator();
        while (argIter.hasNext()) {
            String arg = argIter.next();
            if (arg.isEmpty()) {
                reportDiag(Errors.InvalidFlag(arg));
                return false;
            }

            Option option = null;

            // first, check the provided set of javac options
            if (arg.startsWith("-")) {
                option = Option.lookup(arg, allowableOpts);
            } else if (allowOperands && Option.SOURCEFILE.matches(arg)) {
                option = Option.SOURCEFILE;
            }

            if (option != null) {
                try {
                    option.handleOption(helper, arg, argIter);
                } catch (Option.InvalidValueException e) {
                    error(e);
                    return false;
                }
                continue;
            }

            // check file manager option
            if (fm != null && fm.handleOption(arg, argIter)) {
                continue;
            }

            // none of the above
            reportDiag(Errors.InvalidFlag(arg));
            return false;
        }

        return true;
    }

    /**
     * Validates the overall consistency of the options and operands
     * processed by processOptions.
     * @return true if all args are successfully validated; false otherwise.
     * @throws IllegalStateException if a problem is found and errorMode is set to
     *      ILLEGAL_STATE
     */
    public boolean validate() {
        JavaFileManager fm = getFileManager();
        if (options.isSet(Option.MODULE)) {
            if (!fm.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                log.error(Errors.OutputDirMustBeSpecifiedWithDashMOption);
            } else if (!fm.hasLocation(StandardLocation.MODULE_SOURCE_PATH)) {
                log.error(Errors.ModulesourcepathMustBeSpecifiedWithDashMOption);
            } else {
                java.util.List<String> modules = Arrays.asList(options.get(Option.MODULE).split(","));
                try {
                    for (String module : modules) {
                        Location sourceLoc = fm.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, module);
                        if (sourceLoc == null) {
                            log.error(Errors.ModuleNotFoundInModuleSourcePath(module));
                        } else {
                            Location classLoc = fm.getLocationForModule(StandardLocation.CLASS_OUTPUT, module);

                            for (JavaFileObject file : fm.list(sourceLoc, "", EnumSet.of(JavaFileObject.Kind.SOURCE), true)) {
                                String className = fm.inferBinaryName(sourceLoc, file);
                                JavaFileObject classFile = fm.getJavaFileForInput(classLoc, className, Kind.CLASS);

                                if (classFile == null || classFile.getLastModified() < file.getLastModified()) {
                                    if (fileObjects == null)
                                        fileObjects = new HashSet<>();
                                    fileObjects.add(file);
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    log.printLines(PrefixKind.JAVAC, "msg.io");
                    ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
                    return false;
                }
            }
        }

        if (isEmpty()) {
            // It is allowed to compile nothing if just asking for help or version info.
            // But also note that none of these options are supported in API mode.
            if (options.isSet(Option.HELP)
                    || options.isSet(Option.X)
                    || options.isSet(Option.HELP_LINT)
                    || options.isSet(Option.VERSION)
                    || options.isSet(Option.FULLVERSION)
                    || options.isSet(Option.MODULE)) {
                return true;
            }

            if (!emptyAllowed) {
                if (!errors) {
                    if (JavaCompiler.explicitAnnotationProcessingRequested(options)) {
                        reportDiag(Errors.NoSourceFilesClasses);
                    } else {
                        reportDiag(Errors.NoSourceFiles);
                    }
                }
                return false;
            }
        }

        if (!checkDirectory(Option.D)) {
            return false;
        }
        if (!checkDirectory(Option.S)) {
            return false;
        }
        if (!checkDirectory(Option.H)) {
            return false;
        }

        // The following checks are to help avoid accidental confusion between
        // directories of modules and exploded module directories.
        if (fm instanceof StandardJavaFileManager standardJavaFileManager) {
            if (standardJavaFileManager.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                Path outDir = standardJavaFileManager.getLocationAsPaths(StandardLocation.CLASS_OUTPUT).iterator().next();
                if (standardJavaFileManager.hasLocation(StandardLocation.MODULE_SOURCE_PATH)) {
                    // multi-module mode
                    if (Files.exists(outDir.resolve("module-info.class"))) {
                        log.error(Errors.MultiModuleOutdirCannotBeExplodedModule(outDir));
                    }
                } else {
                    // single-module or legacy mode
                    boolean lintPaths = options.isUnset(Option.XLINT_CUSTOM,
                            "-" + LintCategory.PATH.option);
                    if (lintPaths) {
                        Path outDirParent = outDir.getParent();
                        if (outDirParent != null && Files.exists(outDirParent.resolve("module-info.class"))) {
                            log.warning(LintCategory.PATH, Warnings.OutdirIsInExplodedModule(outDir));
                        }
                    }
                }
            }
        }


        String sourceString = options.get(Option.SOURCE);
        Source source = (sourceString != null)
                ? Source.lookup(sourceString)
                : Source.DEFAULT;
        String targetString = options.get(Option.TARGET);
        Target target = (targetString != null)
                ? Target.lookup(targetString)
                : Target.DEFAULT;

        // We don't check source/target consistency for CLDC, as J2ME
        // profiles are not aligned with J2SE targets; moreover, a
        // single CLDC target may have many profiles.  In addition,
        // this is needed for the continued functioning of the JSR14
        // prototype.
        if (Character.isDigit(target.name.charAt(0))) {
            if (target.compareTo(source.requiredTarget()) < 0) {
                if (targetString != null) {
                    if (sourceString == null) {
                        reportDiag(Warnings.TargetDefaultSourceConflict(targetString, source.requiredTarget()));
                    } else {
                        reportDiag(Warnings.SourceTargetConflict(sourceString, source.requiredTarget()));
                    }
                    return false;
                } else {
                    target = source.requiredTarget();
                    options.put("-target", target.name);
                }
            }
        }

        if (options.isSet(Option.PREVIEW)) {
            if (sourceString == null) {
                //enable-preview must be used with explicit -source or --release
                report(Errors.PreviewWithoutSourceOrRelease);
                return false;
            } else if (source != Source.DEFAULT) {
                //enable-preview must be used with latest source version
                report(Errors.PreviewNotLatest(sourceString, Source.DEFAULT));
                return false;
            }
        }

        String profileString = options.get(Option.PROFILE);
        if (profileString != null) {
            Profile profile = Profile.lookup(profileString);
            if (target.compareTo(Target.JDK1_8) <= 0 && !profile.isValid(target)) {
                // note: -profile not permitted for target >= 9, so error (below) not warning (here)
                reportDiag(Warnings.ProfileTargetConflict(profile, target));
            }

            // This check is only effective in command line mode,
            // where the file manager options are added to options
            if (options.get(Option.BOOT_CLASS_PATH) != null) {
                reportDiag(Errors.ProfileBootclasspathConflict);
            }
        }

        if (options.isSet(Option.SOURCE_PATH) && options.isSet(Option.MODULE_SOURCE_PATH)) {
            reportDiag(Errors.SourcepathModulesourcepathConflict);
        }

        boolean lintOptions = options.isUnset(Option.XLINT_CUSTOM, "-" + LintCategory.OPTIONS.option);
        if (lintOptions && source.compareTo(Source.DEFAULT) < 0 && !options.isSet(Option.RELEASE)) {
            if (fm instanceof BaseFileManager baseFileManager) {
                if (source.compareTo(Source.JDK8) <= 0) {
                    if (baseFileManager.isDefaultBootClassPath())
                        log.warning(LintCategory.OPTIONS, Warnings.SourceNoBootclasspath(source.name));
                } else {
                    if (baseFileManager.isDefaultSystemModulesPath())
                        log.warning(LintCategory.OPTIONS, Warnings.SourceNoSystemModulesPath(source.name));
                }
            }
        }

        boolean obsoleteOptionFound = false;

        if (source.compareTo(Source.MIN) < 0) {
            log.error(Errors.OptionRemovedSource(source.name, Source.MIN.name));
        } else if (source == Source.MIN && lintOptions) {
            log.warning(LintCategory.OPTIONS, Warnings.OptionObsoleteSource(source.name));
            obsoleteOptionFound = true;
        }

        if (target.compareTo(Target.MIN) < 0) {
            log.error(Errors.OptionRemovedTarget(target, Target.MIN));
        } else if (target == Target.MIN && lintOptions) {
            log.warning(LintCategory.OPTIONS, Warnings.OptionObsoleteTarget(target));
            obsoleteOptionFound = true;
        }

        final Target t = target;
        checkOptionAllowed(t.compareTo(Target.JDK1_8) <= 0,
                option -> reportDiag(Errors.OptionNotAllowedWithTarget(option, t)),
                Option.BOOT_CLASS_PATH,
                Option.XBOOTCLASSPATH_PREPEND, Option.XBOOTCLASSPATH, Option.XBOOTCLASSPATH_APPEND,
                Option.ENDORSEDDIRS, Option.DJAVA_ENDORSED_DIRS,
                Option.EXTDIRS, Option.DJAVA_EXT_DIRS,
                Option.PROFILE);

        checkOptionAllowed(t.compareTo(Target.JDK1_9) >= 0,
                option -> reportDiag(Errors.OptionNotAllowedWithTarget(option, t)),
                Option.MODULE_SOURCE_PATH, Option.UPGRADE_MODULE_PATH,
                Option.SYSTEM, Option.MODULE_PATH, Option.ADD_MODULES,
                Option.ADD_EXPORTS, Option.ADD_OPENS, Option.ADD_READS,
                Option.LIMIT_MODULES,
                Option.PATCH_MODULE);

        if (lintOptions && options.isSet(Option.PARAMETERS) && !target.hasMethodParameters()) {
            log.warning(Warnings.OptionParametersUnsupported(target, Target.JDK1_8));
        }

        if (fm.hasLocation(StandardLocation.MODULE_SOURCE_PATH)) {
            if (!options.isSet(Option.PROC, "only")
                    && !fm.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                log.error(Errors.NoOutputDir);
            }
        }

        if (fm.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH) &&
            fm.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH)) {
            log.error(Errors.ProcessorpathNoProcessormodulepath);
        }

        if (obsoleteOptionFound && lintOptions) {
            log.warning(LintCategory.OPTIONS, Warnings.OptionObsoleteSuppression);
        }

        SourceVersion sv = Source.toSourceVersion(source);
        validateAddExports(sv);
        validateAddModules(sv);
        validateAddReads(sv);
        validateLimitModules(sv);
        validateDefaultModuleForCreatedFiles(sv);

        if (lintOptions && options.isSet(Option.ADD_OPENS)) {
            log.warning(LintCategory.OPTIONS, Warnings.AddopensIgnored);
        }

        return !errors && (log.nerrors == 0);
    }

    private void validateAddExports(SourceVersion sv) {
        String addExports = options.get(Option.ADD_EXPORTS);
        if (addExports != null) {
            // Each entry must be of the form sourceModule/sourcePackage=target-list where
            // target-list is a comma separated list of module or ALL-UNNAMED.
            // Empty items in the target-list are ignored.
            // There must be at least one item in the list; this is handled in Option.ADD_EXPORTS.
            Pattern p = Option.ADD_EXPORTS.getPattern();
            for (String e : addExports.split("\0")) {
                Matcher m = p.matcher(e);
                if (m.matches()) {
                    String sourceModuleName = m.group(1);
                    if (!SourceVersion.isName(sourceModuleName, sv)) {
                        // syntactically invalid source name:  e.g. --add-exports m!/p1=m2
                        log.warning(Warnings.BadNameForOption(Option.ADD_EXPORTS, sourceModuleName));
                    }
                    String sourcePackageName = m.group(2);
                    if (!SourceVersion.isName(sourcePackageName, sv)) {
                        // syntactically invalid source name:  e.g. --add-exports m1/p!=m2
                        log.warning(Warnings.BadNameForOption(Option.ADD_EXPORTS, sourcePackageName));
                    }

                    String targetNames = m.group(3);
                    for (String targetName : targetNames.split(",")) {
                        switch (targetName) {
                            case "":
                            case "ALL-UNNAMED":
                                break;

                            default:
                                if (!SourceVersion.isName(targetName, sv)) {
                                    // syntactically invalid target name:  e.g. --add-exports m1/p1=m!
                                    log.warning(Warnings.BadNameForOption(Option.ADD_EXPORTS, targetName));
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    private void validateAddReads(SourceVersion sv) {
        String addReads = options.get(Option.ADD_READS);
        if (addReads != null) {
            // Each entry must be of the form source=target-list where target-list is a
            // comma-separated list of module or ALL-UNNAMED.
            // Empty items in the target list are ignored.
            // There must be at least one item in the list; this is handled in Option.ADD_READS.
            Pattern p = Option.ADD_READS.getPattern();
            for (String e : addReads.split("\0")) {
                Matcher m = p.matcher(e);
                if (m.matches()) {
                    String sourceName = m.group(1);
                    if (!SourceVersion.isName(sourceName, sv)) {
                        // syntactically invalid source name:  e.g. --add-reads m!=m2
                        log.warning(Warnings.BadNameForOption(Option.ADD_READS, sourceName));
                    }

                    String targetNames = m.group(2);
                    for (String targetName : targetNames.split(",", -1)) {
                        switch (targetName) {
                            case "":
                            case "ALL-UNNAMED":
                                break;

                            default:
                                if (!SourceVersion.isName(targetName, sv)) {
                                    // syntactically invalid target name:  e.g. --add-reads m1=m!
                                    log.warning(Warnings.BadNameForOption(Option.ADD_READS, targetName));
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    private void validateAddModules(SourceVersion sv) {
        String addModules = options.get(Option.ADD_MODULES);
        if (addModules != null) {
            // Each entry must be of the form target-list where target-list is a
            // comma separated list of module names, or ALL-DEFAULT, ALL-SYSTEM,
            // or ALL-MODULE_PATH.
            // Empty items in the target list are ignored.
            // There must be at least one item in the list; this is handled in Option.ADD_MODULES.
            for (String moduleName : addModules.split(",")) {
                switch (moduleName) {
                    case "":
                    case "ALL-SYSTEM":
                    case "ALL-MODULE-PATH":
                        break;

                    default:
                        if (!SourceVersion.isName(moduleName, sv)) {
                            // syntactically invalid module name:  e.g. --add-modules m1,m!
                            log.error(Errors.BadNameForOption(Option.ADD_MODULES, moduleName));
                        }
                        break;
                }
            }
        }
    }

    private void validateLimitModules(SourceVersion sv) {
        String limitModules = options.get(Option.LIMIT_MODULES);
        if (limitModules != null) {
            // Each entry must be of the form target-list where target-list is a
            // comma separated list of module names, or ALL-DEFAULT, ALL-SYSTEM,
            // or ALL-MODULE_PATH.
            // Empty items in the target list are ignored.
            // There must be at least one item in the list; this is handled in Option.LIMIT_EXPORTS.
            for (String moduleName : limitModules.split(",")) {
                switch (moduleName) {
                    case "":
                        break;

                    default:
                        if (!SourceVersion.isName(moduleName, sv)) {
                            // syntactically invalid module name:  e.g. --limit-modules m1,m!
                            log.error(Errors.BadNameForOption(Option.LIMIT_MODULES, moduleName));
                        }
                        break;
                }
            }
        }
    }

    private void validateDefaultModuleForCreatedFiles(SourceVersion sv) {
        String moduleName = options.get(Option.DEFAULT_MODULE_FOR_CREATED_FILES);
        if (moduleName != null) {
            if (!SourceVersion.isName(moduleName, sv)) {
                // syntactically invalid module name:  e.g. --default-module-for-created-files m!
                log.error(Errors.BadNameForOption(Option.DEFAULT_MODULE_FOR_CREATED_FILES,
                                                  moduleName));
            }
        }
    }

    /**
     * Returns true if there are no files or classes specified for use.
     * @return true if there are no files or classes specified for use
     */
    public boolean isEmpty() {
        return ((files == null) || files.isEmpty())
                && ((fileObjects == null) || fileObjects.isEmpty())
                && (classNames == null || classNames.isEmpty());
    }

    public void allowEmpty() {
        this.emptyAllowed = true;
    }

    /**
     * Gets the file manager options which may have been deferred
     * during processArgs.
     * @return the deferred file manager options
     */
    public Map<Option, String> getDeferredFileManagerOptions() {
        return deferredFileManagerOptions;
    }

    /**
     * Gets any options specifying plugins to be run.
     * @return options for plugins
     */
    public Set<List<String>> getPluginOpts() {
        String plugins = options.get(Option.PLUGIN);
        if (plugins == null)
            return Collections.emptySet();

        Set<List<String>> pluginOpts = new LinkedHashSet<>();
        for (String plugin: plugins.split("\\x00")) {
            pluginOpts.add(List.from(plugin.split("\\s+")));
        }
        return Collections.unmodifiableSet(pluginOpts);
    }

    /**
     * Gets any options specifying how doclint should be run.
     * An empty list is returned if no doclint options are specified
     * or if the only doclint option is -Xdoclint:none.
     * @return options for doclint
     */
    public List<String> getDocLintOpts() {
        String xdoclint = options.get(Option.XDOCLINT);
        String xdoclintCustom = options.get(Option.XDOCLINT_CUSTOM);
        if (xdoclint == null && xdoclintCustom == null)
            return List.nil();

        Set<String> doclintOpts = new LinkedHashSet<>();
        if (xdoclint != null)
            doclintOpts.add(DocLint.XMSGS_OPTION);
        if (xdoclintCustom != null) {
            for (String s: xdoclintCustom.split("\\s+")) {
                if (s.isEmpty())
                    continue;
                doclintOpts.add(DocLint.XMSGS_CUSTOM_PREFIX + s);
            }
        }

        if (doclintOpts.equals(Collections.singleton(DocLint.XMSGS_CUSTOM_PREFIX + "none")))
            return List.nil();

        String checkPackages = options.get(Option.XDOCLINT_PACKAGE);
        if (checkPackages != null) {
            doclintOpts.add(DocLint.XCHECK_PACKAGE + checkPackages);
        }

        return List.from(doclintOpts.toArray(new String[doclintOpts.size()]));
    }

    private boolean checkDirectory(Option option) {
        String value = options.get(option);
        if (value == null) {
            return true;
        }
        Path file = Paths.get(value);
        if (Files.exists(file) && !Files.isDirectory(file)) {
            reportDiag(Errors.FileNotDirectory(value));
            return false;
        }
        return true;
    }

    private interface ErrorReporter {
        void report(Option o);
    }

    void checkOptionAllowed(boolean allowed, ErrorReporter r, Option... opts) {
        if (!allowed) {
            Stream.of(opts)
                  .filter(options :: isSet)
                  .forEach(r :: report);
        }
    }

    void reportDiag(DiagnosticInfo diag) {
        errors = true;
        switch (errorMode) {
            case ILLEGAL_ARGUMENT: {
                String msg = log.localize(diag);
                throw new PropagatedException(new IllegalArgumentException(msg));
            }
            case ILLEGAL_STATE: {
                String msg = log.localize(diag);
                throw new PropagatedException(new IllegalStateException(msg));
            }
            case LOG:
                report(diag);
        }
    }

    void error(Option.InvalidValueException f) {
        String msg = f.getMessage();
        errors = true;
        switch (errorMode) {
            case ILLEGAL_ARGUMENT: {
                throw new PropagatedException(new IllegalArgumentException(msg, f.getCause()));
            }
            case ILLEGAL_STATE: {
                throw new PropagatedException(new IllegalStateException(msg, f.getCause()));
            }
            case LOG:
                log.printRawLines(msg);
        }
    }

    private void report(DiagnosticInfo diag) {
        // Would be good to have support for -XDrawDiagnostics here
        if (diag instanceof JCDiagnostic.Error errorDiag) {
            log.error(errorDiag);
        } else if (diag instanceof JCDiagnostic.Warning warningDiag){
            log.warning(warningDiag);
        }
    }

    private JavaFileManager getFileManager() {
        if (fileManager == null)
            fileManager = context.get(JavaFileManager.class);
        return fileManager;
    }

    <T> ListBuffer<T> toList(Iterable<? extends T> items) {
        ListBuffer<T> list = new ListBuffer<>();
        if (items != null) {
            for (T item : items) {
                list.add(item);
            }
        }
        return list;
    }

    <T> Set<T> toSet(Iterable<? extends T> items) {
        Set<T> set = new LinkedHashSet<>();
        if (items != null) {
            for (T item : items) {
                set.add(item);
            }
        }
        return set;
    }
}
