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
import java.util.Collections;
import java.util.List;

/**
 * Allows callers to specify whether a given option accepts arguments (required or optional).
 *
 * <p>Instances are returned from {@link OptionParser#accepts(String)} to allow the formation of parser directives as
 * sentences in a "fluent interface" language.  For example:</p>
 *
 * <pre><code>
 *   OptionParser parser = new OptionParser();
 *   parser.accepts( "c" ).<strong>withRequiredArg()</strong>.ofType( Integer.class );
 * </code></pre>
 *
 * <p>If no methods are invoked on an instance of this class, then that instance's option will accept no argument.</p>
 *
 * <p>Note that you should not use the fluent interface clauses in a way that would defeat the typing of option
 * arguments:</p>
 *
 * <pre><code>
 *   OptionParser parser = new OptionParser();
 *   ArgumentAcceptingOptionSpec&lt;String&gt; optionC =
 *       parser.accepts( "c" ).withRequiredArg();
 *   <strong>optionC.ofType( Integer.class );  // DON'T THROW AWAY THE TYPE!</strong>
 *
 *   String value = parser.parse( "-c", "2" ).valueOf( optionC );  // ClassCastException
 * </code></pre>
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public class OptionSpecBuilder extends NoArgumentOptionSpec {
    private final OptionParser parser;

    OptionSpecBuilder( OptionParser parser, List<String> options, String description ) {
        super( options, description );

        this.parser = parser;
        attachToParser();
    }

    private void attachToParser() {
        parser.recognize( this );
    }

    /**
     * Informs an option parser that this builder's option requires an argument.
     *
     * @return a specification for the option
     */
    public ArgumentAcceptingOptionSpec<String> withRequiredArg() {
        ArgumentAcceptingOptionSpec<String> newSpec = new RequiredArgumentOptionSpec<>( options(), description() );
        parser.recognize( newSpec );

        return newSpec;
    }

    /**
     * Informs an option parser that this builder's option accepts an optional argument.
     *
     * @return a specification for the option
     */
    public ArgumentAcceptingOptionSpec<String> withOptionalArg() {
        ArgumentAcceptingOptionSpec<String> newSpec =
            new OptionalArgumentOptionSpec<>( options(), description() );
        parser.recognize( newSpec );

        return newSpec;
    }

    /**
     * <p>Informs an option parser that this builder's option is required if the given option is present on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #requiredUnless(String, String...)
     * requiredUnless} to avoid conflicts.</p>
     *
     * @param dependent an option whose presence on a command line makes this builder's option required
     * @param otherDependents other options whose presence on a command line makes this builder's option required
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws OptionException if any of the dependent options haven't been configured in the parser yet
     */
    public OptionSpecBuilder requiredIf( String dependent, String... otherDependents ) {
        List<String> dependents = validatedDependents( dependent, otherDependents );
        for ( String each : dependents )
            parser.requiredIf( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is required if the given option is present on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #requiredUnless(OptionSpec, OptionSpec[])
     * requiredUnless} to avoid conflicts.</p>
     *
     * <p>This method recognizes only instances of options returned from the fluent interface methods.</p>
     *
     * @param dependent the option whose presence on a command line makes this builder's option required
     * @param otherDependents other options whose presence on a command line makes this builder's option required
     * @return self, so that the caller can add clauses to the fluent interface sentence
     */
    public OptionSpecBuilder requiredIf( OptionSpec<?> dependent, OptionSpec<?>... otherDependents ) {
        parser.requiredIf( options(), dependent );
        for ( OptionSpec<?> each : otherDependents )
            parser.requiredIf( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is required if the given option is absent on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #requiredIf(OptionSpec, OptionSpec[])
     * requiredIf} to avoid conflicts.</p>
     *
     * @param dependent an option whose absence on a command line makes this builder's option required
     * @param otherDependents other options whose absence on a command line makes this builder's option required
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws OptionException if any of the dependent options haven't been configured in the parser yet
     */
    public OptionSpecBuilder requiredUnless( String dependent, String... otherDependents ) {
        List<String> dependents = validatedDependents( dependent, otherDependents );
        for ( String each : dependents ) {
            parser.requiredUnless( options(), each );
        }
        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is required if the given option is absent on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #requiredIf(OptionSpec, OptionSpec[])
     * requiredIf} to avoid conflicts.</p>
     *
     * <p>This method recognizes only instances of options returned from the fluent interface methods.</p>
     *
     * @param dependent the option whose absence on a command line makes this builder's option required
     * @param otherDependents other options whose absence on a command line makes this builder's option required
     * @return self, so that the caller can add clauses to the fluent interface sentence
     */
    public OptionSpecBuilder requiredUnless( OptionSpec<?> dependent, OptionSpec<?>... otherDependents ) {
        parser.requiredUnless( options(), dependent );
        for ( OptionSpec<?> each : otherDependents )
            parser.requiredUnless( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is allowed if the given option is present on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #availableUnless(String, String...)
     * availableUnless} to avoid conflicts.</p>
     *
     * @param dependent an option whose presence on a command line makes this builder's option allowed
     * @param otherDependents other options whose presence on a command line makes this builder's option allowed
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws OptionException if any of the dependent options haven't been configured in the parser yet
     */
    public OptionSpecBuilder availableIf( String dependent, String... otherDependents ) {
        List<String> dependents = validatedDependents( dependent, otherDependents );
        for ( String each : dependents )
            parser.availableIf( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is allowed if the given option is present on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #availableUnless(OptionSpec, OptionSpec[])
     * requiredUnless} to avoid conflicts.</p>
     *
     * <p>This method recognizes only instances of options returned from the fluent interface methods.</p>
     *
     * @param dependent the option whose presence on a command line makes this builder's option allowed
     * @param otherDependents other options whose presence on a command line makes this builder's option allowed
     * @return self, so that the caller can add clauses to the fluent interface sentence
     */
    public OptionSpecBuilder availableIf( OptionSpec<?> dependent, OptionSpec<?>... otherDependents ) {
        parser.availableIf( options(), dependent );

        for ( OptionSpec<?> each : otherDependents )
            parser.availableIf( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is allowed if the given option is absent on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #availableIf(OptionSpec, OptionSpec[])
     * requiredIf} to avoid conflicts.</p>
     *
     * @param dependent an option whose absence on a command line makes this builder's option allowed
     * @param otherDependents other options whose absence on a command line makes this builder's option allowed
     * @return self, so that the caller can add clauses to the fluent interface sentence
     * @throws OptionException if any of the dependent options haven't been configured in the parser yet
     */
    public OptionSpecBuilder availableUnless( String dependent, String... otherDependents ) {
        List<String> dependents = validatedDependents( dependent, otherDependents );
        for ( String each : dependents )
            parser.availableUnless( options(), each );

        return this;
    }

    /**
     * <p>Informs an option parser that this builder's option is allowed if the given option is absent on the command
     * line.</p>
     *
     * <p>For a given option, you <em>should not</em> mix this with {@link #availableIf(OptionSpec, OptionSpec[])
     * requiredIf} to avoid conflicts.</p>
     *
     * <p>This method recognizes only instances of options returned from the fluent interface methods.</p>
     *
     * @param dependent the option whose absence on a command line makes this builder's option allowed
     * @param otherDependents other options whose absence on a command line makes this builder's option allowed
     * @return self, so that the caller can add clauses to the fluent interface sentence
     */
    public OptionSpecBuilder availableUnless( OptionSpec<?> dependent, OptionSpec<?>... otherDependents ) {
        parser.availableUnless( options(), dependent );
        for ( OptionSpec<?> each : otherDependents )
            parser.availableUnless(options(), each);

        return this;
    }

    private List<String> validatedDependents( String dependent, String... otherDependents ) {
        List<String> dependents = new ArrayList<>();
        dependents.add( dependent );
        Collections.addAll( dependents, otherDependents );

        for ( String each : dependents ) {
            if ( !parser.isRecognized( each ) )
                throw new UnconfiguredOptionException( each );
        }

        return dependents;
    }
}
