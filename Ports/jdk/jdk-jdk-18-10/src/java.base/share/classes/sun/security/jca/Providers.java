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

import java.security.Provider;
import sun.security.x509.AlgorithmId;

/**
 * Collection of methods to get and set provider list. Also includes
 * special code for the provider list during JAR verification.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
public class Providers {

    private static final ThreadLocal<ProviderList> threadLists =
        new ThreadLocal<>();

    // number of threads currently using thread-local provider lists
    // tracked to allow an optimization if == 0
    private static volatile int threadListsUsed;

    // current system-wide provider list
    // Note volatile immutable object, so no synchronization needed.
    private static volatile ProviderList providerList;

    static {
        // set providerList to empty list first in case initialization somehow
        // triggers a getInstance() call (although that should not happen)
        providerList = ProviderList.EMPTY;
        providerList = ProviderList.fromSecurityProperties();
    }

    private Providers() {
        // empty
    }

    // After the switch to modules, JDK providers are all in modules and JDK
    // no longer needs to load signed jars during start up.
    //
    // However, for earlier releases, it need special handling to resolve
    // circularities when loading signed JAR files during startup. The code
    // below is part of that.
    //
    // Basically, before we load data from a signed JAR file, we parse
    // the PKCS#7 file and verify the signature. We need a
    // CertificateFactory, Signatures, etc. to do that. We have to make
    // sure that we do not try to load the implementation from the JAR
    // file we are just verifying.
    //
    // To avoid that, we use different provider settings during JAR
    // verification.  However, we do not want those provider settings to
    // interfere with other parts of the system. Therefore, we make them local
    // to the Thread executing the JAR verification code.
    //
    // The code here is used by sun.security.util.SignatureFileVerifier.
    // See there for details.

    // Hardcoded names of providers to use for JAR verification.
    // MUST NOT be on the bootclasspath and not in signed JAR files.
    private static final String[] jarVerificationProviders = {
        "SUN",
        "SunRsaSign",
        // Note: when SunEC is in a signed JAR file, it's not signed
        // by EC algorithms. So it's still safe to be listed here.
        "SunEC",
        "SunJCE",
    };

    // Return Sun provider.
    // This method should only be called by
    // sun.security.util.ManifestEntryVerifier and java.security.SecureRandom.
    public static Provider getSunProvider() {
        return new sun.security.provider.Sun();
    }

    /**
     * Start JAR verification. This sets a special provider list for
     * the current thread. You MUST save the return value from this
     * method and you MUST call stopJarVerification() with that object
     * once you are done.
     */
    public static Object startJarVerification() {
        ProviderList currentList = getProviderList();
        ProviderList jarList = currentList.getJarList(jarVerificationProviders);
        if (jarList.getProvider("SUN") == null) {
            // add backup provider
            Provider p;
            try {
                p = new sun.security.provider.VerificationProvider();
            } catch (Exception e) {
                throw new RuntimeException("Missing provider for jar verification", e);
            }
            ProviderList.add(jarList, p);
        }
        // return the old thread-local provider list, usually null
        return beginThreadProviderList(jarList);
    }

    /**
     * Stop JAR verification. Call once you have completed JAR verification.
     */
    public static void stopJarVerification(Object obj) {
        // restore old thread-local provider list
        endThreadProviderList((ProviderList)obj);
    }

    /**
     * Return the current ProviderList. If the thread-local list is set,
     * it is returned. Otherwise, the system wide list is returned.
     */
    public static ProviderList getProviderList() {
        ProviderList list = getThreadProviderList();
        if (list == null) {
            list = getSystemProviderList();
        }
        return list;
    }

    /**
     * Set the current ProviderList. Affects the thread-local list if set,
     * otherwise the system wide list.
     */
    public static void setProviderList(ProviderList newList) {
        if (getThreadProviderList() == null) {
            setSystemProviderList(newList);
        } else {
            changeThreadProviderList(newList);
        }
        clearCachedValues();
    }

    /**
     * Clears the cached provider-list-specific values. These values need to
     * be re-generated whenever provider list is changed. The logic for
     * generating them is in the respective classes.
     */
    private static void clearCachedValues() {
        JCAUtil.clearDefSecureRandom();
        AlgorithmId.clearAliasOidsTable();
    }

    /**
     * Get the full provider list with invalid providers (those that
     * could not be loaded) removed. This is the list we need to
     * present to applications.
     */
    public static ProviderList getFullProviderList() {
        ProviderList list;
        synchronized (Providers.class) {
            list = getThreadProviderList();
            if (list != null) {
                ProviderList newList = list.removeInvalid();
                if (newList != list) {
                    changeThreadProviderList(newList);
                    list = newList;
                }
                return list;
            }
        }
        list = getSystemProviderList();
        ProviderList newList = list.removeInvalid();
        if (newList != list) {
            setSystemProviderList(newList);
            list = newList;
        }
        return list;
    }

    private static ProviderList getSystemProviderList() {
        return providerList;
    }

    private static void setSystemProviderList(ProviderList list) {
        providerList = list;
    }

    public static ProviderList getThreadProviderList() {
        // avoid accessing the threadlocal if none are currently in use
        // (first use of ThreadLocal.get() for a Thread allocates a Map)
        if (threadListsUsed == 0) {
            return null;
        }
        return threadLists.get();
    }

    // Change the thread local provider list. Use only if the current thread
    // is already using a thread local list and you want to change it in place.
    // In other cases, use the begin/endThreadProviderList() methods.
    private static void changeThreadProviderList(ProviderList list) {
        threadLists.set(list);
    }

    /**
     * Methods to manipulate the thread local provider list. It is for use by
     * JAR verification (see above).
     *
     * It should be used as follows:
     *
     *   ProviderList list = ...;
     *   ProviderList oldList = Providers.beginThreadProviderList(list);
     *   try {
     *     // code that needs thread local provider list
     *   } finally {
     *     Providers.endThreadProviderList(oldList);
     *   }
     *
     */

    public static synchronized ProviderList beginThreadProviderList(ProviderList list) {
        if (ProviderList.debug != null) {
            ProviderList.debug.println("ThreadLocal providers: " + list);
        }
        ProviderList oldList = threadLists.get();
        threadListsUsed++;
        threadLists.set(list);
        return oldList;
    }

    public static synchronized void endThreadProviderList(ProviderList list) {
        if (list == null) {
            if (ProviderList.debug != null) {
                ProviderList.debug.println("Disabling ThreadLocal providers");
            }
            threadLists.remove();
        } else {
            if (ProviderList.debug != null) {
                ProviderList.debug.println
                    ("Restoring previous ThreadLocal providers: " + list);
            }
            threadLists.set(list);
        }
        threadListsUsed--;
    }

}
