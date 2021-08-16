/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4856966 8023980
 * @summary Test KeyFactory of the new RSA provider
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestKeyFactory
 * @run main/othervm -Djava.security.manager=allow TestKeyFactory sm rsakeys.ks.policy
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.spec.*;

public class TestKeyFactory extends PKCS11Test {

    private static final char[] password = "test12".toCharArray();

    private static final String PKCS1_PRIV_STR =
        // the BASE64 string between -----BEGIN RSA PRIVATE KEY-----
        // and -----END RSA PRIVATE KEY-----
        "MIIEowIBAAKCAQEA0OIArlYES4X1XMTLDordtN/XIWFE1wvhl40RsHWM2n99+Stp" +
        "CCJCcUb5FJ2/kefj/XRwB6p5IMpIZrHZqC8XXzlX5fpiFaSu2xnk17oWUKoErW27" +
        "Stm098pU2RoUxWPKVl+42a8iVp8tijNElBNFALCGi0zXOhcTxMh0q1Wk0UhMJqam" +
        "v5YnCKmT4THwwGYn/KeK3M7Qa+o5MoVBHLbeT9LJgEmSluVzIh44Lh6weX0bw72P" +
        "8X2praOhbzg2B343MqS/rMLw6On+0i7ccEgp23vX9G5w85q4A5FSIrk4S/pyv5sO" +
        "rwjCQKBW1TS0/2iB9zNkFMj5/+h7l2oqTT7sSQIDAQABAoIBADn6sXOynoiUC1IP" +
        "sck8lGOTSjSSujfyrVCSsJlJV6qCfuX9va6rS8QDjjnBu531PtxoSHxoPizy2Pvg" +
        "W+kKATPGR/am9DjLuFlKq7GRjoYfWyMEdVtGaKvq9ng4fBF6LHyjHz0VFrPyhQJ6" +
        "TovHeXzCguYBkzAlnbAeb/vqzs/kABbOuSHVi7DsaixCoEX9zOptFYQw/l8rh68+" +
        "UF2bpNNH3jOC1uN3vZtuSwCupqtN+2Mpkx2h04Rk75vWIhrnPeMgmcd3yP4LNZMR" +
        "mfaynb63RRzVkNis7+NVk016SQ1oL79mrBvy5rBg3HeCeArwvqZAmOaWsLSWHzCy" +
        "zlVlMTECgYEA6JlnMpC956Qi8HX5ye4Hu2ovBdbNGtH/TMkZmColJz9P7CvNkNIb" +
        "Od6mvLMydbPHkhdBUDWD4rhiCKHrf5zKju1i24YqWcvuSGotWj4/KQ3+87mLZM+7" +
        "daBsJBmSEVB80sgA9ItqSgOyNoNFpiDgFnlszAfb0n9XXEzB/pwSw1UCgYEA5eXI" +
        "d+eKugugP+n6CluQfyxfN6WWCzfqWToCTTxPn2i12AiEssXy+kyLjupJVLWSivdo" +
        "83wD5LuxFRGc9P+aKQERPhb0AFaxf1llUCXla65/x2So5xjMvtuzgQ0OktPJqJXq" +
        "hYGunctsr5rje33+7vlx4xWkrL2PrQWzJabn7SUCgYEAqw3FesY/Ik7u8u+P1xSZ" +
        "0xXvptek1oiAu7NYgzLbR9WjrQc5kbsyEojPDg6qmSyxI5q+iYIRj3YRgk+xpJNl" +
        "0154SQCNvKPghJiw6aDFSifkytA01tp9/a8QWCwF433RjiFPsoekjvHQ6Y34dofO" +
        "xDhf7lwJKPBFCrfYIqocklECgYAIPI9OHHGP8NKw94UJ0fX/WGug5sHVbQ9sWvOy" +
        "KLMBlxLMxqFadlUaOpvVZvdxnX++ktajwpGxJDhX9OWWsYGobm1buB7N1E1Prrg+" +
        "gt0RWpMhZa3Xeb/8Jorr2Lfo8sWK0LQyTE8hQCSIthfoWL9FeJJn/GKF/dSj8kxU" +
        "0QIGMQKBgG/8U/zZ87DzfXS81P1p+CmH474wmou4KD2/zXp/lDR9+dlIUeijlIbU" +
        "P6Y5xJvT33Y40giW9irShgDHjZgw0ap11K3b2HzLImdPEaBiENo735rpLs8WLK9H" +
        "+yeRbiP2y9To7sTihm9Jrkctzp6sqFtKyye1+S21X1tMz8NGfXen";

