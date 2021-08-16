/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

import java.security.*;
import java.security.SecureRandom;
import java.security.spec.*;

import javax.crypto.spec.DHParameterSpec;

/**
 * Cache for DSA and DH parameter specs. Used by the KeyPairGenerators
 * in the Sun, SunJCE, and SunPKCS11 provider if no parameters have been
 * explicitly specified by the application.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
public final class ParameterCache {

    private ParameterCache() {
        // empty
    }

    // cache of DSA parameters
    private static final Map<Integer,DSAParameterSpec> dsaCache;

    // cache of DH parameters
    private static final Map<Integer,DHParameterSpec> dhCache;

    /**
     * Return cached DSA parameters for the given length combination of
     * prime and subprime, or null if none are available in the cache.
     */
    public static DSAParameterSpec getCachedDSAParameterSpec(int primeLen,
            int subprimeLen) {
        // ensure the sum is unique in all cases, i.e.
        // case#1: (512 <= p <= 1024) AND q=160
        // case#2: p=2048 AND q=224
        // case#3: p=2048 AND q=256
        // case#4: p=3072 AND q=256
        return dsaCache.get(Integer.valueOf(primeLen+subprimeLen));
    }

    /**
     * Return cached DH parameters for the given keylength, or null if none
     * are available in the cache.
     */
    public static DHParameterSpec getCachedDHParameterSpec(int keyLength) {
        return dhCache.get(Integer.valueOf(keyLength));
    }

    /**
     * Return DSA parameters for the given primeLen. Uses cache if
     * possible, generates new parameters and adds them to the cache
     * otherwise.
     */
    public static DSAParameterSpec getDSAParameterSpec(int primeLen,
            SecureRandom random)
            throws NoSuchAlgorithmException, InvalidParameterSpecException,
                   InvalidAlgorithmParameterException {
        if (primeLen <= 1024) {
            return getDSAParameterSpec(primeLen, 160, random);
        } else if (primeLen == 2048) {
            return getDSAParameterSpec(primeLen, 224, random);
        } else if (primeLen == 3072) {
            return getDSAParameterSpec(primeLen, 256, random);
        } else {
            return null;
        }
    }

    /**
     * Return DSA parameters for the given primeLen and subprimeLen.
     * Uses cache if possible, generates new parameters and adds them to the
     * cache otherwise.
     */
    public static DSAParameterSpec getDSAParameterSpec(int primeLen,
            int subprimeLen, SecureRandom random)
            throws NoSuchAlgorithmException, InvalidParameterSpecException,
                   InvalidAlgorithmParameterException {
        DSAParameterSpec spec =
            getCachedDSAParameterSpec(primeLen, subprimeLen);
        if (spec != null) {
            return spec;
        }
        spec = getNewDSAParameterSpec(primeLen, subprimeLen, random);
        dsaCache.put(Integer.valueOf(primeLen + subprimeLen), spec);
        return spec;
    }

    /**
     * Return DH parameters for the given keylength. Uses cache if possible,
     * generates new parameters and adds them to the cache otherwise.
     */
    public static DHParameterSpec getDHParameterSpec(int keyLength,
            SecureRandom random)
            throws NoSuchAlgorithmException, InvalidParameterSpecException {
        DHParameterSpec spec = getCachedDHParameterSpec(keyLength);
        if (spec != null) {
            return spec;
        }
        AlgorithmParameterGenerator gen =
                AlgorithmParameterGenerator.getInstance("DH");
        gen.init(keyLength, random);
        AlgorithmParameters params = gen.generateParameters();
        spec = params.getParameterSpec(DHParameterSpec.class);
        dhCache.put(Integer.valueOf(keyLength), spec);
        return spec;
    }

    /**
     * Return new DSA parameters for the given length combination of prime and
     * sub prime. Do not lookup in cache and do not cache the newly generated
     * parameters. This method really only exists for the legacy method
     * DSAKeyPairGenerator.initialize(int, boolean, SecureRandom).
     */
    public static DSAParameterSpec getNewDSAParameterSpec(int primeLen,
            int subprimeLen, SecureRandom random)
            throws NoSuchAlgorithmException, InvalidParameterSpecException,
                   InvalidAlgorithmParameterException {
        AlgorithmParameterGenerator gen =
                AlgorithmParameterGenerator.getInstance("DSA");
        // Use init(int size, SecureRandom random) for legacy DSA key sizes
        if (primeLen < 1024) {
            gen.init(primeLen, random);
        } else {
            DSAGenParameterSpec genParams =
                new DSAGenParameterSpec(primeLen, subprimeLen);
            gen.init(genParams, random);
        }
        AlgorithmParameters params = gen.generateParameters();
        DSAParameterSpec spec = params.getParameterSpec(DSAParameterSpec.class);
        return spec;
    }

    static {
        dhCache = new ConcurrentHashMap<Integer,DHParameterSpec>();
        dsaCache = new ConcurrentHashMap<Integer,DSAParameterSpec>();

        /*
         * We support precomputed parameter for legacy 512, 768 bit moduli,
         * and (L, N) combinations of (1024, 160), (2048, 224), (2048, 256),
         * (3072, 256). In this file we provide both the seed and counter
         * value of the generation process for each of these seeds,
         * for validation purposes. We also include the test vectors
         * from the DSA specification, FIPS 186, and the FIPS 186
         * Change No 1, which updates the test vector using SHA-1
         * instead of SHA (for both the G function and the message
         * hash.
         */

        /*
         * L = 512
         * SEED = b869c82b35d70e1b1ff91b28e37a62ecdc34409b
         * counter = 123
         */
        BigInteger p512 =
            new BigInteger("fca682ce8e12caba26efccf7110e526db078b05edecb" +
                           "cd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e1" +
                           "2ed0899bcd132acd50d99151bdc43ee737592e17", 16);

        BigInteger q512 =
            new BigInteger("962eddcc369cba8ebb260ee6b6a126d9346e38c5", 16);

        BigInteger g512 =
            new BigInteger("678471b27a9cf44ee91a49c5147db1a9aaf244f05a43" +
                           "4d6486931d2d14271b9e35030b71fd73da179069b32e" +
                           "2935630e1c2062354d0da20a6c416e50be794ca4", 16);

        /*
         * L = 768
         * SEED = 77d0f8c4dad15eb8c4f2f8d6726cefd96d5bb399
         * counter = 263
         */
        BigInteger p768 =
            new BigInteger("e9e642599d355f37c97ffd3567120b8e25c9cd43e" +
                           "927b3a9670fbec5d890141922d2c3b3ad24800937" +
                           "99869d1e846aab49fab0ad26d2ce6a22219d470bc" +
                           "e7d777d4a21fbe9c270b57f607002f3cef8393694" +
                           "cf45ee3688c11a8c56ab127a3daf", 16);

        BigInteger q768 =
            new BigInteger("9cdbd84c9f1ac2f38d0f80f42ab952e7338bf511",
                           16);

        BigInteger g768 =
            new BigInteger("30470ad5a005fb14ce2d9dcd87e38bc7d1b1c5fac" +
                           "baecbe95f190aa7a31d23c4dbbcbe06174544401a" +
                           "5b2c020965d8c2bd2171d3668445771f74ba084d2" +
                           "029d83c1c158547f3a9f1a2715be23d51ae4d3e5a" +
                           "1f6a7064f316933a346d3f529252", 16);


        /*
         * L = 1024
         * SEED = 8d5155894229d5e689ee01e6018a237e2cae64cd
         * counter = 92
         */
        BigInteger p1024 =
            new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523c" +
                           "ef4400c31e3f80b6512669455d402251fb593d8d58" +
                           "fabfc5f5ba30f6cb9b556cd7813b801d346ff26660" +
                           "b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c6" +
                           "1bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554" +
                           "135a169132f675f3ae2b61d72aeff22203199dd148" +
                           "01c7", 16);

        BigInteger q1024 =
            new BigInteger("9760508f15230bccb292b982a2eb840bf0581cf5",
                           16);

        BigInteger g1024 =
            new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa" +
                           "3aea82f9574c0b3d0782675159578ebad4594fe671" +
                           "07108180b449167123e84c281613b7cf09328cc8a6" +
                           "e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f" +
                           "0bfa213562f1fb627a01243bcca4f1bea8519089a8" +
                           "83dfe15ae59f06928b665e807b552564014c3bfecf" +
                           "492a", 16);

        dsaCache.put(Integer.valueOf(512+160),
                                new DSAParameterSpec(p512, q512, g512));
        dsaCache.put(Integer.valueOf(768+160),
                                new DSAParameterSpec(p768, q768, g768));
        dsaCache.put(Integer.valueOf(1024+160),
                                new DSAParameterSpec(p1024, q1024, g1024));
        /*
         * L = 2048, N = 224
         * SEED = 584236080cfa43c09b02354135f4cc5198a19efada08bd866d601ba4
         * counter = 2666
         */
        BigInteger p2048_224 =
            new BigInteger("8f7935d9b9aae9bfabed887acf4951b6f32ec59e3b" +
                           "af3718e8eac4961f3efd3606e74351a9c4183339b8" +
                           "09e7c2ae1c539ba7475b85d011adb8b47987754984" +
                           "695cac0e8f14b3360828a22ffa27110a3d62a99345" +
                           "3409a0fe696c4658f84bdd20819c3709a01057b195" +
                           "adcd00233dba5484b6291f9d648ef883448677979c" +
                           "ec04b434a6ac2e75e9985de23db0292fc1118c9ffa" +
                           "9d8181e7338db792b730d7b9e349592f6809987215" +
                           "3915ea3d6b8b4653c633458f803b32a4c2e0f27290" +
                           "256e4e3f8a3b0838a1c450e4e18c1a29a37ddf5ea1" +
                           "43de4b66ff04903ed5cf1623e158d487c608e97f21" +
                           "1cd81dca23cb6e380765f822e342be484c05763939" +
                           "601cd667", 16);

        BigInteger q2048_224 =
            new BigInteger("baf696a68578f7dfdee7fa67c977c785ef32b233ba" +
                           "e580c0bcd5695d", 16);

        BigInteger g2048_224 =
            new BigInteger("16a65c58204850704e7502a39757040d34da3a3478" +
                           "c154d4e4a5c02d242ee04f96e61e4bd0904abdac8f" +
                           "37eeb1e09f3182d23c9043cb642f88004160edf9ca" +
                           "09b32076a79c32a627f2473e91879ba2c4e744bd20" +
                           "81544cb55b802c368d1fa83ed489e94e0fa0688e32" +
                           "428a5c78c478c68d0527b71c9a3abb0b0be12c4468" +
                           "9639e7d3ce74db101a65aa2b87f64c6826db3ec72f" +
                           "4b5599834bb4edb02f7c90e9a496d3a55d535bebfc" +
                           "45d4f619f63f3dedbb873925c2f224e07731296da8" +
                           "87ec1e4748f87efb5fdeb75484316b2232dee553dd" +
                           "af02112b0d1f02da30973224fe27aeda8b9d4b2922" +
                           "d9ba8be39ed9e103a63c52810bc688b7e2ed4316e1" +
                           "ef17dbde", 16);

        dsaCache.put(Integer.valueOf(2048+224),
                     new DSAParameterSpec(p2048_224, q2048_224, g2048_224));

        /*
         * L = 2048, N = 256
         * SEED = b0b4417601b59cbc9d8ac8f935cadaec \
         *        4f5fbb2f23785609ae466748d9b5a536
         * counter = 497
         */
        BigInteger p2048_256 =
            new BigInteger("95475cf5d93e596c3fcd1d902add02f427f5f3c721" +
                           "0313bb45fb4d5bb2e5fe1cbd678cd4bbdd84c9836b" +
                           "e1f31c0777725aeb6c2fc38b85f48076fa76bcd814" +
                           "6cc89a6fb2f706dd719898c2083dc8d896f84062e2" +
                           "c9c94d137b054a8d8096adb8d51952398eeca852a0" +
                           "af12df83e475aa65d4ec0c38a9560d5661186ff98b" +
                           "9fc9eb60eee8b030376b236bc73be3acdbd74fd61c" +
                           "1d2475fa3077b8f080467881ff7e1ca56fee066d79" +
                           "506ade51edbb5443a563927dbc4ba520086746175c" +
                           "8885925ebc64c6147906773496990cb714ec667304" +
                           "e261faee33b3cbdf008e0c3fa90650d97d3909c927" +
                           "5bf4ac86ffcb3d03e6dfc8ada5934242dd6d3bcca2" +
                           "a406cb0b", 16);

        BigInteger q2048_256 =
            new BigInteger("f8183668ba5fc5bb06b5981e6d8b795d30b8978d43" +
                           "ca0ec572e37e09939a9773", 16);

        BigInteger g2048_256 =
            new BigInteger("42debb9da5b3d88cc956e08787ec3f3a09bba5f48b" +
                           "889a74aaf53174aa0fbe7e3c5b8fcd7a53bef563b0" +
                           "e98560328960a9517f4014d3325fc7962bf1e04937" +
                           "0d76d1314a76137e792f3f0db859d095e4a5b93202" +
                           "4f079ecf2ef09c797452b0770e1350782ed57ddf79" +
                           "4979dcef23cb96f183061965c4ebc93c9c71c56b92" +
                           "5955a75f94cccf1449ac43d586d0beee43251b0b22" +
                           "87349d68de0d144403f13e802f4146d882e057af19" +
                           "b6f6275c6676c8fa0e3ca2713a3257fd1b27d0639f" +
                           "695e347d8d1cf9ac819a26ca9b04cb0eb9b7b03598" +
                           "8d15bbac65212a55239cfc7e58fae38d7250ab9991" +
                           "ffbc97134025fe8ce04c4399ad96569be91a546f49" +
                           "78693c7a", 16);

        dsaCache.put(Integer.valueOf(2048+256),
                new DSAParameterSpec(p2048_256, q2048_256, g2048_256));


        /*
         * L = 3072, N = 256
         * SEED = 9fe304be4d6b9919559f39d5911d12e9 \
         *        5158d6946598cd59775b8f3b8fff3a3f
         * counter = 1186
         */
        BigInteger p3072_256 = new BigInteger(
                "ea9cda9f5fbda66dd830494609405687ab7cf38538e058d1" +
                "e2f68dea95364866e1c05beacded24227edee28cad80bcec" +
                "ad39913be3b713267b3b96c8d9f0f6a03b5dfc9222d5cfe4" +
                "afcc9982f33784f760c3b759aebe3bbe9098a6b84c96f1fd" +
                "e44ce11c084c2a082c7a76a0ef142928b4f328406ab9beb2" +
                "4f84577dd0f46ce86fd8f08488269998bf4742d6425f7a0e" +
                "c75d8660c5dd6f4e3b3d3bee81b2c21afe8c9e8b84b87192" +
                "e2cc20f961d2bcd8133afcf3675ab80681cb374c78f33e29" +
                "d1011083d89f9c5728b94676fccb1b57bc60288c15d85ae8" +
                "38ae1941c5a20ae2b2049b3583fe30da455ddb3e6ad9b995" +
                "5cd9bb5681431622beb0f92da533fcab496cebc447aa1bb5" +
                "a8039522f2da98ff416289323a64df626ab6881870927dce" +
                "e387f13b5c9d24d6cba1d82ed375a082506ee87bc7ae3006" +
                "7f4a94e2ee363d992c40f2725b5db4b3525ebde22bbbfd0f" +
                "a124a588b0f5a4acb3a86951aff09f8c8198fb5b53da0c93" +
                "1cedc598b4f835b779d04d99026c7ba08c4b27f118ac1e3d", 16);

        BigInteger q3072_256 = new BigInteger(
                "c4eeac2bbab79bd831946d717a56a6e687547aa8e9c5494a" +
                "5a4b2f4ca13d6c11", 16);

        BigInteger g3072_256 = new BigInteger(
                "42e5fa7844f8fa9d8998d830d004e7b15b1d276bcbe5f12c" +
                "35ec90c1a25f5832018a6724bd9cdbe803b675509bed167f" +
                "3d7cf8599fc865c6d5a0f79158c1bc918f00a944d0ad0f38" +
                "f520fb91d85d82674d0d5f874faa5fcdfe56cd178c1afdc7" +
                "ce8795727b7dee966ed0b3c5cedcef8aca628befebf2d105" +
                "c7aff8eb0da9c9610737dd64dce1237b82c1b2bc8608d55f" +
                "fda98d7189444e65883315669c05716bde36c78b130aa3df" +
                "2e4d609914c7c8dc470f4e300187c775f81e7b1a9c0dce40" +
                "5d6eab2cbb9d9c4ef44412ba573dd403c4ed7bc2364772f5" +
                "6a30c48de78f5003f9371c55262d2c8ac2246ade3b02fdcf" +
                "cf5cbfde74fbcbfe6e0e0fdf3160764f84d311c179a40af6" +
                "79a8f47ab13c8f706893245eb11edcce451fa2ab98001998" +
                "7f125d8dc96622d419ba0d71f16c6024dce9d364c3b26d8e" +
                "c1a3c828f6c9d14b1d0333b95db77bfdbe3c6bce5337a1a5" +
                "a7ace10111219448447197e2a344cc423be768bb89e27be6" +
                "cbd22085614a5a3360be23b1bfbb6e6e6471363d32c85d31", 16);

        dsaCache.put(Integer.valueOf(3072+256),
                new DSAParameterSpec(p3072_256, q3072_256, g3072_256));

        //
        // Diffie-Hellman Groups
        //

        // the common generator
        BigInteger dhG = BigInteger.TWO;

        //
        // From RFC 7296

        // The prime is: 2^768 - 2 ^704 - 1 + 2^64 * { [2^638 pi] + 149686 }
        BigInteger dhP768 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF", 16);

        // The prime is 2^1024 - 2^960 - 1 + 2^64 * { [2^894 pi] + 129093 }
        BigInteger dhP1024 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381" +
                "FFFFFFFFFFFFFFFF", 16);

        //
        // From RFC 3526

        // The prime is: 2^1536 - 2^1472 - 1 + 2^64 * { [2^1406 pi] + 741804 }
        BigInteger dhP1536 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16);

        // This prime is: 2^2048 - 2^1984 - 1 + 2^64 * { [2^1918 pi] + 124476 }
        BigInteger dhP2048 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);

        // This prime is: 2^3072 - 2^3008 - 1 + 2^64 * { [2^2942 pi] + 1690314 }
        BigInteger dhP3072 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
                "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
                "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
                "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
                "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF", 16);

        // This prime is: 2^4096 - 2^4032 - 1 + 2^64 * { [2^3966 pi] + 240904 }
        BigInteger dhP4096 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
                "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
                "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
                "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
                "43DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D7" +
                "88719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA" +
                "2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6" +
                "287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED" +
                "1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA9" +
                "93B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934063199" +
                "FFFFFFFFFFFFFFFF", 16);

        // This prime is: 2^6144 - 2^6080 - 1 + 2^64 * { [2^6014 pi] + 929484 }
        BigInteger dhP6144 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08" +
                "8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B" +
                "302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9" +
                "A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6" +
                "49286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8" +
                "FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C" +
                "180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF695581718" +
                "3995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D" +
                "04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7D" +
                "B3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D226" +
                "1AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFC" +
                "E0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B26" +
                "99C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB" +
                "04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2" +
                "233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127" +
                "D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934028492" +
                "36C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406" +
                "AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918" +
                "DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B33205151" +
                "2BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03" +
                "F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97F" +
                "BEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AA" +
                "CC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58B" +
                "B7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632" +
                "387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E" +
                "6DCC4024FFFFFFFFFFFFFFFF", 16);

        // This prime is: 2^8192 - 2^8128 - 1 + 2^64 * { [2^8062 pi] + 4743158 }
        BigInteger dhP8192 = new BigInteger(
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
                "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
                "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
                "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
                "43DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D7" +
                "88719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA" +
                "2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6" +
                "287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED" +
                "1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA9" +
                "93B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934028492" +
                "36C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BD" +
                "F8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831" +
                "179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1B" +
                "DB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF" +
                "5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6" +
                "D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F3" +
                "23A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AA" +
                "CC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE328" +
                "06A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55C" +
                "DA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE" +
                "12BF2D5B0B7474D6E694F91E6DBE115974A3926F12FEE5E4" +
                "38777CB6A932DF8CD8BEC4D073B931BA3BC832B68D9DD300" +
                "741FA7BF8AFC47ED2576F6936BA424663AAB639C5AE4F568" +
                "3423B4742BF1C978238F16CBE39D652DE3FDB8BEFC848AD9" +
                "22222E04A4037C0713EB57A81A23F0C73473FC646CEA306B" +
                "4BCBC8862F8385DDFA9D4B7FA2C087E879683303ED5BDD3A" +
                "062B3CF5B3A278A66D2A13F83F44F82DDF310EE074AB6A36" +
                "4597E899A0255DC164F31CC50846851DF9AB48195DED7EA1" +
                "B1D510BD7EE74D73FAF36BC31ECFA268359046F4EB879F92" +
                "4009438B481C6CD7889A002ED5EE382BC9190DA6FC026E47" +
                "9558E4475677E9AA9E3050E2765694DFC81F56E880B96E71" +
                "60C980DD98EDD3DFFFFFFFFFFFFFFFFF", 16);

        // use DSA parameters for DH for sizes not defined in RFC 7296, 3526
        dhCache.put(Integer.valueOf(512), new DHParameterSpec(p512, g512));

        dhCache.put(Integer.valueOf(768), new DHParameterSpec(dhP768, dhG));
        dhCache.put(Integer.valueOf(1024), new DHParameterSpec(dhP1024, dhG));
        dhCache.put(Integer.valueOf(1536), new DHParameterSpec(dhP1536, dhG));
        dhCache.put(Integer.valueOf(2048), new DHParameterSpec(dhP2048, dhG));
        dhCache.put(Integer.valueOf(3072), new DHParameterSpec(dhP3072, dhG));
        dhCache.put(Integer.valueOf(4096), new DHParameterSpec(dhP4096, dhG));
        dhCache.put(Integer.valueOf(6144), new DHParameterSpec(dhP6144, dhG));
        dhCache.put(Integer.valueOf(8192), new DHParameterSpec(dhP8192, dhG));
    }
}
