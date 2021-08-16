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

package sun.security.jca;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;

import java.security.*;

import sun.security.util.PropertyExpander;

/**
 * Class representing a configured provider which encapsulates configuration
 * (provider name + optional argument), the provider loading logic, and
 * the loaded Provider object itself.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class ProviderConfig {

    private static final sun.security.util.Debug debug =
        sun.security.util.Debug.getInstance("jca", "ProviderConfig");

    // suffix for identifying the SunPKCS11-Solaris provider
    private static final String P11_SOL_NAME = "SunPKCS11";

    // config file argument of the SunPKCS11-Solaris provider
    private static final String P11_SOL_ARG  =
        "${java.home}/conf/security/sunpkcs11-solaris.cfg";

    // maximum number of times to try loading a provider before giving up
    private static final int MAX_LOAD_TRIES = 30;

    // could be provider name (module) or provider class name (legacy)
    private final String provName;

    // argument to the Provider.configure() call, never null
    private final String argument;

    // number of times we have already tried to load this provider
    private int tries;

    // Provider object, if loaded
    private volatile Provider provider;

    // flag indicating if we are currently trying to load the provider
    // used to detect recursion
    private boolean isLoading;

    ProviderConfig(String provName, String argument) {
        if (provName.endsWith(P11_SOL_NAME) && argument.equals(P11_SOL_ARG)) {
            checkSunPKCS11Solaris();
        }
        this.provName = provName;
        this.argument = expand(argument);
    }

    ProviderConfig(String provName) {
        this(provName, "");
    }

    ProviderConfig(Provider provider) {
        this.provName = provider.getName();
        this.argument = "";
        this.provider = provider;
    }

    // check if we should try to load the SunPKCS11-Solaris provider
    // avoid if not available (pre Solaris 10) to reduce startup time
    // or if disabled via system property
    private void checkSunPKCS11Solaris() {
        @SuppressWarnings("removal")
        Boolean o = AccessController.doPrivileged(
                                new PrivilegedAction<Boolean>() {
            public Boolean run() {
                File file = new File("/usr/lib/libpkcs11.so");
                if (file.exists() == false) {
                    return Boolean.FALSE;
                }
                if ("false".equalsIgnoreCase(System.getProperty
                        ("sun.security.pkcs11.enable-solaris"))) {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        });
        if (o == Boolean.FALSE) {
            tries = MAX_LOAD_TRIES;
        }
    }

    private boolean hasArgument() {
        return !argument.isEmpty();
    }

    // should we try to load this provider?
    private boolean shouldLoad() {
        return (tries < MAX_LOAD_TRIES);
    }

    // do not try to load this provider again
    private void disableLoad() {
        tries = MAX_LOAD_TRIES;
    }

    boolean isLoaded() {
        return (provider != null);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProviderConfig == false) {
            return false;
        }
        ProviderConfig other = (ProviderConfig)obj;
        return this.provName.equals(other.provName)
            && this.argument.equals(other.argument);

    }

    public int hashCode() {
        return provName.hashCode() + argument.hashCode();
    }

    public String toString() {
        if (hasArgument()) {
            return provName + "('" + argument + "')";
        } else {
            return provName;
        }
    }

    /**
     * Get the provider object. Loads the provider if it is not already loaded.
     */
    @SuppressWarnings("deprecation")
    Provider getProvider() {
        // volatile variable load
        Provider p = provider;
        if (p != null) {
            return p;
        }
        // DCL
        synchronized (this) {
            p = provider;
            if (p != null) {
                return p;
            }
            if (shouldLoad() == false) {
                return null;
            }

            // Create providers which are in java.base directly
            if (provName.equals("SUN") || provName.equals("sun.security.provider.Sun")) {
                p = new sun.security.provider.Sun();
            } else if (provName.equals("SunRsaSign") || provName.equals("sun.security.rsa.SunRsaSign")) {
                p = new sun.security.rsa.SunRsaSign();
            } else if (provName.equals("SunJCE") || provName.equals("com.sun.crypto.provider.SunJCE")) {
                p = new com.sun.crypto.provider.SunJCE();
            } else if (provName.equals("SunJSSE")) {
                p = new sun.security.ssl.SunJSSE();
            } else if (provName.equals("Apple") || provName.equals("apple.security.AppleProvider")) {
                // need to use reflection since this class only exists on MacOsx
                @SuppressWarnings("removal")
                var tmp = AccessController.doPrivileged(new PrivilegedAction<Provider>() {
                    public Provider run() {
                        try {
                            Class<?> c = Class.forName("apple.security.AppleProvider");
                            if (Provider.class.isAssignableFrom(c)) {
                                @SuppressWarnings("deprecation")
                                Object tmp = c.newInstance();
                                return (Provider) tmp;
                            } else {
                                return null;
                            }
                        } catch (Exception ex) {
                            if (debug != null) {
                                debug.println("Error loading provider Apple");
                                ex.printStackTrace();
                            }
                            return null;
                        }
                    }
                });
                p = tmp;
            } else {
                if (isLoading) {
                    // because this method is synchronized, this can only
                    // happen if there is recursion.
                    if (debug != null) {
                        debug.println("Recursion loading provider: " + this);
                        new Exception("Call trace").printStackTrace();
                    }
                    return null;
                }
                try {
                    isLoading = true;
                    tries++;
                    p = doLoadProvider();
                } finally {
                    isLoading = false;
                }
            }
            provider = p;
        }
        return p;
    }

    /**
     * Load and instantiate the Provider described by this class.
     *
     * NOTE use of doPrivileged().
     *
     * @return null if the Provider could not be loaded
     *
     * @throws ProviderException if executing the Provider's constructor
     * throws a ProviderException. All other Exceptions are ignored.
     */
    @SuppressWarnings("removal")
    private Provider doLoadProvider() {
        return AccessController.doPrivileged(new PrivilegedAction<Provider>() {
            public Provider run() {
                if (debug != null) {
                    debug.println("Loading provider " + ProviderConfig.this);
                }
                try {
                    Provider p = ProviderLoader.INSTANCE.load(provName);
                    if (p != null) {
                        if (hasArgument()) {
                            p = p.configure(argument);
                        }
                        if (debug != null) {
                            debug.println("Loaded provider " + p.getName());
                        }
                    } else {
                        if (debug != null) {
                            debug.println("Error loading provider " +
                                ProviderConfig.this);
                        }
                        disableLoad();
                    }
                    return p;
                } catch (Exception e) {
                    if (e instanceof ProviderException) {
                        // pass up
                        throw e;
                    } else {
                        if (debug != null) {
                            debug.println("Error loading provider " +
                                ProviderConfig.this);
                            e.printStackTrace();
                        }
                        disableLoad();
                        return null;
                    }
                } catch (ExceptionInInitializerError err) {
                    // no sufficient permission to initialize provider class
                    if (debug != null) {
                        debug.println("Error loading provider " + ProviderConfig.this);
                        err.printStackTrace();
                    }
                    disableLoad();
                    return null;
                }
            }
        });
    }

    /**
     * Perform property expansion of the provider value.
     *
     * NOTE use of doPrivileged().
     */
    @SuppressWarnings("removal")
    private static String expand(final String value) {
        // shortcut if value does not contain any properties
        if (value.contains("${") == false) {
            return value;
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                try {
                    return PropertyExpander.expand(value);
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
        });
    }

    // Inner class for loading security providers listed in java.security file
    private static final class ProviderLoader {
        static final ProviderLoader INSTANCE = new ProviderLoader();

        private final ServiceLoader<Provider> services;

        private ProviderLoader() {
            // VM should already been booted at this point, if not
            // - Only providers in java.base should be loaded, don't use
            //   ServiceLoader
            // - ClassLoader.getSystemClassLoader() will throw InternalError
            services = ServiceLoader.load(java.security.Provider.class,
                                          ClassLoader.getSystemClassLoader());
        }

        /**
         * Loads the provider with the specified class name.
         *
         * @param pn the name of the provider
         * @return the Provider, or null if it cannot be found or loaded
         * @throws ProviderException all other exceptions are ignored
         */
        public Provider load(String pn) {
            if (debug != null) {
                debug.println("Attempt to load " + pn + " using SL");
            }
            Iterator<Provider> iter = services.iterator();
            while (iter.hasNext()) {
                try {
                    Provider p = iter.next();
                    String pName = p.getName();
                    if (debug != null) {
                        debug.println("Found SL Provider named " + pName);
                    }
                    if (pName.equals(pn)) {
                        return p;
                    }
                } catch (SecurityException | ServiceConfigurationError |
                         InvalidParameterException ex) {
                    // if provider loading fail due to security permission,
                    // log it and move on to next provider
                    if (debug != null) {
                        debug.println("Encountered " + ex +
                            " while iterating through SL, ignore and move on");
                            ex.printStackTrace();
                    }
                }
            }
            // No success with ServiceLoader. Try loading provider the legacy,
            // i.e. pre-module, way via reflection
            try {
                return legacyLoad(pn);
            } catch (ProviderException pe) {
                // pass through
                throw pe;
            } catch (Exception ex) {
                // logged and ignored
                if (debug != null) {
                    debug.println("Encountered " + ex +
                        " during legacy load of " + pn);
                        ex.printStackTrace();
                }
                return null;
            }
        }

        private Provider legacyLoad(String classname) {

            if (debug != null) {
                debug.println("Loading legacy provider: " + classname);
            }

            try {
                Class<?> provClass =
                    ClassLoader.getSystemClassLoader().loadClass(classname);

                // only continue if the specified class extends Provider
                if (!Provider.class.isAssignableFrom(provClass)) {
                    if (debug != null) {
                        debug.println(classname + " is not a provider");
                    }
                    return null;
                }

                @SuppressWarnings("removal")
                Provider p = AccessController.doPrivileged
                    (new PrivilegedExceptionAction<Provider>() {
                    @SuppressWarnings("deprecation") // Class.newInstance
                    public Provider run() throws Exception {
                        return (Provider) provClass.newInstance();
                    }
                });
                return p;
            } catch (Exception e) {
                Throwable t;
                if (e instanceof InvocationTargetException) {
                    t = ((InvocationTargetException)e).getCause();
                } else {
                    t = e;
                }
                if (debug != null) {
                    debug.println("Error loading legacy provider " + classname);
                    t.printStackTrace();
                }
                // provider indicates fatal error, pass through exception
                if (t instanceof ProviderException) {
                    throw (ProviderException) t;
                }
                return null;
            } catch (ExceptionInInitializerError | NoClassDefFoundError err) {
                // no sufficient permission to access/initialize provider class
                if (debug != null) {
                    debug.println("Error loading legacy provider " + classname);
                    err.printStackTrace();
                }
                return null;
            }
        }
    }
}
