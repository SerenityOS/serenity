/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.io.Serializable;

/**
 * {@code Format} is an abstract base class for formatting locale-sensitive
 * information such as dates, messages, and numbers.
 *
 * <p>
 * {@code Format} defines the programming interface for formatting
 * locale-sensitive objects into {@code String}s (the
 * {@code format} method) and for parsing {@code String}s back
 * into objects (the {@code parseObject} method).
 *
 * <p>
 * Generally, a format's {@code parseObject} method must be able to parse
 * any string formatted by its {@code format} method. However, there may
 * be exceptional cases where this is not possible. For example, a
 * {@code format} method might create two adjacent integer numbers with
 * no separator in between, and in this case the {@code parseObject} could
 * not tell which digits belong to which number.
 *
 * <h2>Subclassing</h2>
 *
 * <p>
 * The Java Platform provides three specialized subclasses of {@code Format}--
 * {@code DateFormat}, {@code MessageFormat}, and
 * {@code NumberFormat}--for formatting dates, messages, and numbers,
 * respectively.
 * <p>
 * Concrete subclasses must implement three methods:
 * <ol>
 * <li> {@code format(Object obj, StringBuffer toAppendTo, FieldPosition pos)}
 * <li> {@code formatToCharacterIterator(Object obj)}
 * <li> {@code parseObject(String source, ParsePosition pos)}
 * </ol>
 * These general methods allow polymorphic parsing and formatting of objects
 * and are used, for example, by {@code MessageFormat}.
 * Subclasses often also provide additional {@code format} methods for
 * specific input types as well as {@code parse} methods for specific
 * result types. Any {@code parse} method that does not take a
 * {@code ParsePosition} argument should throw {@code ParseException}
 * when no text in the required format is at the beginning of the input text.
 *
 * <p>
 * Most subclasses will also implement the following factory methods:
 * <ol>
 * <li>
 * {@code getInstance} for getting a useful format object appropriate
 * for the current locale
 * <li>
 * {@code getInstance(Locale)} for getting a useful format
 * object appropriate for the specified locale
 * </ol>
 * In addition, some subclasses may also implement other
 * {@code getXxxxInstance} methods for more specialized control. For
 * example, the {@code NumberFormat} class provides
 * {@code getPercentInstance} and {@code getCurrencyInstance}
 * methods for getting specialized number formatters.
 *
 * <p>
 * Subclasses of {@code Format} that allow programmers to create objects
 * for locales (with {@code getInstance(Locale)} for example)
 * must also implement the following class method:
 * <blockquote>
 * <pre>
 * public static Locale[] getAvailableLocales()
 * </pre>
 * </blockquote>
 *
 * <p>
 * And finally subclasses may define a set of constants to identify the various
 * fields in the formatted output. These constants are used to create a FieldPosition
 * object which identifies what information is contained in the field and its
 * position in the formatted result. These constants should be named
 * <code><em>item</em>_FIELD</code> where <code><em>item</em></code> identifies
 * the field. For examples of these constants, see {@code ERA_FIELD} and its
 * friends in {@link DateFormat}.
 *
 * <h3><a id="synchronization">Synchronization</a></h3>
 *
 * <p>
 * Formats are generally not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 *
 * @see          java.text.ParsePosition
 * @see          java.text.FieldPosition
 * @see          java.text.NumberFormat
 * @see          java.text.DateFormat
 * @see          java.text.MessageFormat
 * @author       Mark Davis
 * @since 1.1
 */
public abstract class Format implements Serializable, Cloneable {

    @java.io.Serial
    private static final long serialVersionUID = -299282585814624189L;

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected Format() {
    }

    /**
     * Formats an object to produce a string. This is equivalent to
     * <blockquote>
     * {@link #format(Object, StringBuffer, FieldPosition) format}<code>(obj,
     *         new StringBuffer(), new FieldPosition(0)).toString();</code>
     * </blockquote>
     *
     * @param obj    The object to format
     * @return       Formatted string.
     * @throws    IllegalArgumentException if the Format cannot format the given
     *            object
     */
    public final String format (Object obj) {
        return format(obj, new StringBuffer(), new FieldPosition(0)).toString();
    }

