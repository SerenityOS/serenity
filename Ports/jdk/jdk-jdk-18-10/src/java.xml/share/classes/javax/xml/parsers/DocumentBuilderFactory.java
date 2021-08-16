/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.parsers;

import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import javax.xml.validation.Schema;

/**
 * Defines a factory API that enables applications to obtain a
 * parser that produces DOM object trees from XML documents.
 *
 * @author Jeff Suttor
 * @author Neeraj Bajaj
 *
 * @since 1.4
 */

public abstract class DocumentBuilderFactory {
    private static final String DEFAULT_IMPL =
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
    private boolean validating = false;
    private boolean namespaceAware = false;
    private boolean whitespace = false;
    private boolean expandEntityRef = true;
    private boolean ignoreComments = false;
    private boolean coalescing = false;

    /**
     * Protected constructor to prevent instantiation.
     * Use {@link #newInstance()}.
     */
    protected DocumentBuilderFactory () {
    }

    /**
     * Creates a new NamespaceAware instance of the {@code DocumentBuilderFactory}
     * builtin system-default implementation. Parsers produced by the factory
     * instance provides support for XML namespaces by default.
     *
     * @implSpec
     * In addition to creating a factory instance using the same process as
     * {@link #newDefaultInstance()}, this method must set NamespaceAware to true.
     *
     * @return a new instance of the {@code DocumentBuilderFactory} builtin
     *         system-default implementation.
     *
     * @since 13
     */
    public static DocumentBuilderFactory newDefaultNSInstance() {
        return makeNSAware(new DocumentBuilderFactoryImpl());
    }

    /**
     * Creates a new NamespaceAware instance of a {@code DocumentBuilderFactory}.
     * Parsers produced by the factory instance provides support for XML namespaces
     * by default.
     *
     * @implSpec
     * In addition to creating a factory instance using the same process as
     * {@link #newInstance()}, this method must set NamespaceAware to true.
     *
     * @return a new instance of a {@code DocumentBuilderFactory}
     *
     * @throws FactoryConfigurationError in case of {@linkplain
     *         java.util.ServiceConfigurationError service configuration error}
     *         or if the implementation is not available or cannot be instantiated.
     *
     * @since 13
     */
    public static DocumentBuilderFactory newNSInstance() {
        return makeNSAware(FactoryFinder.find(DocumentBuilderFactory.class, DEFAULT_IMPL));
    }

    /**
     * Creates a new NamespaceAware instance of a {@code DocumentBuilderFactory}
     * from the class name. Parsers produced by the factory instance provides
     * support for XML namespaces by default.
     *
     * @implSpec
     * In addition to creating a factory instance using the same process as
     * {@link #newInstance(java.lang.String, java.lang.ClassLoader)}, this method
     * must set NamespaceAware to true.
     *
     * @param factoryClassName a fully qualified factory class name that provides
     *                         implementation of
     *                         {@code javax.xml.parsers.DocumentBuilderFactory}.
     *
     * @param classLoader the {@code ClassLoader} used to load the factory class.
     *                    If it is {@code null}, the current {@code Thread}'s
     *                    context classLoader is used to load the factory class.
     *
     * @return a new instance of a {@code DocumentBuilderFactory}
     *
     * @throws FactoryConfigurationError if {@code factoryClassName} is {@code null}, or
     *                                   the factory class cannot be loaded, instantiated.
     *
     * @since 13
     */
    public static DocumentBuilderFactory newNSInstance(String factoryClassName,
            ClassLoader classLoader) {
            return makeNSAware(FactoryFinder.newInstance(
                    DocumentBuilderFactory.class, factoryClassName, classLoader, false));
    }

    /**
     * Creates a new instance of the {@code DocumentBuilderFactory} builtin
     * system-default implementation.
     *
     * @return A new instance of the {@code DocumentBuilderFactory} builtin
     *         system-default implementation.
     *
     * @since 9
     */
    public static DocumentBuilderFactory newDefaultInstance() {
        return new DocumentBuilderFactoryImpl();
    }

