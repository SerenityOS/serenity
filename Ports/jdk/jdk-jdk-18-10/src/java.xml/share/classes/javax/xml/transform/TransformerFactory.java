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

package javax.xml.transform;

import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;

/**
 * <p>A TransformerFactory instance can be used to create
 * {@link javax.xml.transform.Transformer} and
 * {@link javax.xml.transform.Templates} objects.
 *
 * <p>The system property that determines which Factory implementation
 * to create is named {@code "javax.xml.transform.TransformerFactory"}.
 * This property names a concrete subclass of the
 * {@code TransformerFactory} abstract class. If the property is not
 * defined, a platform default is be used.
 *
 * @author Jeff Suttor
 * @author Neeraj Bajaj
 *
 * @since 1.5
 */
public abstract class TransformerFactory {

    /**
     * Default constructor is protected on purpose.
     */
    protected TransformerFactory() { }



    /**
     * Creates a new instance of the {@code TransformerFactory} builtin
     * system-default implementation.
     *
     * @return A new instance of the {@code TransformerFactory} builtin
     *         system-default implementation.
     *
     * @since 9
     */
    public static TransformerFactory newDefaultInstance() {
        return new TransformerFactoryImpl();
    }

    /**
     * Obtains a new instance of a {@code TransformerFactory}. This method uses the
     * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
     * to determine the {@code TransformerFactory} implementation class to load.
     * <p>
     * Once an application has obtained a reference to a
     * {@code TransformerFactory}, it can use the factory to configure
     * and obtain transformer instances.
     *
     * @return new TransformerFactory instance, never null.
     *
     * @throws TransformerFactoryConfigurationError Thrown in case of {@linkplain
     * java.util.ServiceConfigurationError service configuration error} or if
     * the implementation is not available or cannot be instantiated.
     */
    public static TransformerFactory newInstance()
        throws TransformerFactoryConfigurationError {

        return FactoryFinder.find(
            /* The default property name according to the JAXP spec */
            TransformerFactory.class,
            /* The fallback implementation class name, XSLTC */
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    }

    /**
     * Obtain a new instance of a {@code TransformerFactory} from factory class name.
     * This function is useful when there are multiple providers in the classpath.
     * It gives more control to the application as it can specify which provider
     * should be loaded.
     *
     * <p>Once an application has obtained a reference to a
     * {@code TransformerFactory} it can use the factory to configure
     * and obtain transformer instances.
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
     * @param factoryClassName fully qualified factory class name that provides implementation of {@code javax.xml.transform.TransformerFactory}.
     *
     * @param classLoader {@code ClassLoader} used to load the factory class. If {@code null}
     *                     current {@code Thread}'s context classLoader is used to load the factory class.
     *
     * @return new TransformerFactory instance, never null.
     *
     * @throws TransformerFactoryConfigurationError
     *                    if {@code factoryClassName} is {@code null}, or
     *                   the factory class cannot be loaded, instantiated.
     *
     * @see #newInstance()
     *
     * @since 1.6
     */
    public static TransformerFactory newInstance(String factoryClassName, ClassLoader classLoader)
        throws TransformerFactoryConfigurationError{

        //do not fallback if given classloader can't find the class, throw exception
        return  FactoryFinder.newInstance(TransformerFactory.class,
                    factoryClassName, classLoader, false);
    }
    /**
     * Process the {@code Source} into a {@code Transformer}
     * {@code Object}.  The {@code Source} is an XSLT document that
     * conforms to <a href="http://www.w3.org/TR/xslt">
     * XSL Transformations (XSLT) Version 1.0</a>.  Care must
     * be taken not to use this {@code Transformer} in multiple
     * {@code Thread}s running concurrently.
     * Different {@code TransformerFactories} can be used concurrently by
     * different {@code Thread}s.
     *
     * @param source {@code Source } of XSLT document used to create
     *   {@code Transformer}.
     *   Examples of XML {@code Source}s include
     *   {@link javax.xml.transform.dom.DOMSource DOMSource},
     *   {@link javax.xml.transform.sax.SAXSource SAXSource}, and
     *   {@link javax.xml.transform.stream.StreamSource StreamSource}.
     *
     * @return A {@code Transformer} object that may be used to perform
     *   a transformation in a single {@code Thread}, never
     *   {@code null}.
     *
     * @throws TransformerConfigurationException Thrown if there are errors when
     *    parsing the {@code Source} or it is not possible to create a
     *   {@code Transformer} instance.
     *
     * @see <a href="http://www.w3.org/TR/xslt">
     *   XSL Transformations (XSLT) Version 1.0</a>
     */
    public abstract Transformer newTransformer(Source source)
        throws TransformerConfigurationException;

    /**
     * Create a new {@code Transformer} that performs a copy
     * of the {@code Source} to the {@code Result},
     * i.e. the "<em>identity transform</em>".
     *
     * @return A Transformer object that may be used to perform a transformation
     * in a single thread, never null.
     *
     * @throws TransformerConfigurationException When it is not
     *   possible to create a {@code Transformer} instance.
     */
    public abstract Transformer newTransformer()
        throws TransformerConfigurationException;

    /**
     * Process the Source into a Templates object, which is a
     * a compiled representation of the source. This Templates object
     * may then be used concurrently across multiple threads.  Creating
     * a Templates object allows the TransformerFactory to do detailed
     * performance optimization of transformation instructions, without
     * penalizing runtime transformation.
     *
     * @param source An object that holds a URL, input stream, etc.
     *
     * @return A Templates object capable of being used for transformation
     *   purposes, never {@code null}.
     *
     * @throws TransformerConfigurationException When parsing to
     *   construct the Templates object fails.
     */
    public abstract Templates newTemplates(Source source)
        throws TransformerConfigurationException;

    /**
     * Get the stylesheet specification(s) associated with the
     * XML {@code Source} document via the
     * <a href="http://www.w3.org/TR/xml-stylesheet/">
     * xml-stylesheet processing instruction</a> that match the given criteria.
     * Note that it is possible to return several stylesheets, in which case
     * they are applied as if they were a list of imports or cascades in a
     * single stylesheet.
     *
     * @param source The XML source document.
     * @param media The media attribute to be matched.  May be null, in which
     *      case the prefered templates will be used (i.e. alternate = no).
     * @param title The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     *
     * @return A {@code Source} {@code Object} suitable for passing
     *   to the {@code TransformerFactory}.
     *
     * @throws TransformerConfigurationException An {@code Exception}
     *   is thrown if an error occurings during parsing of the
     *   {@code source}.
     *
     * @see <a href="http://www.w3.org/TR/xml-stylesheet/">
     *   Associating Style Sheets with XML documents Version 1.0</a>
     */
    public abstract Source getAssociatedStylesheet(
        Source source,
        String media,
        String title,
        String charset)
        throws TransformerConfigurationException;

    /**
     * Set an object that is used by default during the transformation
     * to resolve URIs used in document(), xsl:import, or xsl:include.
     *
     * @param resolver An object that implements the URIResolver interface,
     * or null.
     */
    public abstract void setURIResolver(URIResolver resolver);

    /**
     * Get the object that is used by default during the transformation
     * to resolve URIs used in document(), xsl:import, or xsl:include.
     *
     * @return The URIResolver that was set with setURIResolver.
     */
    public abstract URIResolver getURIResolver();

    //======= CONFIGURATION METHODS =======

        /**
         * <p>Set a feature for this {@code TransformerFactory} and {@code Transformer}s
         * or {@code Template}s created by this factory.
         *
         * <p>
         * Feature names are fully qualified {@link java.net.URI}s.
         * Implementations may define their own features.
         * An {@link TransformerConfigurationException} is thrown if this {@code TransformerFactory} or the
         * {@code Transformer}s or {@code Template}s it creates cannot support the feature.
         * It is possible for an {@code TransformerFactory} to expose a feature value but be unable to change its state.
         *
         * <p>All implementations are required to support the {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature.
         * When the feature is:
         * <ul>
         *   <li>
         *     {@code true}: the implementation will limit XML processing to conform to implementation limits
         *     and behave in a secure fashion as defined by the implementation.
         *     Examples include resolving user defined style sheets and functions.
         *     If XML processing is limited for security reasons, it will be reported via a call to the registered
         *     {@link ErrorListener#fatalError(TransformerException exception)}.
         *     See {@link  #setErrorListener(ErrorListener listener)}.
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
         * @throws TransformerConfigurationException if this {@code TransformerFactory}
         *   or the {@code Transformer}s or {@code Template}s it creates cannot support this feature.
     * @throws NullPointerException If the {@code name} parameter is null.
         */
        public abstract void setFeature(String name, boolean value)
                throws TransformerConfigurationException;

    /**
     * Look up the value of a feature.
     *
         * <p>
         * Feature names are fully qualified {@link java.net.URI}s.
         * Implementations may define their own features.
         * {@code false} is returned if this {@code TransformerFactory} or the
         * {@code Transformer}s or {@code Template}s it creates cannot support the feature.
         * It is possible for an {@code TransformerFactory} to expose a feature value but be unable to change its state.
         *
         * @param name Feature name.
         *
     * @return The current state of the feature, {@code true} or {@code false}.
     *
     * @throws NullPointerException If the {@code name} parameter is null.
     */
    public abstract boolean getFeature(String name);

    /**
     * Allows the user to set specific attributes on the underlying
     * implementation.  An attribute in this context is defined to
     * be an option that the implementation provides.
     * An {@code IllegalArgumentException} is thrown if the underlying
     * implementation doesn't recognize the attribute.
     * <p>
     * All implementations that implement JAXP 1.5 or newer are required to
     * support the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD}  and
     * {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_STYLESHEET} properties.
     *
     * <ul>
     *   <li>
     *      <p>
     *      Access to external DTDs in the source file is restricted to the protocols
     *      specified by the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property.
     *      If access is denied during transformation due to the restriction of this property,
     *      {@link javax.xml.transform.TransformerException} will be thrown by
     *      {@link javax.xml.transform.Transformer#transform(Source, Result)}.
     *
     *      <p>
     *      Access to external DTDs in the stylesheet is restricted to the protocols
     *      specified by the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property.
     *      If access is denied during the creation of a new transformer due to the
     *      restriction of this property,
     *      {@link javax.xml.transform.TransformerConfigurationException} will be thrown
     *      by the {@link #newTransformer(Source)} method.
     *
     *      <p>
     *      Access to external reference set by the stylesheet processing instruction,
     *      Import and Include element is restricted to the protocols specified by the
     *      {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_STYLESHEET} property.
     *      If access is denied during the creation of a new transformer due to the
     *      restriction of this property,
     *      {@link javax.xml.transform.TransformerConfigurationException} will be thrown
     *      by the {@link #newTransformer(Source)} method.
     *
     *      <p>
     *      Access to external document through XSLT document function is restricted
     *      to the protocols specified by the property. If access is denied during
     *      the transformation due to the restriction of this property,
     *      {@link javax.xml.transform.TransformerException} will be thrown by the
     *      {@link javax.xml.transform.Transformer#transform(Source, Result)} method.
     *
     *   </li>
     * </ul>
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     *
     * @throws IllegalArgumentException When implementation does not
     *   recognize the attribute.
     */
    public abstract void setAttribute(String name, Object value);

    /**
     * Allows the user to retrieve specific attributes on the underlying
     * implementation.
     * An {@code IllegalArgumentException} is thrown if the underlying
     * implementation doesn't recognize the attribute.
     *
     * @param name The name of the attribute.
     *
     * @return value The value of the attribute.
     *
     * @throws IllegalArgumentException When implementation does not
     *   recognize the attribute.
     */
    public abstract Object getAttribute(String name);

    /**
     * Set the error event listener for the TransformerFactory, which
     * is used for the processing of transformation instructions,
     * and not for the transformation itself.
     * An {@code IllegalArgumentException} is thrown if the
     * {@code ErrorListener} listener is {@code null}.
     *
     * @param listener The new error listener.
     *
     * @throws IllegalArgumentException When {@code listener} is
     *   {@code null}
     */
    public abstract void setErrorListener(ErrorListener listener);

    /**
     * Get the error event handler for the TransformerFactory.
     *
     * @return The current error handler, which should never be null.
     */
    public abstract ErrorListener getErrorListener();

}
