/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * The MIT License
 *
 * Copyright (c) 2004-2015 Paul R. Holser, Jr.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package jdk.internal.joptsimple.internal;

import java.util.Iterator;

import static java.lang.System.*;
import static java.util.Arrays.*;

/**
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public final class Strings {
    public static final String EMPTY = "";
    public static final String LINE_SEPARATOR = getProperty( "line.separator" );

    private Strings() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gives a string consisting of the given character repeated the given number of times.
     *
     * @param ch the character to repeat
     * @param count how many times to repeat the character
     * @return the resultant string
     */
    public static String repeat( char ch, int count ) {
        StringBuilder buffer = new StringBuilder();

        for ( int i = 0; i < count; ++i )
            buffer.append( ch );

        return buffer.toString();
    }

    /**
     * Tells whether the given string is either {@code} or consists solely of whitespace characters.
     *
     * @param target string to check
     * @return {@code true} if the target string is null or empty
     */
    public static boolean isNullOrEmpty( String target ) {
        return target == null || target.isEmpty();
    }


    /**
     * Gives a string consisting of a given string prepended and appended with surrounding characters.
     *
     * @param target a string
     * @param begin character to prepend
     * @param end character to append
     * @return the surrounded string
     */
    public static String surround( String target, char begin, char end ) {
        return begin + target + end;
    }

    /**
     * Gives a string consisting of the elements of a given array of strings, each separated by a given separator
     * string.
     *
     * @param pieces the strings to join
     * @param separator the separator
     * @return the joined string
     */
    public static String join( String[] pieces, String separator ) {
        return join( asList( pieces ), separator );
    }

    /**
     * Gives a string consisting of the string representations of the elements of a given array of objects,
     * each separated by a given separator string.
     *
     * @param pieces the elements whose string representations are to be joined
     * @param separator the separator
     * @return the joined string
     */
    public static String join( Iterable<String> pieces, String separator ) {
        StringBuilder buffer = new StringBuilder();

        for ( Iterator<String> iter = pieces.iterator(); iter.hasNext(); ) {
            buffer.append( iter.next() );

            if ( iter.hasNext() )
                buffer.append( separator );
        }

        return buffer.toString();
    }
}
