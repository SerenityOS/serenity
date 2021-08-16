/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import javax.security.auth.x500.X500Principal;

import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.KnownOIDs;
import sun.security.x509.CRLNumberExtension;
import sun.security.x509.X500Name;

/**
 * A {@code CRLSelector} that selects {@code X509CRLs} that
 * match all specified criteria. This class is particularly useful when
 * selecting CRLs from a {@code CertStore} to check revocation status
 * of a particular certificate.
 * <p>
 * When first constructed, an {@code X509CRLSelector} has no criteria
 * enabled and each of the {@code get} methods return a default
 * value ({@code null}). Therefore, the {@link #match match} method
 * would return {@code true} for any {@code X509CRL}. Typically,
 * several criteria are enabled (by calling {@link #setIssuers setIssuers}
 * or {@link #setDateAndTime setDateAndTime}, for instance) and then the
 * {@code X509CRLSelector} is passed to
 * {@link CertStore#getCRLs CertStore.getCRLs} or some similar
 * method.
 * <p>
 * Please refer to <a href="http://tools.ietf.org/html/rfc5280">RFC 5280:
 * Internet X.509 Public Key Infrastructure Certificate and CRL Profile</a>
 * for definitions of the X.509 CRL fields and extensions mentioned below.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CRLSelector
 * @see X509CRL
 *
 * @since       1.4
 * @author      Steve Hanna
 */
public class X509CRLSelector implements CRLSelector {

    static {
        CertPathHelperImpl.initialize();
    }

    private static final Debug debug = Debug.getInstance("certpath");
    private HashSet<Object> issuerNames;
    private HashSet<X500Principal> issuerX500Principals;
    private BigInteger minCRL;
    private BigInteger maxCRL;
    private Date dateAndTime;
    private X509Certificate certChecking;
    private long skew = 0;

    /**
     * Creates an {@code X509CRLSelector}. Initially, no criteria are set
     * so any {@code X509CRL} will match.
     */
    public X509CRLSelector() {}

    /**
     * Sets the issuerNames criterion. The issuer distinguished name in the
     * {@code X509CRL} must match at least one of the specified
     * distinguished names. If {@code null}, any issuer distinguished name
     * will do.
     * <p>
     * This method allows the caller to specify, with a single method call,
     * the complete set of issuer names which {@code X509CRLs} may contain.
     * The specified value replaces the previous value for the issuerNames
     * criterion.
     * <p>
     * The {@code names} parameter (if not {@code null}) is a
     * {@code Collection} of {@code X500Principal}s.
     * <p>
     * Note that the {@code names} parameter can contain duplicate
     * distinguished names, but they may be removed from the
     * {@code Collection} of names returned by the
     * {@link #getIssuers getIssuers} method.
     * <p>
     * Note that a copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @param issuers a {@code Collection} of X500Principals
     *   (or {@code null})
     * @see #getIssuers
     * @since 1.5
     */
    public void setIssuers(Collection<X500Principal> issuers) {
        if ((issuers == null) || issuers.isEmpty()) {
            issuerNames = null;
            issuerX500Principals = null;
        } else {
            // clone
            issuerX500Principals = new HashSet<>(issuers);
            issuerNames = new HashSet<>();
            for (X500Principal p : issuerX500Principals) {
                issuerNames.add(p.getEncoded());
            }
        }
    }

