/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the interfaces for the Simple API for XML (SAX). Supports both
 * the SAX1 and SAX2 APIs.
 *
 * <h2> SAX2 Standard Feature Flags </h2>
 *
 * <p>
 * One of the essential characteristics of SAX2 is that it added
 * feature flags which can be used to examine and perhaps modify
 * parser modes, in particular modes such as validation.
 * Since features are identified by (absolute) URIs, anyone
 * can define such features.
 * Currently defined standard feature URIs have the prefix
 * <code>http://xml.org/sax/features/</code> before an identifier such as
 * <code>validation</code>.  Turn features on or off using
 * <em>setFeature</em>.  Those standard identifiers are:
 *
 *
 * <table class="striped">
 *     <caption>Standard Features</caption>
 *     <thead>
 *     <tr>
 *      <th scope="col">Feature ID</th>
 *      <th scope="col">Access</th>
 *      <th scope="col">Default</th>
 *      <th scope="col">Description</th>
 *      </tr>
 *     </thead>
 *
 *     <tbody>
 *     <tr>
 *      <th scope="row">external-general-entities</th>
 *      <td><em>read/write</em></td>
 *      <td><em>unspecified</em></td>
 *      <td> Reports whether this parser processes external
 *          general entities; always true if validating.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">external-parameter-entities</th>
 *      <td><em>read/write</em></td>
 *      <td><em>unspecified</em></td>
 *      <td> Reports whether this parser processes external
 *          parameter entities; always true if validating.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">is-standalone</th>
 *      <td>(parsing) <em>read-only</em>, (not parsing) <em>none</em></td>
 *      <td>not applicable</td>
 *      <td> May be examined only during a parse, after the
 *          <em>startDocument()</em> callback has been completed; read-only.
 *          The value is true if the document specified standalone="yes" in
 *          its XML declaration, and otherwise is false.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">lexical-handler/parameter-entities</th>
 *      <td><em>read/write</em></td>
 *      <td><em>unspecified</em></td>
 *      <td> A value of "true" indicates that the LexicalHandler will report
 *          the beginning and end of parameter entities.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">namespaces</th>
 *      <td><em>read/write</em></td>
 *      <td>true</td>
 *      <td> A value of "true" indicates namespace URIs and unprefixed local names
 *          for element and attribute names will be available.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">namespace-prefixes</th>
 *      <td><em>read/write</em></td>
 *      <td>false</td>
 *      <td> A value of "true" indicates that XML qualified names (with prefixes) and
 *          attributes (including <em>xmlns*</em> attributes) will be available.
 *              </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">resolve-dtd-uris</th>
 *      <td><em>read/write</em></td>
 *      <td><em>true</em></td>
 *      <td> A value of "true" indicates that system IDs in declarations will
 *          be absolutized (relative to their base URIs) before reporting.
 *          (That is the default behavior for all SAX2 XML parsers.)
 *          A value of "false" indicates those IDs will not be absolutized;
 *          parsers will provide the base URI from
 *          <em>Locator.getSystemId()</em>.
 *          This applies to system IDs passed in <ul>
 *              <li><em>DTDHandler.notationDecl()</em>,
 *              <li><em>DTDHandler.unparsedEntityDecl()</em>, and
 *              <li><em>DeclHandler.externalEntityDecl()</em>.
 *          </ul>
 *          It does not apply to <em>EntityResolver.resolveEntity()</em>,
 *          which is not used to report declarations, or to
 *          <em>LexicalHandler.startDTD()</em>, which already provides
 *          the non-absolutized URI.
 *          </td>
 *      </tr>
 *
 *     <tr>
 *      <th scope="row">string-interning</th>
 *      <td><em>read/write</em></td>
 *      <td><em>unspecified</em></td>
 *      <td> Has a value of "true" if all XML names (for elements, prefixes,
 *          attributes, entities, notations, and local names),
 *          as well as Namespace URIs, will have been interned
 *          using <em>java.lang.String.intern</em>. This supports fast
 *          testing of equality/inequality against string constants,
 *          rather than forcing slower calls to <em>String.equals()</em>.
 *          </td>
 *      </tr>
 *
 *     <tr>
 *     <th scope="row">unicode-normalization-checking</th>
 *     <td><em>read/write</em></td>
 *     <td><em>false</em></td>
 *     <td> Controls whether the parser reports Unicode normalization
 *         errors as described in section 2.13 and Appendix B of the
 *         XML 1.1 Recommendation. If true, Unicode normalization
 *         errors are reported using the ErrorHandler.error() callback.
 *         Such errors are not fatal in themselves (though, obviously,
 *         other Unicode-related encoding errors may be).
 *              </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">use-attributes2</th>
 *      <td><em>read-only</em></td>
 *      <td>not applicable</td>
 *      <td> Returns "true" if the <em>Attributes</em> objects passed by
 *          this parser in <em>ContentHandler.startElement()</em>
 *          implement the <a href="ext/Attributes2.html"
 *          ><em>org.xml.sax.ext.Attributes2</em></a> interface.
 *          That interface exposes additional DTD-related information,
 *          such as whether the attribute was specified in the
 *          source text rather than defaulted.
 *      </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">use-locator2</th>
 *      <td><em>read-only</em></td>
 *      <td>not applicable</td>
 *      <td> Returns "true" if the <em>Locator</em> objects passed by
 *          this parser in <em>ContentHandler.setDocumentLocator()</em>
 *          implement the <a href="ext/Locator2.html"
 *          ><em>org.xml.sax.ext.Locator2</em></a> interface.
 *          That interface exposes additional entity information,
 *          such as the character encoding and XML version used.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">use-entity-resolver2</th>
 *      <td><em>read/write</em></td>
 *      <td><em>true</em></td>
 *      <td> Returns "true" if, when <em>setEntityResolver</em> is given
 *          an object implementing the <a href="ext/EntityResolver2.html"
 *          ><em>org.xml.sax.ext.EntityResolver2</em></a> interface,
 *          those new methods will be used.
 *          Returns "false" to indicate that those methods will not be used.
 *      </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">validation</th>
 *      <td><em>read/write</em></td>
 *      <td><em>unspecified</em></td>
 *      <td> Controls whether the parser is reporting all validity
 *          errors; if true, all external entities will be read.
 *      </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">xmlns-uris</th>
 *      <td><em>read/write</em></td>
 *      <td><em>false</em></td>
 *      <td> Controls whether, when the <em>namespace-prefixes</em> feature
 *          is set, the parser treats namespace declaration attributes as
 *          being in the <em>http://www.w3.org/2000/xmlns/</em> namespace.
 *          By default, SAX2 conforms to the original "Namespaces in XML"
 *          Recommendation, which explicitly states that such attributes are
 *          not in any namespace.
 *          Setting this optional flag to "true" makes the SAX2 events conform to
 *          a later backwards-incompatible revision of that recommendation,
 *          placing those attributes in a namespace.
 *      </td>
 *     </tr>
 *
 *     <tr>
 *         <th scope="row">xml-1.1</th>
 *         <td><em>read-only</em></td>
 *         <td>not applicable</td>
 *         <td> Returns "true" if the parser supports both XML 1.1 and XML 1.0.
 *             Returns "false" if the parser supports only XML 1.0.
 *         </td>
 *     </tr>
 *     </tbody>
 * </table>
 *
 * <p>
 * Support for the default values of the
 * <em>namespaces</em> and <em>namespace-prefixes</em>
 * properties is required.
 * Support for any other feature flags is entirely optional.
 *
 *
 * <p>
 * For default values not specified by SAX2,
 * each XMLReader implementation specifies its default,
 * or may choose not to expose the feature flag.
 * Unless otherwise specified here,
 * implementations may support changing current values
 * of these standard feature flags, but not while parsing.
 *
 *
 * <h2> SAX2 Standard Handler and Property IDs </h2>
 *
 * <p>
 * For parser interface characteristics that are described
 * as objects, a separate namespace is defined.  The
 * objects in this namespace are again identified by URI, and
 * the standard property URIs have the prefix
 * <code>http://xml.org/sax/properties/</code> before an identifier such as
 * <code>lexical-handler</code> or
 * <code>dom-node</code>.  Manage those properties using
 * <em>setProperty()</em>.  Those identifiers are:
 *
 * <table class="striped">
 *     <caption>Standard Property IDs</caption>
 *     <thead>
 *     <tr>
 *      <th scope="col">Property ID</th>
 *      <th scope="col">Description</th>
 *      </tr>
 *      </thead>
 *
 *     <tbody>
 *     <tr>
 *      <th scope="row">declaration-handler</th>
 *      <td> Used to see most DTD declarations except those treated
 *          as lexical ("document element name is ...") or which are
 *          mandatory for all SAX parsers (<em>DTDHandler</em>).
 *          The Object must implement <a href="ext/DeclHandler.html"
 *          ><em>org.xml.sax.ext.DeclHandler</em></a>.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <th scope="row">document-xml-version</th>
 *         <td> May be examined only during a parse, after the startDocument()
 *             callback has been completed; read-only. This property is a
 *             literal string describing the actual XML version of the document,
 *             such as "1.0" or "1.1".
 *         </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">dom-node</th>
 *      <td> For "DOM Walker" style parsers, which ignore their
 *          <em>parser.parse()</em> parameters, this is used to
 *          specify the DOM (sub)tree being walked by the parser.
 *          The Object must implement the
 *          <em>org.w3c.dom.Node</em> interface.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">lexical-handler</th>
 *      <td> Used to see some syntax events that are essential in some
 *          applications:  comments, CDATA delimiters, selected general
 *          entity inclusions, and the start and end of the DTD
 *          (and declaration of document element name).
 *          The Object must implement <a href="ext/LexicalHandler.html"
 *          ><em>org.xml.sax.ext.LexicalHandler</em></a>.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *      <th scope="row">xml-string</th>
 *      <td> Readable only during a parser callback, this exposes a <b>TBS</b>
 *          chunk of characters responsible for the current event.
 *         </td>
 *     </tr>
 *     </tbody>
 * </table>
 *
 * <p>
 * All of these standard properties are optional.
 * XMLReader implementations are not required to support them.
 *
 * @apiNote The SAX API, originally developed at
 * <a href="http://www.saxproject.org">the SAX Project</a>,
 * has been defined by Java SE since 1.4.
 *
 * @since 1.4
 */

package org.xml.sax;
