/*
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
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2000 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de
 * Recherche en Informatique et en Automatique, Keio University). All
 * Rights Reserved. This program is distributed under the W3C's Software
 * Intellectual Property License. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 * See W3C License http://www.w3.org/Consortium/Legal/ for more details.
 */

package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 *  The <code>CSSStyleDeclaration</code> interface represents a single CSS
 * declaration block. This interface may be used to determine the style
 * properties currently set in a block or to set style properties explicitly
 * within the block.
 * <p> While an implementation may not recognize all CSS properties within a
 * CSS declaration block, it is expected to provide access to all specified
 * properties in the style sheet through the <code>CSSStyleDeclaration</code>
 *  interface. Furthermore, implementations that support a specific level of
 * CSS should correctly handle CSS shorthand properties for that level. For
 * a further discussion of shorthand properties, see the
 * <code>CSS2Properties</code> interface.
 * <p> This interface is also used to provide a read-only access to the
 * computed values of an element. See also the <code>ViewCSS</code>
 * interface.  The CSS Object Model doesn't provide an access to the
 * specified or actual values of the CSS cascade.
 * <p>See also the <a href='http://www.w3.org/TR/2000/REC-DOM-Level-2-Style-20001113'>Document Object Model (DOM) Level 2 Style Specification</a>.
 * @since 1.4, DOM Level 2
 */
public interface CSSStyleDeclaration {
    /**
     *  The parsable textual representation of the declaration block
     * (excluding the surrounding curly braces). Setting this attribute will
     * result in the parsing of the new value and resetting of all the
     * properties in the declaration block including the removal or addition
     * of properties.
     */
    public String getCssText();
    /**
     *  The parsable textual representation of the declaration block
     * (excluding the surrounding curly braces). Setting this attribute will
     * result in the parsing of the new value and resetting of all the
     * properties in the declaration block including the removal or addition
     * of properties.
     * @exception DOMException
     *   SYNTAX_ERR: Raised if the specified CSS string value has a syntax
     *   error and is unparsable.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this declaration is
     *   readonly or a property is readonly.
     */
    public void setCssText(String cssText)
                       throws DOMException;

    /**
     *  Used to retrieve the value of a CSS property if it has been explicitly
     * set within this declaration block.
     * @param propertyName  The name of the CSS property. See the CSS
     *   property index.
     * @return  Returns the value of the property if it has been explicitly
     *   set for this declaration block. Returns the empty string if the
     *   property has not been set.
     */
    public String getPropertyValue(String propertyName);

    /**
     *  Used to retrieve the object representation of the value of a CSS
     * property if it has been explicitly set within this declaration block.
     * This method returns <code>null</code> if the property is a shorthand
     * property. Shorthand property values can only be accessed and modified
     * as strings, using the <code>getPropertyValue</code> and
     * <code>setProperty</code> methods.
     * @param propertyName  The name of the CSS property. See the CSS
     *   property index.
     * @return  Returns the value of the property if it has been explicitly
     *   set for this declaration block. Returns <code>null</code> if the
     *   property has not been set.
     */
    public CSSValue getPropertyCSSValue(String propertyName);

    /**
     *  Used to remove a CSS property if it has been explicitly set within
     * this declaration block.
     * @param propertyName  The name of the CSS property. See the CSS
     *   property index.
     * @return  Returns the value of the property if it has been explicitly
     *   set for this declaration block. Returns the empty string if the
     *   property has not been set or the property name does not correspond
     *   to a known CSS property.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this declaration is readonly
     *   or the property is readonly.
     */
    public String removeProperty(String propertyName)
                                 throws DOMException;

    /**
     *  Used to retrieve the priority of a CSS property (e.g. the
     * <code>"important"</code> qualifier) if the priority has been
     * explicitly set in this declaration block.
     * @param propertyName  The name of the CSS property. See the CSS
     *   property index.
     * @return  A string representing the priority (e.g.
     *   <code>"important"</code>) if the property has been explicitly set
     *   in this declaration block and has a priority specified. The empty
     *   string otherwise.
     */
    public String getPropertyPriority(String propertyName);

    /**
     *  Used to set a property value and priority within this declaration
     * block. <code>setProperty</code> permits to modify a property or add a
     * new one in the declaration block. Any call to this method may modify
     * the order of properties in the <code>item</code> method.
     * @param propertyName  The name of the CSS property. See the CSS
     *   property index.
     * @param value  The new value of the property.
     * @param priority  The new priority of the property (e.g.
     *   <code>"important"</code>) or the empty string if none.
     * @exception DOMException
     *   SYNTAX_ERR: Raised if the specified value has a syntax error and is
     *   unparsable.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this declaration is
     *   readonly or the property is readonly.
     */
    public void setProperty(String propertyName,
                            String value,
                            String priority)
                            throws DOMException;

    /**
     *  The number of properties that have been explicitly set in this
     * declaration block. The range of valid indices is 0 to length-1
     * inclusive.
     */
    public int getLength();

    /**
     *  Used to retrieve the properties that have been explicitly set in this
     * declaration block. The order of the properties retrieved using this
     * method does not have to be the order in which they were set. This
     * method can be used to iterate over all properties in this declaration
     * block.
     * @param index  Index of the property name to retrieve.
     * @return  The name of the property at this ordinal position. The empty
     *   string if no property exists at this position.
     */
    public String item(int index);

    /**
     *  The CSS rule that contains this declaration block or <code>null</code>
     * if this <code>CSSStyleDeclaration</code> is not attached to a
     * <code>CSSRule</code>.
     */
    public CSSRule getParentRule();

}
