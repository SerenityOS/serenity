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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public final class Classes {
    private static final Map<Class<?>, Class<?>> WRAPPERS = new HashMap<>( 13 );

    static {
        WRAPPERS.put( boolean.class, Boolean.class );
        WRAPPERS.put( byte.class, Byte.class );
        WRAPPERS.put( char.class, Character.class );
        WRAPPERS.put( double.class, Double.class );
        WRAPPERS.put( float.class, Float.class );
        WRAPPERS.put( int.class, Integer.class );
        WRAPPERS.put( long.class, Long.class );
        WRAPPERS.put( short.class, Short.class );
        WRAPPERS.put( void.class, Void.class );
    }

    private Classes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gives the "short version" of the given class name.  Somewhat naive to inner classes.
     *
     * @param className class name to chew on
     * @return the short name of the class
     */
    public static String shortNameOf( String className ) {
        return className.substring( className.lastIndexOf( '.' ) + 1 );
    }

    /**
     * Gives the primitive wrapper class for the given class. If the given class is not
     * {@linkplain Class#isPrimitive() primitive}, returns the class itself.
     *
     * @param <T> generic class type
     * @param clazz the class to check
     * @return primitive wrapper type if {@code clazz} is primitive, otherwise {@code clazz}
     */
    @SuppressWarnings( "unchecked" )
    public static <T> Class<T> wrapperOf( Class<T> clazz ) {
        return clazz.isPrimitive() ? (Class<T>) WRAPPERS.get( clazz ) : clazz;
    }
}
