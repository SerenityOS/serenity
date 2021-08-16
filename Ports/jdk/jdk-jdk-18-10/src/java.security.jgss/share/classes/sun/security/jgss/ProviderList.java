/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss;

import java.lang.reflect.InvocationTargetException;
import org.ietf.jgss.*;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import sun.security.jgss.spi.*;
import sun.security.jgss.wrapper.NativeGSSFactory;
import sun.security.jgss.wrapper.SunNativeProvider;
import sun.security.action.GetPropertyAction;

/**
 * This class stores the list of providers that this
 * GSS-Implementation is configured to use. The GSSManagerImpl class
 * queries this class whenever it needs a mechanism's factory.<p>
 *
 * This class stores an ordered list of pairs of the form
 * {@code <provider, oid>}. When it attempts to instantiate a mechanism
 * defined by oid o, it steps through the list looking for an entry
 * with oid=o, or with oid=null. (An entry with oid=null matches all
 * mechanisms.) When it finds such an entry, the corresponding
 * provider is approached for the mechanism's factory class.
 * At instantiation time this list in initialized to contain those
 * system wide providers that contain a property of the form
 * "GssApiMechanism.x.y.z..." where "x.y.z..." is a numeric object
 * identifier with numbers x, y, z, etc. Such a property is defined
 * to map to that provider's implementation of the MechanismFactory
 * interface for the mechanism x.y.z...
 * As and when a MechanismFactory is instantiated, it is
 * cached for future use. <p>
 *
 * An application can cause more providers to be added by means of
 * the addProviderAtFront and addProviderAtEnd methods on
 * GSSManager which get delegated to this class. The
 * addProviderAtFront method can also cause a change in the ordering
 * of the providers without adding any new providers, by causing a
 * provider to move up in a list. The method addProviderAtEnd can
 * only add providers at the end of the list if they are not already
 * in the list. The rationale is that an application will call
 * addProviderAtFront when it wants a provider to be used in
 * preference over the default ones. And it will call
 * addProviderAtEnd when it wants a provider to be used in case
 * the system ones don't suffice.<p>
 *
 * If a mechanism's factory is being obtained from a provider as a
 * result of encountering a entryof the form {@code <provider, oid>} where
 * oid is non-null, then the assumption is that the application added
 * this entry and it wants this mechanism to be obtained from this
 * provider. Thus is the provider does not actually contain the
 * requested mechanism, an exception will be thrown. However, if the
 * entry were of the form {@code <provider, null>}, then it is viewed more
 * liberally and is simply skipped over if the provider does not claim to
 * support the requested mechanism.
 */

public final class ProviderList {

    private static final String PROV_PROP_PREFIX = "GssApiMechanism.";
    private static final int PROV_PROP_PREFIX_LEN =
        PROV_PROP_PREFIX.length();

    private static final String SPI_MECH_FACTORY_TYPE
        = "sun.security.jgss.spi.MechanismFactory";

    public static final Oid DEFAULT_MECH_OID;

    static {
        /*
         * Set the default mechanism. Kerberos v5 is the default
         * mechanism unless it is overridden by a system property.
         * with a valid OID value
         */
        Oid defOid = null;
        String defaultOidStr = GetPropertyAction
                .privilegedGetProperty("sun.security.jgss.mechanism");
        if (defaultOidStr != null) {
            defOid = GSSUtil.createOid(defaultOidStr);
        }
        DEFAULT_MECH_OID =
            (defOid == null ? GSSUtil.GSS_KRB5_MECH_OID : defOid);
   }

    private ArrayList<PreferencesEntry> preferences =
                        new ArrayList<PreferencesEntry>(5);
    private HashMap<PreferencesEntry, MechanismFactory> factories =
                        new HashMap<PreferencesEntry, MechanismFactory>(5);
    private HashSet<Oid> mechs = new HashSet<Oid>(5);

    private final GSSCaller caller;

    public ProviderList(GSSCaller caller, boolean useNative) {
        this.caller = caller;
        Provider[] provList;
        if (useNative) {
            provList = new Provider[1];
            provList[0] = new SunNativeProvider();
        } else {
            provList = Security.getProviders();
        }

        for (int i = 0; i < provList.length; i++) {
            Provider prov = provList[i];
            try {
                addProviderAtEnd(prov, null);
            } catch (GSSException ge) {
                // Move on to the next provider
                GSSUtil.debug("Error in adding provider " +
                              prov.getName() + ": " + ge);
            }
        } // End of for loop
    }

