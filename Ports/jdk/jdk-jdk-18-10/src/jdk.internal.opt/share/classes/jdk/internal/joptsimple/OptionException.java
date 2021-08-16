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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jdk.internal.joptsimple.internal.Strings;

import static java.util.Collections.*;
import static jdk.internal.joptsimple.internal.Messages.*;

/**
 * Thrown when a problem occurs during option parsing.
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public abstract class OptionException extends RuntimeException {
    private static final long serialVersionUID = -1L;

    private final List<String> options = new ArrayList<>();

    protected OptionException( List<String> options ) {
        this.options.addAll( options );
    }

    protected OptionException( Collection<? extends OptionSpec<?>> options ) {
        this.options.addAll( specsToStrings( options ) );
    }

    protected OptionException( Collection<? extends OptionSpec<?>> options, Throwable cause ) {
        super( cause );
        this.options.addAll( specsToStrings( options ) );
    }

    private List<String> specsToStrings( Collection<? extends OptionSpec<?>> options ) {
        List<String> strings = new ArrayList<>();
        for ( OptionSpec<?> each : options )
            strings.add( specToString( each ) );
        return strings;
    }

    private String specToString( OptionSpec<?> option ) {
        return Strings.join( new ArrayList<>( option.options() ), "/" );
    }

    /**
     * Gives the option being considered when the exception was created.
     *
     * @return the option being considered when the exception was created
     */
    public List<String> options() {
        return unmodifiableList( options );
    }

    protected final String singleOptionString() {
        return singleOptionString( options.get( 0 ) );
    }

    protected final String singleOptionString( String option ) {
        return option;
    }

    protected final String multipleOptionString() {
        StringBuilder buffer = new StringBuilder( "[" );

        Set<String> asSet = new LinkedHashSet<String>( options );
        for ( Iterator<String> iter = asSet.iterator(); iter.hasNext(); ) {
            buffer.append( singleOptionString(iter.next()) );
            if ( iter.hasNext() )
                buffer.append( ", " );
        }

        buffer.append( ']' );

        return buffer.toString();
    }

    static OptionException unrecognizedOption( String option ) {
        return new UnrecognizedOptionException( option );
    }

    @Override
    public final String getMessage() {
        return localizedMessage( Locale.getDefault() );
    }

    final String localizedMessage( Locale locale ) {
        return formattedMessage( locale );
    }

    private String formattedMessage( Locale locale ) {
        return message( locale, "jdk.internal.joptsimple.ExceptionMessages", getClass(), "message", messageArguments() );
    }

    abstract Object[] messageArguments();
}
