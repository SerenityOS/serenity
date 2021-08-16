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
package com.sun.org.apache.xml.internal.security.keys.storage;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.sun.org.apache.xml.internal.security.keys.storage.implementations.KeyStoreResolver;
import com.sun.org.apache.xml.internal.security.keys.storage.implementations.SingleCertificateResolver;

/**
 * This class collects customized resolvers for Certificates.
 */
public class StorageResolver {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(StorageResolver.class);

    /** Field storageResolvers */
    private final List<StorageResolverSpi> storageResolvers = new ArrayList<>();

    /**
     * Constructor StorageResolver
     *
     * @param resolver
     */
    public StorageResolver(StorageResolverSpi resolver) {
        this.add(resolver);
    }

    /**
     * Constructor StorageResolver
     *
     * @param keyStore
     */
    public StorageResolver(KeyStore keyStore) {
        this.add(keyStore);
    }

    /**
     * Constructor StorageResolver
     *
     * @param x509certificate
     */
    public StorageResolver(X509Certificate x509certificate) {
        this.add(x509certificate);
    }

    /**
     * Method addResolver
     *
     * @param resolver
     */
    public void add(StorageResolverSpi resolver) {
        this.storageResolvers.add(resolver);
    }

    /**
     * Method addKeyStore
     *
     * @param keyStore
     */
    public void add(KeyStore keyStore) {
        try {
            this.add(new KeyStoreResolver(keyStore));
        } catch (StorageResolverException ex) {
            LOG.error("Could not add KeyStore because of: ", ex);
        }
    }

    /**
     * Method addCertificate
     *
     * @param x509certificate
     */
    public void add(X509Certificate x509certificate) {
        this.add(new SingleCertificateResolver(x509certificate));
    }

    /**
     * Method getIterator
     * @return the iterator for the resolvers.
     */
    public Iterator<Certificate> getIterator() {
        return new StorageResolverIterator(this.storageResolvers.iterator());
    }

    /**
     * Class StorageResolverIterator
     * This iterates over all the Certificates found in all the resolvers.
     */
    static class StorageResolverIterator implements Iterator<Certificate> {

        /** Field resolvers */
        private final Iterator<StorageResolverSpi> resolvers;

        /** Field currentResolver */
        private Iterator<Certificate> currentResolver;

        /**
         * Constructor StorageResolverIterator
         *
         * @param resolvers
         */
        public StorageResolverIterator(Iterator<StorageResolverSpi> resolvers) {
            this.resolvers = resolvers;
            currentResolver = findNextResolver();
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            if (currentResolver == null) {
                return false;
            }

            if (currentResolver.hasNext()) {
                return true;
            }

            currentResolver = findNextResolver();
            return currentResolver != null;
        }

        /** {@inheritDoc} */
        public Certificate next() {
            if (hasNext()) {
                return currentResolver.next();
            }

            throw new NoSuchElementException();
        }

        /**
         * Method remove
         */
        public void remove() {
            throw new UnsupportedOperationException("Can't remove keys from KeyStore");
        }

        // Find the next storage with at least one element and return its Iterator
        private Iterator<Certificate> findNextResolver() {
            while (resolvers.hasNext()) {
                StorageResolverSpi resolverSpi = resolvers.next();
                Iterator<Certificate> iter = resolverSpi.getIterator();
                if (iter.hasNext()) {
                    return iter;
                }
            }

            return null;
        }
    }
}
