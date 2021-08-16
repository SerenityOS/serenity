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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.sun.jdi.InternalException;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;

abstract class ConnectorImpl implements Connector {

    Map<String, Argument> defaultArguments = new LinkedHashMap<>();

    // Used by BooleanArgument
    static String trueString = null;
    static String falseString;

    public Map<String,Argument> defaultArguments() {
        Map<String,Argument> defaults = new LinkedHashMap<>();
        Collection<Argument> values = defaultArguments.values();

        Iterator<Argument> iter = values.iterator();
        while (iter.hasNext()) {
            ArgumentImpl argument = (ArgumentImpl)iter.next();
            defaults.put(argument.name(), (Argument)argument.clone());
        }
        return defaults;
    }

    void addStringArgument(String name, String label, String description,
                           String defaultValue, boolean mustSpecify) {
        defaultArguments.put(name,
                             new StringArgumentImpl(name, label,
                                                    description,
                                                    defaultValue,
                                                    mustSpecify));
    }

    void addBooleanArgument(String name, String label, String description,
                            boolean defaultValue, boolean mustSpecify) {
        defaultArguments.put(name,
                             new BooleanArgumentImpl(name, label,
                                                     description,
                                                     defaultValue,
                                                     mustSpecify));
    }

    void addIntegerArgument(String name, String label, String description,
                            String defaultValue, boolean mustSpecify,
                            int min, int max) {
        defaultArguments.put(name,
                             new IntegerArgumentImpl(name, label,
                                                     description,
                                                     defaultValue,
                                                     mustSpecify,
                                                     min, max));
    }

    void addSelectedArgument(String name, String label, String description,
                             String defaultValue, boolean mustSpecify,
                             List<String> list) {
        defaultArguments.put(name,
                             new SelectedArgumentImpl(name, label,
                                                      description,
                                                      defaultValue,
                                                      mustSpecify, list));
    }

    ArgumentImpl argument(String name, Map<String, ? extends Argument> arguments)
                throws IllegalConnectorArgumentsException {

        ArgumentImpl argument = (ArgumentImpl)arguments.get(name);
        if (argument == null) {
            throw new IllegalConnectorArgumentsException(
                         "Argument missing", name);
        }
        String value = argument.value();
        if (value == null || value.length() == 0) {
            if (argument.mustSpecify()) {
            throw new IllegalConnectorArgumentsException(
                         "Argument unspecified", name);
            }
        } else if(!argument.isValid(value)) {
            throw new IllegalConnectorArgumentsException(
                         "Argument invalid", name);
        }

        return argument;
    }


    private ResourceBundle messages = null;

    String getString(String key) {
        if (messages == null) {
            messages = ResourceBundle.getBundle("com.sun.tools.jdi.resources.jdi");
        }
        return messages.getString(key);
    }

    public String toString() {
        String string = name() + " (defaults: ";
        Iterator<Argument> iter = defaultArguments().values().iterator();
        boolean first = true;
        while (iter.hasNext()) {
            ArgumentImpl argument = (ArgumentImpl)iter.next();
            if (!first) {
                string += ", ";
            }
            string += argument.toString();
            first = false;
        }
        string += ")";
        return string;
    }

    @SuppressWarnings("serial") // JDK implementation class
    abstract class ArgumentImpl implements Connector.Argument, Cloneable {
        private String name;
        private String label;
        private String description;
        private String value;
        private boolean mustSpecify;

        ArgumentImpl(String name, String label, String description,
                     String value, boolean mustSpecify) {
            this.name = name;
            this.label = label;
            this.description = description;
            this.value = value;
            this.mustSpecify = mustSpecify;
        }

        public abstract boolean isValid(String value);

        public String name() {
            return name;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public String value() {
            return value;
        }

        public void setValue(String value) {
            if (value == null) {
                throw new NullPointerException("Can't set null value");
            }
            this.value = value;
        }

        public boolean mustSpecify() {
            return mustSpecify;
        }

        public boolean equals(Object obj) {
            if ((obj != null) && (obj instanceof Connector.Argument)) {
                Connector.Argument other = (Connector.Argument)obj;
                return (name().equals(other.name())) &&
                       (description().equals(other.description())) &&
                       (mustSpecify() == other.mustSpecify()) &&
                       (value().equals(other.value()));
            } else {
                return false;
            }
        }

        public int hashCode() {
            return description().hashCode();
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                // Object should always support clone
                throw new InternalException();
            }
        }

        public String toString() {
            return name() + "=" + value();
        }
    }

