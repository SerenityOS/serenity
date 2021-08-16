/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.validation;

import org.w3c.dom.TypeInfo;

/**
 * This class provides access to the type information determined
 * by {@link ValidatorHandler}.
 *
 * <p>
 * Some schema languages, such as W3C XML Schema, encourages a validator
 * to report the "type" it assigns to each attribute/element.
 * Those applications who wish to access this type information can invoke
 * methods defined on this "interface" to access such type information.
 *
 * <p>
 * Implementation of this "interface" can be obtained through the
 * {@link ValidatorHandler#getTypeInfoProvider()} method.
 *
 * @author  Kohsuke Kawaguchi
 * @see org.w3c.dom.TypeInfo
 * @since 1.5
 */
public abstract class TypeInfoProvider {

    /**
     * Constructor for the derived class.
     *
     * <p>
     * The constructor does nothing.
     */
    protected TypeInfoProvider() {
    }

    /**
     * <p>Returns the immutable {@link TypeInfo} object for the current
     * element.</p>
     *
     * <p>The method may only be called by the startElement event
     * or the endElement event
     * of the {@link org.xml.sax.ContentHandler} that the application sets to
     * the {@link ValidatorHandler}.</p>
     *
     * <p>When W3C XML Schema validation is being performed, in the
     * case where an element has a union type, the {@link TypeInfo}
     * returned by a call to <code>getElementTypeInfo()</code> from the
     * startElement
     * event will be the union type. The <code>TypeInfo</code>
     * returned by a call
     * from the endElement event will be the actual member type used
     * to validate the element.</p>
     *
     * @throws IllegalStateException
     *      If this method is called from other {@link org.xml.sax.ContentHandler}
     *      methods.
     * @return
     *      An immutable {@link TypeInfo} object that represents the
     *      type of the current element.
     *      Note that the caller can keep references to the obtained
     *      {@link TypeInfo} longer than the callback scope.
     *
     *      Otherwise, this method returns
     *      null if the validator is unable to
     *      determine the type of the current element for some reason
     *      (for example, if the validator is recovering from
     *      an earlier error.)
     *
     */
    public abstract TypeInfo getElementTypeInfo();

    /**
     * Returns the immutable {@link TypeInfo} object for the specified
     * attribute of the current element.
     *
     * <p>
     * The method may only be called by the startElement event of
     * the {@link org.xml.sax.ContentHandler} that the application sets to the
     * {@link ValidatorHandler}.</p>
     *
     * @param index
     *      The index of the attribute. The same index for
     *      the {@link org.xml.sax.Attributes} object passed to the
     *      <code>startElement</code> callback.
     *
     * @throws IndexOutOfBoundsException
     *      If the index is invalid.
     * @throws IllegalStateException
     *      If this method is called from other {@link org.xml.sax.ContentHandler}
     *      methods.
     *
     * @return
     *      An immutable {@link TypeInfo} object that represents the
     *      type of the specified attribute.
     *      Note that the caller can keep references to the obtained
     *      {@link TypeInfo} longer than the callback scope.
     *
     *      Otherwise, this method returns
     *      null if the validator is unable to
     *      determine the type.
     */
    public abstract TypeInfo getAttributeTypeInfo(int index);

    /**
     * Returns <code>true</code> if the specified attribute is determined
     * to be ID.
     *
     * <p>
     * Exacly how an attribute is "determined to be ID" is up to the
     * schema language. In case of W3C XML Schema, this means
     * that the actual type of the attribute is the built-in ID type
     * or its derived type.
     *
     * <p>
     * A {@link javax.xml.parsers.DocumentBuilder} uses this information
     * to properly implement {@link org.w3c.dom.Attr#isId()}.
     *
     * <p>
     * The method may only be called by the startElement event of
     * the {@link org.xml.sax.ContentHandler} that the application sets to the
     * {@link ValidatorHandler}.
     *
     * @param index
     *      The index of the attribute. The same index for
     *      the {@link org.xml.sax.Attributes} object passed to the
     *      <code>startElement</code> callback.
     *
     * @throws IndexOutOfBoundsException
     *      If the index is invalid.
     * @throws IllegalStateException
     *      If this method is called from other {@link org.xml.sax.ContentHandler}
     *      methods.
     *
     * @return true
     *      if the type of the specified attribute is ID.
     */
    public abstract boolean isIdAttribute(int index);

    /**
     * Returns <code>false</code> if the attribute was added by the validator.
     *
     * <p>
     * This method provides information necessary for
     * a {@link javax.xml.parsers.DocumentBuilder} to determine what
     * the DOM tree should return from the {@link org.w3c.dom.Attr#getSpecified()} method.
     *
     * <p>
     * The method may only be called by the startElement event of
     * the {@link org.xml.sax.ContentHandler} that the application sets to the
     * {@link ValidatorHandler}.
     *
     * <p>
     * A general guideline for validators is to return true if
     * the attribute was originally present in the pipeline, and
     * false if it was added by the validator.
     *
     * @param index
     *      The index of the attribute. The same index for
     *      the {@link org.xml.sax.Attributes} object passed to the
     *      <code>startElement</code> callback.
     *
     * @throws IndexOutOfBoundsException
     *      If the index is invalid.
     * @throws IllegalStateException
     *      If this method is called from other {@link org.xml.sax.ContentHandler}
     *      methods.
     *
     * @return
     *      <code>true</code> if the attribute was present before the validator
     *      processes input. <code>false</code> if the attribute was added
     *      by the validator.
     */
    public abstract boolean isSpecified(int index);
}
