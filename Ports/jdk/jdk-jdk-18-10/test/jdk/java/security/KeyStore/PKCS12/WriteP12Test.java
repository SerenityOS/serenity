/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;

/*
 * @test
 * @bug 8048618
 * @summary Write different types p12 key store to Check the write related
 *  APIs.
 * @run main WriteP12Test
 */

public class WriteP12Test {

    private static final String IN_KEYSTORE_TYPE = "jks";
    private static final String IN_KEYSTORE_PRV = "SUN";

    private static final String IN_KEYSTORE_ENDUSER = "keystoreEU.jks.data";
    private static final String IN_KEYSTORE_CA = "keystoreCA.jks.data";
    private static final String OUT_KEYSTORE = "outKeyStore.p12";

    private static final String IN_STORE_PASS = "storepass";
    private static final String IN_KEY_PASS = "keypass";

    private static final String CERT_PATH = System.getProperty("test.src", ".")
            + File.separator + "certs" + File.separator + "writeP12"
            + File.separator;

    private static final String CA_CERT_STR = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDFzCCAf8CBD8+0nAwDQYJKoZIhvcNAQEFBQAwUDELMAkGA1UEBhMCV\n"
            + "VMxETAPBgNVBAoTCEphdmFTb2Z0MRUwEwYDVQQLEwxTZWN1cml0eSBTUU\n"
            + "UxFzAVBgNVBAMTDlBLQ1MxMiBUZXN0IENBMB4XDTAzMDgxNzAwNTUxMlo\n"
            + "XDTEzMDgxNDAwNTUxMlowUDELMAkGA1UEBhMCVVMxETAPBgNVBAoTCEph\n"
            + "dmFTb2Z0MRUwEwYDVQQLEwxTZWN1cml0eSBTUUUxFzAVBgNVBAMTDlBLQ\n"
            + "1MxMiBUZXN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQ\n"
            + "EAk7Sh+K/yGsmJacZnjfkZfuWxGNJCPW0q69exwoRP+eBHMQwG00yi9aL\n"
            + "SsZAqNpJSCDvpgySOAUmBd+f8WFhHqJfRVREVfv3mradDKZCjhqtsUI7I\n"
            + "wRTYYy9clFkeeK4dHaxbuFMPpUu7yQfwSTXgvOA/UJ4kJuGtaYAdTJI4e\n"
            + "f1mUASo6+dea0UZA/FHCuV7O6z3hr5VHlyhJL2/o/8M5tGBTBISODJSnn\n"
            + "GNBvtQLNHnWYvs470UAE2BtuCGYh1V/3HAH1tRirS3MBBcb1XnIkiiXR3\n"
            + "tjaBSB+XhoCfuG8KtInXXFaAnvKfY9mYFw6VJt9JYQpY2VDC7281/Pbz0\n"
            + "dQIDAQABMA0GCSqGSIb3DQEBBQUAA4IBAQBzXZ8zHWrnC8/E+N/n2Czhx\n"
            + "i18YQc2LPWBDLYTTxoFEazWmYLv1k/JT7Nta1qu1quvxXJ4uV1XHbd9NF\n"
            + "AJWKwtFQEpfv4o6I7qWUPoxnfA+jyqKXxv27z25tzt+Y4xOEhqvO03G0Q\n"
            + "imhkiNt9MF7L69y2U0/U73+uFNGzdAEDiI9EibvICiOnr1TeQ5GekK3Yb\n"
            + "k5qe3lviMZPkkSXepTJI8m0AiXCji+eXj97jVLeH+RxeBchBY+uELrqUr\n"
            + "sVOVWh7IBCqC/V7FqUTkmD1IFlzkkinatpl42s1MbhJId2yQkzaeBRc\n"
            + "suE63bDEtuRWp9ynMO3QA4Yu85uBRWGzQ1Di\n"
            + "-----END CERTIFICATE-----";
    private static final String LEAD_CERT = "-----BEGIN CERTIFICATE-----\n"
            + "MIICwDCCAaigAwIBAgIEPz7S1jANBgkqhkiG9w0BAQQFADBQMQswCQYDV\n"
            + "QQGEwJVUzERMA8GA1UEChMISmF2YVNvZnQxFTATBgNVBAsTDFNlY3VyaX\n"
            + "R5IFNRRTEXMBUGA1UEAxMOUEtDUzEyIFRlc3QgQ0EwHhcNMDAwODA5MDc\n"
            + "wMDAwWhcNMTAwODA3MDcwMDAwWjBSMQswCQYDVQQGEwJVUzERMA8GA1UE\n"
            + "ChMISmF2YVNvZnQxFTATBgNVBAsTDFNlY3VyaXR5IFNRRTEZMBcGA1UEA\n"
            + "xMQUEtDUzEyIFRlc3QgTGVhZDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgY\n"
            + "kCgYEAzq9X2USz/WjDhT+jUyZWqB5h4A33tS11YqH5qYvqjTXjcUI6gOp\n"
            + "moXMafDG9RHRlIccvp51xLp7Ap3WMrv411lWBttqtZi5c1/DEC1cEM/Sl\n"
            + "PCk1r2zFbkJu7QKieXeMcrjZEo6LcBHMwQjIpI+up9cr3VjuyqG/olQkU\n"
            + "mXVuS0CAwEAAaMkMCIwDwYDVR0PAQH/BAUDAweAADAPBgNVHRMBAf8EBT\n"
            + "ADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBhbuim98TWmtv9vSldRE7RvQ8\n"
            + "FlS0TyZVO7kcSNtfCUE4R76J1ElN74Koc5pQnUtduLeQJs2ao/mEcCZsE\n"
            + "zVcwI3mSZrSzPhc8s7w5gOQA4TUwVLSSjKgBCaZ7R3+qJ3QeqPJ5O6sFz\n"
            + "pvBYkgSa4MWptK41jbmT8cwZQJXFCi8WxFFJ+p97F1Ppm3LgmYmtiUP4M\n"
            + "ZQwOBvpTZWXU0WrqFXpzWQx0mg4SX19fZm4nLcJAerCEUphf8ILagtpQM\n"
            + "EErT3/jg6mfCdT3Rj055QXPfF4OiRFevPF5a1fZgrCePCukRQZcd7s8K5\n"
            + "OBIaryuM0MdFtlzxi6XWeUNpVFFHURcy\n"
            + "-----END CERTIFICATE-----";
    private static final String END_CERT = "-----BEGIN CERTIFICATE-----\n"
            + "MIICNjCCAZ+gAwIBAgIEPz7WtzANBgkqhkiG9w0BAQQFADBSMQswCQYDV\n"
            + "QQGEwJVUzERMA8GA1UEChMISmF2YVNvZnQxFTATBgNVBAsTDFNlY3VyaX\n"
            + "R5IFNRRTEZMBcGA1UEAxMQUEtDUzEyIFRlc3QgTGVhZDAeFw0wMDA4MDk\n"
            + "wNzAwMDBaFw0xMDA4MDcwNzAwMDBaMFgxCzAJBgNVBAYTAlVTMREwDwYD\n"
            + "VQQKEwhKYXZhU29mdDEVMBMGA1UECxMMU2VjdXJpdHkgU1FFMR8wHQYDV\n"
            + "QQDExZQS0NTMTIgVGVzdCBFbmQgVXNlciAxMIGfMA0GCSqGSIb3DQEBAQ\n"
            + "UAA4GNADCBiQKBgQDIKomSYomDzH/V63eDQEG7od0DLcnnVZ81pbWhDss\n"
            + "8gHV2m8pADdRqdihBmnSQEaMW4D3uZ4sFE1LtkQls6hjd7SdOsG5Y24L8\n"
            + "15jot9a2JcB73H8H0VKirrObL5BZdt7BtASPDnYtW4Spt++YjDoJFxyF0\n"
            + "HchkavzXaVTlexakwIDAQABoxMwETAPBgNVHQ8BAf8EBQMDB4AAMA0GCS\n"
            + "qGSIb3DQEBBAUAA4GBAIFA3JXEmb9AXn3RD7t+Mn6DoyVDIy5jsn6xOKT\n"
            + "JV25I0obpDUzgw4QaAMmM0ZvusOmZ2wZNS8MtyTUgdANyakbzn5SdxbTy\n"
            + "TLEqQsFbX8UVC38fx5ZM6ExA5YSAvgmXudZpOVC0ATccoZS3JFU8CxSfW\n"
            + "+Q3IC2MLh+QTg3hUJ5b\n-----END CERTIFICATE-----";

