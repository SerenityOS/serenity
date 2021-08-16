/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4942494 7146728
 * @summary KAT test for DH (normal and with secret that has leading a 0x00 byte)
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestShort
 * @run main/othervm -Djava.security.manager=allow TestShort sm
 */

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;

public class TestShort extends PKCS11Test {

    private final static BigInteger p = new BigInteger
    ("132323768951986124075479307182674357577285270296234088722451560397577130"
    + "29036368719146452186041204237350521785240337048752071462798273003935646"
    + "236777459223");

    private final static BigInteger g = new BigInteger
    ("542164405743647514160964848832570512804742839438047437683466730076610826"
    + "26139005426812890807137245973106730741193551360857959820973906708903671"
    + "85141189796");

    private final static BigInteger y1 = new BigInteger
    ("917822587297202019713917824657175324360828836418754472207798053179332700"
    + "39938196470323405362414543604756313574842317687108720161868374135893507"
    + "32549013008");

    private final static BigInteger x1 = new BigInteger
    ("44680539865608058021525420137770558786664900449");

    private final static BigInteger y2 = new BigInteger
    ("971516093764754129400636279042779828227876735997548759620533874940954728"
    + "96003923584532197641582422156725687657451980378160229472095259392582713"
    + "54693857368");

    private final static BigInteger x2 = new BigInteger
    ("433011588852527167500079509018272713204454720683");

    private final static byte[] s2 = parse
    ("00:19:c7:f1:bb:2e:3d:93:fa:02:d2:e9:9f:75:32:b9:e6:7a:a0:4a:10:45:81:d4:2b:"
    + "e2:77:4c:70:41:39:7c:19:fa:65:64:47:49:8a:ad:0a:fa:9d:e9:62:68:97:c5:52"
    + ":b1:37:03:d9:cd:aa:e1:bd:7e:71:0c:fc:15:a1:95");

    private final static BigInteger y3 = new BigInteger
    ("487191942830952492045314176949691887949505843590154039270855000076570641"
    + "84133173374554778014985281423493547105556633876312739488944445812738030"
    + "00691614787");

    private final static BigInteger x3 = new BigInteger
    ("1105612503769813327556221318510360767544481637404");

    private final static byte[] s3 = parse
    ("98:62:f3:e4:ff:2b:8d:8a:5a:20:fe:52:35:56:73:09:8e:b3:e2:cb:e2:45:e5:b7:"
    + "1a:6a:15:d8:a4:8c:0a:ce:f0:15:03:0c:c2:56:82:a2:75:9b:49:fe:ed:60:c5:6e"
    + ":de:47:55:62:4f:16:20:6d:74:cc:7b:95:93:25:2c:ea");

    @Override
    public void main(Provider provider) throws Exception {
        if (provider.getService("KeyAgreement", "DH") == null) {
            System.out.println("DH not supported, skipping");
            return;
        }
        try {
            DHPublicKeySpec publicSpec;
            DHPrivateKeySpec privateSpec;
            KeyFactory kf = KeyFactory.getInstance("DH", provider);
            KeyAgreement ka = KeyAgreement.getInstance("DH", provider);

            PrivateKey pr1 = kf.generatePrivate(new DHPrivateKeySpec(x1, p, g));
            PublicKey pu2 = kf.generatePublic(new DHPublicKeySpec(y2, p, g));
            PublicKey pu3 = kf.generatePublic(new DHPublicKeySpec(y3, p, g));

            ka.init(pr1);
            ka.doPhase(pu2, true);
            byte[] n2 = ka.generateSecret();
            if (Arrays.equals(s2, n2) == false) {
                throw new Exception("mismatch 2");
            }
            System.out.println("short ok");

            ka.init(pr1);
            ka.doPhase(pu3, true);
            byte[] n3 = ka.generateSecret();
            if (Arrays.equals(s3, n3) == false) {
                throw new Exception("mismatch 3");
            }
            System.out.println("normal ok");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception: " + ex);
            ex.printStackTrace();
            throw ex;
        }

/*
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH", provider);
        kpg.initialize(512);
//        KeyPair kp1 = kpg.generateKeyPair();
//      System.out.println(kp1.getPublic());
//      System.out.println(kp1.getPrivate());
        while (true) {
            KeyAgreement ka = KeyAgreement.getInstance("DH", provider);
            ka.init(pr1);
            KeyPair kp2 = kpg.generateKeyPair();
            ka.doPhase(kp2.getPublic(), true);
            byte[] sec = ka.generateSecret();
            if (sec.length == 64) {
                System.out.println(kp2.getPrivate());
                System.out.println(kp2.getPublic());
                System.out.println(toString(sec));
                break;
            }
        }
/**/
    }

    public static void main(String[] args) throws Exception {
        main(new TestShort(), args);
    }

}
