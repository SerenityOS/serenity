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
package com.sun.org.apache.xml.internal.security.c14n;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer11_OmitComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer11_WithComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315ExclOmitComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315ExclWithComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315OmitComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315WithComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.CanonicalizerPhysical;
import com.sun.org.apache.xml.internal.security.exceptions.AlgorithmAlreadyRegisteredException;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.utils.JavaUtils;
import org.w3c.dom.Node;

/**
 *
 */
public final class Canonicalizer {

    /** The output encoding of canonicalized data */
    public static final String ENCODING = StandardCharsets.UTF_8.name();

    /**
     * XPath Expression for selecting every node and continuous comments joined
     * in only one node
     */
    public static final String XPATH_C14N_WITH_COMMENTS_SINGLE_NODE =
        "(.//. | .//@* | .//namespace::*)";

    /**
     * The URL defined in XML-SEC Rec for inclusive c14n <b>without</b> comments.
     */
    public static final String ALGO_ID_C14N_OMIT_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    /**
     * The URL defined in XML-SEC Rec for inclusive c14n <b>with</b> comments.
     */
    public static final String ALGO_ID_C14N_WITH_COMMENTS =
        ALGO_ID_C14N_OMIT_COMMENTS + "#WithComments";
    /**
     * The URL defined in XML-SEC Rec for exclusive c14n <b>without</b> comments.
     */
    public static final String ALGO_ID_C14N_EXCL_OMIT_COMMENTS =
        "http://www.w3.org/2001/10/xml-exc-c14n#";
    /**
     * The URL defined in XML-SEC Rec for exclusive c14n <b>with</b> comments.
     */
    public static final String ALGO_ID_C14N_EXCL_WITH_COMMENTS =
        ALGO_ID_C14N_EXCL_OMIT_COMMENTS + "WithComments";
    /**
     * The URI for inclusive c14n 1.1 <b>without</b> comments.
     */
    public static final String ALGO_ID_C14N11_OMIT_COMMENTS =
        "http://www.w3.org/2006/12/xml-c14n11";
    /**
     * The URI for inclusive c14n 1.1 <b>with</b> comments.
     */
    public static final String ALGO_ID_C14N11_WITH_COMMENTS =
        ALGO_ID_C14N11_OMIT_COMMENTS + "#WithComments";
    /**
     * Non-standard algorithm to serialize the physical representation for XML Encryption
     */
    public static final String ALGO_ID_C14N_PHYSICAL =
        "http://santuario.apache.org/c14n/physical";

    private static Map<String, Class<? extends CanonicalizerSpi>> canonicalizerHash =
        new ConcurrentHashMap<>();

    private final CanonicalizerSpi canonicalizerSpi;

    /**
     * Constructor Canonicalizer
     *
     * @param algorithmURI
     * @throws InvalidCanonicalizerException
     */
    private Canonicalizer(String algorithmURI) throws InvalidCanonicalizerException {
        try {
            Class<? extends CanonicalizerSpi> implementingClass =
                canonicalizerHash.get(algorithmURI);
            canonicalizerSpi = JavaUtils.newInstanceWithEmptyConstructor(implementingClass);
        } catch (Exception e) {
            Object[] exArgs = { algorithmURI };
            throw new InvalidCanonicalizerException(
                e, "signature.Canonicalizer.UnknownCanonicalizer", exArgs
            );
        }
    }

    /**
     * Method getInstance
     *
     * @param algorithmURI
     * @return a Canonicalizer instance ready for the job
     * @throws InvalidCanonicalizerException
     */
    public static final Canonicalizer getInstance(String algorithmURI)
        throws InvalidCanonicalizerException {
        return new Canonicalizer(algorithmURI);
    }

    /**
     * Method register
     *
     * @param algorithmURI
     * @param implementingClass
     * @throws AlgorithmAlreadyRegisteredException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the canonicalizer
     */
    @SuppressWarnings("unchecked")
    public static void register(String algorithmURI, String implementingClass)
        throws AlgorithmAlreadyRegisteredException, ClassNotFoundException {
        JavaUtils.checkRegisterPermission();
        // check whether URI is already registered
        Class<? extends CanonicalizerSpi> registeredClass =
            canonicalizerHash.get(algorithmURI);

        if (registeredClass != null)  {
            Object[] exArgs = { algorithmURI, registeredClass };
            throw new AlgorithmAlreadyRegisteredException("algorithm.alreadyRegistered", exArgs);
        }

        canonicalizerHash.put(
            algorithmURI, (Class<? extends CanonicalizerSpi>)
            ClassLoaderUtils.loadClass(implementingClass, Canonicalizer.class)
        );
    }

