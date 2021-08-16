/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.login;

import javax.security.auth.AuthPermission;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Objects;

import sun.security.jca.GetInstance;

/**
 * A Configuration object is responsible for specifying which LoginModules
 * should be used for a particular application, and in what order the
 * LoginModules should be invoked.
 *
 * <p> A login configuration contains the following information.
 * Note that this example only represents the default syntax for the
 * {@code Configuration}.  Subclass implementations of this class
 * may implement alternative syntaxes and may retrieve the
 * {@code Configuration} from any source such as files, databases,
 * or servers.
 *
 * <pre>
 *      Name {
 *            ModuleClass  Flag    ModuleOptions;
 *            ModuleClass  Flag    ModuleOptions;
 *            ModuleClass  Flag    ModuleOptions;
 *      };
 *      Name {
 *            ModuleClass  Flag    ModuleOptions;
 *            ModuleClass  Flag    ModuleOptions;
 *      };
 *      other {
 *            ModuleClass  Flag    ModuleOptions;
 *            ModuleClass  Flag    ModuleOptions;
 *      };
 * </pre>
 *
 * <p> Each entry in the {@code Configuration} is indexed via an
 * application name, <i>Name</i>, and contains a list of
 * LoginModules configured for that application.  Each {@code LoginModule}
 * is specified via its fully qualified class name.
 * Authentication proceeds down the module list in the exact order specified.
 * If an application does not have a specific entry,
 * it defaults to the specific entry for "<i>other</i>".
 *
 * <p> The <i>Flag</i> value controls the overall behavior as authentication
 * proceeds down the stack.  The following represents a description of the
 * valid values for <i>Flag</i> and their respective semantics:
 *
 * <pre>
 *      1) Required     - The {@code LoginModule} is required to succeed.
 *                      If it succeeds or fails, authentication still continues
 *                      to proceed down the {@code LoginModule} list.
 *
 *      2) Requisite    - The {@code LoginModule} is required to succeed.
 *                      If it succeeds, authentication continues down the
 *                      {@code LoginModule} list.  If it fails,
 *                      control immediately returns to the application
 *                      (authentication does not proceed down the
 *                      {@code LoginModule} list).
 *
 *      3) Sufficient   - The {@code LoginModule} is not required to
 *                      succeed.  If it does succeed, control immediately
 *                      returns to the application (authentication does not
 *                      proceed down the {@code LoginModule} list).
 *                      If it fails, authentication continues down the
 *                      {@code LoginModule} list.
 *
 *      4) Optional     - The {@code LoginModule} is not required to
 *                      succeed.  If it succeeds or fails,
 *                      authentication still continues to proceed down the
 *                      {@code LoginModule} list.
 * </pre>
 *
 * <p> The overall authentication succeeds only if all <i>Required</i> and
 * <i>Requisite</i> LoginModules succeed.  If a <i>Sufficient</i>
 * {@code LoginModule} is configured and succeeds,
 * then only the <i>Required</i> and <i>Requisite</i> LoginModules prior to
 * that <i>Sufficient</i> {@code LoginModule} need to have succeeded for
 * the overall authentication to succeed. If no <i>Required</i> or
 * <i>Requisite</i> LoginModules are configured for an application,
 * then at least one <i>Sufficient</i> or <i>Optional</i>
 * {@code LoginModule} must succeed.
 *
 * <p> <i>ModuleOptions</i> is a space separated list of
 * {@code LoginModule}-specific values which are passed directly to
 * the underlying LoginModules.  Options are defined by the
 * {@code LoginModule} itself, and control the behavior within it.
 * For example, a {@code LoginModule} may define options to support
 * debugging/testing capabilities.  The correct way to specify options in the
 * {@code Configuration} is by using the following key-value pairing:
 * <i>debug="true"</i>.  The key and value should be separated by an
 * 'equals' symbol, and the value should be surrounded by double quotes.
 * If a String in the form, ${system.property}, occurs in the value,
 * it will be expanded to the value of the system property.
 * Note that there is no limit to the number of
 * options a {@code LoginModule} may define.
 *
 * <p> The following represents an example {@code Configuration} entry
 * based on the syntax above:
 *
 * <pre>
 * Login {
 *   com.sun.security.auth.module.UnixLoginModule required;
 *   com.sun.security.auth.module.Krb5LoginModule optional
 *                   useTicketCache="true"
 *                   ticketCache="${user.home}${/}tickets";
 * };
 * </pre>
 *
 * <p> This {@code Configuration} specifies that an application named,
 * "Login", requires users to first authenticate to the
 * <i>com.sun.security.auth.module.UnixLoginModule</i>, which is
 * required to succeed.  Even if the <i>UnixLoginModule</i>
 * authentication fails, the
 * <i>com.sun.security.auth.module.Krb5LoginModule</i>
 * still gets invoked.  This helps hide the source of failure.
 * Since the <i>Krb5LoginModule</i> is <i>Optional</i>, the overall
 * authentication succeeds only if the <i>UnixLoginModule</i>
 * (<i>Required</i>) succeeds.
 *
 * <p> Also note that the LoginModule-specific options,
 * <i>useTicketCache="true"</i> and
 * <i>ticketCache=${user.home}${/}tickets"</i>,
 * are passed to the <i>Krb5LoginModule</i>.
 * These options instruct the <i>Krb5LoginModule</i> to
 * use the ticket cache at the specified location.
 * The system properties, <i>user.home</i> and <i>/</i>
 * (file.separator), are expanded to their respective values.
 *
 * <p> There is only one Configuration object installed in the runtime at any
 * given time.  A Configuration object can be installed by calling the
 * {@code setConfiguration} method.  The installed Configuration object
 * can be obtained by calling the {@code getConfiguration} method.
 *
 * <p> If no Configuration object has been installed in the runtime, a call to
 * {@code getConfiguration} installs an instance of the default
 * Configuration implementation (a default subclass implementation of this
 * abstract class).
 * The default Configuration implementation can be changed by setting the value
 * of the {@code login.configuration.provider} security property to the fully
 * qualified name of the desired Configuration subclass implementation.
 *
 * <p> Application code can directly subclass Configuration to provide a custom
 * implementation.  In addition, an instance of a Configuration object can be
 * constructed by invoking one of the {@code getInstance} factory methods
 * with a standard type.  The default policy type is "JavaLoginConfig".
 * See the Configuration section in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#configuration-types">
 * Java Security Standard Algorithm Names Specification</a>
 * for a list of standard Configuration types.
 *
 * @since 1.4
 * @see javax.security.auth.login.LoginContext
 * @see java.security.Security security properties
 */
