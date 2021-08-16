/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6405536
 * @summary Basic known answer test for ECDH
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @library ../../../../java/security/testlibrary
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestECDH
 * @run main/othervm -Djava.security.manager=allow TestECDH sm policy
 */

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KeyAgreement;

public class TestECDH extends PKCS11Test {

    private final static String pub192a  = "30:49:30:13:06:07:2a:86:48:ce:3d:02:01:06:08:2a:86:48:ce:3d:03:01:01:03:32:00:04:bc:49:85:81:4d:d0:a4:ef:67:09:f1:9f:f5:ee:ff:4c:2f:0e:74:2c:a0:98:a8:69:79:9c:0c:3c:e8:99:f2:f2:3c:6f:48:bf:2a:ea:45:e9:76:be:1b:4a:45:0c:a2:99";
    private final static String priv192a = "30:39:02:01:00:30:13:06:07:2a:86:48:ce:3d:02:01:06:08:2a:86:48:ce:3d:03:01:01:04:1f:30:1d:02:01:01:04:18:50:9a:f1:fb:14:91:08:91:18:b9:46:7f:c3:ff:84:db:be:4c:70:89:41:5e:5a:f5";
    private final static String pub192b  = "30:49:30:13:06:07:2a:86:48:ce:3d:02:01:06:08:2a:86:48:ce:3d:03:01:01:03:32:00:04:41:f3:1d:09:19:6e:dc:bf:6e:14:3a:b8:1a:40:44:ef:7b:51:fc:e1:9a:64:ac:46:47:ab:31:e2:1b:d3:76:d9:85:7a:b8:e6:95:f5:75:3f:13:7a:3a:88:02:57:de:8f";
    private final static String priv192b = "30:39:02:01:00:30:13:06:07:2a:86:48:ce:3d:02:01:06:08:2a:86:48:ce:3d:03:01:01:04:1f:30:1d:02:01:01:04:18:1d:8c:7d:64:1a:c1:ca:7d:59:d6:e7:11:61:e3:4d:d4:64:31:d9:76:17:a4:dd:6b";

    private final static String secret192 = "1f:48:aa:23:8e:6f:8a:70:87:af:3f:cd:53:f9:ae:85:41:1f:25:7e:b9:88:1f:6b";

    private final static String pub163a  = "30:40:30:10:06:07:2a:86:48:ce:3d:02:01:06:05:2b:81:04:00:0f:03:2c:00:04:04:81:99:2a:6d:53:e1:9a:31:4b:42:5b:01:41:bd:69:3f:73:63:f2:c5:02:70:25:7c:81:ce:6a:00:a0:fa:43:33:25:5b:ac:1f:66:82:1f:fa:63";
    private final static String priv163a = "30:33:02:01:00:30:10:06:07:2a:86:48:ce:3d:02:01:06:05:2b:81:04:00:0f:04:1c:30:1a:02:01:01:04:15:01:a0:2c:f6:24:bb:c8:2f:6e:f3:86:e2:24:bc:f1:01:ce:49:15:09:b9";
    private final static String pub163b  = "30:40:30:10:06:07:2a:86:48:ce:3d:02:01:06:05:2b:81:04:00:0f:03:2c:00:04:03:59:e7:69:a5:89:2f:28:ba:75:ac:bf:01:d5:ad:14:d8:f8:19:25:81:01:31:b3:e2:2d:f3:db:f1:d2:cd:fc:94:af:d2:1d:16:58:94:fe:d5:65";
    private final static String priv163b = "30:33:02:01:00:30:10:06:07:2a:86:48:ce:3d:02:01:06:05:2b:81:04:00:0f:04:1c:30:1a:02:01:01:04:15:02:4e:49:b1:8b:36:d8:71:22:81:06:8d:14:a9:4c:5c:7c:61:8b:e2:95";

    private final static String secret163 = "04:ae:71:c1:c6:4d:f4:34:4d:72:70:a4:64:65:7f:2d:88:2d:3f:50:be";


    // from https://tools.ietf.org/html/rfc7027#appendix-A.1
    private final static String pubBrainpoolP256r1a = "305a301406072a8648ce3d020106092b24030302080101070342000444106e913f92bc02a1705d9953a8414db95e1aaa49e81d9e85f929a8e3100be58ab4846f11caccb73ce49cbdd120f5a900a69fd32c272223f789ef10eb089bdc";
    private final static String privBrainpoolP256r1a = "3042020100301406072a8648ce3d020106092b240303020801010704273025020101042081db1ee100150ff2ea338d708271be38300cb54241d79950f77b063039804f1d";
    private final static String pubBrainpoolP256r1b = "305a301406072a8648ce3d020106092b2403030208010107034200048d2d688c6cf93e1160ad04cc4429117dc2c41825e1e9fca0addd34e6f1b39f7b990c57520812be512641e47034832106bc7d3e8dd0e4c7f1136d7006547cec6a";
    private final static String privBrainpoolP256r1b = "3042020100301406072a8648ce3d020106092b240303020801010704273025020101042055e40bc41e37e3e2ad25c3c6654511ffa8474a91a0032087593852d3e7d76bd3";
    private final static String secretBrainpoolP256r1 = "89afc39d41d3b327814b80940b042590f96556ec91e6ae7939bce31f3a18bf2b";

