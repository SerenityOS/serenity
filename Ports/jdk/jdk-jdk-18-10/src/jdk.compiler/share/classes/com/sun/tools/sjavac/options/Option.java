/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.tools.sjavac.CopyFile;
import com.sun.tools.sjavac.Transformer;


/**
 * Sjavac options can be classified as:
 *
 *  (1) relevant only for sjavac, such as --server
 *  (2) relevant for sjavac and javac, such as -d, or
 *  (3) relevant only for javac, such as -g.
 *
 * This enum represents all options from (1) and (2). Note that instances of
 * this enum only entail static information about the option. For storage of
 * option values, refer to com.sun.tools.sjavac.options.Options.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public enum Option {

    SRC("-src", "Location of source files to be compiled") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            List<Path> paths = getFileListArg(iter, helper);
            if (paths != null)
                helper.sourceRoots(paths);
        }
    },
    SOURCE_PATH("--source-path", "Specify search path for sources.") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            List<Path> paths = getFileListArg(iter, helper);
            if (paths != null)
                helper.sourcepath(paths);
        }
    },
    SOURCEPATH("-sourcepath", "An alias for -sourcepath") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            SOURCE_PATH.processMatching(iter, helper);
        }
    },
    MODULE_PATH("--module-path", "Specify search path for modules.") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            List<Path> paths = getFileListArg(iter, helper);
            if (paths != null)
                helper.modulepath(paths);
        }
    },
    P("-p", "An alias for --module-path") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            MODULE_PATH.processMatching(iter, helper);
        }
    },
    CLASS_PATH("--class-path", "Specify search path for classes.") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            List<Path> paths = getFileListArg(iter, helper);
            if (paths != null)
                helper.classpath(paths);
        }
    },
    CLASSPATH("-classpath", "An alias for -classpath.") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            CLASS_PATH.processMatching(iter, helper);
        }
    },
    CP("-cp", "An alias for -classpath") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            CLASS_PATH.processMatching(iter, helper);
        }
    },
    X("-x", "Exclude files matching the given pattern") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            String pattern = getFilePatternArg(iter, helper);
            if (pattern != null)
                helper.exclude(pattern);
        }
    },
    I("-i", "Include only files matching the given pattern") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            String pattern = getFilePatternArg(iter, helper);
            if (pattern != null)
                helper.include(pattern);
        }
    },
    TR("-tr", "Translate resources") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {

            if (!iter.hasNext()) {
                helper.reportError(arg + " must be followed by a translation rule");
                return;
            }

            String trArg = iter.next();

            // Validate argument syntax. Examples:
            //   .prop=com.sun.tools.javac.smart.CompileProperties
            //   .idl=com.sun.corba.CompileIdl
            //   .g3=antlr.CompileGrammar,debug=true
            String ident = "[a-zA-Z_][a-zA-Z0-9_]*";
            Pattern p = Pattern.compile("(?<suffix>\\." + ident + ")=" +
                                        "(?<class>" + ident + "(\\." + ident + ")*)" +
                                        "(?<extra>,.*)?");
            // Check syntax
            Matcher m = p.matcher(trArg);
            if (!m.matches()) {
                helper.reportError("The string \"" + trArg + "\" is not a " +
                                   "valid translate pattern");
                return;
            }

            // Extract relevant parts
            String suffix = m.group("suffix");
            String classname = m.group("class");
            String extra = m.group("extra");

            // Valid suffix?
            if (suffix.matches("\\.(class|java)")) {
                helper.reportError("You cannot have a translator for " +
                                   suffix + " files!");
                return;
            }

            // Construct transformer
            try {
                Class<?> trCls = Class.forName(classname);
                Transformer transformer =
                    (Transformer) trCls.getConstructor().newInstance();
                transformer.setExtra(extra);
                helper.addTransformer(suffix, transformer);
            } catch (Exception e) {
                helper.reportError("Cannot use " + classname +
                                   " as a translator: " + e.getMessage());
            }
        }
    },
    COPY("-copy", "Copy resources") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            if (!iter.hasNext()) {
                helper.reportError(arg + " must be followed by a resource type");
                return;
            }

            String copyArg = iter.next();

            // Validate argument syntax. Examples: .gif, .html
            if (!copyArg.matches("\\.[a-zA-Z_][a-zA-Z0-9_]*")) {
                helper.reportError("The string \"" + copyArg + "\" is not a " +
                                   "valid resource type.");
                return;
            }

            helper.addTransformer(copyArg, new CopyFile());
        }
    },
    J("-j", "Number of cores") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            if (!iter.hasNext() || !iter.peek().matches("\\d+")) {
                helper.reportError(arg + " must be followed by an integer");
                return;
            }
            helper.numCores(Integer.parseInt(iter.next()));
        }
    },
    SERVER("--server:", "Specify server configuration file of running server") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.serverConf(iter.current().substring(arg.length()));
        }
    },
    STARTSERVER("--startserver:", "Start server and use the given configuration file") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.startServerConf(iter.current().substring(arg.length()));
        }
    },
    IMPLICIT("-implicit:", "Specify how to treat implicitly referenced source code") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.implicit(iter.current().substring(arg.length()));
        }
    },
    LOG("--log=", "Specify logging level") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.logLevel(iter.current().substring(arg.length()));
        }
    },
    VERBOSE("-verbose", "Set verbosity level to \"info\"") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.logLevel("info");
        }
    },
    PERMIT_ARTIFACT("--permit-artifact=", "Allow this artifact in destination directory") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            String a = iter.current().substring(arg.length());
            helper.permitArtifact(Paths.get(a).toFile().getAbsolutePath());
        }
    },
    PERMIT_UNIDENTIFIED_ARTIFACTS("--permit-unidentified-artifacts", "Allow unidentified artifacts in destination directory") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.permitUnidentifiedArtifacts();
        }
    },
    PERMIT_SOURCES_WITHOUT_PACKAGE("--permit-sources-without-package", "Permit sources in the default package") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            helper.permitDefaultPackage();
        }
    },
    COMPARE_FOUND_SOURCES("--compare-found-sources", "Compare found sources with given sources") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            Path referenceSourceList = getFileArg(iter, helper, true, false);
            if (referenceSourceList != null)
                helper.compareFoundSources(referenceSourceList);
        }
    },
    D("-d", "Output destination directory") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            Path dir = getFileArg(iter, helper, false, true);
            if (dir != null)
                helper.destDir(dir);
        }
    },
    S("-s", "Directory for generated sources") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            Path dir = getFileArg(iter, helper, false, true);
            if (dir != null)
                helper.generatedSourcesDir(dir);
        }
    },
    H("-h", "Directory for header files") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            Path dir = getFileArg(iter, helper, false, true);
            if (dir != null)
                helper.headerDir(dir);
        }
    },
    STATE_DIR("--state-dir=", "Directory used to store sjavac state and log files.") {
        @Override
        protected void processMatching(ArgumentIterator iter, OptionHelper helper) {
            String p = iter.current().substring(arg.length());
            helper.stateDir(Paths.get(p));
        }
    };


    public final String arg;

    final String description;

    private Option(String arg, String description) {
        this.arg = arg;
        this.description = description;
    }

    /** Retrieve and verify syntax of file list argument. */
    List<Path> getFileListArg(ArgumentIterator iter, OptionHelper helper) {
        if (!iter.hasNext()) {
            helper.reportError(arg + " must be followed by a list of files " +
                              "separated by " + File.pathSeparator);
            return null;
        }
        List<Path> result = new ArrayList<>();
        for (String pathStr : iter.next().split(File.pathSeparator))
            result.add(Paths.get(pathStr));
        return result;
    }

    /** Retrieve and verify syntax of file argument. */
    Path getFileArg(ArgumentIterator iter, OptionHelper helper, boolean fileAcceptable, boolean dirAcceptable) {

        if (!iter.hasNext()) {
            String errmsg = arg + " must be followed by ";
            if (fileAcceptable && dirAcceptable) errmsg += "a file or directory.";
            else if (fileAcceptable) errmsg += "a file.";
            else if (dirAcceptable)  errmsg += "a directory.";
            else throw new IllegalArgumentException("File or directory must be acceptable.");
            helper.reportError(errmsg);
            return null;
        }

        return Paths.get(iter.next());
    }

    /** Retrieve the next file or package argument. */
    String getFilePatternArg(ArgumentIterator iter, OptionHelper helper) {

        if (!iter.hasNext()) {
            helper.reportError(arg + " must be followed by a glob pattern.");
            return null;
        }

        return iter.next();
    }

    // Future cleanup: Change the "=" syntax to ":" syntax to be consistent and
    // to follow the javac-option style.

    public boolean hasOption() {
        return arg.endsWith(":") || arg.endsWith("=");
    }


    /**
     * Process current argument of argIter.
     *
     * It's final, since the option customization is typically done in
     * processMatching.
     *
     * @param argIter Iterator to read current and succeeding arguments from.
     * @param helper The helper to report back to.
     * @return true iff the argument was processed by this option.
     */
    public final boolean processCurrent(ArgumentIterator argIter,
                                        OptionHelper helper) {
        String fullArg = argIter.current(); // "-tr" or "-log=level"
        if (hasOption() ? fullArg.startsWith(arg) : fullArg.equals(arg)) {
            processMatching(argIter, helper);
            return true;
        }
        // Did not match
        return false;
    }

    /** Called by process if the current argument matches this option. */
    protected abstract void processMatching(ArgumentIterator argIter,
                                            OptionHelper helper);
}
