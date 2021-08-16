/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.io.IOException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.security.cert.*;
import java.text.Normalizer;
import java.util.*;
import javax.security.auth.x500.X500Principal;
import javax.net.ssl.SNIHostName;

import sun.net.util.IPAddressUtil;
import sun.security.x509.X500Name;
import sun.security.ssl.SSLLogger;

/**
 * Class to check hostnames against the names specified in a certificate as
 * required for TLS and LDAP.
 *
 */
public class HostnameChecker {

    // Constant for a HostnameChecker for TLS
    public static final byte TYPE_TLS = 1;
    private static final HostnameChecker INSTANCE_TLS =
                                        new HostnameChecker(TYPE_TLS);

    // Constant for a HostnameChecker for LDAP
    public static final byte TYPE_LDAP = 2;
    private static final HostnameChecker INSTANCE_LDAP =
                                        new HostnameChecker(TYPE_LDAP);

    // constants for subject alt names of type DNS and IP
    private static final int ALTNAME_DNS = 2;
    private static final int ALTNAME_IP  = 7;

    // the algorithm to follow to perform the check. Currently unused.
    private final byte checkType;

    private HostnameChecker(byte checkType) {
        this.checkType = checkType;
    }

    /**
     * Get a HostnameChecker instance. checkType should be one of the
     * TYPE_* constants defined in this class.
     */
    public static HostnameChecker getInstance(byte checkType) {
        if (checkType == TYPE_TLS) {
            return INSTANCE_TLS;
        } else if (checkType == TYPE_LDAP) {
            return INSTANCE_LDAP;
        }
        throw new IllegalArgumentException("Unknown check type: " + checkType);
    }

    /**
     * Perform the check.
     *
     * @param expectedName the expected host name or ip address
     * @param cert the certificate to check against
     * @param chainsToPublicCA true if the certificate chains to a public
     *     root CA (as pre-installed in the cacerts file)
     * @throws CertificateException if the name does not match any of
     *     the names specified in the certificate
     */
    public void match(String expectedName, X509Certificate cert,
                      boolean chainsToPublicCA) throws CertificateException {
        if (expectedName == null) {
            throw new CertificateException("Hostname or IP address is " +
                    "undefined.");
        }
        if (isIpAddress(expectedName)) {
           matchIP(expectedName, cert);
        } else {
           matchDNS(expectedName, cert, chainsToPublicCA);
        }
    }

    public void match(String expectedName, X509Certificate cert)
            throws CertificateException {
        match(expectedName, cert, false);
    }

