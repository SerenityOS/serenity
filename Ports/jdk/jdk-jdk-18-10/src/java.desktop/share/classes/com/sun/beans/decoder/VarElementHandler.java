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
 * This class is intended to handle &lt;var&gt; element.
 * This element retrieves the value of specified variable.
 * For example:<pre>
 * &lt;var id="id1" idref="id2"/&gt;</pre>
 * is equivalent to {@code id1 = id2} in Java code.
 * <p>The following attributes are supported:
 * <dl>
 * <dt>idref
 * <dd>the identifier to refer to the variable
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
final class VarElementHandler extends ElementHandler {
    private ValueObject value;

    /**
     * Parses attributes of the element.
     * The following attributes are supported:
     * <dl>
     * <dt>idref
     * <dd>the identifier to refer to the variable
     * <dt>id
     * <dd>the identifier of the variable that is intended to store the result
     * </dl>
     *
     * @param name   the attribute name
     * @param value  the attribute value
     */
    @Override
    public void addAttribute(String name, String value) {
        if (name.equals("idref")) { // NON-NLS: the attribute name
            this.value = ValueObjectImpl.create(getVariable(value));
        } else {
            super.addAttribute(name, value);
        }
    }

    /**
     * Returns the value of this element.
     *
     * @return the value of this element
     */
    @Override
    protected ValueObject getValueObject() {
        if (this.value == null) {
            throw new IllegalArgumentException("Variable name is not set");
        }
        return this.value;
    }
}
