/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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


package javax.security.cert;

import java.io.InputStream;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Security;

import java.math.BigInteger;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PublicKey;
import java.util.BitSet;
import java.util.Date;

/**
 * Abstract class for X.509 v1 certificates. This provides a standard
 * way to access all the version 1 attributes of an X.509 certificate.
 * Attributes that are specific to X.509 v2 or v3 are not available
 * through this interface. Future API evolution will provide full access to
 * complete X.509 v3 attributes.
 * <p>
 * The basic X.509 format was defined by
 * ISO/IEC and ANSI X9 and is described below in ASN.1:
 * <pre>
 * Certificate  ::=  SEQUENCE  {
 *     tbsCertificate       TBSCertificate,
 *     signatureAlgorithm   AlgorithmIdentifier,
 *     signature            BIT STRING  }
 * </pre>
 * <p>
 * These certificates are widely used to support authentication and
 * other functionality in Internet security systems. Common applications
 * include Privacy Enhanced Mail (PEM), Transport Layer Security (SSL),
 * code signing for trusted software distribution, and Secure Electronic
 * Transactions (SET).
 * <p>
 * These certificates are managed and vouched for by <em>Certificate
 * Authorities</em> (CAs). CAs are services which create certificates by
 * placing data in the X.509 standard format and then digitally signing
 * that data. CAs act as trusted third parties, making introductions
 * between principals who have no direct knowledge of each other.
 * CA certificates are either signed by themselves, or by some other
 * CA such as a "root" CA.
 * <p>
 * The ASN.1 definition of {@code tbsCertificate} is:
 * <pre>
 * TBSCertificate  ::=  SEQUENCE  {
 *     version         [0]  EXPLICIT Version DEFAULT v1,
 *     serialNumber         CertificateSerialNumber,
 *     signature            AlgorithmIdentifier,
 *     issuer               Name,
 *     validity             Validity,
 *     subject              Name,
 *     subjectPublicKeyInfo SubjectPublicKeyInfo,
 *     }
 * </pre>
 * <p>
 * Here is sample code to instantiate an X.509 certificate:
 * <pre>
 * InputStream inStream = new FileInputStream("fileName-of-cert");
 * X509Certificate cert = X509Certificate.getInstance(inStream);
 * inStream.close();
 * </pre>
 * OR
 * <pre>
 * byte[] certData = &lt;certificate read from a file, say&gt;
 * X509Certificate cert = X509Certificate.getInstance(certData);
 * </pre>
 * <p>
 * In either case, the code that instantiates an X.509 certificate
 * consults the value of the {@code cert.provider.x509v1} security property
 * to locate the actual implementation or instantiates a default implementation.
 * <p>
 * The {@code cert.provider.x509v1} property is set to a default
 * implementation for X.509 such as:
 * <pre>
 * cert.provider.x509v1=com.sun.security.cert.internal.x509.X509V1CertImpl
 * </pre>
 * <p>
 * The value of this {@code cert.provider.x509v1} property has to be
 * changed to instantiate another implementation. If this security
 * property is not set, a default implementation will be used.
 * Currently, due to possible security restrictions on access to
 * Security properties, this value is looked up and cached at class
 * initialization time and will fallback on a default implementation if
 * the Security property is not accessible.
 *
 * <p><em>Note: The classes in the package {@code javax.security.cert}
 * exist for compatibility with earlier versions of the
 * Java Secure Sockets Extension (JSSE). New applications should instead
 * use the standard Java SE certificate classes located in
 * {@code java.security.cert}.</em></p>
 *
 * @author Hemma Prafullchandra
 * @since 1.4
 * @see Certificate
 * @see java.security.cert.X509Extension
 * @see java.security.Security security properties
 * @deprecated Use the classes in {@code java.security.cert} instead.
 */
@SuppressWarnings("removal")
@Deprecated(since="9", forRemoval=true)
public abstract class X509Certificate extends Certificate {

    /**
     * Constructor for subclasses to call.
     */
    public X509Certificate() {}

    /**
     * Constant to lookup in the Security properties file.
     * In the Security properties file the default implementation
     * for X.509 v3 is given as:
     * <pre>
     * cert.provider.x509v1=com.sun.security.cert.internal.x509.X509V1CertImpl
     * </pre>
     */
    private static final String X509_PROVIDER = "cert.provider.x509v1";
    private static String X509Provider;

