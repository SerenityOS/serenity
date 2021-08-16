/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.remote;


import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader.Provider;
import java.util.function.Predicate;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorFactory.ConnectorFactory;

/**
 * <p>Factory to create JMX API connector servers.  There
 * are no instances of this class.</p>
 *
 * <p>Each connector server is created by an instance of {@link
 * JMXConnectorServerProvider}.  This instance is found as follows.  Suppose
 * the given {@link JMXServiceURL} looks like
 * <code>"service:jmx:<em>protocol</em>:<em>remainder</em>"</code>.
 * Then the factory will attempt to find the appropriate {@link
 * JMXConnectorServerProvider} for <code><em>protocol</em></code>.  Each
 * occurrence of the character <code>+</code> or <code>-</code> in
 * <code><em>protocol</em></code> is replaced by <code>.</code> or
 * <code>_</code>, respectively.</p>
 *
 * <p>A <em>provider package list</em> is searched for as follows:</p>
 *
 * <ol>
 *
 * <li>If the <code>environment</code> parameter to {@link
 * #newJMXConnectorServer(JMXServiceURL,Map,MBeanServer)
 * newJMXConnectorServer} contains the key
 * <code>jmx.remote.protocol.provider.pkgs</code> then the associated
 * value is the provider package list.
 *
 * <li>Otherwise, if the system property
 * <code>jmx.remote.protocol.provider.pkgs</code> exists, then its value
 * is the provider package list.
 *
 * <li>Otherwise, there is no provider package list.
 *
 * </ol>
 *
 * <p>The provider package list is a string that is interpreted as a
 * list of non-empty Java package names separated by vertical bars
 * (<code>|</code>).  If the string is empty, then so is the provider
 * package list.  If the provider package list is not a String, or if
 * it contains an element that is an empty string, a {@link
 * JMXProviderException} is thrown.</p>
 *
 * <p>If the provider package list exists and is not empty, then for
 * each element <code><em>pkg</em></code> of the list, the factory
 * will attempt to load the class
 *
 * <blockquote>
 * <code><em>pkg</em>.<em>protocol</em>.ServerProvider</code>
 * </blockquote>

 * <p>If the <code>environment</code> parameter to {@link
 * #newJMXConnectorServer(JMXServiceURL, Map, MBeanServer)
 * newJMXConnectorServer} contains the key
 * <code>jmx.remote.protocol.provider.class.loader</code> then the
 * associated value is the class loader to use to load the provider.
 * If the associated value is not an instance of {@link
 * java.lang.ClassLoader}, an {@link
 * java.lang.IllegalArgumentException} is thrown.</p>
 *
 * <p>If the <code>jmx.remote.protocol.provider.class.loader</code>
 * key is not present in the <code>environment</code> parameter, the
 * calling thread's context class loader is used.</p>
 *
 * <p>If the attempt to load this class produces a {@link
 * ClassNotFoundException}, the search for a handler continues with
 * the next element of the list.</p>
 *
 * <p>Otherwise, a problem with the provider found is signalled by a
 * {@link JMXProviderException} whose {@link
 * JMXProviderException#getCause() <em>cause</em>} indicates the
 * underlying exception, as follows:</p>
 *
 * <ul>
 *
 * <li>if the attempt to load the class produces an exception other
 * than <code>ClassNotFoundException</code>, that is the
 * <em>cause</em>;
 *
 * <li>if {@link Class#newInstance()} for the class produces an
 * exception, that is the <em>cause</em>.
 *
 * </ul>
 *
 * <p>If no provider is found by the above steps, including the
 * default case where there is no provider package list, then the
 * implementation will use its own provider for
 * <code><em>protocol</em></code>, or it will throw a
 * <code>MalformedURLException</code> if there is none.  An
 * implementation may choose to find providers by other means.  For
 * example, it may support <a
 * href="{@docRoot}/java.base/java/util/ServiceLoader.html#developing-service-providers">service providers</a>,
 * where the service interface is <code>JMXConnectorServerProvider</code>.</p>
 *
 * <p>Every implementation must support the RMI connector protocol with
 * the default RMI transport, specified with string <code>rmi</code>.
 * </p>
 *
 * <p>Once a provider is found, the result of the
 * <code>newJMXConnectorServer</code> method is the result of calling
 * {@link
 * JMXConnectorServerProvider#newJMXConnectorServer(JMXServiceURL,
 * Map, MBeanServer) newJMXConnectorServer} on the provider.</p>
 *
 * <p>The <code>Map</code> parameter passed to the
 * <code>JMXConnectorServerProvider</code> is a new read-only
 * <code>Map</code> that contains all the entries that were in the
 * <code>environment</code> parameter to {@link
 * #newJMXConnectorServer(JMXServiceURL,Map,MBeanServer)
 * JMXConnectorServerFactory.newJMXConnectorServer}, if there was one.
 * Additionally, if the
 * <code>jmx.remote.protocol.provider.class.loader</code> key is not
 * present in the <code>environment</code> parameter, it is added to
 * the new read-only <code>Map</code>. The associated value is the
 * calling thread's context class loader.</p>
 *
 * @since 1.5
 */
public class JMXConnectorServerFactory {

    /**
     * <p>Name of the attribute that specifies the default class
     * loader.  This class loader is used to deserialize objects in
     * requests received from the client, possibly after consulting an
     * MBean-specific class loader.  The value associated with this
     * attribute is an instance of {@link ClassLoader}.</p>
     */
    public static final String DEFAULT_CLASS_LOADER =
        JMXConnectorFactory.DEFAULT_CLASS_LOADER;