    /**
     * Determines if the given provider property represents a GSS-API
     * Oid to MechanismFactory mapping.
     * @return true if this is a GSS-API property, false otherwise.
     */
    private boolean isMechFactoryProperty(String prop) {
        return (prop.startsWith(PROV_PROP_PREFIX) ||
                prop.regionMatches(true, 0, // Try ignoring case
                                   PROV_PROP_PREFIX, 0,
                                   PROV_PROP_PREFIX_LEN));
    }

    private Oid getOidFromMechFactoryProperty(String prop)
        throws GSSException {

        String oidPart = prop.substring(PROV_PROP_PREFIX_LEN);
        return new Oid(oidPart);
    }

    // So the existing code do not have to be changed
    synchronized public MechanismFactory getMechFactory(Oid mechOid)
        throws GSSException {
        if (mechOid == null) mechOid = ProviderList.DEFAULT_MECH_OID;
        return getMechFactory(mechOid, null);
    }

    /**
     * Obtains a MechanismFactory for a given mechanism. If the
     * specified provider is not null, then the impl from the
     * provider is used. Otherwise, the most preferred impl based
     * on the configured preferences is used.
     * @param mechOid the oid of the desired mechanism
     * @return a MechanismFactory for the desired mechanism.
     * @throws GSSException when the specified provider does not
     * support the desired mechanism, or when no provider supports
     * the desired mechanism.
     */
    synchronized public MechanismFactory getMechFactory(Oid mechOid,
                                                        Provider p)
        throws GSSException {

        if (mechOid == null) mechOid = ProviderList.DEFAULT_MECH_OID;

        if (p == null) {
            // Iterate thru all preferences to find right provider
            String className;
            PreferencesEntry entry;

            Iterator<PreferencesEntry> list = preferences.iterator();
            while (list.hasNext()) {
                entry = list.next();
                if (entry.impliesMechanism(mechOid)) {
                    MechanismFactory retVal = getMechFactory(entry, mechOid);
                    if (retVal != null) return retVal;
                }
            } // end of while loop
            throw new GSSExceptionImpl(GSSException.BAD_MECH, mechOid);
        } else {
            // Use the impl from the specified provider; return null if the
            // the mech is unsupported by the specified provider.
            PreferencesEntry entry = new PreferencesEntry(p, mechOid);
            return getMechFactory(entry, mechOid);
        }
    }

    /**
     * Helper routine that uses a preferences entry to obtain an
     * implementation of a MechanismFactory from it.
     * @param e the preferences entry that contains the provider and
     * either a null of an explicit oid that matched the oid of the
     * desired mechanism.
     * @param mechOid the oid of the desired mechanism
     * @throws GSSException If the application explicitly requested
     * this entry's provider to be used for the desired mechanism but
     * some problem is encountered
     */
    private MechanismFactory getMechFactory(PreferencesEntry e, Oid mechOid)
        throws GSSException {
        Provider p = e.getProvider();

        /*
         * See if a MechanismFactory was previously instantiated for
         * this provider and mechanism combination.
         */
        PreferencesEntry searchEntry = new PreferencesEntry(p, mechOid);
        MechanismFactory retVal = factories.get(searchEntry);
        if (retVal == null) {
            /*
             * Apparently not. Now try to instantiate this class from
             * the provider.
             */
            String prop = PROV_PROP_PREFIX + mechOid.toString();
            String className = p.getProperty(prop);
            if (className != null) {
                retVal = getMechFactoryImpl(p, className, mechOid, caller);
                factories.put(searchEntry, retVal);
            } else {
                /*
                 * This provider does not support this mechanism.
                 * If the application explicitly requested that
                 * this provider be used for this mechanism, then
                 * throw an exception
                 */
                if (e.getOid() != null) {
                    throw new GSSExceptionImpl(GSSException.BAD_MECH,
                         "Provider " + p.getName() +
                         " does not support mechanism " + mechOid);
                }
            }
        }
        return retVal;
    }

