/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.*;
import java.util.*;
import javax.crypto.SecretKey;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.keyinfo.*;
import sun.security.util.DerValue;
import sun.security.x509.X500Name;

/**
 * This is a class which supplies several KeySelector implementations
 */
class KeySelectors {

    /**
     * KeySelector which would always return the secret key specified in its
     * constructor.
     */
    static class SecretKeySelector extends KeySelector {
        private SecretKey key;
        SecretKeySelector(byte[] bytes) {
            key = wrapBytes(bytes);
        }
        SecretKeySelector(SecretKey key) {
            this.key = key;
        }

        public KeySelectorResult select(KeyInfo ki,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
            throws KeySelectorException {
            return new SimpleKSResult(key);
        }

        private SecretKey wrapBytes(final byte[] bytes) {
            return new SecretKey() {
                public String getFormat() {
                    return "RAW";
                }

                public String getAlgorithm() {
                    return "Secret key";
                }

                public byte[] getEncoded() {
                    return bytes.clone();
                }
            };
        }
    }

    /**
     * KeySelector which would retrieve the X509Certificate out of the
     * KeyInfo element and return the public key.
     * NOTE: If there is an X509CRL in the KeyInfo element, then revoked
     * certificate will be ignored.
     */
    static class RawX509KeySelector extends KeySelector {

        public KeySelectorResult select(KeyInfo keyInfo,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
            throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            // search for X509Data in keyinfo
            for (XMLStructure kiType : keyInfo.getContent()) {
                if (kiType instanceof X509Data) {
                    X509Data xd = (X509Data) kiType;
                    Object[] entries = xd.getContent().toArray();
                    X509CRL crl = null;
                    // Looking for CRL before finding certificates
                    for (int i = 0; (i<entries.length&&crl != null); i++) {
                        if (entries[i] instanceof X509CRL) {
                            crl = (X509CRL) entries[i];
                        }
                    }
                    boolean hasCRL = false;
                    for (Object o : xd.getContent()) {
                        // skip non-X509Certificate entries
                        if (o instanceof X509Certificate) {
                            if ((purpose != KeySelector.Purpose.VERIFY) &&
                                (crl != null) &&
                                crl.isRevoked((X509Certificate)o)) {
                                continue;
                            } else {
                                return new SimpleKSResult
                                    (((X509Certificate)o).getPublicKey());
                            }
                        }
                    }
                }
            }
            throw new KeySelectorException("No X509Certificate found!");
        }
    }

    /**
     * KeySelector which would retrieve the public key out of the
     * KeyValue element and return it.
     * NOTE: If the key algorithm doesn't match signature algorithm,
     * then the public key will be ignored.
     */
    static class KeyValueKeySelector extends KeySelector {
        public KeySelectorResult select(KeyInfo keyInfo,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
            throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            SignatureMethod sm = (SignatureMethod) method;

            for (XMLStructure xmlStructure : keyInfo.getContent()) {
                if (xmlStructure instanceof KeyValue) {
                    PublicKey pk = null;
                    try {
                        pk = ((KeyValue)xmlStructure).getPublicKey();
                    } catch (KeyException ke) {
                        throw new KeySelectorException(ke);
                    }
                    // make sure algorithm is compatible with method
                    if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                        return new SimpleKSResult(pk);
                    }
                }
            }
            throw new KeySelectorException("No KeyValue element found!");
        }

        static boolean algEquals(String algURI, String algName) {
            algName = algName.toUpperCase(Locale.ROOT);
            return algName.equals("DSA") && algURI.contains("#dsa-")
                    || algName.equals("RSA")
                            && (algURI.contains("#rsa-") || algURI.contains("-rsa-MGF1"))
                    || algName.equals("EC") && algURI.contains("#ecdsa-");
        }
    }

    /**
     * KeySelector which would perform special lookup as documented
     * by the ie/baltimore/merlin-examples testcases and return the
     * matching public key.
     */
    static class CollectionKeySelector extends KeySelector {
        private CertificateFactory cf;
        private File certDir;
        private Vector<X509Certificate> certs;
        private static final int MATCH_SUBJECT = 0;
        private static final int MATCH_ISSUER = 1;
        private static final int MATCH_SERIAL = 2;
        private static final int MATCH_SUBJECT_KEY_ID = 3;
        private static final int MATCH_CERTIFICATE = 4;

        CollectionKeySelector(File dir) {
            certDir = dir;
            try {
                cf = CertificateFactory.getInstance("X509");
            } catch (CertificateException ex) {
                // not going to happen
            }
            certs = new Vector<X509Certificate>();
            File[] files = new File(certDir, "certs").listFiles();
            for (int i = 0; i < files.length; i++) {
                try (FileInputStream fis = new FileInputStream(files[i])) {
                    certs.add((X509Certificate)cf.generateCertificate(fis));
                } catch (Exception ex) { }
            }
        }

