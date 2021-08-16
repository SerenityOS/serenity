/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.smartcardio;

import java.util.*;

import java.security.*;

import sun.security.jca.*;
import sun.security.jca.GetInstance.*;

/**
 * A factory for CardTerminal objects.
 *
 * It allows an application to
 * <ul>
 * <li>obtain a TerminalFactory by calling
 * one of the static factory methods in this class
 * ({@linkplain #getDefault} or {@linkplain #getInstance getInstance()}).
 * <li>use this TerminalFactory object to access the CardTerminals by
 * calling the {@linkplain #terminals} method.
 * </ul>
 *
 * <p>Each TerminalFactory has a <code>type</code> indicating how it
 * was implemented. It must be specified when the implementation is obtained
 * using a {@linkplain #getInstance getInstance()} method and can be retrieved
 * via the {@linkplain #getType} method.
 *
 * <P>The following standard type names have been defined:
 * <dl>
 * <dt><code>PC/SC</code>
 * <dd>an implementation that calls into the PC/SC Smart Card stack
 * of the host platform.
 * Implementations do not require parameters and accept "null" as argument
 * in the getInstance() calls.
 * <dt><code>None</code>
 * <dd>an implementation that does not supply any CardTerminals. On platforms
 * that do not support other implementations,
 * {@linkplain #getDefaultType} returns <code>None</code> and
 * {@linkplain #getDefault} returns an instance of a <code>None</code>
 * TerminalFactory. Factories of this type cannot be obtained by calling the
 * <code>getInstance()</code> methods.
 * </dl>
 * Additional standard types may be defined in the future.
 *
 * <p><strong>Note:</strong>
 * Provider implementations that accept initialization parameters via the
 * <code>getInstance()</code> methods are strongly
 * encouraged to use a {@linkplain java.util.Properties} object as the
 * representation for String name-value pair based parameters whenever
 * possible. This allows applications to more easily interoperate with
 * multiple providers than if each provider used different provider
 * specific class as parameters.
 *
 * <P>TerminalFactory utilizes an extensible service provider framework.
 * Service providers that wish to add a new implementation should see the
 * {@linkplain TerminalFactorySpi} class for more information.
 *
 * @see CardTerminals
 * @see Provider
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public final class TerminalFactory {

    private final static String PROP_NAME =
                        "javax.smartcardio.TerminalFactory.DefaultType";

    private final static String defaultType;

    private final static TerminalFactory defaultFactory;

    static {
        // lookup up the user specified type, default to PC/SC
        @SuppressWarnings("removal")
        String type = AccessController.doPrivileged(
             (PrivilegedAction<String>) () -> System.getProperty(PROP_NAME, "PC/SC")).trim();
        TerminalFactory factory = null;
        try {
            factory = TerminalFactory.getInstance(type, null);
        } catch (Exception e) {
            // ignore
        }
        if (factory == null) {
            // if that did not work, try the Sun PC/SC factory
            try {
                type = "PC/SC";
                Provider sun = Security.getProvider("SunPCSC");
                if (sun == null) {
                    @SuppressWarnings("deprecation")
                    Object o = Class.forName("sun.security.smartcardio.SunPCSC").newInstance();
                    sun = (Provider)o;
                }
                factory = TerminalFactory.getInstance(type, null, sun);
            } catch (Exception e) {
                // ignore
            }
        }
        if (factory == null) {
            type = "None";
            factory = new TerminalFactory
                        (NoneFactorySpi.INSTANCE, NoneProvider.INSTANCE, "None");
        }
        defaultType = type;
        defaultFactory = factory;
    }

    private static final class NoneProvider extends Provider {

        private static final long serialVersionUID = 2745808869881593918L;
        final static Provider INSTANCE = new NoneProvider();
        private NoneProvider() {
            super("None", "1.0", "none");
        }
    }

    private static final class NoneFactorySpi extends TerminalFactorySpi {
        final static TerminalFactorySpi INSTANCE = new NoneFactorySpi();
        private NoneFactorySpi() {
            // empty
        }
        protected CardTerminals engineTerminals() {
            return NoneCardTerminals.INSTANCE;
        }
    }

    private static final class NoneCardTerminals extends CardTerminals {
        final static CardTerminals INSTANCE = new NoneCardTerminals();
        private NoneCardTerminals() {
            // empty
        }
        public List<CardTerminal> list(State state) throws CardException {
            if (state == null) {
                throw new NullPointerException();
            }
            return Collections.emptyList();
        }
        public boolean waitForChange(long timeout) throws CardException {
            throw new IllegalStateException("no terminals");
        }
    }

    private final TerminalFactorySpi spi;

    private final Provider provider;

    private final String type;

    private TerminalFactory(TerminalFactorySpi spi, Provider provider, String type) {
        this.spi = spi;
        this.provider = provider;
        this.type = type;
    }

    /**
     * Get the default TerminalFactory type.
     *
     * <p>It is determined as follows:
     *
     * when this class is initialized, the system property
     * {@systemProperty javax.smartcardio.TerminalFactory.DefaultType}
     * is examined. If it is set, a TerminalFactory of this type is
     * instantiated by calling the {@linkplain #getInstance
     * getInstance(String,Object)} method passing
     * <code>null</code> as the value for <code>params</code>. If the call
     * succeeds, the type becomes the default type and the factory becomes
     * the {@linkplain #getDefault default} factory.
     *
     * <p>If the system property is not set or the getInstance() call fails
     * for any reason, the system defaults to an implementation specific
     * default type and TerminalFactory.
     *
     * @return the default TerminalFactory type
     */
    public static String getDefaultType() {
        return defaultType;
    }

    /**
     * Returns the default TerminalFactory instance. See
     * {@linkplain #getDefaultType} for more information.
     *
     * <p>A default TerminalFactory is always available. However, depending
     * on the implementation, it may not offer any terminals.
     *
     * @return the default TerminalFactory
     */
    public static TerminalFactory getDefault() {
        return defaultFactory;
    }

    /**
     * Returns a TerminalFactory of the specified type that is initialized
     * with the specified parameters.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new TerminalFactory object encapsulating the
     * TerminalFactorySpi implementation from the first
     * Provider that supports the specified type is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@linkplain Security#getProviders() Security.getProviders()} method.
     *
     * <p>The <code>TerminalFactory</code> is initialized with the
     * specified parameters Object. The type of parameters
     * needed may vary between different types of <code>TerminalFactory</code>s.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param type the type of the requested TerminalFactory
     * @param params the parameters to pass to the TerminalFactorySpi
     *   implementation, or null if no parameters are needed
     * @return a TerminalFactory of the specified type
     *
     * @throws NullPointerException if type is null
     * @throws NoSuchAlgorithmException if no Provider supports a
     *   TerminalFactorySpi of the specified type
     */
    public static TerminalFactory getInstance(String type, Object params)
            throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("TerminalFactory",
            TerminalFactorySpi.class, type, params);
        return new TerminalFactory((TerminalFactorySpi)instance.impl,
            instance.provider, type);
    }

    /**
     * Returns a TerminalFactory of the specified type that is initialized
     * with the specified parameters.
     *
     * <p> A new TerminalFactory object encapsulating the
     * TerminalFactorySpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@linkplain Security#getProviders() Security.getProviders()} method.
     *
     * <p>The <code>TerminalFactory</code> is initialized with the
     * specified parameters Object. The type of parameters
     * needed may vary between different types of <code>TerminalFactory</code>s.
     *
     * @param type the type of the requested TerminalFactory
     * @param params the parameters to pass to the TerminalFactorySpi
     *   implementation, or null if no parameters are needed
     * @param provider the name of the provider
     * @return a TerminalFactory of the specified type
     *
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if provider is null or the empty String
     * @throws NoSuchAlgorithmException if a TerminalFactorySpi implementation
     *   of the specified type is not available from the specified provider
     * @throws NoSuchAlgorithmException if no TerminalFactory of the
     *   specified type could be found
     * @throws NoSuchProviderException if the specified provider could not
     *   be found
     */
    public static TerminalFactory getInstance(String type, Object params,
            String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        Instance instance = GetInstance.getInstance("TerminalFactory",
            TerminalFactorySpi.class, type, params, provider);
        return new TerminalFactory((TerminalFactorySpi)instance.impl,
            instance.provider, type);
    }

    /**
     * Returns a TerminalFactory of the specified type that is initialized
     * with the specified parameters.
     *
     * <p> A new TerminalFactory object encapsulating the
     * TerminalFactorySpi implementation from the specified provider object
     * is returned. Note that the specified provider object does not have to be
     * registered in the provider list.
     *
     * <p>The <code>TerminalFactory</code> is initialized with the
     * specified parameters Object. The type of parameters
     * needed may vary between different types of <code>TerminalFactory</code>s.
     *
     * @param type the type of the requested TerminalFactory
     * @param params the parameters to pass to the TerminalFactorySpi
     *   implementation, or null if no parameters are needed
     * @param provider the provider
     * @return a TerminalFactory of the specified type
     *
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if provider is null
     * @throws NoSuchAlgorithmException if a TerminalFactorySpi implementation
     *   of the specified type is not available from the specified Provider
     */
    public static TerminalFactory getInstance(String type, Object params,
            Provider provider) throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("TerminalFactory",
            TerminalFactorySpi.class, type, params, provider);
        return new TerminalFactory((TerminalFactorySpi)instance.impl,
            instance.provider, type);
    }

    /**
     * Returns the provider of this TerminalFactory.
     *
     * @return the provider of this TerminalFactory.
     */
    public Provider getProvider() {
        return provider;
    }

    /**
     * Returns the type of this TerminalFactory. This is the value that was
     * specified in the getInstance() method that returned this object.
     *
     * @return the type of this TerminalFactory
     */
    public String getType() {
        return type;
    }

    /**
     * Returns a new CardTerminals object encapsulating the terminals
     * supported by this factory.
     * See the class comment of the {@linkplain CardTerminals} class
     * regarding how the returned objects can be shared and reused.
     *
     * @return a new CardTerminals object encapsulating the terminals
     * supported by this factory.
     */
    public CardTerminals terminals() {
        return spi.engineTerminals();
    }

    /**
     * Returns a string representation of this TerminalFactory.
     *
     * @return a string representation of this TerminalFactory.
     */
    public String toString() {
        return "TerminalFactory for type " + type + " from provider "
            + provider.getName();
    }

}