    /**
     * Method register
     *
     * @param algorithmURI
     * @param implementingClass
     * @throws AlgorithmAlreadyRegisteredException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the canonicalizer
     */
    public static void register(String algorithmURI, Class<? extends CanonicalizerSpi> implementingClass)
        throws AlgorithmAlreadyRegisteredException, ClassNotFoundException {
        JavaUtils.checkRegisterPermission();
        // check whether URI is already registered
        Class<? extends CanonicalizerSpi> registeredClass = canonicalizerHash.get(algorithmURI);

        if (registeredClass != null)  {
            Object[] exArgs = { algorithmURI, registeredClass };
            throw new AlgorithmAlreadyRegisteredException("algorithm.alreadyRegistered", exArgs);
        }

        canonicalizerHash.put(algorithmURI, implementingClass);
    }

    /**
     * This method registers the default algorithms.
     */
    public static void registerDefaultAlgorithms() {
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS,
            Canonicalizer20010315OmitComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS,
            Canonicalizer20010315WithComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS,
            Canonicalizer20010315ExclOmitComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS,
            Canonicalizer20010315ExclWithComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS,
            Canonicalizer11_OmitComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N11_WITH_COMMENTS,
            Canonicalizer11_WithComments.class
        );
        canonicalizerHash.put(
            Canonicalizer.ALGO_ID_C14N_PHYSICAL,
            CanonicalizerPhysical.class
        );
    }

    /**
     * This method tries to canonicalize the given bytes. It's possible to even
     * canonicalize non-wellformed sequences if they are well-formed after being
     * wrapped with a {@code &gt;a&lt;...&gt;/a&lt;}.
     *
     * @param inputBytes
     * @param writer OutputStream to write the canonicalization result
     * @param secureValidation Whether secure validation is enabled
     * @throws CanonicalizationException
     * @throws java.io.IOException
     * @throws XMLParserException
     */
    public void canonicalize(byte[] inputBytes, OutputStream writer, boolean secureValidation)
        throws XMLParserException, java.io.IOException, CanonicalizationException {
        canonicalizerSpi.engineCanonicalize(inputBytes, writer, secureValidation);
    }

    /**
     * Canonicalizes the subtree rooted by {@code node}.
     *
     * @param node The node to canonicalize
     * @param writer OutputStream to write the canonicalization result
     *
     * @throws CanonicalizationException
     */
    public void canonicalizeSubtree(Node node, OutputStream writer) throws CanonicalizationException {
        canonicalizerSpi.engineCanonicalizeSubTree(node, writer);
    }

    /**
     * Canonicalizes the subtree rooted by {@code node}.
     *
     * @param node
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void canonicalizeSubtree(Node node, String inclusiveNamespaces, OutputStream writer)
        throws CanonicalizationException {
        canonicalizerSpi.engineCanonicalizeSubTree(node, inclusiveNamespaces, writer);
    }

    /**
     * Canonicalizes the subtree rooted by {@code node}.
     *
     * @param node
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void canonicalizeSubtree(Node node, String inclusiveNamespaces,
                                    boolean propagateDefaultNamespace, OutputStream writer)
            throws CanonicalizationException {
        canonicalizerSpi.engineCanonicalizeSubTree(node, inclusiveNamespaces, propagateDefaultNamespace, writer);
    }

    /**
     * Canonicalizes an XPath node set.
     *
     * @param xpathNodeSet
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void canonicalizeXPathNodeSet(Set<Node> xpathNodeSet, OutputStream writer)
        throws CanonicalizationException {
        canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet, writer);
    }

    /**
     * Canonicalizes an XPath node set.
     *
     * @param xpathNodeSet
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void canonicalizeXPathNodeSet(
        Set<Node> xpathNodeSet, String inclusiveNamespaces, OutputStream writer
    ) throws CanonicalizationException {
        canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet, inclusiveNamespaces, writer);
    }

}
