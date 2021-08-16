/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5008156 8248268
 * @run testng NISTWrapKAT
 * @summary Verify that the AES-Key-Wrap and AES-Key-Wrap-Pad ciphers
 * work as expected using NIST test vectors.
 * @author Valerie Peng
 */
import java.security.Key;
import java.security.AlgorithmParameters;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Arrays;
import java.math.BigInteger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

public class NISTWrapKAT {

    private static final String KEK =
        "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    private static final String DATA =
        "00112233445566778899aabbccddeeff000102030405060708090a0b0c0d0e0f";
    // from RFC 3394 sec4
    private static String KW_AES128_128 =
        "1fa68b0a8112b447aef34bd8fb5a7b829d3e862371d2cfe5";
    private static String KW_AES192_128 =
        "96778b25ae6ca435f92b5b97c050aed2468ab8a17ad84e5d";
    private static String KW_AES192_192 =
        "031d33264e15d33268f24ec260743edce1c6c7ddee725a936ba814915c6762d2";
    private static String KW_AES256_128 =
        "64e8c3f9ce0f5ba263e9777905818a2a93c8191e7d6e8ae7";
    private static String KW_AES256_192 =
        "a8f9bc1612c68b3ff6e6f4fbe30e71e4769c8b80a32cb8958cd5d17d6b254da1";
    private static String KW_AES256_256 =
        "28c9f404c4b810f4cbccb35cfb87f8263f5786e2d80ed326cbc7f0e71a99f43bfb988b9b7a02dd21";

    private static String KWP_AES128_56 = "1B1D4BC2A90B1FA389412B3D40FECB20";
    private static String KWP_AES128_112 =
            "EA0BFDE8AF063E8918E811A05D2A4C23A367B45315716B5B";
    private static String KWP_AES192_56 = "87CE2C5C2D7196E09381056B319D91E9";
    private static String KWP_AES192_112 =
            "900484950F84EB6ED74CE81DCDACA26E72BB29D4A6F7AC74";
    private static String KWP_AES192_168 =
            "A402348F1956DB968FDDFD8976420F9DDEB7183CF16B91B0AEB74CAB196C343E";
    private static String KWP_AES256_56 = "809BB1864A18938529E97EFCD9544E9A";
    private static String KWP_AES256_112 =
            "C68168173F141E6D5767611574A941259090DA78D7DF9DF7";
    private static String KWP_AES256_168 =
            "308D49692B5F8CF638D54BB4B985633504237329964C76EBB3F669870A708DBC";
    private static String KWP_AES256_224 =
            "0942747DB07032A3F04CDB2E7DE1CBA038F92BC355393AE9A0E4AE8C901912AC3D3AF0F16D240607";
     // from RFC 5649 sec6
     private static String KEK2 = "5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8";

    private static byte[] toBytes(String hex, int hexLen) {
        if (hexLen < hex.length()) {
            hex = hex.substring(0, hexLen);
        } else {
            hexLen = hex.length();
        }
        int outLen = hexLen >> 1;
        BigInteger temp = new BigInteger(hex, 16);
        byte[] val = temp.toByteArray();
        if (val.length == outLen) {
            return val;
        } else {
            byte[] out = new byte[outLen];
            if (val.length < outLen) {
                // enlarge
                System.arraycopy(val, 0, out, outLen - val.length, val.length);
            } else {
                // truncate
                System.arraycopy(val, val.length - outLen, out, 0, outLen);
            }
            return out;
        }
    }

