/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.spi;

import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import javax.naming.*;

import com.sun.naming.internal.ObjectFactoriesFilter;
import com.sun.naming.internal.VersionHelper;
import com.sun.naming.internal.ResourceManager;
import com.sun.naming.internal.FactoryEnumeration;
import jdk.internal.loader.ClassLoaderValue;

/**
 * This class contains methods for creating context objects
 * and objects referred to by location information in the naming
 * or directory service.
 *<p>
 * This class cannot be instantiated.  It has only static methods.
 *<p>
 * The mention of URL in the documentation for this class refers to
 * a URL string as defined by RFC 1738 and its related RFCs. It is
 * any string that conforms to the syntax described therein, and
 * may not always have corresponding support in the java.net.URL
 * class or Web browsers.
 *<p>
 * NamingManager is safe for concurrent access by multiple threads.
 *<p>
 * Except as otherwise noted,
 * a {@code Name} or environment parameter
 * passed to any method is owned by the caller.
 * The implementation will not modify the object or keep a reference
 * to it, although it may keep a reference to a clone or copy.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 * @since 1.3
 */

public class NamingManager {

    /*
     * Disallow anyone from creating one of these.
     * Made package private so that DirectoryManager can subclass.
     */

    NamingManager() {}

    // should be protected and package private
    static final VersionHelper helper = VersionHelper.getVersionHelper();

// --------- object factory stuff

    /**
     * Package-private; used by DirectoryManager and NamingManager.
     */
    private static ObjectFactoryBuilder object_factory_builder = null;

    private static final ClassLoaderValue<InitialContextFactory> FACTORIES_CACHE =
            new ClassLoaderValue<>();

    /**
     * The ObjectFactoryBuilder determines the policy used when
     * trying to load object factories.
     * See getObjectInstance() and class ObjectFactory for a description
     * of the default policy.
     * setObjectFactoryBuilder() overrides this default policy by installing
     * an ObjectFactoryBuilder. Subsequent object factories will
     * be loaded and created using the installed builder.
     *<p>
     * The builder can only be installed if the executing thread is allowed
     * (by the security manager's checkSetFactory() method) to do so.
     * Once installed, the builder cannot be replaced.
     *
     * @param builder The factory builder to install. If null, no builder
     *                  is installed.
     * @throws SecurityException builder cannot be installed
     *         for security reasons.
     * @throws NamingException builder cannot be installed for
     *         a non-security-related reason.
     * @throws IllegalStateException If a factory has already been installed.
     * @see #getObjectInstance
     * @see ObjectFactory
     * @see ObjectFactoryBuilder
     * @see java.lang.SecurityManager#checkSetFactory
     */
    public static synchronized void setObjectFactoryBuilder(
            ObjectFactoryBuilder builder) throws NamingException {
        if (object_factory_builder != null)
            throw new IllegalStateException("ObjectFactoryBuilder already set");

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        object_factory_builder = builder;
    }

    /**
     * Used for accessing object factory builder.
     */
    static synchronized ObjectFactoryBuilder getObjectFactoryBuilder() {
        return object_factory_builder;
    }


    /**
     * Retrieves the ObjectFactory for the object identified by a reference,
     * using the reference's factory class name and factory codebase
     * to load in the factory's class.
     * @param ref The non-null reference to use.
     * @param factoryName The non-null class name of the factory.
     * @return The object factory for the object identified by ref; null
     * if unable to load the factory.
     */
    static ObjectFactory getObjectFactoryFromReference(
        Reference ref, String factoryName)
        throws IllegalAccessException,
        InstantiationException,
        MalformedURLException {
        Class<?> clas = null;

        // Try to use current class loader
        try {
            clas = helper.loadClassWithoutInit(factoryName);
            // Validate factory's class with the objects factory serial filter
            if (!ObjectFactoriesFilter.canInstantiateObjectsFactory(clas)) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            // ignore and continue
            // e.printStackTrace();
        }
        // All other exceptions are passed up.

        // Not in class path; try to use codebase
        String codebase;
        if (clas == null &&
                (codebase = ref.getFactoryClassLocation()) != null) {
            try {
                clas = helper.loadClass(factoryName, codebase);
                // Validate factory's class with the objects factory serial filter
                if (clas == null ||
                    !ObjectFactoriesFilter.canInstantiateObjectsFactory(clas)) {
                    return null;
                }
            } catch (ClassNotFoundException e) {
            }
        }

        @SuppressWarnings("deprecation") // Class.newInstance
        ObjectFactory result = (clas != null) ? (ObjectFactory) clas.newInstance() : null;
        return result;
    }


