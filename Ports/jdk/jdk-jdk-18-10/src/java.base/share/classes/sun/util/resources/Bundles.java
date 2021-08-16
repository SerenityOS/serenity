/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.util.resources;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.ResourceBundleProvider;
import jdk.internal.access.JavaUtilResourceBundleAccess;
import jdk.internal.access.SharedSecrets;

/**
 */
public abstract class Bundles {

    /** initial size of the bundle cache */
    private static final int INITIAL_CACHE_SIZE = 32;

    /** constant indicating that no resource bundle exists */
    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle() {
            @Override
            public Enumeration<String> getKeys() { return null; }
            @Override
            protected Object handleGetObject(String key) { return null; }
            @Override
            public String toString() { return "NONEXISTENT_BUNDLE"; }
        };

    private static final JavaUtilResourceBundleAccess bundleAccess
                            = SharedSecrets.getJavaUtilResourceBundleAccess();

    /**
     * The cache is a map from cache keys (with bundle base name, locale, and
     * class loader) to either a resource bundle or NONEXISTENT_BUNDLE wrapped by a
     * BundleReference.
     *
     * The cache is a ConcurrentMap, allowing the cache to be searched
     * concurrently by multiple threads.  This will also allow the cache keys
     * to be reclaimed along with the ClassLoaders they reference.
     *
     * This variable would be better named "cache", but we keep the old
     * name for compatibility with some workarounds for bug 4212439.
     */
    private static final ConcurrentMap<CacheKey, BundleReference> cacheList
                            = new ConcurrentHashMap<>(INITIAL_CACHE_SIZE);

    /**
     * Queue for reference objects referring to class loaders or bundles.
     */
    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

    private Bundles() {
    }

    public static ResourceBundle of(String baseName, Locale locale, Strategy strategy) {
        return loadBundleOf(baseName, locale, strategy);
    }

    private static ResourceBundle loadBundleOf(String baseName,
                                               Locale targetLocale,
                                               Strategy strategy) {
        Objects.requireNonNull(baseName);
        Objects.requireNonNull(targetLocale);
        Objects.requireNonNull(strategy);

        CacheKey cacheKey = new CacheKey(baseName, targetLocale);

        ResourceBundle bundle = null;

        // Quick lookup of the cache.
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef != null) {
            bundle = bundleRef.get();
        }

        // If this bundle and all of its parents are valid,
        // then return this bundle.
        if (isValidBundle(bundle)) {
            return bundle;
        }

        // Get the providers for loading the "leaf" bundle (i.e., bundle for
        // targetLocale). If no providers are required for the bundle,
        // none of its parents will require providers.
        Class<? extends ResourceBundleProvider> type
                = strategy.getResourceBundleProviderType(baseName, targetLocale);
        if (type != null) {
            @SuppressWarnings("unchecked")
            ServiceLoader<ResourceBundleProvider> providers
                = (ServiceLoader<ResourceBundleProvider>) ServiceLoader.loadInstalled(type);
            cacheKey.setProviders(providers);
        }

