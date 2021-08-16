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

import java.util.List;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static jdk.internal.joptsimple.internal.Reflection.*;

/**
 * <p>Specification of a command line's non-option arguments.</p>
 *
 * <p>Instances are returned from {@link OptionParser} methods to allow the formation of parser directives as
 * sentences in a "fluent interface" language. For example:</p>
 *
 * <pre>
 *   <code>
 *   OptionParser parser = new OptionParser();
 *   parser.nonOptions( "files to be processed" ).<strong>ofType( File.class )</strong>;
 *   </code>
 * </pre>
 *
 * <p>If no methods are invoked on an instance of this class, then that instance's option will treat the non-option
 * arguments as {@link String}s.</p>
 *
 * @param <V> represents the type of the non-option arguments
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public class NonOptionArgumentSpec<V> extends AbstractOptionSpec<V> {
    static final String NAME = "[arguments]";

    private ValueConverter<V> converter;
    private String argumentDescription = "";

    NonOptionArgumentSpec() {
        this( "" );
    }

    NonOptionArgumentSpec( String description ) {
        super( asList( NAME ), description );
    }

    /**
     * <p>Specifies a type to which the non-option arguments are to be converted.</p>
     *
     * <p>JOpt Simple accepts types that have either:</p>
     *
     * <ol>
     *   <li>a public static method called {@code valueOf} which accepts a single argument of type {@link String}
     *   and whose return type is the same as the class on which the method is declared.  The {@code java.lang}
     *   primitive wrapper classes have such methods.</li>
     *
     *   <li>a public constructor which accepts a single argument of type {@link String}.</li>
     * </ol>
     *
     * <p>This class converts arguments using those methods in that order; that is, {@code valueOf} would be invoked
     * before a one-{@link String}-arg constructor would.</p>
     *
     * <p>Invoking this method will trump any previous calls to this method or to
     * {@link #withValuesConvertedBy(ValueConverter)}.</p>
     *
     * @param <T> represents the runtime class of the desired option argument type
     * @param argumentType desired type of arguments to this spec's option
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws NullPointerException if the type is {@code null}
     * @throws IllegalArgumentException if the type does not have the standard conversion methods
     */
    @SuppressWarnings( "unchecked" )
    public <T> NonOptionArgumentSpec<T> ofType( Class<T> argumentType ) {
        converter = (ValueConverter<V>) findConverter( argumentType );
        return (NonOptionArgumentSpec<T>) this;
    }

    /**
     * <p>Specifies a converter to use to translate non-option arguments into Java objects.  This is useful
     * when converting to types that do not have the requisite factory method or constructor for
     * {@link #ofType(Class)}.</p>
     *
     * <p>Invoking this method will trump any previous calls to this method or to {@link #ofType(Class)}.
     *
     * @param <T> represents the runtime class of the desired non-option argument type
     * @param aConverter the converter to use
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws NullPointerException if the converter is {@code null}
     */
    @SuppressWarnings( "unchecked" )
    public final <T> NonOptionArgumentSpec<T> withValuesConvertedBy( ValueConverter<T> aConverter ) {
        if ( aConverter == null )
            throw new NullPointerException( "illegal null converter" );

        converter = (ValueConverter<V>) aConverter;
        return (NonOptionArgumentSpec<T>) this;
    }

    /**
     * <p>Specifies a description for the non-option arguments that this spec represents.  This description is used
     * when generating help information about the parser.</p>
     *
     * @param description describes the nature of the argument of this spec's option
     * @return self, so that the caller can add clauses to the fluent interface sentence
     */
    public NonOptionArgumentSpec<V> describedAs( String description ) {
        argumentDescription = description;
        return this;
    }

    @Override
    protected final V convert( String argument ) {
        return convertWith( converter, argument );
    }

    @Override
    void handleOption( OptionParser parser, ArgumentList arguments, OptionSet detectedOptions,
        String detectedArgument ) {

        detectedOptions.addWithArgument( this, detectedArgument );
    }

    public List<?> defaultValues() {
        return emptyList();
    }

    public boolean isRequired() {
        return false;
    }

    public boolean acceptsArguments() {
        return false;
    }

    public boolean requiresArgument() {
        return false;
    }

    public String argumentDescription() {
        return argumentDescription;
    }

    public String argumentTypeIndicator() {
        return argumentTypeIndicatorFrom( converter );
    }

    public boolean representsNonOptions() {
        return true;
    }
}
