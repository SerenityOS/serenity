/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.main.Option.InvalidValueException;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import static jdk.javadoc.internal.tool.Main.Result.OK;
import static jdk.javadoc.internal.tool.ToolOptions.ToolOption.Kind.*;

/**
 * Storage and support for javadoc tool options, as distinct from
 * the options supported by any doclet that may be in use.
 * The tool options includes those options which are delegated
 * to javac and/or the file manager, such as options to set
 * the source level, and path options to locate the files to be
 * documented.
 *
 * <p>Some of the methods used to access the values of options
 * have names that begin with a verb, such as {@link #expandRequires}
 * or {@link #ignoreSourceErrors}. Unless otherwise stated,
 * these methods should all be taken as just accessing the value
 * of the associated option.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ToolOptions {
    // The following are the names of options handled in the first pass of option decoding,
    // in Start.preprocess.
    static final String DOCLET = "-doclet";
    static final String DOCLET_PATH = "-docletpath";
    static final String DUMP_ON_ERROR = "--dump-on-error";
    static final String AT = "@";
    static final String J = "-J";
    static final String LOCALE = "-locale";

    /**
     * Argument for command-line option {@code -breakiterator}.
     */
    private boolean breakIterator = false;

    /**
     * Argument for command-line option {@code --dump-on-error}.
     * Dump stack traces for debugging etc.
     * Similar to javac {@code -doe}.
     */
    private boolean dumpOnError = false;

    /**
     * Argument for command-line option {@code -exclude}.
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * Argument for command-line option {@code --expand-requires}.
     */
    private AccessKind expandRequires;

    /**
     * Argument for command-line option {@code --ignore-source-errors}.
     */
    private boolean ignoreSourceErrors;

    /**
     * Argument for command-line option {@code --module}.
     */
    private List<String> modules = new ArrayList<>();

    /**
     * Argument for command-line option {@code -Werror}.
     * Set by -Werror.
     */
    private boolean rejectWarnings = false;

    /**
     * Argument for command-line option {@code --show-members}.
     */
    private AccessKind showMembersAccess;

    /**
     * Argument for command-line option {@code --show-types}.
     */
    private AccessKind showTypesAccess;

    /**
     * Argument for command-line option {@code --show-packages}.
     */
    private AccessKind showPackagesAccess;

    /**
     * Argument for command-line option {@code --show-module-contents}.
     */
    private AccessKind showModuleContents;

    /**
     * Argument for command-line option {@code -quiet}.
     */
    private boolean quiet;

    /**
     * Argument for command-line option {@code -subpackages}.
     */
    private List<String> subpackages = new ArrayList<>();

    /**
     * Argument for command-line option {@code -verbose}.
     */
    private boolean verbose;

    /**
     * Argument for command-line option {@code -xclasses}.
     * If true, names on the command line that would normally be
     * treated as package names are treated as class names instead.
     */
    private boolean xclasses = false;

    /**
     * Options to be given to the file manager, such as path options
     * indicating where to find files to be documented.
     */
    private final Map<Option, String> fileManagerOpts;

    /**
     * Options to be given to the underlying compiler front-end,
     * such as options to indicate the source level to be used.
     */
    private final Options compOpts;

    /**
     * The "helper" to be used when processing compiler options.
     */
    private final OptionHelper compilerOptionHelper;

    /**
     * The log to be used to report diagnostics..
     */
    private final JavadocLog log;

    /**
     * The helper for help and version options
     */
    private final ShowHelper showHelper;

    /**
     * Creates an object to handle tool options.
     *
     * @param context the context used to find other tool-related components
     * @param log the log to be used to report diagnostics
     */
    ToolOptions(Context context, JavadocLog log, ShowHelper showHelper) {
        this.log = log;
        this.showHelper = showHelper;
        compOpts = Options.instance(context);
        fileManagerOpts = new LinkedHashMap<>();
        compilerOptionHelper = getOptionHelper();
        setAccessDefault();
    }

    /**
     * Creates a minimal object, just sufficient to check the names of the
     * supported options.
     */
    private ToolOptions() {
        compOpts = null;
        compilerOptionHelper = null;
        fileManagerOpts = null;
        log = null;
        showHelper = null;
    }

    /**
     * Returns the set of options supported by the tool, excluding any options
     * that are managed by the doclet that may be in use.
     *
     * @return the set of options
     */
    public List<ToolOption> getSupportedOptions() {
        return supportedOptions;
    }

    /**
     * Determines if the given option is supported and if so, the
     * number of arguments the option takes.
     *
     * @param option an option
     * @return the number of arguments the given option takes or -1 if
     * the option is not supported
     * @see javax.tools.DocumentationTool#isSupportedOption(String)
     */
    public static int isSupportedOption(String option) {
        ToolOptions t = new ToolOptions();
        for (ToolOption o : t.supportedOptions) {
            for (String name : o.names) {
                if (name.equals(option))
                    return o.hasArg ? 1 : 0;
            }
        }
        return -1;
    }

    /**
     * Returns the option to be used to process an argument such as may be found on
     * the command line.
     *
     * @param arg the argument
     * @return the option
     */
    ToolOption getOption(String arg) {
        String name = arg;
        if (arg.startsWith("--") && arg.contains("=")) {
            name = arg.substring(0, arg.indexOf('='));
        }
        for (ToolOption o : supportedOptions) {
            for (String n : o.names) {
                if (name.equals(n)) {
                    return o;
                }
            }
        }
        return null;
    }

    private List<ToolOption> supportedOptions = List.of(
            // ----- options for underlying compiler -----

            new ToolOption("-bootclasspath", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.BOOT_CLASS_PATH, primaryName, arg);
                }
            },

            new ToolOption("--class-path -classpath -cp", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.CLASS_PATH, primaryName, arg);
                }
            },

            new ToolOption("-extdirs", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.EXTDIRS, primaryName, arg);
                }
            },

            new ToolOption("--source-path -sourcepath", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.SOURCE_PATH, primaryName, arg);
                }
            },

            new ToolOption("--module-source-path", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.MODULE_SOURCE_PATH, primaryName, arg);
                }
            },

            new ToolOption("--upgrade-module-path", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.UPGRADE_MODULE_PATH, primaryName, arg);
                }
            },

            new ToolOption("--system", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.SYSTEM, primaryName, arg);
                }
            },

            new ToolOption("--module-path -p", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.MODULE_PATH, primaryName, arg);
                }
            },

            new ToolOption("--add-modules", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.ADD_MODULES, primaryName, arg);
                }
            },

            new ToolOption("--limit-modules", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.LIMIT_MODULES, primaryName, arg);
                }
            },

            new ToolOption("--module", STANDARD, true) {
                @Override
                public void process(String arg) {
                    modules.addAll(List.of(arg.split(",")));
                }
            },

            new ToolOption("-encoding", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.ENCODING, primaryName, arg);
                }
            },

            new ToolOption("--release", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.RELEASE, primaryName, arg);
                }
            },

            new ToolOption("--source -source", STANDARD, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.SOURCE, primaryName, arg);
                    processCompilerOption(Option.TARGET, Option.TARGET.primaryName, arg);
                }
            },

            new ToolOption("-Xmaxerrs", EXTENDED, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.XMAXERRS, primaryName, arg);
                }
            },

            new ToolOption("-Xmaxwarns", EXTENDED, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.XMAXWARNS, primaryName, arg);
                }
            },

            new ToolOption("--add-reads", EXTENDED, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.ADD_READS, primaryName, arg);
                }
            },

            new ToolOption("--add-exports", EXTENDED, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.ADD_EXPORTS, primaryName, arg);
                }
            },

            new ToolOption("--patch-module", EXTENDED, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.PATCH_MODULE, primaryName, arg);
                }
            },

            new ToolOption("--add-opens", HIDDEN, true) {
                @Override
                public void process(String arg) throws InvalidValueException {
                    processCompilerOption(Option.ADD_OPENS, primaryName, arg);
                }
            },

            new ToolOption("--enable-preview", STANDARD) {
                @Override
                public void process() throws InvalidValueException {
                    processCompilerOption(Option.PREVIEW, primaryName);
                }
            },

            // ----- doclet options -----

            // This option exists so that it is documented in the command-line help.
            // It is implemented in {@link Start#preprocess}.
            new ToolOption(DOCLET, STANDARD, true),

            // This option exists so that it is documented in the command-line help.
            // It is implemented in {@link Start#preprocess}.
            new ToolOption(DOCLET_PATH, STANDARD, true),

            // ----- selection options -----

            new ToolOption("-subpackages", STANDARD, true) {
                @Override
                public void process(String arg) {
                    subpackages.addAll(List.of(arg.split(":")));
                }
            },

            new ToolOption("-exclude", STANDARD, true) {
                @Override
                public void process(String arg) {
                    excludes.addAll(List.of(arg.split(":")));
                }
            },

            // ----- filtering options -----

            new ToolOption("-package", STANDARD) {
                @Override
                public void process() throws OptionException {
                    setSimpleFilter("package");
                }
            },

            new ToolOption("-private", STANDARD) {
                @Override
                public void process() throws OptionException {
                    setSimpleFilter("private");
                }
            },

            new ToolOption("-protected", STANDARD) {
                @Override
                public void process() throws OptionException {
                    setSimpleFilter("protected");
                }
            },

            new ToolOption("-public", STANDARD) {
                @Override
                public void process() throws OptionException {
                    setSimpleFilter("public");
                }
            },

            new ToolOption("--show-members", STANDARD, true) {
                @Override
                public void process(String arg) throws OptionException {
                    setShowMembersAccess(arg);
                }
            },

            new ToolOption("--show-types", STANDARD, true) {
                @Override
                public void process(String arg) throws OptionException {
                    setShowTypesAccess(arg);
                }
            },

            new ToolOption("--show-packages", STANDARD, true) {
                @Override
                public void process(String arg) throws OptionException {
                    setShowPackageAccess(arg);
                }
            },

            new ToolOption("--show-module-contents", STANDARD, true) {
                @Override
                public void process(String arg) throws OptionException {
                    setShowModuleContents(arg);
                }
            },

            new ToolOption("--expand-requires", STANDARD, true) {
                @Override
                public void process(String arg) throws OptionException {
                    setExpandRequires(arg);
                }
            },

            // ----- output control options -----

            new ToolOption("-quiet", STANDARD) {
                @Override
                public void process() {
                    quiet = true;
                }
            },

            new ToolOption("-verbose", STANDARD) {
                @Override
                public void process() {
                    setVerbose();
                }
            },

            // superseded by -Werror, retained for a while for compatibility,
            // although note that it is an undocumented hidden option, and can
            // be removed without warning
            new ToolOption("-Xwerror", HIDDEN) {
                @Override
                public void process() {
                    rejectWarnings = true;
                }
            },

            new ToolOption("-Werror", STANDARD) {
                @Override
                public void process() {
                    rejectWarnings = true;
                }
            },

            // ----- other options -----

            new ToolOption("-breakiterator", STANDARD) {
                @Override
                public void process() {
                    breakIterator = true;
                }
            },

            // This option exists so that it is documented in the command-line help.
            // It is implemented in {@link Start#preprocess}.
            new ToolOption(LOCALE, STANDARD, true),

            new ToolOption("-Xclasses", HIDDEN) {
                @Override
                public void process() {
                    xclasses = true;
                }
            },

            // This option exists so that it is documented in the command-line help.
            // It is implemented in {@link Start#preprocess}.
            new ToolOption(DUMP_ON_ERROR, HIDDEN),

            new ToolOption("--ignore-source-errors", HIDDEN) {
                @Override
                public void process() {
                    ignoreSourceErrors = true;
                }
            },

            // ----- help options -----

            new ToolOption("--help -help -? -h", STANDARD) {
                @Override
                public void process() throws OptionException {
                    throw new OptionException(OK, showHelper::usage);
                }
            },

            new ToolOption("--help-extra -X", STANDARD) {
                @Override
                public void process() throws OptionException {
                    throw new OptionException(OK, showHelper::Xusage);
                }
            },

            // This option exists so that it is documented in the command-line help.
            // It is actually implemented by the launcher, and can only be used when
            // invoking javadoc from the launcher.
            new ToolOption(J, STANDARD, true) {
                @Override
                public void process() {
                    throw new AssertionError("the -J flag should be caught by the launcher.");
                }
            },

            // This option exists so that it is documented in the command-line help.
            // It is actually implemented by expanding argv early on during execution,
            // and can only be used when using the command-line and related interfaces
            // (i.e. not the javax.tools API).
            new ToolOption(AT, STANDARD, true) {
                @Override
                public void process() {
                    throw new AssertionError("the @ option is handled separately");
                }
            },

            new ToolOption("--version", STANDARD) {
                @Override
                public void process() throws OptionException {
                    throw new OptionException(OK, showHelper::version);
                }
            },

            new ToolOption("--full-version", HIDDEN) {
                @Override
                public void process() throws OptionException {
                    throw new OptionException(OK, showHelper::fullVersion);
                }
            });

    /**
     * Base class for all supported tool options.
     */
    static class ToolOption {
        enum Kind { STANDARD, EXTENDED, HIDDEN }

        final String primaryName;
        final List<String> names;
        final Kind kind;
        final boolean hasArg;
        final boolean hasSuffix; // ex: foo:bar or -foo=bar

        ToolOption(String opt, Kind kind) {
            this(opt, kind, false);
        }

        ToolOption(String names, Kind kind, boolean hasArg) {
            this.names = Arrays.asList(names.split("\\s+"));
            this.primaryName = this.names.get(0);
            this.kind = kind;
            this.hasArg = hasArg;
            char lastChar = names.charAt(names.length() - 1);
            this.hasSuffix = lastChar == ':' || lastChar == '=';
        }

        void process(String arg) throws OptionException, Option.InvalidValueException { }

        void process() throws OptionException, Option.InvalidValueException { }

        List<String> getNames() {
            return names;
        }

        String getParameters(JavadocLog log) {
            return (hasArg || primaryName.endsWith(":"))
                    ? log.getText(getKey(primaryName, ".arg"))
                    : null;
        }

        String getDescription(JavadocLog log) {
            return log.getText(getKey(primaryName, ".desc"));
        }

        private String getKey(String optionName, String suffix) {
            return "main.opt."
                    + optionName
                        .replaceAll("^-*", "")              // remove leading '-'
                        .replaceAll("^@", "at")             // handle '@'
                        .replaceAll("[^A-Za-z0-9]+$", "")   // remove trailing non-alphanumeric
                        .replaceAll("[^A-Za-z0-9]", ".")    // replace internal non-alphanumeric
                    + suffix;
        }
    }

    interface ShowHelper {
        /**
         * Show command-line help for the standard options, as requested by
         * the {@code --help} option and its aliases.
         */
        void usage();

        /**
         * Show command-line help for the extended options, as requested by
         * the {@code --help-extended} option and its aliases.
         */
        void Xusage();

        /**
         * Show the basic version information, as requested by the {@code --version} option.
         */
        void version();

        /**
         * Show the full version information, as requested by the {@code --full-version} option.
         */
        void fullVersion();
    }

    //<editor-fold desc="accessor methods">
    /**
     * Argument for command-line option {@code -breakiterator}.
     */
    boolean breakIterator() {
        return breakIterator;
    }

    /**
     * Argument for command-line option {@code --dump-on-error}.
     * Dump stack traces for debugging etc.
     * Similar to javac {@code -doe}.
     */
    boolean dumpOnError() {
        return dumpOnError;
    }

    void setDumpOnError(boolean v) {
        dumpOnError = v;
    }

    /**
     * Argument for command-line option {@code -exclude}.
     */
    List<String> excludes() {
        return excludes;
    }

    /**
     * Argument for command-line option {@code --expand-requires}.
     */
    AccessKind expandRequires() {
        return expandRequires;
    }

    /**
     * Argument for command-line option {@code --ignore-source-errors}.
     */
    boolean ignoreSourceErrors() {
        return ignoreSourceErrors;
    }

    /**
     * Argument for command-line option {@code --module}.
     */
    List<String> modules() {
        return modules;
    }

    /**
     * Argument for command-line option {@code -Werror}.
     * Set by -Werror.
     */
    boolean rejectWarnings() {
        return rejectWarnings;
    }

    /**
     * Argument for command-line option {@code --show-members}.
     */
    AccessKind showMembersAccess() {
        return showMembersAccess;
    }

    /**
     * Argument for command-line option {@code --show-types}.
     */
    AccessKind showTypesAccess() {
        return showTypesAccess;
    }

    /**
     * Argument for command-line option {@code --show-packages}.
     */
    AccessKind showPackagesAccess() {
        return showPackagesAccess;
    }

    /**
     * Argument for command-line option {@code --show-module-contents}.
     */
    AccessKind showModuleContents() {
        return showModuleContents;
    }

    /**
     * Argument for command-line option {@code -quiet}.
     */
    boolean quiet() {
        return quiet;
    }

    /**
     * Argument for command-line option {@code -subpackages}.
     */
    List<String> subpackages() {
        return subpackages;
    }

    /**
     * Argument for command-line option {@code -verbose}.
     */
    boolean verbose() {
        return verbose;
    }

    /**
     * Argument for command-line option {@code -xclasses}.
     * If true, names on the command line that would normally be
     * treated as package names are treated as class names instead.
     */
    boolean xclasses() {
        return xclasses;
    }

    /**
     * Returns the set of options to be used for the instance of the
     * underlying compiler front-end.
     *
     * @return the options
     */
    Options compilerOptions() {
        return compOpts;
    }

    /**
     * Returns the set of options to be used for the file manager.
     *
     * @return the options
     */
    Map<Option, String> fileManagerOptions() {
        return fileManagerOpts;
    }
    //</editor-fold>

    /**
     * Returns an {@code IllegalOptionValue} exception.
     *
     * @param arg the argument to include in the detail message
     * @return the exception
     */
    private IllegalOptionValue illegalOptionValue(String arg) {
        return new IllegalOptionValue(showHelper::usage, log.getText("main.illegal_option_value", arg));
    }

    /**
     * Process a compiler option.
     *
     * @param option the option object to process the command-line option
     * @param opt    the command-line option
     * @throws Option.InvalidValueException if the command-line option is invalid
     */
    void processCompilerOption(Option option, String opt) throws Option.InvalidValueException {
        option.process(compilerOptionHelper, opt);
    }

    /**
     * Process a compiler option.
     *
     * @param option the option object to process the command-line option
     * @param opt    the command-line option
     * @param arg    the argument for the command-line option
     * @throws Option.InvalidValueException if the command-line option is invalid
     */
    private void processCompilerOption(Option option, String opt, String arg) throws Option.InvalidValueException {
        option.process(compilerOptionHelper, opt, arg);
    }

    /**
     * Returns a "helper" to be used when processing compiler options.
     * @return the helper
     */
    private OptionHelper getOptionHelper() {
        return new OptionHelper.GrumpyHelper(log) {
            @Override
            public String get(com.sun.tools.javac.main.Option option) {
                return compOpts.get(option);
            }

            @Override
            public void put(String name, String value) {
                compOpts.put(name, value);
            }

            @Override
            public void remove(String name) {
                compOpts.remove(name);
            }

            @Override
            public boolean handleFileManagerOption(com.sun.tools.javac.main.Option option, String value) {
                fileManagerOpts.put(option, value);
                return true;
            }
        };
    }

    private void setExpandRequires(String arg) throws OptionException {
        switch (arg) {
            case "transitive":
                expandRequires = AccessKind.PUBLIC;
                break;
            case "all":
                expandRequires = AccessKind.PRIVATE;
                break;
            default:
                throw illegalOptionValue(arg);
        }
    }

    private void setShowModuleContents(String arg) throws OptionException {
        switch (arg) {
            case "api":
                showModuleContents = AccessKind.PUBLIC;
                break;
            case "all":
                showModuleContents = AccessKind.PRIVATE;
                break;
            default:
                throw illegalOptionValue(arg);
        }
    }

    private void setShowPackageAccess(String arg) throws OptionException {
        switch (arg) {
            case "exported":
                showPackagesAccess = AccessKind.PUBLIC;
                break;
            case "all":
                showPackagesAccess = AccessKind.PRIVATE;
                break;
            default:
                throw illegalOptionValue(arg);
        }
    }

    private void setShowTypesAccess(String arg) throws OptionException {
        showTypesAccess = getAccessValue(arg);
    }

    private void setShowMembersAccess(String arg) throws OptionException {
        showMembersAccess = getAccessValue(arg);
    }

    private void setSimpleFilter(String arg) throws OptionException {
        setSimpleAccessOption(arg);
    }

    private void setVerbose() {
        compOpts.put("-verbose", "");
        verbose = true;
    }

    private void setSimpleAccessOption(String arg) throws OptionException {
        setAccess(getAccessValue(arg));
    }

    /*
     * This method handles both the simple options -package,
     * -private, so on, in addition to the new ones such as
     * --show-types:public and so on.
     */
    private AccessKind getAccessValue(String arg) throws OptionException {
        int colon = arg.indexOf(':');
        String value = (colon > 0)
                ? arg.substring(colon + 1)
                : arg;
        switch (value) {
            case "public":
                return AccessKind.PUBLIC;
            case "protected":
                return AccessKind.PROTECTED;
            case "package":
                return AccessKind.PACKAGE;
            case "private":
                return AccessKind.PRIVATE;
            default:
                throw illegalOptionValue(value);
        }
    }

    /*
     * Sets all access members to PROTECTED; this is the default.
     */
    private void setAccessDefault() {
        setAccess(AccessKind.PROTECTED);
    }

    /*
     * This sets access to all the allowed kinds in the
     * access members.
     */
    private void setAccess(AccessKind accessValue) {
        for (ElementKind kind : ElementsTable.ModifierFilter.ALLOWED_KINDS) {
            switch (kind) {
                case METHOD:
                    showMembersAccess = accessValue;
                    break;
                case CLASS:
                    showTypesAccess = accessValue;
                    break;
                case PACKAGE:
                    showPackagesAccess = accessValue;
                    break;
                case MODULE:
                    showModuleContents = accessValue;
                    break;
                default:
                    throw new AssertionError("unknown element kind:" + kind);
            }
        }
    }
}
