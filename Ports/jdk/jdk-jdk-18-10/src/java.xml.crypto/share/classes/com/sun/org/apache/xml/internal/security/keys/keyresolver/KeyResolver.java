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
package com.sun.org.apache.xml.internal.security.keys.keyresolver;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.DEREncodedKeyValueResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.DSAKeyValueResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.ECKeyValueResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.KeyInfoReferenceResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.RSAKeyValueResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.RetrievalMethodResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.X509CertificateResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.X509DigestResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.X509IssuerSerialResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.X509SKIResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.X509SubjectNameResolver;
import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolver;
import com.sun.org.apache.xml.internal.security.utils.JavaUtils;

/**
 * KeyResolver is factory class for subclass of KeyResolverSpi that
 * represent child element of KeyInfo.
 */
public class KeyResolver {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(KeyResolver.class);

    private static List<KeyResolverSpi> resolverList = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean defaultResolversAdded = new AtomicBoolean();

    /**
     * Method length
     *
     * @return the length of resolvers registered
     */
    public static int length() {
        return resolverList.size();
    }

    /**
     * Method getX509Certificate
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return The certificate represented by the element.
     *
     * @throws KeyResolverException
     */
    public static final X509Certificate getX509Certificate(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        for (KeyResolverSpi resolver : resolverList) {
            if (resolver == null) {
                Object[] exArgs = {
                        element != null
                                && element.getNodeType() == Node.ELEMENT_NODE
                                ? element.getTagName() : "null"
                };

                throw new KeyResolverException("utils.resolver.noClass", exArgs);
            }
            LOG.debug("check resolvability by class {}", resolver.getClass());

            X509Certificate cert = resolver.engineLookupResolveX509Certificate(element, baseURI, storage, secureValidation);
            if (cert != null) {
                return cert;
            }
        }

        Object[] exArgs = {
                element != null && element.getNodeType() == Node.ELEMENT_NODE
                        ? element.getTagName() : "null"
        };

        throw new KeyResolverException("utils.resolver.noClass", exArgs);
    }

    /**
     * Method getPublicKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return the public key contained in the element
     *
     * @throws KeyResolverException
     */
    public static final PublicKey getPublicKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        for (KeyResolverSpi resolver : resolverList) {
            if (resolver == null) {
                Object[] exArgs = {
                        element != null
                                && element.getNodeType() == Node.ELEMENT_NODE
                                ? element.getTagName() : "null"
                };

                throw new KeyResolverException("utils.resolver.noClass", exArgs);
            }
            LOG.debug("check resolvability by class {}", resolver.getClass());

            PublicKey cert = resolver.engineLookupAndResolvePublicKey(element, baseURI, storage, secureValidation);
            if (cert != null) {
                return cert;
            }
        }

        Object[] exArgs = {
                element != null && element.getNodeType() == Node.ELEMENT_NODE
                        ? element.getTagName() : "null"
        };