    /**
     * Formats an object and appends the resulting text to a given string
     * buffer.
     * If the {@code pos} argument identifies a field used by the format,
     * then its indices are set to the beginning and end of the first such
     * field encountered.
     *
     * @param obj    The object to format
     * @param toAppendTo    where the text is to be appended
     * @param pos    A {@code FieldPosition} identifying a field
     *               in the formatted text
     * @return       the string buffer passed in as {@code toAppendTo},
     *               with formatted text appended
     * @throws    NullPointerException if {@code toAppendTo} or
     *            {@code pos} is null
     * @throws    IllegalArgumentException if the Format cannot format the given
     *            object
     */
    public abstract StringBuffer format(Object obj,
                    StringBuffer toAppendTo,
                    FieldPosition pos);

    /**
     * Formats an Object producing an {@code AttributedCharacterIterator}.
     * You can use the returned {@code AttributedCharacterIterator}
     * to build the resulting String, as well as to determine information
     * about the resulting String.
     * <p>
     * Each attribute key of the AttributedCharacterIterator will be of type
     * {@code Field}. It is up to each {@code Format} implementation
     * to define what the legal values are for each attribute in the
     * {@code AttributedCharacterIterator}, but typically the attribute
     * key is also used as the attribute value.
     * <p>The default implementation creates an
     * {@code AttributedCharacterIterator} with no attributes. Subclasses
     * that support fields should override this and create an
     * {@code AttributedCharacterIterator} with meaningful attributes.
     *
     * @throws    NullPointerException if obj is null.
     * @throws    IllegalArgumentException when the Format cannot format the
     *            given object.
     * @param obj The object to format
     * @return AttributedCharacterIterator describing the formatted value.
     * @since 1.4
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return createAttributedCharacterIterator(format(obj));
    }

    /**
     * Parses text from a string to produce an object.
     * <p>
     * The method attempts to parse text starting at the index given by
     * {@code pos}.
     * If parsing succeeds, then the index of {@code pos} is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * object is returned. The updated {@code pos} can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of {@code pos} is not
     * changed, the error index of {@code pos} is set to the index of
     * the character where the error occurred, and null is returned.
     *
     * @param source A {@code String}, part of which should be parsed.
     * @param pos A {@code ParsePosition} object with index and error
     *            index information as described above.
     * @return An {@code Object} parsed from the string. In case of
     *         error, returns null.
     * @throws NullPointerException if {@code source} or {@code pos} is null.
     */
    public abstract Object parseObject (String source, ParsePosition pos);

