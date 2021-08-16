/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.*;
import java.util.*;
import static java.util.Locale.ENGLISH;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a "provider" for the
 * Java Security API, where a provider implements some or all parts of
 * Java Security. Services that a provider may implement include:
 *
 * <ul>
 *
 * <li>Algorithms (such as DSA, RSA, or SHA-256).
 *
 * <li>Key generation, conversion, and management facilities (such as for
 * algorithm-specific keys).
 *
 * </ul>
 *
 * <p>Some provider implementations may encounter unrecoverable internal
 * errors during their operation, for example a failure to communicate with a
 * security token. A {@link ProviderException} should be used to indicate
 * such errors.
 *
 * <p>Please note that a provider can be used to implement any security
 * service in Java that uses a pluggable architecture with a choice
 * of implementations that fit underneath.
 *
 * <p>The service type {@code Provider} is reserved for use by the
 * security framework. Services of this type cannot be added, removed,
 * or modified by applications.
 * The following attributes are automatically placed in each Provider object:
 * <table class="striped">
 * <caption><b>Attributes Automatically Placed in a Provider Object</b></caption>
 * <thead>
 * <tr><th scope="col">Name</th><th scope="col">Value</th>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">{@code Provider.id name}</th>
 *     <td>{@code String.valueOf(provider.getName())}</td>
 * <tr><th scope="row">{@code Provider.id version}</th>
 *     <td>{@code String.valueOf(provider.getVersionStr())}</td>
 * <tr><th scope="row">{@code Provider.id info}</th>
 *     <td>{@code String.valueOf(provider.getInfo())}</td>
 * <tr><th scope="row">{@code Provider.id className}</th>
 *     <td>{@code provider.getClass().getName()}</td>
 * </tbody>
 * </table>
 *
 * <p>Each provider has a name and a version string. A provider normally
 * identifies itself with a file named {@code java.security.Provider}
 * in the resource directory {@code META-INF/services}.
 * Security providers are looked up via the {@link ServiceLoader} mechanism
 * using the {@link ClassLoader#getSystemClassLoader application class loader}.
 *
 * <p>Providers may be configured such that they are automatically
 * installed and made available at runtime via the
 * {@link Security#getProviders() Security.getProviders()} method.
 * The mechanism for configuring and installing security providers is
 * implementation-specific.
 *
 * @implNote
 * The JDK implementation supports static registration of the security
 * providers via the {@code conf/security/java.security} file in the Java
 * installation directory. These providers are automatically installed by
 * the JDK runtime, see {@extLink security_guide_jca_provider
 * The Provider Class}
 * in the Java Cryptography Architecture (JCA) Reference Guide
 * for information about how a particular type of provider, the cryptographic
 * service provider, works and is installed.
 *
 * @author Benjamin Renaud
 * @author Andreas Sterbenz
 * @since 1.1
 */
public abstract class Provider extends Properties {

    // Declare serialVersionUID to be compatible with JDK1.1
    @java.io.Serial
    private static final long serialVersionUID = -4298000515446427739L;

    private static final sun.security.util.Debug debug =
        sun.security.util.Debug.getInstance("provider", "Provider");

    /**
     * The provider name.
     *
     * @serial
     */
    private String name;

    /**
     * A description of the provider and its services.
     *
     * @serial
     */
    private String info;

    /**
     * The provider version number.
     *
     * @serial
     */
    private double version;

    /**
     * The provider version string.
     *
     * @serial
     */
    private String versionStr;

    private transient Set<Map.Entry<Object,Object>> entrySet = null;
    private transient int entrySetCallCount = 0;

    private transient boolean initialized;

    private static final Object[] EMPTY = new Object[0];

    private static double parseVersionStr(String s) {
        try {
            int firstDotIdx = s.indexOf('.');
            int nextDotIdx = s.indexOf('.', firstDotIdx + 1);
            if (nextDotIdx != -1) {
                s = s.substring(0, nextDotIdx);
            }
            int endIdx = s.indexOf('-');
            if (endIdx > 0) {
                s = s.substring(0, endIdx);
            }
            endIdx = s.indexOf('+');
            if (endIdx > 0) {
                s = s.substring(0, endIdx);
            }
            return Double.parseDouble(s);
        } catch (NullPointerException | NumberFormatException e) {
            return 0d;
        }
    }

    /**
     * Constructs a provider with the specified name, version number,
     * and information. Calling this constructor is equivalent to call the
     * {@link #Provider(String, String, String)} with {@code name}
     * name, {@code Double.toString(version)}, and {@code info}.
     *
     * @param name the provider name.
     *
     * @param version the provider version number.
     *
     * @param info a description of the provider and its services.
     *
     * @deprecated use {@link #Provider(String, String, String)} instead.
     */
    @Deprecated(since="9")
    protected Provider(String name, double version, String info) {
        this.name = name;
        this.version = version;
        this.versionStr = Double.toString(version);
        this.info = info;
        this.serviceMap = new ConcurrentHashMap<>();
        putId();
        initialized = true;
    }

    /**
     * Constructs a provider with the specified name, version string,
     * and information.
     *
     * <p>The version string contains a version number optionally followed
     * by other information separated by one of the characters of '+', '-'.
     *
     * The format for the version number is:
     *
     * <blockquote><pre>
     *     ^[0-9]+(\.[0-9]+)*
     * </pre></blockquote>
     *
     * <p>In order to return the version number in a double, when there are
     * more than two components (separated by '.' as defined above), only
     * the first two components are retained. The resulting string is then
     * passed to {@link Double#valueOf(String)} to generate version number,
     * i.e. {@link #getVersion}.
     * <p>If the conversion failed, value 0 will be used.
     *
     * @param name the provider name.
     *
     * @param versionStr the provider version string.
     *
     * @param info a description of the provider and its services.
     *
     * @since 9
     */
    protected Provider(String name, String versionStr, String info) {
        this.name = name;
        this.versionStr = versionStr;
        this.version = parseVersionStr(versionStr);
        this.info = info;
        this.serviceMap = new ConcurrentHashMap<>();
        putId();
        initialized = true;
    }

    /**
     * Apply the supplied configuration argument to this provider instance
     * and return the configured provider. Note that if this provider cannot
     * be configured in-place, a new provider will be created and returned.
     * Therefore, callers should always use the returned provider.
     *
     * @implSpec
     * The default implementation throws {@code UnsupportedOperationException}.
     * Subclasses should override this method only if a configuration argument
     * is supported.
     *
     * @param configArg the configuration information for configuring this
     *         provider.
     *
     * @throws UnsupportedOperationException if a configuration argument is
     *         not supported.
     * @throws NullPointerException if the supplied configuration argument is
     *         null.
     * @throws InvalidParameterException if the supplied configuration argument
     *         is invalid.
     * @return a provider configured with the supplied configuration argument.
     *
     * @since 9
     */
    public Provider configure(String configArg) {
        throw new UnsupportedOperationException("configure is not supported");
    }

    /**
     * Check if this provider instance has been configured.
     *
     * @implSpec
     * The default implementation returns true.
     * Subclasses should override this method if the provider instance requires
     * an explicit {@code configure} call after being constructed.
     *
     * @return true if no further configuration is needed, false otherwise.
     *
     * @since 9
     */
    public boolean isConfigured() {
        return true;
    }


    /**
     * Returns the name of this provider.
     *
     * @return the name of this provider.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version number for this provider.
     *
     * @return the version number for this provider.
     *
     * @deprecated use {@link #getVersionStr} instead.
     */
    @Deprecated(since="9")
    public double getVersion() {
        return version;
    }

    /**
     * Returns the version string for this provider.
     *
     * @return the version string for this provider.
     *
     * @since 9
     */
    public String getVersionStr() {
        return versionStr;
    }

    /**
     * Returns a human-readable description of the provider and its
     * services.  This may return an HTML page, with relevant links.
     *
     * @return a description of the provider and its services.
     */
    public String getInfo() {
        return info;
    }

    /**
     * Returns a string with the name and the version string
     * of this provider.
     *
     * @return the string with the name and the version string
     * for this provider.
     */
    public String toString() {
        return name + " version " + versionStr;
    }

    /*
     * override the following methods to ensure that provider
     * information can only be changed if the caller has the appropriate
     * permissions.
     */

    /**
     * Clears this provider so that it no longer contains the properties
     * used to look up facilities implemented by the provider.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "clearProviderProperties."+name}
     * (where {@code name} is the provider name) to see if it's ok to clear
     * this provider.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to clear this provider
     *
     * @since 1.2
     */
    @Override
    public synchronized void clear() {
        check("clearProviderProperties."+name);
        if (debug != null) {
            debug.println("Remove " + name + " provider properties");
        }
        implClear();
    }

    /**
     * Reads a property list (key and element pairs) from the input stream.
     *
     * @param inStream the input stream.
     * @throws    IOException if an error occurred when reading from the
     *               input stream.
     * @see java.util.Properties#load
     */
    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        check("putProviderProperty."+name);
        if (debug != null) {
            debug.println("Load " + name + " provider properties");
        }
        Properties tempProperties = new Properties();
        tempProperties.load(inStream);
        implPutAll(tempProperties);
    }

    /**
     * Copies all of the mappings from the specified Map to this provider.
     * These mappings will replace any properties that this provider had
     * for any of the keys currently in the specified Map.
     *
     * @since 1.2
     */
    @Override
    public synchronized void putAll(Map<?,?> t) {
        check("putProviderProperty."+name);
        if (debug != null) {
            debug.println("Put all " + name + " provider properties");
        }
        implPutAll(t);
    }

    /**
     * Returns an unmodifiable Set view of the property entries contained
     * in this Provider.
     *
     * @see   java.util.Map.Entry
     * @since 1.2
     */
    @Override
    public synchronized Set<Map.Entry<Object,Object>> entrySet() {
        checkInitialized();
        if (entrySet == null) {
            if (entrySetCallCount++ == 0)  // Initial call
                entrySet = Collections.unmodifiableMap(this).entrySet();
            else
                return super.entrySet();   // Recursive call
        }

        // This exception will be thrown if the implementation of
        // Collections.unmodifiableMap.entrySet() is changed such that it
        // no longer calls entrySet() on the backing Map.  (Provider's
        // entrySet implementation depends on this "implementation detail",
        // which is unlikely to change.
        if (entrySetCallCount != 2)
            throw new RuntimeException("Internal error.");

        return entrySet;
    }

    /**
     * Returns an unmodifiable Set view of the property keys contained in
     * this provider.
     *
     * @since 1.2
     */
    @Override
    public Set<Object> keySet() {
        checkInitialized();
        return Collections.unmodifiableSet(super.keySet());
    }

    /**
     * Returns an unmodifiable Collection view of the property values
     * contained in this provider.
     *
     * @since 1.2
     */
    @Override
    public Collection<Object> values() {
        checkInitialized();
        return Collections.unmodifiableCollection(super.values());
    }

    /**
     * Sets the {@code key} property to have the specified
     * {@code value}.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "putProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to set this
     * provider's property values.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values.
     *
     * @since 1.2
     */
    @Override
    public synchronized Object put(Object key, Object value) {
        check("putProviderProperty."+name);
        if (debug != null) {
            debug.println("Set " + name + " provider property [" +
                          key + "/" + value +"]");
        }
        return implPut(key, value);
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "putProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to set this
     * provider's property values.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        check("putProviderProperty."+name);
        if (debug != null) {
            debug.println("Set " + name + " provider property [" +
                          key + "/" + value +"]");
        }
        return implPutIfAbsent(key, value);
    }

    /**
     * Removes the {@code key} property (and its corresponding
     * {@code value}).
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "removeProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to remove this
     * provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to remove this provider's properties.
     *
     * @since 1.2
     */
    @Override
    public synchronized Object remove(Object key) {
        check("removeProviderProperty."+name);
        if (debug != null) {
            debug.println("Remove " + name + " provider property " + key);
        }
        return implRemove(key);
    }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "removeProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to remove this
     * provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to remove this provider's properties.
     *
     * @since 1.8
     */
    @Override
    public synchronized boolean remove(Object key, Object value) {
        check("removeProviderProperty."+name);
        if (debug != null) {
            debug.println("Remove " + name + " provider property " + key);
        }
        return implRemove(key, value);
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "putProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to set this
     * provider's property values.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values.
     *
     * @since 1.8
     */
    @Override
    public synchronized boolean replace(Object key, Object oldValue,
            Object newValue) {
        check("putProviderProperty." + name);

        if (debug != null) {
            debug.println("Replace " + name + " provider property " + key);
        }
        return implReplace(key, oldValue, newValue);
    }

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "putProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to set this
     * provider's property values.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object replace(Object key, Object value) {
        check("putProviderProperty." + name);

        if (debug != null) {
            debug.println("Replace " + name + " provider property " + key);
        }
        return implReplace(key, value);
    }

    /**
     * Replaces each entry's value with the result of invoking the given
     * function on that entry, in the order entries are returned by an entry
     * set iterator, until all entries have been processed or the function
     * throws an exception.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the string {@code "putProviderProperty."+name},
     * where {@code name} is the provider name, to see if it's ok to set this
     * provider's property values.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values.
     *
     * @since 1.8
     */
    @Override
    public synchronized void replaceAll(BiFunction<? super Object,
            ? super Object, ? extends Object> function) {
        check("putProviderProperty." + name);

        if (debug != null) {
            debug.println("ReplaceAll " + name + " provider property ");
        }
        implReplaceAll(function);
    }

    /**
     * Attempts to compute a mapping for the specified key and its
     * current mapped value (or {@code null} if there is no current
     * mapping).
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the strings {@code "putProviderProperty."+name}
     * and {@code "removeProviderProperty."+name}, where {@code name} is the
     * provider name, to see if it's ok to set this provider's property values
     * and remove this provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values or remove properties.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object compute(Object key, BiFunction<? super Object,
            ? super Object, ? extends Object> remappingFunction) {
        check("putProviderProperty." + name);
        check("removeProviderProperty." + name);

        if (debug != null) {
            debug.println("Compute " + name + " provider property " + key);
        }
        return implCompute(key, remappingFunction);
    }

    /**
     * If the specified key is not already associated with a value (or
     * is mapped to {@code null}), attempts to compute its value using
     * the given mapping function and enters it into this map unless
     * {@code null}.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the strings {@code "putProviderProperty."+name}
     * and {@code "removeProviderProperty."+name}, where {@code name} is the
     * provider name, to see if it's ok to set this provider's property values
     * and remove this provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values and remove properties.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object,
            ? extends Object> mappingFunction) {
        check("putProviderProperty." + name);
        check("removeProviderProperty." + name);

        if (debug != null) {
            debug.println("ComputeIfAbsent " + name + " provider property " +
                    key);
        }
        return implComputeIfAbsent(key, mappingFunction);
    }

    /**
     * If the value for the specified key is present and non-null, attempts to
     * compute a new mapping given the key and its current mapped value.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the strings {@code "putProviderProperty."+name}
     * and {@code "removeProviderProperty."+name}, where {@code name} is the
     * provider name, to see if it's ok to set this provider's property values
     * and remove this provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values or remove properties.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object computeIfPresent(Object key, BiFunction<? super Object,
            ? super Object, ? extends Object> remappingFunction) {
        check("putProviderProperty." + name);
        check("removeProviderProperty." + name);

        if (debug != null) {
            debug.println("ComputeIfPresent " + name + " provider property " +
                    key);
        }
        return implComputeIfPresent(key, remappingFunction);
    }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given value. Otherwise,
     * replaces the value with the results of the given remapping function,
     * or removes if the result is null. This method may be of use when
     * combining multiple mapped values for a key.
     *
     * <p>If a security manager is enabled, its {@code checkSecurityAccess}
     * method is called with the strings {@code "putProviderProperty."+name}
     * and {@code "removeProviderProperty."+name}, where {@code name} is the
     * provider name, to see if it's ok to set this provider's property values
     * and remove this provider's properties.
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method
     *          denies access to set property values or remove properties.
     *
     * @since 1.8
     */
    @Override
    public synchronized Object merge(Object key, Object value,  BiFunction<? super Object,
            ? super Object, ? extends Object>  remappingFunction) {
        check("putProviderProperty." + name);
        check("removeProviderProperty." + name);

        if (debug != null) {
            debug.println("Merge " + name + " provider property " + key);
        }
        return implMerge(key, value, remappingFunction);
    }

    // let javadoc show doc from superclass
    @Override
    public Object get(Object key) {
        checkInitialized();
        return super.get(key);
    }
    /**
     * @since 1.8
     */
    @Override
    public synchronized Object getOrDefault(Object key, Object defaultValue) {
        checkInitialized();
        return super.getOrDefault(key, defaultValue);
    }

    /**
     * @since 1.8
     */
    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        checkInitialized();
        super.forEach(action);
    }

    // let javadoc show doc from superclass
    @Override
    public Enumeration<Object> keys() {
        checkInitialized();
        return super.keys();
    }

    // let javadoc show doc from superclass
    @Override
    public Enumeration<Object> elements() {
        checkInitialized();
        return super.elements();
    }

    // let javadoc show doc from superclass
    public String getProperty(String key) {
        checkInitialized();
        return super.getProperty(key);
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException();
        }
    }

    private void check(String directive) {
        checkInitialized();
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSecurityAccess(directive);
        }
    }

    // legacy properties changed since last call to any services method?
    private transient boolean legacyChanged;
    // serviceMap changed since last call to getServices()
    private volatile transient boolean servicesChanged;

    // Map<String,String> used to keep track of legacy registration
    private transient Map<String,String> legacyStrings;

    // Map<ServiceKey,Service>
    // used for services added via putService(), initialized on demand
    private transient Map<ServiceKey,Service> serviceMap;

    // For backward compatibility, the registration ordering of
    // SecureRandom (RNG) algorithms needs to be preserved for
    // "new SecureRandom()" calls when this provider is used
    private transient Set<String> prngAlgos;

    // Map<ServiceKey,Service>
    // used for services added via legacy methods, init on demand
    private transient Map<ServiceKey,Service> legacyMap;

    // Set<Service>
    // Unmodifiable set of all services. Initialized on demand.
    private transient Set<Service> serviceSet;

    // register the id attributes for this provider
    // this is to ensure that equals() and hashCode() do not incorrectly
    // report to different provider objects as the same
    private void putId() {
        // note: name and info may be null
        super.put("Provider.id name", String.valueOf(name));
        super.put("Provider.id version", String.valueOf(versionStr));
        super.put("Provider.id info", String.valueOf(info));
        super.put("Provider.id className", this.getClass().getName());
    }

   /**
    * Reads the {@code ObjectInputStream} for the default serializable fields.
    * If the serialized field {@code versionStr} is found in the STREAM FIELDS,
    * its String value will be used to populate both the version string and
    * version number. If {@code versionStr} is not found, but {@code version}
    * is, then its double value will be used to populate both fields.
    *
    * @param in the {@code ObjectInputStream} to read
    * @throws IOException if an I/O error occurs
    * @throws ClassNotFoundException if a serialized class cannot be loaded
    * @serial
    */
    @java.io.Serial
    private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
        Map<Object,Object> copy = new HashMap<>();
        for (Map.Entry<Object,Object> entry : super.entrySet()) {
            copy.put(entry.getKey(), entry.getValue());
        }
        defaults = null;
        in.defaultReadObject();
        if (this.versionStr == null) {
            // set versionStr based on version when not found in serialized bytes
            this.versionStr = Double.toString(this.version);
        } else {
            // otherwise, set version based on versionStr
            this.version = parseVersionStr(this.versionStr);
        }
        this.serviceMap = new ConcurrentHashMap<>();
        implClear();
        initialized = true;
        putAll(copy);
    }

    // check whether to update 'legacyString' with the specified key
    private boolean checkLegacy(Object key) {
        String keyString = (String)key;
        if (keyString.startsWith("Provider.")) {
            return false;
        }

        legacyChanged = true;
        if (legacyStrings == null) {
            legacyStrings = new LinkedHashMap<>();
        }
        return true;
    }

    /**
     * Copies all of the mappings from the specified Map to this provider.
     * Internal method to be called AFTER the security check has been
     * performed.
     */
    private void implPutAll(Map<?,?> t) {
        for (Map.Entry<?,?> e : t.entrySet()) {
            implPut(e.getKey(), e.getValue());
        }
    }

    private Object implRemove(Object key) {
        if (key instanceof String) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.remove((String)key);
        }
        return super.remove(key);
    }

    private boolean implRemove(Object key, Object value) {
        if (key instanceof String && value instanceof String) {
            if (!checkLegacy(key)) {
                return false;
            }
            legacyStrings.remove((String)key, (String)value);
        }
        return super.remove(key, value);
    }

    private boolean implReplace(Object key, Object oldValue, Object newValue) {
        if ((key instanceof String) && (oldValue instanceof String) &&
                (newValue instanceof String)) {
            if (!checkLegacy(key)) {
                return false;
            }
            legacyStrings.replace((String)key, (String)oldValue,
                    (String)newValue);
        }
        return super.replace(key, oldValue, newValue);
    }

    private Object implReplace(Object key, Object value) {
        if ((key instanceof String) && (value instanceof String)) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.replace((String)key, (String)value);
        }
        return super.replace(key, value);
    }

    @SuppressWarnings("unchecked") // Function must actually operate over strings
    private void implReplaceAll(BiFunction<? super Object, ? super Object,
            ? extends Object> function) {
        legacyChanged = true;
        if (legacyStrings == null) {
            legacyStrings = new LinkedHashMap<>();
        } else {
            legacyStrings.replaceAll((BiFunction<? super String, ? super String,
                    ? extends String>) function);
        }
        super.replaceAll(function);
    }

    @SuppressWarnings("unchecked") // Function must actually operate over strings
    private Object implMerge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object>
            remappingFunction) {
        if ((key instanceof String) && (value instanceof String)) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.merge((String)key, (String)value,
                    (BiFunction<? super String, ? super String,
                    ? extends String>) remappingFunction);
        }
        return super.merge(key, value, remappingFunction);
    }

    @SuppressWarnings("unchecked") // Function must actually operate over strings
    private Object implCompute(Object key, BiFunction<? super Object,
            ? super Object, ? extends Object> remappingFunction) {
        if (key instanceof String) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.compute((String) key,
                    (BiFunction<? super String,? super String,
                    ? extends String>) remappingFunction);
        }
        return super.compute(key, remappingFunction);
    }

    @SuppressWarnings("unchecked") // Function must actually operate over strings
    private Object implComputeIfAbsent(Object key, Function<? super Object,
            ? extends Object> mappingFunction) {
        if (key instanceof String) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.computeIfAbsent((String) key,
                    (Function<? super String, ? extends String>)
                    mappingFunction);
        }
        return super.computeIfAbsent(key, mappingFunction);
    }

    @SuppressWarnings("unchecked") // Function must actually operate over strings
    private Object implComputeIfPresent(Object key, BiFunction<? super Object,
            ? super Object, ? extends Object> remappingFunction) {
        if (key instanceof String) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.computeIfPresent((String) key,
                    (BiFunction<? super String, ? super String,
                    ? extends String>) remappingFunction);
        }
        return super.computeIfPresent(key, remappingFunction);
    }

    private Object implPut(Object key, Object value) {
        if ((key instanceof String) && (value instanceof String)) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.put((String)key, (String)value);
        }
        return super.put(key, value);
    }

    private Object implPutIfAbsent(Object key, Object value) {
        if ((key instanceof String) && (value instanceof String)) {
            if (!checkLegacy(key)) {
                return null;
            }
            legacyStrings.putIfAbsent((String)key, (String)value);
        }
        return super.putIfAbsent(key, value);
    }

    private void implClear() {
        if (legacyStrings != null) {
            legacyStrings.clear();
        }
        if (legacyMap != null) {
            legacyMap.clear();
        }
        serviceMap.clear();
        legacyChanged = false;
        servicesChanged = false;
        serviceSet = null;
        prngAlgos = null;
        super.clear();
        putId();
    }

    // used as key in the serviceMap and legacyMap HashMaps
    private static class ServiceKey {
        private final String type;
        private final String algorithm;
        private final String originalAlgorithm;
        private ServiceKey(String type, String algorithm, boolean intern) {
            this.type = type;
            this.originalAlgorithm = algorithm;
            algorithm = algorithm.toUpperCase(ENGLISH);
            this.algorithm = intern ? algorithm.intern() : algorithm;
        }
        public int hashCode() {
            return type.hashCode() * 31 + algorithm.hashCode();
        }
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof ServiceKey other
                && this.type.equals(other.type)
                && this.algorithm.equals(other.algorithm);
        }
        boolean matches(String type, String algorithm) {
            return (this.type == type) && (this.originalAlgorithm == algorithm);
        }
    }

    /**
     * Ensure all the legacy String properties are fully parsed into
     * service objects.
     */
    private void ensureLegacyParsed() {
        if (legacyChanged == false || (legacyStrings == null)) {
            return;
        }
        serviceSet = null;
        if (legacyMap == null) {
            legacyMap = new ConcurrentHashMap<>();
        } else {
            legacyMap.clear();
        }
        for (Map.Entry<String,String> entry : legacyStrings.entrySet()) {
            parseLegacyPut(entry.getKey(), entry.getValue());
        }
        removeInvalidServices(legacyMap);
        legacyChanged = false;
    }

    /**
     * Remove all invalid services from the Map. Invalid services can only
     * occur if the legacy properties are inconsistent or incomplete.
     */
    private void removeInvalidServices(Map<ServiceKey,Service> map) {
        for (Iterator<Map.Entry<ServiceKey, Service>> t =
                map.entrySet().iterator(); t.hasNext(); ) {
            Service s = t.next().getValue();
            if (s.isValid() == false) {
                t.remove();
            }
        }
    }

    private static String[] getTypeAndAlgorithm(String key) {
        int i = key.indexOf('.');
        if (i < 1) {
            if (debug != null) {
                debug.println("Ignoring invalid entry in provider: "
                        + key);
            }
            return null;
        }
        String type = key.substring(0, i);
        String alg = key.substring(i + 1);
        return new String[] {type, alg};
    }

    private static final String ALIAS_PREFIX = "Alg.Alias.";
    private static final String ALIAS_PREFIX_LOWER = "alg.alias.";
    private static final int ALIAS_LENGTH = ALIAS_PREFIX.length();

    private void parseLegacyPut(String name, String value) {
        if (name.toLowerCase(ENGLISH).startsWith(ALIAS_PREFIX_LOWER)) {
            // e.g. put("Alg.Alias.MessageDigest.SHA", "SHA-1");
            // aliasKey ~ MessageDigest.SHA
            String stdAlg = value;
            String aliasKey = name.substring(ALIAS_LENGTH);
            String[] typeAndAlg = getTypeAndAlgorithm(aliasKey);
            if (typeAndAlg == null) {
                return;
            }
            String type = getEngineName(typeAndAlg[0]);
            String aliasAlg = typeAndAlg[1].intern();
            ServiceKey key = new ServiceKey(type, stdAlg, true);
            Service s = legacyMap.get(key);
            if (s == null) {
                s = new Service(this, type, stdAlg);
                legacyMap.put(key, s);
            }
            legacyMap.put(new ServiceKey(type, aliasAlg, true), s);
            s.addAlias(aliasAlg);
        } else {
            String[] typeAndAlg = getTypeAndAlgorithm(name);
            if (typeAndAlg == null) {
                return;
            }
            int i = typeAndAlg[1].indexOf(' ');
            if (i == -1) {
                // e.g. put("MessageDigest.SHA-1", "sun.security.provider.SHA");
                String type = getEngineName(typeAndAlg[0]);
                String stdAlg = typeAndAlg[1].intern();
                String className = value;
                ServiceKey key = new ServiceKey(type, stdAlg, true);
                Service s = legacyMap.get(key);
                if (s == null) {
                    s = new Service(this, type, stdAlg);
                    legacyMap.put(key, s);
                }
                s.className = className;

                if (type.equals("SecureRandom")) {
                    updateSecureRandomEntries(true, s.algorithm);
                }
            } else { // attribute
                // e.g. put("MessageDigest.SHA-1 ImplementedIn", "Software");
                String attributeValue = value;
                String type = getEngineName(typeAndAlg[0]);
                String attributeString = typeAndAlg[1];
                String stdAlg = attributeString.substring(0, i).intern();
                String attributeName = attributeString.substring(i + 1);
                // kill additional spaces
                while (attributeName.startsWith(" ")) {
                    attributeName = attributeName.substring(1);
                }
                attributeName = attributeName.intern();
                ServiceKey key = new ServiceKey(type, stdAlg, true);
                Service s = legacyMap.get(key);
                if (s == null) {
                    s = new Service(this, type, stdAlg);
                    legacyMap.put(key, s);
                }
                s.addAttribute(attributeName, attributeValue);
            }
        }
    }

    /**
     * Get the service describing this Provider's implementation of the
     * specified type of this algorithm or alias. If no such
     * implementation exists, this method returns null. If there are two
     * matching services, one added to this provider using
     * {@link #putService putService()} and one added via {@link #put put()},
     * the service added via {@link #putService putService()} is returned.
     *
     * @param type the type of {@link Service service} requested
     * (for example, {@code MessageDigest})
     * @param algorithm the case insensitive algorithm name (or alternate
     * alias) of the service requested (for example, {@code SHA-1})
     *
     * @return the service describing this Provider's matching service
     * or null if no such service exists
     *
     * @throws NullPointerException if type or algorithm is null
     *
     * @since 1.5
     */
    public Service getService(String type, String algorithm) {
        checkInitialized();

        // avoid allocating a new ServiceKey object if possible
        ServiceKey key = previousKey;
        if (key.matches(type, algorithm) == false) {
            key = new ServiceKey(type, algorithm, false);
            previousKey = key;
        }
        if (!serviceMap.isEmpty()) {
            Service s = serviceMap.get(key);
            if (s != null) {
                return s;
            }
        }
        synchronized (this) {
            ensureLegacyParsed();
            if (legacyMap != null && !legacyMap.isEmpty()) {
                return legacyMap.get(key);
            }
        }
        return null;
    }

    // ServiceKey from previous getService() call
    // by re-using it if possible we avoid allocating a new object
    // and the toUpperCase() call.
    // re-use will occur e.g. as the framework traverses the provider
    // list and queries each provider with the same values until it finds
    // a matching service
    private static volatile ServiceKey previousKey =
                                            new ServiceKey("", "", false);

    /**
     * Get an unmodifiable Set of all services supported by
     * this Provider.
     *
     * @return an unmodifiable Set of all services supported by
     * this Provider
     *
     * @since 1.5
     */
    public synchronized Set<Service> getServices() {
        checkInitialized();
        if (legacyChanged || servicesChanged) {
            serviceSet = null;
        }
        if (serviceSet == null) {
            ensureLegacyParsed();
            Set<Service> set = new LinkedHashSet<>();
            if (!serviceMap.isEmpty()) {
                set.addAll(serviceMap.values());
            }
            if (legacyMap != null && !legacyMap.isEmpty()) {
                set.addAll(legacyMap.values());
            }
            serviceSet = Collections.unmodifiableSet(set);
            servicesChanged = false;
        }
        return serviceSet;
    }

    /**
     * Add a service. If a service of the same type with the same algorithm
     * name exists and it was added using {@link #putService putService()},
     * it is replaced by the new service.
     * This method also places information about this service
     * in the provider's Hashtable values in the format described in the
     * {@extLink security_guide_jca
     * Java Cryptography Architecture (JCA) Reference Guide}.
     *
     * <p>Also, if there is a security manager, its
     * {@code checkSecurityAccess} method is called with the string
     * {@code "putProviderProperty."+name}, where {@code name} is
     * the provider name, to see if it's ok to set this provider's property
     * values. If the default implementation of {@code checkSecurityAccess}
     * is used (that is, that method is not overridden), then this results in
     * a call to the security manager's {@code checkPermission} method with
     * a {@code SecurityPermission("putProviderProperty."+name)}
     * permission.
     *
     * @param s the Service to add
     *
     * @throws SecurityException
     *      if a security manager exists and its {@link
     *      java.lang.SecurityManager#checkSecurityAccess} method denies
     *      access to set property values.
     * @throws NullPointerException if s is null
     *
     * @since 1.5
     */
    protected void putService(Service s) {
        check("putProviderProperty." + name);
        if (debug != null) {
            debug.println(name + ".putService(): " + s);
        }
        if (s == null) {
            throw new NullPointerException();
        }
        if (s.getProvider() != this) {
            throw new IllegalArgumentException
                    ("service.getProvider() must match this Provider object");
        }
        String type = s.getType();
        String algorithm = s.getAlgorithm();
        ServiceKey key = new ServiceKey(type, algorithm, true);
        implRemoveService(serviceMap.get(key));
        serviceMap.put(key, s);
        for (String alias : s.getAliases()) {
            serviceMap.put(new ServiceKey(type, alias, true), s);
        }
        servicesChanged = true;
        synchronized (this) {
            putPropertyStrings(s);
            if (type.equals("SecureRandom")) {
                updateSecureRandomEntries(true, s.algorithm);
            }
        }
    }

    // keep tracks of the registered secure random algos and store them in order
    private void updateSecureRandomEntries(boolean doAdd, String s) {
        Objects.requireNonNull(s);
        if (doAdd) {
            if (prngAlgos == null) {
                prngAlgos = new LinkedHashSet<String>();
            }
            prngAlgos.add(s);
        } else {
            prngAlgos.remove(s);
        }

        if (debug != null) {
            debug.println((doAdd? "Add":"Remove") + " SecureRandom algo " + s);
        }
    }

    // used by new SecureRandom() to find out the default SecureRandom
    // service for this provider
    synchronized Service getDefaultSecureRandomService() {
        checkInitialized();

        if (legacyChanged) {
            prngAlgos = null;
            ensureLegacyParsed();
        }

        if (prngAlgos != null && !prngAlgos.isEmpty()) {
            // IMPORTANT: use the Service obj returned by getService(...) call
            // as providers may override putService(...)/getService(...) and
            // return their own Service objects
            return getService("SecureRandom", prngAlgos.iterator().next());
        }

        return null;
    }

    /**
     * Put the string properties for this Service in this Provider's
     * Hashtable.
     */
    private void putPropertyStrings(Service s) {
        String type = s.getType();
        String algorithm = s.getAlgorithm();
        // use super() to avoid permission check and other processing
        super.put(type + "." + algorithm, s.getClassName());
        for (String alias : s.getAliases()) {
            super.put(ALIAS_PREFIX + type + "." + alias, algorithm);
        }
        for (Map.Entry<UString,String> entry : s.attributes.entrySet()) {
            String key = type + "." + algorithm + " " + entry.getKey();
            super.put(key, entry.getValue());
        }
    }

    /**
     * Remove the string properties for this Service from this Provider's
     * Hashtable.
     */
    private void removePropertyStrings(Service s) {
        String type = s.getType();
        String algorithm = s.getAlgorithm();
        // use super() to avoid permission check and other processing
        super.remove(type + "." + algorithm);
        for (String alias : s.getAliases()) {
            super.remove(ALIAS_PREFIX + type + "." + alias);
        }
        for (Map.Entry<UString,String> entry : s.attributes.entrySet()) {
            String key = type + "." + algorithm + " " + entry.getKey();
            super.remove(key);
        }
    }

    /**
     * Remove a service previously added using
     * {@link #putService putService()}. The specified service is removed from
     * this provider. It will no longer be returned by
     * {@link #getService getService()} and its information will be removed
     * from this provider's Hashtable.
     *
     * <p>Also, if there is a security manager, its
     * {@code checkSecurityAccess} method is called with the string
     * {@code "removeProviderProperty."+name}, where {@code name} is
     * the provider name, to see if it's ok to remove this provider's
     * properties. If the default implementation of
     * {@code checkSecurityAccess} is used (that is, that method is not
     * overridden), then this results in a call to the security manager's
     * {@code checkPermission} method with a
     * {@code SecurityPermission("removeProviderProperty."+name)}
     * permission.
     *
     * @param s the Service to be removed
     *
     * @throws  SecurityException
     *          if a security manager exists and its {@link
     *          java.lang.SecurityManager#checkSecurityAccess} method denies
     *          access to remove this provider's properties.
     * @throws NullPointerException if s is null
     *
     * @since 1.5
     */
    protected void removeService(Service s) {
        check("removeProviderProperty." + name);
        if (debug != null) {
            debug.println(name + ".removeService(): " + s);
        }
        if (s == null) {
            throw new NullPointerException();
        }
        implRemoveService(s);
    }

    private void implRemoveService(Service s) {
        if ((s == null) || serviceMap.isEmpty()) {
            return;
        }
        String type = s.getType();
        String algorithm = s.getAlgorithm();
        ServiceKey key = new ServiceKey(type, algorithm, false);
        Service oldService = serviceMap.get(key);
        if (s != oldService) {
            return;
        }
        servicesChanged = true;
        serviceMap.remove(key);
        for (String alias : s.getAliases()) {
            serviceMap.remove(new ServiceKey(type, alias, false));
        }
        synchronized (this) {
            removePropertyStrings(s);
            if (type.equals("SecureRandom")) {
                updateSecureRandomEntries(false, s.algorithm);
            }
        }
    }

    // Wrapped String that behaves in a case insensitive way for equals/hashCode
    private static class UString {
        final String string;
        final String lowerString;

        UString(String s) {
            this.string = s;
            this.lowerString = s.toLowerCase(ENGLISH);
        }

        public int hashCode() {
            return lowerString.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof UString other
                    && lowerString.equals(other.lowerString);
        }

        public String toString() {
            return string;
        }
    }

    // describe relevant properties of a type of engine
    private static class EngineDescription {
        final String name;
        final boolean supportsParameter;
        final String constructorParameterClassName;
        private volatile Class<?> constructorParameterClass;

        EngineDescription(String name, boolean sp, String paramName) {
            this.name = name;
            this.supportsParameter = sp;
            this.constructorParameterClassName = paramName;
        }
        Class<?> getConstructorParameterClass() throws ClassNotFoundException {
            Class<?> clazz = constructorParameterClass;
            if (clazz == null) {
                clazz = Class.forName(constructorParameterClassName);
                constructorParameterClass = clazz;
            }
            return clazz;
        }
    }

    // built in knowledge of the engine types shipped as part of the JDK
    private static final Map<String,EngineDescription> knownEngines;

    private static void addEngine(String name, boolean sp, String paramName) {
        EngineDescription ed = new EngineDescription(name, sp, paramName);
        // also index by canonical name to avoid toLowerCase() for some lookups
        knownEngines.put(name.toLowerCase(ENGLISH), ed);
        knownEngines.put(name, ed);
    }

    static {
        knownEngines = new HashMap<>();
        // JCA
        addEngine("AlgorithmParameterGenerator",        false, null);
        addEngine("AlgorithmParameters",                false, null);
        addEngine("KeyFactory",                         false, null);
        addEngine("KeyPairGenerator",                   false, null);
        addEngine("KeyStore",                           false, null);
        addEngine("MessageDigest",                      false, null);
        addEngine("SecureRandom",                       false,
                "java.security.SecureRandomParameters");
        addEngine("Signature",                          true,  null);
        addEngine("CertificateFactory",                 false, null);
        addEngine("CertPathBuilder",                    false, null);
        addEngine("CertPathValidator",                  false, null);
        addEngine("CertStore",                          false,
                            "java.security.cert.CertStoreParameters");
        // JCE
        addEngine("Cipher",                             true,  null);
        addEngine("ExemptionMechanism",                 false, null);
        addEngine("Mac",                                true,  null);
        addEngine("KeyAgreement",                       true,  null);
        addEngine("KeyGenerator",                       false, null);
        addEngine("SecretKeyFactory",                   false, null);
        // JSSE
        addEngine("KeyManagerFactory",                  false, null);
        addEngine("SSLContext",                         false, null);
        addEngine("TrustManagerFactory",                false, null);
        // JGSS
        addEngine("GssApiMechanism",                    false, null);
        // SASL
        addEngine("SaslClientFactory",                  false, null);
        addEngine("SaslServerFactory",                  false, null);
        // POLICY
        addEngine("Policy",                             false,
                            "java.security.Policy$Parameters");
        // CONFIGURATION
        addEngine("Configuration",                      false,
                            "javax.security.auth.login.Configuration$Parameters");
        // XML DSig
        addEngine("XMLSignatureFactory",                false, null);
        addEngine("KeyInfoFactory",                     false, null);
        addEngine("TransformService",                   false, null);
        // Smart Card I/O
        addEngine("TerminalFactory",                    false,
                            "java.lang.Object");
    }

    // get the "standard" (mixed-case) engine name for arbitary case engine name
    // if there is no known engine by that name, return s
    private static String getEngineName(String s) {
        // try original case first, usually correct
        EngineDescription e = knownEngines.get(s);
        if (e == null) {
            e = knownEngines.get(s.toLowerCase(ENGLISH));
        }
        return (e == null) ? s : e.name;
    }

    /**
     * The description of a security service. It encapsulates the properties
     * of a service and contains a factory method to obtain new implementation
     * instances of this service.
     *
     * <p>Each service has a provider that offers the service, a type,
     * an algorithm name, and the name of the class that implements the
     * service. Optionally, it also includes a list of alternate algorithm
     * names for this service (aliases) and attributes, which are a map of
     * (name, value) String pairs.
     *
     * <p>This class defines the methods {@link #supportsParameter
     * supportsParameter()} and {@link #newInstance newInstance()}
     * which are used by the Java security framework when it searches for
     * suitable services and instantiates them. The valid arguments to those
     * methods depend on the type of service. For the service types defined
     * within Java SE, see the
     * {@extLink security_guide_jca
     * Java Cryptography Architecture (JCA) Reference Guide}
     * for the valid values.
     * Note that components outside of Java SE can define additional types of
     * services and their behavior.
     *
     * <p>Instances of this class are immutable.
     *
     * @since 1.5
     */
    public static class Service {
        private final String type;
        private final String algorithm;
        private String className;
        private final Provider provider;
        private List<String> aliases;
        private Map<UString,String> attributes;
        private final EngineDescription engineDescription;

        // Reference to the cached implementation Class object.
        // Will be a Class if this service is loaded from the built-in
        // classloader (unloading not possible), otherwise a WeakReference to a
        // Class
        private Object classCache;

        // Will be a Constructor if this service is loaded from the built-in
        // classloader (unloading not possible), otherwise a WeakReference to
        // a Constructor
        private Object constructorCache;

        // flag indicating whether this service has its attributes for
        // supportedKeyFormats or supportedKeyClasses set
        // if null, the values have not been initialized
        // if TRUE, at least one of supportedFormats/Classes is non null
        private volatile Boolean hasKeyAttributes;

        // supported encoding formats
        private String[] supportedFormats;

        // names of the supported key (super) classes
        private Class<?>[] supportedClasses;

        // whether this service has been registered with the Provider
        private boolean registered;

        private static final Class<?>[] CLASS0 = new Class<?>[0];

        // this constructor and these methods are used for parsing
        // the legacy string properties.

        private Service(Provider provider, String type, String algorithm) {
            this.provider = provider;
            this.type = type;
            this.algorithm = algorithm;
            engineDescription = knownEngines.get(type);
            aliases = Collections.<String>emptyList();
            attributes = Collections.<UString,String>emptyMap();
        }

        private boolean isValid() {
            return (type != null) && (algorithm != null) && (className != null);
        }

        private void addAlias(String alias) {
            if (aliases.isEmpty()) {
                aliases = new ArrayList<>(2);
            }
            aliases.add(alias);
        }

        void addAttribute(String type, String value) {
            if (attributes.isEmpty()) {
                attributes = new HashMap<>(8);
            }
            attributes.put(new UString(type), value);
        }

        /**
         * Construct a new service.
         *
         * @param provider the provider that offers this service
         * @param type the type of this service
         * @param algorithm the algorithm name
         * @param className the name of the class implementing this service
         * @param aliases List of aliases or null if algorithm has no aliases
         * @param attributes Map of attributes or null if this implementation
         *                   has no attributes
         *
         * @throws NullPointerException if provider, type, algorithm, or
         * className is null
         */
        public Service(Provider provider, String type, String algorithm,
                String className, List<String> aliases,
                Map<String,String> attributes) {
            if ((provider == null) || (type == null) ||
                    (algorithm == null) || (className == null)) {
                throw new NullPointerException();
            }
            this.provider = provider;
            this.type = getEngineName(type);
            engineDescription = knownEngines.get(type);
            this.algorithm = algorithm;
            this.className = className;
            if (aliases == null) {
                this.aliases = Collections.<String>emptyList();
            } else {
                this.aliases = new ArrayList<>(aliases);
            }
            if (attributes == null) {
                this.attributes = Collections.<UString,String>emptyMap();
            } else {
                this.attributes = new HashMap<>();
                for (Map.Entry<String,String> entry : attributes.entrySet()) {
                    this.attributes.put(new UString(entry.getKey()), entry.getValue());
                }
            }
        }

        /**
         * Get the type of this service. For example, {@code MessageDigest}.
         *
         * @return the type of this service
         */
        public final String getType() {
            return type;
        }

        /**
         * Return the name of the algorithm of this service. For example,
         * {@code SHA-1}.
         *
         * @return the algorithm of this service
         */
        public final String getAlgorithm() {
            return algorithm;
        }

        /**
         * Return the Provider of this service.
         *
         * @return the Provider of this service
         */
        public final Provider getProvider() {
            return provider;
        }

        /**
         * Return the name of the class implementing this service.
         *
         * @return the name of the class implementing this service
         */
        public final String getClassName() {
            return className;
        }

        // internal only
        private final List<String> getAliases() {
            return aliases;
        }

        /**
         * Return the value of the specified attribute or null if this
         * attribute is not set for this Service.
         *
         * @param name the name of the requested attribute
         *
         * @return the value of the specified attribute or null if the
         *         attribute is not present
         *
         * @throws NullPointerException if name is null
         */
        public final String getAttribute(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            return attributes.get(new UString(name));
        }

        /**
         * Return a new instance of the implementation described by this
         * service. The security provider framework uses this method to
         * construct implementations. Applications will typically not need
         * to call it.
         *
         * <p>The default implementation uses reflection to invoke the
         * standard constructor for this type of service.
         * Security providers can override this method to implement
         * instantiation in a different way.
         * For details and the values of constructorParameter that are
         * valid for the various types of services see the
         * {@extLink security_guide_jca
         * Java Cryptography Architecture (JCA) Reference Guide}.
         *
         * @param constructorParameter the value to pass to the constructor,
         * or null if this type of service does not use a constructorParameter.
         *
         * @return a new implementation of this service
         *
         * @throws InvalidParameterException if the value of
         * constructorParameter is invalid for this type of service.
         * @throws NoSuchAlgorithmException if instantiation failed for
         * any other reason.
         */
        public Object newInstance(Object constructorParameter)
                throws NoSuchAlgorithmException {
            if (registered == false) {
                if (provider.getService(type, algorithm) != this) {
                    throw new NoSuchAlgorithmException
                        ("Service not registered with Provider "
                        + provider.getName() + ": " + this);
                }
                registered = true;
            }
            Class<?> ctrParamClz;
            try {
                EngineDescription cap = engineDescription;
                if (cap == null) {
                    // unknown engine type, use generic code
                    // this is the code path future for non-core
                    // optional packages
                    ctrParamClz = constructorParameter == null?
                        null : constructorParameter.getClass();
                } else {
                    ctrParamClz = cap.constructorParameterClassName == null?
                        null : Class.forName(cap.constructorParameterClassName);
                    if (constructorParameter != null) {
                        if (ctrParamClz == null) {
                            throw new InvalidParameterException
                                ("constructorParameter not used with " + type
                                + " engines");
                        } else {
                            Class<?> argClass = constructorParameter.getClass();
                            if (ctrParamClz.isAssignableFrom(argClass) == false) {
                                throw new InvalidParameterException
                                    ("constructorParameter must be instanceof "
                                    + cap.constructorParameterClassName.replace('$', '.')
                                    + " for engine type " + type);
                            }
                        }
                    }
                }
                // constructorParameter can be null if not provided
                return newInstanceUtil(ctrParamClz, constructorParameter);
            } catch (NoSuchAlgorithmException e) {
                throw e;
            } catch (InvocationTargetException e) {
                throw new NoSuchAlgorithmException
                    ("Error constructing implementation (algorithm: "
                    + algorithm + ", provider: " + provider.getName()
                    + ", class: " + className + ")", e.getCause());
            } catch (Exception e) {
                throw new NoSuchAlgorithmException
                    ("Error constructing implementation (algorithm: "
                    + algorithm + ", provider: " + provider.getName()
                    + ", class: " + className + ")", e);
            }
        }

        private Object newInstanceOf() throws Exception {
            Constructor<?> con = getDefaultConstructor();
            return con.newInstance(EMPTY);
        }

        private Object newInstanceUtil(Class<?> ctrParamClz, Object ctorParamObj)
                throws Exception
        {
            if (ctrParamClz == null) {
                return newInstanceOf();
            } else {
                // Looking for the constructor with a params first and fallback
                // to one without if not found. This is to support the enhanced
                // SecureRandom where both styles of constructors are supported.
                // Before jdk9, there was no params support (only getInstance(alg))
                // and an impl only had the params-less constructor. Since jdk9,
                // there is getInstance(alg,params) and an impl can contain
                // an Impl(params) constructor.
                try {
                    Constructor<?> con = getImplClass().getConstructor(ctrParamClz);
                    return con.newInstance(ctorParamObj);
                } catch (NoSuchMethodException nsme) {
                    // For pre-jdk9 SecureRandom implementations, they only
                    // have params-less constructors which still works when
                    // the input ctorParamObj is null.
                    //
                    // For other primitives using params, ctorParamObj should not
                    // be null and nsme is thrown, just like before.
                    if (ctorParamObj == null) {
                        try {
                            return newInstanceOf();
                        } catch (NoSuchMethodException nsme2) {
                            nsme.addSuppressed(nsme2);
                            throw nsme;
                        }
                    } else {
                        throw nsme;
                    }
                }
            }
        }

        // return the implementation Class object for this service
        private Class<?> getImplClass() throws NoSuchAlgorithmException {
            try {
                Object cache = classCache;
                if (cache instanceof Class<?> clazz) {
                    return clazz;
                }
                Class<?> clazz = null;
                if (cache instanceof WeakReference<?> ref){
                    clazz = (Class<?>)ref.get();
                }
                if (clazz == null) {
                    ClassLoader cl = provider.getClass().getClassLoader();
                    if (cl == null) {
                        clazz = Class.forName(className);
                    } else {
                        clazz = cl.loadClass(className);
                    }
                    if (!Modifier.isPublic(clazz.getModifiers())) {
                        throw new NoSuchAlgorithmException
                            ("class configured for " + type + " (provider: " +
                            provider.getName() + ") is not public.");
                    }
                    classCache = (cl == null) ? clazz : new WeakReference<Class<?>>(clazz);
                }
                return clazz;
            } catch (ClassNotFoundException e) {
                throw new NoSuchAlgorithmException
                    ("class configured for " + type + " (provider: " +
                    provider.getName() + ") cannot be found.", e);
            }
        }

        private Constructor<?> getDefaultConstructor()
            throws NoSuchAlgorithmException, NoSuchMethodException
        {
            Object cache = constructorCache;
            if (cache instanceof Constructor<?> con) {
                return con;
            }
            Constructor<?> con = null;
            if (cache instanceof WeakReference<?> ref){
                con = (Constructor<?>)ref.get();
            }
            if (con == null) {
                Class<?> clazz = getImplClass();
                con = clazz.getConstructor();
                constructorCache = (clazz.getClassLoader() == null)
                        ? con : new WeakReference<Constructor<?>>(con);
            }
            return con;
        }

        /**
         * Test whether this Service can use the specified parameter.
         * Returns false if this service cannot use the parameter. Returns
         * true if this service can use the parameter, if a fast test is
         * infeasible, or if the status is unknown.
         *
         * <p>The security provider framework uses this method with
         * some types of services to quickly exclude non-matching
         * implementations for consideration.
         * Applications will typically not need to call it.
         *
         * <p>For details and the values of parameter that are valid for the
         * various types of services see the top of this class and the
         * {@extLink security_guide_jca
         * Java Cryptography Architecture (JCA) Reference Guide}.
         * Security providers can override it to implement their own test.
         *
         * @param parameter the parameter to test
         *
         * @return false if this service cannot use the specified
         * parameter; true if it can possibly use the parameter
         *
         * @throws InvalidParameterException if the value of parameter is
         * invalid for this type of service or if this method cannot be
         * used with this type of service
         */
        public boolean supportsParameter(Object parameter) {
            EngineDescription cap = engineDescription;
            if (cap == null) {
                // unknown engine type, return true by default
                return true;
            }
            if (!cap.supportsParameter) {
                throw new InvalidParameterException("supportsParameter() not "
                    + "used with " + type + " engines");
            }
            // allow null for keys without attributes for compatibility
            if ((parameter != null) && (!(parameter instanceof Key))) {
                throw new InvalidParameterException
                    ("Parameter must be instanceof Key for engine " + type);
            }
            if (!hasKeyAttributes()) {
                return true;
            }
            if (parameter == null) {
                return false;
            }
            Key key = (Key)parameter;
            if (supportsKeyFormat(key)) {
                return true;
            }
            if (supportsKeyClass(key)) {
                return true;
            }
            return false;
        }

        /**
         * Return whether this service has its supported properties for
         * keys defined. Parses the attributes if not yet initialized.
         */
        private boolean hasKeyAttributes() {
            Boolean b = hasKeyAttributes;
            if (b == null) {
                synchronized (this) {
                    b = hasKeyAttributes;
                    if (b == null) {
                        String s;
                        s = getAttribute("SupportedKeyFormats");
                        if (s != null) {
                            supportedFormats = s.split("\\|");
                        }
                        s = getAttribute("SupportedKeyClasses");
                        if (s != null) {
                            String[] classNames = s.split("\\|");
                            List<Class<?>> classList =
                                new ArrayList<>(classNames.length);
                            for (String className : classNames) {
                                Class<?> clazz = getKeyClass(className);
                                if (clazz != null) {
                                    classList.add(clazz);
                                }
                            }
                            supportedClasses = classList.toArray(CLASS0);
                        }
                        boolean bool = (supportedFormats != null)
                            || (supportedClasses != null);
                        b = Boolean.valueOf(bool);
                        hasKeyAttributes = b;
                    }
                }
            }
            return b.booleanValue();
        }

        // get the key class object of the specified name
        private Class<?> getKeyClass(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                // ignore
            }
            try {
                ClassLoader cl = provider.getClass().getClassLoader();
                if (cl != null) {
                    return cl.loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                // ignore
            }
            return null;
        }

        private boolean supportsKeyFormat(Key key) {
            if (supportedFormats == null) {
                return false;
            }
            String format = key.getFormat();
            if (format == null) {
                return false;
            }
            for (String supportedFormat : supportedFormats) {
                if (supportedFormat.equals(format)) {
                    return true;
                }
            }
            return false;
        }

        private boolean supportsKeyClass(Key key) {
            if (supportedClasses == null) {
                return false;
            }
            Class<?> keyClass = key.getClass();
            for (Class<?> clazz : supportedClasses) {
                if (clazz.isAssignableFrom(keyClass)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Return a String representation of this service.
         *
         * @return a String representation of this service.
         */
        public String toString() {
            String aString = aliases.isEmpty()
                ? "" : "\r\n  aliases: " + aliases.toString();
            String attrs = attributes.isEmpty()
                ? "" : "\r\n  attributes: " + attributes.toString();
            return provider.getName() + ": " + type + "." + algorithm
                + " -> " + className + aString + attrs + "\r\n";
        }
    }
}
