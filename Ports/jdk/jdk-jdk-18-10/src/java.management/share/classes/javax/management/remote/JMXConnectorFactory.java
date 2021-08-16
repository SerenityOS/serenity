/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jmx.mbeanserver.Util;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;
import sun.reflect.misc.ReflectUtil;


/**
 * <p>Factory to create JMX API connector clients.  There
 * are no instances of this class.</p>
 *
 * <p>Connections are usually made using the {@link
 * #connect(JMXServiceURL) connect} method of this class.  More
 * advanced applications can separate the creation of the connector
 * client, using {@link #newJMXConnector(JMXServiceURL, Map)
 * newJMXConnector} and the establishment of the connection itself, using
 * {@link JMXConnector#connect(Map)}.</p>
 *
 * <p>Each client is created by an instance of {@link
 * JMXConnectorProvider}.  This instance is found as follows.  Suppose
 * the given {@link JMXServiceURL} looks like
 * <code>"service:jmx:<em>protocol</em>:<em>remainder</em>"</code>.
 * Then the factory will attempt to find the appropriate {@link
 * JMXConnectorProvider} for <code><em>protocol</em></code>.  Each
 * occurrence of the character <code>+</code> or <code>-</code> in
 * <code><em>protocol</em></code> is replaced by <code>.</code> or
 * <code>_</code>, respectively.</p>
 *
 * <p>A <em>provider package list</em> is searched for as follows:</p>
 *
 * <ol>
 *
 * <li>If the <code>environment</code> parameter to {@link
 * #newJMXConnector(JMXServiceURL, Map) newJMXConnector} contains the
 * key <code>jmx.remote.protocol.provider.pkgs</code> then the
 * associated value is the provider package list.
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
 * <code><em>pkg</em>.<em>protocol</em>.ClientProvider</code>
 * </blockquote>

 * <p>If the <code>environment</code> parameter to {@link
 * #newJMXConnector(JMXServiceURL, Map) newJMXConnector} contains the
 * key <code>jmx.remote.protocol.provider.class.loader</code> then the
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
 * JMXProviderException#getCause() <em>cause</em>} indicates the underlying
 * exception, as follows:</p>
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
 * where the service interface is <code>JMXConnectorProvider</code>.</p>
 *
 * <p>Every implementation must support the RMI connector protocol with
 * the default RMI transport, specified with string <code>rmi</code>.
 * </p>
 *
 * <p>Once a provider is found, the result of the
 * <code>newJMXConnector</code> method is the result of calling {@link
 * JMXConnectorProvider#newJMXConnector(JMXServiceURL,Map) newJMXConnector}
 * on the provider.</p>
 *
 * <p>The <code>Map</code> parameter passed to the
 * <code>JMXConnectorProvider</code> is a new read-only
 * <code>Map</code> that contains all the entries that were in the
 * <code>environment</code> parameter to {@link
 * #newJMXConnector(JMXServiceURL,Map)
 * JMXConnectorFactory.newJMXConnector}, if there was one.
 * Additionally, if the
 * <code>jmx.remote.protocol.provider.class.loader</code> key is not
 * present in the <code>environment</code> parameter, it is added to
 * the new read-only <code>Map</code>.  The associated value is the
 * calling thread's context class loader.</p>
 *
 * @since 1.5
 */
public class JMXConnectorFactory {

    /**
     * <p>Name of the attribute that specifies the default class
     * loader. This class loader is used to deserialize return values and
     * exceptions from remote <code>MBeanServerConnection</code>
     * calls.  The value associated with this attribute is an instance
     * of {@link ClassLoader}.</p>
     */
    public static final String DEFAULT_CLASS_LOADER =
        "jmx.remote.default.class.loader";

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
        new ClassLogger("javax.management.remote.misc", "JMXConnectorFactory");

    /** There are no instances of this class.  */
    private JMXConnectorFactory() {
    }

    /**
     * <p>Creates a connection to the connector server at the given
     * address.</p>
     *
     * <p>This method is equivalent to {@link
     * #connect(JMXServiceURL,Map) connect(serviceURL, null)}.</p>
     *
     * @param serviceURL the address of the connector server to
     * connect to.
     *
     * @return a <code>JMXConnector</code> whose {@link
     * JMXConnector#connect connect} method has been called.
     *
     * @exception NullPointerException if <code>serviceURL</code> is null.
     *
     * @exception IOException if the connector client or the
     * connection cannot be made because of a communication problem.
     *
     * @exception SecurityException if the connection cannot be made
     * for security reasons.
     */
    public static JMXConnector connect(JMXServiceURL serviceURL)
            throws IOException {
        return connect(serviceURL, null);
    }

