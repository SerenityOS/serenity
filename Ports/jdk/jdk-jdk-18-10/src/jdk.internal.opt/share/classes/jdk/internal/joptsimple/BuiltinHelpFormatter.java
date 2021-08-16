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

import java.util.*;

import jdk.internal.joptsimple.internal.Messages;
import jdk.internal.joptsimple.internal.Rows;
import jdk.internal.joptsimple.internal.Strings;

import static jdk.internal.joptsimple.ParserRules.*;
import static jdk.internal.joptsimple.internal.Classes.*;
import static jdk.internal.joptsimple.internal.Strings.*;

/**
 * <p>A help formatter that allows configuration of overall row width and column separator width.</p>
 *
 * <p>The formatter produces output in two sections: one for the options, and one for non-option arguments.</p>
 *
 * <p>The options section has two columns: the left column for the options, and the right column for their
 * descriptions. The formatter will allow as much space as possible for the descriptions, by minimizing the option
 * column's width, no greater than slightly less than half the overall desired width.</p>
 *
 * <p>The non-option arguments section is one column, occupying as much width as it can.</p>
 *
 * <p>Subclasses are free to override bits of this implementation as they see fit. Inspect the code
 * carefully to understand the flow of control that this implementation guarantees.</p>
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public class BuiltinHelpFormatter implements HelpFormatter {
    private final Rows nonOptionRows;
    private final Rows optionRows;

    /**
     * Makes a formatter with a pre-configured overall row width and column separator width.
     */
    BuiltinHelpFormatter() {
        this( 80, 2 );
    }

    /**
     * Makes a formatter with a given overall row width and column separator width.
     *
     * @param desiredOverallWidth how many characters wide to make the overall help display
     * @param desiredColumnSeparatorWidth how many characters wide to make the separation between option column and
     * description column
     */
    public BuiltinHelpFormatter( int desiredOverallWidth, int desiredColumnSeparatorWidth ) {
        nonOptionRows = new Rows( desiredOverallWidth * 2, 0 );
        optionRows = new Rows( desiredOverallWidth, desiredColumnSeparatorWidth );
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation:</p>
     * <ul>
     *     <li>Sorts the given descriptors by their first elements of {@link OptionDescriptor#options()}</li>
     *     <li>Passes the resulting sorted set to {@link #addRows(java.util.Collection)}</li>
     *     <li>Returns the result of {@link #formattedHelpOutput()}</li>
     * </ul>
     */
    public String format( Map<String, ? extends OptionDescriptor> options ) {
        optionRows.reset();
        nonOptionRows.reset();

        Comparator<OptionDescriptor> comparator =
            new Comparator<OptionDescriptor>() {
                public int compare( OptionDescriptor first, OptionDescriptor second ) {
                    return first.options().iterator().next().compareTo( second.options().iterator().next() );
                }
            };

        Set<OptionDescriptor> sorted = new TreeSet<>( comparator );
        sorted.addAll( options.values() );

        addRows( sorted );

        return formattedHelpOutput();
    }

    /**
     * Adds a row of option help output in the left column, with empty space in the right column.
     *
     * @param single text to put in the left column
     */
    protected void addOptionRow( String single ) {
        addOptionRow( single, "" );
    }

    /**
     * Adds a row of option help output in the left and right columns.
     *
     * @param left text to put in the left column
     * @param right text to put in the right column
     */
    protected void addOptionRow( String left, String right ) {
        optionRows.add( left, right );
    }

    /**
     * Adds a single row of non-option argument help.
     *
     * @param single single row of non-option argument help text
     */
    protected void addNonOptionRow( String single ) {
        nonOptionRows.add( single, "" );
    }

    /**
     * Resizes the columns of all the rows to be no wider than the widest element in that column.
     */
    protected void fitRowsToWidth() {
        nonOptionRows.fitToWidth();
        optionRows.fitToWidth();
    }

    /**
     * Produces non-option argument help.
     *
     * @return non-option argument help
     */
    protected String nonOptionOutput() {
        return nonOptionRows.render();
    }

    /**
     * Produces help for options and their descriptions.
     *
     * @return option help
     */
    protected String optionOutput() {
        return optionRows.render();
    }

    /**
     * <p>Produces help output for an entire set of options and non-option arguments.</p>
     *
     * <p>This implementation concatenates:</p>
     * <ul>
     *     <li>the result of {@link #nonOptionOutput()}</li>
     *     <li>if there is non-option output, a line separator</li>
     *     <li>the result of {@link #optionOutput()}</li>
     * </ul>
     *
     * @return help output for entire set of options and non-option arguments
     */
    protected String formattedHelpOutput() {
        StringBuilder formatted = new StringBuilder();
        String nonOptionDisplay = nonOptionOutput();
        if ( !Strings.isNullOrEmpty( nonOptionDisplay ) )
            formatted.append( nonOptionDisplay ).append( LINE_SEPARATOR );
        formatted.append( optionOutput() );

        return formatted.toString();
    }

    /**
     * <p>Adds rows of help output for the given options.</p>
     *
     * <p>This implementation:</p>
     * <ul>
     *     <li>Calls {@link #addNonOptionsDescription(java.util.Collection)} with the options as the argument</li>
 *         <li>If there are no options, calls {@link #addOptionRow(String)} with an argument that indicates
 *         that no options are specified.</li>
 *         <li>Otherwise, calls {@link #addHeaders(java.util.Collection)} with the options as the argument,
 *         followed by {@link #addOptions(java.util.Collection)} with the options as the argument.</li>
     *     <li>Calls {@link #fitRowsToWidth()}.</li>
     * </ul>
     *
     * @param options descriptors for the configured options of a parser
     */
    protected void addRows( Collection<? extends OptionDescriptor> options ) {
        addNonOptionsDescription( options );

        if ( options.isEmpty() )
            addOptionRow( message( "no.options.specified" ) );
        else {
            addHeaders( options );
            addOptions( options );
        }

        fitRowsToWidth();
    }

    /**
     * <p>Adds non-option arguments descriptions to the help output.</p>
     *
     * <p>This implementation:</p>
     * <ul>
     *     <li>{@linkplain #findAndRemoveNonOptionsSpec(java.util.Collection) Finds and removes the non-option
     *     arguments descriptor}</li>
     *     <li>{@linkplain #shouldShowNonOptionArgumentDisplay(OptionDescriptor) Decides whether there is
     *     anything to show for non-option arguments}</li>
     *     <li>If there is, {@linkplain #addNonOptionRow(String) adds a header row} and
     *     {@linkplain #addNonOptionRow(String) adds a}
     *     {@linkplain #createNonOptionArgumentsDisplay(OptionDescriptor) non-option arguments description} </li>
     * </ul>
     *
     * @param options descriptors for the configured options of a parser
     */
    protected void addNonOptionsDescription( Collection<? extends OptionDescriptor> options ) {
        OptionDescriptor nonOptions = findAndRemoveNonOptionsSpec( options );
        if ( shouldShowNonOptionArgumentDisplay( nonOptions ) ) {
            addNonOptionRow( message( "non.option.arguments.header" ) );
            addNonOptionRow( createNonOptionArgumentsDisplay( nonOptions ) );
        }
    }

    /**
     * <p>Decides whether or not to show a non-option arguments help.</p>
     *
     * <p>This implementation responds with {@code true} if the non-option descriptor has a non-{@code null},
     * non-empty value for any of {@link OptionDescriptor#description()},
     * {@link OptionDescriptor#argumentTypeIndicator()}, or {@link OptionDescriptor#argumentDescription()}.</p>
     *
     * @param nonOptionDescriptor non-option argument descriptor
     * @return {@code true} if non-options argument help should be shown
     */
    protected boolean shouldShowNonOptionArgumentDisplay( OptionDescriptor nonOptionDescriptor ) {
        return !Strings.isNullOrEmpty( nonOptionDescriptor.description() )
            || !Strings.isNullOrEmpty( nonOptionDescriptor.argumentTypeIndicator() )
            || !Strings.isNullOrEmpty( nonOptionDescriptor.argumentDescription() );
    }

    /**
     * <p>Creates a non-options argument help string.</p>
     *
     * <p>This implementation creates an empty string buffer and calls
     * {@link #maybeAppendOptionInfo(StringBuilder, OptionDescriptor)}
     * and {@link #maybeAppendNonOptionsDescription(StringBuilder, OptionDescriptor)}, passing them the
     * buffer and the non-option arguments descriptor.</p>
     *
     * @param nonOptionDescriptor non-option argument descriptor
     * @return help string for non-options
     */
    protected String createNonOptionArgumentsDisplay( OptionDescriptor nonOptionDescriptor ) {
        StringBuilder buffer = new StringBuilder();
        maybeAppendOptionInfo( buffer, nonOptionDescriptor );
        maybeAppendNonOptionsDescription( buffer, nonOptionDescriptor );

        return buffer.toString();
    }

    /**
     * <p>Appends help for the given non-option arguments descriptor to the given buffer.</p>
     *
     * <p>This implementation appends {@code " -- "} if the buffer has text in it and the non-option arguments
     * descriptor has a {@link OptionDescriptor#description()}; followed by the
     * {@link OptionDescriptor#description()}.</p>
     *
     * @param buffer string buffer
     * @param nonOptions non-option arguments descriptor
     */
    protected void maybeAppendNonOptionsDescription( StringBuilder buffer, OptionDescriptor nonOptions ) {
        buffer.append( buffer.length() > 0 && !Strings.isNullOrEmpty( nonOptions.description() ) ? " -- " : "" )
            .append( nonOptions.description() );
    }

    /**
     * Finds the non-option arguments descriptor in the given collection, removes it, and returns it.
     *
     * @param options descriptors for the configured options of a parser
     * @return the non-option arguments descriptor
     */
    protected OptionDescriptor findAndRemoveNonOptionsSpec( Collection<? extends OptionDescriptor> options ) {
        for ( Iterator<? extends OptionDescriptor> it = options.iterator(); it.hasNext(); ) {
            OptionDescriptor next = it.next();
            if ( next.representsNonOptions() ) {
                it.remove();
                return next;
            }
        }

        throw new AssertionError( "no non-options argument spec" );
    }

    /**
     * <p>Adds help row headers for option help columns.</p>
     *
     * <p>This implementation uses the headers {@code "Option"} and {@code "Description"}. If the options contain
     * a "required" option, the {@code "Option"} header looks like {@code "Option (* = required)}. Both headers
     * are "underlined" using {@code "-"}.</p>
     *
     * @param options descriptors for the configured options of a parser
     */
    protected void addHeaders( Collection<? extends OptionDescriptor> options ) {
        if ( hasRequiredOption( options ) ) {
            addOptionRow( message( "option.header.with.required.indicator" ), message( "description.header" ) );
            addOptionRow( message( "option.divider.with.required.indicator" ), message( "description.divider" ) );
        } else {
            addOptionRow( message( "option.header" ), message( "description.header" ) );
            addOptionRow( message( "option.divider" ), message( "description.divider" ) );
        }
    }

    /**
     * Tells whether the given option descriptors contain a "required" option.
     *
     * @param options descriptors for the configured options of a parser
     * @return {@code true} if at least one of the options is "required"
     */
    protected final boolean hasRequiredOption( Collection<? extends OptionDescriptor> options ) {
        for ( OptionDescriptor each : options ) {
            if ( each.isRequired() )
                return true;
        }

        return false;
    }

    /**
     * <p>Adds help rows for the given options.</p>
     *
     * <p>This implementation loops over the given options, and for each, calls {@link #addOptionRow(String, String)}
     * using the results of {@link #createOptionDisplay(OptionDescriptor)} and
     * {@link #createDescriptionDisplay(OptionDescriptor)}, respectively, as arguments.</p>
     *
     * @param options descriptors for the configured options of a parser
     */
    protected void addOptions( Collection<? extends OptionDescriptor> options ) {
        for ( OptionDescriptor each : options ) {
            if ( !each.representsNonOptions() )
                addOptionRow( createOptionDisplay( each ), createDescriptionDisplay( each ) );
        }
    }

    /**
     * <p>Creates a string for how the given option descriptor is to be represented in help.</p>
     *
     * <p>This implementation gives a string consisting of the concatenation of:</p>
     * <ul>
     *     <li>{@code "* "} for "required" options, otherwise {@code ""}</li>
     *     <li>For each of the {@link OptionDescriptor#options()} of the descriptor, separated by {@code ", "}:
     *         <ul>
     *             <li>{@link #optionLeader(String)} of the option</li>
     *             <li>the option</li>
     *         </ul>
     *     </li>
     *     <li>the result of {@link #maybeAppendOptionInfo(StringBuilder, OptionDescriptor)}</li>
     * </ul>
     *
     * @param descriptor a descriptor for a configured option of a parser
     * @return help string
     */
    protected String createOptionDisplay( OptionDescriptor descriptor ) {
        StringBuilder buffer = new StringBuilder( descriptor.isRequired() ? "* " : "" );

        for ( Iterator<String> i = descriptor.options().iterator(); i.hasNext(); ) {
            String option = i.next();
            buffer.append( optionLeader( option ) );
            buffer.append( option );

            if ( i.hasNext() )
                buffer.append( ", " );
        }

        maybeAppendOptionInfo( buffer, descriptor );

        return buffer.toString();
    }

    /**
     * <p>Gives a string that represents the given option's "option leader" in help.</p>
     *
     * <p>This implementation answers with {@code "--"} for options of length greater than one; otherwise answers
     * with {@code "-"}.</p>
     *
     * @param option a string option
     * @return an "option leader" string
     */
    protected String optionLeader( String option ) {
        return option.length() > 1 ? DOUBLE_HYPHEN : HYPHEN;
    }

    /**
     * <p>Appends additional info about the given option to the given buffer.</p>
     *
     * <p>This implementation:</p>
     * <ul>
     *     <li>calls {@link #extractTypeIndicator(OptionDescriptor)} for the descriptor</li>
     *     <li>calls {@link jdk.internal.joptsimple.OptionDescriptor#argumentDescription()} for the descriptor</li>
     *     <li>if either of the above is present, calls
     *     {@link #appendOptionHelp(StringBuilder, String, String, boolean)}</li>
     * </ul>
     *
     * @param buffer string buffer
     * @param descriptor a descriptor for a configured option of a parser
     */
    protected void maybeAppendOptionInfo( StringBuilder buffer, OptionDescriptor descriptor ) {
        String indicator = extractTypeIndicator( descriptor );
        String description = descriptor.argumentDescription();
        if ( descriptor.acceptsArguments()
            || !isNullOrEmpty( description )
            || descriptor.representsNonOptions() ) {

            appendOptionHelp( buffer, indicator, description, descriptor.requiresArgument() );
        }
    }

    /**
     * <p>Gives an indicator of the type of arguments of the option described by the given descriptor,
     * for use in help.</p>
     *
     * <p>This implementation asks for the {@link OptionDescriptor#argumentTypeIndicator()} of the given
     * descriptor, and if it is present and not {@code "java.lang.String"}, parses it as a fully qualified
     * class name and returns the base name of that class; otherwise returns {@code "String"}.</p>
     *
     * @param descriptor a descriptor for a configured option of a parser
     * @return type indicator text
     */
    protected String extractTypeIndicator( OptionDescriptor descriptor ) {
        String indicator = descriptor.argumentTypeIndicator();

        if ( !isNullOrEmpty( indicator ) && !String.class.getName().equals( indicator ) )
            return shortNameOf( indicator );

        return "String";
    }

    /**
     * <p>Appends info about an option's argument to the given buffer.</p>
     *
     * <p>This implementation calls {@link #appendTypeIndicator(StringBuilder, String, String, char, char)} with
     * the surrounding characters {@code '<'} and {@code '>'} for options with {@code required} arguments, and
     * with the surrounding characters {@code '['} and {@code ']'} for options with optional arguments.</p>
     *
     * @param buffer string buffer
     * @param typeIndicator type indicator
     * @param description type description
     * @param required indicator of "required"-ness of the argument of the option
     */
    protected void appendOptionHelp( StringBuilder buffer, String typeIndicator, String description,
                                     boolean required ) {
        if ( required )
            appendTypeIndicator( buffer, typeIndicator, description, '<', '>' );
        else
            appendTypeIndicator( buffer, typeIndicator, description, '[', ']' );
    }

    /**
     * <p>Appends a type indicator for an option's argument to the given buffer.</p>
     *
     * <p>This implementation appends, in order:</p>
     * <ul>
     *     <li>{@code ' '}</li>
     *     <li>{@code start}</li>
     *     <li>the type indicator, if not {@code null}</li>
     *     <li>if the description is present, then {@code ": "} plus the description if the type indicator is
     *     present; otherwise the description only</li>
     *     <li>{@code end}</li>
     * </ul>
     *
     * @param buffer string buffer
     * @param typeIndicator type indicator
     * @param description type description
     * @param start starting character
     * @param end ending character
     */
    protected void appendTypeIndicator( StringBuilder buffer, String typeIndicator, String description,
                                        char start, char end ) {
        buffer.append( ' ' ).append( start );
        if ( typeIndicator != null )
            buffer.append( typeIndicator );

        if ( !Strings.isNullOrEmpty( description ) ) {
            if ( typeIndicator != null )
                buffer.append( ": " );

            buffer.append( description );
        }

        buffer.append( end );
    }

    /**
     * <p>Gives a string representing a description of the option with the given descriptor.</p>
     *
     * <p>This implementation:</p>
     * <ul>
     *     <li>Asks for the descriptor's {@link OptionDescriptor#defaultValues()}</li>
     *     <li>If they're not present, answers the descriptor's {@link OptionDescriptor#description()}.</li>
     *     <li>If they are present, concatenates and returns:
     *         <ul>
     *             <li>the descriptor's {@link OptionDescriptor#description()}</li>
     *             <li>{@code ' '}</li>
     *             <li>{@code "default: "} plus the result of {@link #createDefaultValuesDisplay(java.util.List)},
     *             surrounded by parentheses</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param descriptor a descriptor for a configured option of a parser
     * @return display text for the option's description
     */
    protected String createDescriptionDisplay( OptionDescriptor descriptor ) {
        List<?> defaultValues = descriptor.defaultValues();
        if ( defaultValues.isEmpty() )
            return descriptor.description();

        String defaultValuesDisplay = createDefaultValuesDisplay( defaultValues );
        return ( descriptor.description()
            + ' '
            + surround( message( "default.value.header" ) + ' ' + defaultValuesDisplay, '(', ')' )
        ).trim();
    }

    /**
     * <p>Gives a display string for the default values of an option's argument.</p>
     *
     * <p>This implementation gives the {@link Object#toString()} of the first value if there is only one value,
     * otherwise gives the {@link Object#toString()} of the whole list.</p>
     *
     * @param defaultValues some default values for a given option's argument
     * @return a display string for those default values
     */
    protected String createDefaultValuesDisplay( List<?> defaultValues ) {
        return defaultValues.size() == 1 ? defaultValues.get( 0 ).toString() : defaultValues.toString();
    }

    /**
     * <p>Looks up and gives a resource bundle message.</p>
     *
     * <p>This implementation looks in the bundle {@code "jdk.internal.joptsimple.HelpFormatterMessages"} in the default
     * locale, using a key that is the concatenation of this class's fully qualified name, {@code '.'},
     * and the given key suffix, formats the corresponding value using the given arguments, and returns
     * the result.</p>
     *
     * @param keySuffix suffix to use when looking up the bundle message
     * @param args arguments to fill in the message template with
     * @return a formatted localized message
     */
    protected String message( String keySuffix, Object... args ) {
        return Messages.message(
            Locale.getDefault(),
            "jdk.internal.joptsimple.HelpFormatterMessages",
            BuiltinHelpFormatter.class,
            keySuffix,
            args );
    }
}
