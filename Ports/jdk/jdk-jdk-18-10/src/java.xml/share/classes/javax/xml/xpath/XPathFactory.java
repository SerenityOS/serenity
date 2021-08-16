/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import jdk.xml.internal.SecuritySupport;

/**
 * <p>An {@code XPathFactory} instance can be used to create
 * {@link javax.xml.xpath.XPath} objects.</p>
 *
 *<p>See {@link #newInstance(String uri)} for lookup mechanism.</p>
 *
 * <p>The {@link XPathFactory} class is not thread-safe. In other words,
 * it is the application's responsibility to ensure that at most
 * one thread is using a {@link XPathFactory} object at any
 * given moment. Implementations are encouraged to mark methods
 * as <code>synchronized</code> to protect themselves from broken clients.
 *
 * <p>{@link XPathFactory} is not re-entrant. While one of the
 * <code>newInstance</code> methods is being invoked, applications
 * may not attempt to recursively invoke a <code>newInstance</code> method,
 * even from the same thread.
 *
 * @author  Norman Walsh
 * @author  Jeff Suttor
 *
 * @since 1.5
 */
public abstract class XPathFactory {


    /**
     * <p>The default property name according to the JAXP spec.</p>
     */
    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";

    /**
     * <p>Default Object Model URI.</p>
     */
    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";

    /**
     * <p>Protected constructor as {@link #newInstance()} or {@link #newInstance(String uri)}
     * or {@link #newInstance(String uri, String factoryClassName, ClassLoader classLoader)}
     * should be used to create a new instance of an {@code XPathFactory}.</p>
     */
    protected XPathFactory() {
    }

    /**
     * Creates a new instance of the {@code XPathFactory} builtin
     * system-default implementation.
     *
     * @implSpec The {@code XPathFactory} builtin
     * system-default implementation is only required to support the
     * {@link #DEFAULT_OBJECT_MODEL_URI default object model}, the
     * {@linkplain org.w3c.dom W3C DOM}, but may support additional
     * object models.
     *
     * @return A new instance of the {@code XPathFactory} builtin
     *         system-default implementation.
     *
     * @since 9
     */
    public static XPathFactory newDefaultInstance() {
        return new XPathFactoryImpl();
    }

    /**
     * <p>Get a new {@code XPathFactory} instance using the default object model,
     * {@link #DEFAULT_OBJECT_MODEL_URI},
     * the W3C DOM.</p>
     *
     * <p>This method is functionally equivalent to:</p>
     * <pre>
     *   newInstance(DEFAULT_OBJECT_MODEL_URI)
     * </pre>
     *
     * <p>Since the implementation for the W3C DOM is always available, this method will never fail.</p>
     *
     * @return Instance of an {@code XPathFactory}.
     *
     * @throws RuntimeException When there is a failure in creating an
     *   {@code XPathFactory} for the default object model.
     */
    public static XPathFactory newInstance() {

        try {
            return newInstance(DEFAULT_OBJECT_MODEL_URI);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(
                    "XPathFactory#newInstance() failed to create an XPathFactory for the default object model: "
                    + DEFAULT_OBJECT_MODEL_URI
                    + " with the XPathFactoryConfigurationException: "
                    + e.getMessage(), e
            );
        }
    }

    /**
    * Obtains a new {@code XPathFactory} instance using the specified object model.
    * This method uses the
    * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
    * to determine and load the {@code XPathFactory} implementation that supports
    * the specified object model.
    *
    * <p>Tip for Trouble-shooting:
    * <p>See {@link java.util.Properties#load(java.io.InputStream)} for exactly how a property file is parsed.
    * In particular, colons ':' need to be escaped in a property file, so make sure the URIs are properly escaped in it.
    * For example:
    * <pre>
    *   http\://java.sun.com/jaxp/xpath/dom=org.acme.DomXPathFactory
    * </pre>
    *
    * @param uri Identifies the underlying object model.
    *   The specification only defines the URI {@link #DEFAULT_OBJECT_MODEL_URI},
    *   <code>http://java.sun.com/jaxp/xpath/dom</code> for the W3C DOM,
    *   the org.w3c.dom package, and implementations are free to introduce other URIs for other object models.
    *
    * @return Instance of an {@code XPathFactory}.
    *
    * @throws XPathFactoryConfigurationException If the specified object model
    *      is unavailable, or if there is a configuration error.
    * @throws NullPointerException If <code>uri</code> is <code>null</code>.
    * @throws IllegalArgumentException If <code>uri</code> is <code>null</code>
    *   or <code>uri.length() == 0</code>.
    */
    public static XPathFactory newInstance(final String uri)
        throws XPathFactoryConfigurationException {

        if (uri == null) {
            throw new NullPointerException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }

        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }

        ClassLoader classLoader = SecuritySupport.getContextClassLoader();

        if (classLoader == null) {
            //use the current class loader
            classLoader = XPathFactory.class.getClassLoader();
        }

        XPathFactory xpathFactory = new XPathFactoryFinder(classLoader).newFactory(uri);

        if (xpathFactory == null) {
            throw new XPathFactoryConfigurationException(
                    "No XPathFactory implementation found for the object model: "
                    + uri);
        }

