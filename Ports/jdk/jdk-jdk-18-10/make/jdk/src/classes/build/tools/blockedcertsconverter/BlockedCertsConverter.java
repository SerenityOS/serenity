/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.blockedcertsconverter;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * Converts blocked.certs.pem from System.in to blocked.certs in
 * System.out. The input must start with a #! line including the fingerprint
 * algorithm. The output is sorted and unique.
 */
public class BlockedCertsConverter {

    public static void main(String[] args) throws Exception {

        byte[] pattern = "#! java BlockedCertsConverter ".getBytes();
        String mdAlg = "";

        for (int i=0; ; i++) {
            int n = System.in.read();
            if (n < 0) {
                throw new Exception("Unexpected EOF");
            }
            if (i < pattern.length) {
                if (n != pattern[i]) {
                    throw new Exception("The first line must start with \""
                            + new String(pattern) + "\"");
                }
            } else if (i < pattern.length + 100) {
                if (n < 32) {
                    break;
                } else {
                    mdAlg = mdAlg + String.format("%c", n);
                }
            }
        }

        mdAlg = mdAlg.trim();
        System.out.println("Algorithm=" + mdAlg);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs
                = cf.generateCertificates(System.in);

        // Output sorted so that it's easy to locate an entry.
        Set<String> fingerprints = new TreeSet<>();
        for (Certificate cert: certs) {
            fingerprints.addAll(
                getCertificateFingerPrints(mdAlg, (X509Certificate)cert));
        }

        for (String s: fingerprints) {
            System.out.println(s);
        }
    }

    /**
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /**
     * Computes the possible fingerprints of the certificate.
     */
    private static List<String> getCertificateFingerPrints(
            String mdAlg, X509Certificate cert) throws Exception {
        List<String> fingerprints = new ArrayList<>();
        for (byte[] encoding : altEncodings(cert)) {
            MessageDigest md = MessageDigest.getInstance(mdAlg);
            byte[] digest = md.digest(encoding);
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                byte2hex(digest[i], buf);
            }
            fingerprints.add(buf.toString());
        }
        return fingerprints;
    }

    private static List<byte[]> altEncodings(X509Certificate c)
            throws Exception {
        List<byte[]> result = new ArrayList<>();

        DerValue d = new DerValue(c.getEncoded());
        DerValue[] seq = new DerValue[3];
        // tbsCertificate
        seq[0] = d.data.getDerValue();
        // signatureAlgorithm
        seq[1] = d.data.getDerValue();
        // signature
        seq[2] = d.data.getDerValue();

        List<DerValue> algIds = Arrays.asList(seq[1], altAlgId(seq[1]));

        List<DerValue> sigs;
        PublicKey p = c.getPublicKey();
        if (p instanceof ECPublicKey) {
            ECPublicKey ep = (ECPublicKey) p;
            BigInteger mod = ep.getParams().getOrder();
            sigs = Arrays.asList(seq[2], altSig(mod, seq[2]));
        } else {
            sigs = Arrays.asList(seq[2]);
        }

        for (DerValue algId : algIds) {
            for (DerValue sig : sigs) {
                DerOutputStream tmp = new DerOutputStream();
                tmp.putDerValue(seq[0]);
                tmp.putDerValue(algId);
                tmp.putDerValue(sig);
                DerOutputStream tmp2 = new DerOutputStream();
                tmp2.write(DerValue.tag_Sequence, tmp);
                result.add(tmp2.toByteArray());
            }
        }
        return result;
    }

    private static DerValue altSig(BigInteger mod, DerValue sig)
            throws IOException {
        byte[] sigBits = sig.getBitString();
        DerInputStream in =
            new DerInputStream(sigBits, 0, sigBits.length, false);
        DerValue[] values = in.getSequence(2);
        BigInteger r = values[0].getBigInteger();
        BigInteger s = values[1].getBigInteger();
        BigInteger s2 = s.negate().mod(mod);
        DerOutputStream out = new DerOutputStream();
        out.putInteger(r);
        out.putInteger(s2);
        DerOutputStream tmp = new DerOutputStream();
        tmp.putBitString(new DerValue(DerValue.tag_Sequence,
                out.toByteArray()).toByteArray());
        return new DerValue(tmp.toByteArray());
    }

    private static DerValue altAlgId(DerValue algId) throws IOException {
        DerInputStream in = algId.toDerInputStream();
        DerOutputStream bytes = new DerOutputStream();
        bytes.putOID(in.getOID());
        // encode parameters as NULL if not present or omit if NULL
        if (in.available() == 0) {
            bytes.putNull();
        }
        DerOutputStream tmp = new DerOutputStream();
        tmp.write(DerValue.tag_Sequence, bytes);
        return new DerValue(tmp.toByteArray());
    }
}
