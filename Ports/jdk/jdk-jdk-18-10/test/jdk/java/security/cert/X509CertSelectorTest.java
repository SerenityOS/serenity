/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
import static sun.security.x509.GeneralNameInterface.NAME_DIRECTORY;
import static sun.security.x509.NameConstraintsExtension.EXCLUDED_SUBTREES;
import static sun.security.x509.NameConstraintsExtension.PERMITTED_SUBTREES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.CertificatePoliciesExtension;
import sun.security.x509.DNSName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralSubtree;
import sun.security.x509.GeneralSubtrees;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.PolicyInformation;
import sun.security.x509.PrivateKeyUsageExtension;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;

/*
 * @test
 * @bug 8074931
 * @summary This class tests the X509CertSelector. The tests check particular criteria
 *          by setting them to a value that should match our test certificate and
 *          ensuring that they do match, then setting them to a value that should not
 *          match our test certificate and ensuring that they do not match.
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.util
 */
public class X509CertSelectorTest {
    /*
            Certificate:
                Data:
                    Version: 3 (0x2)
                    Serial Number: 954172088 (0x38df82b8)
                Signature Algorithm: dsaWithSHA1
                    Issuer: C=us, O=sun, OU=testing
                    Validity
                        Not Before: Mar 27 15:48:08 2000 GMT
                        Not After : Jun 25 14:48:08 2000 GMT
                    Subject: C=us, O=sun, OU=testing, CN=mullan
                    Subject Public Key Info:
                        Public Key Algorithm: dsaEncryption
                            pub:  0
                            P:    0
                            Q:    0
                            G:    0
                    X509v3 extensions:
                        X509v3 Name Constraints: critical
                            0D.B0@.>1.0...U....us1.0
            ..U.
            ..sun1.0...U....testing1.0
            ..U....mullan
                        X509v3 Subject Key Identifier:
                            56:E8:88:AE:9D:B5:3F:2B:CB:A0:4C:4B:E2:87:53:07:33:77:1B:DF
                        X509v3 Authority Key Identifier:
                            keyid:8E:DD:AF:6F:EE:02:12:F4:61:E9:2F:E3:64:1A:6F:71:32:25:20:C0

                        X509v3 Subject Alternative Name:
                            email:mullan@east.sun.com
                        X509v3 Private Key Usage Period:
                            Not Before: Jan  1 05:00:00 2000 GMT, Not After: Jan  1 05:00:00 2001 GMT
                        X509v3 Key Usage: critical
                            Digital Signature
                        X509v3 Certificate Policies:
                            0$0\..*...0.0...+.......0..
            Testing...
                Signature Algorithm: dsaWithSHA1
                     r:
                         44:c7:35:40:5d:6c:28:75:7f:73:b2:f8:0d:72:6c:
                         09:65:b8:81:14
                     s:
                         76:79:f5:c7:37:3b:0d:9b:db:70:2f:20:80:36:e3:
                         80:e8:a6:c6:71
    */
    private static final String testCert =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICLjCCAeygAwIBAgIEON+CuDALBgcqhkjOOAQDBQAwLTELMAkGA1UEBhMCdXMx\n" +
            "DDAKBgNVBAoTA3N1bjEQMA4GA1UECxMHdGVzdGluZzAeFw0wMDAzMjcxNTQ4MDha\n" +
            "Fw0wMDA2MjUxNDQ4MDhaMD4xCzAJBgNVBAYTAnVzMQwwCgYDVQQKEwNzdW4xEDAO\n" +
            "BgNVBAsTB3Rlc3RpbmcxDzANBgNVBAMTBm11bGxhbjAcMBQGByqGSM44BAEwCQIB\n" +
            "AAIBAAIBAAMEAAIBAKOCASMwggEfMFAGA1UdHgEB/wRGMESgQjBApD4xCzAJBgNV\n" +
            "BAYTAnVzMQwwCgYDVQQKEwNzdW4xEDAOBgNVBAsTB3Rlc3RpbmcxDzANBgNVBAMT\n" +
            "Bm11bGxhbjAdBgNVHQ4EFgQUVuiIrp21PyvLoExL4odTBzN3G98wHwYDVR0jBBgw\n" +
            "FoAUjt2vb+4CEvRh6S/jZBpvcTIlIMAwHgYDVR0RBBcwFYETbXVsbGFuQGVhc3Qu\n" +
            "c3VuLmNvbTArBgNVHRAEJDAigA8yMDAwMDEwMTA1MDAwMFqBDzIwMDEwMTAxMDUw\n" +
            "MDAwWjAPBgNVHQ8BAf8EBQMDB4AAMC0GA1UdIAQmMCQwIgYEKoSAADAaMBgGCCsG\n" +
            "AQUFBwICMAwSClRlc3RpbmcuLi4wCwYHKoZIzjgEAwUAAy8AMCwCFETHNUBdbCh1\n" +
            "f3Oy+A1ybAlluIEUAhR2efXHNzsNm9twLyCANuOA6KbGcQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "";