    // from https://tools.ietf.org/html/draft-merkle-ikev2-ke-brainpool-00#appendix-A.5
    private final static String pubBrainpoolP320r1a = "306a301406072a8648ce3d020106092b240303020801010903520004bc43666c00e4b943fe1c785dd8aa842a42ab54b0b49819f960f77694193cd3afa71b6b3c826c773469e998892c0764468023c8e3a7b8f219a1446042be175d4476b2fdfd85b22ead2f29101a1242a578";
    private final static String privBrainpoolP320r1a = "304a020100301406072a8648ce3d020106092b2403030208010109042f302d02010104287cd9c454ba907f7617e262a7fd73764c4a3157c13f82279ef9f062be5d49a8e390b66a4dcedfa867";
    private final static String pubBrainpoolP320r1b = "306a301406072a8648ce3d020106092b240303020801010903520004b1246229429354d1d687bca48bccd6fc733b146dac03642a0ad4b896f5d8bcbd2f4bca16776e4526a41683898f9a76ef36ea2dc7b74d419e55cf3664721890d6a2b2fb8ceb7c113167ed137a358ee37f";
    private final static String privBrainpoolP320r1b = "304a020100301406072a8648ce3d020106092b2403030208010109042f302d0201010428b832a73da5f671e80d87f09372544801f6812224b19a4bc1b37aa7db0842e6dd3ca11de0f802bfed";
    private final static String secretBrainpoolP320r1 = "730314d906b2f21dc11be05031b028d665696beec7139328cdf70c718be5d208659bb96743a88067";

    // from https://tools.ietf.org/html/rfc7027#appendix-A.2
    private final static String pubBrainpoolP384r1a = "307a301406072a8648ce3d020106092b240303020801010b0362000468b665dd91c195800650cdd363c625f4e742e8134667b767b1b476793588f885ab698c852d4a6e77a252d6380fcaf06855bc91a39c9ec01dee36017b7d673a931236d2f1f5c83942d049e3fa20607493e0d038ff2fd30c2ab67d15c85f7faa59";
    private final static String privBrainpoolP384r1a = "3052020100301406072a8648ce3d020106092b240303020801010b0437303502010104301e20f5e048a5886f1f157c74e91bde2b98c8b52d58e5003d57053fc4b0bd65d6f15eb5d1ee1610df870795143627d042";
    private final static String pubBrainpoolP384r1b = "307a301406072a8648ce3d020106092b240303020801010b036200044d44326f269a597a5b58bba565da5556ed7fd9a8a9eb76c25f46db69d19dc8ce6ad18e404b15738b2086df37e71d1eb462d692136de56cbe93bf5fa3188ef58bc8a3a0ec6c1e151a21038a42e9185329b5b275903d192f8d4e1f32fe9cc78c48";
    private final static String privBrainpoolP384r1b = "3052020100301406072a8648ce3d020106092b240303020801010b043730350201010430032640bc6003c59260f7250c3db58ce647f98e1260acce4acda3dd869f74e01f8ba5e0324309db6a9831497abac96670";
    private final static String secretBrainpoolP384r1 = "0bd9d3a7ea0b3d519d09d8e48d0785fb744a6b355e6304bc51c229fbbce239bbadf6403715c35d4fb2a5444f575d4f42";


    // from https://tools.ietf.org/html/rfc7027#appendix-A.3
    private final static String pubBrainpoolP512r1a = "30819b301406072a8648ce3d020106092b240303020801010d03818200040a420517e406aac0acdce90fcd71487718d3b953efd7fbec5f7f27e28c6149999397e91e029e06457db2d3e640668b392c2a7e737a7f0bf04436d11640fd09fd72e6882e8db28aad36237cd25d580db23783961c8dc52dfa2ec138ad472a0fcef3887cf62b623b2a87de5c588301ea3e5fc269b373b60724f5e82a6ad147fde7";
    private final static String privBrainpoolP512r1a = "3062020100301406072a8648ce3d020106092b240303020801010d04473045020101044016302ff0dbbb5a8d733dab7141c1b45acbc8715939677f6a56850a38bd87bd59b09e80279609ff333eb9d4c061231fb26f92eeb04982a5f1d1764cad57665422";
    private final static String pubBrainpoolP512r1b = "30819b301406072a8648ce3d020106092b240303020801010d03818200049d45f66de5d67e2e6db6e93a59ce0bb48106097ff78a081de781cdb31fce8ccbaaea8dd4320c4119f1e9cd437a2eab3731fa9668ab268d871deda55a5473199f2fdc313095bcdd5fb3a91636f07a959c8e86b5636a1e930e8396049cb481961d365cc11453a06c719835475b12cb52fc3c383bce35e27ef194512b71876285fa";
    private final static String privBrainpoolP512r1b = "3062020100301406072a8648ce3d020106092b240303020801010d044730450201010440230e18e1bcc88a362fa54e4ea3902009292f7f8033624fd471b5d8ace49d12cfabbc19963dab8e2f1eba00bffb29e4d72d13f2224562f405cb80503666b25429";
    private final static String secretBrainpoolP512r1 = "a7927098655f1f9976fa50a9d566865dc530331846381c87256baf3226244b76d36403c024d7bbf0aa0803eaff405d3d24f11a9b5c0bef679fe1454b21c4cd1f";

