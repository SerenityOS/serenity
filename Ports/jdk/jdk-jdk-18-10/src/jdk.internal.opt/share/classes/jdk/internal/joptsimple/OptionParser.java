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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

import jdk.internal.joptsimple.internal.AbbreviationMap;
import jdk.internal.joptsimple.internal.SimpleOptionNameMap;
import jdk.internal.joptsimple.internal.OptionNameMap;
import jdk.internal.joptsimple.util.KeyValuePair;

import static java.util.Collections.*;
import static jdk.internal.joptsimple.OptionException.*;
import static jdk.internal.joptsimple.OptionParserState.*;
import static jdk.internal.joptsimple.ParserRules.*;

/**
 * <p>Parses command line arguments, using a syntax that attempts to take from the best of POSIX {@code getopt()}
 * and GNU {@code getopt_long()}.</p>
 *
 * <p>This parser supports short options and long options.</p>
 *
 * <ul>
 *   <li><dfn>Short options</dfn> begin with a single hyphen ("{@code -}") followed by a single letter or digit,
 *   or question mark ("{@code ?}"), or dot ("{@code .}"), or underscore ("{@code _}").</li>
 *
 *   <li>Short options can accept single arguments. The argument can be made required or optional. The option's
 *   argument can occur:
 *     <ul>
 *       <li>in the slot after the option, as in {@code -d /tmp}</li>
 *       <li>right up against the option, as in {@code -d/tmp}</li>
 *       <li>right up against the option separated by an equals sign ({@code "="}), as in {@code -d=/tmp}</li>
 *     </ul>
 *   To specify <em>n</em> arguments for an option, specify the option <em>n</em> times, once for each argument,
 *   as in {@code -d /tmp -d /var -d /opt}; or, when using the
 *   {@linkplain ArgumentAcceptingOptionSpec#withValuesSeparatedBy(char) "separated values"} clause of the "fluent
 *   interface" (see below), give multiple values separated by a given character as a single argument to the
 *   option.</li>
 *
 *   <li>Short options can be clustered, so that {@code -abc} is treated as {@code -a -b -c}. If a short option
 *   in the cluster can accept an argument, the remaining characters are interpreted as the argument for that
 *   option.</li>
 *
 *   <li>An argument consisting only of two hyphens ({@code "--"}) signals that the remaining arguments are to be
 *   treated as non-options.</li>
 *
 *   <li>An argument consisting only of a single hyphen is considered a non-option argument (though it can be an
 *   argument of an option). Many Unix programs treat single hyphens as stand-ins for the standard input or standard
 *   output streams.</li>
 *
 *   <li><dfn>Long options</dfn> begin with two hyphens ({@code "--"}), followed by multiple letters, digits,
 *   hyphens, question marks, or dots. A hyphen cannot be the first character of a long option specification when
 *   configuring the parser.</li>
 *
 *   <li>You can abbreviate long options, so long as the abbreviation is unique. Suppress this behavior if
 *   you wish using {@linkplain OptionParser#OptionParser(boolean) this constructor}.</li>
 *
 *   <li>Long options can accept single arguments.  The argument can be made required or optional.  The option's
 *   argument can occur:
 *     <ul>
 *       <li>in the slot after the option, as in {@code --directory /tmp}</li>
 *       <li>right up against the option separated by an equals sign ({@code "="}), as in
 *       {@code --directory=/tmp}
 *     </ul>
 *   Specify multiple arguments for a long option in the same manner as for short options (see above).</li>
 *
 *   <li>You can use a single hyphen ({@code "-"}) instead of a double hyphen ({@code "--"}) for a long
 *   option.</li>
 *
 *   <li>The option {@code -W} is reserved.  If you tell the parser to {@linkplain
 *   #recognizeAlternativeLongOptions(boolean) recognize alternative long options}, then it will treat, for example,
 *   {@code -W foo=bar} as the long option {@code foo} with argument {@code bar}, as though you had written
 *   {@code --foo=bar}.</li>
 *
 *   <li>You can specify {@code -W} as a valid short option, or use it as an abbreviation for a long option, but
 *   {@linkplain #recognizeAlternativeLongOptions(boolean) recognizing alternative long options} will always supersede
 *   this behavior.</li>
 *
 *   <li>You can specify a given short or long option multiple times on a single command line. The parser collects
 *   any arguments specified for those options as a list.</li>
 *
 *   <li>If the parser detects an option whose argument is optional, and the next argument "looks like" an option,
 *   that argument is not treated as the argument to the option, but as a potentially valid option. If, on the other
 *   hand, the optional argument is typed as a derivative of {@link Number}, then that argument is treated as the
 *   negative number argument of the option, even if the parser recognizes the corresponding numeric option.
 *   For example:
 *   <pre><code>
 *     OptionParser parser = new OptionParser();
 *     parser.accepts( "a" ).withOptionalArg().ofType( Integer.class );
 *     parser.accepts( "2" );
 *     OptionSet options = parser.parse( "-a", "-2" );
 *   </code></pre>
 *   In this case, the option set contains {@code "a"} with argument {@code -2}, not both {@code "a"} and
 *   {@code "2"}. Swapping the elements in the <em>args</em> array gives the latter.</li>
 * </ul>
 *
 * <p>There are two ways to tell the parser what options to recognize:</p>
 *
 * <ol>
 *   <li>A "fluent interface"-style API for specifying options, available since version 2. Sentences in this fluent
 *   interface language begin with a call to {@link #accepts(String) accepts} or {@link #acceptsAll(List)
 *   acceptsAll} methods; calls on the ensuing chain of objects describe whether the options can take an argument,
 *   whether the argument is required or optional, to what type arguments of the options should be converted if any,
 *   etc. Since version 3, these calls return an instance of {@link OptionSpec}, which can subsequently be used to
 *   retrieve the arguments of the associated option in a type-safe manner.</li>
 *
 *   <li>Since version 1, a more concise way of specifying short options has been to use the special {@linkplain
 *   #OptionParser(String) constructor}. Arguments of options specified in this manner will be of type {@link String}.
 *   Here are the rules for the format of the specification strings this constructor accepts:
 *
 *     <ul>
 *       <li>Any letter or digit is treated as an option character.</li>
 *
 *       <li>An option character can be immediately followed by an asterisk ({@code *)} to indicate that
 *       the option is a "help" option.</li>
 *
 *       <li>If an option character (with possible trailing asterisk) is followed by a single colon ({@code ":"}),
 *       then the option requires an argument.</li>
 *
 *       <li>If an option character (with possible trailing asterisk) is followed by two colons ({@code "::"}),
 *       then the option accepts an optional argument.</li>
 *
 *       <li>Otherwise, the option character accepts no argument.</li>
 *
 *       <li>If the option specification string begins with a plus sign ({@code "+" }), the parser will behave
 *       "POSIX-ly correct".</li>
 *
 *       <li>If the option specification string contains the sequence {@code "W;"} (capital W followed by a
 *       semicolon), the parser will recognize the alternative form of long options.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>Each of the options in a list of options given to {@link #acceptsAll(List) acceptsAll} is treated as a
 * synonym of the others.  For example:</p>
 *   <pre>
 *     <code>
 *     OptionParser parser = new OptionParser();
 *     parser.acceptsAll( asList( "w", "interactive", "confirmation" ) );
 *     OptionSet options = parser.parse( "-w" );
 *     </code>
 *   </pre>
 * <p>In this case, <code>options.{@link OptionSet#has(String) has}</code> would answer {@code true} when given arguments
 * {@code "w"}, {@code "interactive"}, and {@code "confirmation"}. The {@link OptionSet} would give the same
 * responses to these arguments for its other methods as well.</p>
 *
 * <p>By default, as with GNU {@code getopt()}, the parser allows intermixing of options and non-options. If, however,
 * the parser has been created to be "POSIX-ly correct", then the first argument that does not look lexically like an
 * option, and is not a required argument of a preceding option, signals the end of options. You can still bind
 * optional arguments to their options using the abutting (for short options) or {@code =} syntax.</p>
 *
 * <p>Unlike GNU {@code getopt()}, this parser does not honor the environment variable {@code POSIXLY_CORRECT}.
 * "POSIX-ly correct" parsers are configured by either:</p>
 *
 * <ol>
 *   <li>using the method {@link #posixlyCorrect(boolean)}, or</li>
 *
 *   <li>using the {@linkplain #OptionParser(String) constructor} with an argument whose first character is a plus sign
 *   ({@code "+"})</li>
 * </ol>
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 * @see <a href="http://www.gnu.org/software/libc/manual">The GNU C Library</a>
 */
