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

package javax.imageio.spi;

import java.io.File;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.ServiceLoader;

/**
 * A registry for service provider instances for Image I/O service types.
 *
 * <p> Service providers are stored in one or more <i>categories</i>,
 * each of which is defined by a class or interface (described by a
 * {@code Class} object) that all of its members must implement.
 *
 * <p>The set of categories supported by this class is limited
 * to the following standard Image I/O service types:
 *
 * <ul>
 * <li>{@link ImageInputStreamSpi}
 * <li>{@link ImageOutputStreamSpi}
 * <li>{@link ImageReaderSpi}
 * <li>{@link ImageTranscoderSpi}
 * <li>{@link ImageWriterSpi}
 * </ul>
 *
 * <p>An attempt to load a provider that is not a subtype of one of the
 * above types will result in {@code IllegalArgumentException}.
 * <p> For the general mechanism to load service providers, see
 * {@link java.util.ServiceLoader ServiceLoader}, which is
 * the underlying standard mechanism used by this class.
 *
 * <p> Only a single instance of a given leaf class (that is, the
 * actual class returned by {@code getClass()}, as opposed to any
 * inherited classes or interfaces) may be registered.  That is,
 * suppose that the
 * {@code com.mycompany.mypkg.GreenImageReaderProvider} class
 * is a subclass of {@code javax.imageio.spi.ImageReaderSpi}.
 * If a {@code GreenImageReaderProvider} instance is
 * registered, it will be stored in the category defined by the
 * {@code ImageReaderSpi} class.  If a new instance of
 * {@code GreenImageReaderProvider} is registered, it will replace
 * the previous instance.  In practice, service provider objects are
 * usually singletons so this behavior is appropriate.
 *
 * <p> The service provider classes should be lightweight and
 * quick to load.  Implementations of these interfaces should avoid
 * complex dependencies on other classes and on native code. The usual
 * pattern for more complex services is to register a lightweight
 * proxy for the heavyweight service.
 *
 * <p> An application may customize the contents of a registry as it
 * sees fit, so long as it has the appropriate runtime permission.
 *
 * <p> For information on how to create and deploy service providers,
 * refer to the documentation on {@link java.util.ServiceLoader ServiceLoader}
 *
 * @see RegisterableService
 * @see java.util.ServiceLoader
 */
public class ServiceRegistry {

    // Class -> Registry
    private Map<Class<?>, SubRegistry> categoryMap = new HashMap<>();

