/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.stream.events;

import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * The StartElement interface provides access to information about
 * start elements.  A StartElement is reported for each Start Tag
 * in the document.
 *
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @since 1.6
 */
public interface StartElement extends XMLEvent {

  /**
   * Get the name of this event
   * @return the qualified name of this event
   */
  public QName getName();

  /**
   * Returns an Iterator of non-namespace attributes declared on this START_ELEMENT.
   * Returns an empty iterator if there are no attributes.
   * The iterator must contain only implementations of the
   * {@link Attribute} interface.
   * Attributes are fundamentally unordered and may be reported
   * in any order.
   *
   * @return a readonly Iterator over Attribute interfaces, or an
   * empty iterator
   */
  public Iterator<Attribute> getAttributes();

  /**
   * Returns an Iterator of namespaces declared on this element.
   * This Iterator does not contain previously declared namespaces
   * unless they appear on the current START_ELEMENT.
   * Therefore this list may contain redeclared namespaces and duplicate namespace
   * declarations. Use the getNamespaceContext() method to get the
   * current context of namespace declarations.
   *
   * <p>The iterator must contain only implementations of the
   * {@link Namespace} interface.
   *
   * <p>A {@link Namespace} is an {@link Attribute}.  One
   * can iterate over a list of namespaces as a list of attributes.
   * However this method returns only the list of namespaces
   * declared on this START_ELEMENT and does not
   * include the attributes declared on this START_ELEMENT.
   *
   * Returns an empty iterator if there are no namespaces.
   *
   * @return a readonly Iterator over Namespace interfaces, or an
   * empty iterator
   *
   */
  public Iterator<Namespace> getNamespaces();

  /**
   * Returns the attribute referred to by the qname.
   * @param name the qname of the desired attribute
   * @return the attribute corresponding to the name value or null
   */
  public Attribute getAttributeByName(QName name);

  /**
   * Gets a read-only namespace context. If no context is
   * available this method will return an empty namespace context.
   * The NamespaceContext contains information about all namespaces
   * in scope for this StartElement.
   *
   * @return the current namespace context
   */
  public NamespaceContext getNamespaceContext();

  /**
   * Gets the value that the prefix is bound to in the
   * context of this element.  Returns null if
   * the prefix is not bound in this context
   * @param prefix the prefix to lookup
   * @return the uri bound to the prefix or null
   */
  public String getNamespaceURI(String prefix);
}