public class OptionParser implements OptionDeclarer {
    private final OptionNameMap<AbstractOptionSpec<?>> recognizedOptions;
    private final ArrayList<AbstractOptionSpec<?>> trainingOrder;
    private final Map<List<String>, Set<OptionSpec<?>>> requiredIf;
    private final Map<List<String>, Set<OptionSpec<?>>> requiredUnless;
    private final Map<List<String>, Set<OptionSpec<?>>> availableIf;
    private final Map<List<String>, Set<OptionSpec<?>>> availableUnless;

    private OptionParserState state;
    private boolean posixlyCorrect;
    private boolean allowsUnrecognizedOptions;
    private HelpFormatter helpFormatter = new BuiltinHelpFormatter();

    /**
     * Creates an option parser that initially recognizes no options, and does not exhibit "POSIX-ly correct"
     * behavior.
     */
    public OptionParser() {
        this(true);
    }

    /**
     * Creates an option parser that initially recognizes no options, and does not exhibit "POSIX-ly correct"
     * behavior.
     *
     * @param allowAbbreviations whether unambiguous abbreviations of long options should be recognized
     * by the parser
     */
    public OptionParser( boolean allowAbbreviations ) {
        trainingOrder = new ArrayList<>();
        requiredIf = new HashMap<>();
        requiredUnless = new HashMap<>();
        availableIf = new HashMap<>();
        availableUnless = new HashMap<>();
        state = moreOptions( false );

        recognizedOptions = allowAbbreviations
            ? new AbbreviationMap<AbstractOptionSpec<?>>()
            : new SimpleOptionNameMap<AbstractOptionSpec<?>>();

        recognize( new NonOptionArgumentSpec<String>() );
    }

