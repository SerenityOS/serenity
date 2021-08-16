/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.naming.internal;

import java.io.InputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.WeakHashMap;

import javax.naming.*;

/**
  * The ResourceManager class facilitates the reading of JNDI resource files.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  */

public final class ResourceManager {

    /*
     * Name of provider resource files (without the package-name prefix.)
     */
    private static final String PROVIDER_RESOURCE_FILE_NAME =
            "jndiprovider.properties";

    /*
     * Name of application resource files.
     */
    private static final String APP_RESOURCE_FILE_NAME = "jndi.properties";

    /*
     * Name of properties file in <java.home>/conf.
     */
    private static final String JRE_CONF_PROPERTY_FILE_NAME = "jndi.properties";

    /*
     * Internal environment property, that when set to "true", disables
     * application resource files lookup to prevent recursion issues
     * when validating signed JARs.
     */
    private static final String DISABLE_APP_RESOURCE_FILES =
        "com.sun.naming.disable.app.resource.files";

    /*
     * The standard JNDI properties that specify colon-separated lists.
     */
    private static final String[] listProperties = {
        Context.OBJECT_FACTORIES,
        Context.URL_PKG_PREFIXES,
        Context.STATE_FACTORIES,
        // The following shouldn't create a runtime dependence on ldap package.
        javax.naming.ldap.LdapContext.CONTROL_FACTORIES
    };

    private static final VersionHelper helper =
            VersionHelper.getVersionHelper();

    /*
     * A cache of the properties that have been constructed by
     * the ResourceManager.  A Hashtable from a provider resource
     * file is keyed on a class in the resource file's package.
     * One from application resource files is keyed on the thread's
     * context class loader.
     */
    // WeakHashMap<Class | ClassLoader, Hashtable>
    private static final WeakHashMap<Object, Hashtable<? super String, Object>>
            propertiesCache = new WeakHashMap<>(11);

    /*
     * A cache of factory objects (ObjectFactory, StateFactory, ControlFactory).
     *
     * A two-level cache keyed first on context class loader and then
     * on propValue.  Value is a list of class or factory objects,
     * weakly referenced so as not to prevent GC of the class loader.
     * Used in getFactories().
     */
    private static final
        WeakHashMap<ClassLoader, Map<String, List<NamedWeakReference<Object>>>>
            factoryCache = new WeakHashMap<>(11);

    /*
     * A cache of URL factory objects (ObjectFactory).
     *
     * A two-level cache keyed first on context class loader and then
     * on classSuffix+propValue.  Value is the factory itself (weakly
     * referenced so as not to prevent GC of the class loader) or
     * NO_FACTORY if a previous search revealed no factory.  Used in
     * getFactory().
     */
    private static final
        WeakHashMap<ClassLoader, Map<String, WeakReference<Object>>>
            urlFactoryCache = new WeakHashMap<>(11);
    private static final WeakReference<Object> NO_FACTORY =
            new WeakReference<>(null);

    // There should be no instances of this class.
    private ResourceManager() {
    }


    // ---------- Public methods ----------

    /**
     * Given the environment parameter passed to the initial context
     * constructor, returns the full environment for that initial
     * context (never null).  This is based on the environment
     * parameter, the system properties, and all application resource files.
     *
     * <p> This method will modify {@code env} and save
     * a reference to it.  The caller may no longer modify it.
     *
     * @param env       environment passed to initial context constructor.
     *                  Null indicates an empty environment.
     *
     * @throws NamingException if an error occurs while reading a
     *          resource file
     */
    @SuppressWarnings("unchecked")
    public static Hashtable<?, ?> getInitialEnvironment(Hashtable<?, ?> env)
            throws NamingException
    {
        String[] props = VersionHelper.PROPS;   // system properties
        if (env == null) {
            env = new Hashtable<>(11);
        }

        // Merge property values from env param, and system properties.
        // The first value wins: there's no concatenation of
        // colon-separated lists.
        // Read system properties by first trying System.getProperties(),
        // and then trying System.getProperty() if that fails.  The former
        // is more efficient due to fewer permission checks.
        //
        String[] jndiSysProps = helper.getJndiProperties();
        for (int i = 0; i < props.length; i++) {
            Object val = env.get(props[i]);
            if (val == null) {
                // Read system property.
                val = (jndiSysProps != null)
                        ? jndiSysProps[i]
                        : helper.getJndiProperty(i);
            }
            if (val != null) {
                ((Hashtable<String, Object>)env).put(props[i], val);
            }
        }

        // Return without merging if application resource files lookup
        // is disabled.
        String disableAppRes = (String)env.get(DISABLE_APP_RESOURCE_FILES);
        if (disableAppRes != null && disableAppRes.equalsIgnoreCase("true")) {
            return env;
        }

        // Merge the above with the values read from all application
        // resource files.  Colon-separated lists are concatenated.
        mergeTables((Hashtable<Object, Object>)env, getApplicationResources());
        return env;
    }

