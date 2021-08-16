/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214513
 * @summary A PKCS12 keystore from Java 8 using custom PBE parameters cannot be read in Java 11
 */

import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class WrongPBES2 {

    private static final char[] PASS = "changeit".toCharArray();

    // This is a PKCS 12 file using PBES2 with an incorrect encoding before
    // JDK-8202837 is fixed. It is generated with these 2 commands:
    //
    // keytool -genkeypair -alias a -dname CN=A -keystore ks -storepass changeit
    // $JDK10/bin/java WrongPBES2 reenc
    static final String P12_FILE =
            "308208940201033082084d06092a864886f70d010701a082083e0482083a3082" +
            "08363082035206092a864886f70d010701a08203430482033f3082033b308203" +
            "37060b2a864886f70d010c0a0102a08202ee308202ea307406092a864886f70d" +
            "01050d306706092a864886f70d01050d305a303906092a864886f70d01050c30" +
            "2c0414000000000000000000000000000000000000000002030186a002012030" +
            "0c06082a864886f70d020b0500301d060960864801650304012a04105abc352d" +
            "35d98fd77d8c8cf205d690c6048202703ff97bd4488a6a5569993fb6f89792e7" +
            "33632b20fdc502bf0801c8ee80683d064c4dae5b8880ade893a87bfdb4427dfd" +
            "01bc5656fdb50f45f1e8d60b37416d2f3b1be9e8ae145bfe035f9d947fa49a31" +
            "baef86dec6780a0f96a7ad5d9ef850b15f56d6b6f2f0798c190b01b42d955d36" +
            "96f1416efb0c246b56e3715c1ccef701f975f6faf7e7640b8edca79f996682d8" +
            "5a414c8210004f247058c3362f328c12d024ea88de5ab3d6c50a7cf8f32b69de" +
            "999930ccb29be0d278216db0959d9b142e4c46207edaf91f37708e0250e99262" +
            "c4acba4b83c2c0affb3bfc2d5bfe210baffe56ba1f667c9e3c33e0c4164d0c16" +
            "abc89bca2c1af7fc234163a6a17e4852ee08053bdf699c8659ea57f77427ab3b" +
            "cea357a85ffd80589403bc73ff50490f9b0bfb37fedf495aae8ef8a00da7d1e1" +
            "62c49dacf82503e803953f48e302d1ae1b7e7870a479c7861ffc236469807f31" +
            "da0b15a915a62d4f0a42698a4c9779595619effa04765f91342821229752ec6d" +
            "6e0657ad4e41979847b5903ddb6a6819b859797a2f663898694fe294cc85b9c3" +
            "d485516659dcd6d89f631903be35367315b131f9f5ec63ee5bf25f3afcf8bc9c" +
            "d9056a56f951746664cfa3ed0596e005fda6fb753d9c67c92fd1a9f22d06a59e" +
            "9c7af058dddad3b9265fc29c18122f55c07a1780050251990c221ba2b3767586" +
            "405def7a1b224135ac4a694f0e3d27666486531aad9e27a3855f54e2916a642a" +
            "ed06fc5275b675bbf457a26978b0f4306c166119dedc3aedf366cbf34efa647e" +
            "a65b8451c7b1d083fe1ee3a8493a0330ee7da68552614e50ca7326a25d741e40" +
            "ccac2149d1ff5cb8778a3b08ed3d9a1774c971525454b7c20b18610b9bc847cc" +
            "3136301106092a864886f70d01091431041e020061302106092a864886f70d01" +
            "09153114041254696d652031353433363738383539323632308204dc06092a86" +
            "4886f70d010706a08204cd308204c9020100308204c206092a864886f70d0107" +
            "013029060a2a864886f70d010c0106301b0414e6d3162d3b75f103fac6ea3fbf" +
            "96d08c0225d308020300c3508082048834d05f8953cad673f7de3b5446bcdd31" +
            "310b376eec04c393f1077cfda8ad69fe0133cf79fbdd204eeca165e1d6e288b9" +
            "a6625dfe34aa722f230c50da44d463753d3bbd7e762cb0f164b7fbfe4a3aff8c" +
            "dc664ef2c6afd01232de66d81221e977738b81838cadd0fe1f87bdd7278dcdbd" +
            "2307bda516e931691e3da46f82f72073ab6502b9f2b23788c620756eedddf21e" +
            "705a6043d29a8e27285fea79e4a9966f5b08d5f13bc6f4d854f47879ee90e72d" +
            "3a5cc6eccda9edf60c4d73edf3e4dd6c715c06c53e83ba461e10868e5ef5e218" +
            "726bc6714f3071b3f17a075ff9d28d2cb48896b785d48551e8410f2bc882ad04" +
            "88d60c85c3b966e1b484e4190267b38e3179107f3c49abd2b058fb32eab4abfc" +
            "ee914ee4407954e1edb338042ca04b1561df364139a64a1080c79bfdfa17d15b" +
            "6fc26b0572427b2f1b9bbea6d1a730a7dd1b08d22093658f9a5665ae8e9a04b8" +
            "af89a60ea7994d17949b22e2fc6d5d4dd1d280283fd649e1aca6e0e4c90ae509" +
            "afc01bd92127321798562cf5ba75fed840f8ca9082c8c58334a95593fa5b1b57" +
            "ea86cb8452136dee7ecd63872d4848d9052e181f6bca0772a4d4dd43817b9005" +
            "a6314cbd90474a24b6ed9c9896097bba51ca7cd48120b5f9fee3f98dd99bbcd3" +
            "e0ab43bcbb922bcf48cf710f7a5b9d192f419f607b6ca7937d8ed6f990835b88" +
            "7f480cc5fad2708c57c674d0e082af60d8b0108df2bf88b321df4d0431d4dfc0" +
            "4ae21cfd5446715e4af6bcb2224a8d6dc26d71806f21c1e17e8dd2ac57632234" +
            "1d3e72cbcce4c4e59421fc5202ec6d76c623b81a872ab6b8d53e4215234ee8d9" +
            "61148b0f42300fc0c89ec91f528698d79a27af593e5fd56484c97763ebc4a797" +
            "13511ec0e13c59318d1ac444f6121a81a4f505b7e3dca70d6cd74b209fc10003" +
            "78d5f2016c63acd019a1175e41fe60b88ff5e7238b32ef25243da462cabcd6c7" +
            "5789c6055ef4999522acb386138f085e9bc2e9b41ecaf3bbb0990ac8b9efde0e" +
            "eedff4c636bd13ef2dc18bfe5e900d88f4649ce326bc4f8e9bfe058832a595e3" +
            "5c65415ca9606b0d2c705519eb83995a6da302f53e57a93f2be20dc858d3d5e9" +
            "e8fdbc10af092b47c108d2e44eed00879626389b35df014e54ea3bf067d2d800" +
            "b74f67c3576d585e6fedf9634e1ae7941d0de0faed7c76e0296d701728792bf4" +
            "9bf007738d9273af8981bd71ed4a6360c08a4c5ab83ee337141c37679cf5db7a" +
            "5aff7353ed6285bab948cac12559ee6ed9f34ee20e01b538975e26c310a33790" +
            "f8be9fbb907f2585f55d2a456c6be4e46e08edd3aa5392d1c9415a6ded826776" +
            "b04a3804f9b5f078888320ee5b279a4bba3b5c73ae1e8444b9320b92eb6b8e16" +
            "75f505bcf79ff0e0cc80326e9bedbc6baebce47725aef3914e4326b924984788" +
            "1d21d59d71b08ba34283f5784deeeecb9f4a3cfbcb21e48e8a9a94972ee7366e" +
            "04d1d9c60534fdb7bfe5ec559427b6bbe96d41f551dde0800684226920cbc4f4" +
            "1f3a7225e0c6a15d3ebee4e87ae4c40fef4d2e5272bb5b0037030665ae1faba7" +
            "4c41a04277bbe1851e090c8d31e48575bbc96618e3cfeea5a2f80fc1e565adc8" +
            "ffd84b803877ff9b305a4da59e6f707b3dbe95f7c037fbec303e302130090605" +
            "2b0e03021a05000414adb4dd32f3ce248ec62e2fd45835255c2316739e041441" +
            "f87d07019980ef69b635f06017567e36aa3d8802030186a0";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("reenc")) {
            reEncodeWithPBES2();
        } else {
            test();
        }
    }

    private static void test() throws Exception {
        byte[] p12 = new byte[P12_FILE.length() / 2];
        for (int i = 0; i < p12.length; i++) {
            p12[i] = Integer.valueOf(P12_FILE.substring(2 * i, 2 * i + 2), 16)
                    .byteValue();
        }
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new ByteArrayInputStream(p12), PASS);
        System.out.println(ks.getKey("a", PASS));
    }

    private static void reEncodeWithPBES2() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(new File("ks"), PASS);
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(
                (PrivateKey)keyStore.getKey("a", PASS),
                new Certificate[] { keyStore.getCertificate("a") });
        keyStore.setEntry("a", privateKeyEntry, new KeyStore.PasswordProtection(
                PASS, "PBEWithHmacSHA512AndAES_256",
                new PBEParameterSpec(new byte[20], 100_000)));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        keyStore.store(bout, PASS);
        byte[] p12 = bout.toByteArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p12.length; i++) {
            if (i % 32 == 0) {
                if (i != 0) {
                    sb.append("\" +\n");
                }
                sb.append("\"");
            }
            sb.append(String.format("%02x", p12[i] & 0xff));
        }
        sb.append("\";\n");
        System.out.println(sb.toString());
    }
}
