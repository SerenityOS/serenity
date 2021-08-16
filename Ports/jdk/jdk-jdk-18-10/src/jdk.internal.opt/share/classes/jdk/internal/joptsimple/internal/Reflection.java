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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.*;

import jdk.internal.joptsimple.ValueConverter;

import static jdk.internal.joptsimple.internal.Classes.*;

/**
 * Helper methods for reflection.
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public final class Reflection {
    private Reflection() {
        throw new UnsupportedOperationException();
    }

    /**
     * Finds an appropriate value converter for the given class.
     *
     * @param <V> a constraint on the class object to introspect
     * @param clazz class to introspect on
     * @return a converter method or constructor
     */
    public static <V> ValueConverter<V> findConverter( Class<V> clazz ) {
        Class<V> maybeWrapper = wrapperOf( clazz );

        ValueConverter<V> valueOf = valueOfConverter( maybeWrapper );
        if ( valueOf != null )
            return valueOf;

        ValueConverter<V> constructor = constructorConverter( maybeWrapper );
        if ( constructor != null )
            return constructor;

        throw new IllegalArgumentException( clazz + " is not a value type" );
    }

    private static <V> ValueConverter<V> valueOfConverter( Class<V> clazz ) {
        try {
            Method valueOf = clazz.getMethod( "valueOf", String.class );
            if ( meetsConverterRequirements( valueOf, clazz ) )
                return new MethodInvokingValueConverter<>( valueOf, clazz );

            return null;
        } catch ( NoSuchMethodException ignored ) {
            return null;
        }
    }

    private static <V> ValueConverter<V> constructorConverter( Class<V> clazz ) {
        try {
            return new ConstructorInvokingValueConverter<>( clazz.getConstructor( String.class ) );
        } catch ( NoSuchMethodException ignored ) {
            return null;
        }
    }

    /**
     * Invokes the given constructor with the given arguments.
     *
     * @param <T> constraint on the type of the objects yielded by the constructor
     * @param constructor constructor to invoke
     * @param args arguments to hand to the constructor
     * @return the result of invoking the constructor
     * @throws ReflectionException in lieu of the gaggle of reflection-related exceptions
     */
    public static <T> T instantiate( Constructor<T> constructor, Object... args ) {
        try {
            return constructor.newInstance( args );
        } catch ( Exception ex ) {
            throw reflectionException( ex );
        }
    }

    /**
     * Invokes the given static method with the given arguments.
     *
     * @param method method to invoke
     * @param args arguments to hand to the method
     * @return the result of invoking the method
     * @throws ReflectionException in lieu of the gaggle of reflection-related exceptions
     */
    public static Object invoke( Method method, Object... args ) {
        try {
            return method.invoke( null, args );
        } catch ( Exception ex ) {
            throw reflectionException( ex );
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <V> V convertWith( ValueConverter<V> converter, String raw ) {
        return converter == null ? (V) raw : converter.convert( raw );
    }

    private static boolean meetsConverterRequirements( Method method, Class<?> expectedReturnType ) {
        int modifiers = method.getModifiers();
        return isPublic( modifiers ) && isStatic( modifiers ) && expectedReturnType.equals( method.getReturnType() );
    }

    private static RuntimeException reflectionException( Exception ex ) {
        if ( ex instanceof IllegalArgumentException )
            return new ReflectionException( ex );
        if ( ex instanceof InvocationTargetException )
            return new ReflectionException( ex.getCause() );
        if ( ex instanceof RuntimeException )
            return (RuntimeException) ex;

        return new ReflectionException( ex );
    }
}