    /**
     * Creates an option parser and configures it to recognize the short options specified in the given string.
     *
     * Arguments of options specified this way will be of type {@link String}.
     *
     * @param optionSpecification an option specification
     * @throws NullPointerException if {@code optionSpecification} is {@code null}
     * @throws OptionException if the option specification contains illegal characters or otherwise cannot be
     * recognized
     */
    public OptionParser( String optionSpecification ) {
        this();

        new OptionSpecTokenizer( optionSpecification ).configure( this );
    }

    public OptionSpecBuilder accepts( String option ) {
        return acceptsAll( singletonList( option ) );
    }

    public OptionSpecBuilder accepts( String option, String description ) {
        return acceptsAll( singletonList( option ), description );
    }

    public OptionSpecBuilder acceptsAll( List<String> options ) {
        return acceptsAll( options, "" );
    }

    public OptionSpecBuilder acceptsAll( List<String> options, String description ) {
        if ( options.isEmpty() )
            throw new IllegalArgumentException( "need at least one option" );

        ensureLegalOptions( options );

        return new OptionSpecBuilder( this, options, description );
    }

    public NonOptionArgumentSpec<String> nonOptions() {
        NonOptionArgumentSpec<String> spec = new NonOptionArgumentSpec<>();

        recognize( spec );

        return spec;
    }

    public NonOptionArgumentSpec<String> nonOptions( String description ) {
        NonOptionArgumentSpec<String> spec = new NonOptionArgumentSpec<>( description );

        recognize( spec );

        return spec;
    }

    public void posixlyCorrect( boolean setting ) {
        posixlyCorrect = setting;
        state = moreOptions( setting );
    }

    boolean posixlyCorrect() {
        return posixlyCorrect;
    }

    public void allowsUnrecognizedOptions() {
        allowsUnrecognizedOptions = true;
    }

    boolean doesAllowsUnrecognizedOptions() {
        return allowsUnrecognizedOptions;
    }

