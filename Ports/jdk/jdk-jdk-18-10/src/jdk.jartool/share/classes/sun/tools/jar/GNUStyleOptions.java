/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jar;

import java.io.File;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jdk.internal.module.ModulePath;
import jdk.internal.module.ModuleResolution;

/**
 * Parser for GNU Style Options.
 */
class GNUStyleOptions {

    static class BadArgs extends Exception {
        static final long serialVersionUID = 0L;

        boolean showUsage;

        BadArgs(String key, String arg) { super(Main.formatMsg(key, arg)); }
        BadArgs(String key) { super(Main.getMsg(key)); }

        BadArgs showUsage(boolean b) {
            showUsage = b;
            return this;
        }
    }

    static Option[] recognizedOptions = {
            // Main operations
            new Option(false, OptionType.MAIN_OPERATION, "--create", "-c") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.iflag || tool.tflag || tool.uflag || tool.xflag || tool.dflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.cflag = true;
                }
            },
            new Option(true, OptionType.MAIN_OPERATION, "--generate-index", "-i") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.tflag || tool.uflag || tool.xflag || tool.dflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.iflag = true;
                    tool.rootjar = arg;
                }
            },
            new Option(false, OptionType.MAIN_OPERATION, "--list", "-t") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.iflag || tool.uflag || tool.xflag || tool.dflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.tflag = true;
                }
            },
            new Option(false, OptionType.MAIN_OPERATION, "--update", "-u") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.iflag || tool.tflag || tool.xflag || tool.dflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.uflag = true;
                }
            },
            new Option(false, OptionType.MAIN_OPERATION, "--extract", "-x") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.iflag  || tool.tflag || tool.uflag || tool.dflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.xflag = true;
                }
            },
            new Option(false, OptionType.MAIN_OPERATION, "--describe-module", "-d") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.iflag  || tool.tflag || tool.uflag || tool.xflag || tool.validate)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.dflag = true;
                }
            },
            new Option(false, OptionType.MAIN_OPERATION, "--validate") {
                void process(Main tool, String opt, String arg) throws BadArgs {
                    if (tool.cflag || tool.iflag  || tool.tflag || tool.uflag || tool.xflag || tool.dflag)
                        throw new BadArgs("error.multiple.main.operations").showUsage(true);
                    tool.validate = true;
                }
            },

            // Additional options
            new Option(true, OptionType.ANY, "--file", "-f") {
                void process(Main jartool, String opt, String arg) {
                    jartool.fname = arg;
                }
            },
            new Option(false, OptionType.ANY, "--verbose", "-v") {
                void process(Main jartool, String opt, String arg) {
                    jartool.vflag = true;
                }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--main-class", "-e") {
                void process(Main jartool, String opt, String arg) {
                    jartool.ename = arg;
                }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--manifest", "-m") {
                void process(Main jartool, String opt, String arg) {
                    jartool.mname = arg;
                }
            },
            new Option(false, OptionType.CREATE_UPDATE, "--no-manifest", "-M") {
                void process(Main jartool, String opt, String arg) {
                    jartool.Mflag = true;
                }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--module-version") {
                void process(Main jartool, String opt, String arg) {
                    jartool.moduleVersion = Version.parse(arg);
                }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--hash-modules") {
                void process(Main jartool, String opt, String arg) throws BadArgs {
                    try {
                        jartool.modulesToHash = Pattern.compile(arg);
                    } catch (PatternSyntaxException e) {
                        throw new BadArgs("err.badpattern", arg).showUsage(true);
                    }
                }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--module-path", "-p") {
                void process(Main jartool, String opt, String arg) {
                    String[] dirs = arg.split(File.pathSeparator);
                    Path[] paths = new Path[dirs.length];
                    int i = 0;
                    for (String dir : dirs) {
                        paths[i++] = Paths.get(dir);
                    }
                    jartool.moduleFinder = ModulePath.of(Runtime.version(), true, paths);
                }
            },
            new Option(false, OptionType.CREATE_UPDATE, "--do-not-resolve-by-default") {
                void process(Main jartool, String opt, String arg) {
                    ModuleResolution mres = jartool.moduleResolution;
                    jartool.moduleResolution = mres.withDoNotResolveByDefault();
                }
                boolean isExtra() { return true; }
            },
            new Option(true, OptionType.CREATE_UPDATE, "--warn-if-resolved") {
                void process(Main jartool, String opt, String arg) throws BadArgs {
                    ModuleResolution mres = ModuleResolution.empty();
                    if (jartool.moduleResolution.doNotResolveByDefault()) {
                        mres.withDoNotResolveByDefault();
                    }
                    if (arg.equals("deprecated")) {
                        jartool.moduleResolution = mres.withDeprecated();
                    } else if (arg.equals("deprecated-for-removal")) {
                        jartool.moduleResolution = mres.withDeprecatedForRemoval();
                    } else if (arg.equals("incubating")) {
                        jartool.moduleResolution = mres.withIncubating();
                    } else {
                        throw new BadArgs("error.bad.reason", arg);
                    }
                }
                boolean isExtra() { return true; }
            },
            new Option(false, OptionType.CREATE_UPDATE_INDEX, "--no-compress", "-0") {
                void process(Main jartool, String opt, String arg) {
                    jartool.flag0 = true;
                }
            },

            // Hidden options
            new Option(false, OptionType.OTHER, "-P") {
                void process(Main jartool, String opt, String arg) {
                    jartool.pflag = true;
                }
                boolean isHidden() { return true; }
            },

            // Other options
            new Option(true, true, OptionType.OTHER, "--help", "-h", "-?") {
                void process(Main jartool, String opt, String arg) throws BadArgs {
                    if (jartool.info == null) {
                        if (arg == null) {
                            jartool.info = GNUStyleOptions::printHelp;  //  Main.Info.HELP;
                            return;
                        }
                        if (!arg.equals("compat"))
                            throw new BadArgs("error.illegal.option", arg).showUsage(true);
                        // jartool.info = Main.Info.COMPAT_HELP;
                        jartool.info = GNUStyleOptions::printCompatHelp;
                    }
                }
            },
            new Option(false, OptionType.OTHER, "--help-extra") {
                void process(Main jartool, String opt, String arg) throws BadArgs {
                    jartool.info = GNUStyleOptions::printHelpExtra;
                }
            },
            new Option(false, OptionType.OTHER, "--version") {
                void process(Main jartool, String opt, String arg) {
                    if (jartool.info == null)
                        jartool.info = GNUStyleOptions::printVersion;
                }
            }
    };

    enum OptionType {
        MAIN_OPERATION("main"),
        ANY("any"),
        CREATE("create"),
        CREATE_UPDATE("create.update"),
        CREATE_UPDATE_INDEX("create.update.index"),
        OTHER("other");

        /** Resource lookup section prefix. */
        final String name;

        OptionType(String name) { this.name = name; }
    }

    static abstract class Option {
        final boolean hasArg;
        final boolean argIsOptional;
        final String[] aliases;
        final OptionType type;

        Option(boolean hasArg, OptionType type, String... aliases) {
            this(hasArg, false, type, aliases);
        }

        Option(boolean hasArg, boolean argIsOptional, OptionType type, String... aliases) {
            this.hasArg = hasArg;
            this.argIsOptional = argIsOptional;
            this.type = type;
            this.aliases = aliases;
        }

        boolean isHidden() { return false; }

        boolean isExtra() { return false; }

        boolean matches(String opt) {
            for (String a : aliases) {
                if (a.equals(opt)) {
                    return true;
                } else if (opt.startsWith("--") && hasArg && opt.startsWith(a + "=")) {
                    return true;
                } else if (opt.startsWith("--help") && opt.startsWith(a + ":")) {
                    return true;
                }
            }
            return false;
        }

        abstract void process(Main jartool, String opt, String arg) throws BadArgs;
    }

    static int parseOptions(Main jartool, String[] args) throws BadArgs {
        int count = 0;
        if (args.length == 0) {
            jartool.info = GNUStyleOptions::printUsageTryHelp;  //  never be here
            return 0;
        }

        // process options
        for (; count < args.length; count++) {
            if (args[count].charAt(0) != '-' || args[count].equals("-C") ||
                args[count].equals("--release"))
                break;

            String name = args[count];
            if (name.equals("-XDsuppress-tool-removal-message")) {
                jartool.suppressDeprecateMsg = true;
                continue;
            }
            Option option = getOption(name);
            String param = null;
            if (option.hasArg) {
                if (name.startsWith("--help")) {  // "special" optional separator
                    if (name.indexOf(':') > 0) {
                        param = name.substring(name.indexOf(':') + 1, name.length());
                    }
                } else if (name.startsWith("--") && name.indexOf('=') > 0) {
                    param = name.substring(name.indexOf('=') + 1, name.length());
                } else if (count + 1 < args.length) {
                    param = args[++count];
                }
                if (!option.argIsOptional &&
                    (param == null || param.isEmpty() || param.charAt(0) == '-')) {
                    throw new BadArgs("error.missing.arg", name).showUsage(true);
                }
            }
            option.process(jartool, name, param);
        }

        return count;
    }

    private static Option getOption(String name) throws BadArgs {
        for (Option o : recognizedOptions) {
            if (o.matches(name)) {
                return o;
            }
        }
        throw new BadArgs("error.unrecognized.option", name).showUsage(true);
    }

    static void printHelpExtra(PrintWriter out) {
        printHelp0(out, true);
    }

    static void printHelp(PrintWriter out) {
        printHelp0(out, false);
    }

    private static void printHelp0(PrintWriter out, boolean printExtra) {
        out.format("%s%n", Main.getMsg("main.help.preopt"));
        for (OptionType type : OptionType.values()) {
            boolean typeHeadingWritten = false;

            for (Option o : recognizedOptions) {
                if (!o.type.equals(type))
                    continue;
                String name = o.aliases[0].substring(1); // there must always be at least one name
                name = name.charAt(0) == '-' ? name.substring(1) : name;
                if (o.isHidden() || name.equals("h")) {
                    continue;
                }
                if (o.isExtra() && !printExtra) {
                    continue;
                }
                if (!typeHeadingWritten) {
                    out.format("%n%s%n", Main.getMsg("main.help.opt." + type.name));
                    typeHeadingWritten = true;
                }
                out.format("%s%n", Main.getMsg("main.help.opt." + type.name + "." + name));
            }
        }
        out.format("%n%s%n%n", Main.getMsg("main.help.postopt"));
    }

    static void printCompatHelp(PrintWriter out) {
        out.format("%s%n", Main.getMsg("usage.compat"));
    }

    static void printUsageTryHelp(PrintWriter out) {
        out.format("%s%n", Main.getMsg("main.usage.summary.try"));
    }

    static void printVersion(PrintWriter out) {
        out.format("%s %s%n", "jar", System.getProperty("java.version"));
    }
}
