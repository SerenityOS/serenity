/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.*;
import java.lang.ref.WeakReference;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import sun.security.action.*;
import sun.security.util.FilePaths;
import sun.security.validator.TrustStoreUtil;

/**
 * Collection of static utility methods to manage the default trusted KeyStores
 * effectively.
 */
final class TrustStoreManager {

    // A singleton service to manage the default trusted KeyStores effectively.
    private static final TrustAnchorManager tam = new TrustAnchorManager();

    // Restrict instantiation of this class.
    private TrustStoreManager() {
        // empty
    }

    /**
     * Return an unmodifiable set of all trusted X509Certificates contained
     * in the default trusted KeyStore.
     */
    public static Set<X509Certificate> getTrustedCerts() throws Exception {
        return tam.getTrustedCerts(TrustStoreDescriptor.createInstance());
    }

    /**
     * Return an instance of the default trusted KeyStore.
     */
    public static KeyStore getTrustedKeyStore() throws Exception {
        return tam.getKeyStore(TrustStoreDescriptor.createInstance());
    }

    /**
     * A descriptor of the default trusted KeyStore.
     *
     * The preference of the default trusted KeyStore is:
     *    javax.net.ssl.trustStore
     *    jssecacerts
     *    cacerts
     */
    private static final class TrustStoreDescriptor {
        private static final String fileSep = File.separator;
        private static final String defaultStorePath =
                GetPropertyAction.privilegedGetProperty("java.home") +
                fileSep + "lib" + fileSep + "security";
        private static final String defaultStore = FilePaths.cacerts();
        private static final String jsseDefaultStore =
                defaultStorePath + fileSep + "jssecacerts";

        // the trust store name
        private final String storeName;

        // the trust store type, JKS/PKCS12
        private final String storeType;

        // the provider of the trust store
        private final String storeProvider;

        // the password used for the trust store
        private final String storePassword;

        // the File object of the trust store
        private final File storeFile;

        // the last modified time of the store
        private final long lastModified;

        private TrustStoreDescriptor(String storeName, String storeType,
                String storeProvider, String storePassword,
                File storeFile, long lastModified) {
            this.storeName = storeName;
            this.storeType = storeType;
            this.storeProvider = storeProvider;
            this.storePassword = storePassword;
            this.storeFile = storeFile;
            this.lastModified = lastModified;

            if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                SSLLogger.fine(
                    "trustStore is: " + storeName + "\n" +
                    "trustStore type is: " + storeType + "\n" +
                    "trustStore provider is: " + storeProvider + "\n" +
                    "the last modified time is: " + (new Date(lastModified)));
            }
        }

        /**
         * Create an instance of TrustStoreDescriptor for the default
         * trusted KeyStore.
         */
        @SuppressWarnings({"removal","Convert2Lambda"})
        static TrustStoreDescriptor createInstance() {
             return AccessController.doPrivileged(
                    new PrivilegedAction<TrustStoreDescriptor>() {

                @Override
                public TrustStoreDescriptor run() {
                    // Get the system properties for trust store.
                    String storePropName = System.getProperty(
                            "javax.net.ssl.trustStore", jsseDefaultStore);
                    String storePropType = System.getProperty(
                            "javax.net.ssl.trustStoreType",
                            KeyStore.getDefaultType());
                    String storePropProvider = System.getProperty(
                            "javax.net.ssl.trustStoreProvider", "");
                    String storePropPassword = System.getProperty(
                            "javax.net.ssl.trustStorePassword", "");

                    String temporaryName = "";
                    File temporaryFile = null;
                    long temporaryTime = 0L;
                    if (!"NONE".equals(storePropName)) {
                        String[] fileNames =
                                new String[] {storePropName, defaultStore};
                        for (String fileName : fileNames) {
                            File f = new File(fileName);
                            if (f.isFile() && f.canRead()) {
                                temporaryName = fileName;
                                temporaryFile = f;
                                temporaryTime = f.lastModified();

                                break;
                            }

                            // Not break, the file is inaccessible.
                            if (SSLLogger.isOn &&
                                    SSLLogger.isOn("trustmanager")) {
                                SSLLogger.fine(
                                        "Inaccessible trust store: " +
                                        fileName);
                            }
                        }
                    } else {
                        temporaryName = storePropName;
                    }

                    return new TrustStoreDescriptor(
                            temporaryName, storePropType, storePropProvider,
                            storePropPassword, temporaryFile, temporaryTime);
                }
            });
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof TrustStoreDescriptor) {
                TrustStoreDescriptor that = (TrustStoreDescriptor)obj;
                return ((this.lastModified == that.lastModified) &&
                    Objects.equals(this.storeName, that.storeName) &&
                    Objects.equals(this.storeType, that.storeType) &&
                    Objects.equals(this.storeProvider, that.storeProvider));
            }

