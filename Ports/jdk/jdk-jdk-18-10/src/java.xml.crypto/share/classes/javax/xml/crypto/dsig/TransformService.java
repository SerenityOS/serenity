/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: TransformService.java,v 1.6.4.1 2005/09/15 12:42:11 mullan Exp $
 */
package javax.xml.crypto.dsig;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.*;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;


/**
 * A Service Provider Interface for transform and canonicalization algorithms.
 *
 * <p>Each instance of <code>TransformService</code> supports a specific
 * transform or canonicalization algorithm and XML mechanism type. To create a
 * <code>TransformService</code>, call one of the static
 * {@link #getInstance getInstance} methods, passing in the algorithm URI and
 * XML mechanism type desired, for example:
 *
 * <blockquote><code>
 * TransformService ts = TransformService.getInstance(Transform.XPATH2, "DOM");
 * </code></blockquote>
 *
 * <p><code>TransformService</code> implementations are registered and loaded
 * using the {@link java.security.Provider} mechanism.  Each
 * <code>TransformService</code> service provider implementation should include
 * a <code>MechanismType</code> service attribute that identifies the XML
 * mechanism type that it supports. If the attribute is not specified,
 * "DOM" is assumed. For example, a service provider that supports the
 * XPath Filter 2 Transform and DOM mechanism would be specified in the
 * <code>Provider</code> subclass as:
 * <pre>
 *     put("TransformService." + Transform.XPATH2,
 *         "org.example.XPath2TransformService");
 *     put("TransformService." + Transform.XPATH2 + " MechanismType", "DOM");
 * </pre>
 * <code>TransformService</code> implementations that support the DOM
 * mechanism type must abide by the DOM interoperability requirements defined
 * in the <a href="package-summary.html#dom_req">DOM Mechanism
 * Requirements</a>. See the {@code TransformService} section in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#xml-signature-xmlsignaturefactorykeyinfofactorytransformservice-mechanisms">
 * Java Security Standard Algorithm Names Specification</a> for a list of
 * standard algorithm URIs and mechanism types.
 * <p>
 * Once a <code>TransformService</code> has been created, it can be used
 * to process <code>Transform</code> or <code>CanonicalizationMethod</code>
 * objects. If the <code>Transform</code> or <code>CanonicalizationMethod</code>
 * exists in XML form (for example, when validating an existing
 * <code>XMLSignature</code>), the {@link #init(XMLStructure, XMLCryptoContext)}
 * method must be first called to initialize the transform and provide document
 * context (even if there are no parameters). Alternatively, if the
 * <code>Transform</code> or <code>CanonicalizationMethod</code> is being
 * created from scratch, the {@link #init(TransformParameterSpec)} method
 * is called to initialize the transform with parameters and the
 * {@link #marshalParams marshalParams} method is called to marshal the
 * parameters to XML and provide the transform with document context. Finally,
 * the {@link #transform transform} method is called to perform the
 * transformation.
 * <p>
 * <b>Concurrent Access</b>
 * <p>The static methods of this class are guaranteed to be thread-safe.
 * Multiple threads may concurrently invoke the static methods defined in this
 * class with no ill effects.
 *
 * <p>However, this is not true for the non-static methods defined by this
 * class. Unless otherwise documented by a specific provider, threads that
 * need to access a single <code>TransformService</code> instance
 * concurrently should synchronize amongst themselves and provide the
 * necessary locking. Multiple threads each manipulating a different
 * <code>TransformService</code> instance need not synchronize.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 */
public abstract class TransformService implements Transform {

    private String algorithm;
    private String mechanism;
    private Provider provider;

    /**
     * Default constructor, for invocation by subclasses.
     */
    protected TransformService() {}