    /**
     * Obtains a new instance of a {@code DocumentBuilderFactory}.
     * This method uses the
     * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
     * to determine the {@code DocumentBuilderFactory} implementation class to load.
     *
     * <p>
     * Once an application has obtained a reference to a
     * {@code DocumentBuilderFactory}, it can use the factory to
     * configure and obtain parser instances.
     *
     *
     * <h4>Tip for Trouble-shooting</h4>
     * <p>
     * Setting the {@code jaxp.debug} system property will cause
     * this method to print a lot of debug messages
     * to {@code System.err} about what it is doing and where it is looking at.
     *
     * <p>
     * If you have problems loading {@link DocumentBuilder}s, try:
     * <pre>
     * java -Djaxp.debug=1 YourProgram ....
     * </pre>
     *
     * @return New instance of a {@code DocumentBuilderFactory}
     *
     * @throws FactoryConfigurationError in case of {@linkplain
     * java.util.ServiceConfigurationError service configuration error} or if
     * the implementation is not available or cannot be instantiated.
     */
    public static DocumentBuilderFactory newInstance() {
        return FactoryFinder.find(
                /* The default property name according to the JAXP spec */
                DocumentBuilderFactory.class, // "javax.xml.parsers.DocumentBuilderFactory"
                /* The fallback implementation class name */
                DEFAULT_IMPL);
    }

    /**
     * Obtain a new instance of a {@code DocumentBuilderFactory} from class name.
     * This function is useful when there are multiple providers in the classpath.
     * It gives more control to the application as it can specify which provider
     * should be loaded.
     *
     * <p>Once an application has obtained a reference to a {@code DocumentBuilderFactory}
     * it can use the factory to configure and obtain parser instances.
     *
     *
     * <h4>Tip for Trouble-shooting</h4>
     * <p>Setting the {@code jaxp.debug} system property will cause
     * this method to print a lot of debug messages
     * to {@code System.err} about what it is doing and where it is looking at.
     *
     * <p> If you have problems try:
     * <pre>
     * java -Djaxp.debug=1 YourProgram ....
     * </pre>
     *
     * @param factoryClassName fully qualified factory class name that provides
     *        implementation of {@code javax.xml.parsers.DocumentBuilderFactory}.
     *
     * @param classLoader {@code ClassLoader} used to load the factory class. If {@code null}
     *                     current {@code Thread}'s context classLoader is used to load the factory class.
     *
     * @return New instance of a {@code DocumentBuilderFactory}
     *
     * @throws FactoryConfigurationError if {@code factoryClassName} is {@code null}, or
     *                                   the factory class cannot be loaded, instantiated.
     *
     * @see #newInstance()
     *
     * @since 1.6
     */
    public static DocumentBuilderFactory newInstance(String factoryClassName, ClassLoader classLoader){
            //do not fallback if given classloader can't find the class, throw exception
            return FactoryFinder.newInstance(DocumentBuilderFactory.class,
                        factoryClassName, classLoader, false);
    }

    private static DocumentBuilderFactory makeNSAware(DocumentBuilderFactory dbf) {
        dbf.setNamespaceAware(true);
        return dbf;
    }

    /**
     * Creates a new instance of a {@link javax.xml.parsers.DocumentBuilder}
     * using the currently configured parameters.
     *
     * @return A new instance of a DocumentBuilder.
     *
     * @throws ParserConfigurationException if a DocumentBuilder
     *   cannot be created which satisfies the configuration requested.
     */

    public abstract DocumentBuilder newDocumentBuilder()
        throws ParserConfigurationException;