    @Override
    protected boolean skipTest(Provider p) {
        if (p.getService("KeyAgreement", "ECDH") == null) {
            System.out.println("Provider does not support ECDH, skipping");
            return true;
        }

        if (isNSS(p) && getNSSECC() == ECCState.Basic) {
            System.out.println("NSS only supports Basic ECC, skipping");
            return true;
        }

        return false;
    }

    @Override public void main(Provider p) throws Exception {
        /*
         * PKCS11Test.main will remove this provider if needed
         */
        Providers.setAt(p, 1);

        if (false) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", p);
            kpg.initialize(163);
            KeyPair kp = kpg.generateKeyPair();
            System.out.println(toString(kp.getPublic().getEncoded()));
            System.out.println(toString(kp.getPrivate().getEncoded()));
            kp = kpg.generateKeyPair();
            System.out.println(toString(kp.getPublic().getEncoded()));
            System.out.println(toString(kp.getPrivate().getEncoded()));
            return;
        }

        if (getSupportedECParameterSpec("secp192r1", p).isPresent()) {
            test(p, pub192a, priv192a, pub192b, priv192b, secret192);
        }
        if (getSupportedECParameterSpec("sect163r1", p).isPresent()) {
            test(p, pub163a, priv163a, pub163b, priv163b, secret163);
        }
        if (getSupportedECParameterSpec("brainpoolP256r1", p).isPresent()) {
            test(p, pubBrainpoolP256r1a, privBrainpoolP256r1a, pubBrainpoolP256r1b, privBrainpoolP256r1b, secretBrainpoolP256r1);
        }
        if (getSupportedECParameterSpec("brainpoolP320r1", p).isPresent()) {
            test(p, pubBrainpoolP320r1a, privBrainpoolP320r1a, pubBrainpoolP320r1b, privBrainpoolP320r1b, secretBrainpoolP320r1);
        }
        if (getSupportedECParameterSpec("brainpoolP384r1", p).isPresent()) {
            test(p, pubBrainpoolP384r1a, privBrainpoolP384r1a, pubBrainpoolP384r1b, privBrainpoolP384r1b, secretBrainpoolP384r1);
        }
        if (getSupportedECParameterSpec("brainpoolP512r1", p).isPresent()) {
            test(p, pubBrainpoolP512r1a, privBrainpoolP512r1a, pubBrainpoolP512r1b, privBrainpoolP512r1b, secretBrainpoolP512r1);
        }

        System.out.println("OK");
    }

    private final static void test(Provider p, String pub1s, String priv1s,
            String pub2s, String priv2s, String secrets) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC", p);
        PublicKey pub1 = kf.generatePublic(new X509EncodedKeySpec(parse(pub1s)));
        System.out.println("Testing using parameters "
                + ((ECPublicKey)pub1).getParams() + "...");

        PrivateKey priv1 = kf.generatePrivate(new PKCS8EncodedKeySpec(parse(priv1s)));
        PublicKey pub2 = kf.generatePublic(new X509EncodedKeySpec(parse(pub2s)));
        PrivateKey priv2 = kf.generatePrivate(new PKCS8EncodedKeySpec(parse(priv2s)));
        byte[] secret = parse(secrets);

        KeyAgreement ka1 = KeyAgreement.getInstance("ECDH", p);
        ka1.init(priv1);
        ka1.doPhase(pub2, true);
        byte[] s1 = ka1.generateSecret();
        if (Arrays.equals(secret, s1) == false) {
            System.out.println("expected: " + toString(secret));
            System.out.println("actual:   " + toString(s1));
            throw new Exception("Secret 1 does not match");
        }

        KeyAgreement ka2 = KeyAgreement.getInstance("ECDH", p);
        ka2.init(priv2);
        ka2.doPhase(pub1, true);
        byte[] s2 = ka2.generateSecret();
        if (Arrays.equals(secret, s2) == false) {
            System.out.println("expected: " + toString(secret));
            System.out.println("actual:   " + toString(s2));
            throw new Exception("Secret 2 does not match");
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestECDH(), args);
    }

}