    private final Certificate testerCert;
    private final Certificate testLeadCert;
    private final Certificate caCert;

    WriteP12Test() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        caCert = cf.generateCertificate(new ByteArrayInputStream(CA_CERT_STR
                .getBytes()));
        testLeadCert = cf.generateCertificate(new ByteArrayInputStream(
                LEAD_CERT.getBytes()));
        testerCert = cf.generateCertificate(new ByteArrayInputStream(END_CERT
                .getBytes()));
    }

    public static void main(String[] args) throws CertificateException,
            UnrecoverableKeyException, KeyStoreException,
            NoSuchProviderException, NoSuchAlgorithmException, IOException {
        WriteP12Test jstest = new WriteP12Test();
        out.println("test WriteP12CertChain");
        /*
         * WriteP12CertChain: This test creates a p12 keystore contains one
         * entry with private key and a certificate chains contains three
         * certificates in the order of user->lead->ca. This case expects to
         * pass.
         */
        jstest.test(new Certificate[] { jstest.testerCert, jstest.testLeadCert,
                jstest.caCert }, IN_KEYSTORE_ENDUSER, "pkcs12testenduser1",
                "pass", "pass");

        /*
         * WriteP12CertChainBad: same as WriteP12CertChain but chains order is
         * user-ca-lead, the order is wrong so expects to fail.
         */
        out.println("test WriteP12CertChainBad");
        try {
            jstest.test(new Certificate[] { jstest.testerCert, jstest.caCert,
                    jstest.testLeadCert }, IN_KEYSTORE_ENDUSER,
                    "pkcs12testenduser1", "pass", "pass");
            throw new RuntimeException(
                    " Certificate chain is not valid, test should not pass."
                            + " Test failed.");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            out.println(" Certificate chain is not valid,exception is"
                    + " expected. Test passed.");
        }
        /*
         * WriteP12PrivateKey:This test creates a p12 contains a self-signed
         * cert and private key,expects no exception
         */
        out.println("test WriteP12PrivateKey");
        jstest.test(null, IN_KEYSTORE_ENDUSER, "pkcs12testenduser1", "pass",
                "pass");

        /*
         * WriteP12TwoEntry: This test creates a p12 keystore with different
         * storepass and keypass, and contains two entries.
         */
        out.println("test WriteP12TwoEntry");
        jstest.testTwoEntry(IN_KEYSTORE_ENDUSER, IN_KEYSTORE_CA,
                "pkcs12testenduser1", "pass", "pass");
        /*
         * WriteP12TwoPass: This test creates a p12 keystore with different
         * storepass and keypass, and contains one entry with private key and a
         * certificate
         */
        out.println("test WriteP12TwoPass");
        jstest.test(null, IN_KEYSTORE_CA, "pkcs12testCA", "storepass",
                "keypass");
    }

    private void test(Certificate certs[], String inKeyStorePath,
            String userAlias, String outStorePass, String outKeyPass)
            throws KeyStoreException, NoSuchProviderException, IOException,
            CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException {
        // init output key store
        KeyStore outputKeyStore = KeyStore.getInstance("pkcs12", "SunJSSE");
        outputKeyStore.load(null, null);
        try (FileOutputStream fout = new FileOutputStream(OUT_KEYSTORE)) {
            // KeyStore have encoded by Base64.getMimeEncoder().encode(),need
            // decode first.
            byte[] input = Files.readAllBytes(Paths.get(CERT_PATH,
                    inKeyStorePath));
            ByteArrayInputStream arrayIn = new ByteArrayInputStream(Base64
                    .getMimeDecoder().decode(input));
            // input key store
            KeyStore inputKeyStore = KeyStore.getInstance(IN_KEYSTORE_TYPE,
                    IN_KEYSTORE_PRV);
            inputKeyStore.load(arrayIn, IN_STORE_PASS.toCharArray());
            // add key/certificate to output key store
            Key key = inputKeyStore
                    .getKey(userAlias, IN_KEY_PASS.toCharArray());
            out.println("Input Key Algorithm " + key.getAlgorithm());
            out.println("====Input Certs=====");
            if (certs == null) {
                certs = new Certificate[] { inputKeyStore
                        .getCertificate(userAlias) };
            }
            for (Certificate cert : certs) {
                out.println(((X509Certificate) cert).getSubjectDN());
            }
            outputKeyStore.setKeyEntry(userAlias, key,
                    outKeyPass.toCharArray(), certs);
            Certificate retCerts[] = outputKeyStore
                    .getCertificateChain(userAlias);
            out.println("====Output Certs=====");
            for (Certificate retCert : retCerts) {
                out.println(((X509Certificate) retCert).getSubjectDN());
            }
            out.println("====Output Key Algorithm=====");
            Key outKey = outputKeyStore.getKey(userAlias,
                    outKeyPass.toCharArray());
            out.println(outKey.getAlgorithm());

            if (!key.equals(outKey)) {
                throw new RuntimeException("key don't match");
            }
            if (!Arrays.equals(certs, retCerts)) {
                throw new RuntimeException("certs don't match");
            }
            // save output
            outputKeyStore.store(fout, outStorePass.toCharArray());
            // test output
            testKeyStore(outputKeyStore, outKeyPass.toCharArray());
        }
    }

    private void testTwoEntry(String inKeyStoreOnePath,
            String inKeyStoreTwoPath, String userAlias, String outStorePass,
            String outKeyPass) throws KeyStoreException,
            NoSuchProviderException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException {
        // initial KeyStore
        KeyStore outputKeyStore = KeyStore.getInstance("pkcs12", "SunJSSE");
        try (FileOutputStream fout = new FileOutputStream(OUT_KEYSTORE);) {
            outputKeyStore.load(null, null);
            KeyStore inputKeyStoreOne, inputKeyStoreTwo;
            inputKeyStoreOne = KeyStore.getInstance(IN_KEYSTORE_TYPE,
                    IN_KEYSTORE_PRV);
            // KeyStore have encoded by Base64.getMimeEncoder().encode(),need
            // decode first.
            byte[] inputBytes = Files.readAllBytes(Paths.get(CERT_PATH,
                    inKeyStoreOnePath));
            ByteArrayInputStream arrayIn = new ByteArrayInputStream(Base64
                    .getMimeDecoder().decode(inputBytes));
            // input key store
            inputKeyStoreOne.load(arrayIn, IN_STORE_PASS.toCharArray());

            inputBytes = Files.readAllBytes(Paths.get(CERT_PATH,
                    inKeyStoreTwoPath));
            arrayIn = new ByteArrayInputStream(Base64.getMimeDecoder().decode(
                    inputBytes));
            inputKeyStoreTwo = KeyStore.getInstance(IN_KEYSTORE_TYPE,
                    IN_KEYSTORE_PRV);
            inputKeyStoreTwo.load(arrayIn, IN_STORE_PASS.toCharArray());

            // add key/certificate to output key store
            out.println("====First Entry=====");
            Key inputKey = inputKeyStoreOne.getKey(userAlias,
                    IN_KEY_PASS.toCharArray());
            Certificate cert = inputKeyStoreOne.getCertificate(userAlias);
            Certificate certs[] = new Certificate[1];
            certs[0] = cert;

            out.println("====Input1 Key=====");
            out.println(inputKey.getAlgorithm());
            out.println("====Input1 Certs=====");
            out.println("Certificate :");
            out.println(((X509Certificate) cert).getSubjectDN());
            outputKeyStore.setKeyEntry("USER", inputKey,
                    outKeyPass.toCharArray(), certs);
            out.println("====Second Entry=====");
            String caAlias = "pkcs12testca";
            inputKey = inputKeyStoreTwo.getKey(caAlias,
                    IN_KEY_PASS.toCharArray());
            cert = inputKeyStoreTwo.getCertificate(caAlias);
            certs[0] = cert;
            out.println("====Input2 Key=====");
            out.println(inputKey.getAlgorithm());
            out.println("====Input2 Certs=====");
            out.println("Certificate :");
            out.println(((X509Certificate) cert).getSubjectDN());
            outputKeyStore.setKeyEntry("CA", inputKey,
                    outKeyPass.toCharArray(), certs);
            // save output
            outputKeyStore.store(fout, outStorePass.toCharArray());
            // test output
            testKeyStore(outputKeyStore, outKeyPass.toCharArray());
        }
    }

    private void testKeyStore(KeyStore inputKeyStore, char[] keypass)
            throws KeyStoreException, UnrecoverableKeyException,
            NoSuchAlgorithmException {
        out.println("========== Key Store ==========");
        out.println("getProvider : " + inputKeyStore.getProvider());
        out.println("getType : " + inputKeyStore.getType());
        out.println("getDefaultType : " + KeyStore.getDefaultType());

        int idx = 0;
        Enumeration<String> e = inputKeyStore.aliases();
        String alias;
        while (e.hasMoreElements()) {
            alias = e.nextElement();
            if (!inputKeyStore.containsAlias(alias)) {
                throw new RuntimeException("Alias not found");
            }
            out.println("Alias " + idx + " : " + alias);
            out.println("getCreationDate : "
                    + inputKeyStore.getCreationDate(alias));
            X509Certificate cert = (X509Certificate) inputKeyStore
                    .getCertificate(alias);
            out.println("getCertificate : " + cert.getSubjectDN());
            String retAlias = inputKeyStore.getCertificateAlias(cert);
            if (!retAlias.equals(alias)) {
                throw new RuntimeException("Alias mismatch, actually "
                        + retAlias + ", expected " + alias);
            }
            out.println("getCertificateAlias : " + retAlias);
            Certificate[] certs = inputKeyStore.getCertificateChain(alias);
            int i = 0;
            for (Certificate certification : certs) {
                out.println("getCertificateChain " + i
                        + ((X509Certificate) certification).getSubjectDN());
                i++;
            }
            if (inputKeyStore.isCertificateEntry(alias)) {
                throw new RuntimeException(
                        "inputKeystore should not be certEntry because this"
                                + " keystore only contain key pair entries.");
            }
            if (!inputKeyStore.isKeyEntry(alias)) {
                throw new RuntimeException("Entry type unknown.");
            }
            idx++;
        }
        int size = inputKeyStore.size();
        if (idx != size) {
            throw new RuntimeException("Size not match, actually " + idx
                    + ", expected " + size);
        }
    }

}