public abstract class Configuration {

    private static Configuration configuration;

    @SuppressWarnings("removal")
    private final java.security.AccessControlContext acc =
            java.security.AccessController.getContext();

    private static void checkPermission(String type) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AuthPermission
                                ("createLoginConfiguration." + type));
        }
    }

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected Configuration() { }

    /**
     * Get the installed login Configuration.
     *
     * @return the login Configuration.  If a Configuration object was set
     *          via the {@code Configuration.setConfiguration} method,
     *          then that object is returned.  Otherwise, a default
     *          Configuration object is returned.
     *
     * @exception SecurityException if the caller does not have permission
     *                          to retrieve the Configuration.
     *
     * @see #setConfiguration
     */
    @SuppressWarnings("removal")
    public static Configuration getConfiguration() {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new AuthPermission("getLoginConfiguration"));

        synchronized (Configuration.class) {
            if (configuration == null) {
                String config_class = null;
                config_class = AccessController.doPrivileged
                    (new PrivilegedAction<>() {
                    public String run() {
                        return java.security.Security.getProperty
                                    ("login.configuration.provider");
                    }
                });
                if (config_class == null) {
                    config_class = "sun.security.provider.ConfigFile";
                }

                try {
                    final String finalClass = config_class;
                    Configuration untrustedImpl = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<>() {
                                public Configuration run() throws ClassNotFoundException,
                                        InstantiationException,
                                        IllegalAccessException {
                                    Class<? extends Configuration> implClass = Class.forName(
                                            finalClass, false,
                                            Thread.currentThread().getContextClassLoader()
                                    ).asSubclass(Configuration.class);
                                    @SuppressWarnings("deprecation")
                                    Configuration result = implClass.newInstance();
                                    return result;
                                }
                            });
                    AccessController.doPrivileged(
                            new PrivilegedExceptionAction<>() {
                                public Void run() {
                                    setConfiguration(untrustedImpl);
                                    return null;
                                }
                            }, Objects.requireNonNull(untrustedImpl.acc)
                    );
                } catch (PrivilegedActionException e) {
                    Exception ee = e.getException();
                    if (ee instanceof InstantiationException) {
                        throw (SecurityException) new
                            SecurityException
                                    ("Configuration error:" +
                                     ee.getCause().getMessage() +
                                     "\n").initCause(ee.getCause());
                    } else {
                        throw (SecurityException) new
                            SecurityException
                                    ("Configuration error: " +
                                     ee.toString() +
                                     "\n").initCause(ee);
                    }
                }
            }
            return configuration;
        }
    }

    /**
     * Set the login {@code Configuration}.
     *
     * @param configuration the new {@code Configuration}
     *
     * @exception SecurityException if the current thread does not have
     *                  Permission to set the {@code Configuration}.
     *
     * @see #getConfiguration
     */
    public static void setConfiguration(Configuration configuration) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new AuthPermission("setLoginConfiguration"));
        Configuration.configuration = configuration;
    }

    /**
     * Returns a Configuration object of the specified type.
     *
     * <p> This method traverses the list of registered security providers,
     * starting with the most preferred Provider.
     * A new Configuration object encapsulating the
     * ConfigurationSpi implementation from the first
     * Provider that supports the specified type is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param type the specified Configuration type.  See the Configuration
     *    section in the <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#configuration-types">
     *    Java Security Standard Algorithm Names Specification</a>
     *    for a list of standard Configuration types.
     *
     * @param params parameters for the Configuration, which may be null.
     *
     * @return the new {@code Configuration} object
     *
     * @throws IllegalArgumentException if the specified parameters
     *         are not understood by the {@code ConfigurationSpi}
     *         implementation from the selected {@code Provider}
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code ConfigurationSpi} implementation for the specified type
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @throws SecurityException if the caller does not have permission
     *         to get a {@code Configuration} instance for the specified type
     *
     * @see Provider
     *
     * @since 1.6
     */
    public static Configuration getInstance(String type,
                                Configuration.Parameters params)
                throws NoSuchAlgorithmException {

        Objects.requireNonNull(type, "null type name");
        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance
                                                        ("Configuration",
                                                        ConfigurationSpi.class,
                                                        type,
                                                        params);
            return new ConfigDelegate((ConfigurationSpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException (nsae);
        }
    }

    /**
     * Returns a Configuration object of the specified type.
     *
     * <p> A new Configuration object encapsulating the
     * ConfigurationSpi implementation from the specified provider
     * is returned.   The specified provider must be registered
     * in the provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param type the specified Configuration type.  See the Configuration
     *    section in the <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#configuration-types">
     *    Java Security Standard Algorithm Names Specification</a>
     *    for a list of standard Configuration types.
     *
     * @param params parameters for the Configuration, which may be null.
     *
     * @param provider the provider.
     *
     * @return the new {@code Configuration} object
     *
     * @throws IllegalArgumentException if the specified provider
     *         is {@code null} or empty, or if the specified parameters
     *         are not understood by the {@code ConfigurationSpi}
     *         implementation from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NoSuchAlgorithmException if the specified provider does not
     *         support a {@code ConfigurationSpi} implementation for the
     *         specified type
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @throws SecurityException if the caller does not have permission
     *         to get a {@code Configuration} instance for the specified type
     *
     * @see Provider
     * @since 1.6
     */
    public static Configuration getInstance(String type,
                                Configuration.Parameters params,
                                String provider)
                throws NoSuchProviderException, NoSuchAlgorithmException {

        Objects.requireNonNull(type, "null type name");
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance
                                                        ("Configuration",
                                                        ConfigurationSpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new ConfigDelegate((ConfigurationSpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException (nsae);
        }
    }

    /**
     * Returns a Configuration object of the specified type.
     *
     * <p> A new Configuration object encapsulating the
     * ConfigurationSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param type the specified Configuration type.  See the Configuration
     *    section in the <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#configuration-types">
     *    Java Security Standard Algorithm Names Specification</a>
     *    for a list of standard Configuration types.
     *
     * @param params parameters for the Configuration, which may be null.
     *
     * @param provider the Provider.
     *
     * @return the new {@code Configuration} object
     *
     * @throws IllegalArgumentException if the specified {@code Provider}
     *         is {@code null}, or if the specified parameters are not
     *         understood by the {@code ConfigurationSpi} implementation
     *         from the specified Provider
     *
     * @throws NoSuchAlgorithmException if the specified {@code Provider}
     *         does not support a {@code ConfigurationSpi} implementation
     *         for the specified type
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @throws SecurityException if the caller does not have permission
     *         to get a {@code Configuration} instance for the specified type
     *
     * @see Provider
     * @since 1.6
     */
    public static Configuration getInstance(String type,
                                Configuration.Parameters params,
                                Provider provider)
                throws NoSuchAlgorithmException {

        Objects.requireNonNull(type, "null type name");
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance
                                                        ("Configuration",
                                                        ConfigurationSpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new ConfigDelegate((ConfigurationSpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException (nsae);
        }
    }

    private static Configuration handleException(NoSuchAlgorithmException nsae)
                throws NoSuchAlgorithmException {
        Throwable cause = nsae.getCause();
        if (cause instanceof IllegalArgumentException) {
            throw (IllegalArgumentException)cause;
        }
        throw nsae;
    }

    /**
     * Return the Provider of this Configuration.
     *
     * <p> This Configuration instance will only have a Provider if it
     * was obtained via a call to {@code Configuration.getInstance}.
     * Otherwise this method returns null.
     *
     * @return the Provider of this Configuration, or null.
     *
     * @since 1.6
     */
    public Provider getProvider() {
        return null;
    }

    /**
     * Return the type of this Configuration.
     *
     * <p> This Configuration instance will only have a type if it
     * was obtained via a call to {@code Configuration.getInstance}.
     * Otherwise this method returns null.
     *
     * @return the type of this Configuration, or null.
     *
     * @since 1.6
     */
    public String getType() {
        return null;
    }

    /**
     * Return Configuration parameters.
     *
     * <p> This Configuration instance will only have parameters if it
     * was obtained via a call to {@code Configuration.getInstance}.
     * Otherwise this method returns null.
     *
     * @return Configuration parameters, or null.
     *
     * @since 1.6
     */
    public Configuration.Parameters getParameters() {
        return null;
    }

    /**
     * Retrieve the AppConfigurationEntries for the specified {@code name}
     * from this Configuration.
     *
     * @param name the name used to index the Configuration.
     *
     * @return an array of AppConfigurationEntries for the specified {@code name}
     *          from this Configuration, or null if there are no entries
     *          for the specified {@code name}
     */
    public abstract AppConfigurationEntry[] getAppConfigurationEntry
                                                        (String name);

    /**
     * Refresh and reload the Configuration.
     *
     * <p> This method causes this Configuration object to refresh/reload its
     * contents in an implementation-dependent manner.
     * For example, if this Configuration object stores its entries in a file,
     * calling {@code refresh} may cause the file to be re-read.
     *
     * <p> The default implementation of this method does nothing.
     * This method should be overridden if a refresh operation is supported
     * by the implementation.
     *
     * @exception SecurityException if the caller does not have permission
     *                          to refresh its Configuration.
     */
    public void refresh() { }

    /**
     * This subclass is returned by the getInstance calls.  All Configuration
     * calls are delegated to the underlying ConfigurationSpi.
     */
    private static class ConfigDelegate extends Configuration {

        private ConfigurationSpi spi;
        private Provider p;
        private String type;
        private Configuration.Parameters params;

        private ConfigDelegate(ConfigurationSpi spi, Provider p,
                        String type, Configuration.Parameters params) {
            this.spi = spi;
            this.p = p;
            this.type = type;
            this.params = params;
        }

        public String getType() { return type; }

        public Configuration.Parameters getParameters() { return params; }

        public Provider getProvider() { return p; }

        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return spi.engineGetAppConfigurationEntry(name);
        }

        public void refresh() {
            spi.engineRefresh();
        }
    }

    /**
     * This represents a marker interface for Configuration parameters.
     *
     * @since 1.6
     */
    public static interface Parameters { }
}