    @DataProvider
    public Object[][] testData() {
        return new Object[][] {
            { "AESWrap", KEK, 16, DATA, 16, KW_AES128_128 },
            { "AESWrap", KEK, 24, DATA, 16, KW_AES192_128 },
            { "AESWrap", KEK, 24, DATA, 24, KW_AES192_192 },
            { "AESWrap", KEK, 32, DATA, 16, KW_AES256_128 },
            { "AESWrap", KEK, 32, DATA, 24, KW_AES256_192 },
            { "AESWrap", KEK, 32, DATA, 32, KW_AES256_256 },
            { "AESWrap_128", KEK, 16, DATA, 16, KW_AES128_128 },
            { "AESWrap_192", KEK, 24, DATA, 16, KW_AES192_128 },
            { "AESWrap_256", KEK, 32, DATA, 16, KW_AES256_128 },
            { "AES/KW/NoPadding", KEK, 16, DATA, 16, KW_AES128_128 },
            { "AES/KW/NoPadding", KEK, 24, DATA, 16, KW_AES192_128 },
            { "AES/KW/NoPadding", KEK, 24, DATA, 24, KW_AES192_192 },
            { "AES/KW/NoPadding", KEK, 32, DATA, 16, KW_AES256_128 },
            { "AES/KW/NoPadding", KEK, 32, DATA, 24, KW_AES256_192 },
            { "AES/KW/NoPadding", KEK, 32, DATA, 32, KW_AES256_256 },
            { "AESWrapPad", KEK, 16, DATA, 7, KWP_AES128_56 },
            { "AESWrapPad", KEK, 16, DATA, 14, KWP_AES128_112 },
            { "AESWrapPad", KEK, 24, DATA, 7, KWP_AES192_56 },
            { "AESWrapPad", KEK, 24, DATA, 14, KWP_AES192_112 },
            { "AESWrapPad", KEK, 24, DATA, 21, KWP_AES192_168 },
            { "AESWrapPad", KEK, 32, DATA, 7, KWP_AES256_56 },
            { "AESWrapPad", KEK, 32, DATA, 14, KWP_AES256_112 },
            { "AESWrapPad", KEK, 32, DATA, 21, KWP_AES256_168 },
            { "AESWrapPad", KEK, 32, DATA, 28, KWP_AES256_224 },
            { "AESWrapPad_128", KEK, 16, DATA, 7, KWP_AES128_56 },
            { "AESWrapPad_192", KEK, 24, DATA, 7, KWP_AES192_56 },
            { "AESWrapPad_256", KEK, 32, DATA, 7, KWP_AES256_56 },
            { "AES/KWP/NoPadding", KEK, 16, DATA, 7, KWP_AES128_56 },
            { "AES/KWP/NoPadding", KEK, 16, DATA, 14, KWP_AES128_112 },
            { "AES/KWP/NoPadding", KEK, 24, DATA, 7, KWP_AES192_56 },
            { "AES/KWP/NoPadding", KEK, 24, DATA, 14, KWP_AES192_112 },
            { "AES/KWP/NoPadding", KEK, 24, DATA, 21, KWP_AES192_168 },
            { "AES/KWP/NoPadding", KEK, 32, DATA, 7, KWP_AES256_56 },
            { "AES/KWP/NoPadding", KEK, 32, DATA, 14, KWP_AES256_112 },
            { "AES/KWP/NoPadding", KEK, 32, DATA, 21, KWP_AES256_168 },
            { "AES/KWP/NoPadding", KEK, 32, DATA, 28, KWP_AES256_224 },
            { "AES/KWP/NoPadding", KEK2, 24, "466F7250617369", 7,
              "AFBEB0F07DFBF5419200F2CCB50BB24F" },
            { "AES/KWP/NoPadding", KEK2, 24,
              "C37B7E6492584340BED12207808941155068F738", 20,
              "138BDEAA9B8FA7FC61F97742E72248EE5AE6AE5360D1AE6A5F54F373FA543B6A" },
            // some more test vectors for KW and KWP
            // from csrc.nist.gov/groups/STM/cavp/documents/mac/kwtestvectors.zip
            { "AES/KW/NoPadding", "7575da3a93607cc2bfd8cec7aadfd9a6", 16,
              "42136d3c384a3eeac95a066fd28fed3f", 16,
              "031f6bd7e61e643df68594816f64caa3f56fabea2548f5fb" },
            { "AES/KW/NoPadding", "e5d058e7f1c22c016c4e1cc9b26b9f8f", 16,
              "7f604e9b8d39d3c91e193fe6f196c1e3da6211a7c9a33b8873b64b138d1803" +
              "e4", 32,
              "60b9f8ac797c56e01e9b5f84d65816a980777869f67991a0e6dc19b8cd75c9" +
              "b54db4a38456bbd6f3" },
            { "AES/KW/NoPadding", "67ae4270bcdd31e8326b7e7f94c80276", 16,
              "57e748b62fbc37ba25e904ee973d01b136cf7c1d0c8c5c87", 24,
              "96cec0e3272a21faa550a857957aa38ce3c1cf06f0dd9f5b5c5c422cef6c69" +
              "a1" },
            { "AES/KW/NoPadding", "d7aa53aefad65cd95b57c8eee7b0a906", 16,
              "4a8daee6774751fc4489e837b8f7fba6896c70bb3d5e53053c92eb58046ee4" +
              "a7002e542311253b97", 40,
              "84ebd38cf06c674dcf186977de4a40c6dde3e7f49361a43420a887d2931b29" +
              "c23e2db72e95e4107001da925181bb7097" },
            { "AES/KW/NoPadding", "98311985c4661d7e811ee56070e6fecf", 16,
              "18840c96813864ef3093b48cdde6ac5d78248b96d4a2cd1f15f0b56f98213d" +
              "bf87e1ccad04e0d4f1954c233ea3e48fdad8f2b1156e54e19e3b5f4a66d2e9" +
              "149032b876c51249165fe8c28e112a685b2d228a8ac308017574274af36a4e" +
              "a3877bcc9850bafe8fc0e0a712faca0dea98396f9143bc5819fe4933a806e9" +
              "b965133e3c695a45f0fbd6961798c400d7477287df64798b651e0d3009c13f" +
              "7a2246c28f983509b9e5339919f2cdffcdfc550693cba9491c00334c4a62d8" +
              "78c4d0ca57b1104bc0174968ea8e3730b9e68db49678b23cd508ebe3e12e94" +
              "b0ad3791023a8ef95473f0b32f906738f34e94e45a4480ad768072e1853adb" +
              "63996b9ac27a1dade70804b82290a2274c6dcc3ccd40a8b38a56a5eb03f590" +
              "75de015e8f9096f53549f6374e02da947fb849287a447f757cc340b6bded71" +
              "d480988b6d2fcd984fba841470519830304667fef0a577b4cf84f76aef9deb" +
              "84dde36abfbd76673c17113dbea7a3e24bf9b57a8fd17173a1ef91497b732b" +
              "3fa8889bed58a503a0d3e20bc27ec4dbf5d13a93cbad05495e3df15e1fe34a" +
              "3a6d6f648ea4aa60b2f114f30944ae593675dac2db188f90a3c483fb82cec0" +
              "f0d295544d798b62cdcb51c6c036af1a341d78babf87b92609c1d866e311a4" +
              "6abccc8ba8c6a41420359bb061d7e752c0ed25990eef57c9f9e190572203f8" +
              "c473edf8cfc8c26d34e37240f45ded97", 512,
              "625aea9122b7b57b9f36446f9053acc42c6435a7f69d91b41547026f833291" +
              "d488e477c7ccba698c143633a304f463d6af4a3e72c189234fcfc360013e65" +
              "b07b7f7a36c529d3fdbbdbd6224bf100c14bc5354893b44790f54c739a2b1f" +
              "5bda82d70fb600ed9b0606dbddea52e508b492b72d8779856274aaaaddc0a3" +
              "edb6cfc788b603101bedfcc3f44baa62336bd950c2e349d5daf04f2e23ec26" +
              "28893d214e277569c565e5e6aa8b72ffa14118a3b57f814b4deb179980b5ee" +
              "efa4fd93f1751850466e929be537801babc2120f3ff1ffe5fea813ec7788ea" +
              "f43f5ef657e5af48395c3ad11aaf741549090b58670695f7c95c68e00576ca" +
              "18ef0313f2b4b757219fc8db3dc2db28721d6f912547ebfebcd96935c3100a" +
              "a4e4df9955acae1b4e2c10df1166d46c4285ab631c6d2ce58ad3ae99c07c01" +
              "9dcd15958694055281ccd6f803af290431f188cc4c429e84a4c30fd9c63968" +
              "dfd0951c417efb71921c207de172a9546bdd3e2bb35b45e140892c649f88c3" +
              "1a438f864e801a69f8010aa3d77a26601a7a89067c81b0f7e70d8e82f21f88" +
              "c7d0bb0c8ca0db875d6c3f8c6f6d709bbb31c7da2e31f3571daa2c5ab13bfc" +
              "16624cf35abd526e84269fb45bbd2fcd8c383d6fbb700bc4b5205b3ef8c432" +
              "3dc0d9e0370e56a3d1e5e76aa4de082e4c2a0afd092845bd5dab52a4594318" +
              "1461b76e3984b95f48bea80a94944241d04b5634c86274e7" },
            { "AES/KWP/NoPadding", "6decf10a1caf8e3b80c7a4be8c9c84e8", 16,
              "49", 1, "01a7d657fc4a5b216f261cca4d052c2b" },
            { "AES/KWP/NoPadding", "a8e06da625a65b25cf5030826830b661", 16,
              "43acff293120dd5d", 8, "b6f967616dd8d772e9fea295a456dba7" },
            { "AES/KWP/NoPadding", "7865e20f3c21659ab4690b629cdf3cc4", 16,
              "bd6843d420378dc896", 9,
              "41eca956d4aa047eb5cf4efe659661e74db6f8c564e23500" },
            { "AES/KWP/NoPadding", "be96dc195ec034d616486ed70e97fe83", 16,
              "85b5437b6335ebba7635903a4493d12a77d9357a9e0dbc013456d85f1d3201",
              31,
              "974769b3a7b4d5d32985f87fddf9990631e5610fbfb278387b58b1f48e05c7" +
              "7d2fb7575c5169eb0e" },
            { "AES/KWP/NoPadding", "0e54956a24c7d4a343f90269fb18a17f", 16,
              "817ddabdc5d215eee233adff97e92193c6beec52a71340477f70243a794ce9" +
              "54af51e356c9940e4ab198f0e68c543355f65ad179cb2d60dd369eaeb9ed14" +
              "1fb18c9e4054ac7fdc83506896990a4d20833d2d6e9a34938796ee67c9d7d2" +
              "3058544a4a35f2954103ce443a95a7e785602075ca0a73da37899e4568106b" +
              "b2dbf1f901377d4d3380c70fa5175ebc550481ac6f15986a4407fde5c23ff3" +
              "17e37544c0a25f87117506597db5bb79850c86247b73a5d0090417d63e4c25" +
              "7ea0220c2c04db07a34f0ab7954e1dfa2007a1466795c4d0c2aa09ca3986c0" +
              "28185b43a466526594afc9c891c263a7c608304bc1957c9873f544dc71e6f8" +
              "47c48d32026ed03b2333825452ee7e12a50e1cd7d678319264c65f78001996" +
              "d37fae7f9861fbd21cb506c2f8a3b0ee53c7debe17111b6e3f78a5c5677857" +
              "b082c2c4943dfd1edf6337fea98a44fc25928361156ef38d865948b979cf6f" +
              "4b46bd2119f12f0891cef7fc9d0638fd105fc05f9968d16948d1cb820751e8" +
              "2e44cb68e99d4f072ffd1577da6c0631b5827bec7e1b9ec72d18b74cf5f233" +
              "e85013c1668ceb5d7a1f5e0f016b0ff726a0a9d41e2cea8e14a2f56492b146" +
              "06d3fafd8ac141335f39f90d56863735628e8f17be90e100ef0785f3cd57db" +
              "8b9d89a6b2189dc2ea00c285d2657983f8bd7883c215477e67a55556401f1d" +
              "8b27d4e0d541c7fb7ace370c2e428884", 512,
              "876f3e53ba9cf4f6a521ac198bc813d0ede0f862ab6082e3e0a06ad82b4f27" +
              "9582f7c43bb63574608446bc2a05f401a68f74086cf2776b4b3df6b3679c2e" +
              "dfb91c024db54c6831e0752ae6f86c7596462de905ee0be908c1b9d043ecaf" +
              "e2ad1cbddb904e18ebc9b7a107031be3a87059516a3d1257812d9c801b0b9f" +
              "21539e70c47150c128d87c5e58fa6e4371aedde69c7b5cd16b73ac42267632" +
              "8131f3ac48c602bb6e0741805aad9d23b33b3523b86cf0588cdf9dc6c4d5f9" +
              "fa43d88ca17976eaf48fb37a41a598266da04144373df5631cc5126341c200" +
              "a0c8499b29ae96e6e6e6c2bdf8d8903da62bf8ddae970569b695240e77f8ac" +
              "5b191da5034008b6ef21936858e69bac372bbafd8794f6b03711503c187552" +
              "8a9348681844edb199a0664d740f0f0b1f866c4248c80fe8b5700a3c4134cd" +
              "ddb17676e0cd37d6d81831a0f4adfba071bb0935502480eccd48b28be5954e" +
              "a6c7d873b51b8bd2b709c5b6132ed31296510915073c18f7012f0eff6a9aad" +
              "5340a19fd5e372d35260b718d9e4807b1954c24e6a4fd48e4dbb8f395474e9" +
              "9ab577367d2ab5ccaa18c947331047dc3986e213a878b41089aa221019dad4" +
              "191a4feefd095f8606c2700a46d71cbb13efb6957df925ec26071c04d04d5a" +
              "94e138e5fc5d1f059236aad76208077dcc607b1dd2086f9c04e33f955822b4" +
              "57eecd68bd5f24836ecedbac675e6ed93d8a787cb57ad68e" },
        };
    }