    /**
     * Constructs a {@code ServiceRegistry} instance with a
     * set of categories taken from the {@code categories}
     * argument. The categories must all be members of the set
     * of service types listed in the class specification.
     *
     * @param categories an {@code Iterator} containing
     * {@code Class} objects to be used to define categories.
     *
     * @exception IllegalArgumentException if
     * {@code categories} is {@code null}, or if
     * one of the categories is not an allowed service type.
     */
    public ServiceRegistry(Iterator<Class<?>> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories == null!");
        }
        while (categories.hasNext()) {
            Class<?> category = categories.next();
            checkClassAllowed(category);
            SubRegistry reg = new SubRegistry(this, category);
            categoryMap.put(category, reg);
        }
    }

    /**
     * Searches for implementations of a particular service class
     * using the given class loader.
     *
     * <p>The service class must be one of the service types listed
     * in the class specification. If it is not, {@code IllegalArgumentException}
     * will be thrown.
     *
     * <p> This method transforms the name of the given service class
     * into a provider-configuration filename as described in the
     * class comment and then uses the {@code getResources}
     * method of the given class loader to find all available files
     * with that name.  These files are then read and parsed to
     * produce a list of provider-class names.  The iterator that is
     * returned uses the given class loader to look up and then
     * instantiate each element of the list.
     *
     * <p> Because it is possible for extensions to be installed into
     * a running Java virtual machine, this method may return
     * different results each time it is invoked.
     *
     * @param providerClass a {@code Class} object indicating the
     * class or interface of the service providers being detected.
     *
     * @param loader the class loader to be used to load
     * provider-configuration files and instantiate provider classes,
     * or {@code null} if the system class loader (or, failing that
     * the bootstrap class loader) is to be used.
     *
     * @param <T> the type of the providerClass.
     *
     * @return An {@code Iterator} that yields provider objects
     * for the given service, in some arbitrary order.  The iterator
     * will throw an {@code Error} if a provider-configuration
     * file violates the specified format or if a provider class
     * cannot be found and instantiated.
     *
     * @exception IllegalArgumentException if
     * {@code providerClass} is {@code null}, or if it is
     * not one of the allowed service types.
     */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass,
                                                  ClassLoader loader)
    {
        if (providerClass == null) {
            throw new IllegalArgumentException("providerClass == null!");
        }
        checkClassAllowed(providerClass);
        return ServiceLoader.load(providerClass, loader).iterator();
    }

    /**
     * Locates and incrementally instantiates the available providers
     * of a given service using the context class loader.  This
     * convenience method is equivalent to:
     *
     * <pre>
     *   ClassLoader cl = Thread.currentThread().getContextClassLoader();
     *   return Service.providers(service, cl);
     * </pre>
     *
     * <p>The service class must be one of the service types listed
     * in the class specification. If it is not, {@code IllegalArgumentException}
     * will be thrown.
     *
     * @param providerClass a {@code Class} object indicating the
     * class or interface of the service providers being detected.
     *
     * @param <T> the type of the providerClass.
     *
     * @return An {@code Iterator} that yields provider objects
     * for the given service, in some arbitrary order.  The iterator
     * will throw an {@code Error} if a provider-configuration
     * file violates the specified format or if a provider class
     * cannot be found and instantiated.
     *
     * @exception IllegalArgumentException if
     * {@code providerClass} is {@code null}, or if it is
     * not one of the allowed service types.
     */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass) {
        if (providerClass == null) {
            throw new IllegalArgumentException("providerClass == null!");
        }
        checkClassAllowed(providerClass);
        return ServiceLoader.load(providerClass).iterator();
    }

    /**
     * Returns an {@code Iterator} of {@code Class} objects
     * indicating the current set of categories.  The iterator will be
     * empty if no categories exist.
     *
     * @return an {@code Iterator} containing
     * {@code Class} objects.
     */
    public Iterator<Class<?>> getCategories() {
        Set<Class<?>> keySet = categoryMap.keySet();
        return keySet.iterator();
    }

    /**
     * Returns an Iterator containing the subregistries to which the
     * provider belongs.
     */
    private Iterator<SubRegistry> getSubRegistries(Object provider) {
        List<SubRegistry> l = new ArrayList<>();
        for (Class<?> c : categoryMap.keySet()) {
            if (c.isAssignableFrom(provider.getClass())) {
                l.add(categoryMap.get(c));
            }
        }
        return l.iterator();
    }

    /**
     * Adds a service provider object to the registry.  The provider
     * is associated with the given category.
     *
     * <p> If {@code provider} implements the
     * {@code RegisterableService} interface, its
     * {@code onRegistration} method will be called.  Its
     * {@code onDeregistration} method will be called each time
     * it is deregistered from a category, for example if a
     * category is removed or the registry is garbage collected.
     *
     * @param provider the service provide object to be registered.
     * @param category the category under which to register the
     * provider.
     * @param <T> the type of the provider.
     *
     * @return true if no provider of the same class was previously
     * registered in the same category category.
     *
     * @exception IllegalArgumentException if {@code provider} is
     * {@code null}.
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     * @exception ClassCastException if provider does not implement
     * the {@code Class} defined by {@code category}.
     */
    public <T> boolean registerServiceProvider(T provider,
                                               Class<T> category) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        if (!category.isAssignableFrom(provider.getClass())) {
            throw new ClassCastException();
        }

        return reg.registerServiceProvider(provider);
    }

    /**
     * Adds a service provider object to the registry.  The provider
     * is associated within each category present in the registry
     * whose {@code Class} it implements.
     *
     * <p> If {@code provider} implements the
     * {@code RegisterableService} interface, its
     * {@code onRegistration} method will be called once for each
     * category it is registered under.  Its
     * {@code onDeregistration} method will be called each time
     * it is deregistered from a category or when the registry is
     * finalized.
     *
     * @param provider the service provider object to be registered.
     *
     * @exception IllegalArgumentException if
     * {@code provider} is {@code null}.
     */
    public void registerServiceProvider(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        Iterator<SubRegistry> regs = getSubRegistries(provider);
        while (regs.hasNext()) {
            SubRegistry reg = regs.next();
            reg.registerServiceProvider(provider);
        }
    }

    /**
     * Adds a set of service provider objects, taken from an
     * {@code Iterator} to the registry.  Each provider is
     * associated within each category present in the registry whose
     * {@code Class} it implements.
     *
     * <p> For each entry of {@code providers} that implements
     * the {@code RegisterableService} interface, its
     * {@code onRegistration} method will be called once for each
     * category it is registered under.  Its
     * {@code onDeregistration} method will be called each time
     * it is deregistered from a category or when the registry is
     * finalized.
     *
     * @param providers an Iterator containing service provider
     * objects to be registered.
     *
     * @exception IllegalArgumentException if {@code providers}
     * is {@code null} or contains a {@code null} entry.
     */
    public void registerServiceProviders(Iterator<?> providers) {
        if (providers == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        while (providers.hasNext()) {
            registerServiceProvider(providers.next());
        }
    }

    /**
     * Removes a service provider object from the given category.  If
     * the provider was not previously registered, nothing happens and
     * {@code false} is returned.  Otherwise, {@code true}
     * is returned.  If an object of the same class as
     * {@code provider} but not equal (using {@code ==}) to
     * {@code provider} is registered, it will not be
     * deregistered.
     *
     * <p> If {@code provider} implements the
     * {@code RegisterableService} interface, its
     * {@code onDeregistration} method will be called.
     *
     * @param provider the service provider object to be deregistered.
     * @param category the category from which to deregister the
     * provider.
     * @param <T> the type of the provider.
     *
     * @return {@code true} if the provider was previously
     * registered in the same category category,
     * {@code false} otherwise.
     *
     * @exception IllegalArgumentException if {@code provider} is
     * {@code null}.
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     * @exception ClassCastException if provider does not implement
     * the class defined by {@code category}.
     */
    public <T> boolean deregisterServiceProvider(T provider,
                                                 Class<T> category) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        if (!category.isAssignableFrom(provider.getClass())) {
            throw new ClassCastException();
        }
        return reg.deregisterServiceProvider(provider);
    }

    /**
     * Removes a service provider object from all categories that
     * contain it.
     *
     * @param provider the service provider object to be deregistered.
     *
     * @exception IllegalArgumentException if {@code provider} is
     * {@code null}.
     */
    public void deregisterServiceProvider(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        Iterator<SubRegistry> regs = getSubRegistries(provider);
        while (regs.hasNext()) {
            SubRegistry reg = regs.next();
            reg.deregisterServiceProvider(provider);
        }
    }

    /**
     * Returns {@code true} if {@code provider} is currently
     * registered.
     *
     * @param provider the service provider object to be queried.
     *
     * @return {@code true} if the given provider has been
     * registered.
     *
     * @exception IllegalArgumentException if {@code provider} is
     * {@code null}.
     */
    public boolean contains(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        Iterator<SubRegistry> regs = getSubRegistries(provider);
        while (regs.hasNext()) {
            SubRegistry reg = regs.next();
            if (reg.contains(provider)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an {@code Iterator} containing all registered
     * service providers in the given category.  If
     * {@code useOrdering} is {@code false}, the iterator
     * will return all of the server provider objects in an arbitrary
     * order.  Otherwise, the ordering will respect any pairwise
     * orderings that have been set.  If the graph of pairwise
     * orderings contains cycles, any providers that belong to a cycle
     * will not be returned.
     *
     * @param category the category to be retrieved from.
     * @param useOrdering {@code true} if pairwise orderings
     * should be taken account in ordering the returned objects.
     * @param <T> the type of the category.
     *
     * @return an {@code Iterator} containing service provider
     * objects from the given category, possibly in order.
     *
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     */
    public <T> Iterator<T> getServiceProviders(Class<T> category,
                                               boolean useOrdering) {
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        @SuppressWarnings("unchecked")
        Iterator<T> it = (Iterator<T>)reg.getServiceProviders(useOrdering);
        return it;
    }

    /**
     * A simple filter interface used by
     * {@code ServiceRegistry.getServiceProviders} to select
     * providers matching an arbitrary criterion.  Classes that
     * implement this interface should be defined in order to make use
     * of the {@code getServiceProviders} method of
     * {@code ServiceRegistry} that takes a {@code Filter}.
     *
     * @see ServiceRegistry#getServiceProviders(Class, ServiceRegistry.Filter, boolean)
     */
    public interface Filter {

        /**
         * Returns {@code true} if the given
         * {@code provider} object matches the criterion defined
         * by this {@code Filter}.
         *
         * @param provider a service provider {@code Object}.
         *
         * @return true if the provider matches the criterion.
         */
        boolean filter(Object provider);
    }

    /**
     * Returns an {@code Iterator} containing service provider
     * objects within a given category that satisfy a criterion
     * imposed by the supplied {@code ServiceRegistry.Filter}
     * object's {@code filter} method.
     *
     * <p> The {@code useOrdering} argument controls the
     * ordering of the results using the same rules as
     * {@code getServiceProviders(Class, boolean)}.
     *
     * @param category the category to be retrieved from.
     * @param filter an instance of {@code ServiceRegistry.Filter}
     * whose {@code filter} method will be invoked.
     * @param useOrdering {@code true} if pairwise orderings
     * should be taken account in ordering the returned objects.
     * @param <T> the type of the category.
     *
     * @return an {@code Iterator} containing service provider
     * objects from the given category, possibly in order.
     *
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     */
    public <T> Iterator<T> getServiceProviders(Class<T> category,
                                               Filter filter,
                                               boolean useOrdering) {
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        Iterator<T> iter = getServiceProviders(category, useOrdering);
        return new FilterIterator<>(iter, filter);
    }

    /**
     * Returns the currently registered service provider object that
     * is of the given class type.  At most one object of a given
     * class is allowed to be registered at any given time.  If no
     * registered object has the desired class type, {@code null}
     * is returned.
     *
     * @param providerClass the {@code Class} of the desired
     * service provider object.
     * @param <T> the type of the provider.
     *
     * @return a currently registered service provider object with the
     * desired {@code Class} type, or {@code null} is none is
     * present.
     *
     * @exception IllegalArgumentException if {@code providerClass} is
     * {@code null}.
     */
    public <T> T getServiceProviderByClass(Class<T> providerClass) {
        if (providerClass == null) {
            throw new IllegalArgumentException("providerClass == null!");
        }
        for (Class<?> c : categoryMap.keySet()) {
            if (c.isAssignableFrom(providerClass)) {
                SubRegistry reg = categoryMap.get(c);
                T provider = reg.getServiceProviderByClass(providerClass);
                if (provider != null) {
                    return provider;
                }
            }
        }
        return null;
    }

    /**
     * Sets a pairwise ordering between two service provider objects
     * within a given category.  If one or both objects are not
     * currently registered within the given category, or if the
     * desired ordering is already set, nothing happens and
     * {@code false} is returned.  If the providers previously
     * were ordered in the reverse direction, that ordering is
     * removed.
     *
     * <p> The ordering will be used by the
     * {@code getServiceProviders} methods when their
     * {@code useOrdering} argument is {@code true}.
     *
     * @param category a {@code Class} object indicating the
     * category under which the preference is to be established.
     * @param firstProvider the preferred provider.
     * @param secondProvider the provider to which
     * {@code firstProvider} is preferred.
     * @param <T> the type of the category.
     *
     * @return {@code true} if a previously unset ordering
     * was established.
     *
     * @exception IllegalArgumentException if either provider is
     * {@code null} or they are the same object.
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     */
    public <T> boolean setOrdering(Class<T> category,
                                   T firstProvider,
                                   T secondProvider) {
        if (firstProvider == null || secondProvider == null) {
            throw new IllegalArgumentException("provider is null!");
        }
        if (firstProvider == secondProvider) {
            throw new IllegalArgumentException("providers are the same!");
        }
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        if (reg.contains(firstProvider) &&
            reg.contains(secondProvider)) {
            return reg.setOrdering(firstProvider, secondProvider);
        }
        return false;
    }

    /**
     * Sets a pairwise ordering between two service provider objects
     * within a given category.  If one or both objects are not
     * currently registered within the given category, or if no
     * ordering is currently set between them, nothing happens
     * and {@code false} is returned.
     *
     * <p> The ordering will be used by the
     * {@code getServiceProviders} methods when their
     * {@code useOrdering} argument is {@code true}.
     *
     * @param category a {@code Class} object indicating the
     * category under which the preference is to be disestablished.
     * @param firstProvider the formerly preferred provider.
     * @param secondProvider the provider to which
     * {@code firstProvider} was formerly preferred.
     * @param <T> the type of the category.
     *
     * @return {@code true} if a previously set ordering was
     * disestablished.
     *
     * @exception IllegalArgumentException if either provider is
     * {@code null} or they are the same object.
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     */
    public <T> boolean unsetOrdering(Class<T> category,
                                     T firstProvider,
                                     T secondProvider) {
        if (firstProvider == null || secondProvider == null) {
            throw new IllegalArgumentException("provider is null!");
        }
        if (firstProvider == secondProvider) {
            throw new IllegalArgumentException("providers are the same!");
        }
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        if (reg.contains(firstProvider) &&
            reg.contains(secondProvider)) {
            return reg.unsetOrdering(firstProvider, secondProvider);
        }
        return false;
    }

    /**
     * Deregisters all service provider object currently registered
     * under the given category.
     *
     * @param category the category to be emptied.
     *
     * @exception IllegalArgumentException if there is no category
     * corresponding to {@code category}.
     */
    public void deregisterAll(Class<?> category) {
        SubRegistry reg = categoryMap.get(category);
        if (reg == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        reg.clear();
    }

    /**
     * Deregisters all currently registered service providers from all
     * categories.
     */
    public void deregisterAll() {
        for (SubRegistry reg : categoryMap.values()) {
            reg.clear();
        }
    }

    /**
     * Finalizes this object prior to garbage collection.  The
     * {@code deregisterAll} method is called to deregister all
     * currently registered service providers.  This method should not
     * be called from application code.
     *
     * @exception Throwable if an error occurs during superclass
     * finalization.
     *
     * @deprecated The {@code finalize} method has been deprecated.
     *     Subclasses that override {@code finalize} in order to perform cleanup
     *     should be modified to use alternative cleanup mechanisms and
     *     to remove the overriding {@code finalize} method.
     *     When overriding the {@code finalize} method, its implementation must explicitly
     *     ensure that {@code super.finalize()} is invoked as described in {@link Object#finalize}.
     *     See the specification for {@link Object#finalize()} for further
     *     information about migration options.
     */
    @Deprecated(since="9")
    public void finalize() throws Throwable {
        deregisterAll();
        super.finalize();
    }

    /**
     * Checks whether the provided class is one of the allowed
     * ImageIO service provider classes. If it is, returns normally.
     * If it is not, throws IllegalArgumentException.
     *
     * @param clazz
     * @throws IllegalArgumentException if clazz is null or is not one of the allowed set
     */
    private static void checkClassAllowed(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("class must not be null");
        }

        if (   clazz != ImageInputStreamSpi.class
            && clazz != ImageOutputStreamSpi.class
            && clazz != ImageReaderSpi.class
            && clazz != ImageTranscoderSpi.class
            && clazz != ImageWriterSpi.class) {
            throw new IllegalArgumentException(clazz.getName() + " is not an ImageIO SPI class");
        }
    }
}


/**
 * A portion of a registry dealing with a single superclass or
 * interface.
 */
class SubRegistry {

    ServiceRegistry registry;

    Class<?> category;

    // Provider Objects organized by partial ordering
    final PartiallyOrderedSet<Object> poset = new PartiallyOrderedSet<>();

    // Class -> Provider Object of that class
    // No way to express heterogeneous map, we want
    // Map<Class<T>, T>, where T is ?
    final Map<Class<?>, Object> map = new HashMap<>();
    @SuppressWarnings("removal")
    final Map<Class<?>, AccessControlContext> accMap = new HashMap<>();

    public SubRegistry(ServiceRegistry registry, Class<?> category) {
        this.registry = registry;
        this.category = category;
    }

    @SuppressWarnings("removal")
    public synchronized boolean registerServiceProvider(Object provider) {
        Object oprovider = map.get(provider.getClass());
        boolean present =  oprovider != null;

        if (present) {
            deregisterServiceProvider(oprovider);
        }
        map.put(provider.getClass(), provider);
        accMap.put(provider.getClass(), AccessController.getContext());
        poset.add(provider);
        if (provider instanceof RegisterableService) {
            RegisterableService rs = (RegisterableService)provider;
            try {
                rs.onRegistration(registry, category);
            } catch (Throwable t) {
                System.err.println("Caught and handled this exception :");
                t.printStackTrace();
            }
        }

        return !present;
    }

    /**
     * If the provider was not previously registered, do nothing.
     *
     * @return true if the provider was previously registered.
     */
    public synchronized boolean deregisterServiceProvider(Object provider) {
        Object oprovider = map.get(provider.getClass());

        if (provider == oprovider) {
            map.remove(provider.getClass());
            accMap.remove(provider.getClass());
            poset.remove(provider);
            if (provider instanceof RegisterableService) {
                RegisterableService rs = (RegisterableService)provider;
                rs.onDeregistration(registry, category);
            }

            return true;
        }
        return false;
    }

    public synchronized boolean contains(Object provider) {
        Object oprovider = map.get(provider.getClass());
        return oprovider == provider;
    }

    public synchronized boolean setOrdering(Object firstProvider,
                                            Object secondProvider) {
        return poset.setOrdering(firstProvider, secondProvider);
    }

    public synchronized boolean unsetOrdering(Object firstProvider,
                                              Object secondProvider) {
        return poset.unsetOrdering(firstProvider, secondProvider);
    }

    public synchronized Iterator<Object> getServiceProviders
                                         (boolean useOrdering) {
        if (useOrdering) {
            return poset.iterator();
        } else {
            return map.values().iterator();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T getServiceProviderByClass
                              (Class<T> providerClass) {
        return (T)map.get(providerClass);
    }

    @SuppressWarnings("removal")
    public synchronized void clear() {
        Iterator<Object> iter = map.values().iterator();
        while (iter.hasNext()) {
            Object provider = iter.next();
            iter.remove();

            if (provider instanceof RegisterableService) {
                RegisterableService rs = (RegisterableService)provider;
                AccessControlContext acc = accMap.get(provider.getClass());
                if (acc != null || System.getSecurityManager() == null) {
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    rs.onDeregistration(registry, category);
                        return null;
                    }, acc);
                }
            }
        }
        poset.clear();
        accMap.clear();
    }

    @SuppressWarnings("deprecation")
    public synchronized void finalize() {
        clear();
    }
}


/**
 * A class for wrapping {@code Iterators} with a filter function.
 * This provides an iterator for a subset without duplication.
 */
class FilterIterator<T> implements Iterator<T> {

    private Iterator<? extends T> iter;
    private ServiceRegistry.Filter filter;

    private T next = null;

    public FilterIterator(Iterator<? extends T> iter,
                          ServiceRegistry.Filter filter) {
        this.iter = iter;
        this.filter = filter;
        advance();
    }

    private void advance() {
        while (iter.hasNext()) {
            T elt = iter.next();
            if (filter.filter(elt)) {
                next = elt;
                return;
            }
        }

        next = null;
    }

    public boolean hasNext() {
        return next != null;
    }

    public T next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        T o = next;
        advance();
        return o;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
