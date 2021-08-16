/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

/**
 * @test
 * @bug 8189953
 * @summary tests the pattern generation algorithm
 * @modules java.logging/java.util.logging:open
 * @run main/othervm FileHandlerPatternGeneration
 * @author danielfuchs
 */
public class FileHandlerPatternGeneration {

    /**
     * An array of strings where the elements at even indices are the input
     * to give to FileHandler::generate(pattern, count, generation, unique),
     * and the elements at the next odd index are a partially computed expected
     * output, where %t, %h, %u, %g and file separator still need to be replaced.
     * The final expected output is obtained by passing the partially computed
     * output to FileHandlerPatternGeneration::generateExpected
     * <p>
     * The test verifies that {@code
     *    FileHandler.generate(PATTERN[i], c, g, u).toString()
     * }
     * is equal to {@code
     *    FileHandlerPatternGeneration.generateExpected(PATTERN[i],
     *                                                  PATTERN[i+1],
     *                                                  c, g, u)
     * }
     */
    static final String[] PATTERNS = {
            "C:/Workspace/hoge.log",         "C:/Workspace/hoge.log",
            "C:/Workspace%g/hoge.log",       "C:/Workspace%g/hoge.log",
            "C:/%uWorkspace/hoge.log",       "C:/%uWorkspace/hoge.log",
            "C:/%uWorkspace%g/hoge.log",     "C:/%uWorkspace%g/hoge.log",
            "C:/Workspace/%ghoge.log",       "C:/Workspace/%ghoge.log",
            "C:/Workspace/%ghoge%u.log",     "C:/Workspace/%ghoge%u.log",
            "C:/Workspace-%g/hoge.log",      "C:/Workspace-%g/hoge.log",
            "C:/Work%hspace/hoge.log",       "%h/space/hoge.log",
            "C:/Works%tpace%g/hoge.log",     "%t/pace%g/hoge.log",
            "C:/%uWork%hspace/hoge.log",     "%h/space/hoge.log",
            "C:/%uWorkspace%g/%thoge.log",   "%t/hoge.log",
            "C:/Workspace/%g%h%%hoge.log",   "%h/%%hoge.log",
            "C:/Work%h%%hspace/hoge.log",    "%h/%%hspace/hoge.log",
            "C:/Works%t%%hpace%g/hoge.log",  "%t/%%hpace%g/hoge.log",
            "C:/%uWork%h%%tspace/hoge.log",  "%h/%%tspace/hoge.log",
            "C:/%uWorkspace%g/%t%%hoge.log", "%t/%%hoge.log",
            "C:/Workspace/%g%h%%hoge.log",   "%h/%%hoge.log",
            "ahaha",                         "ahaha",
            "ahaha/ahabe",                   "ahaha/ahabe",
            "../ahaha/ahabe",                "../ahaha/ahabe",
            "/x%ty/w/hoge.log",              "%t/y/w/hoge.log",
            "/x/%ty/w/hoge.log",             "%t/y/w/hoge.log",
            "/x%t/y/w/hoge.log",             "%t/y/w/hoge.log",
            "/x/%t/y/w/hoge.log",            "%t/y/w/hoge.log",
            "%ty/w/hoge.log",                "%t/y/w/hoge.log",
            "%t/y/w/hoge.log",               "%t/y/w/hoge.log",
            "/x%hy/w/hoge.log",              "%h/y/w/hoge.log",
            "/x/%hy/w/hoge.log",             "%h/y/w/hoge.log",
            "/x%h/y/w/hoge.log",             "%h/y/w/hoge.log",
            "/x/%h/y/w/hoge.log",            "%h/y/w/hoge.log",
            "%hy/w/hoge.log",                "%h/y/w/hoge.log",
            "%h/y/w/hoge.log",               "%h/y/w/hoge.log",
            "ahaha-%u-%g",                   "ahaha-%u-%g",
            "ahaha-%g/ahabe-%u",             "ahaha-%g/ahabe-%u",
            "../ahaha-%u/ahabe",             "../ahaha-%u/ahabe",
            "/x%ty/w/hoge-%g.log",           "%t/y/w/hoge-%g.log",
            "/x/%ty/w/hoge-%u.log",          "%t/y/w/hoge-%u.log",
            "%u-%g/x%t/y/w/hoge.log",        "%t/y/w/hoge.log",
            "/x/%g%t%u/y/w/hoge.log",        "%t/%u/y/w/hoge.log",
            "%ty/w-%g/hoge.log",             "%t/y/w-%g/hoge.log",
            "%t/y/w-%u/hoge.log",            "%t/y/w-%u/hoge.log",
            "/x%hy/%u-%g-w/hoge.log",        "%h/y/%u-%g-w/hoge.log",
            "/x/%hy/w-%u-%g/hoge.log",       "%h/y/w-%u-%g/hoge.log",
            "/x%h/y/w/%u-%ghoge.log",        "%h/y/w/%u-%ghoge.log",
            "/x/%h/y/w/hoge-%u-%g.log",      "%h/y/w/hoge-%u-%g.log",
            "%hy/w/%u-%g-hoge.log",          "%h/y/w/%u-%g-hoge.log",
            "%h/y/w/hoge-%u-%g.log",         "%h/y/w/hoge-%u-%g.log",
            "/x/y/z/hoge-%u.log",            "/x/y/z/hoge-%u.log",
    };

