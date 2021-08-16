/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Util {

    public static String toFileSystemPath(String pkgId) {
        if (pkgId == null || pkgId.length()==0) return null;
        String pn;
        if (pkgId.charAt(0) == ':') {
            // When the module is the default empty module.
            // Do not prepend the module directory, because there is none.
            // Thus :java.foo.bar translates to java/foo/bar (or \)
            pn = pkgId.substring(1).replace('.',File.separatorChar);
        } else {
            // There is a module. Thus jdk.base:java.foo.bar translates
            // into jdk.base/java/foo/bar
            int cp = pkgId.indexOf(':');
            String mn = pkgId.substring(0,cp);
            pn = mn+File.separatorChar+pkgId.substring(cp+1).replace('.',File.separatorChar);
        }
        return pn;
    }

    public static String justPackageName(String pkgName) {
        int c = pkgName.indexOf(":");
        if (c == -1)
            throw new IllegalArgumentException("Expected ':' in package name (" + pkgName + ")");
        return pkgName.substring(c+1);
    }

    public static String extractStringOption(String opName, String s) {
        return extractStringOption(opName, s, null);
    }

    private static String extractStringOptionWithDelimiter(String opName, String s, String deflt, char delimiter) {
        int p = s.indexOf(opName+"=");
        if (p == -1) return deflt;
        p+=opName.length()+1;
        int pe = s.indexOf(delimiter, p);
        if (pe == -1) pe = s.length();
        return s.substring(p, pe);
    }

    public static String extractStringOption(String opName, String s, String deflt) {
        return extractStringOptionWithDelimiter(opName, s, deflt, ',');
    }

    public static String extractStringOptionLine(String opName, String s, String deflt) {
        return extractStringOptionWithDelimiter(opName, s, deflt, '\n').strip();
    }

    public static boolean extractBooleanOption(String opName, String s, boolean deflt) {
        String str = extractStringOption(opName, s);
        return "true".equals(str) ? true
             : "false".equals(str) ? false
             : deflt;
    }

    public static int extractIntOption(String opName, String s) {
        return extractIntOption(opName, s, 0);
    }

    public static int extractIntOption(String opName, String s, int deflt) {
        int p = s.indexOf(opName+"=");
        if (p == -1) return deflt;
        p+=opName.length()+1;
        int pe = s.indexOf(',', p);
        if (pe == -1) pe = s.length();
        int v = 0;
        try {
            v = Integer.parseInt(s.substring(p, pe));
        } catch (Exception e) {}
        return v;
    }

    /**
     * Extract the package name from a fully qualified class name.
     *
     * Example: Given "pkg.subpkg.A" this method returns ":pkg.subpkg".
     * Given "C" this method returns ":".
     *
     * @returns package name of the given class name
     */
    public static String pkgNameOfClassName(String fqClassName) {
        int i = fqClassName.lastIndexOf('.');
        String pkg = i == -1 ? "" : fqClassName.substring(0, i);
        return ":" + pkg;
    }

    /**
     * Clean out unwanted sub options supplied inside a primary option.
     * For example to only had portfile remaining from:
     *    settings="--server:id=foo,portfile=bar"
     * do settings = cleanOptions("--server:",Util.set("-portfile"),settings);
     *    now settings equals "--server:portfile=bar"
     *
     * @param allowedSubOptions A set of the allowed sub options, id portfile etc.
     * @param s The option settings string.
     */
    public static String cleanSubOptions(Set<String> allowedSubOptions, String s) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            String o = st.nextToken();
            int p = o.indexOf('=');
            if (p>0) {
                String key = o.substring(0,p);
                String val = o.substring(p+1);
                if (allowedSubOptions.contains(key)) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(key+"="+val);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Convenience method to create a set with strings.
     */
    public static Set<String> set(String... ss) {
        Set<String> set = new HashSet<>();
        set.addAll(Arrays.asList(ss));
        return set;
    }

    /**
     * Normalize windows drive letter paths to upper case to enable string
     * comparison.
     *
     * @param file File name to normalize
     * @return The normalized string if file has a drive letter at the beginning,
     *         otherwise the original string.
     */
    public static String normalizeDriveLetter(String file) {
        if (file.length() > 2 && file.charAt(1) == ':') {
            return Character.toUpperCase(file.charAt(0)) + file.substring(1);
        } else if (file.length() > 3 && file.charAt(0) == '*'
                   && file.charAt(2) == ':') {
            // Handle a wildcard * at the beginning of the string.
            return file.substring(0, 1) + Character.toUpperCase(file.charAt(1))
                   + file.substring(2);
        }
        return file;
    }

    /**
     * Locate the setting for the server properties.
     */
    public static String findServerSettings(String[] args) {
        for (String s : args) {
            if (s.startsWith("--server:")) {
                return s;
            }
        }
        return null;
    }

    public static <E> Set<E> union(Set<? extends E> s1,
                                   Set<? extends E> s2) {
        Set<E> union = new HashSet<>();
        union.addAll(s1);
        union.addAll(s2);
        return union;
    }

    public static <E> Set<E> subtract(Set<? extends E> orig,
                                      Set<? extends E> toSubtract) {
        Set<E> difference = new HashSet<>(orig);
        difference.removeAll(toSubtract);
        return difference;
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // TODO: Remove when refactoring from java.io.File to java.nio.file.Path.
    public static File pathToFile(Path path) {
        return path == null ? null : path.toFile();
    }

    public static <E> Set<E> intersection(Collection<? extends E> c1,
                                          Collection<? extends E> c2) {
        Set<E> intersection = new HashSet<E>(c1);
        intersection.retainAll(c2);
        return intersection;
    }

    public static <I, T> Map<I, T> indexBy(Collection<? extends T> c,
                                           Function<? super T, ? extends I> indexFunction) {
        return c.stream().collect(Collectors.<T, I, T>toMap(indexFunction, o -> o));
    }

    public static String fileSuffix(Path file) {
        String fileNameStr = file.getFileName().toString();
        int dotIndex = fileNameStr.indexOf('.');
        return dotIndex == -1 ? "" : fileNameStr.substring(dotIndex);
    }

    public static Stream<String> getLines(String str) {
        return str.isEmpty()
                ? Stream.empty()
                : Stream.of(str.split(Pattern.quote(System.lineSeparator())));
    }
}