            return false;
        }


        // Please be careful if computing security-sensitive attributes'
        // hash code.  For example the storePassword should not be computed.
        @Override
        public int hashCode() {
            int result = 17;

            if (storeName != null && !storeName.isEmpty()) {
                result = 31 * result + storeName.hashCode();
            }

            if (storeType != null && !storeType.isEmpty()) {
                result = 31 * result + storeType.hashCode();
            }

            if (storeProvider != null && !storeProvider.isEmpty()) {
                result = 31 * result + storeProvider.hashCode();
            }

            if (storeFile != null) {
                result = 31 * result + storeFile.hashCode();
            }

            if (lastModified != 0L) {
                result = (int)(31 * result + lastModified);
            }

            return result;
        }
    }

    /**
     * The trust anchors manager used to expedite the performance.
     *
     * This class can be used to provide singleton services to access default
     * trust KeyStore more effectively.
     */
    private static final class TrustAnchorManager {
        // Last trust store descriptor.
        private TrustStoreDescriptor descriptor;

        // The key store used for the trust anchors.
        //
        // Use weak reference so that the heavy loaded KeyStore object can
        // be atomically cleared, and reloaded if needed.
        private WeakReference<KeyStore> ksRef;

        // The trusted X.509 certificates in the key store.
        //
        // Use weak reference so that the heavy loaded certificates collection
        // objects can be atomically cleared, and reloaded if needed.
        private WeakReference<Set<X509Certificate>> csRef;

        private final ReentrantLock tamLock = new ReentrantLock();

        private TrustAnchorManager() {
            this.descriptor = null;
            this.ksRef = new WeakReference<>(null);
            this.csRef = new WeakReference<>(null);
        }

        /**
         * Get the default trusted KeyStore with the specified descriptor.
         *
         * @return null if the underlying KeyStore is not available.
         */
        KeyStore getKeyStore(
                TrustStoreDescriptor descriptor) throws Exception {

            TrustStoreDescriptor temporaryDesc = this.descriptor;
            KeyStore ks = ksRef.get();
            if ((ks != null) && descriptor.equals(temporaryDesc)) {
                return ks;
            }

            tamLock.lock();
            try {
                // double check
                ks = ksRef.get();
                if ((ks != null) && descriptor.equals(temporaryDesc)) {
                    return ks;
                }

                // Reload a new key store.
                if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                    SSLLogger.fine("Reload the trust store");
                }

                ks = loadKeyStore(descriptor);
                this.descriptor = descriptor;
                this.ksRef = new WeakReference<>(ks);
            } finally {
                tamLock.unlock();
            }

            return ks;
        }

        /**
         * Get trusted certificates in the default trusted KeyStore with
         * the specified descriptor.
         *
         * @return empty collection if the underlying KeyStore is not available.
         */
        Set<X509Certificate> getTrustedCerts(
                TrustStoreDescriptor descriptor) throws Exception {

            KeyStore ks = null;
            TrustStoreDescriptor temporaryDesc = this.descriptor;
            Set<X509Certificate> certs = csRef.get();
            if ((certs != null) && descriptor.equals(temporaryDesc)) {
                return certs;
            }

            tamLock.lock();
            try {
                // double check
                temporaryDesc = this.descriptor;
                certs = csRef.get();
                if (certs != null) {
                    if (descriptor.equals(temporaryDesc)) {
                        return certs;
                    } else {
                        // Use the new descriptor.
                        this.descriptor = descriptor;
                    }
                } else {
                    // Try to use the cached store at first.
                    if (descriptor.equals(temporaryDesc)) {
                        ks = ksRef.get();
                    } else {
                        // Use the new descriptor.
                        this.descriptor = descriptor;
                    }
                }

                // Reload the trust store if needed.
                if (ks == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                        SSLLogger.fine("Reload the trust store");
                    }
                    ks = loadKeyStore(descriptor);
                    this.ksRef = new WeakReference<>(ks);
                }

                // Reload trust certs from the key store.
                if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                    SSLLogger.fine("Reload trust certs");
                }

                certs = loadTrustedCerts(ks);
                if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                    SSLLogger.fine("Reloaded " + certs.size() + " trust certs");
                }

                this.csRef = new WeakReference<>(certs);
            } finally {
                tamLock.unlock();
            }

            return certs;
        }

        /**
         * Load the KeyStore as described in the specified descriptor.
         */
        private static KeyStore loadKeyStore(
                TrustStoreDescriptor descriptor) throws Exception {
            if (!"NONE".equals(descriptor.storeName) &&
                    descriptor.storeFile == null) {

                // No file available, no KeyStore available.
                if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                    SSLLogger.fine("No available key store");
                }

                return null;
            }

            KeyStore ks;
            if (descriptor.storeProvider.isEmpty()) {
                ks = KeyStore.getInstance(descriptor.storeType);
            } else {
                ks = KeyStore.getInstance(
                        descriptor.storeType, descriptor.storeProvider);
            }

            char[] password = null;
            if (!descriptor.storePassword.isEmpty()) {
                password = descriptor.storePassword.toCharArray();
            }

            if (!"NONE".equals(descriptor.storeName)) {
                try (@SuppressWarnings("removal") FileInputStream fis = AccessController.doPrivileged(
                        new OpenFileInputStreamAction(descriptor.storeFile))) {
                    ks.load(fis, password);
                } catch (FileNotFoundException fnfe) {
                    // No file available, no KeyStore available.
                    if (SSLLogger.isOn && SSLLogger.isOn("trustmanager")) {
                        SSLLogger.fine(
                            "Not available key store: " + descriptor.storeName);
                    }

                    return null;
                }
            } else {
                ks.load(null, password);
            }

            return ks;
        }

        /**
         * Load trusted certificates from the specified KeyStore.
         */
        private static Set<X509Certificate> loadTrustedCerts(KeyStore ks) {
            if (ks == null) {
                return Collections.<X509Certificate>emptySet();
            }

            return TrustStoreUtil.getTrustedCerts(ks);
        }
    }
}