    /**
      * Retrieves the property from the environment, or from the provider
      * resource file associated with the given context.  The environment
      * may in turn contain values that come from system properties,
      * or application resource files.
      *
      * If {@code concat} is true and both the environment and the provider
      * resource file contain the property, the two values are concatenated
      * (with a ':' separator).
      *
      * Returns null if no value is found.
      *
      * @param propName The non-null property name
      * @param env      The possibly null environment properties
      * @param ctx      The possibly null context
      * @param concat   True if multiple values should be concatenated
      * @return the property value, or null is there is none.
      * @throws NamingException if an error occurs while reading the provider
      * resource file.
      */
    public static String getProperty(String propName, Hashtable<?,?> env,
        Context ctx, boolean concat)
            throws NamingException {

        String val1 = (env != null) ? (String)env.get(propName) : null;
        if ((ctx == null) ||
            ((val1 != null) && !concat)) {
            return val1;
        }
        String val2 = (String)getProviderResource(ctx).get(propName);
        if (val1 == null) {
            return val2;
        } else if ((val2 == null) || !concat) {
            return val1;
        } else {
            return (val1 + ":" + val2);
        }
    }

    /**
     * Retrieves an enumeration of factory classes/object specified by a
     * property.
     *
     * The property is gotten from the environment and the provider
     * resource file associated with the given context and concatenated.
     * See getProperty(). The resulting property value is a list of class names.
     *<p>
     * This method then loads each class using the current thread's context
     * class loader and keeps them in a list. Any class that cannot be loaded
     * is ignored. The resulting list is then cached in a two-level
     * hash table, keyed first by the context class loader and then by
     * the property's value.
     * The next time threads of the same context class loader call this
     * method, they can use the cached list.
     *<p>
     * After obtaining the list either from the cache or by creating one from
     * the property value, this method then creates and returns a
     * FactoryEnumeration using the list. As the FactoryEnumeration is
     * traversed, the cached Class object in the list is instantiated and
     * replaced by an instance of the factory object itself.  Both class
     * objects and factories are wrapped in weak references so as not to
     * prevent GC of the class loader.
     *<p>
     * Note that multiple threads can be accessing the same cached list
     * via FactoryEnumeration, which locks the list during each next().
     * The size of the list will not change,
     * but a cached Class object might be replaced by an instantiated factory
     * object.
     *
     * @param propName  The non-null property name
     * @param env       The possibly null environment properties
     * @param ctx       The possibly null context
     * @return An enumeration of factory classes/objects; null if none.
     * @exception NamingException If encounter problem while reading the provider
     * property file.
     * @see javax.naming.spi.NamingManager#getObjectInstance
     * @see javax.naming.spi.NamingManager#getStateToBind
     * @see javax.naming.spi.DirectoryManager#getObjectInstance
     * @see javax.naming.spi.DirectoryManager#getStateToBind
     * @see javax.naming.ldap.ControlFactory#getControlInstance
     */
    public static FactoryEnumeration getFactories(String propName,
        Hashtable<?,?> env, Context ctx) throws NamingException {

        String facProp = getProperty(propName, env, ctx, true);
        if (facProp == null)
            return null;  // no classes specified; return null

        // Cache is based on context class loader and property val
        ClassLoader loader = helper.getContextClassLoader();

        Map<String, List<NamedWeakReference<Object>>> perLoaderCache = null;
        synchronized (factoryCache) {
            perLoaderCache = factoryCache.get(loader);
            if (perLoaderCache == null) {
                perLoaderCache = new HashMap<>(11);
                factoryCache.put(loader, perLoaderCache);
            }
        }

        synchronized (perLoaderCache) {
            List<NamedWeakReference<Object>> factories =
                    perLoaderCache.get(facProp);
            if (factories != null) {
                // Cached list
                return factories.size() == 0 ? null
                    : new FactoryEnumeration(factories, loader);
            } else {
                // Populate list with classes named in facProp; skipping
                // those that we cannot load
                StringTokenizer parser = new StringTokenizer(facProp, ":");
                factories = new ArrayList<>(5);
                while (parser.hasMoreTokens()) {
                    try {
                        // System.out.println("loading");
                        String className = parser.nextToken();
                        Class<?> c = helper.loadClass(className, loader);
                        factories.add(new NamedWeakReference<Object>(c, className));
                    } catch (Exception e) {
                        // ignore ClassNotFoundException, IllegalArgumentException
                    }
                }
                // System.out.println("adding to cache: " + factories);
                perLoaderCache.put(facProp, factories);
                return new FactoryEnumeration(factories, loader);
            }
        }
    }