    /**
     * Parses text from the beginning of the given string to produce an object.
     * The method may not use the entire text of the given string.
     *
     * @param source A {@code String} whose beginning should be parsed.
     * @return An {@code Object} parsed from the string.
     * @throws    ParseException if the beginning of the specified string
     *            cannot be parsed.
     * @throws NullPointerException if {@code source} is null.
     */
    public Object parseObject(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object result = parseObject(source, pos);
        if (pos.index == 0) {
            throw new ParseException("Format.parseObject(String) failed",
                pos.errorIndex);
        }
        return result;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // will never happen
            throw new InternalError(e);
        }
    }

    //
    // Convenience methods for creating AttributedCharacterIterators from
    // different parameters.
    //

    /**
     * Creates an {@code AttributedCharacterIterator} for the String
     * {@code s}.
     *
     * @param s String to create AttributedCharacterIterator from
     * @return AttributedCharacterIterator wrapping s
     */
    AttributedCharacterIterator createAttributedCharacterIterator(String s) {
        AttributedString as = new AttributedString(s);

        return as.getIterator();
    }

    /**
     * Creates an {@code AttributedCharacterIterator} containing the
     * concatenated contents of the passed in
     * {@code AttributedCharacterIterator}s.
     *
     * @param iterators AttributedCharacterIterators used to create resulting
     *                  AttributedCharacterIterators
     * @return AttributedCharacterIterator wrapping passed in
     *         AttributedCharacterIterators
     */
    AttributedCharacterIterator createAttributedCharacterIterator(
                       AttributedCharacterIterator[] iterators) {
        AttributedString as = new AttributedString(iterators);

        return as.getIterator();
    }

    /**
     * Returns an AttributedCharacterIterator with the String
     * {@code string} and additional key/value pair {@code key},
     * {@code value}.
     *
     * @param string String to create AttributedCharacterIterator from
     * @param key Key for AttributedCharacterIterator
     * @param value Value associated with key in AttributedCharacterIterator
     * @return AttributedCharacterIterator wrapping args
     */
    AttributedCharacterIterator createAttributedCharacterIterator(
                      String string, AttributedCharacterIterator.Attribute key,
                      Object value) {
        AttributedString as = new AttributedString(string);

        as.addAttribute(key, value);
        return as.getIterator();
    }

    /**
     * Creates an AttributedCharacterIterator with the contents of
     * {@code iterator} and the additional attribute {@code key}
     * {@code value}.
     *
     * @param iterator Initial AttributedCharacterIterator to add arg to
     * @param key Key for AttributedCharacterIterator
     * @param value Value associated with key in AttributedCharacterIterator
     * @return AttributedCharacterIterator wrapping args
     */
    AttributedCharacterIterator createAttributedCharacterIterator(
              AttributedCharacterIterator iterator,
              AttributedCharacterIterator.Attribute key, Object value) {
        AttributedString as = new AttributedString(iterator);

        as.addAttribute(key, value);
        return as.getIterator();
    }


    /**
     * Defines constants that are used as attribute keys in the
     * {@code AttributedCharacterIterator} returned
     * from {@code Format.formatToCharacterIterator} and as
     * field identifiers in {@code FieldPosition}.
     *
     * @since 1.4
     */
    public static class Field extends AttributedCharacterIterator.Attribute {

        // Proclaim serial compatibility with 1.4 FCS
        @java.io.Serial
        private static final long serialVersionUID = 276966692217360283L;

        /**
         * Creates a Field with the specified name.
         *
         * @param name Name of the attribute
         */
        protected Field(String name) {
            super(name);
        }
    }


    /**
     * FieldDelegate is notified by the various {@code Format}
     * implementations as they are formatting the Objects. This allows for
     * storage of the individual sections of the formatted String for
     * later use, such as in a {@code FieldPosition} or for an
     * {@code AttributedCharacterIterator}.
     * <p>
     * Delegates should NOT assume that the {@code Format} will notify
     * the delegate of fields in any particular order.
     *
     * @see FieldPosition#getFieldDelegate
     * @see CharacterIteratorFieldDelegate
     */
    interface FieldDelegate {
        /**
         * Notified when a particular region of the String is formatted. This
         * method will be invoked if there is no corresponding integer field id
         * matching {@code attr}.
         *
         * @param attr Identifies the field matched
         * @param value Value associated with the field
         * @param start Beginning location of the field, will be >= 0
         * @param end End of the field, will be >= start and <= buffer.length()
         * @param buffer Contains current formatted value, receiver should
         *        NOT modify it.
         */
        public void formatted(Format.Field attr, Object value, int start,
                              int end, StringBuffer buffer);

        /**
         * Notified when a particular region of the String is formatted.
         *
         * @param fieldID Identifies the field by integer
         * @param attr Identifies the field matched
         * @param value Value associated with the field
         * @param start Beginning location of the field, will be >= 0
         * @param end End of the field, will be >= start and <= buffer.length()
         * @param buffer Contains current formatted value, receiver should
         *        NOT modify it.
         */
        public void formatted(int fieldID, Format.Field attr, Object value,
                              int start, int end, StringBuffer buffer);
    }
}