    private static final String testKey =
            "MIIBtjCCASsGByqGSM44BAEwggEeAoGBAIVWPEkcxbxhQRCqVzg55tNqbP5j0K4kdu4bkmXvfqC5\n" +
            "+qA75DvnfzsOJseb+9AuKXWk/DvCzFDmrY1YaU3scZC3OQEO9lEO3F4VDKOaudY6OT1SI22pAIwz\n" +
            "j5pvq+i7zOp4xUqkQUeh/4iQSfxOT5UrFGjkcbnbpVkCXD/GxAz7AhUAjtnm3dVIddUUHl6wxpZ7\n" +
            "GcA6gSsCgYAf/PXzQtemgIDjpFrNNSgTEKkLposBXKatAM+gUKlMUjf8SQvquqPxDtRrscGjXkoL\n" +
            "oTkaR7/akULYFpBvUcFkeIFiCnJg8M9XhCWdLvn9MPt+jR2oxookvCb9xLtD6WvIM/wd/nZ1iK4u\n" +
            "iY1+q85xvns/Awbtwl7oZDAwE2TUKAOBhAACgYBDc9UZ+3xsZubUZvRG5cpyJceYpJp2exOPVJXn\n" +
            "jR4CcR+cT9bAJpFSxqE/8KtNHXxHdu4f3DU67IMOVDpugzihyzXJvNm3w2H9x+6xczHG2wjvAJeh\n" +
            "X62EWbUatxPXFAoVKZWuUbaYaZzdWBDtNRrCuKKsLo0GFy8g2BZISuD3jw==\n" +
            "";

    // Certificate to run tests on
    private final X509Certificate cert;

    public static void main(String[] args) throws Exception {
        X509CertSelectorTest test = new X509CertSelectorTest();
        test.doTest();
    }