    // the (count, generation, unique) parameters to pass to
    // FileHandler.generate(pattern, count, generation, unique)
    static final int[][] GENERATIONS = {
        {0, 0, 0},
        {0, 1, 0},
        {0, 1, 1},
        {1, 1, 0},
        {1, 1, 1},
        {1, 1, 2},
        {1, 2, 3},
        {3, 4, 0},
        {3, 4, 1},
        {3, 4, 2},
        {3, 0, 5},
        {3, 1, 5},
        {3, 2, 5},
    };

    static final Class<FileHandler> FILE_HANDLER_CLASS = FileHandler.class;
    static final Method GENERATE;
    static final String USER_HOME;
    static final String TMP;
    static {
        Method generate;
        try {
           generate = FILE_HANDLER_CLASS.getDeclaredMethod("generate",
                                                            String.class,
                                                            int.class,
                                                            int.class,
                                                            int.class);
           generate.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        GENERATE = generate;
        USER_HOME = System.getProperty("user.home");
        TMP = System.getProperty("java.io.tmpdir", USER_HOME);
    }

    public static void main(String... args) throws Throwable {

        for (int i=0; i < PATTERNS.length; i+=2) {
            String s = PATTERNS[i];
            String partial = PATTERNS[i+1];
            System.out.println("generate: " + s);
            for (int[] gen : GENERATIONS) {
                String expected = generateExpected(s, partial, gen[0], gen[1], gen[2]);
                String output = generate(s, gen[0], gen[1], gen[2]).toString();
                System.out.println("\t" + Arrays.toString(gen)+ ": " + output);
                if (!expected.equals(output)) {
                    throw new RuntimeException("test failed for \""
                            + s +"\" " + Arrays.toString(gen) + ": "
                            + "\n\tgenerated: \"" + output +"\""
                            + "\n\t expected: \"" + expected +"\"");
                }
            }
        }

    }

    // Strip the trailing separator from the string, if present
    static String stripTrailingSeparator(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() -1);
        } else if (s.endsWith(File.separator)) {
            return s.substring(0, s.length() - File.separator.length());
        } else {
            return s;
        }
    }

    /**
     * Compute the final expected output from a partially computed output found
     * at PATTERNS[i+1]
     * @param s           The pattern string, found at PATTERN[i]
     *                    (with i % 2 == 0)
     * @param partial     The partially computed output, found at PATTERN[i+1]
     * @param count       The count parameter given to FileHandler::generate
     * @param generation  The generation parameter given to FileHandler::generate
     * @param unique      The unique parameter given to FileHandler::generate
     * @return  The expected output that FileHandler.generate(s, count, gen, unique)
     *          should produce.
     */
    static String generateExpected(String s, String partial,
                                   int count, int generation, int unique)
    {
        boolean sawu = s.replace("%%", "$$$$").contains("%u");
        boolean sawg = s.replace("%%", "$$$$").contains("%g");
        String result = partial.replace("%%", "$$$$");
        String tmp = stripTrailingSeparator(TMP);
        String home = stripTrailingSeparator(USER_HOME);
        result = result.replace("%h", home);
        result = result.replace("%t", tmp);
        result = result.replace("%g", String.valueOf(generation));
        result = result.replace("%u", String.valueOf(unique));
        result = result.replace("$$$$", "%");
        result = result.replace("/", File.separator);
        if (count > 1 && !sawg) {
            result = result + "." + generation;
        }
        if (unique > 0 && !sawu) {
            result = result + "." + unique;
        }
        return result;
    }

    // Calls FileHandler.generate(s, count, generation, unique) through reflection
    static File generate(String s, int count, int generation, int unique)
            throws Throwable
    {
        try {
            return (File) GENERATE.invoke(null, s, count, generation, unique);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
