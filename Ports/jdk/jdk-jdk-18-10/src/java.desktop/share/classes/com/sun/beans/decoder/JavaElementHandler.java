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

import java.beans.XMLDecoder;

/**
 * This class is intended to handle &lt;java&gt; element.
 * Each element that appears in the body of this element
 * is evaluated in the context of the decoder itself.
 * Typically this outer context is used to retrieve the owner of the decoder,
 * which can be set before reading the archive.
 * <p>The following attributes are supported:
 * <dl>
 * <dt>version
 * <dd>the Java version (not supported)
 * <dt>class
 * <dd>the type of preferable parser (not supported)
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @see DocumentHandler#getOwner
 * @see DocumentHandler#setOwner
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
final class JavaElementHandler extends ElementHandler {
    private Class<?> type;
    private ValueObject value;

    /**
     * Parses attributes of the element.
     * The following attributes are supported:
     * <dl>
     * <dt>version
     * <dd>the Java version (not supported)
     * <dt>class
     * <dd>the type of preferable parser (not supported)
     * <dt>id
     * <dd>the identifier of the variable that is intended to store the result
     * </dl>
     *
     * @param name   the attribute name
     * @param value  the attribute value
     */
    @Override
    public void addAttribute(String name, String value) {
        if (name.equals("version")) { // NON-NLS: the attribute name
            // unsupported attribute
        } else if (name.equals("class")) { // NON-NLS: the attribute name
            // check class for owner
            this.type = getOwner().findClass(value);
        } else {
            super.addAttribute(name, value);
        }
    }

    /**
     * Adds the argument to the list of readed objects.
     *
     * @param argument  the value of the element that contained in this one
     */
    @Override
    protected void addArgument(Object argument) {
        getOwner().addObject(argument);
    }

    /**
     * Tests whether the value of this element can be used
     * as an argument of the element that contained in this one.
     *
     * @return {@code true} if the value of this element should be used
     *         as an argument of the element that contained in this one,
     *         {@code false} otherwise
     */
    @Override
    protected boolean isArgument() {
        return false; // do not use owner as object
    }

    /**
     * Returns the value of this element.
     *
     * @return the value of this element
     */
    @Override
    protected ValueObject getValueObject() {
        if (this.value == null) {
            this.value = ValueObjectImpl.create(getValue());
        }
        return this.value;
    }

    /**
     * Returns the owner of the owner document handler
     * as a value of &lt;java&gt; element.
     *
     * @return the owner of the owner document handler
     */
    private Object getValue() {
        Object owner = getOwner().getOwner();
        if ((this.type == null) || isValid(owner)) {
            return owner;
        }
        if (owner instanceof XMLDecoder) {
            XMLDecoder decoder = (XMLDecoder) owner;
            owner = decoder.getOwner();
            if (isValid(owner)) {
                return owner;
            }
        }
        throw new IllegalStateException("Unexpected owner class: " + owner.getClass().getName());
    }

    /**
     * Validates the owner of the &lt;java&gt; element.
     * The owner is valid if it is {@code null} or an instance
     * of the class specified by the {@code class} attribute.
     *
     * @param owner  the owner of the &lt;java&gt; element
     * @return {@code true} if the {@code owner} is valid;
     *         {@code false} otherwise
     */
    private boolean isValid(Object owner) {
        return (owner == null) || this.type.isInstance(owner);
    }
}