        List<Locale> candidateLocales = strategy.getCandidateLocales(baseName, targetLocale);
        bundle = findBundleOf(cacheKey, strategy, baseName, candidateLocales, 0);
        if (bundle == null) {
            throwMissingResourceException(baseName, targetLocale, cacheKey.getCause());
        }
        return bundle;
    }

    private static ResourceBundle findBundleOf(CacheKey cacheKey,
                                               Strategy strategy,
                                               String baseName,
                                               List<Locale> candidateLocales,
                                               int index) {
        ResourceBundle parent = null;
        Locale targetLocale = candidateLocales.get(index);
        if (index != candidateLocales.size() - 1) {
            parent = findBundleOf(cacheKey, strategy, baseName, candidateLocales, index + 1);
        }

        // Before we do the real loading work, see whether we need to
        // do some housekeeping: If resource bundles have been nulled out,
        // remove all related information from the cache.
        cleanupCache();

        // find an individual ResourceBundle in the cache
        cacheKey.setLocale(targetLocale);
        ResourceBundle bundle = findBundleInCache(cacheKey);
        if (bundle != null) {
            if (bundle == NONEXISTENT_BUNDLE) {
                return parent;
            }
            if (bundleAccess.getParent(bundle) == parent) {
                return bundle;
            }
            // Remove bundle from the cache.
            BundleReference bundleRef = cacheList.get(cacheKey);
            if (bundleRef != null && bundleRef.get() == bundle) {
                cacheList.remove(cacheKey, bundleRef);
            }
        }

        // Determine if providers should be used for loading the bundle.
        // An assumption here is that if the leaf bundle of a look-up path is
        // in java.base, all bundles of the path are in java.base.
        // (e.g., en_US of path en_US -> en -> root is in java.base and the rest
        // are in java.base as well)
        // This assumption isn't valid for general bundle loading.
        ServiceLoader<ResourceBundleProvider> providers = cacheKey.getProviders();
        if (providers != null) {
            if (strategy.getResourceBundleProviderType(baseName, targetLocale) == null) {
                providers = null;
            }
        }

        CacheKey constKey = (CacheKey) cacheKey.clone();
        try {
            if (providers != null) {
                bundle = loadBundleFromProviders(baseName, targetLocale, providers, cacheKey);
            } else {
                try {
                    String bundleName = strategy.toBundleName(baseName, targetLocale);
                    Class<?> c = Class.forName(Bundles.class.getModule(), bundleName);
                    if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
                        @SuppressWarnings("unchecked")
                        Class<ResourceBundle> bundleClass = (Class<ResourceBundle>) c;
                        bundle = bundleAccess.newResourceBundle(bundleClass);
                    }
                    if (bundle == null) {
                        var otherBundleName = toOtherBundleName(baseName, bundleName, targetLocale);
                        if (!bundleName.equals(otherBundleName)) {
                            c = Class.forName(Bundles.class.getModule(), otherBundleName);
                            if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
                                @SuppressWarnings("unchecked")
                                Class<ResourceBundle> bundleClass = (Class<ResourceBundle>) c;
                                bundle = bundleAccess.newResourceBundle(bundleClass);
                            }
                        }
                    }
                } catch (Exception e) {
                    cacheKey.setCause(e);
                }
            }
        } finally {
            if (constKey.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        if (bundle == null) {
            // Put NONEXISTENT_BUNDLE in the cache as a mark that there's no bundle
            // instance for the locale.
            putBundleInCache(cacheKey, NONEXISTENT_BUNDLE);
            return parent;
        }

        if (parent != null && bundleAccess.getParent(bundle) == null) {
            bundleAccess.setParent(bundle, parent);
        }
        bundleAccess.setLocale(bundle, targetLocale);
        bundleAccess.setName(bundle, baseName);
        bundle = putBundleInCache(cacheKey, bundle);
        return bundle;
    }

    private static void cleanupCache() {
        Object ref;
        while ((ref = referenceQueue.poll()) != null) {
            cacheList.remove(((CacheKeyReference)ref).getCacheKey());
        }
    }

    /**
     * Loads ResourceBundle from service providers.
     */
    @SuppressWarnings("removal")
    private static ResourceBundle loadBundleFromProviders(String baseName,
                                                          Locale locale,
                                                          ServiceLoader<ResourceBundleProvider> providers,
                                                          CacheKey cacheKey)
    {
        return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public ResourceBundle run() {
                        for (Iterator<ResourceBundleProvider> itr = providers.iterator(); itr.hasNext(); ) {
                            try {
                                ResourceBundleProvider provider = itr.next();
                                ResourceBundle bundle = provider.getBundle(baseName, locale);
                                if (bundle != null) {
                                    return bundle;
                                }
                            } catch (ServiceConfigurationError | SecurityException e) {
                                if (cacheKey != null) {
                                    cacheKey.setCause(e);
                                }
                            }
                        }
                        return null;
                    }
                });

    }

    private static boolean isValidBundle(ResourceBundle bundle) {
        return bundle != null && bundle != NONEXISTENT_BUNDLE;
    }

    /**
     * Throw a MissingResourceException with proper message
     */
    private static void throwMissingResourceException(String baseName,
                                                      Locale locale,
                                                      Throwable cause) {
        // If the cause is a MissingResourceException, avoid creating
        // a long chain. (6355009)
        if (cause instanceof MissingResourceException) {
            cause = null;
        }
        MissingResourceException e;
        e = new MissingResourceException("Can't find bundle for base name "
                                         + baseName + ", locale " + locale,
                                         baseName + "_" + locale, // className
                                         "");
        e.initCause(cause);
        throw e;
    }

    /**
     * Finds a bundle in the cache.
     *
     * @param cacheKey the key to look up the cache
     * @return the ResourceBundle found in the cache or null
     */
    private static ResourceBundle findBundleInCache(CacheKey cacheKey) {
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef == null) {
            return null;
        }
        return bundleRef.get();
    }

    /**
     * Put a new bundle in the cache.
     *
     * @param cacheKey the key for the resource bundle
     * @param bundle the resource bundle to be put in the cache
     * @return the ResourceBundle for the cacheKey; if someone has put
     * the bundle before this call, the one found in the cache is
     * returned.
     */
    private static ResourceBundle putBundleInCache(CacheKey cacheKey,
                                                   ResourceBundle bundle) {
        CacheKey key = (CacheKey) cacheKey.clone();
        BundleReference bundleRef = new BundleReference(bundle, referenceQueue, key);

        // Put the bundle in the cache if it's not been in the cache.
        BundleReference result = cacheList.putIfAbsent(key, bundleRef);

        // If someone else has put the same bundle in the cache before
        // us, we should use the one in the cache.
        if (result != null) {
            ResourceBundle rb = result.get();
            if (rb != null) {
                // Clear the back link to the cache key
                bundle = rb;
                // Clear the reference in the BundleReference so that
                // it won't be enqueued.
                bundleRef.clear();
            } else {
                // Replace the invalid (garbage collected)
                // instance with the valid one.
                cacheList.put(key, bundleRef);
            }
        }
        return bundle;
    }

    /**
     * Generates the other bundle name for languages that have changed,
     * i.e. "he", "id", and "yi"
     *
     * @param baseName ResourceBundle base name
     * @param bundleName ResourceBundle bundle name
     * @param locale locale
     * @return the other bundle name, or the same name for non-legacy ISO languages
     */
    public static String toOtherBundleName(String baseName, String bundleName, Locale locale) {
        var simpleName= baseName.substring(baseName.lastIndexOf('.') + 1);
        var suffix = bundleName.substring(bundleName.lastIndexOf(simpleName) + simpleName.length());
        var otherSuffix = switch(locale.getLanguage()) {
            case "he" -> suffix.replaceFirst("^_he(_.*)?$", "_iw$1");
            case "id" -> suffix.replaceFirst("^_id(_.*)?$", "_in$1");
            case "yi" -> suffix.replaceFirst("^_yi(_.*)?$", "_ji$1");
            case "iw" -> suffix.replaceFirst("^_iw(_.*)?$", "_he$1");
            case "in" -> suffix.replaceFirst("^_in(_.*)?$", "_id$1");
            case "ji" -> suffix.replaceFirst("^_ji(_.*)?$", "_yi$1");
            default -> suffix;
        };

        if (suffix.equals(otherSuffix)) {
            return bundleName;
        } else {
            return bundleName.substring(0, bundleName.lastIndexOf(suffix)) + otherSuffix;
        }
    }

    /**
     * The Strategy interface defines methods that are called by Bundles.of during
     * the resource bundle loading process.
     */
    public interface Strategy {
        /**
         * Returns a list of locales to be looked up for bundle loading.
         */
        List<Locale> getCandidateLocales(String baseName, Locale locale);

        /**
         * Returns the bundle name for the given baseName and locale.
         */
        String toBundleName(String baseName, Locale locale);

        /**
         * Returns the service provider type for the given baseName
         * and locale, or null if no service providers should be used.
         */
        Class<? extends ResourceBundleProvider> getResourceBundleProviderType(String baseName,
                                                                                     Locale locale);
    }

    /**
     * The common interface to get a CacheKey in LoaderReference and
     * BundleReference.
     */
    private static interface CacheKeyReference {
        CacheKey getCacheKey();
    }

    /**
     * References to bundles are soft references so that they can be garbage
     * collected when they have no hard references.
     */
    private static class BundleReference extends SoftReference<ResourceBundle>
                                         implements CacheKeyReference {
        private final CacheKey cacheKey;

        BundleReference(ResourceBundle referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            cacheKey = key;
        }

        @Override
        public CacheKey getCacheKey() {
            return cacheKey;
        }
    }

    /**
     * Key used for cached resource bundles.  The key checks the base
     * name, the locale, and the class loader to determine if the
     * resource is a match to the requested one. The loader may be
     * null, but the base name and the locale must have a non-null
     * value.
     */
    private static class CacheKey implements Cloneable {
        // These two are the actual keys for lookup in Map.
        private String name;
        private Locale locale;

        // Placeholder for an error report by a Throwable
        private Throwable cause;

        // Hash code value cache to avoid recalculating the hash code
        // of this instance.
        private int hashCodeCache;

        // The service loader to load bundles or null if no service loader
        // is required.
        private ServiceLoader<ResourceBundleProvider> providers;

        CacheKey(String baseName, Locale locale) {
            this.name = baseName;
            this.locale = locale;
            calculateHashCode();
        }

        String getName() {
            return name;
        }

        CacheKey setName(String baseName) {
            if (!this.name.equals(baseName)) {
                this.name = baseName;
                calculateHashCode();
            }
            return this;
        }

        Locale getLocale() {
            return locale;
        }

        CacheKey setLocale(Locale locale) {
            if (!this.locale.equals(locale)) {
                this.locale = locale;
                calculateHashCode();
            }
            return this;
        }

        ServiceLoader<ResourceBundleProvider> getProviders() {
            return providers;
        }

        void setProviders(ServiceLoader<ResourceBundleProvider> providers) {
            this.providers = providers;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            try {
                final CacheKey otherEntry = (CacheKey)other;
                //quick check to see if they are not equal
                if (hashCodeCache != otherEntry.hashCodeCache) {
                    return false;
                }
                return locale.equals(otherEntry.locale)
                        && name.equals(otherEntry.name);
            } catch (NullPointerException | ClassCastException e) {
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashCodeCache;
        }

        private void calculateHashCode() {
            hashCodeCache = name.hashCode() << 3;
            hashCodeCache ^= locale.hashCode();
        }

        @Override
        public Object clone() {
            try {
                CacheKey clone = (CacheKey) super.clone();
                // Clear the reference to a Throwable
                clone.cause = null;
                // Clear the reference to a ServiceLoader
                clone.providers = null;
                return clone;
            } catch (CloneNotSupportedException e) {
                //this should never happen
                throw new InternalError(e);
            }
        }

        private void setCause(Throwable cause) {
            if (this.cause == null) {
                this.cause = cause;
            } else {
                // Override the cause if the previous one is
                // ClassNotFoundException.
                if (this.cause instanceof ClassNotFoundException) {
                    this.cause = cause;
                }
            }
        }

        private Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            String l = locale.toString();
            if (l.isEmpty()) {
                if (!locale.getVariant().isEmpty()) {
                    l = "__" + locale.getVariant();
                } else {
                    l = "\"\"";
                }
            }
            return "CacheKey[" + name + ", lc=" + l + ")]";
        }
    }
}