    @Test(dataProvider = "testData")
    public void testKeyWrap(String algo, String key, int keyLen,
            String data, int dataLen, String expected) throws Exception {
        System.out.println("Testing " +  algo + " Cipher with wrapping " +
            dataLen + "-byte key with " + 8*keyLen + "-bit KEK");
        int allowed = Cipher.getMaxAllowedKeyLength("AES");
        if (keyLen > allowed) {
            System.out.println("=> skip, exceeds max allowed size " + allowed);
            return;
        }
        Cipher c1 = Cipher.getInstance(algo, "SunJCE");
        Cipher c2 = Cipher.getInstance(algo, "SunJCE");
        Cipher c3 = Cipher.getInstance(algo, "SunJCE");

        byte[] keyVal = toBytes(key, keyLen << 1);
        byte[] dataVal = toBytes(data, dataLen << 1);

        SecretKey cipherKey = new SecretKeySpec(keyVal, "AES");
        SecretKey toBeWrappedKey = new SecretKeySpec(dataVal, "AES");

        c1.init(Cipher.WRAP_MODE, cipherKey);
        IvParameterSpec ivSpec = new IvParameterSpec(c1.getIV());
        c2.init(Cipher.WRAP_MODE, cipherKey, ivSpec);
        AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
        params.init(ivSpec);
        c3.init(Cipher.WRAP_MODE, cipherKey, params);

        // first test WRAP with known values
        byte[] wrapped = c1.wrap(toBeWrappedKey);
        byte[] wrapped2 = c2.wrap(toBeWrappedKey);
        byte[] wrapped3 = c3.wrap(toBeWrappedKey);

        byte[] expectedVal = toBytes(expected, expected.length());

        if (!Arrays.equals(wrapped, expectedVal) ||
                !Arrays.equals(wrapped2, expectedVal) ||
                !Arrays.equals(wrapped3, expectedVal)) {
            throw new Exception("Wrap test failed; got different result");
        }

        // then test UNWRAP and compare with the initial values
        c1.init(Cipher.UNWRAP_MODE, cipherKey);
        ivSpec = new IvParameterSpec(c1.getIV());
        c2.init(Cipher.UNWRAP_MODE, cipherKey, ivSpec);
        params = AlgorithmParameters.getInstance("AES");
        params.init(ivSpec);
        c3.init(Cipher.UNWRAP_MODE, cipherKey, params);

        Key unwrapped = c1.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
        Key unwrapped2 = c2.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
        Key unwrapped3 = c3.unwrap(wrapped, "AES", Cipher.SECRET_KEY);

        if (!Arrays.equals(unwrapped.getEncoded(), dataVal) ||
                !Arrays.equals(unwrapped2.getEncoded(), dataVal) ||
                !Arrays.equals(unwrapped3.getEncoded(), dataVal)) {
            throw new Exception("Unwrap failed; got different result");
        }
    }

