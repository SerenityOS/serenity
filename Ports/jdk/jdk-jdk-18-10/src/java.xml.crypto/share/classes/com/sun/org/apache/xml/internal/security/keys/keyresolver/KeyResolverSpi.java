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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;

import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolver;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is an abstract class to resolve a Key of some kind given a KeyInfo element.
 *
 * If you want the your KeyResolver, at firstly you must extend this class, and register
 * as following in config.xml
 * <PRE>
 *  &lt;KeyResolver URI="http://www.w3.org/2000/09/xmldsig#KeyValue"
 *   JAVACLASS="MyPackage.MyKeyValueImpl"//gt;
 * </PRE>
 *
 * Extensions of this class must be thread-safe.
 */
public abstract class KeyResolverSpi {

    /**
     * This method returns whether the KeyResolverSpi is able to perform the requested action.
     *
     * @param element
     * @param baseURI
     * @param storage
     * @return whether the KeyResolverSpi is able to perform the requested action.
     */
    protected abstract boolean engineCanResolve(Element element, String baseURI, StorageResolver storage);

    /**
     * Method engineResolvePublicKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved public key from the registered from the element.
     *
     * @throws KeyResolverException
     */
    protected abstract PublicKey engineResolvePublicKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException;

    /**
     * Method engineLookupAndResolvePublicKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved public key from the registered from the element.
     *
     * @throws KeyResolverException
     */
    public PublicKey engineLookupAndResolvePublicKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        if (!engineCanResolve(element, baseURI, storage)) {
            return null;
        }
        return engineResolvePublicKey(element, baseURI, storage, secureValidation);
    }

    /**
     * Method engineResolveCertificate
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved X509Certificate key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    protected abstract X509Certificate engineResolveX509Certificate(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException;

    /**
     * Method engineLookupResolveX509Certificate
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved X509Certificate key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    public X509Certificate engineLookupResolveX509Certificate(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        if (!engineCanResolve(element, baseURI, storage)) {
            return null;
        }
        return engineResolveX509Certificate(element, baseURI, storage, secureValidation);

    }
    /**
     * Method engineResolveSecretKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved SecretKey key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    protected abstract SecretKey engineResolveSecretKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException;

    /**
     * Method engineLookupAndResolveSecretKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved SecretKey key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    public SecretKey engineLookupAndResolveSecretKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        if (!engineCanResolve(element, baseURI, storage)) {
            return null;
        }
        return engineResolveSecretKey(element, baseURI, storage, secureValidation);
    }

    /**
     * Method engineResolvePrivateKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved PrivateKey key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    protected abstract PrivateKey engineResolvePrivateKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException;

    /**
     * Method engineLookupAndResolvePrivateKey
     *
     * @param element
     * @param baseURI
     * @param storage
     * @param secureValidation
     * @return resolved PrivateKey key from the registered from the elements
     *
     * @throws KeyResolverException
     */
    public PrivateKey engineLookupAndResolvePrivateKey(
        Element element, String baseURI, StorageResolver storage, boolean secureValidation
    ) throws KeyResolverException {
        if (!engineCanResolve(element, baseURI, storage)) {
            return null;
        }
        return engineResolvePrivateKey(element, baseURI, storage, secureValidation);
    }

    /**
     * Parses a byte array and returns the parsed Element.
     *
     * @param bytes
     * @return the Document Element after parsing bytes
     * @throws KeyResolverException if something goes wrong
     */
    protected static Element getDocFromBytes(byte[] bytes, boolean secureValidation) throws KeyResolverException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Document doc = XMLUtils.read(is, secureValidation);
            return doc.getDocumentElement();
        } catch (XMLParserException ex) {
            throw new KeyResolverException(ex);
        } catch (IOException ex) {
            throw new KeyResolverException(ex);
        }
    }

}
