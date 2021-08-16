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
 * Defines the generic APIs for processing transformation instructions,
 * and performing a transformation from source to result. These interfaces have no
 * dependencies on SAX or the DOM standard, and try to make as few assumptions as
 * possible about the details of the source and result of a transformation. It
 * achieves this by defining {@link javax.xml.transform.Source} and
 * {@link javax.xml.transform.Result} interfaces.
 *
 * <p>
 * To provide concrete classes for the user, the API defines specializations
 * of the interfaces found at the root level. These interfaces are found in
 * {@link javax.xml.transform.sax}, {@link javax.xml.transform.dom},
 * {@link javax.xml.transform.stax}, and {@link javax.xml.transform.stream}.
 *
 *
 * <h2>Creating Objects</h2>
 *
 * <p>
 * The API allows a concrete {@link javax.xml.transform.TransformerFactory}
 * object to be created from the static function
 * {@link javax.xml.transform.TransformerFactory#newInstance}.
 *
 *
 * <h2>Specification of Inputs and Outputs</h2>
 *
 * <p>
 * This API defines two interface objects called {@link javax.xml.transform.Source}
 * and {@link javax.xml.transform.Result}. In order to pass Source and Result
 * objects to the interfaces, concrete classes must be used. The following concrete
 * representations are defined for each of these objects:
 * {@link javax.xml.transform.stream.StreamSource} and
 * {@link javax.xml.transform.stream.StreamResult},
 * {@link javax.xml.transform.stax.StAXSource} and
 * {@link javax.xml.transform.stax.StAXResult}, and
 * {@link javax.xml.transform.sax.SAXSource} and
 * {@link javax.xml.transform.sax.SAXResult}, and
 * {@link javax.xml.transform.dom.DOMSource} and
 * {@link javax.xml.transform.dom.DOMResult}. Each of these objects defines a
 * FEATURE string (which is in the form of a URL), which can be passed into
 * {@link javax.xml.transform.TransformerFactory#getFeature} to see if the given
 * type of Source or Result object is supported. For instance, to test if a
 * DOMSource and a StreamResult is supported, you can apply the following test.
 *
 * <pre>
 * <code>
 * TransformerFactory tfactory = TransformerFactory.newInstance();
 * if (tfactory.getFeature(DOMSource.FEATURE) &amp;&amp;
 *     tfactory.getFeature(StreamResult.FEATURE)) {
 *     ...
 * }
 * </code>
 * </pre>
 *
 *
 * <h2><a id="qname-delimiter">Qualified Name Representation</a></h2>
 *
 * <p>
 * <a href="http://www.w3.org/TR/REC-xml-names">Namespaces</a> present something
 * of a problem area when dealing with XML objects. Qualified Names appear in XML
 * markup as prefixed names. But the prefixes themselves do not hold identity.
 * Rather, it is the URIs that they contextually map to that hold the identity.
 * Therefore, when passing a Qualified Name like "xyz:foo" among Java programs,
 * one must provide a means to map "xyz" to a namespace.
 *
 * <p>
 * One solution has been to create a "QName" object that holds the namespace URI,
 * as well as the prefix and local name, but this is not always an optimal solution,
 * as when, for example, you want to use unique strings as keys in a dictionary
 * object. Not having a string representation also makes it difficult to specify
 * a namespaced identity outside the context of an XML document.
 *
 * <p>
 * In order to pass namespaced values to transformations, for instance when setting
 * a property or a parameter on a {@link javax.xml.transform.Transformer} object,
 * this specification defines that a String "qname" object parameter be passed as
 * two-part string, the namespace URI enclosed in curly braces ({}), followed by
 * the local name. If the qname has a null URI, then the String object only
 * contains the local name. An application can safely check for a non-null URI by
 * testing to see if the first character of the name is a '{' character.
 *
 * <p>
 * For example, if a URI and local name were obtained from an element defined with
 * &lt;xyz:foo xmlns:xyz="http://xyz.foo.com/yada/baz.html"/&gt;, then the
 * Qualified Name would be "{http://xyz.foo.com/yada/baz.html}foo". Note that the
 * prefix is lost.
 *
 *
 * <h2>Result Tree Serialization</h2>
 *
 * <p>
 * Serialization of the result tree to a stream can be controlled with the
 * {@link javax.xml.transform.Transformer#setOutputProperties} and the
 * {@link javax.xml.transform.Transformer#setOutputProperty} methods.
 * These properties only apply to stream results, they have no effect when
 * the result is a DOM tree or SAX event stream.
 *
 * <p>
 * Strings that match the <a href="http://www.w3.org/TR/xslt#output">XSLT
 * specification for xsl:output attributes</a> can be referenced from the
 * {@link javax.xml.transform.OutputKeys} class. Other strings can be
 * specified as well.
 * If the transformer does not recognize an output key, a
 * {@link java.lang.IllegalArgumentException} is thrown, unless the key name
 * is <a href="#qname-delimiter">namespace qualified</a>. Output key names
 * that are namespace qualified are always allowed, although they may be
 * ignored by some implementations.
 *
 * <p>
 * If all that is desired is the simple identity transformation of a
 * source to a result, then {@link javax.xml.transform.TransformerFactory}
 * provides a
 * {@link javax.xml.transform.TransformerFactory#newTransformer()} method
 * with no arguments. This method creates a Transformer that effectively copies
 * the source to the result. This method may be used to create a DOM from SAX
 * events or to create an XML or HTML stream from a DOM or SAX events.
 *
 * <h2>Exceptions and Error Reporting</h2>
 *
 * <p>
 * The transformation API throw three types of specialized exceptions. A
 * {@link javax.xml.transform.TransformerFactoryConfigurationError} is parallel to
 * the {@link javax.xml.parsers.FactoryConfigurationError}, and is thrown
 * when a configuration problem with the TransformerFactory exists. This error
 * will typically be thrown when the transformation factory class specified with
 * the "javax.xml.transform.TransformerFactory" system property cannot be found or
 * instantiated.
 *
 * <p>
 * A {@link javax.xml.transform.TransformerConfigurationException}
 * may be thrown if for any reason a Transformer can not be created. A
 * TransformerConfigurationException may be thrown if there is a syntax error in
 * the transformation instructions, for example when
 * {@link javax.xml.transform.TransformerFactory#newTransformer} is
 * called.
 *
 * <p>
 * {@link javax.xml.transform.TransformerException} is a general
 * exception that occurs during the course of a transformation. A transformer
 * exception may wrap another exception, and if any of the
 * {@link javax.xml.transform.TransformerException#printStackTrace()}
 * methods are called on it, it will produce a list of stack dumps, starting from
 * the most recent. The transformer exception also provides a
 * {@link javax.xml.transform.SourceLocator} object which indicates where
 * in the source tree or transformation instructions the error occurred.
 * {@link javax.xml.transform.TransformerException#getMessageAndLocation()}
 * may be called to get an error message with location info, and
 * {@link javax.xml.transform.TransformerException#getLocationAsString()}
 * may be called to get just the location string.
 *
 * <p>
 * Transformation warnings and errors are sent to an
 * {@link javax.xml.transform.ErrorListener}, at which point the application may
 * decide to report the error or warning, and may decide to throw an
 * <code>Exception</code> for a non-fatal error. The <code>ErrorListener</code>
 * may be set via {@link javax.xml.transform.TransformerFactory#setErrorListener}
 * for reporting errors that have to do with syntax errors in the transformation
 * instructions, or via {@link javax.xml.transform.Transformer#setErrorListener}
 * to report errors that occur during the transformation. The <code>ErrorListener</code>
 * on both objects will always be valid and non-<code>null</code>, whether set by
 * the application or a default implementation provided by the processor.
 *
 *
 * <h2>Resolution of URIs within a transformation</h2>
 *
 * <p>
 * The API provides a way for URIs referenced from within the stylesheet
 * instructions or within the transformation to be resolved by the calling
 * application. This can be done by creating a class that implements the
 * {@link javax.xml.transform.URIResolver} interface, with its one method,
 * {@link javax.xml.transform.URIResolver#resolve}, and use this class to
 * set the URI resolution for the transformation instructions or transformation
 * with {@link javax.xml.transform.TransformerFactory#setURIResolver} or
 * {@link javax.xml.transform.Transformer#setURIResolver}. The
 * <code>URIResolver.resolve</code> method takes two String arguments, the URI
 * found in the stylesheet instructions or built as part of the transformation
 * process, and the base URI against which the first argument will be made absolute
 * if the absolute URI is required.
 * The returned {@link javax.xml.transform.Source} object must be usable by
 * the transformer, as specified in its implemented features.
 *
 * @since 1.5
 */

package javax.xml.transform;