        Vector<X509Certificate> match(int matchType, Object value,
                                      Vector<X509Certificate> pool) {
            Vector<X509Certificate> matchResult = new Vector<>();
            for (int j=0; j < pool.size(); j++) {
                X509Certificate c = pool.get(j);
                switch (matchType) {
                case MATCH_SUBJECT:
                    try {
                        if (c.getSubjectDN().equals(new X500Name((String)value))) {
                            matchResult.add(c);
                        }
                    } catch (IOException ioe) { }
                    break;
                case MATCH_ISSUER:
                    try {
                        if (c.getIssuerDN().equals(new X500Name((String)value))) {
                            matchResult.add(c);
                        }
                    } catch (IOException ioe) { }
                    break;
                case MATCH_SERIAL:
                    if (c.getSerialNumber().equals(value)) {
                        matchResult.add(c);
                    }

                    break;
                case MATCH_SUBJECT_KEY_ID:
                    byte[] extension = c.getExtensionValue("2.5.29.14");
                    if (extension != null) {
                        try {
                            DerValue derValue = new DerValue(extension);
                            DerValue derValue2 = new DerValue(derValue.getOctetString());
                            byte[] extVal = derValue2.getOctetString();

                            if (Arrays.equals(extVal, (byte[]) value)) {
                                matchResult.add(c);
                            }
                        } catch (IOException ex) { }
                    }
                    break;
                case MATCH_CERTIFICATE:
                    if (c.equals(value)) {
                        matchResult.add(c);
                    }
                    break;
                }
            }
            return matchResult;
        }

        public KeySelectorResult select(KeyInfo keyInfo,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
            throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            for (XMLStructure xmlStructure : keyInfo.getContent()) {
                try {
                    if (xmlStructure instanceof KeyName) {
                        String name = ((KeyName)xmlStructure).getName();
                        PublicKey pk = null;
                        File certFile = new File(new File(certDir, "certs"),
                                                 name.toLowerCase() + ".crt");
                        try (FileInputStream fis = new FileInputStream(certFile)) {
                            // Lookup the public key using the key name 'Xxx',
                            // i.e. the public key is in "certs/xxx.crt".
                            X509Certificate cert = (X509Certificate)
                                cf.generateCertificate(fis);
                            pk = cert.getPublicKey();
                        } catch (FileNotFoundException e) {
                            // assume KeyName contains subject DN and search
                            // collection of certs for match
                            Vector<X509Certificate> result =
                                match(MATCH_SUBJECT, name, certs);
                            int numOfMatches = (result==null? 0:result.size());
                            if (numOfMatches != 1) {
                                throw new KeySelectorException
                                    ((numOfMatches==0?"No":"More than one") +
                                     " match found");
                            }
                            pk = result.get(0).getPublicKey();
                        }
                        return new SimpleKSResult(pk);
                    } else if (xmlStructure instanceof RetrievalMethod) {
                        // Lookup the public key using the retrievel method.
                        // NOTE: only X509Certificate type is supported.
                        RetrievalMethod rm = (RetrievalMethod) xmlStructure;
                        String type = rm.getType();
                        if (type.equals(X509Data.RAW_X509_CERTIFICATE_TYPE)) {
                            String uri = rm.getURI();
                            try (FileInputStream fis =
                                 new FileInputStream(new File(certDir, uri))) {
                                X509Certificate cert = (X509Certificate)
                                    cf.generateCertificate(fis);
                                return new SimpleKSResult(cert.getPublicKey());
                            }
                        } else {
                            throw new KeySelectorException
                                ("Unsupported RetrievalMethod type");
                        }
                    } else if (xmlStructure instanceof X509Data) {
                        List content = ((X509Data)xmlStructure).getContent();
                        Vector<X509Certificate> result = null;
                        // Lookup the public key using the information
                        // specified in X509Data element, i.e. searching
                        // over the collection of certificate files under
                        // "certs" subdirectory and return those match.
                        for (Object obj : content) {
                            if (obj instanceof String) {
                                result = match(MATCH_SUBJECT, obj, certs);
                            } else if (obj instanceof byte[]) {
                                result = match(MATCH_SUBJECT_KEY_ID, obj,
                                               certs);
                            } else if (obj instanceof X509Certificate) {
                                result = match(MATCH_CERTIFICATE, obj, certs);
                            } else if (obj instanceof X509IssuerSerial) {
                                X509IssuerSerial is = (X509IssuerSerial) obj;
                                result = match(MATCH_SERIAL,
                                               is.getSerialNumber(), certs);
                                result = match(MATCH_ISSUER,
                                               is.getIssuerName(), result);
                            } else {
                                throw new KeySelectorException("Unsupported X509Data: " + obj);
                            }
                        }
                        int numOfMatches = (result==null? 0:result.size());
                        if (numOfMatches != 1) {
                            throw new KeySelectorException
                                ((numOfMatches==0?"No":"More than one") +
                                 " match found");
                        }
                        return new SimpleKSResult(result.get(0).getPublicKey());
                    }
                } catch (Exception ex) {
                    throw new KeySelectorException(ex);
                }
            }
            throw new KeySelectorException("No matching key found!");
        }
    }

    static class ByteUtil {

        private static String mapping = "0123456789ABCDEF";
        private static int numBytesPerRow = 6;

        private static String getHex(byte value) {
            int low = value & 0x0f;
            int high = ((value >> 4) & 0x0f);
            char[] res = new char[2];
            res[0] = mapping.charAt(high);
            res[1] = mapping.charAt(low);
            return new String(res);
        }

        static String dumpArray(byte[] in) {
            int numDumped = 0;
            StringBuffer buf = new StringBuffer(512);
            buf.append("{");
            for (int i=0;i<(in.length/numBytesPerRow); i++) {
                for (int j=0; j<(numBytesPerRow); j++) {
                    buf.append("(byte)0x" + getHex(in[i*numBytesPerRow+j]) +
                               ", ");
                }
                numDumped += numBytesPerRow;
            }
            while (numDumped < in.length) {
                buf.append("(byte)0x" + getHex(in[numDumped]) + " ");
                numDumped += 1;
            }
            buf.append("}");
            return buf.toString();
        }
    }
}

class SimpleKSResult implements KeySelectorResult {
    private final Key key;

    SimpleKSResult(Key key) { this.key = key; }

    public Key getKey() { return key; }
}