    /**
     * Specifies that the parser produced by this code will
     * provide support for XML namespaces. By default the value of this is set
     * to {@code false}
     *
     * @param awareness true if the parser produced will provide support
     *                  for XML namespaces; false otherwise.
     */

    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    /**
     * Specifies that the parser produced by this code will
     * validate documents as they are parsed. By default the value of this
     * is set to {@code false}.
     *
     * <p>
     * Note that "the validation" here means
     * <a href="http://www.w3.org/TR/REC-xml#proc-types">a validating
     * parser</a> as defined in the XML recommendation.
     * In other words, it essentially just controls the DTD validation.
     * (except the legacy two properties defined in JAXP 1.2.)
     *
     * <p>
     * To use modern schema languages such as W3C XML Schema or
     * RELAX NG instead of DTD, you can configure your parser to be
     * a non-validating parser by leaving the {@link #setValidating(boolean)}
     * method {@code false}, then use the {@link #setSchema(Schema)}
     * method to associate a schema to a parser.
     *
     * @param validating true if the parser produced will validate documents
     *                   as they are parsed; false otherwise.
     */

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Specifies that the parsers created by this  factory must eliminate
     * whitespace in element content (sometimes known loosely as
     * 'ignorable whitespace') when parsing XML documents (see XML Rec
     * 2.10). Note that only whitespace which is directly contained within
     * element content that has an element only content model (see XML
     * Rec 3.2.1) will be eliminated. Due to reliance on the content model
     * this setting requires the parser to be in validating mode. By default
     * the value of this is set to {@code false}.
     *
     * @param whitespace true if the parser created must eliminate whitespace
     *                   in the element content when parsing XML documents;
     *                   false otherwise.
     */

    public void setIgnoringElementContentWhitespace(boolean whitespace) {
        this.whitespace = whitespace;
    }

    /**
     * Specifies that the parser produced by this code will
     * expand entity reference nodes. By default the value of this is set to
     * {@code true}
     *
     * @param expandEntityRef true if the parser produced will expand entity
     *                        reference nodes; false otherwise.
     */

    public void setExpandEntityReferences(boolean expandEntityRef) {
        this.expandEntityRef = expandEntityRef;
    }

    /**
     * Specifies that the parser produced by this code will
     * ignore comments. By default the value of this is set to {@code false}.
     *
     * @param ignoreComments {@code boolean} value to ignore comments during processing
     */

    public void setIgnoringComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    /**
     * Specifies that the parser produced by this code will
     * convert CDATA nodes to Text nodes and append it to the
     * adjacent (if any) text node. By default the value of this is set to
     * {@code false}
     *
     * @param coalescing  true if the parser produced will convert CDATA nodes
     *                    to Text nodes and append it to the adjacent (if any)
     *                    text node; false otherwise.
     */