    /**
     * <strong>Note:</strong> use {@linkplain #setIssuers(Collection)} instead
     * or only specify the byte array form of distinguished names when using
     * this method. See {@link #addIssuerName(String)} for more information.
     * <p>
     * Sets the issuerNames criterion. The issuer distinguished name in the
     * {@code X509CRL} must match at least one of the specified
     * distinguished names. If {@code null}, any issuer distinguished name
     * will do.
     * <p>
     * This method allows the caller to specify, with a single method call,
     * the complete set of issuer names which {@code X509CRLs} may contain.
     * The specified value replaces the previous value for the issuerNames
     * criterion.
     * <p>
     * The {@code names} parameter (if not {@code null}) is a
     * {@code Collection} of names. Each name is a {@code String}
     * or a byte array representing a distinguished name (in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> or
     * ASN.1 DER encoded form, respectively). If {@code null} is supplied
     * as the value for this argument, no issuerNames check will be performed.
     * <p>
     * Note that the {@code names} parameter can contain duplicate
     * distinguished names, but they may be removed from the
     * {@code Collection} of names returned by the
     * {@link #getIssuerNames getIssuerNames} method.
     * <p>
     * If a name is specified as a byte array, it should contain a single DER
     * encoded distinguished name, as defined in X.501. The ASN.1 notation for
     * this structure is as follows.
     * <pre>{@code
     * Name ::= CHOICE {
     *   RDNSequence }
     *
     * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
     *
     * RelativeDistinguishedName ::=
     *   SET SIZE (1 .. MAX) OF AttributeTypeAndValue
     *
     * AttributeTypeAndValue ::= SEQUENCE {
     *   type     AttributeType,
     *   value    AttributeValue }
     *
     * AttributeType ::= OBJECT IDENTIFIER
     *
     * AttributeValue ::= ANY DEFINED BY AttributeType
     * ....
     * DirectoryString ::= CHOICE {
     *       teletexString           TeletexString (SIZE (1..MAX)),
     *       printableString         PrintableString (SIZE (1..MAX)),
     *       universalString         UniversalString (SIZE (1..MAX)),
     *       utf8String              UTF8String (SIZE (1.. MAX)),
     *       bmpString               BMPString (SIZE (1..MAX)) }
     * }</pre>
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @param names a {@code Collection} of names (or {@code null})
     * @throws IOException if a parsing error occurs
     * @see #getIssuerNames
     */
    public void setIssuerNames(Collection<?> names) throws IOException {
        if (names == null || names.size() == 0) {
            issuerNames = null;
            issuerX500Principals = null;
        } else {
            HashSet<Object> tempNames = cloneAndCheckIssuerNames(names);
            // Ensure that we either set both of these or neither
            issuerX500Principals = parseIssuerNames(tempNames);
            issuerNames = tempNames;
        }
    }

    /**
     * Adds a name to the issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names.
     * <p>
     * This method allows the caller to add a name to the set of issuer names
     * which {@code X509CRLs} may contain. The specified name is added to
     * any previous value for the issuerNames criterion.
     * If the specified name is a duplicate, it may be ignored.
     *
     * @param issuer the issuer as X500Principal
     * @since 1.5
     */
    public void addIssuer(X500Principal issuer) {
        addIssuerNameInternal(issuer.getEncoded(), issuer);
    }

    /**
     * Adds a name to the issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names.
     * <p>
     * This method allows the caller to add a name to the set of issuer names
     * which {@code X509CRLs} may contain. The specified name is added to
     * any previous value for the issuerNames criterion.
     * If the specified name is a duplicate, it may be ignored.
     *
     * @param name the name in
     *     <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> form
     * @throws IOException if a parsing error occurs
     *
     * @deprecated Use {@link #addIssuer(X500Principal)} or
     * {@link #addIssuerName(byte[])} instead. This method should not be
     * relied on as it can fail to match some CRLs because of a loss of
     * encoding information in the RFC 2253 String form of some distinguished
     * names.
     */
    @Deprecated(since="16")
    public void addIssuerName(String name) throws IOException {
        addIssuerNameInternal(name, new X500Name(name).asX500Principal());
    }

    /**
     * Adds a name to the issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names.
     * <p>
     * This method allows the caller to add a name to the set of issuer names
     * which {@code X509CRLs} may contain. The specified name is added to
     * any previous value for the issuerNames criterion. If the specified name
     * is a duplicate, it may be ignored.
     * If a name is specified as a byte array, it should contain a single DER
     * encoded distinguished name, as defined in X.501. The ASN.1 notation for
     * this structure is as follows.
     * <p>
     * The name is provided as a byte array. This byte array should contain
     * a single DER encoded distinguished name, as defined in X.501. The ASN.1
     * notation for this structure appears in the documentation for
     * {@link #setIssuerNames setIssuerNames(Collection names)}.
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param name a byte array containing the name in ASN.1 DER encoded form
     * @throws IOException if a parsing error occurs
     */
    public void addIssuerName(byte[] name) throws IOException {
        // clone because byte arrays are modifiable
        addIssuerNameInternal(name.clone(), new X500Name(name).asX500Principal());
    }