    public X509CertSelectorTest() throws CertificateException, IOException {
        cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(testCert.getBytes()));
    }

    // Runs the test.
    private void doTest() throws Exception {
        System.out.println("START OF TESTS FOR " + "X509CertSelector");

        testSerialNumber();
        testIssuer();
        testSubjectKeyIdentifier();
        testAuthorityKeyIdentifier();
        testCertificateValid();
        testPrivateKeyValid();
        testSubjectPublicKeyAlgID();
        testKeyUsage();
        testSubjectAltName();
        testPolicy();
        testPathToName();
        testSubject();
        testSubjectPublicKey();
        testNameConstraints();
        testBasicConstraints();
        testCertificate();
    }

    // Tests matching on the serial number contained in the certificate.
    private void testSerialNumber() {
        System.out.println("X.509 Certificate Match on serialNumber");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        selector.setSerialNumber(new BigInteger("999999999"));
        checkMatch(selector, cert, false);

        // good match
        selector.setSerialNumber(cert.getSerialNumber());
        checkMatch(selector, cert, true);
    }

    // Tests matching on the issuer name contained in the certificate.
    private void testIssuer() throws IOException {
        System.out.println("X.509 Certificate Match on issuer");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        selector.setIssuer("ou=bogus,ou=east,o=sun,c=us");
        checkMatch(selector, cert, false);

        // good match
        selector.setIssuer((cert.getIssuerX500Principal()).getName("RFC2253"));
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the subject key identifier contained in the
     * certificate.
     */
    private void testSubjectKeyIdentifier() throws IOException {
        System.out.println("X.509 Certificate Match on subjectKeyIdentifier");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        selector.setSubjectKeyIdentifier(b);
        checkMatch(selector, cert, false);

        // good match
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.14"));
        byte[] encoded = in.getOctetString();
        selector.setSubjectKeyIdentifier(encoded);
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the authority key identifier contained in the
     * certificate.
     */
    private void testAuthorityKeyIdentifier() throws IOException {
        System.out.println("X.509 Certificate Match on authorityKeyIdentifier");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        AuthorityKeyIdentifierExtension a = new AuthorityKeyIdentifierExtension(new KeyIdentifier(b), null, null);
        selector.setAuthorityKeyIdentifier(a.getExtensionValue());
        checkMatch(selector, cert, false);

        // good match
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.35"));
        byte[] encoded = in.getOctetString();
        selector.setAuthorityKeyIdentifier(encoded);
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the certificate validity component contained in the
     * certificate.
     */
    private void testCertificateValid() {
        System.out.println("X.509 Certificate Match on certificateValid");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        Calendar cal = Calendar.getInstance();
        cal.set(1968, 12, 31);
        selector.setCertificateValid(cal.getTime());
        checkMatch(selector, cert, false);

        // good match
        selector.setCertificateValid(cert.getNotBefore());
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the private key validity component contained in the
     * certificate.
     */
    private void testPrivateKeyValid() throws IOException, CertificateException {
        System.out.println("X.509 Certificate Match on privateKeyValid");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        Calendar cal = Calendar.getInstance();
        cal.set(1968, 12, 31);
        selector.setPrivateKeyValid(cal.getTime());
        checkMatch(selector, cert, false);

        // good match
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.16"));
        byte[] encoded = in.getOctetString();
        PrivateKeyUsageExtension ext = new PrivateKeyUsageExtension(false, encoded);
        Date validDate = (Date) ext.get(PrivateKeyUsageExtension.NOT_BEFORE);
        selector.setPrivateKeyValid(validDate);
        checkMatch(selector, cert, true);

    }

    private ObjectIdentifier getCertPubKeyAlgOID(X509Certificate xcert) throws IOException {
        byte[] encodedKey = xcert.getPublicKey().getEncoded();
        DerValue val = new DerValue(encodedKey);
        if (val.tag != DerValue.tag_Sequence) {
            throw new RuntimeException("invalid key format");
        }

        return AlgorithmId.parse(val.data.getDerValue()).getOID();
    }

    /*
     * Tests matching on the subject public key algorithm ID component contained
     * in the certificate.
     */
    private void testSubjectPublicKeyAlgID() throws IOException {
        System.out.println("X.509 Certificate Match on subjectPublicKeyAlgID");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        selector.setSubjectPublicKeyAlgID("2.5.29.14");
        checkMatch(selector, cert, false);

        // good match
        selector.setSubjectPublicKeyAlgID(getCertPubKeyAlgOID(cert).toString());
        checkMatch(selector, cert, true);

    }

    // Tests matching on the key usage extension contained in the certificate.
    private void testKeyUsage() {
        System.out.println("X.509 Certificate Match on keyUsage");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        boolean[] keyUsage = { true, false, true, false, true, false, true, false };
        selector.setKeyUsage(keyUsage);
        System.out.println("Selector = " + selector.toString());
        checkMatch(selector, cert, false);

        // good match
        selector.setKeyUsage(cert.getKeyUsage());
        System.out.println("Selector = " + selector.toString());
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the subject alternative name extension contained in the
     * certificate.
     */
    private void testSubjectAltName() throws IOException {
        System.out.println("X.509 Certificate Match on subjectAltName");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        GeneralNameInterface dnsName = new DNSName("foo.com");
        DerOutputStream tmp = new DerOutputStream();
        dnsName.encode(tmp);
        selector.addSubjectAlternativeName(2, tmp.toByteArray());
        checkMatch(selector, cert, false);

        // good match
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.17"));
        byte[] encoded = in.getOctetString();
        SubjectAlternativeNameExtension ext = new SubjectAlternativeNameExtension(false, encoded);
        GeneralNames names = (GeneralNames) ext.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
        GeneralName name = (GeneralName) names.get(0);
        selector.setSubjectAlternativeNames(null);
        DerOutputStream tmp2 = new DerOutputStream();
        name.getName().encode(tmp2);
        selector.addSubjectAlternativeName(name.getType(), tmp2.toByteArray());
        checkMatch(selector, cert, true);

        // good match 2 (matches at least one)
        selector.setMatchAllSubjectAltNames(false);
        selector.addSubjectAlternativeName(2, "foo.com");
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the policy constraints extension contained in the
     * certificate.
     */
    private void testPolicy() throws IOException {
        System.out.println("X.509 Certificate Match on certificatePolicies");
        // test encoding of CertificatePoliciesExtension because we wrote the
        // code
        // bad match
        X509CertSelector selector = new X509CertSelector();
        Set<String> s = new HashSet<>();
        s.add(new String("1.2.5.7.68"));
        selector.setPolicy(s);
        checkMatch(selector, cert, false);

        // good match
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.32"));
        CertificatePoliciesExtension ext = new CertificatePoliciesExtension(false, in.getOctetString());
        List<PolicyInformation> policies = ext.get(CertificatePoliciesExtension.POLICIES);
        // match on the first policy id
        PolicyInformation policyInfo = (PolicyInformation) policies.get(0);
        s.clear();
        s.add(policyInfo.getPolicyIdentifier().getIdentifier().toString());
        selector.setPolicy(s);
        checkMatch(selector, cert, true);
    }

    /*
     * Tests matching on the name constraints extension contained in the
     * certificate.
     */
    private void testPathToName() throws IOException {
        System.out.println("X.509 Certificate Match on pathToName");

        X509CertSelector selector = null;
        DerInputStream in = new DerInputStream(cert.getExtensionValue("2.5.29.30"));
        byte[] encoded = in.getOctetString();
        NameConstraintsExtension ext = new NameConstraintsExtension(false, encoded);
        GeneralSubtrees permitted = (GeneralSubtrees) ext.get(PERMITTED_SUBTREES);
        GeneralSubtrees excluded = (GeneralSubtrees) ext.get(EXCLUDED_SUBTREES);

        // bad matches on pathToName within excluded subtrees
        if (excluded != null) {
            Iterator<GeneralSubtree> e = excluded.iterator();
            while (e.hasNext()) {
                GeneralSubtree tree = e.next();
                if (tree.getName().getType() == NAME_DIRECTORY) {
                    X500Name excludedDN1 = new X500Name(tree.getName().toString());
                    X500Name excludedDN2 = new X500Name("CN=Bogus, " + tree.getName().toString());
                    DerOutputStream derDN1 = new DerOutputStream();
                    DerOutputStream derDN2 = new DerOutputStream();
                    excludedDN1.encode(derDN1);
                    excludedDN2.encode(derDN2);
                    selector = new X509CertSelector();
                    selector.addPathToName(NAME_DIRECTORY, derDN1.toByteArray());
                    checkMatch(selector, cert, false);
                    selector.setPathToNames(null);
                    selector.addPathToName(NAME_DIRECTORY, derDN2.toByteArray());
                    checkMatch(selector, cert, false);
                }
            }
        }

        // good matches on pathToName within permitted subtrees
        if (permitted != null) {
            Iterator<GeneralSubtree> e = permitted.iterator();
            while (e.hasNext()) {
                GeneralSubtree tree = e.next();
                if (tree.getName().getType() == NAME_DIRECTORY) {
                    X500Name permittedDN1 = new X500Name(tree.getName().toString());
                    X500Name permittedDN2 = new X500Name("CN=good, " + tree.getName().toString());
                    DerOutputStream derDN1 = new DerOutputStream();
                    DerOutputStream derDN2 = new DerOutputStream();
                    permittedDN1.encode(derDN1);
                    permittedDN2.encode(derDN2);
                    selector = new X509CertSelector();
                    selector.addPathToName(NAME_DIRECTORY, derDN1.toByteArray());
                    checkMatch(selector, cert, true);
                    selector.setPathToNames(null);
                    selector.addPathToName(NAME_DIRECTORY, derDN2.toByteArray());
                    checkMatch(selector, cert, true);
                }
            }
        }
    }

    // Tests matching on the subject name contained in the certificate.
    private void testSubject() throws IOException {
        System.out.println("X.509 Certificate Match on subject");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        selector.setSubject("ou=bogus,ou=east,o=sun,c=us");
        checkMatch(selector, cert, false);

        // good match
        selector.setSubject(cert.getSubjectX500Principal().getName("RFC2253"));
        checkMatch(selector, cert, true);
    }

    // Tests matching on the subject public key contained in the certificate.
    private void testSubjectPublicKey() throws IOException, GeneralSecurityException {
        System.out.println("X.509 Certificate Match on subject public key");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                Base64.getMimeDecoder().decode(testKey.getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        selector.setSubjectPublicKey(pubKey);
        checkMatch(selector, cert, false);

        // good match
        selector.setSubjectPublicKey(cert.getPublicKey());
        checkMatch(selector, cert, true);
    }

    // Tests matching on the name constraints contained in the certificate.
    private void testNameConstraints() throws IOException {
        System.out.println("X.509 Certificate Match on name constraints");
        // bad match
        GeneralSubtrees subjectTree = new GeneralSubtrees();
        subjectTree.add(getGeneralSubtree((X500Name) cert.getSubjectDN()));
        NameConstraintsExtension ext = new NameConstraintsExtension((GeneralSubtrees) null, subjectTree);
        X509CertSelector selector = new X509CertSelector();
        selector.setNameConstraints(ext.getExtensionValue());
        checkMatch(selector, cert, false);

        // good match
        ext = new NameConstraintsExtension(subjectTree, null);
        selector.setNameConstraints(ext.getExtensionValue());
        checkMatch(selector, cert, true);
    }

    // Tests matching on basic constraints.
    private void testBasicConstraints() {
        System.out.println("X.509 Certificate Match on basic constraints");
        // bad match
        X509CertSelector selector = new X509CertSelector();
        int mpl = cert.getBasicConstraints();
        selector.setBasicConstraints(0);
        checkMatch(selector, cert, false);

        // good match
        selector.setBasicConstraints(mpl);
        checkMatch(selector, cert, true);
    }

    // Tests certificateEquals criterion
    private void testCertificate() {
        System.out.println("X.509 Certificate Match on certificateEquals criterion");

        X509CertSelector selector = new X509CertSelector();
        // good match
        selector.setCertificate(cert);
        checkMatch(selector, cert, true);
    }

    private void checkMatch(X509CertSelector selector, X509Certificate cert, boolean match) {
        boolean result = selector.match(cert);
        if (match != result)
            throw new RuntimeException(selector + " match " + cert + " is " + result + ", but expect " + match);
    }

    private static GeneralSubtree getGeneralSubtree(GeneralNameInterface gni) {
        // Create a new GeneralSubtree with the specified name, 0 base, and
        // unlimited length
        GeneralName gn = new GeneralName(gni);
        GeneralSubtree subTree = new GeneralSubtree(gn, 0, -1);
        return subTree;
    }
}
