/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;
import jdk.javadoc.doclet.Taglet;
import jdk.javadoc.internal.doclets.toolkit.builders.BuilderFactory;
import jdk.javadoc.internal.doclets.toolkit.taglets.TagletManager;
import jdk.javadoc.internal.doclets.toolkit.util.Comparators;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileFactory;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.Extern;
import jdk.javadoc.internal.doclets.toolkit.util.Group;
import jdk.javadoc.internal.doclets.toolkit.util.MetaKeywords;
import jdk.javadoc.internal.doclets.toolkit.util.SimpleDocletException;
import jdk.javadoc.internal.doclets.toolkit.util.TypeElementCatalog;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.Utils.Pair;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberCache;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;
import jdk.javadoc.internal.doclint.DocLint;

/**
 * Configure the output based on the options. Doclets should sub-class
 * BaseConfiguration, to configure and add their own options. This class contains
 * all user options which are supported by the standard doclet.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public abstract class BaseConfiguration {
    /**
     * The doclet that created this configuration.
     */
    public final Doclet doclet;

    /**
     * The factory for builders.
     */
    protected BuilderFactory builderFactory;

    /**
     * The taglet manager.
     */
    public TagletManager tagletManager;

    /**
     * The meta tag keywords instance.
     */
    public MetaKeywords metakeywords;

    /**
     * The doclet environment.
     */
    public DocletEnvironment docEnv;

    /**
     * An utility class for commonly used helpers
     */
    public Utils utils;

    /**
     * All the temporary accessors to javac internals.
     */
    public WorkArounds workArounds;

    /**
     * Sourcepath from where to read the source files. Default is classpath.
     */
    public String sourcepath = "";

    /**
     * Generate modules documentation if more than one module is present.
     */
    public boolean showModules = false;

    /**
     * The catalog of classes specified on the command-line
     */
    public TypeElementCatalog typeElementCatalog;

    /**
     * The package grouping instance.
     */
    public final Group group = new Group(this);

    /**
     * The tracker of external package links.
     */
    public Extern extern;

    public final Reporter reporter;

    public final Locale locale;

    public abstract Messages getMessages();

    public abstract Resources getDocResources();

    /**
     * Returns the version of the {@link #doclet doclet}.
     *
     * @return the version
     */
    public abstract Runtime.Version getDocletVersion();

    /**
     * This method should be defined in all those doclets (configurations),
     * which want to derive themselves from this BaseConfiguration. This method
     * can be used to finish up the options setup.
     *
     * @return true if successful and false otherwise
     */

    public abstract boolean finishOptionSettings();

    public CommentUtils cmtUtils;

    /**
     * A sorted set of included packages.
     */
    public SortedSet<PackageElement> packages = null;

    public OverviewElement overviewElement;

    public DocFileFactory docFileFactory;

    /**
     * A sorted map, giving the (specified|included|other) packages for each module.
     */
    public SortedMap<ModuleElement, Set<PackageElement>> modulePackages;

    /**
     * The list of known modules, that should be documented.
     */
    public SortedSet<ModuleElement> modules;

    protected static final String sharedResourceBundleName =
            "jdk.javadoc.internal.doclets.toolkit.resources.doclets";

    VisibleMemberCache visibleMemberCache = null;

    public PropertyUtils propertyUtils = null;

    /**
     * Constructs the format-independent configuration needed by the doclet.
     *
     * @apiNote The {@code doclet} parameter is used when
     * {@link Taglet#init(DocletEnvironment, Doclet) initializing tags}.
     * Some doclets (such as the {@link StandardDoclet}), may delegate to another
     * (such as the {@code HtmlDoclet}).  In such cases, the primary doclet (i.e
     * {@code StandardDoclet}) should be provided here, and not any internal
     * class like {@code HtmlDoclet}.
     *
     * @param doclet   the doclet for this run of javadoc
     * @param locale   the locale for the generated documentation
     * @param reporter the reporter to use for console messages
     */
    public BaseConfiguration(Doclet doclet, Locale locale, Reporter reporter) {
        this.doclet = doclet;
        this.locale = locale;
        this.reporter = reporter;
    }

    public abstract BaseOptions getOptions();

    private boolean initialized = false;

    protected void initConfiguration(DocletEnvironment docEnv,
                                     Function<String, String> resourceKeyMapper) {
        if (initialized) {
            throw new IllegalStateException("configuration previously initialized");
        }
        initialized = true;
        this.docEnv = docEnv;
        // Utils needs docEnv, safe to init now.
        utils = new Utils(this);

        BaseOptions options = getOptions();
        if (!options.javafx()) {
            options.setJavaFX(isJavaFXMode());
        }

        getDocResources().setKeyMapper(resourceKeyMapper);

        // Once docEnv and Utils have been initialized, others should be safe.
        metakeywords = new MetaKeywords(this);
        cmtUtils = new CommentUtils(this);
        workArounds = new WorkArounds(this);
        visibleMemberCache = new VisibleMemberCache(this);
        propertyUtils = new PropertyUtils(this);

        Splitter specifiedSplitter = new Splitter(docEnv, false);
        specifiedModuleElements = Collections.unmodifiableSet(specifiedSplitter.mset);
        specifiedPackageElements = Collections.unmodifiableSet(specifiedSplitter.pset);
        specifiedTypeElements = Collections.unmodifiableSet(specifiedSplitter.tset);

        Splitter includedSplitter = new Splitter(docEnv, true);
        includedModuleElements = Collections.unmodifiableSet(includedSplitter.mset);
        includedPackageElements = Collections.unmodifiableSet(includedSplitter.pset);
        includedTypeElements = Collections.unmodifiableSet(includedSplitter.tset);
    }

    /**
     * Return the builder factory for this doclet.
     *
     * @return the builder factory for this doclet.
     */
    public BuilderFactory getBuilderFactory() {
        if (builderFactory == null) {
            builderFactory = new BuilderFactory(this);
        }
        return builderFactory;
    }

    public Reporter getReporter() {
        return this.reporter;
    }

    private Set<ModuleElement> specifiedModuleElements;

    public Set<ModuleElement> getSpecifiedModuleElements() {
        return specifiedModuleElements;
    }

    private Set<PackageElement> specifiedPackageElements;

    public Set<PackageElement> getSpecifiedPackageElements() {
        return specifiedPackageElements;
    }

    private Set<TypeElement> specifiedTypeElements;

    public Set<TypeElement> getSpecifiedTypeElements() {
        return specifiedTypeElements;
    }

    private Set<ModuleElement> includedModuleElements;

    public Set<ModuleElement> getIncludedModuleElements() {
        return includedModuleElements;
    }

    private Set<PackageElement> includedPackageElements;

    public Set<PackageElement> getIncludedPackageElements() {
        return includedPackageElements;
    }

    private Set<TypeElement> includedTypeElements;

    public Set<TypeElement> getIncludedTypeElements() {
        return includedTypeElements;
    }

    private void initModules() {
        Comparators comparators = utils.comparators;
        // Build the modules structure used by the doclet
        modules = new TreeSet<>(comparators.makeModuleComparator());
        modules.addAll(getSpecifiedModuleElements());

        modulePackages = new TreeMap<>(comparators.makeModuleComparator());
        for (PackageElement p : packages) {
            ModuleElement mdle = docEnv.getElementUtils().getModuleOf(p);
            if (mdle != null && !mdle.isUnnamed()) {
                Set<PackageElement> s = modulePackages
                        .computeIfAbsent(mdle, m -> new TreeSet<>(comparators.makePackageComparator()));
                s.add(p);
            }
        }

        for (PackageElement p : getIncludedPackageElements()) {
            ModuleElement mdle = docEnv.getElementUtils().getModuleOf(p);
            if (mdle != null && !mdle.isUnnamed()) {
                Set<PackageElement> s = modulePackages
                        .computeIfAbsent(mdle, m -> new TreeSet<>(comparators.makePackageComparator()));
                s.add(p);
            }
        }

        // add entries for modules which may not have exported packages
        modules.forEach(mdle -> modulePackages.computeIfAbsent(mdle, m -> Collections.emptySet()));

        modules.addAll(modulePackages.keySet());
        showModules = !modules.isEmpty();
        for (Set<PackageElement> pkgs : modulePackages.values()) {
            packages.addAll(pkgs);
        }
    }

    private void initPackages() {
        packages = new TreeSet<>(utils.comparators.makePackageComparator());
        // add all the included packages
        packages.addAll(includedPackageElements);
    }

    /*
     * when this is called all the option have been set, this method,
     * initializes certain components before anything else is started.
     */
    protected boolean finishOptionSettings0() throws DocletException {
        BaseOptions options = getOptions();
        extern = new Extern(this);
        initDestDirectory();
        for (String link : options.linkList()) {
            extern.link(link, reporter);
        }
        for (Pair<String, String> linkOfflinePair : options.linkOfflineList()) {
            extern.link(linkOfflinePair.first, linkOfflinePair.second, reporter);
        }
        if (!options.noPlatformLinks()) {
            extern.checkPlatformLinks(options.linkPlatformProperties(), reporter);
        }
        typeElementCatalog = new TypeElementCatalog(includedTypeElements, this);
        initTagletManager(options.customTagStrs());
        options.groupPairs().forEach(grp -> {
            if (showModules) {
                group.checkModuleGroups(grp.first, grp.second);
            } else {
                group.checkPackageGroups(grp.first, grp.second);
            }
        });

        PackageElement unnamedPackage;
        Elements elementUtils = utils.elementUtils;
        if (docEnv.getSourceVersion().compareTo(SourceVersion.RELEASE_9) >= 0) {
            ModuleElement unnamedModule = elementUtils.getModuleElement("");
            unnamedPackage = elementUtils.getPackageElement(unnamedModule, "");
        } else {
            unnamedPackage = elementUtils.getPackageElement("");
        }
        overviewElement = new OverviewElement(unnamedPackage, getOverviewPath());
        return true;
    }

    /**
     * Set the command-line options supported by this configuration.
     *
     * @return true if the options are set successfully
     * @throws DocletException if there is a problem while setting the options
     */
    public boolean setOptions() throws DocletException {
        initPackages();
        initModules();
        return finishOptionSettings0()
                && finishOptionSettings();
    }

    private void initDestDirectory() throws DocletException {
        String destDirName = getOptions().destDirName();
        if (!destDirName.isEmpty()) {
            Messages messages = getMessages();
            DocFile destDir = DocFile.createFileForDirectory(this, destDirName);
            if (!destDir.exists()) {
                //Create the output directory (in case it doesn't exist yet)
                messages.notice("doclet.dest_dir_create", destDirName);
                destDir.mkdirs();
            } else if (!destDir.isDirectory()) {
                throw new SimpleDocletException(messages.getResources().getText(
                        "doclet.destination_directory_not_directory_0",
                        destDir.getPath()));
            } else if (!destDir.canWrite()) {
                throw new SimpleDocletException(messages.getResources().getText(
                        "doclet.destination_directory_not_writable_0",
                        destDir.getPath()));
            }
        }
        DocFileFactory.getFactory(this).setDestDir(destDirName);
    }

    /**
     * Initialize the taglet manager.  The strings to initialize the simple custom tags should
     * be in the following format:  "[tag name]:[location str]:[heading]".
     *
     * @param customTagStrs the set two dimensional arrays of strings.  These arrays contain
     *                      either -tag or -taglet arguments.
     */
    private void initTagletManager(Set<List<String>> customTagStrs) {
        tagletManager = tagletManager != null ? tagletManager : new TagletManager(this);
        JavaFileManager fileManager = getFileManager();
        Messages messages = getMessages();
        try {
            tagletManager.initTagletPath(fileManager);
            tagletManager.loadTaglets(fileManager);

            for (List<String> args : customTagStrs) {
                if (args.get(0).equals("-taglet")) {
                    tagletManager.addCustomTag(args.get(1), fileManager);
                    continue;
                }
                List<String> tokens = tokenize(args.get(1), TagletManager.SIMPLE_TAGLET_OPT_SEPARATOR, 3);
                switch (tokens.size()) {
                    case 1:
                        String tagName = args.get(1);
                        if (tagletManager.isKnownCustomTag(tagName)) {
                            //reorder a standard tag
                            tagletManager.addNewSimpleCustomTag(tagName, null, "");
                        } else {
                            //Create a simple tag with the heading that has the same name as the tag.
                            StringBuilder heading = new StringBuilder(tagName + ":");
                            heading.setCharAt(0, Character.toUpperCase(tagName.charAt(0)));
                            tagletManager.addNewSimpleCustomTag(tagName, heading.toString(), "a");
                        }
                        break;

                    case 2:
                        //Add simple taglet without heading, probably to excluding it in the output.
                        tagletManager.addNewSimpleCustomTag(tokens.get(0), tokens.get(1), "");
                        break;

                    case 3:
                        tagletManager.addNewSimpleCustomTag(tokens.get(0), tokens.get(2), tokens.get(1));
                        break;

                    default:
                        messages.error("doclet.Error_invalid_custom_tag_argument", args.get(1));
                }
            }
        } catch (IOException e) {
            messages.error("doclet.taglet_could_not_set_location", e.toString());
        }
    }

    /**
     * Given a string, return an array of tokens.  The separator can be escaped
     * with the '\' character.  The '\' character may also be escaped by the
     * '\' character.
     *
     * @param s         the string to tokenize.
     * @param separator the separator char.
     * @param maxTokens the maximum number of tokens returned.  If the
     *                  max is reached, the remaining part of s is appended
     *                  to the end of the last token.
     * @return an array of tokens.
     */
    private List<String> tokenize(String s, char separator, int maxTokens) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean prevIsEscapeChar = false;
        for (int i = 0; i < s.length(); i += Character.charCount(i)) {
            int currentChar = s.codePointAt(i);
            if (prevIsEscapeChar) {
                // Case 1:  escaped character
                token.appendCodePoint(currentChar);
                prevIsEscapeChar = false;
            } else if (currentChar == separator && tokens.size() < maxTokens - 1) {
                // Case 2:  separator
                tokens.add(token.toString());
                token = new StringBuilder();
            } else if (currentChar == '\\') {
                // Case 3:  escape character
                prevIsEscapeChar = true;
            } else {
                // Case 4:  regular character
                token.appendCodePoint(currentChar);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    /**
     * Return true if the given doc-file subdirectory should be excluded and
     * false otherwise.
     *
     * @param docfilesubdir the doc-files subdirectory to check.
     * @return true if the directory is excluded.
     */
    public boolean shouldExcludeDocFileDir(String docfilesubdir) {
        Set<String> excludedDocFileDirs = getOptions().excludedDocFileDirs();
        return excludedDocFileDirs.contains(docfilesubdir);
    }

    /**
     * Return true if the given qualifier should be excluded and false otherwise.
     *
     * @param qualifier the qualifier to check.
     * @return true if the qualifier should be excluded
     */
    public boolean shouldExcludeQualifier(String qualifier) {
        Set<String> excludedQualifiers = getOptions().excludedQualifiers();
        if (excludedQualifiers.contains("all") ||
                excludedQualifiers.contains(qualifier) ||
                excludedQualifiers.contains(qualifier + ".*")) {
            return true;
        } else {
            int index = -1;
            while ((index = qualifier.indexOf(".", index + 1)) != -1) {
                if (excludedQualifiers.contains(qualifier.substring(0, index + 1) + "*")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return the qualified name of the Element if its qualifier is not excluded.
     * Otherwise return the unqualified Element name.
     *
     * @param te the TypeElement to check.
     * @return the class name
     */
    public String getClassName(TypeElement te) {
        PackageElement pkg = utils.containingPackage(te);
        return shouldExcludeQualifier(utils.getPackageName(pkg))
                ? utils.getSimpleName(te)
                : utils.getFullyQualifiedName(te);
    }

    /**
     * Return true if the TypeElement element is getting documented, depending upon
     * -nodeprecated option and the deprecation information. Return true if
     * -nodeprecated is not used. Return false if -nodeprecated is used and if
     * either TypeElement element is deprecated or the containing package is deprecated.
     *
     * @param te the TypeElement for which the page generation is checked
     * @return true if it is a generated doc.
     */
    public boolean isGeneratedDoc(TypeElement te) {
        boolean nodeprecated = getOptions().noDeprecated();
        if (!nodeprecated) {
            return true;
        }
        return !(utils.isDeprecated(te) || utils.isDeprecated(utils.containingPackage(te)));
    }

    /**
     * Return the doclet specific instance of a writer factory.
     *
     * @return the {@link WriterFactory} for the doclet.
     */
    public abstract WriterFactory getWriterFactory();

    /**
     * Return the Locale for this document.
     *
     * @return the current locale
     */
    public abstract Locale getLocale();

    /**
     * Return the path of the overview file and null if it does not exist.
     *
     * @return the path of the overview file.
     */
    public abstract JavaFileObject getOverviewPath();

    /**
     * Return the current file manager.
     *
     * @return JavaFileManager
     */
    public abstract JavaFileManager getFileManager();

    public abstract boolean showMessage(DocTreePath path, String key);

    public abstract boolean showMessage(Element e, String key);

    /*
     * Splits the elements in a collection to its individual
     * collection.
     */
    private static class Splitter {

        final Set<ModuleElement> mset = new LinkedHashSet<>();
        final Set<PackageElement> pset = new LinkedHashSet<>();
        final Set<TypeElement> tset = new LinkedHashSet<>();

        Splitter(DocletEnvironment docEnv, boolean included) {

            Set<? extends Element> inset = included
                    ? docEnv.getIncludedElements()
                    : docEnv.getSpecifiedElements();

            for (Element e : inset) {
                new SimpleElementVisitor14<Void, Void>() {
                    @Override
                    @DefinedBy(Api.LANGUAGE_MODEL)
                    public Void visitModule(ModuleElement e, Void p) {
                        mset.add(e);
                        return null;
                    }

                    @Override
                    @DefinedBy(Api.LANGUAGE_MODEL)
                    public Void visitPackage(PackageElement e, Void p) {
                        pset.add(e);
                        return null;
                    }

                    @Override
                    @DefinedBy(Api.LANGUAGE_MODEL)
                    public Void visitType(TypeElement e, Void p) {
                        tset.add(e);
                        return null;
                    }

                    @Override
                    @DefinedBy(Api.LANGUAGE_MODEL)
                    protected Void defaultAction(Element e, Void p) {
                        throw new AssertionError("unexpected element: " + e);
                    }

                }.visit(e);
            }
        }
    }

    /**
     * Returns whether or not to allow JavaScript in comments.
     * Default is off; can be set true from a command-line option.
     *
     * @return the allowScriptInComments
     */
    public boolean isAllowScriptInComments() {
        return getOptions().allowScriptInComments();
    }

    public synchronized VisibleMemberTable getVisibleMemberTable(TypeElement te) {
        return visibleMemberCache.getVisibleMemberTable(te);
    }

    /**
     * Determines if JavaFX is available in the compilation environment.
     * @return true if JavaFX is available
     */
    public boolean isJavaFXMode() {
        TypeElement observable = utils.elementUtils.getTypeElement("javafx.beans.Observable");
        if (observable == null) {
            return false;
        }
        ModuleElement javafxModule = utils.elementUtils.getModuleOf(observable);
        return javafxModule == null
                || javafxModule.isUnnamed()
                || javafxModule.getQualifiedName().contentEquals("javafx.base");
    }


    //<editor-fold desc="DocLint support">

    private DocLint doclint;

    Map<CompilationUnitTree, Boolean> shouldCheck = new HashMap<>();

    public void runDocLint(TreePath path) {
        CompilationUnitTree unit = path.getCompilationUnit();
        if (doclint != null && shouldCheck.computeIfAbsent(unit, doclint::shouldCheck)) {
            doclint.scan(path);
        }
    }

    /**
     * Initializes DocLint, if appropriate, depending on options derived
     * from the doclet command-line options, and the set of custom tags
     * that should be ignored by DocLint.
     *
     * DocLint is not enabled if the option {@code -Xmsgs:none} is given,
     * and it is not followed by any options to enable any groups.
     * Note that arguments for {@code -Xmsgs:} can be given individually
     * in separate {@code -Xmsgs:} options, or in a comma-separated list
     * for a single option. For example, the following are equivalent:
     * <ul>
     *     <li>{@code -Xmsgs:all} {@code -Xmsgs:-html}
     *     <li>{@code -Xmsgs:all,-html}
     * </ul>
     *
     * @param opts  options for DocLint, derived from the corresponding doclet
     *              command-line options
     * @param customTagNames the names of custom tags, to be ignored by doclint
     */
    public void initDocLint(List<String> opts, Set<String> customTagNames) {
        List<String> doclintOpts = new ArrayList<>();

        // basic analysis of -Xmsgs and -Xmsgs: options to see if doclint is enabled
        Set<String> groups = new HashSet<>();
        boolean seenXmsgs = false;
        for (String opt : opts) {
            if (opt.equals(DocLint.XMSGS_OPTION)) {
                groups.add("all");
                seenXmsgs = true;
            } else if (opt.startsWith(DocLint.XMSGS_CUSTOM_PREFIX)) {
                String[] args = opt.substring(DocLint.XMSGS_CUSTOM_PREFIX.length())
                        .split(DocLint.SEPARATOR);
                for (String a : args) {
                    if (a.equals("none")) {
                        groups.clear();
                    } else if (a.startsWith("-")) {
                        groups.remove(a.substring(1));
                    } else {
                        groups.add(a);
                    }
                }
                seenXmsgs = true;
            }
            doclintOpts.add(opt);
        }

        if (seenXmsgs) {
            if (groups.isEmpty()) {
                // no groups enabled; do not init doclint
                return;
            }
        } else {
            // no -Xmsgs options of any kind, use default
            doclintOpts.add(DocLint.XMSGS_OPTION);
        }

        if (!customTagNames.isEmpty()) {
            String customTags = String.join(DocLint.SEPARATOR, customTagNames);
            doclintOpts.add(DocLint.XCUSTOM_TAGS_PREFIX + customTags);
        }

        doclint = new DocLint();
        doclint.init(docEnv.getDocTrees(), docEnv.getElementUtils(), docEnv.getTypeUtils(),
                doclintOpts.toArray(new String[0]));
    }

    public boolean haveDocLint() {
        return (doclint != null);
    }
    //</editor-fold>
}