        throw new KeyResolverException("utils.resolver.noClass", exArgs);
    }

    /**
     * This method is used for registering {@link KeyResolverSpi}s which are
     * available to <I>all</I> {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} objects. This means that
     * personalized {@link KeyResolverSpi}s should only be registered directly
     * to the {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} using
     * {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo#registerInternalKeyResolver}.
     * Please note that this method will create a new copy of the underlying array, as the
     * underlying collection is a CopyOnWriteArrayList.
     *
     * @param className
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the key resolver
     */
    public static void register(String className) throws
            ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        JavaUtils.checkRegisterPermission();
        KeyResolverSpi keyResolverSpi =
            (KeyResolverSpi) JavaUtils.newInstanceWithEmptyConstructor(ClassLoaderUtils.loadClass(className, KeyResolver.class));
        register(keyResolverSpi, false);
    }

    /**
     * This method is used for registering {@link KeyResolverSpi}s which are
     * available to <I>all</I> {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} objects. This means that
     * personalized {@link KeyResolverSpi}s should only be registered directly
     * to the {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} using
     * {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo#registerInternalKeyResolver}.
     * Please note that this method will create a new copy of the underlying array, as the
     * underlying collection is a CopyOnWriteArrayList.
     *
     * @param className
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the key resolver
     */
    public static void registerAtStart(String className) {
        JavaUtils.checkRegisterPermission();
        KeyResolverSpi keyResolverSpi = null;
        Exception ex = null;
        try {
            keyResolverSpi = (KeyResolverSpi) JavaUtils.newInstanceWithEmptyConstructor(
                    ClassLoaderUtils.loadClass(className, KeyResolver.class));
            register(keyResolverSpi, true);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            ex = e;
        }

        if (ex != null) {
            throw (IllegalArgumentException) new
                    IllegalArgumentException("Invalid KeyResolver class name").initCause(ex);
        }
    }

    /**
     * This method is used for registering {@link KeyResolverSpi}s which are
     * available to <I>all</I> {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} objects. This means that
     * personalized {@link KeyResolverSpi}s should only be registered directly
     * to the {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} using
     * {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo#registerInternalKeyResolver}.
     * Please note that this method will create a new copy of the underlying array, as the
     * underlying collection is a CopyOnWriteArrayList.
     *
     * @param keyResolverSpi a KeyResolverSpi instance to register
     * @param start whether to register the KeyResolverSpi at the start of the list or not
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the key resolver
     */
    public static void register(
        KeyResolverSpi keyResolverSpi,
        boolean start
    ) {
        JavaUtils.checkRegisterPermission();
        if (start) {
            resolverList.add(0, keyResolverSpi);
        } else {
            resolverList.add(keyResolverSpi);
        }
    }

    /**
     * This method is used for registering {@link KeyResolverSpi}s which are
     * available to <I>all</I> {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} objects. This means that
     * personalized {@link KeyResolverSpi}s should only be registered directly
     * to the {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo} using
     * {@link com.sun.org.apache.xml.internal.security.keys.KeyInfo#registerInternalKeyResolver}.
     * The KeyResolverSpi instances are not registered as a global resolver.
     *
     *
     * @param classNames
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the key resolver
     */
    public static void registerClassNames(List<String> classNames)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        JavaUtils.checkRegisterPermission();
        List<KeyResolverSpi> keyResolverList = new ArrayList<>(classNames.size());
        for (String className : classNames) {
            KeyResolverSpi keyResolverSpi = (KeyResolverSpi)JavaUtils
                    .newInstanceWithEmptyConstructor(ClassLoaderUtils.loadClass(className, KeyResolver.class));
            keyResolverList.add(keyResolverSpi);
        }
        resolverList.addAll(keyResolverList);
    }

    /**
     * This method registers the default resolvers.
     */
    public static void registerDefaultResolvers() {
        // Add a guard so that we don't repeatedly add the default resolvers
        if (defaultResolversAdded.compareAndSet(false, true)) {
            List<KeyResolverSpi> keyResolverList = new ArrayList<>();
            keyResolverList.add(new RSAKeyValueResolver());
            keyResolverList.add(new DSAKeyValueResolver());
            keyResolverList.add(new X509CertificateResolver());
            keyResolverList.add(new X509SKIResolver());
            keyResolverList.add(new RetrievalMethodResolver());
            keyResolverList.add(new X509SubjectNameResolver());
            keyResolverList.add(new X509IssuerSerialResolver());
            keyResolverList.add(new DEREncodedKeyValueResolver());
            keyResolverList.add(new KeyInfoReferenceResolver());
            keyResolverList.add(new X509DigestResolver());
            keyResolverList.add(new ECKeyValueResolver());

            resolverList.addAll(keyResolverList);
        }
    }

    /**
     * Iterate over the KeyResolverSpi instances
     */
    static class ResolverIterator implements Iterator<KeyResolverSpi> {
        private List<KeyResolverSpi> res;
        private Iterator<KeyResolverSpi> it;

        public ResolverIterator(List<KeyResolverSpi> list) {
            res = list;
            it = res.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public KeyResolverSpi next() {
            KeyResolverSpi resolver = it.next();
            if (resolver == null) {
                throw new RuntimeException("utils.resolver.noClass");
            }

            return resolver;
        }

        public void remove() {
            throw new UnsupportedOperationException("Can't remove resolvers using the iterator");
        }
    }

    public static Iterator<KeyResolverSpi> iterator() {
        return new ResolverIterator(resolverList);
    }
}