    /**
     * Helper routine to obtain a MechanismFactory implementation
     * from the same class loader as the provider of this
     * implementation.
     * @param p the provider whose classloader must be used for
     * instantiating the desired MechanismFactory
     * @ param className the name of the MechanismFactory class
     * @throws GSSException If some error occurs when trying to
     * instantiate this MechanismFactory.
     */
    private static MechanismFactory getMechFactoryImpl(Provider p,
                                                       String className,
                                                       Oid mechOid,
                                                       GSSCaller caller)
        throws GSSException {

        try {
            Class<?> baseClass = Class.forName(SPI_MECH_FACTORY_TYPE);

            /*
             * Load the implementation class with the same class loader
             * that was used to load the provider.
             * In order to get the class loader of a class, the
             * caller's class loader must be the same as or an ancestor of
             * the class loader being returned. Otherwise, the caller must
             * have "getClassLoader" permission, or a SecurityException
             * will be thrown.
             */

            ClassLoader cl = p.getClass().getClassLoader();
            Class<?> implClass;
            if (cl != null) {
                implClass = cl.loadClass(className);
            } else {
                implClass = Class.forName(className);
            }

            if (baseClass.isAssignableFrom(implClass)) {

                java.lang.reflect.Constructor<?> c =
                                implClass.getConstructor(GSSCaller.class);
                MechanismFactory mf = (MechanismFactory) (c.newInstance(caller));

                if (mf instanceof NativeGSSFactory) {
                    ((NativeGSSFactory) mf).setMech(mechOid);
                }
                return mf;
            } else {
                throw createGSSException(p, className, "is not a " +
                                         SPI_MECH_FACTORY_TYPE, null);
            }
        } catch (ClassNotFoundException e) {
            throw createGSSException(p, className, "cannot be created", e);
        } catch (NoSuchMethodException e) {
            throw createGSSException(p, className, "cannot be created", e);
        } catch (InvocationTargetException e) {
            throw createGSSException(p, className, "cannot be created", e);
        } catch (InstantiationException e) {
            throw createGSSException(p, className, "cannot be created", e);
        } catch (IllegalAccessException e) {
            throw createGSSException(p, className, "cannot be created", e);
        } catch (SecurityException e) {
            throw createGSSException(p, className, "cannot be created", e);
        }
    }

    // Only used by getMechFactoryImpl
    private static GSSException createGSSException(Provider p,
                                                   String className,
                                                   String trailingMsg,
                                                   Exception cause) {
        String errClassInfo = className + " configured by " +
            p.getName() + " for GSS-API Mechanism Factory ";
        return new GSSExceptionImpl(GSSException.BAD_MECH,
                                    errClassInfo + trailingMsg,
                                    cause);
    }

    public Oid[] getMechs() {
        return mechs.toArray(new Oid[] {});
    }

    synchronized public void addProviderAtFront(Provider p, Oid mechOid)
        throws GSSException {

        PreferencesEntry newEntry = new PreferencesEntry(p, mechOid);
        PreferencesEntry oldEntry;
        boolean foundSomeMech;

        Iterator<PreferencesEntry> list = preferences.iterator();
        while (list.hasNext()) {
            oldEntry = list.next();
            if (newEntry.implies(oldEntry))
                list.remove();
        }

        if (mechOid == null) {
            foundSomeMech = addAllMechsFromProvider(p);
        } else {
            String oidStr = mechOid.toString();
            if (p.getProperty(PROV_PROP_PREFIX + oidStr) == null)
                throw new GSSExceptionImpl(GSSException.BAD_MECH,
                                           "Provider " + p.getName()
                                           + " does not support "
                                           + oidStr);
            mechs.add(mechOid);
            foundSomeMech = true;
        }

        if (foundSomeMech) {
            preferences.add(0, newEntry);
        }
    }

