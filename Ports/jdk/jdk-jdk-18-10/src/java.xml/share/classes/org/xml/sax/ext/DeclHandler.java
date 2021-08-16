/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.ext;

import org.xml.sax.SAXException;


/**
 * SAX2 extension handler for DTD declaration events.
 *
 * <p>This is an optional extension handler for SAX2 to provide more
 * complete information about DTD declarations in an XML document.
 * XML readers are not required to recognize this handler, and it
 * is not part of core-only SAX2 distributions.</p>
 *
 * <p>Note that data-related DTD declarations (unparsed entities and
 * notations) are already reported through the {@link
 * org.xml.sax.DTDHandler DTDHandler} interface.</p>
 *
 * <p>If you are using the declaration handler together with a lexical
 * handler, all of the events will occur between the
 * {@link org.xml.sax.ext.LexicalHandler#startDTD startDTD} and the
 * {@link org.xml.sax.ext.LexicalHandler#endDTD endDTD} events.</p>
 *
 * <p>To set the DeclHandler for an XML reader, use the
 * {@link org.xml.sax.XMLReader#setProperty setProperty} method
 * with the property name
 * <code>http://xml.org/sax/properties/declaration-handler</code>
 * and an object implementing this interface (or null) as the value.
 * If the reader does not report declaration events, it will throw a
 * {@link org.xml.sax.SAXNotRecognizedException SAXNotRecognizedException}
 * when you attempt to register the handler.</p>
 *
 * @since 1.4, SAX 2.0 (extensions 1.0)
 * @author David Megginson
 */
public interface DeclHandler
{

    /**
     * Report an element type declaration.
     *
     * <p>The content model will consist of the string "EMPTY", the
     * string "ANY", or a parenthesised group, optionally followed
     * by an occurrence indicator.  The model will be normalized so
     * that all parameter entities are fully resolved and all whitespace
     * is removed,and will include the enclosing parentheses.  Other
     * normalization (such as removing redundant parentheses or
     * simplifying occurrence indicators) is at the discretion of the
     * parser.</p>
     *
     * @param name The element type name.
     * @param model The content model as a normalized string.
     * @throws SAXException The application may raise an exception.
     */
    public abstract void elementDecl (String name, String model)
        throws SAXException;


    /**
     * Report an attribute type declaration.
     *
     * <p>Only the effective (first) declaration for an attribute will
     * be reported.  The type will be one of the strings "CDATA",
     * "ID", "IDREF", "IDREFS", "NMTOKEN", "NMTOKENS", "ENTITY",
     * "ENTITIES", a parenthesized token group with
     * the separator "|" and all whitespace removed, or the word
     * "NOTATION" followed by a space followed by a parenthesized
     * token group with all whitespace removed.</p>
     *
     * <p>The value will be the value as reported to applications,
     * appropriately normalized and with entity and character
     * references expanded.  </p>
     *
     * @param eName The name of the associated element.
     * @param aName The name of the attribute.
     * @param type A string representing the attribute type.
     * @param mode A string representing the attribute defaulting mode
     *        ("#IMPLIED", "#REQUIRED", or "#FIXED") or null if
     *        none of these applies.
     * @param value A string representing the attribute's default value,
     *        or null if there is none.
     * @throws SAXException The application may raise an exception.
     */
    public abstract void attributeDecl (String eName,
                                        String aName,
                                        String type,
                                        String mode,
                                        String value)
        throws SAXException;


    /**
     * Report an internal entity declaration.
     *
     * <p>Only the effective (first) declaration for each entity
     * will be reported.  All parameter entities in the value
     * will be expanded, but general entities will not.</p>
     *
     * @param name The name of the entity.  If it is a parameter
     *        entity, the name will begin with '%'.
     * @param value The replacement text of the entity.
     * @throws SAXException The application may raise an exception.
     * @see #externalEntityDecl
     * @see org.xml.sax.DTDHandler#unparsedEntityDecl
     */
    public abstract void internalEntityDecl (String name, String value)
        throws SAXException;


    /**
     * Report a parsed external entity declaration.
     *
     * <p>Only the effective (first) declaration for each entity
     * will be reported.</p>
     *
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @param name The name of the entity.  If it is a parameter
     *        entity, the name will begin with '%'.
     * @param publicId The entity's public identifier, or null if none
     *        was given.
     * @param systemId The entity's system identifier.
     * @throws SAXException The application may raise an exception.
     * @see #internalEntityDecl
     * @see org.xml.sax.DTDHandler#unparsedEntityDecl
     */
    public abstract void externalEntityDecl (String name, String publicId,
                                             String systemId)
        throws SAXException;

}

// end of DeclHandler.java