    public void recognizeAlternativeLongOptions( boolean recognize ) {
        if ( recognize )
            recognize( new AlternativeLongOptionSpec() );
        else
            recognizedOptions.remove( String.valueOf( RESERVED_FOR_EXTENSIONS ) );
    }

    void recognize( AbstractOptionSpec<?> spec ) {
        recognizedOptions.putAll( spec.options(), spec );
        trainingOrder.add( spec );
    }

    /**
     * Writes information about the options this parser recognizes to the given output sink.
     *
     * The output sink is flushed, but not closed.
     *
     * @param sink the sink to write information to
     * @throws IOException if there is a problem writing to the sink
     * @throws NullPointerException if {@code sink} is {@code null}
     * @see #printHelpOn(Writer)
     */
    public void printHelpOn( OutputStream sink ) throws IOException {
        printHelpOn( new OutputStreamWriter( sink ) );
    }

    /**
     * Writes information about the options this parser recognizes to the given output sink.
     *
     * The output sink is flushed, but not closed.
     *
     * @param sink the sink to write information to
     * @throws IOException if there is a problem writing to the sink
     * @throws NullPointerException if {@code sink} is {@code null}
     * @see #printHelpOn(OutputStream)
     */
    public void printHelpOn( Writer sink ) throws IOException {
        sink.write( helpFormatter.format( _recognizedOptions() ) );
        sink.flush();
    }

    /**
     * Tells the parser to use the given formatter when asked to {@linkplain #printHelpOn(java.io.Writer) print help}.
     *
     * @param formatter the formatter to use for printing help
     * @throws NullPointerException if the formatter is {@code null}
     */
    public void formatHelpWith( HelpFormatter formatter ) {
        if ( formatter == null )
            throw new NullPointerException();

        helpFormatter = formatter;
    }

    /**
     * Retrieves all options-spec pairings which have been configured for the parser in the same order as declared
     * during training. Option flags for specs are alphabetized by {@link OptionSpec#options()}; only the order of the
     * specs is preserved.
     *
     * (Note: prior to 4.7 the order was alphabetical across all options regardless of spec.)
     *
     * @return a map containing all the configured options and their corresponding {@link OptionSpec}
     * @since 4.6
     */
    public Map<String, OptionSpec<?>> recognizedOptions() {
        return new LinkedHashMap<String, OptionSpec<?>>( _recognizedOptions() );
    }

    private Map<String, AbstractOptionSpec<?>> _recognizedOptions() {
        Map<String, AbstractOptionSpec<?>> options = new LinkedHashMap<>();
        for ( AbstractOptionSpec<?> spec : trainingOrder ) {
            for ( String option : spec.options() )
                options.put( option, spec );
        }
        return options;
    }

   /**
     * Parses the given command line arguments according to the option specifications given to the parser.
     *
     * @param arguments arguments to parse
     * @return an {@link OptionSet} describing the parsed options, their arguments, and any non-option arguments found
     * @throws OptionException if problems are detected while parsing
     * @throws NullPointerException if the argument list is {@code null}
     */
    public OptionSet parse( String... arguments ) {
        ArgumentList argumentList = new ArgumentList( arguments );
        OptionSet detected = new OptionSet( recognizedOptions.toJavaUtilMap() );
        detected.add( recognizedOptions.get( NonOptionArgumentSpec.NAME ) );

        while ( argumentList.hasMore() )
            state.handleArgument( this, argumentList, detected );

        reset();

        ensureRequiredOptions( detected );
        ensureAllowedOptions( detected );

        return detected;
    }

    /**
     * Mandates mutual exclusiveness for the options built by the specified builders.
     *
     * @param specs descriptors for options that should be mutually exclusive on a command line.
     * @throws NullPointerException if {@code specs} is {@code null}
     */
    public void mutuallyExclusive( OptionSpecBuilder... specs ) {
        for ( int i = 0; i < specs.length; i++ ) {
            for ( int j = 0; j < specs.length; j++ ) {
                if ( i != j )
                    specs[i].availableUnless( specs[j] );
            }
        }
    }