    /**
     * Returns a <code>TransformService</code> that supports the specified
     * algorithm URI (ex: {@link Transform#XPATH2}) and mechanism type
     * (ex: DOM).
     *
     * <p>This method uses the standard JCA provider lookup mechanism to
     * locate and instantiate a <code>TransformService</code> implementation
     * of the desired algorithm and <code>MechanismType</code> service
     * attribute. It traverses the list of registered security
     * <code>Provider</code>s, starting with the most preferred
     * <code>Provider</code>. A new <code>TransformService</code> object
     * from the first <code>Provider</code> that supports the specified
     * algorithm and mechanism type is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param algorithm the URI of the algorithm. See the
     *    {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-transform-transformservice-algorithms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard transform algorithms.
     * @param mechanismType the type of the XML processing mechanism and
     *    representation. See the {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-xmlsignaturefactorykeyinfofactorytransformservice-mechanisms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard mechanism types.
     * @return a new <code>TransformService</code>
     * @throws NullPointerException if <code>algorithm</code> or
     *   <code>mechanismType</code> is  <code>null</code>
     * @throws NoSuchAlgorithmException if no <code>Provider</code> supports a
     *   <code>TransformService</code> implementation for the specified
     *   algorithm and mechanism type
     * @see Provider
     */
    public static TransformService getInstance
        (String algorithm, String mechanismType)
        throws NoSuchAlgorithmException {
        if (mechanismType == null || algorithm == null) {
            throw new NullPointerException();
        }
        boolean dom = false;
        if (mechanismType.equals("DOM")) {
            dom = true;
        }

        Provider[] provs = Security.getProviders();
        for (Provider p : provs) {
            Service s = p.getService("TransformService", algorithm);
            if (s != null) {
                String value = s.getAttribute("MechanismType");
                if ((value == null && dom) ||
                    (value != null && value.equals(mechanismType))) {
                    Object obj = s.newInstance(null);
                    if (obj instanceof TransformService) {
                        TransformService ts = (TransformService) obj;
                        ts.algorithm = algorithm;
                        ts.mechanism = mechanismType;
                        ts.provider = p;
                        return ts;
                    }
                }
            }
        }
        throw new NoSuchAlgorithmException
            (algorithm + " algorithm and " + mechanismType
                 + " mechanism not available");
    }

    /**
     * Returns a <code>TransformService</code> that supports the specified
     * algorithm URI (ex: {@link Transform#XPATH2}) and mechanism type
     * (ex: DOM) as supplied by the specified provider. Note that the specified
     * <code>Provider</code> object does not have to be registered in the
     * provider list.
     *
     * @param algorithm the URI of the algorithm. See the
     *    {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-transform-transformservice-algorithms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard transform algorithms.
     * @param mechanismType the type of the XML processing mechanism and
     *    representation. See the {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-xmlsignaturefactorykeyinfofactorytransformservice-mechanisms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard mechanism types.
     * @param provider the <code>Provider</code> object
     * @return a new <code>TransformService</code>
     * @throws NullPointerException if <code>provider</code>,
     *   <code>algorithm</code>, or <code>mechanismType</code> is
     *   <code>null</code>
     * @throws NoSuchAlgorithmException if a <code>TransformService</code>
     *   implementation for the specified algorithm and mechanism type is not
     *   available from the specified <code>Provider</code> object
     * @see Provider
     */
    public static TransformService getInstance
        (String algorithm, String mechanismType, Provider provider)
        throws NoSuchAlgorithmException {
        if (mechanismType == null || algorithm == null || provider == null) {
            throw new NullPointerException();
        }

        boolean dom = false;
        if (mechanismType.equals("DOM")) {
            dom = true;
        }
        Service s = provider.getService("TransformService", algorithm);
        if (s != null) {
            String value = s.getAttribute("MechanismType");
            if ((value == null && dom) ||
                (value != null && value.equals(mechanismType))) {
                Object obj = s.newInstance(null);
                if (obj instanceof TransformService) {
                    TransformService ts = (TransformService) obj;
                    ts.algorithm = algorithm;
                    ts.mechanism = mechanismType;
                    ts.provider = provider;
                    return ts;
                }
            }
        }
        throw new NoSuchAlgorithmException
            (algorithm + " algorithm and " + mechanismType
                 + " mechanism not available from " + provider.getName());
    }

