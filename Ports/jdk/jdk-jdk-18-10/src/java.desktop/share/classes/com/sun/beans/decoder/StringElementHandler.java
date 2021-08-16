/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans.decoder;

/**
 * This class is intended to handle &lt;string&gt; element.
 * This element specifies {@link String} values.
 * The result value is created from text of the body of this element.
 * For example:<pre>
 * &lt;string&gt;description&lt;/string&gt;</pre>
 * is equivalent to {@code "description"} in Java code.
 * The value of inner element is calculated
 * before adding to the string using {@link String#valueOf(Object)}.
 * Note that all characters are used including whitespaces (' ', '\t', '\n', '\r').
 * So the value of the element<pre>
 * &lt;string&gt&lt;true&gt&lt;/string&gt;</pre>
 * is not equal to the value of the element<pre>
 * &lt;string&gt;
 *     &lt;true&gt;
 * &lt;/string&gt;</pre>
 * <p>The following attribute is supported:
 * <dl>
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
public class StringElementHandler extends ElementHandler {
    private StringBuilder sb = new StringBuilder();
    private ValueObject value = ValueObjectImpl.NULL;

    /**
     * Adds the character that contained in this element.
     *
     * @param ch  the character
     */
    @Override
    public final void addCharacter(char ch) {
        if (this.sb == null) {
            throw new IllegalStateException("Could not add chararcter to evaluated string element");
        }
        this.sb.append(ch);
    }

    /**
     * Adds the string value of the argument to the string value of this element.
     *
     * @param argument  the value of the element that contained in this one
     */
    @Override
    protected final void addArgument(Object argument) {
        if (this.sb == null) {
            throw new IllegalStateException("Could not add argument to evaluated string element");
        }
        this.sb.append(argument);
    }

    /**
     * Returns the value of this element.
     *
     * @return the value of this element
     */
    @Override
    protected final ValueObject getValueObject() {
        if (this.sb != null) {
            try {
                this.value = ValueObjectImpl.create(getValue(this.sb.toString()));
            }
            catch (RuntimeException exception) {
                getOwner().handleException(exception);
            }
            finally {
                this.sb = null;
            }
        }
        return this.value;
    }

    /**
     * Returns the text of the body of this element.
     * This method evaluates value from text of the body,
     * and should be overridden in those handlers
     * that extend behavior of this element.
     *
     * @param argument  the text of the body
     * @return evaluated value
     */
    protected Object getValue(String argument) {
        return argument;
    }
}
