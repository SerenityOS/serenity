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
/*
 * ===========================================================================
 *
 * (C) Copyright IBM Corp. 2003 All Rights Reserved.
 *
 * ===========================================================================
 */
/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import java.util.*;
import java.security.*;

import javax.xml.crypto.dsig.*;

/**
 * The XMLDSig RI Provider.
 *
 */

/**
 * Defines the XMLDSigRI provider.
 */

public final class XMLDSigRI extends Provider {

    static final long serialVersionUID = -5049765099299494554L;

    private static final String INFO = "XMLDSig " +
        "(DOM XMLSignatureFactory; DOM KeyInfoFactory; " +
        "C14N 1.0, C14N 1.1, Exclusive C14N, Base64, Enveloped, XPath, " +
        "XPath2, XSLT TransformServices)";

    @SuppressWarnings("removal")
    private static final String VER =
        AccessController.doPrivileged(new PrivilegedAction<>() {
            public String run() {
                return System.getProperty("java.specification.version");
            }
        });

    private static final class ProviderService extends Provider.Service {

        ProviderService(Provider p, String type, String algo, String cn) {
            super(p, type, algo, cn, null, null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            String[] aliases) {
            super(p, type, algo, cn,
                aliases == null ? null : Arrays.asList(aliases), null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            String[] aliases, Map<String, String> attrs) {
            super(p, type, algo, cn,
                  aliases == null ? null : Arrays.asList(aliases), attrs);
        }

        @Override
        public Object newInstance(Object ctrParamObj)
            throws NoSuchAlgorithmException {
            String type = getType();
            if (ctrParamObj != null) {
                throw new InvalidParameterException
                    ("constructorParameter not used with " + type + " engines");
            }

            String algo = getAlgorithm();
            try {
                if ("XMLSignatureFactory".equals(type)) {
                    if ("DOM".equals(algo)) {
                        return new DOMXMLSignatureFactory();
                    }
                } else if ("KeyInfoFactory".equals(type)) {
                    if ("DOM".equals(algo)) {
                        return new DOMKeyInfoFactory();
                    }
                } else if ("TransformService".equals(type)) {
                    if (algo.equals(CanonicalizationMethod.INCLUSIVE) ||
                        algo.equals(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS)) {
                        return new DOMCanonicalXMLC14NMethod();
                    } else if ("http://www.w3.org/2006/12/xml-c14n11".equals(algo) ||
                        "http://www.w3.org/2006/12/xml-c14n11#WithComments".equals(algo)) {
                        return new DOMCanonicalXMLC14N11Method();
                    } else if (algo.equals(CanonicalizationMethod.EXCLUSIVE) ||
                        algo.equals(CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS)) {
                        return new DOMExcC14NMethod();
                    } else if (algo.equals(Transform.BASE64)) {
                        return new DOMBase64Transform();
                    } else if (algo.equals(Transform.ENVELOPED)) {
                        return new DOMEnvelopedTransform();
                    } else if (algo.equals(Transform.XPATH2)) {
                        return new DOMXPathFilter2Transform();
                    } else if (algo.equals(Transform.XPATH)) {
                        return new DOMXPathTransform();
                    } else if (algo.equals(Transform.XSLT)) {
                        return new DOMXSLTTransform();
                    }
                }
            } catch (Exception ex) {
                throw new NoSuchAlgorithmException("Error constructing " +
                    type + " for " + algo + " using XMLDSig", ex);
            }
            throw new ProviderException("No impl for " + algo +
                " " + type);
        }
    }

    @SuppressWarnings("removal")
    public XMLDSigRI() {
        // This is the JDK XMLDSig provider, synced from
        // Apache Santuario XML Security for Java, version 2.2.1
        super("XMLDSig", VER, INFO);

        final Provider p = this;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Map<String, String> MECH_TYPE = new HashMap<>();
                MECH_TYPE.put("MechanismType", "DOM");

                putService(new ProviderService(p, "XMLSignatureFactory",
                    "DOM", "org.jcp.xml.dsig.internal.dom.DOMXMLSignatureFactory"));

                putService(new ProviderService(p, "KeyInfoFactory",
                    "DOM", "org.jcp.xml.dsig.internal.dom.DOMKeyInfoFactory"));


                // Inclusive C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.INCLUSIVE,
                    "org.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14NMethod",
                    new String[] {"INCLUSIVE"}, MECH_TYPE));

                // InclusiveWithComments C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                    "org.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14NMethod",
                    new String[] {"INCLUSIVE_WITH_COMMENTS"}, MECH_TYPE));

                // Inclusive C14N 1.1
                putService(new ProviderService(p, "TransformService",
                    "http://www.w3.org/2006/12/xml-c14n11",
                    "org.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14N11Method",
                    null, MECH_TYPE));

                // InclusiveWithComments C14N 1.1
                putService(new ProviderService(p, "TransformService",
                    "http://www.w3.org/2006/12/xml-c14n11#WithComments",
                    "org.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14N11Method",
                    null, MECH_TYPE));

                // Exclusive C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.EXCLUSIVE,
                    "org.jcp.xml.dsig.internal.dom.DOMExcC14NMethod",
                    new String[] {"EXCLUSIVE"}, MECH_TYPE));

                // ExclusiveWithComments C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
                    "org.jcp.xml.dsig.internal.dom.DOMExcC14NMethod",
                    new String[] {"EXCLUSIVE_WITH_COMMENTS"}, MECH_TYPE));

                // Base64 Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.BASE64,
                    "org.jcp.xml.dsig.internal.dom.DOMBase64Transform",
                    new String[] {"BASE64"}, MECH_TYPE));

                // Enveloped Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.ENVELOPED,
                    "org.jcp.xml.dsig.internal.dom.DOMEnvelopedTransform",
                    new String[] {"ENVELOPED"}, MECH_TYPE));

                // XPath2 Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XPATH2,
                    "org.jcp.xml.dsig.internal.dom.DOMXPathFilter2Transform",
                    new String[] {"XPATH2"}, MECH_TYPE));

                // XPath Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XPATH,
                    "org.jcp.xml.dsig.internal.dom.DOMXPathTransform",
                    new String[] {"XPATH"}, MECH_TYPE));

                // XSLT Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XSLT,
                    "org.jcp.xml.dsig.internal.dom.DOMXSLTTransform",
                    new String[] {"XSLT"}, MECH_TYPE));
                return null;
            }
        });
    }
}
