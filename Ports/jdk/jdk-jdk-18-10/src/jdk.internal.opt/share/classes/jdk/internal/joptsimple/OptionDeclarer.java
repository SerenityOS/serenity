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

package jdk.internal.joptsimple;

import java.util.List;

/**
 * Trains the option parser. This interface aids integration that disposes declaration of options but not actual
 * command-line parsing.
 *
 * Typical use is for another class to implement {@code OptionDeclarer} as a facade, forwarding calls to an
 * {@code OptionParser} instance.
 *
 * Note that although this is an interface, the returned values of calls are concrete jopt-simple classes.
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 * @see OptionParser
 * @since 4.6
 */
public interface OptionDeclarer {
    /**
     * Tells the parser to recognize the given option.
     *
     * <p>This method returns an instance of {@link OptionSpecBuilder} to allow the formation of parser directives
     * as sentences in a fluent interface language. For example:</p>
     *
     * <pre><code>
     *   OptionDeclarer parser = new OptionParser();
     *   parser.<strong>accepts( "c" )</strong>.withRequiredArg().ofType( Integer.class );
     * </code></pre>
     *
     * <p>If no methods are invoked on the returned {@link OptionSpecBuilder}, then the parser treats the option as
     * accepting no argument.</p>
     *
     * @param option the option to recognize
     * @return an object that can be used to flesh out more detail about the option
     * @throws OptionException if the option contains illegal characters
     * @throws NullPointerException if the option is {@code null}
     */
    OptionSpecBuilder accepts( String option );

    /**
     * Tells the parser to recognize the given option.
     *
     * @see #accepts(String)
     * @param option the option to recognize
     * @param description a string that describes the purpose of the option. This is used when generating help
     * information about the parser.
     * @return an object that can be used to flesh out more detail about the option
     * @throws OptionException if the option contains illegal characters
     * @throws NullPointerException if the option is {@code null}
     */
    OptionSpecBuilder accepts( String option, String description );

    /**
     * Tells the parser to recognize the given options, and treat them as synonymous.
     *
     * @see #accepts(String)
     * @param options the options to recognize and treat as synonymous
     * @return an object that can be used to flesh out more detail about the options
     * @throws OptionException if any of the options contain illegal characters
     * @throws NullPointerException if the option list or any of its elements are {@code null}
     */
    OptionSpecBuilder acceptsAll( List<String> options );

    /**
     * Tells the parser to recognize the given options, and treat them as synonymous.
     *
     * @see #acceptsAll(List)
     * @param options the options to recognize and treat as synonymous
     * @param description a string that describes the purpose of the option.  This is used when generating help
     * information about the parser.
     * @return an object that can be used to flesh out more detail about the options
     * @throws OptionException if any of the options contain illegal characters
     * @throws NullPointerException if the option list or any of its elements are {@code null}
     * @throws IllegalArgumentException if the option list is empty
     */
    OptionSpecBuilder acceptsAll( List<String> options, String description );

    /**
     * Gives an object that represents an access point for non-option arguments on a command line.
     *
     * @return an object that can be used to flesh out more detail about the non-option arguments
     */
    NonOptionArgumentSpec<String> nonOptions();

    /**
     * Gives an object that represents an access point for non-option arguments on a command line.
     *
     * @see #nonOptions()
     * @param description a string that describes the purpose of the non-option arguments. This is used when generating
     * help information about the parser.
     * @return an object that can be used to flesh out more detail about the non-option arguments
     */
    NonOptionArgumentSpec<String> nonOptions( String description );

    /**
     * Tells the parser whether or not to behave "POSIX-ly correct"-ly.
     *
     * @param setting {@code true} if the parser should behave "POSIX-ly correct"-ly
     */
    void posixlyCorrect( boolean setting );

    /**
     * <p>Tells the parser to treat unrecognized options as non-option arguments.</p>
     *
     * <p>If not called, then the parser raises an {@link OptionException} when it encounters an unrecognized
     * option.</p>
     */
    void allowsUnrecognizedOptions();

    /**
     * Tells the parser either to recognize or ignore {@code -W}-style long options.
     *
     * @param recognize {@code true} if the parser is to recognize the special style of long options
     */
    void recognizeAlternativeLongOptions( boolean recognize );
}
