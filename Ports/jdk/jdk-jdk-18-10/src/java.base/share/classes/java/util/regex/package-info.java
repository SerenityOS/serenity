/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Classes for matching character sequences against patterns specified
 * by regular expressions.
 *
 * <p> An instance of the {@link java.util.regex.Pattern} class
 * represents a regular expression that is specified in string form in
 * a syntax similar to that used by Perl.
 *
 * <p> Instances of the {@link java.util.regex.Matcher} class are used
 * to match character sequences against a given pattern.  Input is
 * provided to matchers via the {@link java.lang.CharSequence}
 * interface in order to support matching against characters from a
 * wide variety of input sources. </p>
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a
 * method in any class or interface in this package will cause a
 * {@link java.lang.NullPointerException NullPointerException} to be
 * thrown.
 *
 * <h2>Related Documentation</h2>
 *
 * <p> An excellent tutorial and overview of regular expressions is <a
 * href="http://www.oreilly.com/catalog/regex/"><i>Mastering Regular
 * Expressions</i>, Jeffrey E. F. Friedl, O'Reilly and Associates,
 * 1997.</a> </p>
 *
 * @since 1.4
 * @author Mike McCloskey
 * @author Mark Reinhold
 */
package java.util.regex;
