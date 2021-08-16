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

package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * <p><code>XPathFunctionResolver</code> provides access to the set of user defined <code>XPathFunction</code>s.</p>
 *
 * <p>XPath functions are resolved by name and arity.
 * The resolver is not needed for XPath built-in functions and the resolver
 * <strong><em>cannot</em></strong> be used to override those functions.</p>
 *
 * <p>In particular, the resolver is only called for functions in an another
 * namespace (functions with an explicit prefix). This means that you cannot
 * use the <code>XPathFunctionResolver</code> to implement specifications
 * like <a href="http://www.w3.org/TR/xmldsig-core/">XML-Signature Syntax
 * and Processing</a> which extend the function library of XPath 1.0 in the
 * same namespace. This is a consequence of the design of the resolver.</p>
 *
 * <p>If you wish to implement additional built-in functions, you will have to
 * extend the underlying implementation directly.</p>
 *
 * @author  Norman Walsh
 * @author  Jeff Suttor
 * @see <a href="http://www.w3.org/TR/xpath#corelib">XML Path Language (XPath) Version 1.0, Core Function Library</a>
 * @since 1.5
 */
public interface XPathFunctionResolver {
  /**
   * <p>Find a function in the set of available functions.</p>
   *
   * <p>If <code>functionName</code> or <code>arity</code> is <code>null</code>, then a <code>NullPointerException</code> is thrown.</p>
   *
   * @param functionName The function name.
   * @param arity The number of arguments that the returned function must accept.
   *
   * @return The function or <code>null</code> if no function named <code>functionName</code> with <code>arity</code> arguments exists.
   *
   * @throws NullPointerException If <code>functionName</code> or <code>arity</code> is <code>null</code>.
   */
  public XPathFunction resolveFunction(QName functionName, int arity);
}
