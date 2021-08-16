/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

/**
 * Class {@code TextSyntax} is an abstract base class providing the common
 * implementation of all attributes whose value is a string. The text attribute
 * includes a locale to indicate the natural language. Thus, a text attribute
 * always represents a localized string. Once constructed, a text attribute's
 * value is immutable.
 *
 * @author David Mendenhall
 * @author Alan Kaminsky
 */
public abstract class TextSyntax implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8130648736378144102L;

    /**
     * String value of this text attribute.
     *
     * @serial
     */
    private String value;

    /**
     * Locale of this text attribute.
     *
     * @serial
     */
    private Locale locale;

    /**
     * Constructs a {@code TextAttribute} with the specified string and locale.
     *
     * @param  value text string
     * @param  locale natural language of the text string. {@code null} is
     *         interpreted to mean the default locale for as returned by
     *         {@code Locale.getDefault()}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    protected TextSyntax(String value, Locale locale) {
        this.value = verify (value);
        this.locale = verify (locale);
    }

    private static String verify(String value) {
        if (value == null) {
            throw new NullPointerException(" value is null");
        }
        return value;
    }

    private static Locale verify(Locale locale) {
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }

    /**
     * Returns this text attribute's text string.
     *
     * @return the text string
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns this text attribute's text string's natural language (locale).
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns a hashcode for this text attribute.
     *
     * @return a hashcode value for this object
     */
    public int hashCode() {
        return value.hashCode() ^ locale.hashCode();
    }

    /**
     * Returns whether this text attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code TextSyntax}.
     *   <li>This text attribute's underlying string and {@code object}'s
     *   underlying string are equal.
     *   <li>This text attribute's locale and {@code object}'s locale are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this text
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return(object != null &&
               object instanceof TextSyntax &&
               this.value.equals (((TextSyntax) object).value) &&
               this.locale.equals (((TextSyntax) object).locale));
    }

    /**
     * Returns a {@code String} identifying this text attribute. The
     * {@code String} is the attribute's underlying text string.
     *
     * @return a {@code String} identifying this object
     */
    public String toString(){
        return value;
    }
}
