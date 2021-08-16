/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.keys.storage.implementations;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolverException;
import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolverSpi;

/**
 * Makes the Certificates from a JAVA {@link KeyStore} object available to the
 * {@link com.sun.org.apache.xml.internal.security.keys.storage.StorageResolver}.
 */
public class KeyStoreResolver extends StorageResolverSpi {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(KeyStoreResolver.class);

    /** Field keyStore */
    private final KeyStore keyStore;

    /**
     * Constructor KeyStoreResolver
     *
     * @param keyStore is the keystore which contains the Certificates
     * @throws StorageResolverException
     */
    public KeyStoreResolver(KeyStore keyStore) throws StorageResolverException {
        this.keyStore = keyStore;
        // Do a quick check on the keystore
        try {
            keyStore.aliases();
        } catch (KeyStoreException ex) {
            throw new StorageResolverException(ex);
        }
    }

    /** {@inheritDoc} */
    public Iterator<Certificate> getIterator() {
        return new KeyStoreIterator(this.keyStore);
    }

    /**
     * Class KeyStoreIterator
     */
    static class KeyStoreIterator implements Iterator<Certificate> {

        private final List<Certificate> certs;

        private int i;

        /**
         * Constructor KeyStoreIterator
         *
         * @param keyStore
         */
        public KeyStoreIterator(KeyStore keyStore) {

            List<Certificate> tmpCerts = new ArrayList<>();
            try {
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = keyStore.getCertificate(alias);
                    if (cert != null) {
                        tmpCerts.add(cert);
                    }
                }
            } catch (KeyStoreException ex) {
                LOG.debug("Error reading certificates: {}", ex.getMessage());
            }

            certs = Collections.unmodifiableList(tmpCerts);
            this.i = 0;
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            return this.i < this.certs.size();
        }

        /** {@inheritDoc} */
        public Certificate next() {
            if (hasNext()) {
                return this.certs.get(this.i++);
            }

            throw new NoSuchElementException();
        }

        /**
         * Method remove
         */
        public void remove() {
            throw new UnsupportedOperationException("Can't remove keys from KeyStore");
        }

    }

}