    /**
     * Returns a <code>TransformService</code> that supports the specified
     * algorithm URI (ex: {@link Transform#XPATH2}) and mechanism type
     * (ex: DOM) as supplied by the specified provider. The specified provider
     * must be registered in the security provider list.
     *
     * <p>Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the URI of the algorithm. See the
     *    {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-transform-transformservice-algorithms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard transform algorithms.
     * @param mechanismType the type of the XML processing mechanism and
     *    representation. See the {@code TransformService} section in the
     *    <a href=
     *    "{@docRoot}/../specs/security/standard-names.html#xml-signature-xmlsignaturefactorykeyinfofactorytransformservice-mechanisms">
     *    Java Security Standard Algorithm Names Specification</a> for a list of
     *    standard mechanism types.
     * @param provider the string name of the provider
     * @return a new <code>TransformService</code>
     * @throws NoSuchProviderException if the specified provider is not
     *   registered in the security provider list
     * @throws NullPointerException if <code>provider</code>,
     *   <code>mechanismType</code>, or <code>algorithm</code> is
     *   <code>null</code>
     * @throws NoSuchAlgorithmException if a <code>TransformService</code>
     *   implementation for the specified algorithm and mechanism type is not
     *   available from the specified provider
     * @see Provider
     */
    public static TransformService getInstance
        (String algorithm, String mechanismType, String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException {
        if (mechanismType == null || algorithm == null || provider == null) {
            throw new NullPointerException();
        } else if (provider.length() == 0) {
            throw new NoSuchProviderException();
        }
        boolean dom = false;
        if (mechanismType.equals("DOM")) {
            dom = true;
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("No such provider: " +
                                              provider);
        }
        Service s = p.getService("TransformService", algorithm);
        if (s != null) {
            String value = s.getAttribute("MechanismType");
            if ((value == null && dom) ||
                (value != null && value.equals(mechanismType))) {
                Object obj = s.newInstance(null);
                if (obj instanceof TransformService) {
                    TransformService ts = (TransformService) obj;
                    ts.algorithm = algorithm;
                    ts.mechanism = mechanismType;
                    ts.provider = p;
                    return ts;
                }
            }
        }
        throw new NoSuchAlgorithmException
            (algorithm + " algorithm and " + mechanismType
                 + " mechanism not available from " + provider);
    }

    private static class MechanismMapEntry implements Map.Entry<String,String> {
        private final String mechanism;
        private final String algorithm;
        private final String key;
        MechanismMapEntry(String algorithm, String mechanism) {
            this.algorithm = algorithm;
            this.mechanism = mechanism;
            this.key = "TransformService." + algorithm + " MechanismType";
        }
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            return (getKey()==null ?
                    e.getKey()==null : getKey().equals(e.getKey())) &&
                   (getValue()==null ?
                    e.getValue()==null : getValue().equals(e.getValue()));
        }
        public String getKey() {
            return key;
        }
        public String getValue() {
            return mechanism;
        }
        public String setValue(String value) {
            throw new UnsupportedOperationException();
        }
        public int hashCode() {
            return (getKey()==null ? 0 : getKey().hashCode()) ^
                   (getValue()==null ? 0 : getValue().hashCode());
        }
    }

    /**
     * Returns the mechanism type supported by this <code>TransformService</code>.
     *
     * @return the mechanism type
     */
    public final String getMechanismType() {
        return mechanism;
    }

    /**
     * Returns the URI of the algorithm supported by this
     * <code>TransformService</code>.
     *
     * @return the algorithm URI
     */
    public final String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the provider of this <code>TransformService</code>.
     *
     * @return the provider
     */
    public final Provider getProvider() {
        return provider;
    }

    /**
     * Initializes this <code>TransformService</code> with the specified
     * parameters.
     *
     * <p>If the parameters exist in XML form, the
     * {@link #init(XMLStructure, XMLCryptoContext)} method should be used to
     * initialize the <code>TransformService</code>.
     *
     * @param params the algorithm parameters (may be <code>null</code> if
     *   not required or optional)
     * @throws InvalidAlgorithmParameterException if the specified parameters
     *   are invalid for this algorithm
     */
    public abstract void init(TransformParameterSpec params)
        throws InvalidAlgorithmParameterException;

    /**
     * Marshals the algorithm-specific parameters. If there are no parameters
     * to be marshalled, this method returns without throwing an exception.
     *
     * @param parent a mechanism-specific structure containing the parent
     *    node that the marshalled parameters should be appended to
     * @param context the <code>XMLCryptoContext</code> containing
     *    additional context (may be <code>null</code> if not applicable)
     * @throws ClassCastException if the type of <code>parent</code> or
     *    <code>context</code> is not compatible with this
     *    <code>TransformService</code>
     * @throws NullPointerException if <code>parent</code> is <code>null</code>
     * @throws MarshalException if the parameters cannot be marshalled
     */
    public abstract void marshalParams
        (XMLStructure parent, XMLCryptoContext context)
        throws MarshalException;

    /**
     * Initializes this <code>TransformService</code> with the specified
     * parameters and document context.
     *
     * @param parent a mechanism-specific structure containing the parent
     *    structure
     * @param context the <code>XMLCryptoContext</code> containing
     *    additional context (may be <code>null</code> if not applicable)
     * @throws ClassCastException if the type of <code>parent</code> or
     *    <code>context</code> is not compatible with this
     *    <code>TransformService</code>
     * @throws NullPointerException if <code>parent</code> is <code>null</code>
     * @throws InvalidAlgorithmParameterException if the specified parameters
     *   are invalid for this algorithm
     */
    public abstract void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException;
}