    synchronized public void addProviderAtEnd(Provider p, Oid mechOid)
        throws GSSException {

        PreferencesEntry newEntry = new PreferencesEntry(p, mechOid);
        PreferencesEntry oldEntry;
        boolean foundSomeMech;

        Iterator<PreferencesEntry> list = preferences.iterator();
        while (list.hasNext()) {
            oldEntry = list.next();
            if (oldEntry.implies(newEntry))
                return;
        }

        // System.out.println("addProviderAtEnd: No it is not redundant");

        if (mechOid == null)
            foundSomeMech = addAllMechsFromProvider(p);
        else {
            String oidStr = mechOid.toString();
            if (p.getProperty(PROV_PROP_PREFIX + oidStr) == null)
                throw new GSSExceptionImpl(GSSException.BAD_MECH,
                                       "Provider " + p.getName()
                                       + " does not support "
                                       + oidStr);
            mechs.add(mechOid);
            foundSomeMech = true;
        }

        if (foundSomeMech) {
            preferences.add(newEntry);
        }
    }

    /**
     * Helper routine to go through all properties contined in a
     * provider and add its mechanisms to the list of supported
     * mechanisms. If no default mechanism has been assinged so far,
     * it sets the default MechanismFactory and Oid as well.
     * @param p the provider to query
     * @return true if there is at least one mechanism that this
     * provider contributed, false otherwise
     */
    private boolean addAllMechsFromProvider(Provider p) {

        String prop;
        boolean retVal = false;

        // Get all props for this provider
        Enumeration<Object> props = p.keys();

        // See if there are any GSS prop's
        while (props.hasMoreElements()) {
            prop = (String) props.nextElement();
            if (isMechFactoryProperty(prop)) {
                // Ok! This is a GSS provider!
                try {
                    Oid mechOid = getOidFromMechFactoryProperty(prop);
                    mechs.add(mechOid);
                    retVal = true;
                } catch (GSSException e) {
                    // Skip to next property
                    GSSUtil.debug("Ignore the invalid property " +
                                  prop + " from provider " + p.getName());
                }
            } // Processed GSS property
        } // while loop

        return retVal;

    }

    /**
     * Stores a provider and a mechanism oid indicating that the
     * provider should be used for the mechanism. If the mechanism
     * Oid is null, then it indicates that this preference holds for
     * any mechanism.<p>
     *
     * The ProviderList maintains an ordered list of
     * PreferencesEntry's and iterates thru them as it tries to
     * instantiate MechanismFactory's.
     */
    private static final class PreferencesEntry {
        private Provider p;
        private Oid oid;
        PreferencesEntry(Provider p, Oid oid) {
            this.p = p;
            this.oid = oid;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof PreferencesEntry)) {
                return false;
            }

            PreferencesEntry that = (PreferencesEntry)other;
            if (this.p.getName().equals(that.p.getName())) {
                if (this.oid != null && that.oid != null) {
                    return this.oid.equals(that.oid);
                } else {
                    return (this.oid == null && that.oid == null);
                }
            }

            return false;
        }

        public int hashCode() {
            int result = 17;

            result = 37 * result + p.getName().hashCode();
            if (oid != null) {
                result = 37 * result + oid.hashCode();
            }

            return result;
        }

        /**
         * Determines if a preference implies another. A preference
         * implies another if the latter is subsumed by the
         * former. e.g., <Provider1, null> implies <Provider1, OidX>
         * because the null in the former indicates that it should
         * be used for all mechanisms.
         */
        boolean implies(Object other) {

            if (other instanceof PreferencesEntry) {
                PreferencesEntry temp = (PreferencesEntry) other;
                return (equals(temp) ||
                        p.getName().equals(temp.p.getName()) &&
                        oid == null);
            } else {
                return false;
            }
        }

        Provider getProvider() {
            return p;
        }

        Oid getOid() {
            return oid;
        }

        /**
         * Determines if this entry is applicable to the desired
         * mechanism. The entry is applicable to the desired mech if
         * it contains the same oid or if it contains a null oid
         * indicating that it is applicable to all mechs.
         * @param mechOid the desired mechanism
         * @return true if the provider in this entry should be
         * queried for this mechanism.
         */
        boolean impliesMechanism(Oid oid) {
            return (this.oid == null || this.oid.equals(oid));
        }

        // For debugging
        public String toString() {
            StringBuilder sb = new StringBuilder("<");
            sb.append(p.getName());
            sb.append(", ");
            sb.append(oid);
            sb.append(">");
            return sb.toString();
        }
    }
}