    private static final String PKCS1_PUB_STR =
        // the BASE64 string between -----BEGIN RSA PUBLIC KEY-----
        // and -----END RSA PUBLIC KEY-----
        "MIIBCgKCAQEA0OIArlYES4X1XMTLDordtN/XIWFE1wvhl40RsHWM2n99+StpCCJC" +
        "cUb5FJ2/kefj/XRwB6p5IMpIZrHZqC8XXzlX5fpiFaSu2xnk17oWUKoErW27Stm0" +
        "98pU2RoUxWPKVl+42a8iVp8tijNElBNFALCGi0zXOhcTxMh0q1Wk0UhMJqamv5Yn" +
        "CKmT4THwwGYn/KeK3M7Qa+o5MoVBHLbeT9LJgEmSluVzIh44Lh6weX0bw72P8X2p" +
        "raOhbzg2B343MqS/rMLw6On+0i7ccEgp23vX9G5w85q4A5FSIrk4S/pyv5sOrwjC" +
        "QKBW1TS0/2iB9zNkFMj5/+h7l2oqTT7sSQIDAQAB";


    private static final PrivateKey CUSTOM_PRIV;
    private static final PublicKey CUSTOM_PUB;

    static {
        byte[] encodedPriv = Base64.getDecoder().decode(PKCS1_PRIV_STR);
        CUSTOM_PRIV = new PrivateKey() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }
            @Override
            public String getFormat() {
                return "PKCS#1";
            }
            @Override
            public byte[] getEncoded() {
                return encodedPriv.clone();
            }
        };
        byte[] encodedPub = Base64.getDecoder().decode(PKCS1_PUB_STR);
        CUSTOM_PUB = new PublicKey() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }
            @Override
            public String getFormat() {
                return "PKCS#1";
            }
            @Override
            public byte[] getEncoded() {
                return encodedPub.clone();
            }
        };
    }

    static KeyStore getKeyStore() throws Exception {
        KeyStore ks;
        try (InputStream in = new FileInputStream(new File(BASE, "rsakeys.ks"))) {
            ks = KeyStore.getInstance("JKS");
            ks.load(in, password);
        }
        return ks;
    }

    /**
     * Test that key1 (reference key) and key2 (key to be tested) are
     * equivalent
     */
    private static void testKey(Key key1, Key key2) throws Exception {
        if (key2.getAlgorithm().equals("RSA") == false) {
            throw new Exception("Algorithm not RSA");
        }
        if (key1 instanceof PublicKey) {
            if (key2.getFormat().equals("X.509") == false) {
                throw new Exception("Format not X.509");
            }
        } else if (key1 instanceof PrivateKey) {
            if (key2.getFormat().equals("PKCS#8") == false) {
                throw new Exception("Format not PKCS#8");
            }
        }
        // skip equals check when key1 is custom key
        if (key1 != CUSTOM_PRIV && key1 != CUSTOM_PUB) {
            if (!key1.equals(key2)) {
                throw new Exception("Keys not equal");
            }
        }
        // only compare encodings if keys are of the same format
        if (key1.getFormat().equals(key2.getFormat()) &&
            !Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            throw new Exception("Encodings not equal");
        }
    }

    private static void testPublic(KeyFactory kf, PublicKey key)
            throws Exception {
        System.out.println("Testing " + (key == CUSTOM_PUB? "PKCS#1" : "") +
            " public key...");
        PublicKey key2 = (PublicKey)kf.translateKey(key);
        KeySpec rsaSpec = kf.getKeySpec(key, RSAPublicKeySpec.class);
        PublicKey key3 = kf.generatePublic(rsaSpec);
        KeySpec x509Spec = kf.getKeySpec(key, X509EncodedKeySpec.class);
        PublicKey key4 = kf.generatePublic(x509Spec);
        if (key != CUSTOM_PUB) {
            testKey(key, key);
        }
        testKey(key, key2);
        testKey(key, key3);
        testKey(key, key4);

        if (key.getFormat().equalsIgnoreCase("X.509")) {
            KeySpec x509Spec2 = new X509EncodedKeySpec(key.getEncoded());
            PublicKey key5 = kf.generatePublic(x509Spec2);
            testKey(key, key5);
        }

    }

    private static void testPrivate(KeyFactory kf, PrivateKey key)
            throws Exception {
        System.out.println("Testing " + (key == CUSTOM_PRIV? "PKCS#1" : "") +
            " private key...");
        PrivateKey key2 = (PrivateKey)kf.translateKey(key);
        KeySpec rsaSpec = kf.getKeySpec(key, RSAPrivateCrtKeySpec.class);
        PrivateKey key3 = kf.generatePrivate(rsaSpec);
        KeySpec pkcs8Spec = kf.getKeySpec(key, PKCS8EncodedKeySpec.class);
        PrivateKey key4 = kf.generatePrivate(pkcs8Spec);
        if (key != CUSTOM_PRIV) {
            testKey(key, key);
        }
        testKey(key, key2);
        testKey(key, key3);
        testKey(key, key4);

        if (key.getFormat().equalsIgnoreCase("PKCS#8")) {
            KeySpec pkcs8Spec2 = new PKCS8EncodedKeySpec(key.getEncoded());
            PrivateKey key5 = kf.generatePrivate(pkcs8Spec2);
            testKey(key, key5);
        }

        // XXX PKCS#11 providers may not support non-CRT keys (e.g. NSS)
//      KeySpec rsaSpec2 = kf.getKeySpec(key, RSAPrivateKeySpec.class);
//      PrivateKey key6 = kf.generatePrivate(rsaSpec2);
//      RSAPrivateKey rsaKey = (RSAPrivateKey)key;
//      KeySpec rsaSpec3 = new RSAPrivateKeySpec(rsaKey.getModulus(), rsaKey.getPrivateExponent());
//      PrivateKey key7 = kf.generatePrivate(rsaSpec3);
//      testKey(key6, key6);
//      testKey(key6, key7);
    }

    private static void test(KeyFactory kf, Key key) throws Exception {
        if (key.getAlgorithm().equals("RSA") == false) {
            System.out.println("Not an RSA key, ignoring");
        }
        if (key instanceof PublicKey) {
            testPublic(kf, (PublicKey)key);
        } else if (key instanceof PrivateKey) {
            testPrivate(kf, (PrivateKey)key);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestKeyFactory(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        long start = System.currentTimeMillis();
        KeyStore ks = getKeyStore();
        KeyFactory kf = KeyFactory.getInstance("RSA", p);
        for (Enumeration e = ks.aliases(); e.hasMoreElements(); ) {
            String alias = (String)e.nextElement();
            Key key = null;
            if (ks.isKeyEntry(alias)) {
                test(kf, ks.getKey(alias, password));
                test(kf, ks.getCertificate(alias).getPublicKey());
            }
        }
        // repeat the test w/ PKCS#1 RSA Private Key
        test(kf, CUSTOM_PRIV);
        test(kf, CUSTOM_PUB);

        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }
}
