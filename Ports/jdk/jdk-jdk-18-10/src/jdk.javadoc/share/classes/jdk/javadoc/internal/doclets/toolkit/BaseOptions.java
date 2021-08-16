/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Storage for the format-independent options supported by the toolkit.
 * The objects to handle command-line options, and to initialize this
 * object, are all subtypes of {@link BaseOptions.Option},
 * returned by {@link BaseOptions#getSupportedOptions()}.
 *
 * <p>Some of the methods used to access the values of options
 * have names that begin with a verb, such as {@link #copyDocfileSubdirs}
 * or {@link #showVersion}. Unless otherwise stated,
 * these methods should all be taken as just accessing the value
 * of the associated option.
 */
public abstract class BaseOptions {

    //<editor-fold desc="Option values">

    /**
     * Argument for command-line option {@code --allow-script-in-comments}.
     * Allow JavaScript in doc comments.
     */
    private boolean allowScriptInComments = false;

    /**
     * Argument for command-line option {@code -docfilessubdirs}.
     * True if we should recursively copy the doc-file subdirectories
     */
    private boolean copyDocfileSubdirs = false;

    /**
     * Arguments for command-line option {@code -tag} and {@code -taglet}.
     */
    private final LinkedHashSet<List<String>> customTagStrs = new LinkedHashSet<>();

    /**
     * Argument for command-line option {@code -d}.
     * Destination directory name, in which doclet will generate the entire
     * documentation. Default is current directory.
     */
    private String destDirName = "";

    /**
     * Argument for command-line option {@code --disable-javafx-strict-checks}.
     * Primarily used to disable strict checks in the regression
     * tests allowing those tests to be executed successfully, for
     * instance, with OpenJDK builds which may not contain FX libraries.
     */
    private boolean disableJavaFxStrictChecks = false;

    /**
     * Argument for command-line option {@code -docencoding}.
     * Encoding for this document. Default is default encoding for this
     * platform.
     */
    private String docEncoding = null;

    /**
     * Argument for command-line option {@code ???}.
     * Destination directory name, in which doclet will copy the doc-files to.
     */
    private String docFileDestDirName = "";

    /**
     * Argument for hidden command-line option {@code --dump-on-error}.
     */
    private boolean dumpOnError = false;

    /**
     * Argument for command-line option {@code -encoding}.
     * Encoding for this document. Default is default encoding for this
     * platform.
     */
    private String encoding = null;

    /**
     * Argument for command-line option {@code -excludedocfilessubdir}.
     * The set of doc-file subdirectories to exclude.
     */
    private Set<String> excludedDocFileDirs;

    /**
     * Argument for command-line option {@code -noqualifier}.
     * The set of qualifiers to exclude.
     */
    private Set<String> excludedQualifiers;

    /**
     * Arguments for command-line option {@code -group}
     */
    private List<Utils.Pair<String, String>> groupPairs;

    /**
     * Argument for command-line option {@code --javafx} or {@code -javafx}.
     * Generate documentation for JavaFX getters and setters automatically
     * by copying it from the appropriate property definition.
     */
    private boolean javafx = false;

    /**
     * Argument for command-line option {@code -keywords}.
     * True if user wants to add member names as meta keywords.
     * Set to false because meta keywords are ignored in general
     * by most Internet search engines.
     */
    private boolean keywords = false;

    /**
     * Arguments for command-line option {@code -link}.
     */
    // A list containing urls
    private final List<String> linkList = new ArrayList<>();

    /**
     * Arguments for command-line option {@code -linkoffline}.
     */
    // A list of pairs containing urls and package list
    private final List<Utils.Pair<String, String>> linkOfflineList = new ArrayList<>();

    /**
     * Location of alternative platform link properties file.
     */
    private String linkPlatformProperties;

    /**
     * Argument for command-line option {@code -linksource}.
     * True if we should generate browsable sources.
     */
    private boolean linkSource = false;

    /**
     * Argument for command-line option {@code -nocomment}.
     * True if user wants to suppress descriptions and tags.
     */
    private boolean noComment = false;

    /**
     * Argument for command-line option {@code -nodeprecated}.
     * Don't generate deprecated API information at all, if -nodeprecated
     * option is used. <code>nodeprecated</code> is set to true if
     * -nodeprecated option is used. Default is generate deprecated API
     * information.
     */
    private boolean noDeprecated = false;

    /**
     * Argument for command-line option {@code --no-platform-links}.
     * True if command-line option "--no-platform-links" is used. Default value is
     * false.
     */
    private boolean noPlatformLinks = false;

    /**
     * Argument for command-line option {@code -nosince}.
     * True if command-line option "-nosince" is used. Default value is
     * false.
     */
    private boolean noSince = false;

    /**
     * Argument for command-line option {@code -notimestamp}.
     * True if user wants to suppress time stamp in output.
     * Default is false.
     */
    private boolean noTimestamp = false;

    /**
     * Argument for command-line option {@code -quiet}.
     * Suppress all messages
     */
    private boolean quiet = false;

    /**
     * Argument for command-line option {@code -serialwarn}.
     * This is true if option "-serialwarn" is used. Default value is false to
     * suppress excessive warnings about serial tag.
     */
    private boolean serialWarn = false;

    /**
     * Argument for command-line option {@code -author}.
     * Generate author specific information for all the classes if @author
     * tag is used in the doc comment and if -author option is used.
     * <code>showauthor</code> is set to true if -author option is used.
     * Default is don't show author information.
     */
    private boolean showAuthor = false;

    /**
     * Argument for command-line option {@code --show-taglets}.
     * Show taglets (internal debug switch)
     */
    private boolean showTaglets = false;

    /**
     * Argument for command-line option {@code -version}.
     * Generate version specific information for the all the classes
     * if @version tag is used in the doc comment and if -version option is
     * used. {@code showVersion} is set to true if -version option is
     * used. Default is don't show version information.
     */
    private boolean showVersion = false;

    /**
     * Argument for command line option {@code --since}.
     * Specifies a list of release names for which to document API changes.
     */
    private List<String> since = List.of();

    /**
     * Argument for command line option {@code --since-label}.
     * Specifies custom text to use as heading of New API page.
     */
    private String sinceLabel;

    /**
     * Argument for command-line option {@code -sourcetab}.
     * The specified amount of space between tab stops.
     */
    private int sourceTabSize;

    /**
     * Value for command-line option {@code --override-methods summary}
     * or  {@code --override-methods detail}.
     * Specifies whether those methods that override a super-type's method
     * with no changes to the API contract should be summarized in the
     * footnote section.
     */
    private boolean summarizeOverriddenMethods = false;

    /**
     * Argument for command-line option {@code -tagletpath}.
     * The path to Taglets
     */
    private String tagletPath = null;

    //</editor-fold>

    private final BaseConfiguration config;

    protected BaseOptions(BaseConfiguration config) {
        this.config = config;

        excludedDocFileDirs = new HashSet<>();
        excludedQualifiers = new HashSet<>();
        sourceTabSize = DocletConstants.DEFAULT_TAB_STOP_LENGTH;
        groupPairs = new ArrayList<>(0);
    }

    public Set<? extends Option> getSupportedOptions() {
        Resources resources = config.getDocResources();
        Messages messages = config.getMessages();
        Reporter reporter = config.getReporter();

        List<Option> options = List.of(
                new Option(resources, "-author") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        showAuthor = true;
                        return true;
                    }
                },

                new Option(resources, "-d", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        destDirName = addTrailingFileSep(args.get(0));
                        return true;
                    }
                },

                new Option(resources, "-docencoding", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        docEncoding = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-docfilessubdirs") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        copyDocfileSubdirs = true;
                        return true;
                    }
                },

                new Hidden(resources, "-encoding", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        encoding = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-excludedocfilessubdir", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        addToSet(excludedDocFileDirs, args.get(0));
                        return true;
                    }
                },

                new Option(resources, "-group", 2) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        groupPairs.add(new Utils.Pair<>(args.get(0), args.get(1)));
                        return true;
                    }
                },

                new Option(resources, "--javafx -javafx") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        javafx = true;
                        return true;
                    }
                },

                new Option(resources, "-keywords") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        keywords = true;
                        return true;
                    }
                },

                new Option(resources, "-link", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        linkList.add(args.get(0));
                        return true;
                    }
                },

                new Option(resources, "-linksource") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        linkSource = true;
                        return true;
                    }
                },

                new Option(resources, "-linkoffline", 2) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        linkOfflineList.add(new Utils.Pair<>(args.get(0), args.get(1)));
                        return true;
                    }
                },

                new Option(resources, "--link-platform-properties", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        linkPlatformProperties = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-nocomment") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noComment = true;
                        return true;
                    }
                },

                new Option(resources, "-nodeprecated") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noDeprecated = true;
                        return true;
                    }
                },

                new Option(resources, "-nosince") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noSince = true;
                        return true;
                    }
                },

                new Option(resources, "-notimestamp") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noTimestamp = true;
                        return true;
                    }
                },

                new Option(resources, "-noqualifier", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        addToSet(excludedQualifiers, args.get(0));
                        return true;
                    }
                },

                new Option(resources, "--no-platform-links") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noPlatformLinks = true;
                        return true;
                    }
                },

                new Option(resources, "--override-methods", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        String o = args.get(0);
                        switch (o) {
                            case "summary":
                                summarizeOverriddenMethods = true;
                                break;
                            case "detail":
                                summarizeOverriddenMethods = false;
                                break;
                            default:
                                reporter.print(ERROR,
                                        resources.getText("doclet.Option_invalid",o, "--override-methods"));
                                return false;
                        }
                        return true;
                    }
                },

                new Hidden(resources, "-quiet") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        quiet = true;
                        return true;
                    }
                },

                new Option(resources, "-serialwarn") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        serialWarn = true;
                        return true;
                    }
                },

                new Option(resources, "--since", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        since = Arrays.stream(args.get(0).split(",")).map(String::trim).toList();
                        return true;
                    }
                },

                new Option(resources, "--since-label", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        sinceLabel = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-sourcetab", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        linkSource = true;
                        try {
                            sourceTabSize = Integer.parseInt(args.get(0));
                        } catch (NumberFormatException e) {
                            //Set to -1 so that warning will be printed
                            //to indicate what is valid argument.
                            sourceTabSize = -1;
                        }
                        if (sourceTabSize <= 0) {
                            messages.warning("doclet.sourcetab_warning");
                            sourceTabSize = DocletConstants.DEFAULT_TAB_STOP_LENGTH;
                        }
                        return true;
                    }
                },

                new Option(resources, "-tag", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(opt);
                        list.add(args.get(0));
                        customTagStrs.add(list);
                        return true;
                    }
                },

                new Option(resources, "-taglet", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(opt);
                        list.add(args.get(0));
                        customTagStrs.add(list);
                        return true;
                    }
                },

                new Option(resources, "-tagletpath", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        tagletPath = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-version") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        showVersion = true;
                        return true;
                    }
                },

                new Hidden(resources, "--dump-on-error") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        dumpOnError = true;
                        return true;
                    }
                },

                new Option(resources, "--allow-script-in-comments") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        allowScriptInComments = true;
                        return true;
                    }
                },

                new Hidden(resources, "--disable-javafx-strict-checks") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        disableJavaFxStrictChecks = true;
                        return true;
                    }
                },

                new Hidden(resources, "--show-taglets") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        showTaglets = true;
                        return true;
                    }
                }
        );
        return new TreeSet<>(options);
    }

    /**
     * This checks for the validity of the options used by the user.
     * As of this writing, this checks only docencoding.
     *
     * @return true if all the options are valid.
     */
    protected boolean generalValidOptions() {
        if (docEncoding != null) {
            if (!checkOutputFileEncoding(docEncoding)) {
                return false;
            }
        }
        if (docEncoding == null && (encoding != null && !encoding.isEmpty())) {
            if (!checkOutputFileEncoding(encoding)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check the validity of the given Source or Output File encoding on this
     * platform.
     *
     * @param docencoding output file encoding.
     */
    private boolean checkOutputFileEncoding(String docencoding) {
        OutputStream ost = new ByteArrayOutputStream();
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(ost, docencoding);
        } catch (UnsupportedEncodingException exc) {
            config.reporter.print(ERROR,
                    config.getDocResources().getText("doclet.Encoding_not_supported", docencoding));
            return false;
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (IOException exc) {
            }
        }
        return true;
    }

    private void addToSet(Set<String> s, String str) {
        StringTokenizer st = new StringTokenizer(str, ":");
        String current;
        while (st.hasMoreTokens()) {
            current = st.nextToken();
            s.add(current);
        }
    }

    /**
     * Add a trailing file separator, if not found. Remove superfluous
     * file separators if any. Preserve the front double file separator for
     * UNC paths.
     *
     * @param path Path under consideration.
     * @return String Properly constructed path string.
     */
    protected static String addTrailingFileSep(String path) {
        String fs = System.getProperty("file.separator");
        String dblfs = fs + fs;
        int indexDblfs;
        while ((indexDblfs = path.indexOf(dblfs, 1)) >= 0) {
            path = path.substring(0, indexDblfs) +
                    path.substring(indexDblfs + fs.length());
        }
        if (!path.endsWith(fs))
            path += fs;
        return path;
    }

    /**
     * Argument for command-line option {@code --allow-script-in-comments}.
     * Allow JavaScript in doc comments.
     */
    boolean allowScriptInComments() {
        return allowScriptInComments;
    }

    /**
     * Argument for command-line option {@code -docfilessubdirs}.
     * True if we should recursively copy the doc-file subdirectories
     */
    public boolean copyDocfileSubdirs() {
        return copyDocfileSubdirs;
    }

    /**
     * Arguments for command-line option {@code -tag} and {@code -taglet}.
     */
    LinkedHashSet<List<String>> customTagStrs() {
        return customTagStrs;
    }

    /**
     * Argument for command-line option {@code -d}.
     * Destination directory name, in which doclet will generate the entire
     * documentation. Default is current directory.
     */
    String destDirName() {
        return destDirName;
    }

    /**
     * Argument for command-line option {@code --disable-javafx-strict-checks}.
     * Primarily used to disable strict checks in the regression
     * tests allowing those tests to be executed successfully, for
     * instance, with OpenJDK builds which may not contain FX libraries.
     */
    boolean disableJavaFxStrictChecks() {
        return disableJavaFxStrictChecks;
    }

    /**
     * Argument for command-line option {@code -docencoding}.
     * Encoding for this document. Default is default encoding for this
     * platform.
     */
    public String docEncoding() {
        return docEncoding;
    }

    public void setDocEncoding(String docEncoding) {
        this.docEncoding = docEncoding;
    }


    /**
     * Argument for command-line option {@code ???}.
     * Destination directory name, in which doclet will copy the doc-files to.
     */
    String docFileDestDirName() {
        return docFileDestDirName;
    }

    /**
     * Argument for hidden command-line option {@code --dump-on-error}.
     */
    boolean dumpOnError() {
        return dumpOnError;
    }

    /**
     * Argument for command-line option {@code -encoding}.
     * Encoding for this document. Default is default encoding for this
     * platform.
     */
    public String encoding() {
        return encoding;
    }

    /**
     * Argument for command-line option {@code -excludedocfilessubdir}.
     * The set of doc-file subdirectories to exclude.
     */
    Set<String> excludedDocFileDirs() {
        return excludedDocFileDirs;
    }

    /**
     * Argument for command-line option {@code -noqualifier}.
     * The set of qualifiers to exclude.
     */
    Set<String> excludedQualifiers() {
        return excludedQualifiers;
    }

    /**
     * Arguments for command-line option {@code -group}
     */
    List<Utils.Pair<String, String>> groupPairs() {
        return groupPairs;
    }

    /**
     * Argument for command-line option {@code --javafx} or {@code -javafx}.
     * Generate documentation for JavaFX getters and setters automatically
     * by copying it from the appropriate property definition.
     */
    public boolean javafx() {
        return javafx;
    }

    public void setJavaFX(boolean javafx) {
        this.javafx = javafx;
    }

    /**
     * Argument for command-line option {@code -keywords}.
     * True if user wants to add member names as meta keywords.
     * Set to false because meta keywords are ignored in general
     * by most Internet search engines.
     */
    public boolean keywords() {
        return keywords;
    }

    /**
     * Arguments for command-line option {@code -link}.
     */
    List<String> linkList() {
        return linkList;
    }

    /**
     * Arguments for command-line option {@code -linkoffline}.
     */
    List<Utils.Pair<String, String>> linkOfflineList() {
        return linkOfflineList;
    }

    /**
     * Argument for command-line option {@code --link-platform-properties}.
     */
    String linkPlatformProperties() {
        return linkPlatformProperties;
    }

    /**
     * Argument for command-line option {@code -linksource}.
     * True if we should generate browsable sources.
     */
    public boolean linkSource() {
        return linkSource;
    }

    /**
     * Argument for command-line option {@code -nocomment}.
     * True if user wants to suppress descriptions and tags.
     */
    public boolean noComment() {
        return noComment;
    }

    /**
     * Argument for command-line option {@code -nodeprecated}.
     * Don't generate deprecated API information at all if -nodeprecated
     * option is used. {@code noDeprecated} is set to {@code true} if
     * {@code -nodeprecated} option is used.
     * Default is generate deprecated API information.
     */
    public boolean noDeprecated() {
        return noDeprecated;
    }

    /**
     * Argument for command-line option {@code --no-platform-links}.
     * True if command-line option {@code --no-platform-links"} is used.
     * Default value is false.
     */
    public boolean noPlatformLinks() {
        return noPlatformLinks;
    }

    /**
     * Argument for command-line option {@code -nosince}.
     * True if command-line option {@code -nosince"} is used.
     * Default value is false.
     */
    public boolean noSince() {
        return noSince;
    }

    /**
     * Argument for command-line option {@code -notimestamp}.
     * True if user wants to suppress time stamp in output.
     * Default is false.
     */
    public boolean noTimestamp() {
        return noTimestamp;
    }

    /**
     * Argument for command-line option {@code -quiet}.
     * Suppress all messages
     */
    boolean quiet() {
        return quiet;
    }

    /**
     * Argument for command-line option {@code -serialwarn}.
     * This is true if option "-serialwarn" is used. Default value is false to
     * suppress excessive warnings about serial tag.
     */
    public boolean serialWarn() {
        return serialWarn;
    }

    /**
     * Argument for command-line option {@code -author}.
     * Generate author specific information for all the classes if @author
     * tag is used in the doc comment and if -author option is used.
     * <code>showauthor</code> is set to true if -author option is used.
     * Default is don't show author information.
     */
    public boolean showAuthor() {
        return showAuthor;
    }

    /**
     * Argument for command-line option {@code --show-taglets}.
     * Show taglets (internal debug switch)
     */
    public boolean showTaglets() {
        return showTaglets;
    }

    /**
     * Argument for command-line option {@code -version}.
     * Generate version specific information for the all the classes
     * if @version tag is used in the doc comment and if -version option is
     * used. {@code showVersion} is set to true if -version option is
     * used. Default is don't show version information.
     */
    public boolean showVersion() {
        return showVersion;
    }

    /**
     * Arguments for command line option {@code --since}.
     */
    public List<String> since() {
        return Collections.unmodifiableList(since);
    }

    /**
     * Arguments for command line option {@code --since-label}.
     */
    public String sinceLabel() {
        return sinceLabel;
    }

    /**
     * Argument for command-line option {@code -sourcetab}.
     * The specified amount of space between tab stops.
     */
    public int sourceTabSize() {
        return sourceTabSize;
    }

    /**
     * Value for command-line option {@code --override-methods summary}
     * or  {@code --override-methods detail}.
     * Specifies whether those methods that override a super-type's method
     * with no changes to the API contract should be summarized in the
     * footnote section.
     */
    public boolean summarizeOverriddenMethods() {
        return summarizeOverriddenMethods;
    }

    /**
     * Argument for command-line option {@code -tagletpath}.
     * The path to Taglets
     */
    public String tagletPath() {
        return tagletPath;
    }

    protected abstract static class Option implements Doclet.Option, Comparable<Option> {
        private final String[] names;
        private final String parameters;
        private final String description;
        private final int argCount;

        protected Option(Resources resources, String name, int argCount) {
            this(resources, null, name, argCount);
        }

        protected Option(Resources resources, String keyBase, String name, int argCount) {
            this.names = name.trim().split("\\s+");
            if (keyBase == null) {
                keyBase = "doclet.usage." + Utils.toLowerCase(names[0]).replaceAll("^-+", "");
            }
            String desc = getOptionsMessage(resources, keyBase + ".description");
            if (desc.isEmpty()) {
                this.description = "<MISSING KEY>";
                this.parameters = "<MISSING KEY>";
            } else {
                this.description = desc;
                this.parameters = getOptionsMessage(resources, keyBase + ".parameters");
            }
            this.argCount = argCount;
        }

        protected Option(Resources resources, String name) {
            this(resources, name, 0);
        }

        private String getOptionsMessage(Resources resources, String key) {
            try {
                return resources.getText(key);
            } catch (MissingResourceException ignore) {
                return "";
            }
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return Arrays.asList(names);
        }

        @Override
        public String getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            return Arrays.toString(names);
        }

        @Override
        public int getArgumentCount() {
            return argCount;
        }

        public boolean matches(String option) {
            for (String name : names) {
                boolean matchCase = name.startsWith("--");
                if (option.startsWith("--") && option.contains("=")) {
                    return name.equals(option.substring(option.indexOf("=") + 1));
                } else if (matchCase) {
                    return name.equals(option);
                }
                return name.equalsIgnoreCase(option);
            }
            return false;
        }

        @Override
        public int compareTo(Option that) {
            return this.getNames().get(0).compareTo(that.getNames().get(0));
        }
    }

    protected abstract static class XOption extends Option {

        public XOption(Resources resources, String prefix, String name, int argCount) {
            super(resources, prefix, name, argCount);
        }

        public XOption(Resources resources, String name, int argCount) {
            super(resources, name, argCount);
        }

        public XOption(Resources resources, String name) {
            this(resources, name, 0);
        }

        @Override
        public Option.Kind getKind() {
            return Kind.EXTENDED;
        }
    }

    protected abstract static class Hidden extends Option {

        public Hidden(Resources resources, String name, int argCount) {
            super(resources, name, argCount);
        }

        public Hidden(Resources resources, String name) {
            this(resources, name, 0);
        }

        @Override
        public Option.Kind getKind() {
            return Kind.OTHER;
        }
    }
}
