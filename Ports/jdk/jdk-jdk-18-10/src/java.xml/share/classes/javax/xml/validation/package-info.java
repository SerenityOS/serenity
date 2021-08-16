/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * <p>
 * Provides an API for validation of XML documents.  <em>Validation</em> is the
 * process of verifying that an XML document is an instance of a specified XML
 * <em>schema</em>.  An XML schema defines the content model (also called a
 * <em>grammar</em> or <em>vocabulary</em>) that its instance documents will
 * represent.
 *
 * <p>
 * There are a number of popular technologies available for creating an XML schema.
 * Some of the most popular ones include:
 *
 * <ul>
 *     <li><strong>Document Type Definition (DTD)</strong>
 *         - XML's built-in schema language.
 *     </li>
 *     <li><strong><a href="http://www.w3.org/XML/Schema">W3C XML Schema (WXS)</a></strong> -
 *         an object-oriented XML schema language. WXS also provides a type system
 *         for constraining the character data of an XML document. WXS is maintained
 *         by the <a href="http://www.w3.org">World Wide Web Consortium (W3C)</a>
 *         and is a W3C Recommendation (that is, a ratified W3C standard specification).
 *     </li>
 *     <li><strong><a href="http://www.relaxng.org">RELAX NG (RNG)</a></strong> -
 *         a pattern-based, user-friendly XML schema language. RNG schemas may
 *         also use types to constrain XML character data. RNG is maintained by
 *         the <a href="http://www.oasis-open.org">Organization for the Advancement
 *         of Structured Information Standards (OASIS)</a> and is both an OASIS
 *         and an <a href="http://www.iso.org">ISO (International Organization
 *         for Standardization)</a> standard.
 *     </li>
 *     <li><strong><a href="http://standards.iso.org/ittf/PubliclyAvailableStandards/c055982_ISO_IEC_19757-3_2016.zip">Schematron</a></strong> -
 *         a rules-based XML schema language. Whereas DTD, WXS, and RNG are designed
 *         to express the structure of a content model, Schematron is designed to
 *         enforce individual rules that are difficult or impossible to express
 *         with other schema languages. Schematron is intended to supplement a
 *         schema written in structural schema language such as the aforementioned.
 *         Schematron is <a href="http://standards.iso.org/ittf/PubliclyAvailableStandards/index.html">an ISO standard</a>.
 *     </li>
 * </ul>
 * <p>
 * While JAXP supports validation as a feature of an XML parser, represented by
 * either a {@link javax.xml.parsers.SAXParser} or {@link javax.xml.parsers.DocumentBuilder}
 * instance, the {@code Validation} API is preferred.
 *
 * <p>
 * The JAXP validation API decouples the validation of an instance document from
 * the parsing of an XML document. This is advantageous for several reasons,
 * some of which are:
 *
 * <ul>
 *     <li><strong>Support for additional schema langauges.</strong>
 *         The JAXP parser implementations support only a subset of the available
 *         XML schema languages. The Validation API provides a standard mechanism
 *         through which applications may take of advantage of specialization
 *         validation libraries which support additional schema languages.
 *     </li>
 *     <li><strong>Easy runtime coupling of an XML instance and schema.</strong>
 *         Specifying the location of a schema to use for validation with JAXP
 *         parsers can be confusing. The Validation API makes this process simple
 *         (see <a href="#example-1">example</a> below).
 *     </li>
 * </ul>
 * <p>
 * <a id="example-1"><strong>Usage example</strong>.</a> The following example
 * demonstrates validating an XML document with the Validation API
 * (for readability, some exception handling is not shown):
 *
 * <pre>
 *
 *     // parse an XML document into a DOM tree
 *     DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
 *     Document document = parser.parse(new File("instance.xml"));
 *
 *     // create a SchemaFactory capable of understanding WXS schemas
 *     SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
 *
 *     // load a WXS schema, represented by a Schema instance
 *     Source schemaFile = new StreamSource(new File("mySchema.xsd"));
 *     Schema schema = factory.newSchema(schemaFile);
 *
 *     // create a Validator instance, which can be used to validate an instance document
 *     Validator validator = schema.newValidator();
 *
 *     // validate the DOM tree
 *     try {
 *         validator.validate(new DOMSource(document));
 *     } catch (SAXException e) {
 *         // instance document is invalid!
 *     }
 * </pre>
 * <p>
 * The JAXP parsing API has been integrated with the Validation API. Applications
 * may create a {@link javax.xml.validation.Schema} with the validation API
 * and associate it with a {@link javax.xml.parsers.DocumentBuilderFactory} or
 * a {@link javax.xml.parsers.SAXParserFactory} instance by using the
 * {@link javax.xml.parsers.DocumentBuilderFactory#setSchema(Schema)} and
 * {@link javax.xml.parsers.SAXParserFactory#setSchema(Schema)} methods.
 * <strong>You should not</strong> both set a schema and call <code>setValidating(true)</code>
 * on a parser factory. The former technique will cause parsers to use the new
 * validation API; the latter will cause parsers to use their own internal validation
 * facilities. <strong>Turning on both of these options simultaneously will cause
 * either redundant behavior or error conditions.</strong>
 *
 *
 * @since 1.5
 */

package javax.xml.validation;
