/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202837
 * @summary PBES2 AlgorithmId encoding error in PKCS12 KeyStore
 */

import java.io.ByteArrayInputStream;
import java.security.KeyStore;

public class PBES2Encoding {

    // This is a PKCS 12 file using PBES2 to encrypt the cert and key. It is
    // generated with these commands:
    //
    // keytool -keystore ks -genkeypair -keyalg DSA -storepass changeit -alias a -dname CN=A
    // openssl pkcs12 -in ks -nodes -out kandc -passin pass:changeit
    // openssl pkcs12 -export -in kandc -out p12 -name a -passout pass:changeit
    //         -certpbe AES-128-CBC -keypbe AES-128-CBC

    static final String P12_FILE =
            "308208860201033082084c06092a864886f70d010701a082083d048208393082" +
            "08353082050406092a864886f70d010706a08204f5308204f1020100308204ea" +
            "06092a864886f70d010701304906092a864886f70d01050d303c301b06092a86" +
            "4886f70d01050c300e040879e103dcdf95bd3e02020800301d06096086480165" +
            "0304010204108afe09885014c1315a431956bf051cd1808204909af1a5964e45" +
            "fa24887c963d422500a54576bda5b79beba0a97ec61fd7f68e1d7acca9c7a2ff" +
            "fa90cc161d1e930a8315158089ce5e842ae758f63d723bd02b82120e663c2965" +
            "a32101d27f268f7101d9b8786ddeeb8ddfa57d63c7b110ad9015b6279f8c5adf" +
            "d2a78f82d6641bbb4c363bfe6d5be78993c54a1a52d83acba21f70a70c104c18" +
            "1968df3009896fb3af534661a8a3d25a09974adfd6dd587f067fd5a2017f0d34" +
            "99ca0dfd9950f555e950ceb3120408fb9b72e3b43020e4cde2f1c7726901328d" +
            "78471d7825ce6c9968d9f8e3ae2f2963cfc7d81bd1facfa55f56d711f3568673" +
            "6c75bc6ae67beee1a70355bf1daa17aba7cdc4fe35b06881503b7a99d1c3efeb" +
            "e036217042e2f7918c038dc5505447acbe84db6ac10527dd65597b25c8d5f68a" +
            "349ca4da866b76934aa7c1f4c452b6c45bd97d991c35d3ce63e20342817f7874" +
            "40d73d2b3329aafba3c95ed2601d2fc85231f0f1797f7e36ae4ff379af030a09" +
            "1d9c476468eff8be50632c706f0308a5e3a307a1929241b8fcd4fef447c6606b" +
            "7c45cc01c3dae196b54479d453428c378ec9cdbd45ff514793a91d61d0b230ff" +
            "4af62765773e9b51ef2f5965c046b25e7c09b42838b2a19fe5262756a64e7b96" +
            "6fa0159f369d6afb9f4486e2cc7e56a9b9d5afdd28e8c0e19ff4cdf2f7904a12" +
            "8284201f51dbdfb3fffb2d482764226c8bee83190d99e6bd0bf310eab5501922" +
            "aede091d3ee8fc405874a63fc696c577829192c65b70964bf54db87c77fe823b" +
            "19a8344275142387926909c39c0008d9111c78d7e296ba74ca448010db327e11" +
            "0927fe2029a5fcfde66d6883a26bab78e3dfdb9569a64d117b26e04d627b35b7" +
            "3a795eb9e6ea7bd4469403cb8a822831c71bc587d3b81b0ae4ca381df8b6872c" +
            "8bea5f8c99be643f34bafe5bac178c5a36a4c8088e0535eda0511326e3b0ae5e" +
            "dafde080886fa539f659525d3fcd38468e8c00c05d223d6bd060ef875894f7bc" +
            "63e61854fad5f6146959d0447a4714a3b79292890ae52c7aa82075f56386e3d3" +
            "fa2d3376156dc2f5811bd1ac2ca97cb1068a22577513e68a7a0116c6268f9146" +
            "a718c9e11dad701f3562be8cb9beb3aadd2003b32e3d88afbf178f7a7b5daf09" +
            "f5acaad1fe0dd27d7094a522a39ede71e621dc2b25b4e855d9a1853cdfa5f6f7" +
            "b4a0e1c7a5eacd4903aef9eae6a1c2df370137830bcf5cae2e96eef2d9934e9d" +
            "21e756499370cba32dc923f26915864b2a3cd5b046fad05922ff686f8ccb0b2b" +
            "4bce27d4c91a0c4d3fab3fd65eb0327d2435c27bdd789b66cb88fe56c31b1785" +
            "b8820a7c07947f3bf0d6d18ab2d334d927a70dad2fcdad31422138bb3ef39a3d" +
            "0e66c298e66d38efad20a8e963b98a59e8b6c5d92aea4364c5f720ea9ab6f510" +
            "2c5ccad50bcb3b5d3fe1ae2810118af0339a6b980c3e2ff1014675ef3a8ea84c" +
            "3a27b18a088979cddaee68cb65761fdeafc1ab766c13ca8c073cadedec3bf7c0" +
            "bdc2e91dcbb1295100a3d66838992a19049a7c25ad683c55ed9831cf187dfdba" +
            "242c38a9a32b9d023753c31519987f43d57a02b238230e93f8c5f5ab64516ece" +
            "eb915dda45ceb7257e87c909a381248a809b30202884b26eac08b53f9de2478f" +
            "3b0b410698e44744fbe63082032906092a864886f70d010701a082031a048203" +
            "16308203123082030e060b2a864886f70d010c0a0102a08202c3308202bf3049" +
            "06092a864886f70d01050d303c301b06092a864886f70d01050c300e040875ea" +
            "e60a1bd8525102020800301d060960864801650304010204101c066ab644ec44" +
            "506b2accab7458b77f04820270c4f2702354ebcd5eb1bfb82bd22382035a7907" +
            "ab5af91d045250ac1d56f95e4b0d944a99bccd266ea4f7402e5c2082f70ac8ff" +
            "081242bbd0e9b374eedcafbca01983ca9e324d8850cad4ac43396b1a3250e365" +
            "fa01e3882d19a01f017724a90242d0558d75399cf310ac551cd60d92e26bc8b2" +
            "7872882b1f41819d1f660f18a0a2825bd81c861e02124c586046a3609f36713b" +
            "dcefdc617788032d13268dfb6530205757aba368950041830cbd07ad3ef3987a" +
            "5d71c1cf9987be05de696b4191a44f405227c89fc857dfbf956fe0ab1a0d8c02" +
            "613773b1234acd9d3c75994ea00883c1686e3e57661d9937c1837770b3dd2874" +
            "0ccfcff02d1998cb9907a78b9d95475542cd3e064231f40e425a745dbc5cfec8" +
            "30f7b6e935487e68b664d998ddfaa06db44c247a0f012f17099c8bb955827e13" +
            "5017b2526bee9a222e70933f6d7b8968dffe4ca033022d4eac85259434d68e89" +
            "43d3c9e4c516ec88bb81971d6751803aef4afdb01505f81f8f71d3c074ab788f" +
            "7a5d197c3985488b6acc53c23bef91906f3009c6ec199cc916fcb88876a28727" +
            "32ee95d59f636d78e597e10a0e305bd1a5ccda8ad9716f0b5e9c8ca9bfa9ee54" +
            "224c1183d499d063c6c1ec02b7f9a482b8983bcbad6b64eefc77ef961ec4dd02" +
            "1f832e3c048b9f77034bbd896b7ab13a9f22d7fe94c88626e77b7c0b2d9fac44" +
            "914bd9c50cc69ef58044ae1cc423eb321bf5ce2c7505df45d21b932c675c0c5b" +
            "3705328245bc70ac262808519681f94489912a3dea891aab2d3bdc573b6b17cf" +
            "6bfd8c1a93404a91efaca5441cd2192b71529a543938056382a7f54fabea4760" +
            "6ef9ea7c8cdd663036e528ae6043ff138354b43b85cf488f3748fb1051313830" +
            "1106092a864886f70d01091431041e020061302306092a864886f70d01091531" +
            "160414664fad18d5583599e1cbe7fe694f36237e2272c530313021300906052b" +
            "0e03021a0500041472658e404aba0df42263cff430397794c379977504084962" +
            "aeaf211dfa1f02020800";

    public static void main(String[] args) throws Exception {

        byte[] p12 = new byte[P12_FILE.length() / 2];
        for (int i = 0; i < p12.length; i++) {
            p12[i] = Integer.valueOf(P12_FILE.substring(2 * i, 2 * i + 2), 16)
                    .byteValue();
        }

        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new ByteArrayInputStream(p12), "changeit".toCharArray());

        // Make sure both cert and key can be retrieved
        System.out.println(ks.getCertificate("a"));
        System.out.println(ks.getKey("a", "changeit".toCharArray()));
    }
}
