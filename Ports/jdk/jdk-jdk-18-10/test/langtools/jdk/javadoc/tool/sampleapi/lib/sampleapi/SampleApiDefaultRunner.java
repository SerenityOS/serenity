/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package sampleapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SampleApiDefaultRunner {

    public static final String MSG_NO_OUTDIR =
        "SampleApi: no outdir set";
    public static final String MSG_NO_RESDIR =
        "SampleApi: no resourcedir set";
    public static final String MSG_DUP_OUTDIR =
        "SampleApi: duplicated outdir detected: ";
    public static final String MSG_DUP_RESDIR =
        "SampleApi: duplicated resourcedir detected: ";
    public static final String MSG_USE_FIRST =
        "           will use first occurance: ";
    public static final String MSG_INVAL_OUTDIR =
        "SampleApi: outdir is not valid: ";
    public static final String MSG_INVAL_RESDIR =
        "SampleApi: outdir is not valid: ";
    public static final String MSG_CANNOT_GEN =
        "SampleApi: cannot generate output: ";
    public static final String MSG_WRONG_OPTION =
        "SampleApi: incorrect option: ";
    public static final String MSG_USE_HELP =
        "           use -? for help";
    public static final String[] MSG_HELP = {
        "SampleApi options:",
        "    -?|-h|--help                                 - print help",
        "    -r=<dir>|--resdir=<dir>|--resourcedir=<dir>  - set <dir> to find xml resources",
        "    -o=<dir>|--outdir=<dir>                      - set <dir> to generate output"
    };

    public static void main(String... args) throws Exception {
        System.exit(execute(args));
    }

    public static int execute(String... args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return 1;
        }

        String outDirName = "";
        String resDirName = "";

        boolean isOutDirSet = false;
        boolean isResDirSet = false;
        boolean isHelpPrinted = false;
        for (String arg : args) {
            Option option = new Option(arg);
            switch (option.getOptionName()) {
                case "-?":
                case "-h":
                case "--help":
                    if (!isHelpPrinted) {
                        printHelp();
                        isHelpPrinted = true;
                    }
                    break;
                case "-o":
                case "--outdir":
                    if (!isOutDirSet) {
                        outDirName = option.getOptionValue();
                        isOutDirSet = true;
                    } else {
                        System.err.println(MSG_DUP_OUTDIR + option.getOptionValue());
                        System.err.println(MSG_USE_FIRST + outDirName);
                    }
                    break;
                case "-r":
                case "--resdir":
                case "--resourcedir":
                    if (!isResDirSet) {
                        resDirName = option.getOptionValue();
                        isResDirSet = true;
                    } else {
                        System.err.println(MSG_DUP_RESDIR + option.getOptionValue());
                        System.err.println(MSG_USE_FIRST + resDirName);
                    }
                    break;
                default:
                    System.err.println(MSG_WRONG_OPTION + arg);
                    System.err.println(MSG_USE_HELP);
                    break;
            }

        }

        if (!isOutDirSet) {
            System.err.println(MSG_NO_OUTDIR);
            return 1;
        }

        if (outDirName.length() == 0) {
            System.err.println(MSG_INVAL_OUTDIR + outDirName);
            return 1;
        }

        if (!isResDirSet) {
            System.err.println(MSG_NO_RESDIR);
            return 1;
        }

        if (resDirName.length() == 0) {
            System.err.println(MSG_INVAL_RESDIR + resDirName);
            return 1;
        }

        Path resDir = Paths.get(resDirName);
        Path outDir = Paths.get(outDirName);
        Files.createDirectories(outDir);
        SampleApi apiGen = new SampleApi();

        apiGen.load(resDir).generate(outDir);

        return 0;
    }

    private static void printHelp() {
        for (String line : MSG_HELP)
            System.out.println(line);
    }

    private static class Option {

        private final String optionName;
        private final String optionValue;

        public Option(String arg) {
            int delimPos = arg.indexOf('=');

            if (delimPos == -1) {
                optionName = arg;
                optionValue = "";
            } else {
                optionName = arg.substring(0, delimPos);
                optionValue = arg.substring(delimPos + 1, arg.length());
            }
        }

        public String getOptionName() {
            return optionName;
        }

        public String getOptionValue() {
            return optionValue;
        }
    }
}
