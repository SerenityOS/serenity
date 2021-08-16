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
 * The base class for element handlers.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 *
 * @see DocumentHandler
 */
public abstract class ElementHandler {
    private DocumentHandler owner;
    private ElementHandler parent;

    private String id;

    /**
     * Returns the document handler that creates this element handler.
     *
     * @return the owner document handler
     */
    public final DocumentHandler getOwner() {
        return this.owner;
    }

    /**
     * Sets the document handler that creates this element handler.
     * The owner document handler should be set after instantiation.
     * Such approach is used to simplify the extensibility.
     *
     * @param owner  the owner document handler
     * @see DocumentHandler#startElement
     */
    final void setOwner(DocumentHandler owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Every element should have owner");
        }
        this.owner = owner;
    }

    /**
     * Returns the element handler that contains this one.
     *
     * @return the parent element handler
     */
    public final ElementHandler getParent() {
        return this.parent;
    }

    /**
     * Sets the element handler that contains this one.
     * The parent element handler should be set after instantiation.
     * Such approach is used to simplify the extensibility.
     *
     * @param parent  the parent element handler
     * @see DocumentHandler#startElement
     */
    final void setParent(ElementHandler parent) {
        this.parent = parent;
    }

    /**
     * Returns the value of the variable with specified identifier.
     *
     * @param id  the identifier
     * @return the value of the variable
     */
    protected final Object getVariable(String id) {
        if (id.equals(this.id)) {
            ValueObject value = getValueObject();
            if (value.isVoid()) {
                throw new IllegalStateException("The element does not return value");
            }
            return value.getValue();
        }
        return (this.parent != null)
                ? this.parent.getVariable(id)
                : this.owner.getVariable(id);
    }

    /**
     * Returns the value of the parent element.
     *
     * @return the value of the parent element
     */
    protected Object getContextBean() {
        if (this.parent != null) {
            ValueObject value = this.parent.getValueObject();
            if (!value.isVoid()) {
                return value.getValue();
            }
            throw new IllegalStateException("The outer element does not return value");
        } else {
            Object value = this.owner.getOwner();
            if (value != null) {
                return value;
            }
            throw new IllegalStateException("The topmost element does not have context");
        }
    }

    /**
     * Parses attributes of the element.
     * By default, the following attribute is supported:
     * <dl>
     * <dt>id
     * <dd>the identifier of the variable that is intended to store the result
     * </dl>
     *
     * @param name   the attribute name
     * @param value  the attribute value
     */
    public void addAttribute(String name, String value) {
        if (name.equals("id")) { // NON-NLS: the attribute name
            this.id = value;
        } else {
            throw new IllegalArgumentException("Unsupported attribute: " + name);
        }
    }

    /**
     * This method is called before parsing of the element's body.
     * All attributes are parsed at this point.
     * By default, do nothing.
     */
    public void startElement() {
    }

    /**
     * This method is called after parsing of the element's body.
     * By default, it calculates the value of this element.
     * The following tasks are executing for any non-void value:
     * <ol>
     * <li>If the {@code id} attribute is set
     * the value of the variable with the specified identifier
     * is set to the value of this element.</li>
     * <li>This element is used as an argument of parent element if it is possible.</li>
     * </ol>
     *
     * @see #isArgument
     */
    public void endElement() {
        // do nothing if no value returned
        ValueObject value = getValueObject();
        if (!value.isVoid()) {
            if (this.id != null) {
                this.owner.setVariable(this.id, value.getValue());
            }
            if (isArgument()) {
                if (this.parent != null) {
                    this.parent.addArgument(value.getValue());
                } else {
                    this.owner.addObject(value.getValue());
                }
            }
        }
    }

    /**
     * Adds the character that contained in this element.
     * By default, only whitespaces are acceptable.
     *
     * @param ch  the character
     */
    public void addCharacter(char ch) {
        if ((ch != ' ') && (ch != '\n') && (ch != '\t') && (ch != '\r')) {
            throw new IllegalStateException("Illegal character with code " + (int) ch);
        }
    }

    /**
     * Adds the argument that is used to calculate the value of this element.
     * By default, no arguments are acceptable.
     *
     * @param argument  the value of the element that contained in this one
     */
    protected void addArgument(Object argument) {
        throw new IllegalStateException("Could not add argument to simple element");
    }

    /**
     * Tests whether the value of this element can be used
     * as an argument of the element that contained in this one.
     *
     * @return {@code true} if the value of this element can be used
     *         as an argument of the element that contained in this one,
     *         {@code false} otherwise
     */
    protected boolean isArgument() {
        return this.id == null;
    }

    /**
     * Returns the value of this element.
     *
     * @return the value of this element
     */
    protected abstract ValueObject getValueObject();
}