    /**
     * A private method that adds a name (String or byte array) to the
     * issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names.
     *
     * @param name the name in string or byte array form
     * @param principal the name in X500Principal form
     * @throws IOException if a parsing error occurs
     */
    private void addIssuerNameInternal(Object name, X500Principal principal) {
        if (issuerNames == null) {
            issuerNames = new HashSet<>();
        }
        if (issuerX500Principals == null) {
            issuerX500Principals = new HashSet<>();
        }
        issuerNames.add(name);
        issuerX500Principals.add(principal);
    }

    /**
     * Clone and check an argument of the form passed to
     * setIssuerNames. Throw an IOException if the argument is malformed.
     *
     * @param names a {@code Collection} of names. Each entry is a
     *              String or a byte array (the name, in string or ASN.1
     *              DER encoded form, respectively). {@code null} is
     *              not an acceptable value.
     * @return a deep copy of the specified {@code Collection}
     * @throws IOException if a parsing error occurs
     */
    private static HashSet<Object> cloneAndCheckIssuerNames(Collection<?> names)
        throws IOException
    {
        HashSet<Object> namesCopy = new HashSet<>();
        Iterator<?> i = names.iterator();
        while (i.hasNext()) {
            Object nameObject = i.next();
            if (!(nameObject instanceof byte []) &&
                !(nameObject instanceof String))
                throw new IOException("name not byte array or String");
            if (nameObject instanceof byte [])
                namesCopy.add(((byte []) nameObject).clone());
            else
                namesCopy.add(nameObject);
        }
        return(namesCopy);
    }

