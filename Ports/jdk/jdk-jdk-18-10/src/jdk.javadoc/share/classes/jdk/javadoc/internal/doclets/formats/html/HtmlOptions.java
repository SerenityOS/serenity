/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.Resources;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclint.DocLint;

/**
 * Storage for all options supported by the
 * {@link jdk.javadoc.doclet.StandardDoclet standard doclet},
 * including the format-independent options handled
 * by {@link BaseOptions}.
 *
 * <p>Some of the methods used to access the values of options
 * have names that begin with a verb, such as {@link #createOverview}
 * or {@link #splitIndex}. Unless otherwise stated,
 * these methods should all be taken as just accessing the value
 * of the associated option.
 *
 */
public class HtmlOptions extends BaseOptions {
    //<editor-fold desc="Option values">
    /**
     * Argument for command-line option {@code --add-stylesheet}.
     */
    private List<String> additionalStylesheets = new ArrayList<>();

    /**
     * Argument for command-line option {@code -bottom}.
     */
    private String bottom = "";

    /**
     * Argument for command-line option {@code -charset}.
     * The META charset tag used for cross-platform viewing.
     */
    private String charset = null;

    /**
     * Argument for command-line option {@code -use}.
     * True if command-line option "-use" is used. Default value is false.
     */
    private boolean classUse = false;

    /**
     * Argument for command-line option {@code -noindex}.
     * False if command-line option "-noindex" is used. Default value is true.
     */
    private boolean createIndex = true;

    /**
     * Argument for command-line option {@code -overview}.
     * This is true if option "-overview" is used or option "-overview" is not
     * used and number of packages is more than one.
     */
    private boolean createOverview = false;

    /**
     * Argument for command-line option {@code -notree}.
     * False if command-line option "-notree" is used. Default value is true.
     */
    private boolean createTree = true;

    /**
     * Arguments for command-line option {@code -Xdoclint} and friends.
     * Collected set of doclint options.
     */
    private List<String> doclintOpts = new ArrayList<>();

    /**
     * Argument for command-line option {@code -Xdocrootparent}.
     */
    private String docrootParent = "";

    /**
     * Argument for command-line option {@code -doctitle}.
     */
    private String docTitle = "";

    /**
     * Argument for command-line option {@code -header}.
     */
    private String header = "";

    /**
     * Argument for command-line option {@code -helpfile}.
     */
    private String helpFile = "";

    /**
     * Argument for command-line option {@code --legal-notices}.
     */
    private String legalNotices = "";

    /**
     * Argument for command-line option {@code -nodeprecatedlist}.
     * True if command-line option "-nodeprecatedlist" is used. Default value is
     * false.
     */
    private boolean noDeprecatedList = false;

    /**
     * Argument for command-line option {@code -nohelp}.
     * True if command-line option "-nohelp" is used. Default value is false.
     */
    private boolean noHelp = false;

    /**
     * Argument for command-line option {@code -nonavbar}.
     * True if command-line option "-nonavbar" is used. Default value is false.
     */
    private boolean noNavbar = false;

    /**
     * Argument for command-line option {@code -nooverview}.
     * True if command-line option "-nooverview" is used. Default value is
     * false
     */
    private boolean noOverview = false;

    /**
     * Argument for command-line option {@code -overview}.
     * The overview path specified with "-overview" flag.
     */
    private String overviewPath = null;

    /**
     * Argument for command-line option {@code -packagesheader}.
     */
    private String packagesHeader = "";

    /**
     * Argument for command-line option {@code -splitindex}.
     * True if command-line option "-splitindex" is used. Default value is
     * false.
     */
    private boolean splitIndex = false;

    /**
     * Argument for command-line option {@code -stylesheetfile}.
     */
    private String stylesheetFile = "";

    /**
     * Argument for command-line option {@code -top}.
     */
    private String top = "";

    /**
     * Argument for command-line option {@code -windowtitle}.
     */
    private String windowTitle = "";
    //</editor-fold>

    private HtmlConfiguration config;

    HtmlOptions(HtmlConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        Messages messages = config.getMessages();
        Resources resources = messages.getResources();

        List<Option> options = List.of(
                new Option(resources, "--add-stylesheet", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        additionalStylesheets.add(args.get(0));
                        return true;
                    }
                },

                new Option(resources, "-bottom", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        bottom = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-charset", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        charset = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-doctitle", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        docTitle = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-footer", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        messages.warning("doclet.footer_specified");
                        return true;
                    }
                },

