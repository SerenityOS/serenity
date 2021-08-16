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

import java.util.*;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;

/**
 * List of Providers. Used to represent the provider preferences.
 *
 * The system starts out with a ProviderList that only has the names
 * of the Providers.
 * When using ServiceLoader to load the providers, Providers are created
 * semi-eagerly as we iterate through them looking for a match.
 *
 * For compatibility reasons, Providers that could not be loaded are ignored
 * and internally presented as the instance EMPTY_PROVIDER. However, those
 * objects cannot be presented to applications. Call the convert() method
 * to force all Providers to be loaded and to obtain a ProviderList with
 * invalid entries removed. All this is handled by the Security class.
 *
 * Note that all indices used by this class are 0-based per general Java
 * convention. These must be converted to the 1-based indices used by the
 * Security class externally when needed.
 *
 * Instances of this class are immutable. This eliminates the need for
 * cloning and synchronization in consumers. The add() and remove() style
 * methods are static in order to avoid confusion about the immutability.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
public final class ProviderList {

    static final sun.security.util.Debug debug =
        sun.security.util.Debug.getInstance("jca", "ProviderList");

    private static final ProviderConfig[] PC0 = new ProviderConfig[0];

    private static final Provider[] P0 = new Provider[0];

    // constant for an ProviderList with no elements
    static final ProviderList EMPTY = new ProviderList(PC0, true);

    // list of all jdk.security.provider.preferred entries
    static private PreferredList preferredPropList = null;

    // dummy provider object to use during initialization
    // used to avoid explicit null checks in various places
    private static final Provider EMPTY_PROVIDER =
        new Provider("##Empty##", "1.0", "initialization in progress") {
            @java.io.Serial
            private static final long serialVersionUID = 1151354171352296389L;
            // override getService() to return null slightly faster
            public Service getService(String type, String algorithm) {
                return null;
            }
        };

    // construct a ProviderList from the security properties
    // (static provider configuration in the java.security file)
    @SuppressWarnings("removal")
    static ProviderList fromSecurityProperties() {
        // doPrivileged() because of Security.getProperty()
        return AccessController.doPrivileged(
                        new PrivilegedAction<ProviderList>() {
            public ProviderList run() {
                return new ProviderList();
            }
        });
    }

    public static ProviderList add(ProviderList providerList, Provider p) {
        return insertAt(providerList, p, -1);
    }

    public static ProviderList insertAt(ProviderList providerList, Provider p,
            int position) {
        if (providerList.getProvider(p.getName()) != null) {
            return providerList;
        }
        List<ProviderConfig> list = new ArrayList<>
                                    (Arrays.asList(providerList.configs));
        int n = list.size();
        if ((position < 0) || (position > n)) {
            position = n;
        }
        list.add(position, new ProviderConfig(p));
        return new ProviderList(list.toArray(PC0), true);
    }

    public static ProviderList remove(ProviderList providerList, String name) {
        // make sure provider exists
        if (providerList.getProvider(name) == null) {
            return providerList;
        }
        // copy all except matching to new list
        ProviderConfig[] configs = new ProviderConfig[providerList.size() - 1];
        int j = 0;
        for (ProviderConfig config : providerList.configs) {
            if (config.getProvider().getName().equals(name) == false) {
                configs[j++] = config;
            }
        }
        return new ProviderList(configs, true);
    }

    // Create a new ProviderList from the specified Providers.
    // This method is for use by SunJSSE.
    public static ProviderList newList(Provider ... providers) {
        ProviderConfig[] configs = new ProviderConfig[providers.length];
        for (int i = 0; i < providers.length; i++) {
            configs[i] = new ProviderConfig(providers[i]);
        }
        return new ProviderList(configs, true);
    }

    // configuration of the providers
    private final ProviderConfig[] configs;

    // flag indicating whether all configs have been loaded successfully
    private volatile boolean allLoaded;

    // List returned by providers()
    private final List<Provider> userList = new AbstractList<Provider>() {
        public int size() {
            return configs.length;
        }
        public Provider get(int index) {
            return getProvider(index);
        }
    };

    /**
     * Create a new ProviderList from an array of configs
     */
    private ProviderList(ProviderConfig[] configs, boolean allLoaded) {
        this.configs = configs;
        this.allLoaded = allLoaded;
    }

    /**
     * Return a new ProviderList parsed from the java.security Properties.
     */
    private ProviderList() {
        List<ProviderConfig> configList = new ArrayList<>();
        String entry;
        int i = 1;

        while ((entry = Security.getProperty("security.provider." + i)) != null) {
            entry = entry.trim();
            if (entry.isEmpty()) {
                System.err.println("invalid entry for " +
                                   "security.provider." + i);
                break;
            }
            int k = entry.indexOf(' ');
            ProviderConfig config;
            if (k == -1) {
                config = new ProviderConfig(entry);
            } else {
                String provName = entry.substring(0, k);
                String argument = entry.substring(k + 1).trim();
                config = new ProviderConfig(provName, argument);
            }

            // Get rid of duplicate providers.
            if (configList.contains(config) == false) {
                configList.add(config);
            }
            i++;
        }
        configs = configList.toArray(PC0);

        // Load config entries for use when getInstance is called
        entry = Security.getProperty("jdk.security.provider.preferred");
        if (entry != null && !(entry = entry.trim()).isEmpty()) {
            String[] entries = entry.split(",");
            if (ProviderList.preferredPropList == null) {
                ProviderList.preferredPropList = new PreferredList();
            }

            for (String e : entries) {
                i = e.indexOf(':');
                if (i < 0) {
                    if (debug != null) {
                        debug.println("invalid preferred entry skipped.  " +
                                "Missing colon delimiter \"" + e + "\"");
                    }
                    continue;
                }
                ProviderList.preferredPropList.add(new PreferredEntry(
                        e.substring(0, i).trim(), e.substring(i + 1).trim()));
            }
        }

        if (debug != null) {
            debug.println("provider configuration: " + configList);
            debug.println("config configuration: " +
                    ProviderList.preferredPropList);
        }
    }

    /**
     * Construct a special ProviderList for JAR verification. It consists
     * of the providers specified via jarClassNames, which must be on the
     * bootclasspath and cannot be in signed JAR files. This is to avoid
     * possible recursion and deadlock during verification.
     */
    ProviderList getJarList(String[] jarProvNames) {
        List<ProviderConfig> newConfigs = new ArrayList<>();
        for (String provName : jarProvNames) {
            ProviderConfig newConfig = new ProviderConfig(provName);
            for (ProviderConfig config : configs) {
                // if the equivalent object is present in this provider list,
                // use the old object rather than the new object.
                // this ensures that when the provider is loaded in the
                // new thread local list, it will also become available
                // in this provider list
                if (config.equals(newConfig)) {
                    newConfig = config;
                    break;
                }
            }
            newConfigs.add(newConfig);
        }
        ProviderConfig[] configArray = newConfigs.toArray(PC0);
        return new ProviderList(configArray, false);
    }

    public int size() {
        return configs.length;
    }

    /**
     * Return the Provider at the specified index. Returns EMPTY_PROVIDER
     * if the provider could not be loaded at this time.
     */
    Provider getProvider(int index) {
        Provider p = configs[index].getProvider();
        return (p != null) ? p : EMPTY_PROVIDER;
    }

    /**
     * Return an unmodifiable List of all Providers in this List. The
     * individual Providers are loaded on demand. Elements that could not
     * be initialized are replaced with EMPTY_PROVIDER.
     */
    public List<Provider> providers() {
        return userList;
    }

    private ProviderConfig getProviderConfig(String name) {
        int index = getIndex(name);
        return (index != -1) ? configs[index] : null;
    }

    // return the Provider with the specified name or null
    public Provider getProvider(String name) {
        ProviderConfig config = getProviderConfig(name);
        return (config == null) ? null : config.getProvider();
    }

    /**
     * Return the index at which the provider with the specified name is
     * installed or -1 if it is not present in this ProviderList.
     */
    public int getIndex(String name) {
        for (int i = 0; i < configs.length; i++) {
            Provider p = getProvider(i);
            if (p.getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // attempt to load all Providers not already loaded
    private int loadAll() {
        if (allLoaded) {
            return configs.length;
        }
        if (debug != null) {
            debug.println("Loading all providers");
            new Exception("Debug Info. Call trace:").printStackTrace();
        }
        int n = 0;
        for (int i = 0; i < configs.length; i++) {
            Provider p = configs[i].getProvider();
            if (p != null) {
                n++;
            }
        }
        if (n == configs.length) {
            allLoaded = true;
        }
        return n;
    }

    /**
     * Try to load all Providers and return the ProviderList. If one or
     * more Providers could not be loaded, a new ProviderList with those
     * entries removed is returned. Otherwise, the method returns this.
     */
    ProviderList removeInvalid() {
        int n = loadAll();
        if (n == configs.length) {
            return this;
        }
        ProviderConfig[] newConfigs = new ProviderConfig[n];
        for (int i = 0, j = 0; i < configs.length; i++) {
            ProviderConfig config = configs[i];
            if (config.isLoaded()) {
                newConfigs[j++] = config;
            }
        }
        return new ProviderList(newConfigs, true);
    }

    // return the providers as an array
    public Provider[] toArray() {
        return providers().toArray(P0);
    }

    // return a String representation of this ProviderList
    public String toString() {
        return Arrays.asList(configs).toString();
    }

    /**
     * Return a Service describing an implementation of the specified
     * algorithm from the Provider with the highest precedence that
     * supports that algorithm. Return null if no Provider supports this
     * algorithm.
     */
    public Service getService(String type, String name) {
        ArrayList<PreferredEntry> pList = null;
        int i;

        // Preferred provider list
        if (preferredPropList != null &&
                (pList = preferredPropList.getAll(type, name)) != null) {
            for (i = 0; i < pList.size(); i++) {
                Provider p = getProvider(pList.get(i).provider);
                Service s = p.getService(type, name);
                if (s != null) {
                    return s;
                }
            }
        }

        for (i = 0; i < configs.length; i++) {
            Provider p = getProvider(i);
            Service s = p.getService(type, name);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Return a List containing all the Services describing implementations
     * of the specified algorithms in precedence order. If no implementation
     * exists, this method returns an empty List.
     *
     * The elements of this list are determined lazily on demand.
     *
     * The List returned is NOT thread safe.
     */
    public List<Service> getServices(String type, String algorithm) {
        return new ServiceList(type, algorithm);
    }

    /**
     * This method exists for compatibility with JCE only. It will be removed
     * once JCE has been changed to use the replacement method.
     * @deprecated use {@code getServices(List<ServiceId>)} instead
     */
    @Deprecated
    public List<Service> getServices(String type, List<String> algorithms) {
        List<ServiceId> ids = new ArrayList<>();
        for (String alg : algorithms) {
            ids.add(new ServiceId(type, alg));
        }
        return getServices(ids);
    }

    public List<Service> getServices(List<ServiceId> ids) {
        return new ServiceList(ids);
    }

    /**
     * Inner class for a List of Services. Custom List implementation in
     * order to delay Provider initialization and lookup.
     * Not thread safe.
     */
    private final class ServiceList extends AbstractList<Service> {

        // type and algorithm for simple lookup
        // avoid allocating/traversing the ServiceId list for these lookups
        private final String type;
        private final String algorithm;

        // list of ids for parallel lookup
        // if ids is non-null, type and algorithm are null
        private final List<ServiceId> ids;

        // first service we have found
        // it is stored in a separate variable so that we can avoid
        // allocating the services list if we do not need the second service.
        // this is the case if we don't failover (failovers are typically rare)
        private Service firstService;

        // list of the services we have found so far
        private List<Service> services;

        // index into config[] of the next provider we need to query
        private int providerIndex = 0;

        // Matching preferred provider list for this ServiceList
        ArrayList<PreferredEntry> preferredList = null;
        private int preferredIndex = 0;

        ServiceList(String type, String algorithm) {
            this.type = type;
            this.algorithm = algorithm;
            this.ids = null;
        }

        ServiceList(List<ServiceId> ids) {
            this.type = null;
            this.algorithm = null;
            this.ids = ids;
        }

        private void addService(Service s) {
            if (firstService == null) {
                firstService = s;
            } else {
                if (services == null) {
                    services = new ArrayList<Service>(4);
                    services.add(firstService);
                }
                services.add(s);
            }
        }

        private Service tryGet(int index) {
            Provider p;

            // If preferred providers are configured, check for matches with
            // the requested service.
            if (preferredPropList != null && preferredList == null) {
                preferredList = preferredPropList.getAll(this);
            }

            while (true) {
                if ((index == 0) && (firstService != null)) {
                    return firstService;
                } else if ((services != null) && (services.size() > index)) {
                    return services.get(index);
                }
                if (providerIndex >= configs.length) {
                    return null;
                }

                // If there were matches with a preferred provider, iterate
                // through the list first before going through the
                // ordered list (java.security.provider.#)
                if (preferredList != null &&
                        preferredIndex < preferredList.size()) {
                    PreferredEntry entry = preferredList.get(preferredIndex++);
                    // Look for the provider name in the PreferredEntry
                    p = getProvider(entry.provider);
                    if (p == null) {
                        if (debug != null) {
                            debug.println("No provider found with name: " +
                                    entry.provider);
                        }
                        continue;
                    }
                } else {
                    // check all algorithms in this provider before moving on
                    p = getProvider(providerIndex++);
                }

                if (type != null) {
                    // simple lookup
                    Service s = p.getService(type, algorithm);
                    if (s != null) {
                        addService(s);
                    }
                } else {
                    // parallel lookup
                    for (ServiceId id : ids) {
                        Service s = p.getService(id.type, id.algorithm);
                        if (s != null) {
                            addService(s);
                        }
                    }
                }
            }
        }

        public Service get(int index) {
            Service s = tryGet(index);
            if (s == null) {
                throw new IndexOutOfBoundsException();
            }
            return s;
        }

        public int size() {
            int n;
            if (services != null) {
                n = services.size();
            } else {
                n = (firstService != null) ? 1 : 0;
            }
            while (tryGet(n) != null) {
                n++;
            }
            return n;
        }

        // override isEmpty() and iterator() to not call size()
        // this avoids loading + checking all Providers

        public boolean isEmpty() {
            return (tryGet(0) == null);
        }

        public Iterator<Service> iterator() {
            return new Iterator<Service>() {
                int index;

                public boolean hasNext() {
                    return tryGet(index) != null;
                }

                public Service next() {
                    Service s = tryGet(index);
                    if (s == null) {
                        throw new NoSuchElementException();
                    }
                    index++;
                    return s;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    // Provider list defined by jdk.security.provider.preferred entry
    static final class PreferredList {
        ArrayList<PreferredEntry> list = new ArrayList<PreferredEntry>();

        /*
         * Return a list of all preferred entries that match the passed
         * ServiceList.
         */
        ArrayList<PreferredEntry> getAll(ServiceList s) {
            if (s.ids == null) {
                return getAll(s.type, s.algorithm);

            }

            ArrayList<PreferredEntry> l = new ArrayList<PreferredEntry>();
            for (ServiceId id : s.ids) {
                implGetAll(l, id.type, id.algorithm);
            }

            return l;
        }

        /*
         * Return a list of all preferred entries that match the passed
         * type and algorithm.
         */
        ArrayList<PreferredEntry> getAll(String type, String algorithm) {
            ArrayList<PreferredEntry> l = new ArrayList<PreferredEntry>();
            implGetAll(l, type, algorithm);
            return l;
        }

        /*
         * Compare each preferred entry against the passed type and
         * algorithm, putting any matches in the passed ArrayList.
         */
        private void implGetAll(ArrayList<PreferredEntry> l, String type,
                String algorithm) {
            PreferredEntry e;

            for (int i = 0; i < size(); i++) {
                e = list.get(i);
                if (e.match(type, algorithm)) {
                    l.add(e);
                }
            }
        }

        public PreferredEntry get(int i) {
            return list.get(i);
        }

        public int size() {
            return list.size();
        }

        public boolean add(PreferredEntry e) {
            return list.add(e);
        }

        public String toString() {
            String s = "";
            for (PreferredEntry e: list) {
                s += e.toString();
            }
            return s;
        }
    }

    /* Defined Groups for jdk.security.provider.preferred */
    private static final String SHA2Group[] = { "SHA-224", "SHA-256",
            "SHA-384", "SHA-512", "SHA-512/224", "SHA-512/256" };
    private static final String HmacSHA2Group[] = { "HmacSHA224",
            "HmacSHA256", "HmacSHA384", "HmacSHA512"};
    private static final String SHA2RSAGroup[] = { "SHA224withRSA",
            "SHA256withRSA", "SHA384withRSA", "SHA512withRSA"};
    private static final String SHA2DSAGroup[] = { "SHA224withDSA",
            "SHA256withDSA", "SHA384withDSA", "SHA512withDSA"};
    private static final String SHA2ECDSAGroup[] = { "SHA224withECDSA",
            "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA"};
    private static final String SHA3Group[] = { "SHA3-224", "SHA3-256",
            "SHA3-384", "SHA3-512" };
    private static final String HmacSHA3Group[] = { "HmacSHA3-224",
            "HmacSHA3-256", "HmacSHA3-384", "HmacSHA3-512"};

    // Individual preferred property entry from jdk.security.provider.preferred
    private static class PreferredEntry {
        private String type = null;
        private String algorithm;
        private String provider;
        private String alternateNames[] = null;
        private boolean group = false;

        PreferredEntry(String t, String p) {
            int i = t.indexOf('.');
            if (i > 0) {
                type = t.substring(0, i);
                algorithm = t.substring(i + 1);
            } else {
                algorithm = t;
            }

            provider = p;
            // Group definitions
            if (type != null && type.compareToIgnoreCase("Group") == 0) {
                // Currently intrinsic algorithm groups
                if (algorithm.compareToIgnoreCase("SHA2") == 0) {
                    alternateNames = SHA2Group;
                } else if (algorithm.compareToIgnoreCase("HmacSHA2") == 0) {
                    alternateNames = HmacSHA2Group;
                } else if (algorithm.compareToIgnoreCase("SHA2RSA") == 0) {
                    alternateNames = SHA2RSAGroup;
                } else if (algorithm.compareToIgnoreCase("SHA2DSA") == 0) {
                    alternateNames = SHA2DSAGroup;
                } else if (algorithm.compareToIgnoreCase("SHA2ECDSA") == 0) {
                    alternateNames = SHA2ECDSAGroup;
                } else if (algorithm.compareToIgnoreCase("SHA3") == 0) {
                    alternateNames = SHA3Group;
                } else if (algorithm.compareToIgnoreCase("HmacSHA3") == 0) {
                    alternateNames = HmacSHA3Group;
                }
                if (alternateNames != null) {
                    group = true;
                }

            // If the algorithm name given is SHA1
            } else if (algorithm.compareToIgnoreCase("SHA1") == 0) {
                alternateNames = new String[] { "SHA-1" };
            } else if (algorithm.compareToIgnoreCase("SHA-1") == 0) {
                alternateNames = new String[] { "SHA1" };
            }
        }

        boolean match(String t, String a) {
            if (debug != null) {
                debug.println("Config check:  " + toString() + " == " +
                        print(t, a, null));
            }

            // Compare service type if configured
            if (type != null && !group && type.compareToIgnoreCase(t) != 0) {
                return false;
            }

            // Compare the algorithm string.
            if (!group && a.compareToIgnoreCase(algorithm) == 0) {
                if (debug != null) {
                    debug.println("Config entry matched:  " + toString());
                }
                return true;
            }

            if (alternateNames != null) {
                for (String alt : alternateNames) {
                    if (debug != null) {
                        debug.println("AltName check:  " + print(type, alt,
                                provider));
                    }
                    if (a.compareToIgnoreCase(alt) == 0) {
                        if (debug != null) {
                            debug.println("AltName entry matched:  " +
                                    provider);
                        }
                        return true;
                    }
                }
            }

            // No match
            return false;
        }

        // Print debugging output of PreferredEntry
        private String print(String t, String a, String p) {
            return "[" + ((t != null) ? t : "" ) + ", " + a +
                    ((p != null) ? " : " + p : "" ) + "] ";
        }

        public String toString() {
            return print(type, algorithm, provider);
        }
    }

}