    /**
     * Clone an argument of the form passed to setIssuerNames.
     * Throw a RuntimeException if the argument is malformed.
     * <p>
     * This method wraps cloneAndCheckIssuerNames, changing any IOException
     * into a RuntimeException. This method should be used when the object being
     * cloned has already been checked, so there should never be any exceptions.
     *
     * @param names a {@code Collection} of names. Each entry is a
     *              String or a byte array (the name, in string or ASN.1
     *              DER encoded form, respectively). {@code null} is
     *              not an acceptable value.
     * @return a deep copy of the specified {@code Collection}
     * @throws RuntimeException if a parsing error occurs
     */
    private static HashSet<Object> cloneIssuerNames(Collection<Object> names) {
        try {
            return cloneAndCheckIssuerNames(names);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Parse an argument of the form passed to setIssuerNames,
     * returning a Collection of issuerX500Principals.
     * Throw an IOException if the argument is malformed.
     *
     * @param names a {@code Collection} of names. Each entry is a
     *              String or a byte array (the name, in string or ASN.1
     *              DER encoded form, respectively). <Code>Null</Code> is
     *              not an acceptable value.
     * @return a HashSet of issuerX500Principals
     * @throws IOException if a parsing error occurs
     */
    private static HashSet<X500Principal> parseIssuerNames(Collection<Object> names)
    throws IOException {
        HashSet<X500Principal> x500Principals = new HashSet<>();
        for (Iterator<Object> t = names.iterator(); t.hasNext(); ) {
            Object nameObject = t.next();
            if (nameObject instanceof String) {
                x500Principals.add(new X500Name((String)nameObject).asX500Principal());
            } else {
                try {
                    x500Principals.add(new X500Principal((byte[])nameObject));
                } catch (IllegalArgumentException e) {
                    throw (IOException)new IOException("Invalid name").initCause(e);
                }
            }
        }
        return x500Principals;
    }

    /**
     * Sets the minCRLNumber criterion. The {@code X509CRL} must have a
     * CRL number extension whose value is greater than or equal to the
     * specified value. If {@code null}, no minCRLNumber check will be
     * done.
     *
     * @param minCRL the minimum CRL number accepted (or {@code null})
     */
    public void setMinCRLNumber(BigInteger minCRL) {
        this.minCRL = minCRL;
    }

    /**
     * Sets the maxCRLNumber criterion. The {@code X509CRL} must have a
     * CRL number extension whose value is less than or equal to the
     * specified value. If {@code null}, no maxCRLNumber check will be
     * done.
     *
     * @param maxCRL the maximum CRL number accepted (or {@code null})
     */
    public void setMaxCRLNumber(BigInteger maxCRL) {
        this.maxCRL = maxCRL;
    }

    /**
     * Sets the dateAndTime criterion. The specified date must be
     * equal to or later than the value of the thisUpdate component
     * of the {@code X509CRL} and earlier than the value of the
     * nextUpdate component. There is no match if the {@code X509CRL}
     * does not contain a nextUpdate component.
     * If {@code null}, no dateAndTime check will be done.
     * <p>
     * Note that the {@code Date} supplied here is cloned to protect
     * against subsequent modifications.
     *
     * @param dateAndTime the {@code Date} to match against
     *                    (or {@code null})
     * @see #getDateAndTime
     */
    public void setDateAndTime(Date dateAndTime) {
        if (dateAndTime == null)
            this.dateAndTime = null;
        else
            this.dateAndTime = new Date(dateAndTime.getTime());
        this.skew = 0;
    }

    /**
     * Sets the dateAndTime criterion and allows for the specified clock skew
     * (in milliseconds) when checking against the validity period of the CRL.
     */
    void setDateAndTime(Date dateAndTime, long skew) {
        this.dateAndTime =
            (dateAndTime == null ? null : new Date(dateAndTime.getTime()));
        this.skew = skew;
    }

    /**
     * Sets the certificate being checked. This is not a criterion. Rather,
     * it is optional information that may help a {@code CertStore}
     * find CRLs that would be relevant when checking revocation for the
     * specified certificate. If {@code null} is specified, then no
     * such optional information is provided.
     *
     * @param cert the {@code X509Certificate} being checked
     *             (or {@code null})
     * @see #getCertificateChecking
     */
    public void setCertificateChecking(X509Certificate cert) {
        certChecking = cert;
    }

    /**
     * Returns the issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names. If the value returned is {@code null}, any
     * issuer distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a
     * unmodifiable {@code Collection} of {@code X500Principal}s.
     *
     * @return an unmodifiable {@code Collection} of names
     *   (or {@code null})
     * @see #setIssuers
     * @since 1.5
     */
    public Collection<X500Principal> getIssuers() {
        if (issuerX500Principals == null) {
            return null;
        }
        return Collections.unmodifiableCollection(issuerX500Principals);
    }

    /**
     * Returns a copy of the issuerNames criterion. The issuer distinguished
     * name in the {@code X509CRL} must match at least one of the specified
     * distinguished names. If the value returned is {@code null}, any
     * issuer distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a
     * {@code Collection} of names. Each name is a {@code String}
     * or a byte array representing a distinguished name (in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> or
     * ASN.1 DER encoded form, respectively).  Note that the
     * {@code Collection} returned may contain duplicate names.
     * <p>
     * If a name is specified as a byte array, it should contain a single DER
     * encoded distinguished name, as defined in X.501. The ASN.1 notation for
     * this structure is given in the documentation for
     * {@link #setIssuerNames setIssuerNames(Collection names)}.
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @return a {@code Collection} of names (or {@code null})
     * @see #setIssuerNames
     */
    public Collection<Object> getIssuerNames() {
        if (issuerNames == null) {
            return null;
        }
        return cloneIssuerNames(issuerNames);
    }

    /**
     * Returns the minCRLNumber criterion. The {@code X509CRL} must have a
     * CRL number extension whose value is greater than or equal to the
     * specified value. If {@code null}, no minCRLNumber check will be done.
     *
     * @return the minimum CRL number accepted (or {@code null})
     */
    public BigInteger getMinCRL() {
        return minCRL;
    }

    /**
     * Returns the maxCRLNumber criterion. The {@code X509CRL} must have a
     * CRL number extension whose value is less than or equal to the
     * specified value. If {@code null}, no maxCRLNumber check will be
     * done.
     *
     * @return the maximum CRL number accepted (or {@code null})
     */
    public BigInteger getMaxCRL() {
        return maxCRL;
    }

    /**
     * Returns the dateAndTime criterion. The specified date must be
     * equal to or later than the value of the thisUpdate component
     * of the {@code X509CRL} and earlier than the value of the
     * nextUpdate component. There is no match if the
     * {@code X509CRL} does not contain a nextUpdate component.
     * If {@code null}, no dateAndTime check will be done.
     * <p>
     * Note that the {@code Date} returned is cloned to protect against
     * subsequent modifications.
     *
     * @return the {@code Date} to match against (or {@code null})
     * @see #setDateAndTime
     */
    public Date getDateAndTime() {
        if (dateAndTime == null)
            return null;
        return (Date) dateAndTime.clone();
    }

    /**
     * Returns the certificate being checked. This is not a criterion. Rather,
     * it is optional information that may help a {@code CertStore}
     * find CRLs that would be relevant when checking revocation for the
     * specified certificate. If the value returned is {@code null}, then
     * no such optional information is provided.
     *
     * @return the certificate being checked (or {@code null})
     * @see #setCertificateChecking
     */
    public X509Certificate getCertificateChecking() {
        return certChecking;
    }

    /**
     * Returns a printable representation of the {@code X509CRLSelector}.
     *
     * @return a {@code String} describing the contents of the
     *         {@code X509CRLSelector}.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CRLSelector: [\n");
        if (issuerNames != null) {
            sb.append("  IssuerNames:\n");
            Iterator<Object> i = issuerNames.iterator();
            while (i.hasNext())
                sb.append("    " + i.next() + "\n");
        }
        if (minCRL != null)
            sb.append("  minCRLNumber: " + minCRL + "\n");
        if (maxCRL != null)
            sb.append("  maxCRLNumber: " + maxCRL + "\n");
        if (dateAndTime != null)
            sb.append("  dateAndTime: " + dateAndTime + "\n");
        if (certChecking != null)
            sb.append("  Certificate being checked: " + certChecking + "\n");
        sb.append("]");
        return sb.toString();
    }

    /**
     * Decides whether a {@code CRL} should be selected.
     *
     * @param crl the {@code CRL} to be checked
     * @return {@code true} if the {@code CRL} should be selected,
     *         {@code false} otherwise
     */
    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL xcrl)) {
            return false;
        }

