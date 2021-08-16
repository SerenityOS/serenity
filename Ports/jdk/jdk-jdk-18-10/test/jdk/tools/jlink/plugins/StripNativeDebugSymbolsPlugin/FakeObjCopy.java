/*
 * Copyright (c) 2019, Red Hat, Inc.
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

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fake objcopy used by StripNativeDebugSymbolsTest. It prints the
 * passed in arguments to a log and creates a fake debug info file
 * for --only-keep-debug invocation. Note that the first argument is
 * the path to the log file. This argument will be omitted when
 * logged.
 *
 * Callers need to ensure the log file is properly truncated.
 *
 */
public class FakeObjCopy {

    private static final String OBJCOPY_ONLY_KEEP_DEBUG_OPT = "--only-keep-debug";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new AssertionError("At least one argument expected");
        }
        String[] objCopyArgs = new String[args.length - 1];
        System.arraycopy(args, 1, objCopyArgs, 0, objCopyArgs.length);
        String logFile = args[0];
        System.out.println("DEBUG: Fake objcopy called. Log file is: " + logFile);
        // Log options
        String line = Arrays.asList(objCopyArgs).stream().collect(Collectors.joining(" "));
        Files.write(Paths.get(logFile),
                    List.<String>of(line),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        // Handle --only-keep-debug option as plugin attempts to read
        // debug info file after this utility being called.
        if (objCopyArgs.length == 3 && OBJCOPY_ONLY_KEEP_DEBUG_OPT.equals(objCopyArgs[0])) {
            handleOnlyKeepDebug(objCopyArgs[2]);
        }
    }

    private static void handleOnlyKeepDebug(String dbgFile) throws Exception {
        try (PrintWriter pw = new PrintWriter(new File(dbgFile))) {
            pw.println("Fake objcopy debug info file");
        }
        System.out.println("DEBUG: wrote fake debug file " + dbgFile);
    }

}
