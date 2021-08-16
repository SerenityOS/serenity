/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.commentchecker;

import java.io.*;
import java.util.StringTokenizer;

/**
 * CommentChecker is a utility which verifies that there aren't
 * "/*" or "/**" tokens inside any comment blocks in one or more
 * Java source files.  Although it is legal to have beginning
 * comment delimiters inside of a comment block (JLS 3.7), there
 * have been errors where a dropped end-comment delimiter in a
 * method'd doc-comment effectively "erased" that method.  We're
 * therefore restricting beginning comment delimiters inside of
 * JDK source (at least the Swing team is for their portion).
 *
 * To scan a few files, run CommentChecker as follows:
 *
 *     java CommentChecker file1.java file2.java ...
 *
 * There are too many Java files in the JDK base for most shells
 * to support listing in a single command, so CommentChecker also
 * supports cpio and tar-style filename passing, where "-"
 * indicates that the list of files is read from stdin:
 *
 *     find . -name SCCS -prune -o -name '*.java' -print | \
 *        java CommentChecker -
 *
 * @author Thomas Ball
 */
public class CommentChecker {

    static int errors = 0;

    // Turn on this flag and recompile to dump this tool's state changes.
    final static boolean verbose = false;

    static void check(String fileName) {
        BufferedReader in = null;
        boolean inComment = false;
        boolean inLineComment = false;
        boolean inQuote = false;
        boolean inEscape = false;
        int lastChar = -1;
        int lineNumber = 1;

        try {
            in = new BufferedReader(new FileReader(fileName));
            while (true) {
                int ch = in.read();
                if (ch == -1) {
                    if (inQuote || inComment) {
                        error(fileName + ": premature EOF.");
                    }
                    return;
                }

                if (verbose) {
                    System.out.print((char)ch);
                }

                switch (ch) {
                  case '\n':
                    if (inQuote && !inComment) {
                        error(fileName + ":" + lineNumber +
                              " dangling quote.");
                        inQuote = false;
                    }
                    if (inLineComment) {
                        inLineComment = false;
                        if (verbose) {
                            System.out.println("\ninLineComment=false");
                        }
                    }
                    lineNumber++;
                    break;

                  case '\"':
                    if (!inComment && !inLineComment && !inEscape &&
                        !(!inQuote && lastChar == '\'')) {
                        inQuote = !inQuote;
                        if (verbose) {
                            System.out.println("\ninQuote=" + inQuote);
                        }
                    }
                    break;

                  case '/':
                    if (!inQuote && lastChar == '*') {
                        inComment = false;
                        if (verbose) {
                            System.out.println("\ninComment=false");
                        }
                    }
                    if (!inQuote && lastChar == '/') {
                        inLineComment = true;
                        if (verbose) {
                            System.out.println("\ninLineComment=true");
                        }
                    }
                    break;

                  case '*':
                    if (!inQuote && lastChar == '/') {
                        if (inComment) {
                            error(fileName + ":" + lineNumber +
                                  " nested comment.");
                        }
                        inComment = true;
                        if (verbose) {
                            System.out.println("\ninComment=true");
                        }
                    }
                    break;
                }

                lastChar = ch;

                // Watch for escaped characters, such as '\"'.
                if (ch == '\\' && !inEscape) {
                    inEscape = true;
                    if (verbose) {
                        System.out.println("\ninEscape set");
                    }
                } else {
                    inEscape = false;
                }
            }
        } catch (FileNotFoundException fnfe) {
            error(fileName + " not found.");
        } catch (IOException ioe) {
            error(fileName + ": " + ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    error(fileName + ": " + e);
                }
            }
        }
    }

    static void error(String description) {
        System.err.println(description);
        errors++;
    }

    static void exit() {
        if (errors != 1) {
            System.out.println("There were " + errors + " errors.");
        } else {
            System.out.println("There was 1 error.");
        }
        System.exit(errors);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: java CommentChecker [-] file.java ...");
            System.exit(1);
        }

        if (args.length == 1 && args[0].equals("-")) {
            /* read filenames in one per line from stdin, ala cpio.
             * This is good for checking the whole JDK in one pass:
             *
             *    cpio . -name SCCS -prune -o -name '*.java' -print | \
             *        java CommentChecker -
             */
            try {
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String fileName = br.readLine();
                    if (fileName == null) {
                        break;
                    }
                    check(fileName);
                }
                br.close();
            } catch (Exception e) {
                error("error reading System.in: " + e);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                check(args[i]);
            }
        }

        exit();
    }
}