    /**
     * Retrieves a factory from a list of packages specified in a
     * property.
     *
     * The property is gotten from the environment and the provider
     * resource file associated with the given context and concatenated.
     * classSuffix is added to the end of this list.
     * See getProperty(). The resulting property value is a list of package
     * prefixes.
     *<p>
     * This method then constructs a list of class names by concatenating
     * each package prefix with classSuffix and attempts to load and
     * instantiate the class until one succeeds.
     * Any class that cannot be loaded is ignored.
     * The resulting object is then cached in a two-level hash table,
     * keyed first by the context class loader and then by the property's
     * value and classSuffix.
     * The next time threads of the same context class loader call this
     * method, they use the cached factory.
     * If no factory can be loaded, NO_FACTORY is recorded in the table
     * so that next time it'll return quickly.
     *
     * @param propName  The non-null property name
     * @param env       The possibly null environment properties
     * @param ctx       The possibly null context
     * @param classSuffix The non-null class name
     *                  (e.g. ".ldap.ldapURLContextFactory).
     * @param defaultPkgPrefix The non-null default package prefix.
     *        (e.g., "com.sun.jndi.url").
     * @return An factory object; null if none.
     * @exception NamingException If encounter problem while reading the provider
     * property file, or problem instantiating the factory.
     *
     * @see javax.naming.spi.NamingManager#getURLContext
     * @see javax.naming.spi.NamingManager#getURLObject
     */
    public static Object getFactory(String propName, Hashtable<?,?> env,
            Context ctx, String classSuffix, String defaultPkgPrefix)
            throws NamingException {

        // Merge property with provider property and supplied default
        String facProp = getProperty(propName, env, ctx, true);
        if (facProp != null)
            facProp += (":" + defaultPkgPrefix);
        else
            facProp = defaultPkgPrefix;

        // Cache factory based on context class loader, class name, and
        // property val
        ClassLoader loader = helper.getContextClassLoader();
        String key = classSuffix + " " + facProp;

        Map<String, WeakReference<Object>> perLoaderCache = null;
        synchronized (urlFactoryCache) {
            perLoaderCache = urlFactoryCache.get(loader);
            if (perLoaderCache == null) {
                perLoaderCache = new HashMap<>(11);
                urlFactoryCache.put(loader, perLoaderCache);
            }
        }

        synchronized (perLoaderCache) {
            Object factory = null;

            WeakReference<Object> factoryRef = perLoaderCache.get(key);
            if (factoryRef == NO_FACTORY) {
                return null;
            } else if (factoryRef != null) {
                factory = factoryRef.get();
                if (factory != null) {  // check if weak ref has been cleared
                    return factory;
                }
            }

            // Not cached; find first factory and cache
            StringTokenizer parser = new StringTokenizer(facProp, ":");
            String className;
            while (factory == null && parser.hasMoreTokens()) {
                className = parser.nextToken() + classSuffix;
                try {
                    // System.out.println("loading " + className);
                    @SuppressWarnings("deprecation") // Class.newInstance
                    Object tmp = helper.loadClass(className, loader).newInstance();
                    factory = tmp;
                } catch (InstantiationException e) {
                    NamingException ne =
                        new NamingException("Cannot instantiate " + className);
                    ne.setRootCause(e);
                    throw ne;
                } catch (IllegalAccessException e) {
                    NamingException ne =
                        new NamingException("Cannot access " + className);
                    ne.setRootCause(e);
                    throw ne;
                } catch (Exception e) {
                    // ignore ClassNotFoundException, IllegalArgumentException,
                    // etc.
                }
            }

            // Cache it.
            perLoaderCache.put(key, (factory != null)
                                        ? new WeakReference<>(factory)
                                        : NO_FACTORY);
            return factory;
        }
    }