    /**
     * <p>Creates a connection to the connector server at the given
     * address.</p>
     *
     * <p>This method is equivalent to:</p>
     *
     * <pre>
     * JMXConnector conn = JMXConnectorFactory.newJMXConnector(serviceURL,
     *                                                         environment);
     * conn.connect(environment);
     * </pre>
     *
     * @param serviceURL the address of the connector server to connect to.
     *
     * @param environment a set of attributes to determine how the
     * connection is made.  This parameter can be null.  Keys in this
     * map must be Strings.  The appropriate type of each associated
     * value depends on the attribute.  The contents of
     * <code>environment</code> are not changed by this call.
     *
     * @return a <code>JMXConnector</code> representing the newly-made
     * connection.  Each successful call to this method produces a
     * different object.
     *
     * @exception NullPointerException if <code>serviceURL</code> is null.
     *
     * @exception IOException if the connector client or the
     * connection cannot be made because of a communication problem.
     *
     * @exception SecurityException if the connection cannot be made
     * for security reasons.
     */
    public static JMXConnector connect(JMXServiceURL serviceURL,
                                       Map<String,?> environment)
            throws IOException {
        if (serviceURL == null)
            throw new NullPointerException("Null JMXServiceURL");
        JMXConnector conn = newJMXConnector(serviceURL, environment);
        conn.connect(environment);
        return conn;
    }

    private static <K,V> Map<K,V> newHashMap() {
        return new HashMap<K,V>();
    }

    private static <K> Map<K,Object> newHashMap(Map<K,?> map) {
        return new HashMap<K,Object>(map);
    }

    /**
     * <p>Creates a connector client for the connector server at the
     * given address.  The resultant client is not connected until its
     * {@link JMXConnector#connect(Map) connect} method is called.</p>
     *
     * @param serviceURL the address of the connector server to connect to.
     *
     * @param environment a set of attributes to determine how the
     * connection is made.  This parameter can be null.  Keys in this
     * map must be Strings.  The appropriate type of each associated
     * value depends on the attribute.  The contents of
     * <code>environment</code> are not changed by this call.
     *
     * @return a <code>JMXConnector</code> representing the new
     * connector client.  Each successful call to this method produces
     * a different object.
     *
     * @exception NullPointerException if <code>serviceURL</code> is null.
     *
     * @exception IOException if the connector client cannot be made
     * because of a communication problem.
     *
     * @exception MalformedURLException if there is no provider for the
     * protocol in <code>serviceURL</code>.
     *
     * @exception JMXProviderException if there is a provider for the
     * protocol in <code>serviceURL</code> but it cannot be used for
     * some reason.
     */
    public static JMXConnector newJMXConnector(JMXServiceURL serviceURL,
                                               Map<String,?> environment)
            throws IOException {

        final Map<String,Object> envcopy;
        if (environment == null)
            envcopy = newHashMap();
        else {
            EnvHelp.checkAttributes(environment);
            envcopy = newHashMap(environment);
        }

        final ClassLoader loader = resolveClassLoader(envcopy);
        final Class<JMXConnectorProvider> targetInterface =
                JMXConnectorProvider.class;
        final String protocol = serviceURL.getProtocol();
        final String providerClassName = "ClientProvider";
        final JMXServiceURL providerURL = serviceURL;

        JMXConnectorProvider provider = getProvider(providerURL, envcopy,
                                               providerClassName,
                                               targetInterface,
                                               loader);

        IOException exception = null;
        if (provider == null) {
            Predicate<Provider<?>> systemProvider =
                    JMXConnectorFactory::isSystemProvider;
            // Loader is null when context class loader is set to null
            // and no loader has been provided in map.
            // com.sun.jmx.remote.util.Service class extracted from j2se
            // provider search algorithm doesn't handle well null classloader.
            JMXConnector connection = null;
            if (loader != null) {
                try {
                    connection = getConnectorAsService(loader,
                                                       providerURL,
                                                       envcopy,
                                                       systemProvider.negate());
                    if (connection != null) return connection;
                } catch (JMXProviderException e) {
                    throw e;
                } catch (IOException e) {
                    exception = e;
                }
            }
            connection = getConnectorAsService(
                             JMXConnectorFactory.class.getClassLoader(),
                             providerURL,
                             Collections.unmodifiableMap(envcopy),
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

        final Map<String,Object> fixedenv =
                Collections.unmodifiableMap(envcopy);

        return provider.newJMXConnector(serviceURL, fixedenv);
    }

    @SuppressWarnings("removal")
    private static String resolvePkgs(Map<String, ?> env)
            throws JMXProviderException {

        Object pkgsObject = null;

        if (env != null)
            pkgsObject = env.get(PROTOCOL_PROVIDER_PACKAGES);

        if (pkgsObject == null)
            pkgsObject =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(PROTOCOL_PROVIDER_PACKAGES);
                    }
                });