        /* match on issuer name */
        if (issuerNames != null) {
            X500Principal issuer = xcrl.getIssuerX500Principal();
            Iterator<X500Principal> i = issuerX500Principals.iterator();
            boolean found = false;
            while (!found && i.hasNext()) {
                if (i.next().equals(issuer)) {
                    found = true;
                }
            }
            if (!found) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: issuer DNs "
                        + "don't match");
                }
                return false;
            }
        }

        if ((minCRL != null) || (maxCRL != null)) {
            /* Get CRL number extension from CRL */
            byte[] crlNumExtVal = xcrl.getExtensionValue(KnownOIDs.CRLNumber.value());
            if (crlNumExtVal == null) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: no CRLNumber");
                }
            }
            BigInteger crlNum;
            try {
                DerInputStream in = new DerInputStream(crlNumExtVal);
                byte[] encoded = in.getOctetString();
                CRLNumberExtension crlNumExt =
                    new CRLNumberExtension(Boolean.FALSE, encoded);
                crlNum = crlNumExt.get(CRLNumberExtension.NUMBER);
            } catch (IOException ex) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: exception in "
                        + "decoding CRL number");
                }
                return false;
            }

            /* match on minCRLNumber */
            if (minCRL != null) {
                if (crlNum.compareTo(minCRL) < 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too small");
                    }
                    return false;
                }
            }

            /* match on maxCRLNumber */
            if (maxCRL != null) {
                if (crlNum.compareTo(maxCRL) > 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too large");
                    }
                    return false;
                }
            }
        }


        /* match on dateAndTime */
        if (dateAndTime != null) {
            Date crlThisUpdate = xcrl.getThisUpdate();
            Date nextUpdate = xcrl.getNextUpdate();
            if (nextUpdate == null) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: nextUpdate null");
                }
                return false;
            }
            Date nowPlusSkew = dateAndTime;
            Date nowMinusSkew = dateAndTime;
            if (skew > 0) {
                nowPlusSkew = new Date(dateAndTime.getTime() + skew);
                nowMinusSkew = new Date(dateAndTime.getTime() - skew);
            }

            // Check that the test date is within the validity interval:
            //   [ thisUpdate - MAX_CLOCK_SKEW,
            //     nextUpdate + MAX_CLOCK_SKEW ]
            if (nowMinusSkew.after(nextUpdate)
                || nowPlusSkew.before(crlThisUpdate)) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: update out-of-range");
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a copy of this object.
     *
     * @return the copy
     */
    public Object clone() {
        try {
            X509CRLSelector copy = (X509CRLSelector)super.clone();
            if (issuerNames != null) {
                copy.issuerNames =
                        new HashSet<>(issuerNames);
                copy.issuerX500Principals =
                        new HashSet<>(issuerX500Principals);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            /* Cannot happen */
            throw new InternalError(e.toString(), e);
        }
    }
}
