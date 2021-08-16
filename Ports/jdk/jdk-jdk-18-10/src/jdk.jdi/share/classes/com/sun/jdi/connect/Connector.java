/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.connect;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A method of connection between a debugger and a target VM.
 * A connector encapsulates exactly one {@link Transport}. used
 * to establish the connection. Each connector has a set of arguments
 * which controls its operation. The arguments are stored as a
 * map, keyed by a string. Each implementation defines the string
 * argument keys it accepts.
 *
 * @see LaunchingConnector
 * @see AttachingConnector
 * @see ListeningConnector
 * @see Connector.Argument
 *
 * @author Gordon Hirsch
 * @since  1.3
 */
public interface Connector {

    /**
     * Returns a short identifier for the connector. Connector implementors
     * should follow similar naming conventions as are used with packages
     * to avoid name collisions. For example, the Sun connector
     * implementations have names prefixed with "com.sun.jdi.".
     * Not intended for exposure to end-user.
     *
     * @return the name of this connector.
     */
    String name();

    /**
     * Returns a human-readable description of this connector
     * and its purpose.
     *
     * @return the description of this connector
     */
    String description();

    /**
     * Returns the transport mechanism used by this connector to establish
     * connections with a target VM.
     *
     * @return the {@link Transport} used by this connector.
     */
    Transport transport();

    /**
     * Returns the arguments accepted by this Connector and their
     * default values. The keys of the returned map are string argument
     * names. The values are {@link Connector.Argument} containing
     * information about the argument and its default value.
     *
     * @return the map associating argument names with argument
     * information and default value.
     */
    Map<String, Connector.Argument> defaultArguments();

    /**
     * Specification for and value of a Connector argument.
     * Will always implement a subinterface of Argument:
     * {@link Connector.StringArgument}, {@link Connector.BooleanArgument},
     * {@link Connector.IntegerArgument},
     * or {@link Connector.SelectedArgument}.
     */
    public interface Argument extends Serializable {

        /**
         * Returns a short, unique identifier for the argument.
         * Not intended for exposure to end-user.
         *
         * @return the name of this argument.
         */
        String name();

        /**
         * Returns a short human-readable label for this argument.
         *
         * @return a label for this argument
         */
        String label();

        /**
         * Returns a human-readable description of this argument
         * and its purpose.
         *
         * @return the description of this argument
         */
        String description();

        /**
         * Returns the current value of the argument. Initially, the
         * default value is returned. If the value is currently unspecified,
         * null is returned.
         *
         * @return the current value of the argument.
         */
        String value();

        /**
         * Sets the value of the argument.
         * The value should be checked with {@link #isValid(String)}
         * before setting it; invalid values will throw an exception
         * when the connection is established - for example,
         * on {@link LaunchingConnector#launch}
         */
        void setValue(String value);

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if the value is valid to be
         * used in {@link #setValue(String)}
         */
        boolean isValid(String value);

        /**
         * Indicates whether the argument must be specified. If true,
         * {@link #setValue} must be used to set a non-null value before
         * using this argument in establishing a connection.
         *
         * @return <code>true</code> if the argument must be specified;
         * <code>false</code> otherwise.
         */
        boolean mustSpecify();
    }

    /**
     * Specification for and value of a Connector argument,
     * whose value is Boolean.  Boolean values are represented
     * by the localized versions of the strings "true" and "false".
     */
    public interface BooleanArgument extends Argument {

        /**
         * Sets the value of the argument.
         */
        void setValue(boolean value);

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value is a string
         * representation of a boolean value.
         * @see #stringValueOf(boolean)
         */
        boolean isValid(String value);

        /**
         * Return the string representation of the <code>value</code>
         * parameter.
         * Does not set or examine the current value of <code>this</code>
         * instance.
         * @return the localized String representation of the
         * boolean value.
         */
        String stringValueOf(boolean value);

        /**
         * Return the value of the argument as a boolean.  Since
         * the argument may not have been set or may have an invalid
         * value {@link #isValid(String)} should be called on
         * {@link #value()} to check its validity.  If it is invalid
         * the boolean returned by this method is undefined.
         * @return the value of the argument as a boolean.
         */
        boolean booleanValue();
    }

    /**
     * Specification for and value of a Connector argument,
     * whose value is an integer.  Integer values are represented
     * by their corresponding strings.
     */
    public interface IntegerArgument extends Argument {

        /**
         * Sets the value of the argument.
         * The value should be checked with {@link #isValid(int)}
         * before setting it; invalid values will throw an exception
         * when the connection is established - for example,
         * on {@link LaunchingConnector#launch}
         */
        void setValue(int value);

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value represents an int that is
         * <code>{@link #min()} &lt;= value &lt;= {@link #max()}</code>
         */
        boolean isValid(String value);

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if
         * <code>{@link #min()} &lt;= value  &lt;= {@link #max()}</code>
         */
        boolean isValid(int value);

        /**
         * Return the string representation of the <code>value</code>
         * parameter.
         * Does not set or examine the current value of <code>this</code>
         * instance.
         * @return the String representation of the
         * int value.
         */
        String stringValueOf(int value);

        /**
         * Return the value of the argument as a int.  Since
         * the argument may not have been set or may have an invalid
         * value {@link #isValid(String)} should be called on
         * {@link #value()} to check its validity.  If it is invalid
         * the int returned by this method is undefined.
         * @return the value of the argument as a int.
         */
        int intValue();

        /**
         * The upper bound for the value.
         * @return the maximum allowed value for this argument.
         */
        int max();

        /**
         * The lower bound for the value.
         * @return the minimum allowed value for this argument.
         */
        int min();
    }

    /**
     * Specification for and value of a Connector argument,
     * whose value is a String.
     */
    public interface StringArgument extends Argument {
        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> always
         */
        boolean isValid(String value);
    }

    /**
     * Specification for and value of a Connector argument,
     * whose value is a String selected from a list of choices.
     */
    public interface SelectedArgument extends Argument {
        /**
         * Return the possible values for the argument
         * @return {@link List} of {@link String}
         */
        List<String> choices();

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value is one of {@link #choices()}.
         */
        boolean isValid(String value);
    }
}