    // ---------- Private methods ----------

    /*
     * Returns the properties contained in the provider resource file
     * of an object's package.  Returns an empty hash table if the
     * object is null or the resource file cannot be found.  The
     * results are cached.
     *
     * @throws NamingException if an error occurs while reading the file.
     */
    private static Hashtable<? super String, Object>
        getProviderResource(Object obj)
            throws NamingException
    {
        if (obj == null) {
            return (new Hashtable<>(1));
        }
        synchronized (propertiesCache) {
            Class<?> c = obj.getClass();

            Hashtable<? super String, Object> props =
                    propertiesCache.get(c);
            if (props != null) {
                return props;
            }
            props = new Properties();

            InputStream istream =
                helper.getResourceAsStream(c, PROVIDER_RESOURCE_FILE_NAME);

            if (istream != null) {
                try {
                    ((Properties)props).load(istream);
                } catch (IOException e) {
                    NamingException ne = new ConfigurationException(
                            "Error reading provider resource file for " + c);
                    ne.setRootCause(e);
                    throw ne;
                }
            }
            propertiesCache.put(c, props);
            return props;
        }
    }


    /*
     * Returns the Hashtable (never null) that results from merging
     * all application resource files available to this thread's
     * context class loader.  The properties file in <java.home>/conf
     * is also merged in.  The results are cached.
     *
     * SECURITY NOTES:
     * 1.  JNDI needs permission to read the application resource files.
     * 2.  Any class will be able to use JNDI to view the contents of
     * the application resource files in its own classpath.  Give
     * careful consideration to this before storing sensitive
     * information there.
     *
     * @throws NamingException if an error occurs while reading a resource
     *  file.
     */
    private static Hashtable<? super String, Object> getApplicationResources()
            throws NamingException {

        ClassLoader cl = helper.getContextClassLoader();

        synchronized (propertiesCache) {
            Hashtable<? super String, Object> result = propertiesCache.get(cl);
            if (result != null) {
                return result;
            }

            try {
                NamingEnumeration<InputStream> resources =
                    helper.getResources(cl, APP_RESOURCE_FILE_NAME);
                try {
                    while (resources.hasMore()) {
                        Properties props = new Properties();
                        InputStream istream = resources.next();
                        try {
                            props.load(istream);
                        } finally {
                            istream.close();
                        }

                        if (result == null) {
                            result = props;
                        } else {
                            mergeTables(result, props);
                        }
                    }
                } finally {
                    while (resources.hasMore()) {
                        resources.next().close();
                    }
                }

                // Merge in properties from file in <java.home>/conf.
                InputStream istream =
                    helper.getJavaHomeConfStream(JRE_CONF_PROPERTY_FILE_NAME);
                if (istream != null) {
                    try {
                        Properties props = new Properties();
                        props.load(istream);

                        if (result == null) {
                            result = props;
                        } else {
                            mergeTables(result, props);
                        }
                    } finally {
                        istream.close();
                    }
                }

            } catch (IOException e) {
                NamingException ne = new ConfigurationException(
                        "Error reading application resource file");
                ne.setRootCause(e);
                throw ne;
            }
            if (result == null) {
                result = new Hashtable<>(11);
            }
            propertiesCache.put(cl, result);
            return result;
        }
    }

    /*
     * Merge the properties from one hash table into another.  Each
     * property in props2 that is not in props1 is added to props1.
     * For each property in both hash tables that is one of the
     * standard JNDI properties that specify colon-separated lists,
     * the values are concatenated and stored in props1.
     */
    private static void mergeTables(Hashtable<? super String, Object> props1,
                                    Hashtable<? super String, Object> props2) {
        for (Object key : props2.keySet()) {
            String prop = (String)key;
            Object val1 = props1.get(prop);
            if (val1 == null) {
                props1.put(prop, props2.get(prop));
            } else if (isListProperty(prop)) {
                String val2 = (String)props2.get(prop);
                props1.put(prop, ((String)val1) + ":" + val2);
            }
        }
    }

    /*
     * Is a property one of the standard JNDI properties that specify
     * colon-separated lists?
     */
    private static boolean isListProperty(String prop) {
        prop = prop.intern();
        for (int i = 0; i < listProperties.length; i++) {
            if (prop == listProperties[i]) {
                return true;
            }
        }
        return false;
    }
}