    class BooleanArgumentImpl extends ConnectorImpl.ArgumentImpl
                              implements Connector.BooleanArgument {
        private static final long serialVersionUID = 1624542968639361316L;
        BooleanArgumentImpl(String name, String label, String description,
                            boolean value,
                            boolean mustSpecify) {
            super(name, label, description, null, mustSpecify);
            if(trueString == null) {
                trueString = getString("true");
                falseString = getString("false");
            }
            setValue(value);
        }

        /**
         * Sets the value of the argument.
         */
        public void setValue(boolean value) {
            setValue(stringValueOf(value));
        }

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value is a string
         * representation of a boolean value.
         * @see #stringValueOf(boolean)
         */
        public boolean isValid(String value) {
            return value.equals(trueString) || value.equals(falseString);
        }

        /**
         * Return the string representation of the <code>value</code>
         * parameter.
         * Does not set or examine the value or the argument.
         * @return the localized String representation of the
         * boolean value.
         */
        public String stringValueOf(boolean value) {
            return value? trueString : falseString;
        }

        /**
         * Return the value of the argument as a boolean.  Since
         * the argument may not have been set or may have an invalid
         * value {@link #isValid(String)} should be called on
         * {@link #value()} to check its validity.  If it is invalid
         * the boolean returned by this method is undefined.
         * @return the value of the argument as a boolean.
         */
        public boolean booleanValue() {
            return value().equals(trueString);
        }
    }

    class IntegerArgumentImpl extends ConnectorImpl.ArgumentImpl
                              implements Connector.IntegerArgument {
        private static final long serialVersionUID = 763286081923797770L;
        private final int min;
        private final int max;

        IntegerArgumentImpl(String name, String label, String description,
                            String value,
                            boolean mustSpecify, int min, int max) {
            super(name, label, description, value, mustSpecify);
            this.min = min;
            this.max = max;
        }

        /**
         * Sets the value of the argument.
         * The value should be checked with {@link #isValid(int)}
         * before setting it; invalid values will throw an exception
         * when the connection is established - for example,
         * on {@link LaunchingConnector#launch}
         */
        public void setValue(int value) {
            setValue(stringValueOf(value));
        }

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value represents an int that is
         * <code>{@link #min()} &lt;= value &lt;= {@link #max()}</code>
         */
        public boolean isValid(String value) {
            if (value == null) {
                return false;
            }
            try {
                return isValid(Integer.decode(value).intValue());
            } catch (NumberFormatException exc) {
                return false;
            }
        }

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if
         * <code>{@link #min()} &lt;= value  &lt;= {@link #max()}</code>
         */
        public boolean isValid(int value) {
            return min <= value && value <= max;
        }

        /**
         * Return the string representation of the <code>value</code>
         * parameter.
         * Does not set or examine the value or the argument.
         * @return the String representation of the
         * int value.
         */
        public String stringValueOf(int value) {
            // *** Should this be internationalized????
            // *** Even Brian Beck was unsure if an Arabic programmer
            // *** would expect port numbers in Arabic numerals,
            // *** so punt for now.
            return ""+value;
        }

        /**
         * Return the value of the argument as a int.  Since
         * the argument may not have been set or may have an invalid
         * value {@link #isValid(String)} should be called on
         * {@link #value()} to check its validity.  If it is invalid
         * the int returned by this method is undefined.
         * @return the value of the argument as a int.
         */
        public int intValue() {
            if (value() == null) {
                return 0;
            }
            try {
                return Integer.decode(value()).intValue();
            } catch(NumberFormatException exc) {
                return 0;
            }
        }

        /**
         * The upper bound for the value.
         * @return the maximum allowed value for this argument.
         */
        public int max() {
            return max;
        }

        /**
         * The lower bound for the value.
         * @return the minimum allowed value for this argument.
         */
        public int min() {
            return min;
        }
    }

    class StringArgumentImpl extends ConnectorImpl.ArgumentImpl
                             implements Connector.StringArgument {
        private static final long serialVersionUID = 7500484902692107464L;
        StringArgumentImpl(String name, String label, String description,
                           String value, boolean mustSpecify) {
            super(name, label, description, value, mustSpecify);
        }

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> always
         */
        public boolean isValid(String value) {
            return true;
        }
    }

    class SelectedArgumentImpl extends ConnectorImpl.ArgumentImpl
                              implements Connector.SelectedArgument {
        private static final long serialVersionUID = -5689584530908382517L;
        private final List<String> choices;

        SelectedArgumentImpl(String name, String label, String description,
                             String value,
                             boolean mustSpecify, List<String> choices) {
            super(name, label, description, value, mustSpecify);
            this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
        }

        /**
         * Return the possible values for the argument
         * @return {@link List} of {@link String}
         */
        public List<String> choices() {
            return choices;
        }

        /**
         * Performs basic sanity check of argument.
         * @return <code>true</code> if value is one of {@link #choices()}.
         */
        public boolean isValid(String value) {
            return choices.contains(value);
        }
    }
}
