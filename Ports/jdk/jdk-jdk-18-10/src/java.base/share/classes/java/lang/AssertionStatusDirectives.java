/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * A collection of assertion status directives (such as "enable assertions
 * in package p" or "disable assertions in class c").  This class is used by
 * the JVM to communicate the assertion status directives implied by
 * the {@code java} command line flags {@code -enableassertions}
 * ({@code -ea}) and {@code -disableassertions} ({@code -da}).
 *
 * @since  1.4
 * @author Josh Bloch
 */
class AssertionStatusDirectives {
    /**
     * The classes for which assertions are to be enabled or disabled.
     * The strings in this array are fully qualified class names (for
     * example,"com.xyz.foo.Bar").
     */
    String[] classes;

    /**
     * A parallel array to {@code classes}, indicating whether each class
     * is to have assertions enabled or disabled.  A value of {@code true}
     * for {@code classEnabled[i]} indicates that the class named by
     * {@code classes[i]} should have assertions enabled; a value of
     * {@code false} indicates that it should have classes disabled.
     * This array must have the same number of elements as {@code classes}.
     *
     * <p>In the case of conflicting directives for the same class, the
     * last directive for a given class wins.  In other words, if a string
     * {@code s} appears multiple times in the {@code classes} array
     * and {@code i} is the highest integer for which
     * {@code classes[i].equals(s)}, then {@code classEnabled[i]}
     * indicates whether assertions are to be enabled in class {@code s}.
     */
    boolean[] classEnabled;

    /**
     * The package-trees for which assertions are to be enabled or disabled.
     * The strings in this array are compete or partial package names
     * (for example, "com.xyz" or "com.xyz.foo").
     */
    String[] packages;

    /**
     * A parallel array to {@code packages}, indicating whether each
     * package-tree is to have assertions enabled or disabled.  A value of
     * {@code true} for {@code packageEnabled[i]} indicates that the
     * package-tree named by {@code packages[i]} should have assertions
     * enabled; a value of {@code false} indicates that it should have
     * assertions disabled.  This array must have the same number of
     * elements as {@code packages}.
     *
     * In the case of conflicting directives for the same package-tree, the
     * last directive for a given package-tree wins.  In other words, if a
     * string {@code s} appears multiple times in the {@code packages} array
     * and {@code i} is the highest integer for which
     * {@code packages[i].equals(s)}, then {@code packageEnabled[i]}
     * indicates whether assertions are to be enabled in package-tree
     * {@code s}.
     */
    boolean[] packageEnabled;

    /**
     * Whether or not assertions in non-system classes are to be enabled
     * by default.
     */
    boolean deflt;
}
