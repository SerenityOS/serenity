/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import java.io.*;
import java.util.LinkedList;

/**
 * Utility class intended to read file line by line and skip comments.
 */
public class CommentedFileReader {

    /**
     * Type of comments that should be removed from file.
     */
    public static enum CommentStyle {
        /**
         * Comments started with <i>#</i>.
         */
        BASH,
        /**
         * Comments started with <i>//</i>.
         */
        JAVA
    }

    /**
     * Get lines from specified file and filter out comments.
     * Only comments in BASH style will be filtered out.
     *
     * @param path to file that should be readed
     * @return filtered lines from file
     */
    public static String[] readFile(String path) throws IOException {
        return readFile(new File(path), CommentStyle.BASH);
    }

    /**
     * Get lines from specified file and filter out comments.
     * Only comments in BASH style will be filtered out.
     *
     * @param file that should be readed
     * @return filtered lines from file
     */
    public static String[] readFile(File file) throws IOException {
        return readFile(file, CommentStyle.BASH);
    }

    /**
     * Get lines from specified file without comments.
     *
     * @param path to file that should be readed
     * @param commentStyle describes what strings will be treated as comments
     * @return filtered lines from file
     */
    public static String[] readFile(String path, CommentStyle commentStyle) throws IOException {
        return readFile(new File(path), commentStyle);
    }

    /**
     * Get lines from specified file without comments.
     *
     * @param file that should be readed
     * @param commentStyle describes what strings will be treated as comments
     * @return filtered lines from file
     */
    public static String[] readFile(File file, CommentStyle commentStyle) throws IOException {
        LinkedList<String> entries = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String commentBeginning;

        switch (commentStyle) {
        case BASH:
            commentBeginning = "#";
            break;
        case JAVA:
            commentBeginning = "//";
            break;
        default:
            throw new IllegalArgumentException("Unknown comment style");
        }

        while (true) {
            String entry = reader.readLine();
            if (entry == null) {
                break;
            }

            entry = entry.replaceAll(commentBeginning + ".*", "").trim();

            if (entry.length() > 0) {
                entries.add(entry);
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

}