    static {
        X509Provider = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public String run() {
                    return Security.getProperty(X509_PROVIDER);
                }
            }
        );
    }

    /**
     * Instantiates an X509Certificate object, and initializes it with
     * the data read from the input stream {@code inStream}.
     * The implementation (X509Certificate is an abstract class) is
     * provided by the class specified as the value of the
     * {@code cert.provider.x509v1} security property.
     *
     * <p>Note: Only one DER-encoded
     * certificate is expected to be in the input stream.
     * Also, all X509Certificate
     * subclasses must provide a constructor of the form:
     * <pre>{@code
     * public <subClass>(InputStream inStream) ...
     * }</pre>
     *
     * @param inStream an input stream with the data to be read to
     *        initialize the certificate.
     * @return an X509Certificate object initialized with the data
     *         from the input stream.
     * @exception CertificateException if a class initialization
     *            or certificate parsing error occurs.
     */
    public static final X509Certificate getInstance(InputStream inStream)
    throws CertificateException {
        return getInst((Object)inStream);
    }

    /**
     * Instantiates an X509Certificate object, and initializes it with
     * the specified byte array.
     * The implementation (X509Certificate is an abstract class) is
     * provided by the class specified as the value of the
     * {@code cert.provider.x509v1} security property.
     *
     * <p>Note: All X509Certificate
     * subclasses must provide a constructor of the form:
     * <pre>{@code
     * public <subClass>(InputStream inStream) ...
     * }</pre>
     *
     * @param certData a byte array containing the DER-encoded
     *        certificate.
     * @return an X509Certificate object initialized with the data
     *         from {@code certData}.
     * @exception CertificateException if a class initialization
     *            or certificate parsing error occurs.
     */
    public static final X509Certificate getInstance(byte[] certData)
    throws CertificateException {
        return getInst((Object)certData);
    }

    private static final X509Certificate getInst(Object value)
    throws CertificateException {
        /*
         * This turns out not to work for now. To run under JDK1.2 we would
         * need to call beginPrivileged() but we can't do that and run
         * under JDK1.1.
         */
        String className = X509Provider;
        if (className == null || className.isEmpty()) {
            // shouldn't happen, but assume corrupted properties file
            // provide access to sun implementation
            className = "com.sun.security.cert.internal.x509.X509V1CertImpl";
        }
        try {
            Class<?>[] params = null;
            if (value instanceof InputStream) {
                params = new Class<?>[] { InputStream.class };
            } else if (value instanceof byte[]) {
                params = new Class<?>[] { value.getClass() };
            } else
                throw new CertificateException("Unsupported argument type");
            Class<?> certClass = Class.forName(className);

            // get the appropriate constructor and instantiate it
            Constructor<?> cons = certClass.getConstructor(params);

            // get a new instance
            Object obj = cons.newInstance(new Object[] {value});
            return (X509Certificate)obj;

        } catch (ClassNotFoundException e) {
          throw new CertificateException("Could not find class: " + e);
        } catch (IllegalAccessException e) {
          throw new CertificateException("Could not access class: " + e);
        } catch (InstantiationException e) {
          throw new CertificateException("Problems instantiating: " + e);
        } catch (InvocationTargetException e) {
          throw new CertificateException("InvocationTargetException: "
                                         + e.getTargetException());
        } catch (NoSuchMethodException e) {
          throw new CertificateException("Could not find class method: "
                                          + e.getMessage());
        }
    }

    /**
     * Checks that the certificate is currently valid. It is if
     * the current date and time are within the validity period given in the
     * certificate.
     * <p>
     * The validity period consists of two date/time values:
     * the first and last dates (and times) on which the certificate
     * is valid. It is defined in
     * ASN.1 as:
     * <pre>
     * validity             Validity
     *
     * Validity ::= SEQUENCE {
     *     notBefore      CertificateValidityDate,
     *     notAfter       CertificateValidityDate }
     *
     * CertificateValidityDate ::= CHOICE {
     *     utcTime        UTCTime,
     *     generalTime    GeneralizedTime }
     * </pre>
     *
     * @exception CertificateExpiredException if the certificate has expired.
     * @exception CertificateNotYetValidException if the certificate is not
     *            yet valid.
     */
    public abstract void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;

    /**
     * Checks that the specified date is within the certificate's
     * validity period. In other words, this determines whether the
     * certificate would be valid at the specified date/time.
     *
     * @param date the Date to check against to see if this certificate
     *        is valid at that date/time.
     * @exception CertificateExpiredException if the certificate has expired
     *            with respect to the {@code date} supplied.
     * @exception CertificateNotYetValidException if the certificate is not
     *            yet valid with respect to the {@code date} supplied.
     * @see #checkValidity()
     */
    public abstract void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;

    /**
     * Gets the {@code version} (version number) value from the
     * certificate. The ASN.1 definition for this is:
     * <pre>
     * version         [0]  EXPLICIT Version DEFAULT v1
     *
     * Version  ::=  INTEGER  {  v1(0), v2(1), v3(2)  }
     * </pre>
     *
     * @return the version number from the ASN.1 encoding, i.e. 0, 1 or 2.
     */
    public abstract int getVersion();

    /**
     * Gets the {@code serialNumber} value from the certificate.
     * The serial number is an integer assigned by the certification
     * authority to each certificate. It must be unique for each
     * certificate issued by a given CA (i.e., the issuer name and
     * serial number identify a unique certificate).
     * The ASN.1 definition for this is:
     * <pre>
     * serialNumber     CertificateSerialNumber
     *
     * CertificateSerialNumber  ::=  INTEGER
     * </pre>
     *
     * @return the serial number.
     */
    public abstract BigInteger getSerialNumber();

    /**
     * Gets the {@code issuer} (issuer distinguished name) value from
     * the certificate. The issuer name identifies the entity that signed (and
     * issued) the certificate.
     *
     * <p>The issuer name field contains an
     * X.500 distinguished name (DN).
     * The ASN.1 definition for this is:
     * <pre>
     * issuer    Name
     *
     * Name ::= CHOICE { RDNSequence }
     * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
     * RelativeDistinguishedName ::=
     *     SET OF AttributeValueAssertion
     *
     * AttributeValueAssertion ::= SEQUENCE {
     *                               AttributeType,
     *                               AttributeValue }
     * AttributeType ::= OBJECT IDENTIFIER
     * AttributeValue ::= ANY
     * </pre>
     * The {@code Name} describes a hierarchical name composed of
     * attributes, such as country name, and corresponding values, such as US.
     * The type of the {@code AttributeValue} component is determined by
     * the {@code AttributeType}; in general it will be a
     * {@code directoryString}. A {@code directoryString} is usually
     * one of {@code PrintableString},
     * {@code TeletexString} or {@code UniversalString}.
     *
     * @return a Principal whose name is the issuer distinguished name.
     */
    public abstract Principal getIssuerDN();

    /**
     * Gets the {@code subject} (subject distinguished name) value
     * from the certificate.
     * The ASN.1 definition for this is:
     * <pre>
     * subject    Name
     * </pre>
     *
     * <p>See {@link #getIssuerDN() getIssuerDN} for {@code Name}
     * and other relevant definitions.
     *
     * @return a Principal whose name is the subject name.
     * @see #getIssuerDN()
     */
    public abstract Principal getSubjectDN();

    /**
     * Gets the {@code notBefore} date from the validity period of
     * the certificate.
     * The relevant ASN.1 definitions are:
     * <pre>
     * validity             Validity
     *
     * Validity ::= SEQUENCE {
     *     notBefore      CertificateValidityDate,
     *     notAfter       CertificateValidityDate }
     *
     * CertificateValidityDate ::= CHOICE {
     *     utcTime        UTCTime,
     *     generalTime    GeneralizedTime }
     * </pre>
     *
     * @return the start date of the validity period.
     * @see #checkValidity()
     */
    public abstract Date getNotBefore();

    /**
     * Gets the {@code notAfter} date from the validity period of
     * the certificate. See {@link #getNotBefore() getNotBefore}
     * for relevant ASN.1 definitions.
     *
     * @return the end date of the validity period.
     * @see #checkValidity()
     */
    public abstract Date getNotAfter();

    /**
     * Gets the signature algorithm name for the certificate
     * signature algorithm. An example is the string "SHA-1/DSA".
     * The ASN.1 definition for this is:
     * <pre>
     * signatureAlgorithm   AlgorithmIdentifier
     *
     * AlgorithmIdentifier  ::=  SEQUENCE  {
     *     algorithm               OBJECT IDENTIFIER,
     *     parameters              ANY DEFINED BY algorithm OPTIONAL  }
     *                             -- contains a value of the type
     *                             -- registered for use with the
     *                             -- algorithm object identifier value
     * </pre>
     *
     * <p>The algorithm name is determined from the {@code algorithm}
     * OID string.
     *
     * @return the signature algorithm name.
     */
    public abstract String getSigAlgName();

    /**
     * Gets the signature algorithm OID string from the certificate.
     * An OID is represented by a set of positive whole numbers separated
     * by periods.
     * For example, the string "1.2.840.10040.4.3" identifies the SHA-1
     * with DSA signature algorithm, as per the PKIX part I.
     *
     * <p>See {@link #getSigAlgName() getSigAlgName} for
     * relevant ASN.1 definitions.
     *
     * @return the signature algorithm OID string.
     */
    public abstract String getSigAlgOID();

    /**
     * Gets the DER-encoded signature algorithm parameters from this
     * certificate's signature algorithm. In most cases, the signature
     * algorithm parameters are null; the parameters are usually
     * supplied with the certificate's public key.
     *
     * <p>See {@link #getSigAlgName() getSigAlgName} for
     * relevant ASN.1 definitions.
     *
     * @return the DER-encoded signature algorithm parameters, or
     *         null if no parameters are present.
     */
    public abstract byte[] getSigAlgParams();
}