        if (pkgsObject == null)
            return null;

        if (!(pkgsObject instanceof String)) {
            final String msg = "Value of " + PROTOCOL_PROVIDER_PACKAGES +
                " parameter is not a String: " +
                pkgsObject.getClass().getName();
            throw new JMXProviderException(msg);
        }

        final String pkgs = (String) pkgsObject;
        if (pkgs.trim().isEmpty())
            return null;

        // pkgs may not contain an empty element
        if (pkgs.startsWith("|") || pkgs.endsWith("|") ||
            pkgs.indexOf("||") >= 0) {
            final String msg = "Value of " + PROTOCOL_PROVIDER_PACKAGES +
                " contains an empty element: " + pkgs;
            throw new JMXProviderException(msg);
        }

        return pkgs;
    }

    static <T> T getProvider(JMXServiceURL serviceURL,
                             final Map<String, Object> environment,
                             String providerClassName,
                             Class<T> targetInterface,
                             final ClassLoader loader)
            throws IOException {

        final String protocol = serviceURL.getProtocol();

        final String pkgs = resolvePkgs(environment);

        T instance = null;

        if (pkgs != null) {
            instance =
                getProvider(protocol, pkgs, loader, providerClassName,
                            targetInterface);

            if (instance != null) {
                boolean needsWrap = (loader != instance.getClass().getClassLoader());
                environment.put(PROTOCOL_PROVIDER_CLASS_LOADER, needsWrap ? wrap(loader) : loader);
            }
        }

        return instance;
    }

    @SuppressWarnings("removal")
    private static ClassLoader wrap(final ClassLoader parent) {
        return parent != null ? AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return new ClassLoader(parent) {
                    @Override
                    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                        ReflectUtil.checkPackageAccess(name);
                        return super.loadClass(name, resolve);
                    }
                };
            }
        }) : null;
    }

    /**
     * Checks whether the given provider is our system provider for
     * the RMI connector.
     * If providers for additional protocols are added in the future
     * then the name of their modules may need to be added here.
     * System providers will be loaded only if no other provider is found.
     * @param provider the provider to test.
     * @return true if this provider is a default system provider.
     */
    static boolean isSystemProvider(Provider<?> provider) {
        Module providerModule = provider.type().getModule();
        return providerModule.isNamed()
           && providerModule.getName().equals("java.management.rmi");
    }

    /**
     * Creates a JMXConnector from the first JMXConnectorProvider service
     * supporting the given url that can be loaded from the given loader.
     * <p>
     * Parses the list of JMXConnectorProvider services that can be loaded
     * from the given loader, only retaining those that satisfy the given filter.
     * Then for each provider, attempts to create a new JMXConnector.
     * The first JMXConnector successfully created is returned.
     * <p>
     * The filter predicate is usually used to either exclude system providers
     * or only retain system providers (see isSystemProvider(...) above).
     *
     * @param loader The ClassLoader to use when looking up an implementation
     *        of the service. If null, then only installed services will be
     *        considered.
     *
     * @param url The JMXServiceURL of the connector for which a provider is
     *        requested.
     *
     * @param filter A filter used to exclude or return provider
     *        implementations. Typically the filter will either exclude
     *        system services (system default implementations) or only
     *        retain those.
     *        This can allow to first look for custom implementations (e.g.
     *        deployed on the CLASSPATH with META-INF/services) and
     *        then only default to system implementations.
     *
     * @throws IOException if no connector could not be instantiated, and
     *         at least one provider threw an exception that wasn't a
     *         {@code MalformedURLException} or a {@code JMProviderException}.
     *
     * @throws JMXProviderException if a provider for the protocol in
     *         <code>url</code> was found, but couldn't create the connector
     *         some reason.
     *
     * @return an instance of JMXConnector if a provider was found from
     *         which one could be instantiated, {@code null} otherwise.
     */
    private static JMXConnector getConnectorAsService(ClassLoader loader,
                                                      JMXServiceURL url,
                                                      Map<String, ?> map,
                                                      Predicate<Provider<?>> filter)
        throws IOException {

        final ConnectorFactory<JMXConnectorProvider, JMXConnector> factory =
                (p) -> p.newJMXConnector(url, map);
        return getConnectorAsService(JMXConnectorProvider.class, loader, url,
                                     filter, factory);
    }


    /**
     * A factory function that can create a connector from a provider.
     * The pair (P,C) will be either one of:
     * a. (JMXConnectorProvider, JMXConnector) or
     * b. (JMXConnectorServerProvider, JMXConnectorServer)
     */
    @FunctionalInterface
    static interface ConnectorFactory<P,C> {
        public C apply(P provider) throws Exception;
    }

    /**
     * An instance of ProviderFinder is used to traverse a
     * {@code Stream<Provider<P>>} and find the first implementation of P
     * that supports creating a connector C from the given JMXServiceURL.
     * <p>
     * The pair (P,C) will be either one of: <br>
     * a. (JMXConnectorProvider, JMXConnector) or <br>
     * b. (JMXConnectorServerProvider, JMXConnectorServer)
     * <p>
     * The first connector successfully created while traversing the stream
     * is stored in the ProviderFinder instance. After that, the
     * ProviderFinder::test method, if called, will always return false, skipping
     * the remaining providers.
     * <p>
     * An instance of ProviderFinder is always expected to be used in conjunction
     * with Stream::findFirst, so that the stream traversal is stopped as soon
     * as a matching provider is found.
     * <p>
     * At the end of the stream traversal, the ProviderFinder::get method can be
     * used to obtain the connector instance (an instance of C) that was created.
     * If no connector could be created, and an exception was encountered while
     * traversing the stream and attempting to create the connector, then that
     * exception will be thrown by ProviderFinder::get, wrapped, if needed,
     * inside an IOException.
     * <p>
     * If any JMXProviderException is encountered while traversing the stream and
     * attempting to create the connector, that exception will be wrapped in an
     * UncheckedIOException and thrown immediately within the stream, thus
     * interrupting the traversal.
     * <p>
     * If no matching provider was found (no provider found or attempting
     * factory.apply always returned null or threw a MalformedURLException,
     * indicating the provider didn't support the protocol asked for by
     * the JMXServiceURL), then ProviderFinder::get will simply return null.
     */
    private static final class ProviderFinder<P,C> implements Predicate<Provider<P>> {

        final ConnectorFactory<P,C> factory;
        final JMXServiceURL  url;
        private IOException  exception = null;
        private C connection = null;

        ProviderFinder(ConnectorFactory<P,C> factory, JMXServiceURL url) {
            this.factory = factory;
            this.url = url;
        }

        /**
         * Returns {@code true} for the first provider {@code sp} that can
         * be used to obtain an instance of {@code C} from the given
         * {@code factory}.
         *
         * @param sp a candidate provider for instantiating {@code C}.
         *
         * @throws UncheckedIOException if {@code sp} throws a
         *         JMXProviderException. The JMXProviderException is set as the
         *         root cause.
         *
         * @return {@code true} for the first provider {@code sp} for which
         *         {@code C} could be instantiated, {@code false} otherwise.
         */
        public boolean test(Provider<P> sp) {
            if (connection == null) {
                P provider = sp.get();
                try {
                    connection = factory.apply(provider);
                    return connection != null;
                } catch (JMXProviderException e) {
                    throw new UncheckedIOException(e);
                } catch (Exception e) {
                    if (logger.traceOn())
                        logger.trace("getConnectorAsService",
                             "URL[" + url +
                             "] Service provider exception: " + e);
                    if (!(e instanceof MalformedURLException)) {
                        if (exception == null) {
                            if (e instanceof IOException) {
                                exception = (IOException) e;
                            } else {
                                exception = EnvHelp.initCause(
                                    new IOException(e.getMessage()), e);
                            }
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Returns an instance of {@code C} if a provider was found from
         * which {@code C} could be instantiated.
         *
         * @throws IOException if {@code C} could not be instantiated, and
         *         at least one provider threw an exception that wasn't a
         *         {@code MalformedURLException} or a {@code JMProviderException}.
         *
         * @return an instance of {@code C} if a provider was found from
         *         which {@code C} could be instantiated, {@code null} otherwise.
         */
        C get() throws IOException {
            if (connection != null) return connection;
            else if (exception != null) throw exception;
            else return null;
        }
    }

    /**
     * Creates a connector from a provider loaded from the ServiceLoader.
     * <p>
     * The pair (P,C) will be either one of: <br>
     * a. (JMXConnectorProvider, JMXConnector) or <br>
     * b. (JMXConnectorServerProvider, JMXConnectorServer)
     *
     * @param providerClass The service type for which an implementation
     *        should be looked up from the {@code ServiceLoader}. This will
     *        be either {@code JMXConnectorProvider.class} or
     *        {@code JMXConnectorServerProvider.class}
     *
     * @param loader The ClassLoader to use when looking up an implementation
     *        of the service. If null, then only installed services will be
     *        considered.
     *
     * @param url The JMXServiceURL of the connector for which a provider is
     *        requested.
     *
     * @param filter A filter used to exclude or return provider
     *        implementations. Typically the filter will either exclude
     *        system services (system default implementations) or only
     *        retain those.
     *        This can allow to first look for custom implementations (e.g.
     *        deployed on the CLASSPATH with META-INF/services) and
     *        then only default to system implementations.
     *
     * @param factory A functional factory that can attempt to create an
     *        instance of connector {@code C} from a provider {@code P}.
     *        Typically, this is a simple wrapper over {@code
     *        JMXConnectorProvider::newJMXConnector} or {@code
     *        JMXConnectorProviderServer::newJMXConnectorServer}.
     *
     * @throws IOException if {@code C} could not be instantiated, and
     *         at least one provider {@code P} threw an exception that wasn't a
     *         {@code MalformedURLException} or a {@code JMProviderException}.
     *
     * @throws JMXProviderException if a provider {@code P} for the protocol in
     *         <code>url</code> was found, but couldn't create the connector
     *         {@code C} for some reason.
     *
     * @return an instance of {@code C} if a provider {@code P} was found from
     *         which one could be instantiated, {@code null} otherwise.
     */
    static <P,C> C getConnectorAsService(Class<P> providerClass,
                                         ClassLoader loader,
                                         JMXServiceURL url,
                                         Predicate<Provider<?>> filter,
                                         ConnectorFactory<P,C> factory)
        throws IOException {

        // sanity check
        if (JMXConnectorProvider.class != providerClass
            && JMXConnectorServerProvider.class != providerClass) {
            // should never happen
            throw new InternalError("Unsupported service interface: "
                                    + providerClass.getName());
        }

        ServiceLoader<P> serviceLoader = loader == null
                ? ServiceLoader.loadInstalled(providerClass)
                : ServiceLoader.load(providerClass, loader);
        Stream<Provider<P>> stream = serviceLoader.stream().filter(filter);
        ProviderFinder<P,C> finder = new ProviderFinder<>(factory, url);

        try {
            stream.filter(finder).findFirst();
            return finder.get();
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof JMXProviderException) {
                throw (JMXProviderException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    static <T> T getProvider(String protocol,
                              String pkgs,
                              ClassLoader loader,
                              String providerClassName,
                              Class<T> targetInterface)
            throws IOException {

        StringTokenizer tokenizer = new StringTokenizer(pkgs, "|");

        while (tokenizer.hasMoreTokens()) {
            String pkg = tokenizer.nextToken();
            String className = (pkg + "." + protocol2package(protocol) +
                                "." + providerClassName);
            Class<?> providerClass;
            try {
                providerClass = Class.forName(className, true, loader);
            } catch (ClassNotFoundException e) {
                //Add trace.
                continue;
            }

            if (!targetInterface.isAssignableFrom(providerClass)) {
                final String msg =
                    "Provider class does not implement " +
                    targetInterface.getName() + ": " +
                    providerClass.getName();
                throw new JMXProviderException(msg);
            }

            // We have just proved that this cast is correct
            Class<? extends T> providerClassT = Util.cast(providerClass);
            try {
                @SuppressWarnings("deprecation")
                T result = providerClassT.newInstance();
                return result;
            } catch (Exception e) {
                final String msg =
                    "Exception when instantiating provider [" + className +
                    "]";
                throw new JMXProviderException(msg, e);
            }
        }

        return null;
    }

    static ClassLoader resolveClassLoader(Map<String, ?> environment) {
        ClassLoader loader = null;

        if (environment != null) {
            try {
                loader = (ClassLoader)
                    environment.get(PROTOCOL_PROVIDER_CLASS_LOADER);
            } catch (ClassCastException e) {
                final String msg =
                    "The ClassLoader supplied in the environment map using " +
                    "the " + PROTOCOL_PROVIDER_CLASS_LOADER +
                    " attribute is not an instance of java.lang.ClassLoader";
                throw new IllegalArgumentException(msg);
            }
        }

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        return loader;
    }

    private static String protocol2package(String protocol) {
        return protocol.replace('+', '.').replace('-', '_');
    }
}