    /**
     * Creates an object using the factories specified in the
     * {@code Context.OBJECT_FACTORIES} property of the environment
     * or of the provider resource file associated with {@code nameCtx}.
     *
     * @return factory created; null if cannot create
     */
    private static Object createObjectFromFactories(Object obj, Name name,
            Context nameCtx, Hashtable<?,?> environment) throws Exception {

        FactoryEnumeration factories = ResourceManager.getFactories(
            Context.OBJECT_FACTORIES, environment, nameCtx);

        if (factories == null)
            return null;

        // Try each factory until one succeeds
        ObjectFactory factory;
        Object answer = null;
        while (answer == null && factories.hasMore()) {
            factory = (ObjectFactory)factories.next();
            answer = factory.getObjectInstance(obj, name, nameCtx, environment);
        }
        return answer;
    }

    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
            return str.substring(0, colon_posn);
        return null;
    }

    /**
     * Creates an instance of an object for the specified object
     * and environment.
     * <p>
     * If an object factory builder has been installed, it is used to
     * create a factory for creating the object.
     * Otherwise, the following rules are used to create the object:
     *<ol>
     * <li>If {@code refInfo} is a {@code Reference}
     *    or {@code Referenceable} containing a factory class name,
     *    use the named factory to create the object.
     *    Return {@code refInfo} if the factory cannot be created.
     *    Under JDK 1.1, if the factory class must be loaded from a location
     *    specified in the reference, a {@code SecurityManager} must have
     *    been installed or the factory creation will fail.
     *    If an exception is encountered while creating the factory,
     *    it is passed up to the caller.
     * <li>If {@code refInfo} is a {@code Reference} or
     *    {@code Referenceable} with no factory class name,
     *    and the address or addresses are {@code StringRefAddr}s with
     *    address type "URL",
     *    try the URL context factory corresponding to each URL's scheme id
     *    to create the object (see {@code getURLContext()}).
     *    If that fails, continue to the next step.
     * <li> Use the object factories specified in
     *    the {@code Context.OBJECT_FACTORIES} property of the environment,
     *    and of the provider resource file associated with
     *    {@code nameCtx}, in that order.
     *    The value of this property is a colon-separated list of factory
     *    class names that are tried in order, and the first one that succeeds
     *    in creating an object is the one used.
     *    If none of the factories can be loaded,
     *    return {@code refInfo}.
     *    If an exception is encountered while creating the object, the
     *    exception is passed up to the caller.
     *</ol>
     *<p>
     * Service providers that implement the {@code DirContext}
     * interface should use
     * {@code DirectoryManager.getObjectInstance()}, not this method.
     * Service providers that implement only the {@code Context}
     * interface should use this method.
     * <p>
     * Note that an object factory (an object that implements the ObjectFactory
     * interface) must be public and must have a public constructor that
     * accepts no arguments.
     * In cases where the factory is in a named module then it must be in a
     * package which is exported by that module to the {@code java.naming}
     * module.
     * <p>
     * The {@code name} and {@code nameCtx} parameters may
     * optionally be used to specify the name of the object being created.
     * {@code name} is the name of the object, relative to context
     * {@code nameCtx}.  This information could be useful to the object
     * factory or to the object implementation.
     *  If there are several possible contexts from which the object
     *  could be named -- as will often be the case -- it is up to
     *  the caller to select one.  A good rule of thumb is to select the
     * "deepest" context available.
     * If {@code nameCtx} is null, {@code name} is relative
     * to the default initial context.  If no name is being specified, the
     * {@code name} parameter should be null.
     *
     * @param refInfo The possibly null object for which to create an object.
     * @param name The name of this object relative to {@code nameCtx}.
     *          Specifying a name is optional; if it is
     *          omitted, {@code name} should be null.
     * @param nameCtx The context relative to which the {@code name}
     *          parameter is specified.  If null, {@code name} is
     *          relative to the default initial context.
     * @param environment The possibly null environment to
     *          be used in the creation of the object factory and the object.
     * @return An object created using {@code refInfo}; or
     *          {@code refInfo} if an object cannot be created using
     *          the algorithm described above.
     * @throws NamingException if a naming exception was encountered
     *  while attempting to get a URL context, or if one of the
     *          factories accessed throws a NamingException.
     * @throws Exception if one of the factories accessed throws an
     *          exception, or if an error was encountered while loading
     *          and instantiating the factory and object classes.
     *          A factory should only throw an exception if it does not want
     *          other factories to be used in an attempt to create an object.
     *  See ObjectFactory.getObjectInstance().
     * @see #getURLContext
     * @see ObjectFactory
     * @see ObjectFactory#getObjectInstance
     */
    public static Object
        getObjectInstance(Object refInfo, Name name, Context nameCtx,
                          Hashtable<?,?> environment)
        throws Exception
    {

        ObjectFactory factory;

        // Use builder if installed
        ObjectFactoryBuilder builder = getObjectFactoryBuilder();
        if (builder != null) {
            // builder must return non-null factory
            factory = builder.createObjectFactory(refInfo, environment);
            return factory.getObjectInstance(refInfo, name, nameCtx,
                environment);
        }

        // Use reference if possible
        Reference ref = null;
        if (refInfo instanceof Reference) {
            ref = (Reference) refInfo;
        } else if (refInfo instanceof Referenceable) {
            ref = ((Referenceable)(refInfo)).getReference();
        }

        Object answer;

        if (ref != null) {
            String f = ref.getFactoryClassName();
            if (f != null) {
                // if reference identifies a factory, use exclusively

                factory = getObjectFactoryFromReference(ref, f);
                if (factory != null) {
                    return factory.getObjectInstance(ref, name, nameCtx,
                                                     environment);
                }
                // No factory found, so return original refInfo.
                // Will reach this point if factory class is not in
                // class path and reference does not contain a URL for it
                return refInfo;

            } else {
                // if reference has no factory, check for addresses
                // containing URLs

                answer = processURLAddrs(ref, name, nameCtx, environment);
                if (answer != null) {
                    return answer;
                }
            }
        }

        // try using any specified factories
        answer =
            createObjectFromFactories(refInfo, name, nameCtx, environment);
        return (answer != null) ? answer : refInfo;
    }

    /*
     * Ref has no factory.  For each address of type "URL", try its URL
     * context factory.  Returns null if unsuccessful in creating and
     * invoking a factory.
     */
    static Object processURLAddrs(Reference ref, Name name, Context nameCtx,
                                  Hashtable<?,?> environment)
            throws NamingException {

        for (int i = 0; i < ref.size(); i++) {
            RefAddr addr = ref.get(i);
            if (addr instanceof StringRefAddr &&
                addr.getType().equalsIgnoreCase("URL")) {

                String url = (String)addr.getContent();
                Object answer = processURL(url, name, nameCtx, environment);
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
    }

    private static Object processURL(Object refInfo, Name name,
                                     Context nameCtx, Hashtable<?,?> environment)
            throws NamingException {
        Object answer;

        // If refInfo is a URL string, try to use its URL context factory
        // If no context found, continue to try object factories.
        if (refInfo instanceof String) {
            String url = (String)refInfo;
            String scheme = getURLScheme(url);
            if (scheme != null) {
                answer = getURLObject(scheme, refInfo, name, nameCtx,
                                      environment);
                if (answer != null) {
                    return answer;
                }
            }
        }

        // If refInfo is an array of URL strings,
        // try to find a context factory for any one of its URLs.
        // If no context found, continue to try object factories.
        if (refInfo instanceof String[]) {
            String[] urls = (String[])refInfo;
            for (int i = 0; i <urls.length; i++) {
                String scheme = getURLScheme(urls[i]);
                if (scheme != null) {
                    answer = getURLObject(scheme, refInfo, name, nameCtx,
                                          environment);
                    if (answer != null)
                        return answer;
                }
            }
        }
        return null;
    }


    /**
     * Retrieves a context identified by {@code obj}, using the specified
     * environment.
     * Used by ContinuationContext.
     *
     * @param obj       The object identifying the context.
     * @param name      The name of the context being returned, relative to
     *                  {@code nameCtx}, or null if no name is being
     *                  specified.
     *                  See the {@code getObjectInstance} method for
     *                  details.
     * @param nameCtx   The context relative to which {@code name} is
     *                  specified, or null for the default initial context.
     *                  See the {@code getObjectInstance} method for
     *                  details.
     * @param environment Environment specifying characteristics of the
     *                  resulting context.
     * @return A context identified by {@code obj}.
     *
     * @see #getObjectInstance
     */
    static Context getContext(Object obj, Name name, Context nameCtx,
                              Hashtable<?,?> environment) throws NamingException {
        Object answer;

        if (obj instanceof Context) {
            // %%% Ignore environment for now.  OK since method not public.
            return (Context)obj;
        }

        try {
            answer = getObjectInstance(obj, name, nameCtx, environment);
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }

        return (answer instanceof Context)
            ? (Context)answer
            : null;
    }

    // Used by ContinuationContext
    static Resolver getResolver(Object obj, Name name, Context nameCtx,
                                Hashtable<?,?> environment) throws NamingException {
        Object answer;

        if (obj instanceof Resolver) {
            // %%% Ignore environment for now.  OK since method not public.
            return (Resolver)obj;
        }

        try {
            answer = getObjectInstance(obj, name, nameCtx, environment);
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }

        return (answer instanceof Resolver)
            ? (Resolver)answer
            : null;
    }


    /***************** URL Context implementations ***************/

    /**
     * Creates a context for the given URL scheme id.
     * <p>
     * The resulting context is for resolving URLs of the
     * scheme {@code scheme}. The resulting context is not tied
     * to a specific URL. It is able to handle arbitrary URLs with
     * the specified scheme.
     *<p>
     * The class name of the factory that creates the resulting context
     * has the naming convention <i>scheme-id</i>URLContextFactory
     * (e.g. "ftpURLContextFactory" for the "ftp" scheme-id),
     * in the package specified as follows.
     * The {@code Context.URL_PKG_PREFIXES} environment property (which
     * may contain values taken from system properties,
     * or application resource files)
     * contains a colon-separated list of package prefixes.
     * Each package prefix in
     * the property is tried in the order specified to load the factory class.
     * The default package prefix is "com.sun.jndi.url" (if none of the
     * specified packages work, this default is tried).
     * The complete package name is constructed using the package prefix,
     * concatenated with the scheme id.
     *<p>
     * For example, if the scheme id is "ldap", and the
     * {@code Context.URL_PKG_PREFIXES} property
     * contains "com.widget:com.wiz.jndi",
     * the naming manager would attempt to load the following classes
     * until one is successfully instantiated:
     *<ul>
     * <li>com.widget.ldap.ldapURLContextFactory
     *  <li>com.wiz.jndi.ldap.ldapURLContextFactory
     *  <li>com.sun.jndi.url.ldap.ldapURLContextFactory
     *</ul>
     * If none of the package prefixes work, null is returned.
     *<p>
     * If a factory is instantiated, it is invoked with the following
     * parameters to produce the resulting context.
     * <p>
     * {@code factory.getObjectInstance(null, environment);}
     * <p>
     * For example, invoking getObjectInstance() as shown above
     * on a LDAP URL context factory would return a
     * context that can resolve LDAP urls
     * (e.g. "ldap://ldap.wiz.com/o=wiz,c=us",
     * "ldap://ldap.umich.edu/o=umich,c=us", ...).
     *<p>
     * Note that an object factory (an object that implements the ObjectFactory
     * interface) must be public and must have a public constructor that
     * accepts no arguments.
     * In cases where the factory is in a named module then it must be in a
     * package which is exported by that module to the {@code java.naming}
     * module.
     *
     * @param scheme    The non-null scheme-id of the URLs supported by the context.
     * @param environment The possibly null environment properties to be
     *           used in the creation of the object factory and the context.
     * @return A context for resolving URLs with the
     *         scheme id {@code scheme};
     *  {@code null} if the factory for creating the
     *         context is not found.
     * @throws NamingException If a naming exception occurs while creating
     *         the context.
     * @see #getObjectInstance
     * @see ObjectFactory#getObjectInstance
     */
    public static Context getURLContext(String scheme,
                                        Hashtable<?,?> environment)
        throws NamingException
    {
        // pass in 'null' to indicate creation of generic context for scheme
        // (i.e. not specific to a URL).

            Object answer = getURLObject(scheme, null, null, null, environment);
            if (answer instanceof Context) {
                return (Context)answer;
            } else {
                return null;
            }
    }

    private static final String defaultPkgPrefix = "com.sun.jndi.url";

    /**
     * Creates an object for the given URL scheme id using
     * the supplied urlInfo.
     * <p>
     * If urlInfo is null, the result is a context for resolving URLs
     * with the scheme id 'scheme'.
     * If urlInfo is a URL, the result is a context named by the URL.
     * Names passed to this context is assumed to be relative to this
     * context (i.e. not a URL). For example, if urlInfo is
     * "ldap://ldap.wiz.com/o=Wiz,c=us", the resulting context will
     * be that pointed to by "o=Wiz,c=us" on the server 'ldap.wiz.com'.
     * Subsequent names that can be passed to this context will be
     * LDAP names relative to this context (e.g. cn="Barbs Jensen").
     * If urlInfo is an array of URLs, the URLs are assumed
     * to be equivalent in terms of the context to which they refer.
     * The resulting context is like that of the single URL case.
     * If urlInfo is of any other type, that is handled by the
     * context factory for the URL scheme.
     * @param scheme the URL scheme id for the context
     * @param urlInfo information used to create the context
     * @param name name of this object relative to {@code nameCtx}
     * @param nameCtx Context whose provider resource file will be searched
     *          for package prefix values (or null if none)
     * @param environment Environment properties for creating the context
     * @see javax.naming.InitialContext
     */
    private static Object getURLObject(String scheme, Object urlInfo,
                                       Name name, Context nameCtx,
                                       Hashtable<?,?> environment)
            throws NamingException {

        // e.g. "ftpURLContextFactory"
        ObjectFactory factory = (ObjectFactory)ResourceManager.getFactory(
            Context.URL_PKG_PREFIXES, environment, nameCtx,
            "." + scheme + "." + scheme + "URLContextFactory", defaultPkgPrefix);

        if (factory == null)
          return null;

        // Found object factory
        try {
            return factory.getObjectInstance(urlInfo, name, nameCtx, environment);
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }

    }


// ------------ Initial Context Factory Stuff
    private static InitialContextFactoryBuilder initctx_factory_builder = null;

    /**
     * Use this method for accessing initctx_factory_builder while
     * inside an unsynchronized method.
     */
    private static synchronized InitialContextFactoryBuilder
    getInitialContextFactoryBuilder() {
        return initctx_factory_builder;
    }

    /**
     * Creates an initial context using the specified environment
     * properties.
     * <p>
     * This is done as follows:
     * <ul>
     * <li>If an InitialContextFactoryBuilder has been installed,
     *     it is used to create the factory for creating the initial
     *     context</li>
     * <li>Otherwise, the class specified in the
     *     {@code Context.INITIAL_CONTEXT_FACTORY} environment property
     *     is used
     *     <ul>
     *     <li>First, the {@linkplain java.util.ServiceLoader ServiceLoader}
     *         mechanism tries to locate an {@code InitialContextFactory}
     *         provider using the current thread's context class loader</li>
     *     <li>Failing that, this implementation tries to locate a suitable
     *         {@code InitialContextFactory} using a built-in mechanism
     *         <br>
     *         (Note that an initial context factory (an object that implements
     *         the InitialContextFactory interface) must be public and must have
     *         a public constructor that accepts no arguments.
     *         In cases where the factory is in a named module then it must
     *         be in a package which is exported by that module to the
     *         {@code java.naming} module.)</li>
     *     </ul>
     * </li>
     * </ul>
     * @param env The possibly null environment properties used when
     *                  creating the context.
     * @return A non-null initial context.
     * @throws NoInitialContextException If the
     *          {@code Context.INITIAL_CONTEXT_FACTORY} property
     *         is not found or names a nonexistent
     *         class or a class that cannot be instantiated,
     *          or if the initial context could not be created for some other
     *          reason.
     * @throws NamingException If some other naming exception was encountered.
     * @see javax.naming.InitialContext
     * @see javax.naming.directory.InitialDirContext
     */
    @SuppressWarnings("removal")
    public static Context getInitialContext(Hashtable<?,?> env)
        throws NamingException {
        ClassLoader loader;
        InitialContextFactory factory = null;

        InitialContextFactoryBuilder builder = getInitialContextFactoryBuilder();
        if (builder == null) {
            // No builder installed, use property
            // Get initial context factory class name

            String className = env != null ?
                (String)env.get(Context.INITIAL_CONTEXT_FACTORY) : null;
            if (className == null) {
                NoInitialContextException ne = new NoInitialContextException(
                    "Need to specify class name in environment or system " +
                    "property, or in an application resource file: " +
                    Context.INITIAL_CONTEXT_FACTORY);
                throw ne;
            }

            if (System.getSecurityManager() == null) {
                loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) loader = ClassLoader.getSystemClassLoader();
            } else {
                PrivilegedAction<ClassLoader> pa = () -> {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    return (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
                };
                loader = AccessController.doPrivileged(pa);
            }

            var key = FACTORIES_CACHE.sub(className);
            try {
                factory = key.computeIfAbsent(loader, (ld, ky) -> getFactory(ky.key()));
            } catch (FactoryInitializationError e) {
                throw e.getCause();
            }
        } else {
            factory = builder.createInitialContextFactory(env);
        }

        return factory.getInitialContext(env);
    }

    private static InitialContextFactory getFactory(String className) {
        InitialContextFactory factory;
        try {
            ServiceLoader<InitialContextFactory> loader =
                    ServiceLoader.load(InitialContextFactory.class);

            factory = loader
                    .stream()
                    .filter(p -> p.type().getName().equals(className))
                    .findFirst()
                    .map(ServiceLoader.Provider::get)
                    .orElse(null);
        } catch (ServiceConfigurationError e) {
            NoInitialContextException ne =
                    new NoInitialContextException(
                            "Cannot load initial context factory "
                                    + "'" + className + "'");
            ne.setRootCause(e);
            throw new FactoryInitializationError(ne);
        }

        if (factory == null) {
            try {
                @SuppressWarnings("deprecation")
                Object o = helper.loadClass(className).newInstance();
                factory = (InitialContextFactory) o;
            } catch (Exception e) {
                NoInitialContextException ne =
                        new NoInitialContextException(
                                "Cannot instantiate class: " + className);
                ne.setRootCause(e);
                throw new FactoryInitializationError(ne);
            }
        }
        return factory;
    }


    /**
     * Sets the InitialContextFactory builder to be builder.
     *
     *<p>
     * The builder can only be installed if the executing thread is allowed by
     * the security manager to do so. Once installed, the builder cannot
     * be replaced.
     * @param builder The initial context factory builder to install. If null,
     *                no builder is set.
     * @throws SecurityException builder cannot be installed for security
     *         reasons.
     * @throws NamingException builder cannot be installed for
     *         a non-security-related reason.
     * @throws IllegalStateException If a builder was previous installed.
     * @see #hasInitialContextFactoryBuilder
     * @see java.lang.SecurityManager#checkSetFactory
     */
    public static synchronized void setInitialContextFactoryBuilder(
        InitialContextFactoryBuilder builder)
        throws NamingException {
            if (initctx_factory_builder != null)
                throw new IllegalStateException(
                    "InitialContextFactoryBuilder already set");

            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkSetFactory();
            }
            initctx_factory_builder = builder;
    }

    /**
     * Determines whether an initial context factory builder has
     * been set.
     * @return true if an initial context factory builder has
     *           been set; false otherwise.
     * @see #setInitialContextFactoryBuilder
     */
    public static boolean hasInitialContextFactoryBuilder() {
        return (getInitialContextFactoryBuilder() != null);
    }

// -----  Continuation Context Stuff

    /**
     * Constant that holds the name of the environment property into
     * which {@code getContinuationContext()} stores the value of its
     * {@code CannotProceedException} parameter.
     * This property is inherited by the continuation context, and may
     * be used by that context's service provider to inspect the
     * fields of the exception.
     *<p>
     * The value of this constant is "java.naming.spi.CannotProceedException".
     *
     * @see #getContinuationContext
     * @since 1.3
     */
    public static final String CPE = "java.naming.spi.CannotProceedException";

    /**
     * Creates a context in which to continue a context operation.
     *<p>
     * In performing an operation on a name that spans multiple
     * namespaces, a context from one naming system may need to pass
     * the operation on to the next naming system.  The context
     * implementation does this by first constructing a
     * {@code CannotProceedException} containing information
     * pinpointing how far it has proceeded.  It then obtains a
     * continuation context from JNDI by calling
     * {@code getContinuationContext}.  The context
     * implementation should then resume the context operation by
     * invoking the same operation on the continuation context, using
     * the remainder of the name that has not yet been resolved.
     *<p>
     * Before making use of the {@code cpe} parameter, this method
     * updates the environment associated with that object by setting
     * the value of the property <a href="#CPE">{@code CPE}</a>
     * to {@code cpe}.  This property will be inherited by the
     * continuation context, and may be used by that context's
     * service provider to inspect the fields of this exception.
     *
     * @param cpe
     *          The non-null exception that triggered this continuation.
     * @return A non-null Context object for continuing the operation.
     * @throws NamingException If a naming exception occurred.
     */
    @SuppressWarnings("unchecked")
    public static Context getContinuationContext(CannotProceedException cpe)
            throws NamingException {

        Hashtable<Object,Object> env = (Hashtable<Object,Object>)cpe.getEnvironment();
        if (env == null) {
            env = new Hashtable<>(7);
        } else {
            // Make a (shallow) copy of the environment.
            env = (Hashtable<Object,Object>)env.clone();
        }
        env.put(CPE, cpe);

        ContinuationContext cctx = new ContinuationContext(cpe, env);
        return cctx.getTargetContext();
    }

// ------------ State Factory Stuff

    /**
     * Retrieves the state of an object for binding.
     * <p>
     * Service providers that implement the {@code DirContext} interface
     * should use {@code DirectoryManager.getStateToBind()}, not this method.
     * Service providers that implement only the {@code Context} interface
     * should use this method.
     *<p>
     * This method uses the specified state factories in
     * the {@code Context.STATE_FACTORIES} property from the environment
     * properties, and from the provider resource file associated with
     * {@code nameCtx}, in that order.
     *    The value of this property is a colon-separated list of factory
     *    class names that are tried in order, and the first one that succeeds
     *    in returning the object's state is the one used.
     * If no object's state can be retrieved in this way, return the
     * object itself.
     *    If an exception is encountered while retrieving the state, the
     *    exception is passed up to the caller.
     * <p>
     * Note that a state factory
     * (an object that implements the StateFactory
     * interface) must be public and must have a public constructor that
     * accepts no arguments.
     * In cases where the factory is in a named module then it must be in a
     * package which is exported by that module to the {@code java.naming}
     * module.
     * <p>
     * The {@code name} and {@code nameCtx} parameters may
     * optionally be used to specify the name of the object being created.
     * See the description of "Name and Context Parameters" in
     * {@link ObjectFactory#getObjectInstance
     *          ObjectFactory.getObjectInstance()}
     * for details.
     * <p>
     * This method may return a {@code Referenceable} object.  The
     * service provider obtaining this object may choose to store it
     * directly, or to extract its reference (using
     * {@code Referenceable.getReference()}) and store that instead.
     *
     * @param obj The non-null object for which to get state to bind.
     * @param name The name of this object relative to {@code nameCtx},
     *          or null if no name is specified.
     * @param nameCtx The context relative to which the {@code name}
     *          parameter is specified, or null if {@code name} is
     *          relative to the default initial context.
     *  @param environment The possibly null environment to
     *          be used in the creation of the state factory and
     *  the object's state.
     * @return The non-null object representing {@code obj}'s state for
     *  binding.  It could be the object ({@code obj}) itself.
     * @throws NamingException If one of the factories accessed throws an
     *          exception, or if an error was encountered while loading
     *          and instantiating the factory and object classes.
     *          A factory should only throw an exception if it does not want
     *          other factories to be used in an attempt to create an object.
     *  See {@code StateFactory.getStateToBind()}.
     * @see StateFactory
     * @see StateFactory#getStateToBind
     * @see DirectoryManager#getStateToBind
     * @since 1.3
     */
    public static Object
        getStateToBind(Object obj, Name name, Context nameCtx,
                       Hashtable<?,?> environment)
        throws NamingException
    {

        FactoryEnumeration factories = ResourceManager.getFactories(
            Context.STATE_FACTORIES, environment, nameCtx);

        if (factories == null) {
            return obj;
        }

        // Try each factory until one succeeds
        StateFactory factory;
        Object answer = null;
        while (answer == null && factories.hasMore()) {
            factory = (StateFactory)factories.next();
            answer = factory.getStateToBind(obj, name, nameCtx, environment);
        }

        return (answer != null) ? answer : obj;
    }

    /**
     * Thrown when an error is encountered while loading and instantiating the
     * context factory classes.
     */
    private static class FactoryInitializationError extends Error {
        @java.io.Serial
        static final long serialVersionUID = -5805552256848841560L;

        private FactoryInitializationError(NoInitialContextException cause) {
            super(cause);
        }

        @Override
        public NoInitialContextException getCause() {
            return (NoInitialContextException) super.getCause();
        }
    }
}
