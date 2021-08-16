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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.*;
import java.util.stream.Collectors;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

import static javax.tools.StandardLocation.*;

import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler.Handler;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformDescription.PluginInfo;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Abort;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.Iterators;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.MatchingUtils;
import com.sun.tools.javac.util.ModuleHelper;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import static com.sun.tools.javac.code.Lint.LintCategory.PROCESSING;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import com.sun.tools.javac.comp.Annotate;
import static com.sun.tools.javac.comp.CompileStates.CompileState;
import static com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag.*;

/**
 * Objects of this class hold and manage the state needed to support
 * annotation processing.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class JavacProcessingEnvironment implements ProcessingEnvironment, Closeable {
    private final Options options;

    private final boolean printProcessorInfo;
    private final boolean printRounds;
    private final boolean verbose;
    private final boolean lint;
    private final boolean fatalErrors;
    private final boolean werror;
    private final boolean showResolveErrors;

    private final JavacFiler filer;
    private final JavacMessager messager;
    private final JavacElements elementUtils;
    private final JavacTypes typeUtils;
    private final JavaCompiler compiler;
    private final Modules modules;
    private final Types types;
    private final Annotate annotate;

    /**
     * Holds relevant state history of which processors have been
     * used.
     */
    private DiscoveredProcessors discoveredProcs;

    /**
     * Map of processor-specific options.
     */
    private final Map<String, String> processorOptions;

    /**
     */
    private final Set<String> unmatchedProcessorOptions;

    /**
     * Annotations implicitly processed and claimed by javac.
     */
    private final Set<String> platformAnnotations;

    /**
     * Set of packages given on command line.
     */
    private Set<PackageSymbol> specifiedPackages = Collections.emptySet();

    /** The log to be used for error reporting.
     */
    final Log log;

    /** Diagnostic factory.
     */
    JCDiagnostic.Factory diags;

    /**
     * Source level of the compile.
     */
    Source source;

    private ClassLoader processorClassLoader;
    private ServiceLoader<Processor> serviceLoader;
    private SecurityException processorLoaderException;

    private final JavaFileManager fileManager;

    /**
     * JavacMessages object used for localization
     */
    private JavacMessages messages;

    private MultiTaskListener taskListener;
    private final Symtab symtab;
    private final DeferredCompletionFailureHandler dcfh;
    private final Names names;
    private final Enter enter;
    private final Completer initialCompleter;
    private final Check chk;

    private final Context context;

    /**
     * Support for preview language features.
     */
    private final Preview preview;

    /** Get the JavacProcessingEnvironment instance for this context. */
    public static JavacProcessingEnvironment instance(Context context) {
        JavacProcessingEnvironment instance = context.get(JavacProcessingEnvironment.class);
        if (instance == null)
            instance = new JavacProcessingEnvironment(context);
        return instance;
    }

    protected JavacProcessingEnvironment(Context context) {
        this.context = context;
        context.put(JavacProcessingEnvironment.class, this);
        log = Log.instance(context);
        source = Source.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        options = Options.instance(context);
        printProcessorInfo = options.isSet(Option.XPRINTPROCESSORINFO);
        printRounds = options.isSet(Option.XPRINTROUNDS);
        verbose = options.isSet(Option.VERBOSE);
        lint = Lint.instance(context).isEnabled(PROCESSING);
        compiler = JavaCompiler.instance(context);
        if (options.isSet(Option.PROC, "only") || options.isSet(Option.XPRINT)) {
            compiler.shouldStopPolicyIfNoError = CompileState.PROCESS;
        }
        fatalErrors = options.isSet("fatalEnterError");
        showResolveErrors = options.isSet("showResolveErrors");
        werror = options.isSet(Option.WERROR);
        fileManager = context.get(JavaFileManager.class);
        platformAnnotations = initPlatformAnnotations();

        // Initialize services before any processors are initialized
        // in case processors use them.
        filer = new JavacFiler(context);
        messager = new JavacMessager(context, this);
        elementUtils = JavacElements.instance(context);
        typeUtils = JavacTypes.instance(context);
        modules = Modules.instance(context);
        types = Types.instance(context);
        annotate = Annotate.instance(context);
        processorOptions = initProcessorOptions();
        unmatchedProcessorOptions = initUnmatchedProcessorOptions();
        messages = JavacMessages.instance(context);
        taskListener = MultiTaskListener.instance(context);
        symtab = Symtab.instance(context);
        dcfh = DeferredCompletionFailureHandler.instance(context);
        names = Names.instance(context);
        enter = Enter.instance(context);
        initialCompleter = ClassFinder.instance(context).getCompleter();
        chk = Check.instance(context);
        preview = Preview.instance(context);
        initProcessorLoader();
    }

    public void setProcessors(Iterable<? extends Processor> processors) {
        Assert.checkNull(discoveredProcs);
        initProcessorIterator(processors);
    }

    private Set<String> initPlatformAnnotations() {
        final String module_prefix =
            Feature.MODULES.allowedInSource(source) ? "java.base/" : "";
        return Set.of(module_prefix + "java.lang.Deprecated",
                      module_prefix + "java.lang.FunctionalInterface",
                      module_prefix + "java.lang.Override",
                      module_prefix + "java.lang.SafeVarargs",
                      module_prefix + "java.lang.SuppressWarnings",

                      module_prefix + "java.lang.annotation.Documented",
                      module_prefix + "java.lang.annotation.Inherited",
                      module_prefix + "java.lang.annotation.Native",
                      module_prefix + "java.lang.annotation.Repeatable",
                      module_prefix + "java.lang.annotation.Retention",
                      module_prefix + "java.lang.annotation.Target",

                      module_prefix + "java.io.Serial");
    }

    private void initProcessorLoader() {
        try {
            if (fileManager.hasLocation(ANNOTATION_PROCESSOR_MODULE_PATH)) {
                try {
                    serviceLoader = fileManager.getServiceLoader(ANNOTATION_PROCESSOR_MODULE_PATH, Processor.class);
                } catch (IOException e) {
                    throw new Abort(e);
                }
            } else {
                // If processorpath is not explicitly set, use the classpath.
                processorClassLoader = fileManager.hasLocation(ANNOTATION_PROCESSOR_PATH)
                    ? fileManager.getClassLoader(ANNOTATION_PROCESSOR_PATH)
                    : fileManager.getClassLoader(CLASS_PATH);

                if (options.isSet("accessInternalAPI"))
                    ModuleHelper.addExports(getClass().getModule(), processorClassLoader.getUnnamedModule());

                if (processorClassLoader != null && processorClassLoader instanceof Closeable closeable) {
                    compiler.closeables = compiler.closeables.prepend(closeable);
                }
            }
        } catch (SecurityException e) {
            processorLoaderException = e;
        }
    }

    private void initProcessorIterator(Iterable<? extends Processor> processors) {
        Iterator<? extends Processor> processorIterator;

        if (options.isSet(Option.XPRINT)) {
            try {
                processorIterator = List.of(new PrintingProcessor()).iterator();
            } catch (Throwable t) {
                AssertionError assertError =
                    new AssertionError("Problem instantiating PrintingProcessor.");
                assertError.initCause(t);
                throw assertError;
            }
        } else if (processors != null) {
            processorIterator = processors.iterator();
        } else {
            if (processorLoaderException == null) {
                /*
                 * If the "-processor" option is used, search the appropriate
                 * path for the named class.  Otherwise, use a service
                 * provider mechanism to create the processor iterator.
                 */
                String processorNames = options.get(Option.PROCESSOR);
                if (fileManager.hasLocation(ANNOTATION_PROCESSOR_MODULE_PATH)) {
                    processorIterator = (processorNames == null) ?
                            new ServiceIterator(serviceLoader, log) :
                            new NameServiceIterator(serviceLoader, log, processorNames);
                } else if (processorNames != null) {
                    processorIterator = new NameProcessIterator(processorNames, processorClassLoader, log);
                } else {
                    processorIterator = new ServiceIterator(processorClassLoader, log);
                }
            } else {
                /*
                 * A security exception will occur if we can't create a classloader.
                 * Ignore the exception if, with hindsight, we didn't need it anyway
                 * (i.e. no processor was specified either explicitly, or implicitly,
                 * in service configuration file.) Otherwise, we cannot continue.
                 */
                processorIterator = handleServiceLoaderUnavailability("proc.cant.create.loader",
                        processorLoaderException);
            }
        }
        PlatformDescription platformProvider = context.get(PlatformDescription.class);
        java.util.List<Processor> platformProcessors = Collections.emptyList();
        if (platformProvider != null) {
            platformProcessors = platformProvider.getAnnotationProcessors()
                                                 .stream()
                                                 .map(PluginInfo::getPlugin)
                                                 .toList();
        }
        List<Iterator<? extends Processor>> iterators = List.of(processorIterator,
                                                                platformProcessors.iterator());
        Iterator<? extends Processor> compoundIterator =
                Iterators.createCompoundIterator(iterators, i -> i);
        discoveredProcs = new DiscoveredProcessors(compoundIterator);
    }

    public <S> ServiceLoader<S> getServiceLoader(Class<S> service) {
        if (fileManager.hasLocation(ANNOTATION_PROCESSOR_MODULE_PATH)) {
            try {
                return fileManager.getServiceLoader(ANNOTATION_PROCESSOR_MODULE_PATH, service);
            } catch (IOException e) {
                throw new Abort(e);
            }
        } else {
            return ServiceLoader.load(service, getProcessorClassLoader());
        }
    }

    /**
     * Returns an empty processor iterator if no processors are on the
     * relevant path, otherwise if processors are present, logs an
     * error.  Called when a service loader is unavailable for some
     * reason, either because a service loader class cannot be found
     * or because a security policy prevents class loaders from being
     * created.
     *
     * @param key The resource key to use to log an error message
     * @param e   If non-null, pass this exception to Abort
     */
    private Iterator<Processor> handleServiceLoaderUnavailability(String key, Exception e) {
        if (fileManager instanceof JavacFileManager standardFileManager) {
            Iterable<? extends Path> workingPath = fileManager.hasLocation(ANNOTATION_PROCESSOR_PATH)
                ? standardFileManager.getLocationAsPaths(ANNOTATION_PROCESSOR_PATH)
                : standardFileManager.getLocationAsPaths(CLASS_PATH);

            if (needClassLoader(options.get(Option.PROCESSOR), workingPath) )
                handleException(key, e);

        } else {
            handleException(key, e);
        }

        java.util.List<Processor> pl = Collections.emptyList();
        return pl.iterator();
    }

    /**
     * Handle a security exception thrown during initializing the
     * Processor iterator.
     */
    private void handleException(String key, Exception e) {
        if (e != null) {
            log.error(key, e.getLocalizedMessage());
            throw new Abort(e);
        } else {
            log.error(key);
            throw new Abort();
        }
    }

    /**
     * Use a service loader appropriate for the platform to provide an
     * iterator over annotations processors; fails if a loader is
     * needed but unavailable.
     */
    private class ServiceIterator implements Iterator<Processor> {
        Iterator<Processor> iterator;
        Log log;
        ServiceLoader<Processor> loader;

        ServiceIterator(ClassLoader classLoader, Log log) {
            this.log = log;
            try {
                try {
                    loader = ServiceLoader.load(Processor.class, classLoader);
                    this.iterator = loader.iterator();
                } catch (Exception e) {
                    // Fail softly if a loader is not actually needed.
                    this.iterator = handleServiceLoaderUnavailability("proc.no.service", null);
                }
            } catch (Throwable t) {
                log.error(Errors.ProcServiceProblem);
                throw new Abort(t);
            }
        }

        ServiceIterator(ServiceLoader<Processor> loader, Log log) {
            this.log = log;
            this.loader = loader;
            this.iterator = loader.iterator();
        }

        @Override
        public boolean hasNext() {
            try {
                return internalHasNext();
            } catch(ServiceConfigurationError sce) {
                log.error(Errors.ProcBadConfigFile(sce.getLocalizedMessage()));
                throw new Abort(sce);
            } catch (UnsupportedClassVersionError ucve) {
                log.error(Errors.ProcCantLoadClass(ucve.getLocalizedMessage()));
                throw new Abort(ucve);
            } catch (ClassFormatError cfe) {
                log.error(Errors.ProcCantLoadClass(cfe.getLocalizedMessage()));
                throw new Abort(cfe);
            } catch (Throwable t) {
                log.error(Errors.ProcBadConfigFile(t.getLocalizedMessage()));
                throw new Abort(t);
            }
        }

        boolean internalHasNext() {
            return iterator.hasNext();
        }

        @Override
        public Processor next() {
            try {
                return internalNext();
            } catch (ServiceConfigurationError sce) {
                log.error(Errors.ProcBadConfigFile(sce.getLocalizedMessage()));
                throw new Abort(sce);
            } catch (Throwable t) {
                log.error(Errors.ProcBadConfigFile(t.getLocalizedMessage()));
                throw new Abort(t);
            }
        }

        Processor internalNext() {
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void close() {
            if (loader != null) {
                try {
                    loader.reload();
                } catch(Exception e) {
                    // Ignore problems during a call to reload.
                }
            }
        }
    }

    private class NameServiceIterator extends ServiceIterator {
        private Map<String, Processor> namedProcessorsMap = new HashMap<>();;
        private Iterator<String> processorNames = null;
        private Processor nextProc = null;

        public NameServiceIterator(ServiceLoader<Processor> loader, Log log, String theNames) {
            super(loader, log);
            this.processorNames = Arrays.asList(theNames.split(",")).iterator();
        }

        @Override
        boolean internalHasNext() {
            if (nextProc != null) {
                return true;
            }
            if (!processorNames.hasNext()) {
                namedProcessorsMap = null;
                return false;
            }
            String processorName = processorNames.next();
            Processor theProcessor = namedProcessorsMap.get(processorName);
            if (theProcessor != null) {
                namedProcessorsMap.remove(processorName);
                nextProc = theProcessor;
                return true;
            } else {
                while (iterator.hasNext()) {
                    theProcessor = iterator.next();
                    String name = theProcessor.getClass().getName();
                    if (name.equals(processorName)) {
                        nextProc = theProcessor;
                        return true;
                    } else {
                        namedProcessorsMap.put(name, theProcessor);
                    }
                }
                log.error(Errors.ProcProcessorNotFound(processorName));
                return false;
            }
        }

        @Override
        Processor internalNext() {
            if (hasNext()) {
                Processor p = nextProc;
                nextProc = null;
                return p;
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    private static class NameProcessIterator implements Iterator<Processor> {
        Processor nextProc = null;
        Iterator<String> names;
        ClassLoader processorCL;
        Log log;

        NameProcessIterator(String names, ClassLoader processorCL, Log log) {
            this.names = Arrays.asList(names.split(",")).iterator();
            this.processorCL = processorCL;
            this.log = log;
        }

        public boolean hasNext() {
            if (nextProc != null)
                return true;
            else {
                if (!names.hasNext()) {
                    return false;
                } else {
                    Processor processor = getNextProcessor(names.next());
                    if (processor == null) {
                        return false;
                    } else {
                        nextProc = processor;
                        return true;
                    }
                }
            }
        }

        private Processor getNextProcessor(String processorName) {
            try {
                try {
                    Class<?> processorClass = processorCL.loadClass(processorName);
                    ensureReadable(processorClass);
                    return (Processor) processorClass.getConstructor().newInstance();
                } catch (ClassNotFoundException cnfe) {
                    log.error(Errors.ProcProcessorNotFound(processorName));
                    return null;
                } catch (ClassCastException cce) {
                    log.error(Errors.ProcProcessorWrongType(processorName));
                    return null;
                } catch (Exception e ) {
                    log.error(Errors.ProcProcessorCantInstantiate(processorName));
                    return null;
                }
            } catch (ClientCodeException e) {
                throw e;
            } catch (Throwable t) {
                throw new AnnotationProcessingError(t);
            }
        }

        public Processor next() {
            if (hasNext()) {
                Processor p = nextProc;
                nextProc = null;
                return p;
            } else
                throw new NoSuchElementException();
        }

        public void remove () {
            throw new UnsupportedOperationException();
        }

        /**
         * Ensures that the module of the given class is readable to this
         * module.
         */
        private void ensureReadable(Class<?> targetClass) {
            try {
                Method getModuleMethod = Class.class.getMethod("getModule");
                Object thisModule = getModuleMethod.invoke(this.getClass());
                Object targetModule = getModuleMethod.invoke(targetClass);

                Class<?> moduleClass = getModuleMethod.getReturnType();
                Method addReadsMethod = moduleClass.getMethod("addReads", moduleClass);
                addReadsMethod.invoke(thisModule, targetModule);
            } catch (NoSuchMethodException e) {
                // ignore
            } catch (Exception e) {
                throw new InternalError(e);
            }
        }
    }

    public boolean atLeastOneProcessor() {
        return discoveredProcs.iterator().hasNext();
    }

    private Map<String, String> initProcessorOptions() {
        Set<String> keySet = options.keySet();
        Map<String, String> tempOptions = new LinkedHashMap<>();

        for(String key : keySet) {
            if (key.startsWith("-A") && key.length() > 2) {
                int sepIndex = key.indexOf('=');
                String candidateKey = null;
                String candidateValue = null;

                if (sepIndex == -1)
                    candidateKey = key.substring(2);
                else if (sepIndex >= 3) {
                    candidateKey = key.substring(2, sepIndex);
                    candidateValue = (sepIndex < key.length()-1)?
                        key.substring(sepIndex+1) : null;
                }
                tempOptions.put(candidateKey, candidateValue);
            }
        }

        PlatformDescription platformProvider = context.get(PlatformDescription.class);

        if (platformProvider != null) {
            for (PluginInfo<Processor> ap : platformProvider.getAnnotationProcessors()) {
                tempOptions.putAll(ap.getOptions());
            }
        }

        return Collections.unmodifiableMap(tempOptions);
    }

    private Set<String> initUnmatchedProcessorOptions() {
        Set<String> unmatchedProcessorOptions = new HashSet<>();
        unmatchedProcessorOptions.addAll(processorOptions.keySet());
        return unmatchedProcessorOptions;
    }

    /**
     * State about how a processor has been used by the tool.  If a
     * processor has been used on a prior round, its process method is
     * called on all subsequent rounds, perhaps with an empty set of
     * annotations to process.  The {@code annotationSupported} method
     * caches the supported annotation information from the first (and
     * only) getSupportedAnnotationTypes call to the processor.
     */
    static class ProcessorState {
        public Processor processor;
        public boolean   contributed;
        private Set<String> supportedAnnotationStrings; // Used for warning generation
        private Set<Pattern> supportedAnnotationPatterns;
        private Set<String> supportedOptionNames;

        ProcessorState(Processor p, Log log, Source source, DeferredCompletionFailureHandler dcfh,
                       boolean allowModules, ProcessingEnvironment env, boolean lint) {
            processor = p;
            contributed = false;

            Handler prevDeferredHandler = dcfh.setHandler(dcfh.userCodeHandler);
            try {
                processor.init(env);

                checkSourceVersionCompatibility(source, log);


                // Check for direct duplicates in the strings of
                // supported annotation types. Do not check for
                // duplicates that would result after stripping of
                // module prefixes.
                supportedAnnotationStrings = new LinkedHashSet<>();
                supportedAnnotationPatterns = new LinkedHashSet<>();
                for (String annotationPattern : processor.getSupportedAnnotationTypes()) {
                    boolean patternAdded = supportedAnnotationStrings.add(annotationPattern);

                    supportedAnnotationPatterns.
                        add(importStringToPattern(allowModules, annotationPattern,
                                                  processor, log, lint));
                    if (lint && !patternAdded) {
                        log.warning(Warnings.ProcDuplicateSupportedAnnotation(annotationPattern,
                                                                              p.getClass().getName()));
                    }
                }

                // If a processor supports "*", that matches
                // everything and other entries are redundant. With
                // more work, it could be checked that the supported
                // annotation types were otherwise non-overlapping
                // with each other in other cases, for example "foo.*"
                // and "foo.bar.*".
                if (lint &&
                    supportedAnnotationPatterns.contains(MatchingUtils.validImportStringToPattern("*")) &&
                    supportedAnnotationPatterns.size() > 1) {
                    log.warning(Warnings.ProcRedundantTypesWithWildcard(p.getClass().getName()));
                }

                supportedOptionNames = new LinkedHashSet<>();
                for (String optionName : processor.getSupportedOptions() ) {
                    if (checkOptionName(optionName, log)) {
                        boolean optionAdded = supportedOptionNames.add(optionName);
                        if (lint && !optionAdded) {
                            log.warning(Warnings.ProcDuplicateOptionName(optionName,
                                                                         p.getClass().getName()));
                        }
                    }
                }

            } catch (ClientCodeException e) {
                throw e;
            } catch (Throwable t) {
                throw new AnnotationProcessingError(t);
            } finally {
                dcfh.setHandler(prevDeferredHandler);
            }
        }

        /**
         * Checks whether or not a processor's source version is
         * compatible with the compilation source version.  The
         * processor's source version needs to be greater than or
         * equal to the source version of the compile.
         */
        private void checkSourceVersionCompatibility(Source source, Log log) {
            SourceVersion procSourceVersion = processor.getSupportedSourceVersion();
            if (procSourceVersion.compareTo(Source.toSourceVersion(source)) < 0 )  {
                log.warning(Warnings.ProcProcessorIncompatibleSourceVersion(procSourceVersion,
                                                                            processor.getClass().getName(),
                                                                            source.name));
            }
        }

        private boolean checkOptionName(String optionName, Log log) {
            boolean valid = isValidOptionName(optionName);
            if (!valid)
                log.error(Errors.ProcProcessorBadOptionName(optionName,
                                                            processor.getClass().getName()));
            return valid;
        }

        public boolean annotationSupported(String annotationName) {
            for(Pattern p: supportedAnnotationPatterns) {
                if (p.matcher(annotationName).matches())
                    return true;
            }
            return false;
        }

        /**
         * Remove options that are matched by this processor.
         */
        public void removeSupportedOptions(Set<String> unmatchedProcessorOptions) {
            unmatchedProcessorOptions.removeAll(supportedOptionNames);
        }
    }

    // TODO: These two classes can probably be rewritten better...
    /**
     * This class holds information about the processors that have
     * been discovered so far as well as the means to discover more, if
     * necessary.  A single iterator should be used per round of
     * annotation processing.  The iterator first visits already
     * discovered processors then fails over to the service provider
     * mechanism if additional queries are made.
     */
    class DiscoveredProcessors implements Iterable<ProcessorState> {

        class ProcessorStateIterator implements Iterator<ProcessorState> {
            DiscoveredProcessors psi;
            Iterator<ProcessorState> innerIter;
            boolean onProcIterator;

            ProcessorStateIterator(DiscoveredProcessors psi) {
                this.psi = psi;
                this.innerIter = psi.procStateList.iterator();
                this.onProcIterator = false;
            }

            public ProcessorState next() {
                if (!onProcIterator) {
                    if (innerIter.hasNext())
                        return innerIter.next();
                    else
                        onProcIterator = true;
                }

                if (psi.processorIterator.hasNext()) {
                    ProcessorState ps = new ProcessorState(psi.processorIterator.next(),
                                                           log, source, dcfh,
                                                           Feature.MODULES.allowedInSource(source),
                                                           JavacProcessingEnvironment.this,
                                                           lint);
                    psi.procStateList.add(ps);
                    return ps;
                } else
                    throw new NoSuchElementException();
            }

            public boolean hasNext() {
                if (onProcIterator)
                    return  psi.processorIterator.hasNext();
                else
                    return innerIter.hasNext() || psi.processorIterator.hasNext();
            }

            public void remove () {
                throw new UnsupportedOperationException();
            }

            /**
             * Run all remaining processors on the procStateList that
             * have not already run this round with an empty set of
             * annotations.
             */
            public void runContributingProcs(RoundEnvironment re) {
                if (!onProcIterator) {
                    Set<TypeElement> emptyTypeElements = Collections.emptySet();
                    while(innerIter.hasNext()) {
                        ProcessorState ps = innerIter.next();
                        if (ps.contributed)
                            callProcessor(ps.processor, emptyTypeElements, re);
                    }
                }
            }
        }

        Iterator<? extends Processor> processorIterator;
        ArrayList<ProcessorState>  procStateList;

        public ProcessorStateIterator iterator() {
            return new ProcessorStateIterator(this);
        }

        DiscoveredProcessors(Iterator<? extends Processor> processorIterator) {
            this.processorIterator = processorIterator;
            this.procStateList = new ArrayList<>();
        }

        /**
         * Free jar files, etc. if using a service loader.
         */
        public void close() {
            if (processorIterator != null &&
                processorIterator instanceof ServiceIterator serviceIterator) {
                serviceIterator.close();
            }
        }
    }

    private void discoverAndRunProcs(Set<TypeElement> annotationsPresent,
                                     List<ClassSymbol> topLevelClasses,
                                     List<PackageSymbol> packageInfoFiles,
                                     List<ModuleSymbol> moduleInfoFiles) {
        Map<String, TypeElement> unmatchedAnnotations = new HashMap<>(annotationsPresent.size());

        for(TypeElement a  : annotationsPresent) {
            ModuleElement mod = elementUtils.getModuleOf(a);
            String moduleSpec = Feature.MODULES.allowedInSource(source) && mod != null ? mod.getQualifiedName() + "/" : "";
            unmatchedAnnotations.put(moduleSpec + a.getQualifiedName().toString(),
                                     a);
        }

        // Give "*" processors a chance to match
        if (unmatchedAnnotations.size() == 0)
            unmatchedAnnotations.put("", null);

        DiscoveredProcessors.ProcessorStateIterator psi = discoveredProcs.iterator();
        // TODO: Create proper argument values; need past round
        // information to fill in this constructor.  Note that the 1
        // st round of processing could be the last round if there
        // were parse errors on the initial source files; however, we
        // are not doing processing in that case.

        Set<Element> rootElements = new LinkedHashSet<>();
        rootElements.addAll(topLevelClasses);
        rootElements.addAll(packageInfoFiles);
        rootElements.addAll(moduleInfoFiles);
        rootElements = Collections.unmodifiableSet(rootElements);

        RoundEnvironment renv = new JavacRoundEnvironment(false,
                                                          false,
                                                          rootElements,
                                                          JavacProcessingEnvironment.this);

        while(unmatchedAnnotations.size() > 0 && psi.hasNext() ) {
            ProcessorState ps = psi.next();
            Set<String>  matchedNames = new HashSet<>();
            Set<TypeElement> typeElements = new LinkedHashSet<>();

            for (Map.Entry<String, TypeElement> entry: unmatchedAnnotations.entrySet()) {
                String unmatchedAnnotationName = entry.getKey();
                if (ps.annotationSupported(unmatchedAnnotationName) ) {
                    matchedNames.add(unmatchedAnnotationName);
                    TypeElement te = entry.getValue();
                    if (te != null)
                        typeElements.add(te);
                }
            }

            if (matchedNames.size() > 0 || ps.contributed) {
                boolean processingResult = callProcessor(ps.processor, typeElements, renv);
                ps.contributed = true;
                ps.removeSupportedOptions(unmatchedProcessorOptions);

                if (printProcessorInfo || verbose) {
                    log.printLines("x.print.processor.info",
                            ps.processor.getClass().getName(),
                            matchedNames.toString(),
                            processingResult);
                }

                if (processingResult) {
                    unmatchedAnnotations.keySet().removeAll(matchedNames);
                }

            }
        }
        unmatchedAnnotations.remove("");

        if (lint && unmatchedAnnotations.size() > 0) {
            // Remove annotations processed by javac
            unmatchedAnnotations.keySet().removeAll(platformAnnotations);
            if (unmatchedAnnotations.size() > 0) {
                log.warning(Warnings.ProcAnnotationsWithoutProcessors(unmatchedAnnotations.keySet()));
            }
        }

        // Run contributing processors that haven't run yet
        psi.runContributingProcs(renv);
    }

    /**
     * Computes the set of annotations on the symbol in question.
     * Leave class public for external testing purposes.
     */
    public static class ComputeAnnotationSet extends
        ElementScanner14<Set<TypeElement>, Set<TypeElement>> {
        final Elements elements;

        public ComputeAnnotationSet(Elements elements) {
            super();
            this.elements = elements;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<TypeElement> visitPackage(PackageElement e, Set<TypeElement> p) {
            // Don't scan enclosed elements of a package
            return p;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<TypeElement> visitType(TypeElement e, Set<TypeElement> p) {
            // Type parameters are not considered to be enclosed by a type
            scan(e.getTypeParameters(), p);
            return super.visitType(e, p);
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<TypeElement> visitExecutable(ExecutableElement e, Set<TypeElement> p) {
            // Type parameters are not considered to be enclosed by an executable
            scan(e.getTypeParameters(), p);
            return super.visitExecutable(e, p);
        }

        void addAnnotations(Element e, Set<TypeElement> p) {
            for (AnnotationMirror annotationMirror :
                     elements.getAllAnnotationMirrors(e) ) {
                Element e2 = annotationMirror.getAnnotationType().asElement();
                p.add((TypeElement) e2);
            }
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public Set<TypeElement> scan(Element e, Set<TypeElement> p) {
            addAnnotations(e, p);
            return super.scan(e, p);
        }
    }

    private boolean callProcessor(Processor proc,
                                         Set<? extends TypeElement> tes,
                                         RoundEnvironment renv) {
        Handler prevDeferredHandler = dcfh.setHandler(dcfh.userCodeHandler);
        try {
            return proc.process(tes, renv);
        } catch (ClassFinder.BadClassFile ex) {
            log.error(Errors.ProcCantAccess1(ex.sym, ex.getDetailValue()));
            return false;
        } catch (CompletionFailure ex) {
            StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            log.error(Errors.ProcCantAccess(ex.sym, ex.getDetailValue(), out.toString()));
            return false;
        } catch (ClientCodeException e) {
            throw e;
        } catch (Throwable t) {
            throw new AnnotationProcessingError(t);
        } finally {
            dcfh.setHandler(prevDeferredHandler);
        }
    }

    /**
     * Helper object for a single round of annotation processing.
     */
    class Round {
        /** The round number. */
        final int number;
        /** The diagnostic handler for the round. */
        final Log.DeferredDiagnosticHandler deferredDiagnosticHandler;

        /** The ASTs to be compiled. */
        List<JCCompilationUnit> roots;
        /** The trees that need to be cleaned - includes roots and implicitly parsed trees. */
        Set<JCCompilationUnit> treesToClean;
        /** The classes to be compiler that have were generated. */
        Map<ModuleSymbol, Map<String, JavaFileObject>> genClassFiles;

        /** The set of annotations to be processed this round. */
        Set<TypeElement> annotationsPresent;
        /** The set of top level classes to be processed this round. */
        List<ClassSymbol> topLevelClasses;
        /** The set of package-info files to be processed this round. */
        List<PackageSymbol> packageInfoFiles;
        /** The set of module-info files to be processed this round. */
        List<ModuleSymbol> moduleInfoFiles;

        /** Create a round (common code). */
        private Round(int number, Set<JCCompilationUnit> treesToClean,
                Log.DeferredDiagnosticHandler deferredDiagnosticHandler) {
            this.number = number;

            if (number == 1) {
                Assert.checkNonNull(deferredDiagnosticHandler);
                this.deferredDiagnosticHandler = deferredDiagnosticHandler;
            } else {
                this.deferredDiagnosticHandler = new Log.DeferredDiagnosticHandler(log);
                compiler.setDeferredDiagnosticHandler(this.deferredDiagnosticHandler);
            }

            // the following will be populated as needed
            topLevelClasses  = List.nil();
            packageInfoFiles = List.nil();
            moduleInfoFiles = List.nil();
            this.treesToClean = treesToClean;
        }

        /** Create the first round. */
        Round(List<JCCompilationUnit> roots,
              List<ClassSymbol> classSymbols,
              Set<JCCompilationUnit> treesToClean,
              Log.DeferredDiagnosticHandler deferredDiagnosticHandler) {
            this(1, treesToClean, deferredDiagnosticHandler);
            this.roots = roots;
            genClassFiles = new HashMap<>();

            // The reverse() in the following line is to maintain behavioural
            // compatibility with the previous revision of the code. Strictly speaking,
            // it should not be necessary, but a javah golden file test fails without it.
            topLevelClasses =
                getTopLevelClasses(roots).prependList(classSymbols.reverse());

            packageInfoFiles = getPackageInfoFiles(roots);

            moduleInfoFiles = getModuleInfoFiles(roots);

            findAnnotationsPresent();
        }

        /** Create a new round. */
        private Round(Round prev,
                Set<JavaFileObject> newSourceFiles, Map<ModuleSymbol, Map<String,JavaFileObject>> newClassFiles) {
            this(prev.number+1, prev.treesToClean, null);
            prev.newRound();
            this.genClassFiles = prev.genClassFiles;

            //parse the generated files even despite errors reported so far, to eliminate
            //recoverable errors related to the type declared in the generated files:
            List<JCCompilationUnit> parsedFiles = compiler.parseFiles(newSourceFiles, true);
            roots = prev.roots.appendList(parsedFiles);

            // Check for errors after parsing
            if (unrecoverableError()) {
                compiler.initModules(List.nil());
                return;
            }

            roots = compiler.initModules(roots);

            enterClassFiles(genClassFiles);
            List<ClassSymbol> newClasses = enterClassFiles(newClassFiles);
            for (Entry<ModuleSymbol, Map<String, JavaFileObject>> moduleAndClassFiles : newClassFiles.entrySet()) {
                genClassFiles.computeIfAbsent(moduleAndClassFiles.getKey(), m -> new LinkedHashMap<>()).putAll(moduleAndClassFiles.getValue());
            }
            enterTrees(roots);

            if (unrecoverableError())
                return;

            topLevelClasses = join(
                    getTopLevelClasses(parsedFiles),
                    getTopLevelClassesFromClasses(newClasses));

            packageInfoFiles = join(
                    getPackageInfoFiles(parsedFiles),
                    getPackageInfoFilesFromClasses(newClasses));

            moduleInfoFiles = List.nil(); //module-info cannot be generated

            findAnnotationsPresent();
        }

        /** Create the next round to be used. */
        Round next(Set<JavaFileObject> newSourceFiles, Map<ModuleSymbol, Map<String, JavaFileObject>> newClassFiles) {
            return new Round(this, newSourceFiles, newClassFiles);
        }

        /** Prepare the compiler for the final compilation. */
        void finalCompiler() {
            newRound();
        }

        /** Return the number of errors found so far in this round.
         * This may include unrecoverable errors, such as parse errors,
         * and transient errors, such as missing symbols. */
        int errorCount() {
            return compiler.errorCount();
        }

        /** Return the number of warnings found so far in this round. */
        int warningCount() {
            return compiler.warningCount();
        }

        /** Return whether or not an unrecoverable error has occurred. */
        boolean unrecoverableError() {
            if (messager.errorRaised())
                return true;

            for (JCDiagnostic d: deferredDiagnosticHandler.getDiagnostics()) {
                switch (d.getKind()) {
                    case WARNING:
                        if (werror)
                            return true;
                        break;

                    case ERROR:
                        if (fatalErrors || !d.isFlagSet(RECOVERABLE))
                            return true;
                        break;
                }
            }

            return false;
        }

        /** Find the set of annotations present in the set of top level
         *  classes and package info files to be processed this round. */
        void findAnnotationsPresent() {
            ComputeAnnotationSet annotationComputer = new ComputeAnnotationSet(elementUtils);
            // Use annotation processing to compute the set of annotations present
            annotationsPresent = new LinkedHashSet<>();
            for (ClassSymbol classSym : topLevelClasses)
                annotationComputer.scan(classSym, annotationsPresent);
            for (PackageSymbol pkgSym : packageInfoFiles)
                annotationComputer.scan(pkgSym, annotationsPresent);
            for (ModuleSymbol mdlSym : moduleInfoFiles)
                annotationComputer.scan(mdlSym, annotationsPresent);
        }

        /** Enter a set of generated class files. */
        private List<ClassSymbol> enterClassFiles(Map<ModuleSymbol, Map<String, JavaFileObject>> modulesAndClassFiles) {
            List<ClassSymbol> list = List.nil();

            for (Entry<ModuleSymbol, Map<String, JavaFileObject>> moduleAndClassFiles : modulesAndClassFiles.entrySet()) {
                for (Map.Entry<String,JavaFileObject> entry : moduleAndClassFiles.getValue().entrySet()) {
                    Name name = names.fromString(entry.getKey());
                    JavaFileObject file = entry.getValue();
                    if (file.getKind() != JavaFileObject.Kind.CLASS)
                        throw new AssertionError(file);
                    ClassSymbol cs;
                    if (isPkgInfo(file, JavaFileObject.Kind.CLASS)) {
                        Name packageName = Convert.packagePart(name);
                        PackageSymbol p = symtab.enterPackage(moduleAndClassFiles.getKey(), packageName);
                        if (p.package_info == null)
                            p.package_info = symtab.enterClass(moduleAndClassFiles.getKey(), Convert.shortName(name), p);
                        cs = p.package_info;
                        cs.reset();
                        if (cs.classfile == null)
                            cs.classfile = file;
                        cs.completer = initialCompleter;
                    } else {
                        cs = symtab.enterClass(moduleAndClassFiles.getKey(), name);
                        cs.reset();
                        cs.classfile = file;
                        cs.completer = initialCompleter;
                        if (cs.owner.kind == PCK) {
                            cs.owner.members().enter(cs); //XXX - OverwriteBetweenCompilations; syms.getClass is not sufficient anymore
                        }
                    }
                    list = list.prepend(cs);
                }
            }
            return list.reverse();
        }

        /** Enter a set of syntax trees. */
        private void enterTrees(List<JCCompilationUnit> roots) {
            compiler.enterTrees(roots);
        }

        /** Run a processing round. */
        void run(boolean lastRound, boolean errorStatus) {
            printRoundInfo(lastRound);

            if (!taskListener.isEmpty())
                taskListener.started(new TaskEvent(TaskEvent.Kind.ANNOTATION_PROCESSING_ROUND));

            try {
                if (lastRound) {
                    filer.setLastRound(true);
                    Set<Element> emptyRootElements = Collections.emptySet(); // immutable
                    RoundEnvironment renv = new JavacRoundEnvironment(true,
                            errorStatus,
                            emptyRootElements,
                            JavacProcessingEnvironment.this);
                    discoveredProcs.iterator().runContributingProcs(renv);
                } else {
                    discoverAndRunProcs(annotationsPresent, topLevelClasses, packageInfoFiles, moduleInfoFiles);
                }
            } catch (Throwable t) {
                // we're specifically expecting Abort here, but if any Throwable
                // comes by, we should flush all deferred diagnostics, rather than
                // drop them on the ground.
                deferredDiagnosticHandler.reportDeferredDiagnostics();
                log.popDiagnosticHandler(deferredDiagnosticHandler);
                compiler.setDeferredDiagnosticHandler(null);
                throw t;
            } finally {
                if (!taskListener.isEmpty())
                    taskListener.finished(new TaskEvent(TaskEvent.Kind.ANNOTATION_PROCESSING_ROUND));
            }
        }

        void showDiagnostics(boolean showAll) {
            deferredDiagnosticHandler.reportDeferredDiagnostics(showAll ? ACCEPT_ALL
                                                                        : ACCEPT_NON_RECOVERABLE);
            log.popDiagnosticHandler(deferredDiagnosticHandler);
            compiler.setDeferredDiagnosticHandler(null);
        }
        //where:
            private final Predicate<JCDiagnostic> ACCEPT_NON_RECOVERABLE =
                    d -> d.getKind() != JCDiagnostic.Kind.ERROR ||
                         !d.isFlagSet(DiagnosticFlag.RECOVERABLE) ||
                         d.isFlagSet(DiagnosticFlag.API);
            private final Predicate<JCDiagnostic> ACCEPT_ALL = d -> true;

        /** Print info about this round. */
        private void printRoundInfo(boolean lastRound) {
            if (printRounds || verbose) {
                List<ClassSymbol> tlc = lastRound ? List.nil() : topLevelClasses;
                Set<TypeElement> ap = lastRound ? Collections.emptySet() : annotationsPresent;
                log.printLines("x.print.rounds",
                        number,
                        "{" + tlc.toString(", ") + "}",
                        ap,
                        lastRound);
            }
        }

        /** Prepare for new round of annotation processing. Cleans trees, resets symbols, and
         * asks selected services to prepare to a new round of annotation processing.
         */
        private void newRound() {
            //ensure treesToClean contains all trees, including implicitly parsed ones
            for (Env<AttrContext> env : enter.getEnvs()) {
                treesToClean.add(env.toplevel);
            }
            for (JCCompilationUnit node : treesToClean) {
                treeCleaner.scan(node);
            }
            chk.newRound();
            enter.newRound();
            filer.newRound();
            messager.newRound();
            compiler.newRound();
            modules.newRound();
            types.newRound();
            annotate.newRound();
            elementUtils.newRound();

            boolean foundError = false;

            for (ClassSymbol cs : symtab.getAllClasses()) {
                if (cs.kind == ERR) {
                    foundError = true;
                    break;
                }
            }

            if (foundError) {
                for (ClassSymbol cs : symtab.getAllClasses()) {
                    if (cs.classfile != null || cs.kind == ERR) {
                        Kinds.Kind symKind = cs.kind;
                        cs.reset();
                        if (symKind == ERR) {
                            cs.type = new ClassType(cs.type.getEnclosingType(), null, cs);
                        }
                        if (cs.isCompleted()) {
                            cs.completer = initialCompleter;
                        }
                    }
                }
            }
        }
    }


    // TODO: internal catch clauses?; catch and rethrow an annotation
    // processing error
    public boolean doProcessing(List<JCCompilationUnit> roots,
                                List<ClassSymbol> classSymbols,
                                Iterable<? extends PackageSymbol> pckSymbols,
                                Log.DeferredDiagnosticHandler deferredDiagnosticHandler) {
        final Set<JCCompilationUnit> treesToClean =
                Collections.newSetFromMap(new IdentityHashMap<JCCompilationUnit, Boolean>());

        //fill already attributed implicit trees:
        for (Env<AttrContext> env : enter.getEnvs()) {
            treesToClean.add(env.toplevel);
        }

        Set<PackageSymbol> specifiedPackages = new LinkedHashSet<>();
        for (PackageSymbol psym : pckSymbols)
            specifiedPackages.add(psym);
        this.specifiedPackages = Collections.unmodifiableSet(specifiedPackages);

        Round round = new Round(roots, classSymbols, treesToClean, deferredDiagnosticHandler);

        boolean errorStatus;
        boolean moreToDo;
        do {
            // Run processors for round n
            round.run(false, false);

            // Processors for round n have run to completion.
            // Check for errors and whether there is more work to do.
            errorStatus = round.unrecoverableError();
            moreToDo = moreToDo();

            round.showDiagnostics(showResolveErrors);

            // Set up next round.
            // Copy mutable collections returned from filer.
            round = round.next(
                    new LinkedHashSet<>(filer.getGeneratedSourceFileObjects()),
                    new LinkedHashMap<>(filer.getGeneratedClasses()));

             // Check for errors during setup.
            if (round.unrecoverableError())
                errorStatus = true;

        } while (moreToDo && !errorStatus);

        // run last round
        round.run(true, errorStatus);
        round.showDiagnostics(true);

        filer.warnIfUnclosedFiles();
        warnIfUnmatchedOptions();

        /*
         * If an annotation processor raises an error in a round,
         * that round runs to completion and one last round occurs.
         * The last round may also occur because no more source or
         * class files have been generated.  Therefore, if an error
         * was raised on either of the last *two* rounds, the compile
         * should exit with a nonzero exit code.  The current value of
         * errorStatus holds whether or not an error was raised on the
         * second to last round; errorRaised() gives the error status
         * of the last round.
         */
        if (messager.errorRaised()
                || werror && round.warningCount() > 0 && round.errorCount() > 0)
            errorStatus = true;

        Set<JavaFileObject> newSourceFiles =
                new LinkedHashSet<>(filer.getGeneratedSourceFileObjects());
        roots = round.roots;

        errorStatus = errorStatus || (compiler.errorCount() > 0);


        if (newSourceFiles.size() > 0)
            roots = roots.appendList(compiler.parseFiles(newSourceFiles));

        errorStatus = errorStatus || (compiler.errorCount() > 0);

        if (errorStatus && compiler.errorCount() == 0) {
            compiler.log.nerrors++;
        }

        if (compiler.continueAfterProcessAnnotations()) {
            round.finalCompiler();
            compiler.enterTrees(compiler.initModules(roots));
        } else {
            compiler.todo.clear();
        }

        // Free resources
        this.close();

        if (!taskListener.isEmpty())
            taskListener.finished(new TaskEvent(TaskEvent.Kind.ANNOTATION_PROCESSING));

        return true;
    }

    private void warnIfUnmatchedOptions() {
        if (!unmatchedProcessorOptions.isEmpty()) {
            log.warning(Warnings.ProcUnmatchedProcessorOptions(unmatchedProcessorOptions.toString()));
        }
    }

    /**
     * Free resources related to annotation processing.
     */
    public void close() {
        filer.close();
        if (discoveredProcs != null) // Make calling close idempotent
            discoveredProcs.close();
        discoveredProcs = null;
    }

    private List<ClassSymbol> getTopLevelClasses(List<? extends JCCompilationUnit> units) {
        List<ClassSymbol> classes = List.nil();
        for (JCCompilationUnit unit : units) {
            for (JCTree node : unit.defs) {
                if (node.hasTag(JCTree.Tag.CLASSDEF)) {
                    ClassSymbol sym = ((JCClassDecl) node).sym;
                    Assert.checkNonNull(sym);
                    classes = classes.prepend(sym);
                }
            }
        }
        return classes.reverse();
    }

    private List<ClassSymbol> getTopLevelClassesFromClasses(List<? extends ClassSymbol> syms) {
        List<ClassSymbol> classes = List.nil();
        for (ClassSymbol sym : syms) {
            if (!isPkgInfo(sym)) {
                classes = classes.prepend(sym);
            }
        }
        return classes.reverse();
    }

    private List<PackageSymbol> getPackageInfoFiles(List<? extends JCCompilationUnit> units) {
        List<PackageSymbol> packages = List.nil();
        for (JCCompilationUnit unit : units) {
            if (isPkgInfo(unit.sourcefile, JavaFileObject.Kind.SOURCE)) {
                packages = packages.prepend(unit.packge);
            }
        }
        return packages.reverse();
    }

    private List<PackageSymbol> getPackageInfoFilesFromClasses(List<? extends ClassSymbol> syms) {
        List<PackageSymbol> packages = List.nil();
        for (ClassSymbol sym : syms) {
            if (isPkgInfo(sym)) {
                packages = packages.prepend((PackageSymbol) sym.owner);
            }
        }
        return packages.reverse();
    }

    private List<ModuleSymbol> getModuleInfoFiles(List<? extends JCCompilationUnit> units) {
        List<ModuleSymbol> modules = List.nil();
        for (JCCompilationUnit unit : units) {
            if (isModuleInfo(unit.sourcefile, JavaFileObject.Kind.SOURCE) && unit.defs.nonEmpty()) {
                for (JCTree tree : unit.defs) {
                    if (tree.hasTag(Tag.IMPORT)) {
                        continue;
                    }
                    else if (tree.hasTag(Tag.MODULEDEF)) {
                        modules = modules.prepend(unit.modle);
                        break;
                    }
                    else {
                        break;
                    }
                }
            }
        }
        return modules.reverse();
    }

    // avoid unchecked warning from use of varargs
    private static <T> List<T> join(List<T> list1, List<T> list2) {
        return list1.appendList(list2);
    }

    private boolean isPkgInfo(JavaFileObject fo, JavaFileObject.Kind kind) {
        return fo.isNameCompatible("package-info", kind);
    }

    private boolean isPkgInfo(ClassSymbol sym) {
        return isPkgInfo(sym.classfile, JavaFileObject.Kind.CLASS) && (sym.packge().package_info == sym);
    }

    private boolean isModuleInfo(JavaFileObject fo, JavaFileObject.Kind kind) {
        return fo.isNameCompatible("module-info", kind);
    }

    /*
     * Called retroactively to determine if a class loader was required,
     * after we have failed to create one.
     */
    private boolean needClassLoader(String procNames, Iterable<? extends Path> workingpath) {
        if (procNames != null)
            return true;

        URL[] urls = new URL[1];
        for(Path pathElement : workingpath) {
            try {
                urls[0] = pathElement.toUri().toURL();
                if (ServiceProxy.hasService(Processor.class, urls))
                    return true;
            } catch (MalformedURLException ex) {
                throw new AssertionError(ex);
            }
            catch (ServiceProxy.ServiceConfigurationError e) {
                log.error(Errors.ProcBadConfigFile(e.getLocalizedMessage()));
                return true;
            }
        }

        return false;
    }

    class ImplicitCompleter implements Completer {

        private final JCCompilationUnit topLevel;

        public ImplicitCompleter(JCCompilationUnit topLevel) {
            this.topLevel = topLevel;
        }

        @Override public void complete(Symbol sym) throws CompletionFailure {
            compiler.readSourceFile(topLevel, (ClassSymbol) sym);
        }
    }

    private final TreeScanner treeCleaner = new TreeScanner() {
            public void scan(JCTree node) {
                super.scan(node);
                if (node != null)
                    node.type = null;
            }
            JCCompilationUnit topLevel;
            public void visitTopLevel(JCCompilationUnit node) {
                if (node.packge != null) {
                    if (isPkgInfo(node.sourcefile, Kind.SOURCE)) {
                        node.packge.package_info.reset();
                    }
                    node.packge.reset();
                }
                if (isModuleInfo(node.sourcefile, Kind.SOURCE)) {
                    node.modle.reset();
                    node.modle.completer = sym -> modules.enter(List.of(node), node.modle.module_info);
                    node.modle.module_info.reset();
                    node.modle.module_info.members_field = WriteableScope.create(node.modle.module_info);
                }
                node.packge = null;
                topLevel = node;
                try {
                    super.visitTopLevel(node);
                } finally {
                    topLevel = null;
                }
            }
            public void visitClassDef(JCClassDecl node) {
                super.visitClassDef(node);
                // remove generated constructor that may have been added during attribution:
                List<JCTree> beforeConstructor = List.nil();
                List<JCTree> defs = node.defs;
                while (defs.nonEmpty() && !defs.head.hasTag(Tag.METHODDEF)) {
                    beforeConstructor = beforeConstructor.prepend(defs.head);
                    defs = defs.tail;
                }
                if (defs.nonEmpty() &&
                    (((JCMethodDecl) defs.head).mods.flags & Flags.GENERATEDCONSTR) != 0) {
                    defs = defs.tail;
                    while (beforeConstructor.nonEmpty()) {
                        defs = defs.prepend(beforeConstructor.head);
                        beforeConstructor = beforeConstructor.tail;
                    }
                    node.defs = defs;
                }
                if (node.sym != null) {
                    node.sym.completer = new ImplicitCompleter(topLevel);
                    List<? extends RecordComponent> recordComponents = node.sym.getRecordComponents();
                    for (RecordComponent rc : recordComponents) {
                        List<JCAnnotation> originalAnnos = rc.getOriginalAnnos();
                        originalAnnos.stream().forEach(a -> visitAnnotation(a));
                    }
                    // we should empty the list of permitted subclasses for next round
                    node.sym.permitted = List.nil();
                }
                node.sym = null;
            }
            public void visitMethodDef(JCMethodDecl node) {
                // remove super constructor call that may have been added during attribution:
                if (TreeInfo.isConstructor(node) && node.sym != null && node.sym.owner.isEnum() &&
                    node.body.stats.nonEmpty() && TreeInfo.isSuperCall(node.body.stats.head) &&
                    node.body.stats.head.pos == node.body.pos) {
                    node.body.stats = node.body.stats.tail;
                }
                node.sym = null;
                super.visitMethodDef(node);
            }
            public void visitVarDef(JCVariableDecl node) {
                node.sym = null;
                super.visitVarDef(node);
            }
            public void visitNewClass(JCNewClass node) {
                node.constructor = null;
                super.visitNewClass(node);
            }
            public void visitAssignop(JCAssignOp node) {
                node.operator = null;
                super.visitAssignop(node);
            }
            public void visitUnary(JCUnary node) {
                node.operator = null;
                super.visitUnary(node);
            }
            public void visitBinary(JCBinary node) {
                node.operator = null;
                super.visitBinary(node);
            }
            public void visitSelect(JCFieldAccess node) {
                node.sym = null;
                super.visitSelect(node);
            }
            public void visitIdent(JCIdent node) {
                node.sym = null;
                super.visitIdent(node);
            }
            public void visitAnnotation(JCAnnotation node) {
                node.attribute = null;
                super.visitAnnotation(node);
            }
        };


    private boolean moreToDo() {
        return filer.newFiles();
    }

    /**
     * {@inheritDoc}
     *
     * Command line options suitable for presenting to annotation
     * processors.
     * {@literal "-Afoo=bar"} should be {@literal "-Afoo" => "bar"}.
     */
    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public Map<String,String> getOptions() {
        return processorOptions;
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public Messager getMessager() {
        return messager;
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public JavacFiler getFiler() {
        return filer;
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public JavacElements getElementUtils() {
        return elementUtils;
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public JavacTypes getTypeUtils() {
        return typeUtils;
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public SourceVersion getSourceVersion() {
        return Source.toSourceVersion(source);
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public Locale getLocale() {
        return messages.getCurrentLocale();
    }

    @DefinedBy(Api.ANNOTATION_PROCESSING)
    public boolean isPreviewEnabled() {
        return preview.isEnabled();
    }

    public Set<Symbol.PackageSymbol> getSpecifiedPackages() {
        return specifiedPackages;
    }

    public static final Pattern noMatches  = Pattern.compile("(\\P{all})+");

    /**
     * Convert import-style string for supported annotations into a
     * regex matching that string.  If the string is not a valid
     * import-style string, return a regex that won't match anything.
     */
    private static Pattern importStringToPattern(boolean allowModules, String s, Processor p, Log log, boolean lint) {
        String module;
        String pkg;
        int slash = s.indexOf('/');
        if (slash == (-1)) {
            if (s.equals("*")) {
                return MatchingUtils.validImportStringToPattern(s);
            }
            module = allowModules ? ".*/" : "";
            pkg = s;
        } else {
            String moduleName = s.substring(0, slash);
            if (!SourceVersion.isName(moduleName)) {
                return warnAndNoMatches(s, p, log, lint);
            }
            module = Pattern.quote(moduleName + "/");
            // And warn if module is specified if modules aren't supported, conditional on -Xlint:proc?
            pkg = s.substring(slash + 1);
        }
        if (MatchingUtils.isValidImportString(pkg)) {
            return Pattern.compile(module + MatchingUtils.validImportStringToPatternString(pkg));
        } else {
            return warnAndNoMatches(s, p, log, lint);
        }
    }

    private static Pattern warnAndNoMatches(String s, Processor p, Log log, boolean lint) {
        if (lint) {
            log.warning(Warnings.ProcMalformedSupportedString(s, p.getClass().getName()));
        }
        return noMatches; // won't match any valid identifier
    }

    /**
     * For internal use only.  This method may be removed without warning.
     */
    public Context getContext() {
        return context;
    }

    /**
     * For internal use only.  This method may be removed without warning.
     */
    public ClassLoader getProcessorClassLoader() {
        return processorClassLoader;
    }

    public String toString() {
        return "javac ProcessingEnvironment";
    }

    public static boolean isValidOptionName(String optionName) {
        for(String s : optionName.split("\\.", -1)) {
            if (!SourceVersion.isIdentifier(s))
                return false;
        }
        return true;
    }
}