    private void ensureRequiredOptions( OptionSet options ) {
        List<AbstractOptionSpec<?>> missingRequiredOptions = missingRequiredOptions(options);
        boolean helpOptionPresent = isHelpOptionPresent( options );

        if ( !missingRequiredOptions.isEmpty() && !helpOptionPresent )
            throw new MissingRequiredOptionsException( missingRequiredOptions );
    }

    private void ensureAllowedOptions( OptionSet options ) {
        List<AbstractOptionSpec<?>> forbiddenOptions = unavailableOptions( options );
        boolean helpOptionPresent = isHelpOptionPresent( options );

        if ( !forbiddenOptions.isEmpty() && !helpOptionPresent )
            throw new UnavailableOptionException( forbiddenOptions );
    }

    private List<AbstractOptionSpec<?>> missingRequiredOptions( OptionSet options ) {
        List<AbstractOptionSpec<?>> missingRequiredOptions = new ArrayList<>();

        for ( AbstractOptionSpec<?> each : recognizedOptions.toJavaUtilMap().values() ) {
            if ( each.isRequired() && !options.has( each ) )
                missingRequiredOptions.add(each);
        }

        for ( Map.Entry<List<String>, Set<OptionSpec<?>>> each : requiredIf.entrySet() ) {
            AbstractOptionSpec<?> required = specFor( each.getKey().iterator().next() );

            if ( optionsHasAnyOf( options, each.getValue() ) && !options.has( required ) )
                missingRequiredOptions.add( required );
        }

        for ( Map.Entry<List<String>, Set<OptionSpec<?>>> each : requiredUnless.entrySet() ) {
            AbstractOptionSpec<?> required = specFor(each.getKey().iterator().next());

            if ( !optionsHasAnyOf( options, each.getValue() ) && !options.has( required ) )
                missingRequiredOptions.add( required );
        }

        return missingRequiredOptions;
    }

    private List<AbstractOptionSpec<?>> unavailableOptions(OptionSet options) {
        List<AbstractOptionSpec<?>> unavailableOptions = new ArrayList<>();

        for ( Map.Entry<List<String>, Set<OptionSpec<?>>> eachEntry : availableIf.entrySet() ) {
            AbstractOptionSpec<?> forbidden = specFor( eachEntry.getKey().iterator().next() );

            if ( !optionsHasAnyOf( options, eachEntry.getValue() ) && options.has( forbidden ) ) {
                unavailableOptions.add(forbidden);
            }
        }

        for ( Map.Entry<List<String>, Set<OptionSpec<?>>> eachEntry : availableUnless.entrySet() ) {
            AbstractOptionSpec<?> forbidden = specFor( eachEntry.getKey().iterator().next() );

            if ( optionsHasAnyOf( options, eachEntry.getValue() ) && options.has( forbidden ) ) {
                unavailableOptions.add(forbidden);
            }
        }

        return unavailableOptions;
    }

    private boolean optionsHasAnyOf( OptionSet options, Collection<OptionSpec<?>> specs ) {
        for ( OptionSpec<?> each : specs ) {
            if ( options.has( each ) )
                return true;
        }

        return false;
    }

    private boolean isHelpOptionPresent( OptionSet options ) {
        boolean helpOptionPresent = false;

        for ( AbstractOptionSpec<?> each : recognizedOptions.toJavaUtilMap().values() ) {
            if ( each.isForHelp() && options.has( each ) ) {
                helpOptionPresent = true;
                break;
            }
        }

        return helpOptionPresent;
    }

    void handleLongOptionToken( String candidate, ArgumentList arguments, OptionSet detected ) {
        KeyValuePair optionAndArgument = parseLongOptionWithArgument( candidate );

        if ( !isRecognized( optionAndArgument.key ) )
            throw unrecognizedOption( optionAndArgument.key );

        AbstractOptionSpec<?> optionSpec = specFor( optionAndArgument.key );
        optionSpec.handleOption( this, arguments, detected, optionAndArgument.value );
    }

    void handleShortOptionToken( String candidate, ArgumentList arguments, OptionSet detected ) {
        KeyValuePair optionAndArgument = parseShortOptionWithArgument( candidate );

        if ( isRecognized( optionAndArgument.key ) ) {
            specFor( optionAndArgument.key ).handleOption( this, arguments, detected, optionAndArgument.value );
        }
        else
            handleShortOptionCluster( candidate, arguments, detected );
    }