    public void setCoalescing(boolean coalescing) {
        this.coalescing = coalescing;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which are namespace aware.
     *
     * @return  true if the factory is configured to produce parsers which
     *          are namespace aware; false otherwise.
     */

    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which validate the XML content during parse.
     *
     * @return  true if the factory is configured to produce parsers
     *          which validate the XML content during parse; false otherwise.
     */

    public boolean isValidating() {
        return validating;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which ignore ignorable whitespace in element content.
     *
     * @return  true if the factory is configured to produce parsers
     *          which ignore ignorable whitespace in element content;
     *          false otherwise.
     */

    public boolean isIgnoringElementContentWhitespace() {
        return whitespace;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which expand entity reference nodes.
     *
     * @return  true if the factory is configured to produce parsers
     *          which expand entity reference nodes; false otherwise.
     */

    public boolean isExpandEntityReferences() {
        return expandEntityRef;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which ignores comments.
     *
     * @return  true if the factory is configured to produce parsers
     *          which ignores comments; false otherwise.
     */

    public boolean isIgnoringComments() {
        return ignoreComments;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which converts CDATA nodes to Text nodes and appends it to
     * the adjacent (if any) Text node.
     *
     * @return  true if the factory is configured to produce parsers
     *          which converts CDATA nodes to Text nodes and appends it to
     *          the adjacent (if any) Text node; false otherwise.
     */

    public boolean isCoalescing() {
        return coalescing;
    }

    /**
     * Allows the user to set specific attributes on the underlying
     * implementation.
     * <p>
     * All implementations that implement JAXP 1.5 or newer are required to
     * support the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} and
     * {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} properties.
     *
     * <ul>
     *   <li>
     *      Setting the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property
     *      restricts the access to external DTDs, external Entity References to the
     *      protocols specified by the property.
     *      If access is denied during parsing due to the restriction of this property,
     *      {@link org.xml.sax.SAXException} will be thrown by the parse methods defined by
     *      {@link javax.xml.parsers.DocumentBuilder}.
     *   </li>
     *   <li>
     *      Setting the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} property
     *      restricts the access to external Schema set by the schemaLocation attribute to
     *      the protocols specified by the property.  If access is denied during parsing
     *      due to the restriction of this property, {@link org.xml.sax.SAXException}
     *      will be thrown by the parse methods defined by
     *      {@link javax.xml.parsers.DocumentBuilder}.
     *   </li>
     * </ul>
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     *
     * @throws IllegalArgumentException thrown if the underlying
     *   implementation doesn't recognize the attribute.
     */
    public abstract void setAttribute(String name, Object value)
                throws IllegalArgumentException;

    /**
     * Allows the user to retrieve specific attributes on the underlying
     * implementation.
     *
     * @param name The name of the attribute.
     *
     * @return value The value of the attribute.
     *
     * @throws IllegalArgumentException thrown if the underlying
     *   implementation doesn't recognize the attribute.
     */
    public abstract Object getAttribute(String name)
                throws IllegalArgumentException;

    /**
     * Set a feature for this {@code DocumentBuilderFactory}
     * and {@code DocumentBuilder}s created by this factory.
     *
     * <p>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * A {@link ParserConfigurationException} is thrown if this {@code DocumentBuilderFactory} or the
     * {@code DocumentBuilder}s it creates cannot support the feature.
     * It is possible for a {@code DocumentBuilderFactory} to expose a feature value but be unable to change its state.
     *
     *
     * <p>
     * All implementations are required to support the {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature.
     * When the feature is:
     * <ul>
     *   <li>
     *     {@code true}: the implementation will limit XML processing to conform to implementation limits.
     *     Examples include entity expansion limits and XML Schema constructs that would consume large amounts of resources.
     *     If XML processing is limited for security reasons, it will be reported via a call to the registered
     *    {@link org.xml.sax.ErrorHandler#fatalError(SAXParseException exception)}.
     *     See {@link  DocumentBuilder#setErrorHandler(org.xml.sax.ErrorHandler errorHandler)}.
     *   </li>
     *   <li>
     *     {@code false}: the implementation will processing XML according to the XML specifications without
     *     regard to possible implementation limits.
     *   </li>
     * </ul>
     *
     * @param name Feature name.
     * @param value Is feature state {@code true} or {@code false}.
     *
     * @throws ParserConfigurationException if this {@code DocumentBuilderFactory} or the {@code DocumentBuilder}s
     *   it creates cannot support this feature.
     * @throws NullPointerException If the {@code name} parameter is null.
     * @since 1.5
     */
    public abstract void setFeature(String name, boolean value)
            throws ParserConfigurationException;

    /**
     * Get the state of the named feature.
     *
     * <p>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link ParserConfigurationException} is thrown if this {@code DocumentBuilderFactory} or the
     * {@code DocumentBuilder}s it creates cannot support the feature.
     * It is possible for an {@code DocumentBuilderFactory} to expose a feature value but be unable to change its state.
     *
     * @param name Feature name.
     *
     * @return State of the named feature.
     *
     * @throws ParserConfigurationException if this {@code DocumentBuilderFactory}
     *   or the {@code DocumentBuilder}s it creates cannot support this feature.
     * @since 1.5
     */
    public abstract boolean getFeature(String name)
            throws ParserConfigurationException;


    /**
     * Gets the {@link Schema} object specified through
     * the {@link #setSchema(Schema schema)} method.
     *
     * @return
     *      the {@link Schema} object that was last set through
     *      the {@link #setSchema(Schema)} method, or null
     *      if the method was not invoked since a {@link DocumentBuilderFactory}
     *      is created.
     *
     * @throws UnsupportedOperationException When implementation does not
     *   override this method.
     *
     * @since 1.5
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \""
            + this.getClass().getPackage().getSpecificationTitle()
            + "\" version \""
            + this.getClass().getPackage().getSpecificationVersion()
            + "\""
            );

    }

    /**
     * Set the {@link Schema} to be used by parsers created
     * from this factory.
     *
     * <p>
     * When a {@link Schema} is non-null, a parser will use a validator
     * created from it to validate documents before it passes information
     * down to the application.
     *
     * <p>When errors are found by the validator, the parser is responsible
     * to report them to the user-specified {@link org.xml.sax.ErrorHandler}
     * (or if the error handler is not set, ignore them or throw them), just
     * like any other errors found by the parser itself.
     * In other words, if the user-specified {@link org.xml.sax.ErrorHandler}
     * is set, it must receive those errors, and if not, they must be
     * treated according to the implementation specific
     * default error handling rules.
     *
     * <p>
     * A validator may modify the outcome of a parse (for example by
     * adding default values that were missing in documents), and a parser
     * is responsible to make sure that the application will receive
     * modified DOM trees.
     *
     * <p>
     * Initially, null is set as the {@link Schema}.
     *
     * <p>
     * This processing will take effect even if
     * the {@link #isValidating()} method returns {@code false}.
     *
     * <p>It is an error to use
     * the {@code http://java.sun.com/xml/jaxp/properties/schemaSource}
     * property and/or the {@code http://java.sun.com/xml/jaxp/properties/schemaLanguage}
     * property in conjunction with a {@link Schema} object.
     * Such configuration will cause a {@link ParserConfigurationException}
     * exception when the {@link #newDocumentBuilder()} is invoked.
     *
     *
     * <h4>Note for implementors</h4>
     *
     * <p>
     * A parser must be able to work with any {@link Schema}
     * implementation. However, parsers and schemas are allowed
     * to use implementation-specific custom mechanisms
     * as long as they yield the result described in the specification.
     *
     *
     * @param schema {@code Schema} to use or {@code null}
     *   to remove a schema.
     *
     * @throws UnsupportedOperationException When implementation does not
     *   override this method.
     *
     * @since 1.5
     */
    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException(
            "This parser does not support specification \""
            + this.getClass().getPackage().getSpecificationTitle()
            + "\" version \""
            + this.getClass().getPackage().getSpecificationVersion()
            + "\""
            );
    }



    /**
     * Set state of XInclude processing.
     *
     * <p>If XInclude markup is found in the document instance, should it be
     * processed as specified in <a href="http://www.w3.org/TR/xinclude/">
     * XML Inclusions (XInclude) Version 1.0</a>.
     *
     * <p>XInclude processing defaults to {@code false}.
     *
     * @param state Set XInclude processing to {@code true} or
     *   {@code false}
     *
     * @throws UnsupportedOperationException When implementation does not
     *   override this method.
     *
     * @since 1.5
     */
    public void setXIncludeAware(final boolean state) {
        if (state) {
            throw new UnsupportedOperationException(" setXIncludeAware " +
                "is not supported on this JAXP" +
                " implementation or earlier: " + this.getClass());
        }
    }

    /**
     * Get state of XInclude processing.
     *
     * @return current state of XInclude processing
     *
     * @throws UnsupportedOperationException When implementation does not
     *   override this method.
     *
     * @since 1.5
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \""
            + this.getClass().getPackage().getSpecificationTitle()
            + "\" version \""
            + this.getClass().getPackage().getSpecificationVersion()
            + "\""
            );
    }
}
