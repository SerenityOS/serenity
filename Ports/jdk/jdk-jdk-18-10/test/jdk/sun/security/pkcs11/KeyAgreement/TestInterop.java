/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7146728
 * @summary Interop test for DH with secret that has a leading 0x00 byte
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestInterop
 * @run main/othervm -Djava.security.manager=allow TestInterop sm
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

public class TestInterop extends PKCS11Test {

    private final static BigInteger p = new BigInteger
    ("171718397966129586011229151993178480901904202533705695869569760169920539"
    + "80807543778874708672297590042574075430109846864794139516459381007417046"
    + "27996080624930219892858374168155487210358743785481212360509485282294161"
    + "39585571568998066586304075565145536350296006867635076744949977849997684"
    + "222020336013226588207303");

    private final static BigInteger g = new BigInteger("2");

    private final static BigInteger ya = new BigInteger
    ("687709211571508809414670982463565909269384277848448625781941269577397703"
    + "73675199968849153119146758339814638228795348558483510369322822476757204"
    + "22158455966026517829008713407587339322132253724742557954802911059639161"
    + "24827916158465757962384625410294483756242900146397201260757102085985457"
    + "09397033481077351036224");

    private final static BigInteger xa = new BigInteger
    ("104917367119952955556289227181599819745346393858545449202252025137706135"
    + "98100778613457655440586438263591136003106529323555991109623536177695714"
    + "66884181531401472902830508361532232717792847436112280721439936797741371"
    + "245140912614191507");

    private final static BigInteger yb  = new BigInteger
    ("163887874871842952463100699681506173424091615364591742415764095471629919"
    + "08421025296419917755446931473037086355546823601999684501737493240373415"
    + "65608293667837249198973539289354492348897732633852665609611113031379864"
    + "58514616034107537409230452318065341748503347627733368519091332060477528"
    + "173423377887175351037810");

    private final static BigInteger xb = new BigInteger
    ("127757517533485947079959908591028646859165238853082197617179368337276371"
    + "51601819447716934542027725311863797141734616730248519214531856941516613"
    + "30313414180008978013330410484011186019824874948204261839391153650949864"
    + "429505597086564709");

    @Override
    public void main(Provider prov) throws Exception {
        if (prov.getService("KeyAgreement", "DH") == null) {
            System.out.println("DH not supported, skipping");
            return;
        }
        try {
            System.out.println("testing generateSecret()");

            DHPublicKeySpec publicSpec;
            DHPrivateKeySpec privateSpec;
            KeyFactory kf = KeyFactory.getInstance("DH");
            KeyAgreement ka = KeyAgreement.getInstance("DH", prov);
            KeyAgreement kbSunJCE = KeyAgreement.getInstance("DH", "SunJCE");
            DHPrivateKeySpec privSpecA = new DHPrivateKeySpec(xa, p, g);
            DHPublicKeySpec pubSpecA = new DHPublicKeySpec(ya, p, g);
            PrivateKey privA = kf.generatePrivate(privSpecA);
            PublicKey pubA = kf.generatePublic(pubSpecA);

            DHPrivateKeySpec privSpecB = new DHPrivateKeySpec(xb, p, g);
            DHPublicKeySpec pubSpecB = new DHPublicKeySpec(yb, p, g);
            PrivateKey privB = kf.generatePrivate(privSpecB);
            PublicKey pubB = kf.generatePublic(pubSpecB);

            ka.init(privA);
            ka.doPhase(pubB, true);
            byte[] n1 = ka.generateSecret();

            kbSunJCE.init(privB);
            kbSunJCE.doPhase(pubA, true);
            byte[] n2 = kbSunJCE.generateSecret();

            if (Arrays.equals(n1, n2) == false) {
                throw new Exception("values mismatch!");
            } else {
                System.out.println("values: same");
            }

            System.out.println("testing generateSecret(byte[], int)");
            byte[] n3 = new byte[n1.length];
            ka.init(privB);
            ka.doPhase(pubA, true);
            int n3Len = ka.generateSecret(n3, 0);
            if (n3Len != n3.length) {
                throw new Exception("PKCS11 Length mismatch!");
            } else System.out.println("PKCS11 Length: ok");
            byte[] n4 = new byte[n2.length];
            kbSunJCE.init(privA);
            kbSunJCE.doPhase(pubB, true);
            int n4Len = kbSunJCE.generateSecret(n4, 0);
            if (n4Len != n4.length) {
                throw new Exception("SunJCE Length mismatch!");
            } else System.out.println("SunJCE Length: ok");

            if (Arrays.equals(n3, n4) == false) {
                throw new Exception("values mismatch! ");
            } else {
                System.out.println("values: same");
            }
        } catch (Exception ex) {
            System.out.println("Unexpected ex: " + ex);
            ex.printStackTrace();
            throw ex;
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestInterop(), args);
    }
}
