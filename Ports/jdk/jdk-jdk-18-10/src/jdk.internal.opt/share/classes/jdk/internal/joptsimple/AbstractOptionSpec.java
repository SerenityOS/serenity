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

package jdk.internal.joptsimple;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.*;

import jdk.internal.joptsimple.internal.Reflection;
import jdk.internal.joptsimple.internal.ReflectionException;

import static jdk.internal.joptsimple.internal.Strings.*;

/**
 * @param <V> represents the type of the arguments this option accepts
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public abstract class AbstractOptionSpec<V> implements OptionSpec<V>, OptionDescriptor {
    private final List<String> options = new ArrayList<>();
    private final String description;
    private boolean forHelp;

    AbstractOptionSpec( String option ) {
        this( singletonList( option ), EMPTY );
    }

    AbstractOptionSpec( List<String> options, String description ) {
        arrangeOptions( options );

        this.description = description;
    }

    public final List<String> options() {
        return unmodifiableList( options );
    }

    public final List<V> values( OptionSet detectedOptions ) {
        return detectedOptions.valuesOf( this );
    }

    public final V value( OptionSet detectedOptions ) {
        return detectedOptions.valueOf( this );
    }

    public String description() {
        return description;
    }

    public final AbstractOptionSpec<V> forHelp() {
        forHelp = true;
        return this;
    }

    public final boolean isForHelp() {
        return forHelp;
    }

    public boolean representsNonOptions() {
        return false;
    }

    protected abstract V convert( String argument );

    protected V convertWith( ValueConverter<V> converter, String argument ) {
        try {
            return Reflection.convertWith( converter, argument );
        } catch ( ReflectionException | ValueConversionException ex ) {
            throw new OptionArgumentConversionException( this, argument, ex );
        }
    }

    protected String argumentTypeIndicatorFrom( ValueConverter<V> converter ) {
        if ( converter == null )
            return null;

        String pattern = converter.valuePattern();
        return pattern == null ? converter.valueType().getName() : pattern;
    }

    abstract void handleOption( OptionParser parser, ArgumentList arguments, OptionSet detectedOptions,
        String detectedArgument );

    private void arrangeOptions( List<String> unarranged ) {
        if ( unarranged.size() == 1 ) {
            options.addAll( unarranged );
            return;
        }

        List<String> shortOptions = new ArrayList<>();
        List<String> longOptions = new ArrayList<>();

        for ( String each : unarranged ) {
            if ( each.length() == 1 )
                shortOptions.add( each );
            else
                longOptions.add( each );
        }

        sort( shortOptions );
        sort( longOptions );

        options.addAll( shortOptions );
        options.addAll( longOptions );
    }

    @Override
    public boolean equals( Object that ) {
        if ( !( that instanceof AbstractOptionSpec<?> ) )
            return false;

        AbstractOptionSpec<?> other = (AbstractOptionSpec<?>) that;
        return options.equals( other.options );
    }

    @Override
    public int hashCode() {
        return options.hashCode();
    }

    @Override
    public String toString() {
        return options.toString();
    }
}