    @Test(dataProvider = "testData")
    public void testEnc(String algo, String key, int keyLen, String data, int dataLen, String expected)
            throws Exception {
        System.out.println("Testing " +  algo + " Cipher with enc " +
            dataLen + "-byte data with " + 8*keyLen + "-bit KEK");
        int allowed = Cipher.getMaxAllowedKeyLength("AES");
        if (keyLen > allowed) {
            System.out.println("=> skip, exceeds max allowed size " + allowed);
            return;
        }
        Cipher c1 = Cipher.getInstance(algo, "SunJCE");
        Cipher c2 = Cipher.getInstance(algo, "SunJCE");
        Cipher c3 = Cipher.getInstance(algo, "SunJCE");

        byte[] keyVal = toBytes(key, keyLen << 1);
        byte[] dataVal = toBytes(data, dataLen << 1);

        SecretKey cipherKey = new SecretKeySpec(keyVal, "AES");
        c1.init(Cipher.ENCRYPT_MODE, cipherKey);
        IvParameterSpec ivSpec = new IvParameterSpec(c1.getIV());
        c2.init(Cipher.ENCRYPT_MODE, cipherKey, ivSpec);
        AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
        params.init(ivSpec);
        c3.init(Cipher.ENCRYPT_MODE, cipherKey, params);

        // first test encryption with known values
        byte[] ct11 = c1.update(dataVal);
        byte[] ct12 = c1.doFinal();
        byte[] ct2 = c1.doFinal(dataVal);
        byte[] ct22 = c2.doFinal(dataVal);
        byte[] ct32 = c3.doFinal(dataVal);

        byte[] expectedVal = toBytes(expected, expected.length());

        if (ct11 != null || !Arrays.equals(ct12, ct2) ||
                !Arrays.equals(ct2, expectedVal) ||
                !Arrays.equals(ct22, expectedVal) ||
                !Arrays.equals(ct32, expectedVal)) {
            throw new Exception("Encryption failed; got different result");
        }

        // then test decryption and compare with the initial values
        c1.init(Cipher.DECRYPT_MODE, cipherKey);
        ivSpec = new IvParameterSpec(c1.getIV());
        c2.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);
        params = AlgorithmParameters.getInstance("AES");
        params.init(ivSpec);
        c3.init(Cipher.DECRYPT_MODE, cipherKey, params);

        byte[] pt11 = c1.update(ct12);
        byte[] pt12 = c1.doFinal();
        byte[] pt2 = c1.doFinal(ct2);
        byte[] pt22 = c2.doFinal(ct2);
        byte[] pt32 = c3.doFinal(ct2);

        if (pt11 != null || !Arrays.equals(pt12, pt2) ||
                !Arrays.equals(pt2, dataVal) || !Arrays.equals(pt22, dataVal) ||
                !Arrays.equals(pt32, dataVal)) {
            throw new Exception("Decryption failed; got different result");
        }
    }
}