    /**
     * Test whether the given hostname looks like a literal IPv4 or IPv6
     * address. The hostname does not need to be a fully qualified name.
     *
     * This is not a strict check that performs full input validation.
     * That means if the method returns true, name need not be a correct
     * IP address, rather that it does not represent a valid DNS hostname.
     * Likewise for IP addresses when it returns false.
     */
    private static boolean isIpAddress(String name) {
        if (IPAddressUtil.isIPv4LiteralAddress(name) ||
            IPAddressUtil.isIPv6LiteralAddress(name)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if the certificate allows use of the given IP address.
     *
     * From RFC2818:
     * In some cases, the URI is specified as an IP address rather than a
     * hostname. In this case, the iPAddress subjectAltName must be present
     * in the certificate and must exactly match the IP in the URI.
     */
    private static void matchIP(String expectedIP, X509Certificate cert)
            throws CertificateException {
        Collection<List<?>> subjAltNames = cert.getSubjectAlternativeNames();
        if (subjAltNames == null) {
            throw new CertificateException
                                ("No subject alternative names present");
        }
        for (List<?> next : subjAltNames) {
            // For IP address, it needs to be exact match
            if (((Integer)next.get(0)).intValue() == ALTNAME_IP) {
                String ipAddress = (String)next.get(1);
                if (expectedIP.equalsIgnoreCase(ipAddress)) {
                    return;
                } else {
                    // compare InetAddress objects in order to ensure
                    // equality between a long IPv6 address and its
                    // abbreviated form.
                    try {
                        if (InetAddress.getByName(expectedIP).equals(
                                InetAddress.getByName(ipAddress))) {
                            return;
                        }
                    } catch (UnknownHostException e) {
                    } catch (SecurityException e) {}
                }
            }
        }
        throw new CertificateException("No subject alternative " +
                        "names matching " + "IP address " +
                        expectedIP + " found");
    }

    /**
     * Check if the certificate allows use of the given DNS name.
     *
     * From RFC2818:
     * If a subjectAltName extension of type dNSName is present, that MUST
     * be used as the identity. Otherwise, the (most specific) Common Name
     * field in the Subject field of the certificate MUST be used. Although
     * the use of the Common Name is existing practice, it is deprecated and
     * Certification Authorities are encouraged to use the dNSName instead.
     *
     * Matching is performed using the matching rules specified by
     * [RFC5280].  If more than one identity of a given type is present in
     * the certificate (e.g., more than one dNSName name, a match in any one
     * of the set is considered acceptable.)
     */
    private void matchDNS(String expectedName, X509Certificate cert,
                          boolean chainsToPublicCA)
            throws CertificateException {
        // Check that the expected name is a valid domain name.
        try {
            // Using the checking implemented in SNIHostName
            SNIHostName sni = new SNIHostName(expectedName);
        } catch (IllegalArgumentException iae) {
            throw new CertificateException(
                "Illegal given domain name: " + expectedName, iae);
        }

        Collection<List<?>> subjAltNames = cert.getSubjectAlternativeNames();
        if (subjAltNames != null) {
            boolean foundDNS = false;
            for (List<?> next : subjAltNames) {
                if (((Integer)next.get(0)).intValue() == ALTNAME_DNS) {
                    foundDNS = true;
                    String dnsName = (String)next.get(1);
                    if (isMatched(expectedName, dnsName, chainsToPublicCA)) {
                        return;
                    }
                }
            }
            if (foundDNS) {
                // if certificate contains any subject alt names of type DNS
                // but none match, reject
                throw new CertificateException("No subject alternative DNS "
                        + "name matching " + expectedName + " found.");
            }
        }
        X500Name subjectName = getSubjectX500Name(cert);
        DerValue derValue = subjectName.findMostSpecificAttribute
                                                    (X500Name.commonName_oid);
        if (derValue != null) {
            try {
                String cname = derValue.getAsString();
                if (!Normalizer.isNormalized(cname, Normalizer.Form.NFKC)) {
                    throw new CertificateException("Not a formal name "
                            + cname);
                }
                if (isMatched(expectedName, cname, chainsToPublicCA)) {
                    return;
                }
            } catch (IOException e) {
                // ignore
            }
        }
        String msg = "No name matching " + expectedName + " found";
        throw new CertificateException(msg);
    }


    /**
     * Return the subject of a certificate as X500Name, by reparsing if
     * necessary. X500Name should only be used if access to name components
     * is required, in other cases X500Principal is to be preferred.
     *
     * This method is currently used from within JSSE, do not remove.
     */
    @SuppressWarnings("deprecation")
    public static X500Name getSubjectX500Name(X509Certificate cert)
            throws CertificateParsingException {
        try {
            Principal subjectDN = cert.getSubjectDN();
            if (subjectDN instanceof X500Name) {
                return (X500Name)subjectDN;
            } else {
                X500Principal subjectX500 = cert.getSubjectX500Principal();
                return new X500Name(subjectX500.getEncoded());
            }
        } catch (IOException e) {
            throw(CertificateParsingException)
                new CertificateParsingException().initCause(e);
        }
    }


    /**
     * Returns true if name matches against template.<p>
     *
     * The matching is performed as per RFC 2818 rules for TLS and
     * RFC 2830 rules for LDAP.<p>
     *
     * The <code>name</code> parameter should represent a DNS name.  The
     * <code>template</code> parameter may contain the wildcard character '*'.
     */
    private boolean isMatched(String name, String template,
                              boolean chainsToPublicCA) {

        // Normalize to Unicode, because PSL is in Unicode.
        try {
            name = IDN.toUnicode(IDN.toASCII(name));
            template = IDN.toUnicode(IDN.toASCII(template));
        } catch (RuntimeException re) {
            if (SSLLogger.isOn) {
                SSLLogger.fine("Failed to normalize to Unicode: " + re);
            }

            return false;
        }

        if (hasIllegalWildcard(template, chainsToPublicCA)) {
            return false;
        }

        // check the validity of the domain name template.
        try {
            // Replacing wildcard character '*' with 'z' so as to check
            // the domain name template validity.
            //
            // Using the checking implemented in SNIHostName
            new SNIHostName(template.replace('*', 'z'));
        } catch (IllegalArgumentException iae) {
            // It would be nice to add debug log if not matching.
            return false;
        }

        if (checkType == TYPE_TLS) {
            return matchAllWildcards(name, template);
        } else if (checkType == TYPE_LDAP) {
            return matchLeftmostWildcard(name, template);
        } else {
            return false;
        }
    }

    /**
     * Returns true if the template contains an illegal wildcard character.
     */
    private static boolean hasIllegalWildcard(
            String template, boolean chainsToPublicCA) {
        // not ok if it is a single wildcard character or "*."
        if (template.equals("*") || template.equals("*.")) {
            if (SSLLogger.isOn) {
                SSLLogger.fine(
                    "Certificate domain name has illegal single " +
                      "wildcard character: " + template);
            }
            return true;
        }

        int lastWildcardIndex = template.lastIndexOf("*");

        // ok if it has no wildcard character
        if (lastWildcardIndex == -1) {
            return false;
        }

        String afterWildcard = template.substring(lastWildcardIndex);
        int firstDotIndex = afterWildcard.indexOf(".");

        // not ok if there is no dot after wildcard (ex: "*com")
        if (firstDotIndex == -1) {
            if (SSLLogger.isOn) {
                SSLLogger.fine(
                    "Certificate domain name has illegal wildcard, " +
                    "no dot after wildcard character: " + template);
            }
            return true;
        }

        if (!chainsToPublicCA) {
            return false; // skip check for non-public certificates
        }

        // If the wildcarded domain is a top-level domain under which names
        // can be registered, then a wildcard is not allowed.
        String wildcardedDomain = afterWildcard.substring(firstDotIndex + 1);
        String templateDomainSuffix =
                RegisteredDomain.from("z." + wildcardedDomain)
                    .filter(d -> d.type() == RegisteredDomain.Type.ICANN)
                    .map(RegisteredDomain::publicSuffix).orElse(null);
        if (templateDomainSuffix == null) {
            return false;   // skip check if not known public suffix
        }

        // Is it a top-level domain?
        if (wildcardedDomain.equalsIgnoreCase(templateDomainSuffix)) {
            if (SSLLogger.isOn) {
                SSLLogger.fine(
                    "Certificate domain name has illegal " +
                    "wildcard for top-level public suffix: " + template);
            }
            return true;
        }

        return false;
    }

    /**
     * Returns true if name matches against template.<p>
     *
     * According to RFC 2818, section 3.1 -
     * Names may contain the wildcard character * which is
     * considered to match any single domain name component
     * or component fragment.
     * E.g., *.a.com matches foo.a.com but not
     * bar.foo.a.com. f*.com matches foo.com but not bar.com.
     */
    private static boolean matchAllWildcards(String name,
         String template) {
        name = name.toLowerCase(Locale.ENGLISH);
        template = template.toLowerCase(Locale.ENGLISH);
        StringTokenizer nameSt = new StringTokenizer(name, ".");
        StringTokenizer templateSt = new StringTokenizer(template, ".");

        if (nameSt.countTokens() != templateSt.countTokens()) {
            return false;
        }

        while (nameSt.hasMoreTokens()) {
            if (!matchWildCards(nameSt.nextToken(),
                        templateSt.nextToken())) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns true if name matches against template.<p>
     *
     * As per RFC 2830, section 3.6 -
     * The "*" wildcard character is allowed.  If present, it applies only
     * to the left-most name component.
     * E.g. *.bar.com would match a.bar.com, b.bar.com, etc. but not
     * bar.com.
     */
    private static boolean matchLeftmostWildcard(String name,
                         String template) {
        name = name.toLowerCase(Locale.ENGLISH);
        template = template.toLowerCase(Locale.ENGLISH);

        // Retrieve leftmost component
        int templateIdx = template.indexOf(".");
        int nameIdx = name.indexOf(".");

        if (templateIdx == -1)
            templateIdx = template.length();
        if (nameIdx == -1)
            nameIdx = name.length();

        if (matchWildCards(name.substring(0, nameIdx),
            template.substring(0, templateIdx))) {

            // match rest of the name
            return template.substring(templateIdx).equals(
                        name.substring(nameIdx));
        } else {
            return false;
        }
    }


    /**
     * Returns true if the name matches against the template that may
     * contain wildcard char * <p>
     */
    private static boolean matchWildCards(String name, String template) {

        int wildcardIdx = template.indexOf("*");
        if (wildcardIdx == -1)
            return name.equals(template);

        boolean isBeginning = true;
        String beforeWildcard = "";
        String afterWildcard = template;

        while (wildcardIdx != -1) {

            // match in sequence the non-wildcard chars in the template.
            beforeWildcard = afterWildcard.substring(0, wildcardIdx);
            afterWildcard = afterWildcard.substring(wildcardIdx + 1);

            int beforeStartIdx = name.indexOf(beforeWildcard);
            if ((beforeStartIdx == -1) ||
                        (isBeginning && beforeStartIdx != 0)) {
                return false;
            }
            isBeginning = false;

            // update the match scope
            name = name.substring(beforeStartIdx + beforeWildcard.length());
            wildcardIdx = afterWildcard.indexOf("*");
        }
        return name.endsWith(afterWildcard);
    }
}
