/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.util.Set;

/**
 * Utility class for checking module, package, and class names.
 */

public final class Checks {

    private Checks() { }

    /**
     * Checks a name to ensure that it's a legal module name.
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         module name
     */
    public static String requireModuleName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Null module name");
        int next;
        int off = 0;
        while ((next = name.indexOf('.', off)) != -1) {
            String id = name.substring(off, next);
            if (!isJavaIdentifier(id)) {
                throw new IllegalArgumentException(name + ": Invalid module name"
                        + ": '" + id + "' is not a Java identifier");
            }
            off = next+1;
        }
        String last = name.substring(off);
        if (!isJavaIdentifier(last)) {
            throw new IllegalArgumentException(name + ": Invalid module name"
                    + ": '" + last + "' is not a Java identifier");
        }
        return name;
    }

    /**
     * Checks a name to ensure that it's a legal package name.
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         package name
     */
    public static String requirePackageName(String name) {
        return requireTypeName("package name", name);
    }

    /**
     * Returns {@code true} if the given name is a legal package name.
     */
    public static boolean isPackageName(String name) {
        return isTypeName(name);
    }

    /**
     * Checks a name to ensure that it's a legal qualified class name
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         qualified class name
     */
    public static String requireServiceTypeName(String name) {
        return requireQualifiedClassName("service type name", name);
    }

    /**
     * Checks a name to ensure that it's a legal qualified class name.
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         qualified class name
     */
    public static String requireServiceProviderName(String name) {
        return requireQualifiedClassName("service provider name", name);
    }

    /**
     * Checks a name to ensure that it's a legal qualified class name in
     * a named package.
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         qualified class name in a named package
     */
    public static String requireQualifiedClassName(String what, String name) {
        requireTypeName(what, name);
        if (name.indexOf('.') == -1)
            throw new IllegalArgumentException(name + ": is not a qualified name of"
                                               + " a Java class in a named package");
        return name;
    }

    /**
     * Returns {@code true} if the given name is a legal class name.
     */
    public static boolean isClassName(String name) {
        return isTypeName(name);
    }

    /**
     * Returns {@code true} if the given name is a legal type name.
     */
    private static boolean isTypeName(String name) {
        int next;
        int off = 0;
        while ((next = name.indexOf('.', off)) != -1) {
            String id = name.substring(off, next);
            if (!isJavaIdentifier(id))
                return false;
            off = next+1;
        }
        String last = name.substring(off);
        return isJavaIdentifier(last);
    }

    /**
     * Checks if the given name is a legal type name.
     *
     * @throws IllegalArgumentException if name is null or not a legal
     *         type name
     */
    private static String requireTypeName(String what, String name) {
        if (name == null)
            throw new IllegalArgumentException("Null " + what);
        int next;
        int off = 0;
        while ((next = name.indexOf('.', off)) != -1) {
            String id = name.substring(off, next);
            if (!isJavaIdentifier(id)) {
                throw new IllegalArgumentException(name + ": Invalid " + what
                        + ": '" + id + "' is not a Java identifier");
            }
            off = next + 1;
        }
        String last = name.substring(off);
        if (!isJavaIdentifier(last)) {
            throw new IllegalArgumentException(name + ": Invalid " + what
                    + ": '" + last + "' is not a Java identifier");
        }
        return name;
    }

    /**
     * Returns true if the given string is a legal Java identifier,
     * otherwise false.
     */
    private static boolean isJavaIdentifier(String str) {
        if (str.isEmpty() || RESERVED.contains(str))
            return false;

        int first = Character.codePointAt(str, 0);
        if (!Character.isJavaIdentifierStart(first))
            return false;

        int i = Character.charCount(first);
        while (i < str.length()) {
            int cp = Character.codePointAt(str, i);
            if (!Character.isJavaIdentifierPart(cp))
                return false;
            i += Character.charCount(cp);
        }

        return true;
    }

    // keywords, boolean and null literals, not allowed in identifiers
    private static final Set<String> RESERVED = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "true",
            "false",
            "null",
            "_"
    );
}