    private void handleShortOptionCluster( String candidate, ArgumentList arguments, OptionSet detected ) {
        char[] options = extractShortOptionsFrom( candidate );
        validateOptionCharacters( options );

        for ( int i = 0; i < options.length; i++ ) {
            AbstractOptionSpec<?> optionSpec = specFor( options[ i ] );

            if ( optionSpec.acceptsArguments() && options.length > i + 1 ) {
                String detectedArgument = String.valueOf( options, i + 1, options.length - 1 - i );
                optionSpec.handleOption( this, arguments, detected, detectedArgument );
                break;
            }

            optionSpec.handleOption( this, arguments, detected, null );
        }
    }

    void handleNonOptionArgument( String candidate, ArgumentList arguments, OptionSet detectedOptions ) {
        specFor( NonOptionArgumentSpec.NAME ).handleOption( this, arguments, detectedOptions, candidate );
    }

    void noMoreOptions() {
        state = OptionParserState.noMoreOptions();
    }

    boolean looksLikeAnOption( String argument ) {
        return isShortOptionToken( argument ) || isLongOptionToken( argument );
    }

    boolean isRecognized( String option ) {
        return recognizedOptions.contains( option );
    }

    void requiredIf( List<String> precedentSynonyms, String required ) {
        requiredIf( precedentSynonyms, specFor( required ) );
    }

    void requiredIf( List<String> precedentSynonyms, OptionSpec<?> required ) {
        putDependentOption( precedentSynonyms, required, requiredIf );
    }

    void requiredUnless( List<String> precedentSynonyms, String required ) {
        requiredUnless( precedentSynonyms, specFor( required ) );
    }

    void requiredUnless( List<String> precedentSynonyms, OptionSpec<?> required ) {
        putDependentOption( precedentSynonyms, required, requiredUnless );
    }

    void availableIf( List<String> precedentSynonyms, String available ) {
        availableIf( precedentSynonyms, specFor( available ) );
    }

    void availableIf( List<String> precedentSynonyms, OptionSpec<?> available) {
        putDependentOption( precedentSynonyms, available, availableIf );
    }

    void availableUnless( List<String> precedentSynonyms, String available ) {
        availableUnless( precedentSynonyms, specFor( available ) );
    }

    void availableUnless( List<String> precedentSynonyms, OptionSpec<?> available ) {
        putDependentOption( precedentSynonyms, available, availableUnless );
    }

    private void putDependentOption( List<String> precedentSynonyms, OptionSpec<?> required,
        Map<List<String>, Set<OptionSpec<?>>> target ) {

        for ( String each : precedentSynonyms ) {
            AbstractOptionSpec<?> spec = specFor( each );
            if ( spec == null )
                throw new UnconfiguredOptionException( precedentSynonyms );
        }

        Set<OptionSpec<?>> associated = target.get( precedentSynonyms );
        if ( associated == null ) {
            associated = new HashSet<>();
            target.put( precedentSynonyms, associated );
        }

        associated.add( required );
    }

    private AbstractOptionSpec<?> specFor( char option ) {
        return specFor( String.valueOf( option ) );
    }

    private AbstractOptionSpec<?> specFor( String option ) {
        return recognizedOptions.get( option );
    }

    private void reset() {
        state = moreOptions( posixlyCorrect );
    }

    private static char[] extractShortOptionsFrom( String argument ) {
        char[] options = new char[ argument.length() - 1 ];
        argument.getChars( 1, argument.length(), options, 0 );

        return options;
    }

    private void validateOptionCharacters( char[] options ) {
        for ( char each : options ) {
            String option = String.valueOf( each );

            if ( !isRecognized( option ) )
                throw unrecognizedOption( option );

            if ( specFor( option ).acceptsArguments() )
                return;
        }
    }

    private static KeyValuePair parseLongOptionWithArgument( String argument ) {
        return KeyValuePair.valueOf( argument.substring( 2 ) );
    }

    private static KeyValuePair parseShortOptionWithArgument( String argument ) {
        return KeyValuePair.valueOf( argument.substring( 1 ) );
    }
}