        return xpathFactory;
    }

    /**
     * <p>Obtain a new instance of a {@code XPathFactory} from a factory class name. {@code XPathFactory}
     * is returned if specified factory class supports the specified object model.
     * This function is useful when there are multiple providers in the classpath.
     * It gives more control to the application as it can specify which provider
     * should be loaded.</p>
     *
     *
     * <h4>Tip for Trouble-shooting</h4>
     * <p>Setting the <code>jaxp.debug</code> system property will cause
     * this method to print a lot of debug messages
     * to <code>System.err</code> about what it is doing and where it is looking at.</p>
     *
     * <p> If you have problems try:</p>
     * <pre>
     * java -Djaxp.debug=1 YourProgram ....
     * </pre>
     *
     * @param uri         Identifies the underlying object model. The specification only defines the URI
     *                    {@link #DEFAULT_OBJECT_MODEL_URI},<code>http://java.sun.com/jaxp/xpath/dom</code>
     *                    for the W3C DOM, the org.w3c.dom package, and implementations are free to introduce
     *                    other URIs for other object models.
     *
     * @param factoryClassName fully qualified factory class name that provides implementation of <code>javax.xml.xpath.XPathFactory</code>.
     *
     * @param classLoader <code>ClassLoader</code> used to load the factory class. If <code>null</code>
     *                     current <code>Thread</code>'s context classLoader is used to load the factory class.
     *
     *
     * @return New instance of a {@code XPathFactory}
     *
     * @throws XPathFactoryConfigurationException
     *                   if <code>factoryClassName</code> is <code>null</code>, or
     *                   the factory class cannot be loaded, instantiated
     *                   or the factory class does not support the object model specified
     *                   in the <code>uri</code> parameter.
     *
     * @throws NullPointerException If <code>uri</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>uri</code> is <code>null</code>
     *          or <code>uri.length() == 0</code>.
     *
     * @see #newInstance()
     * @see #newInstance(String uri)
     *
     * @since 1.6
     */
    public static XPathFactory newInstance(String uri, String factoryClassName, ClassLoader classLoader)
        throws XPathFactoryConfigurationException{
        ClassLoader cl = classLoader;

        if (uri == null) {
            throw new NullPointerException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }

        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                    "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }

        if (cl == null) {
            cl = SecuritySupport.getContextClassLoader();
        }

        XPathFactory f = new XPathFactoryFinder(cl).createInstance(factoryClassName);

        if (f == null) {
            throw new XPathFactoryConfigurationException(
                    "No XPathFactory implementation found for the object model: "
                    + uri);
        }
        //if this factory supports the given schemalanguage return this factory else thrown exception
        if (f.isObjectModelSupported(uri)) {
            return f;
        } else {
            throw new XPathFactoryConfigurationException("Factory "
                    + factoryClassName + " doesn't support given " + uri
                    + " object model");
        }

    }

    /**
     * <p>Is specified object model supported by this {@code XPathFactory}?</p>
     *
     * @param objectModel Specifies the object model which the returned {@code XPathFactory} will understand.
     *
     * @return <code>true</code> if {@code XPathFactory} supports <code>objectModel</code>, else <code>false</code>.
     *
     * @throws NullPointerException If <code>objectModel</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>objectModel.length() == 0</code>.
     */
    public abstract boolean isObjectModelSupported(String objectModel);

    /**
     * <p>Set a feature for this {@code XPathFactory} and
     * <code>XPath</code>s created by this factory.</p>
     *
     * <p>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link XPathFactoryConfigurationException} is thrown if this
     * {@code XPathFactory} or the <code>XPath</code>s
     * it creates cannot support the feature.
     * It is possible for an {@code XPathFactory} to expose a feature value
     * but be unable to change its state.
     * </p>
     *
     * <p>
     * All implementations are required to support the {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature.
     * When the feature is <code>true</code>, any reference to  an external function is an error.
     * Under these conditions, the implementation must not call the {@link XPathFunctionResolver}
     * and must throw an {@link XPathFunctionException}.
     * </p>
     *
     * @param name Feature name.
     * @param value Is feature state <code>true</code> or <code>false</code>.
     *
     * @throws XPathFactoryConfigurationException if this {@code XPathFactory} or the <code>XPath</code>s
     *   it creates cannot support this feature.
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     */
    public abstract void setFeature(String name, boolean value)
            throws XPathFactoryConfigurationException;

    /**
     * <p>Get the state of the named feature.</p>
     *
     * <p>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link XPathFactoryConfigurationException} is thrown if this
     * {@code XPathFactory} or the <code>XPath</code>s
     * it creates cannot support the feature.
     * It is possible for an {@code XPathFactory} to expose a feature value
     * but be unable to change its state.
     * </p>
     *
     * @param name Feature name.
     *
     * @return State of the named feature.
     *
     * @throws XPathFactoryConfigurationException if this
     *   {@code XPathFactory} or the <code>XPath</code>s
     *   it creates cannot support this feature.
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     */
    public abstract boolean getFeature(String name)
            throws XPathFactoryConfigurationException;

    /**
     * <p>Establish a default variable resolver.</p>
     *
     * <p>Any <code>XPath</code> objects constructed from this factory will use
     * the specified resolver by default.</p>
     *
     * <p>A <code>NullPointerException</code> is thrown if <code>resolver</code>
     * is <code>null</code>.</p>
     *
     * @param resolver Variable resolver.
     *
     * @throws NullPointerException If <code>resolver</code> is
     *   <code>null</code>.
     */
    public abstract void setXPathVariableResolver(XPathVariableResolver resolver);

    /**
     * <p>Establish a default function resolver.</p>
     *
     * <p>Any <code>XPath</code> objects constructed from this factory will
     * use the specified resolver by default.</p>
     *
     * <p>A <code>NullPointerException</code> is thrown if
     * <code>resolver</code> is <code>null</code>.</p>
     *
     * @param resolver XPath function resolver.
     *
     * @throws NullPointerException If <code>resolver</code> is
     *   <code>null</code>.
     */
    public abstract void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /**
    * <p>Return a new <code>XPath</code> using the underlying object
    * model determined when the {@code XPathFactory} was instantiated.</p>
    *
    * @return New instance of an <code>XPath</code>.
    */
    public abstract XPath newXPath();
}