                new Option(resources, "-header", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        header = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-helpfile", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        if (noHelp) {
                            messages.error("doclet.Option_conflict", "-helpfile", "-nohelp");
                            return false;
                        }
                        if (!helpFile.isEmpty()) {
                            messages.error("doclet.Option_reuse", "-helpfile");
                            return false;
                        }
                        helpFile = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-html5") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        return true;
                    }
                },

                new XOption(resources, "--legal-notices", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        legalNotices = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-nohelp") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        noHelp = true;
                        if (!helpFile.isEmpty()) {
                            messages.error("doclet.Option_conflict", "-nohelp", "-helpfile");
                            return false;
                        }
                        return true;
                    }
                },

                new Option(resources, "-nodeprecatedlist") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        noDeprecatedList = true;
                        return true;
                    }
                },

                new Option(resources, "-noindex") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        createIndex = false;
                        if (splitIndex) {
                            messages.error("doclet.Option_conflict", "-noindex", "-splitindex");
                            return false;
                        }
                        return true;
                    }
                },

                new Option(resources, "-nonavbar") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        noNavbar = true;
                        return true;
                    }
                },

                new Hidden(resources, "-nooverview") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        noOverview = true;
                        if (overviewPath != null) {
                            messages.error("doclet.Option_conflict", "-nooverview", "-overview");
                            return false;
                        }
                        return true;
                    }
                },

                new Option(resources, "-notree") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        createTree = false;
                        return true;
                    }
                },

                new Option(resources, "-overview", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        overviewPath = args.get(0);
                        if (noOverview) {
                            messages.error("doclet.Option_conflict", "-overview", "-nooverview");
                            return false;
                        }
                        return true;
                    }
                },

                new Hidden(resources, "-packagesheader", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        packagesHeader = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-splitindex") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        splitIndex = true;
                        if (!createIndex) {
                            messages.error("doclet.Option_conflict", "-splitindex", "-noindex");
                            return false;
                        }
                        return true;
                    }
                },

                new Option(resources, "--main-stylesheet -stylesheetfile", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        stylesheetFile = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-top", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        top = args.get(0);
                        return true;
                    }
                },

                new Option(resources, "-use") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        classUse = true;
                        return true;
                    }
                },

                new Option(resources, "-windowtitle", 1) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        windowTitle = args.get(0).replaceAll("<.*?>", "");
                        return true;
                    }
                },

                new XOption(resources, "-Xdoclint") {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        doclintOpts.add(DocLint.XMSGS_OPTION);
                        return true;
                    }
                },

                new XOption(resources, "doclet.usage.xdoclint-extended", "-Xdoclint:", 0) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        String dopt = opt.replace("-Xdoclint:", DocLint.XMSGS_CUSTOM_PREFIX);
                        if (dopt.contains("/")) {
                            messages.error("doclet.Option_doclint_no_qualifiers");
                            return false;
                        }
                        if (!(new DocLint()).isValidOption(dopt)) {
                            messages.error("doclet.Option_doclint_invalid_arg");
                            return false;
                        }
                        doclintOpts.add(dopt);
                        return true;
                    }
                },

                new XOption(resources, "doclet.usage.xdoclint-package", "-Xdoclint/package:", 0) {
                    @Override
                    public boolean process(String opt,  List<String> args) {
                        String dopt = opt.replace("-Xdoclint/package:", DocLint.XCHECK_PACKAGE);
                        if (!(new DocLint()).isValidOption(dopt)) {
                            messages.error("doclet.Option_doclint_package_invalid_arg");
                            return false;
                        }
                        doclintOpts.add(dopt);
                        return true;
                    }
                },

                new XOption(resources, "-Xdocrootparent", 1) {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        docrootParent = args.get(0);
                        try {
                            new URL(docrootParent);
                        } catch (MalformedURLException e) {
                            messages.error("doclet.MalformedURL", docrootParent);
                            return false;
                        }
                        return true;
                    }
                },

                new XOption(resources, "--no-frames") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        messages.warning("doclet.NoFrames_specified");
                        return true;
                    }
                }
        );
        Set<BaseOptions.Option> allOptions = new TreeSet<>();
        allOptions.addAll(options);
        allOptions.addAll(super.getSupportedOptions());
        return allOptions;
    }

    protected boolean validateOptions() {
        // check shared options
        if (!generalValidOptions()) {
            return false;
        }

        Messages messages = config.getMessages();

        // check if helpfile exists
        if (!helpFile.isEmpty()) {
            DocFile help = DocFile.createFileForInput(config, helpFile);
            if (!help.exists()) {
                messages.error("doclet.File_not_found", helpFile);
                return false;
            }
        }
        // check if stylesheetFile exists
        if (!stylesheetFile.isEmpty()) {
            DocFile stylesheet = DocFile.createFileForInput(config, stylesheetFile);
            if (!stylesheet.exists()) {
                messages.error("doclet.File_not_found", stylesheetFile);
                return false;
            }
        }
        // check if additional stylesheets exists
        for (String ssheet : additionalStylesheets) {
            DocFile ssfile = DocFile.createFileForInput(config, ssheet);
            if (!ssfile.exists()) {
                messages.error("doclet.File_not_found", ssheet);
                return false;
            }
        }

        // In a more object-oriented world, this would be done by methods on the Option objects.
        // Note that -windowtitle silently removes any and all HTML elements, and so does not need
        // to be handled here.
        Utils utils = config.utils;
        utils.checkJavaScriptInOption("-header", header);
        utils.checkJavaScriptInOption("-top", top);
        utils.checkJavaScriptInOption("-bottom", bottom);
        utils.checkJavaScriptInOption("-doctitle", docTitle);
        utils.checkJavaScriptInOption("-packagesheader", packagesHeader);

        return true;
    }

    /**
     * Argument for command-line option {@code --add-stylesheet}.
     */
    List<String> additionalStylesheets() {
        return additionalStylesheets;
    }

    /**
     * Argument for command-line option {@code -bottom}.
     */
    String bottom() {
        return bottom;
    }

    /**
     * Argument for command-line option {@code -charset}.
     * The META charset tag used for cross-platform viewing.
     */
    String charset() {
        return charset;
    }

    void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Argument for command-line option {@code -use}.
     * True if command-line option "-use" is used. Default value is false.
     */
    public boolean classUse() {
        return classUse;
    }

    /**
     * Argument for command-line option {@code -noindex}.
     * False if command-line option "-noindex" is used. Default value is true.
     */
    public boolean createIndex() {
        return createIndex;
    }

    /**
     * Argument for command-line option {@code -overview}.
     * This is true if option "-overview" is used or option "-overview" is not
     * used and number of packages is more than one.
     */
    public boolean createOverview() {
        return createOverview;
    }

    public void setCreateOverview(boolean createOverview) {
        this.createOverview = createOverview;
    }

    /**
     * Argument for command-line option {@code -notree}.
     * False if command-line option "-notree" is used. Default value is true.
     */
    public boolean createTree() {
        return createTree;
    }

    /**
     * Arguments for command-line option {@code -Xdoclint} and friends.
     * Collected set of doclint options.
     */
    List<String> doclintOpts() {
        return doclintOpts;
    }

    /**
     * Argument for command-line option {@code -Xdocrootparent}.
     */
    String docrootParent() {
        return docrootParent;
    }

    /**
     * Argument for command-line option {@code -doctitle}.
     */
    String docTitle() {
        return docTitle;
    }

    /**
     * Argument for command-line option {@code -header}.
     */
    String header() {
        return header;
    }

    /**
     * Argument for command-line option {@code -helpfile}.
     */
    public String helpFile() {
        return helpFile;
    }

    /**
     * Argument for command-line option {@code --legal-notices}.
     */
    public String legalNotices() {
        return legalNotices;
    }

    /**
     * Argument for command-line option {@code -nodeprecated}.
     * True if command-line option "-nodeprecated" is used. Default value is
     * false.
     */
    public boolean noDeprecatedList() {
        return noDeprecatedList;
    }

    /**
     * Argument for command-line option {@code -nohelp}.
     * True if command-line option "-nohelp" is used. Default value is false.
     */
    public boolean noHelp() {
        return noHelp;
    }

    /**
     * Argument for command-line option {@code -nonavbar}.
     * True if command-line option "-nonavbar" is used. Default value is false.
     */
    public boolean noNavbar() {
        return noNavbar;
    }

    /**
     * Argument for command-line option {@code -nooverview}.
     * True if command-line option "-nooverview" is used. Default value is
     * false
     */
    boolean noOverview() {
        return noOverview;
    }

    /**
     * Argument for command-line option {@code -overview}.
     * The overview path specified with "-overview" flag.
     */
    String overviewPath() {
        return overviewPath;
    }

    /**
     * Argument for command-line option {@code -packagesheader}.
     */
    String packagesHeader() {
        return packagesHeader;
    }

    /**
     * Argument for command-line option {@code -splitindex}.
     * True if command-line option "-splitindex" is used. Default value is
     * false.
     */
    public boolean splitIndex() {
        return splitIndex;
    }

    /**
     * Argument for command-line option {@code -stylesheetfile}.
     */
    String stylesheetFile() {
        return stylesheetFile;
    }

    /**
     * Argument for command-line option {@code -top}.
     */
    String top() {
        return top;
    }

    /**
     * Argument for command-line option {@code -windowtitle}.
     */
    String windowTitle() {
        return windowTitle;
    }
}
