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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolverSpi;

/**
 * This {@link StorageResolverSpi} makes a single {@link X509Certificate}
 * available to the {@link com.sun.org.apache.xml.internal.security.keys.storage.StorageResolver}.
 */
public class SingleCertificateResolver extends StorageResolverSpi {

    /** Field certificate */
    private final X509Certificate certificate;

    /**
     * @param x509cert the single {@link X509Certificate}
     */
    public SingleCertificateResolver(X509Certificate x509cert) {
        this.certificate = x509cert;
    }

    /** {@inheritDoc} */
    public Iterator<Certificate> getIterator() {
        return new InternalIterator(this.certificate);
    }

    /**
     * Class InternalIterator
     */
    static class InternalIterator implements Iterator<Certificate> {

        /** Field alreadyReturned */
        private boolean alreadyReturned;

        /** Field certificate */
        private final X509Certificate certificate;

        /**
         * Constructor InternalIterator
         *
         * @param x509cert
         */
        public InternalIterator(X509Certificate x509cert) {
            this.certificate = x509cert;
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            return !this.alreadyReturned;
        }

        /** {@inheritDoc} */
        public Certificate next() {
            if (this.alreadyReturned) {
                throw new NoSuchElementException();
            }
            this.alreadyReturned = true;
            return this.certificate;
        }

        /**
         * Method remove
         */
        public void remove() {
            throw new UnsupportedOperationException("Can't remove keys from KeyStore");
        }
    }
}