    /**
     * <p>Name of the attribute that specifies the default class
     * loader MBean name.  This class loader is used to deserialize objects in
     * requests received from the client, possibly after consulting an
     * MBean-specific class loader.  The value associated with this
     * attribute is an instance of {@link javax.management.ObjectName
     * ObjectName}.</p>
     */
    public static final String DEFAULT_CLASS_LOADER_NAME =
        "jmx.remote.default.class.loader.name";

    /**
     * <p>Name of the attribute that specifies the provider packages
     * that are consulted when looking for the handler for a protocol.
     * The value associated with this attribute is a string with
     * package names separated by vertical bars (<code>|</code>).</p>
     */
    public static final String PROTOCOL_PROVIDER_PACKAGES =
        "jmx.remote.protocol.provider.pkgs";

    /**
     * <p>Name of the attribute that specifies the class
     * loader for loading protocol providers.
     * The value associated with this attribute is an instance
     * of {@link ClassLoader}.</p>
     */
    public static final String PROTOCOL_PROVIDER_CLASS_LOADER =
        "jmx.remote.protocol.provider.class.loader";

    private static final String PROTOCOL_PROVIDER_DEFAULT_PACKAGE =
        "com.sun.jmx.remote.protocol";

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc","JMXConnectorServerFactory");

    /** There are no instances of this class.  */
    private JMXConnectorServerFactory() {
    }

    private static JMXConnectorServer
        getConnectorServerAsService(ClassLoader loader, JMXServiceURL url,
                                    Map<String, ?> map, MBeanServer mbs,
                                    Predicate<Provider<?>> filter)
        throws IOException {
        final ConnectorFactory<JMXConnectorServerProvider,JMXConnectorServer>
              factory = (p) -> p.newJMXConnectorServer(url, map, mbs);
        return JMXConnectorFactory.getConnectorAsService(
                                     JMXConnectorServerProvider.class,
                                     loader, url, filter, factory);
    }

    /**
     * <p>Creates a connector server at the given address.  The
     * resultant server is not started until its {@link
     * JMXConnectorServer#start() start} method is called.</p>
     *
     * @param serviceURL the address of the new connector server.  The
     * actual address of the new connector server, as returned by its
     * {@link JMXConnectorServer#getAddress() getAddress} method, will
     * not necessarily be exactly the same.  For example, it might
     * include a port number if the original address did not.
     *
     * @param environment a set of attributes to control the new
     * connector server's behavior.  This parameter can be null.
     * Keys in this map must be Strings.  The appropriate type of each
     * associated value depends on the attribute.  The contents of
     * <code>environment</code> are not changed by this call.
     *
     * @param mbeanServer the MBean server that this connector server
     * is attached to.  Null if this connector server will be attached
     * to an MBean server by being registered in it.
     *
     * @return a <code>JMXConnectorServer</code> representing the new
     * connector server.  Each successful call to this method produces
     * a different object.
     *
     * @exception NullPointerException if <code>serviceURL</code> is null.
     *
     * @exception IOException if the connector server cannot be made
     * because of a communication problem.
     *
     * @exception MalformedURLException if there is no provider for the
     * protocol in <code>serviceURL</code>.
     *
     * @exception JMXProviderException if there is a provider for the
     * protocol in <code>serviceURL</code> but it cannot be used for
     * some reason.
     */
    public static JMXConnectorServer
        newJMXConnectorServer(JMXServiceURL serviceURL,
                              Map<String,?> environment,
                              MBeanServer mbeanServer)
            throws IOException {
        Map<String, Object> envcopy;
        if (environment == null)
            envcopy = new HashMap<String, Object>();
        else {
            EnvHelp.checkAttributes(environment);
            envcopy = new HashMap<String, Object>(environment);
        }

        final Class<JMXConnectorServerProvider> targetInterface =
                JMXConnectorServerProvider.class;
        final ClassLoader loader =
            JMXConnectorFactory.resolveClassLoader(envcopy);
        final String protocol = serviceURL.getProtocol();
        final String providerClassName = "ServerProvider";

        JMXConnectorServerProvider provider =
            JMXConnectorFactory.getProvider(serviceURL,
                                            envcopy,
                                            providerClassName,
                                            targetInterface,
                                            loader);

        IOException exception = null;
        JMXConnectorServer connection = null;
        if (provider == null) {
            Predicate<Provider<?>> systemProvider =
                    JMXConnectorFactory::isSystemProvider;
            // Loader is null when context class loader is set to null
            // and no loader has been provided in map.
            // com.sun.jmx.remote.util.Service class extracted from j2se
            // provider search algorithm doesn't handle well null classloader.
            if (loader != null) {
                try {
                    connection =
                        getConnectorServerAsService(loader,
                                                    serviceURL,
                                                    envcopy,
                                                    mbeanServer,
                                                    systemProvider.negate());
                    if (connection != null)
                        return connection;
                } catch (JMXProviderException e) {
                    throw e;
                } catch (IOException e) {
                    exception = e;
                }
            }
            connection = getConnectorServerAsService(
                            JMXConnectorFactory.class.getClassLoader(),
                            serviceURL,
                            Collections.unmodifiableMap(envcopy),
                            mbeanServer,
                            systemProvider);
            if (connection != null) return connection;
        }

        if (provider == null) {
            MalformedURLException e =
                new MalformedURLException("Unsupported protocol: " + protocol);
            if (exception == null) {
                throw e;
            } else {
                throw EnvHelp.initCause(e, exception);
            }
        }

        envcopy = Collections.unmodifiableMap(envcopy);

        return provider.newJMXConnectorServer(serviceURL,
                                              envcopy,
                                              mbeanServer);
    }
}
