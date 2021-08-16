/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5008159 5008156
 * @summary Verify that the two key wrap ciphers, i.e. "DESedeWrap"
 * and "AESWrap", work as expected.
 * @run main XMLEncKAT
 * @author Valerie Peng
 */
import java.util.Base64;
import java.security.Key;
import java.security.AlgorithmParameters;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

public class XMLEncKAT {

    private static byte[] desEdeKey_1;
    private static byte[] aes128Key_1;
    private static byte[] aes192Key_1;
    private static byte[] aes256Key_1;
    private static byte[] desEdeKey_2;
    private static byte[] aes128Key_2;
    private static byte[] aes192Key_2;
    private static byte[] aes256Key_2;

    private static Base64.Decoder base64D = Base64.getDecoder();
    private static Base64.Encoder base64E = Base64.getEncoder();

    static {
        try {
            desEdeKey_1 = "abcdefghijklmnopqrstuvwx".getBytes("ASCII");
            aes128Key_1 = "abcdefghijklmnop".getBytes("ASCII");
            aes192Key_1 = "abcdefghijklmnopqrstuvwx".getBytes("ASCII");
            aes256Key_1 = "abcdefghijklmnopqrstuvwxyz012345".getBytes("ASCII");
        } catch (UnsupportedEncodingException uee) {
            // should never happen
        }

        desEdeKey_2 = base64D.decode
            ("yI+J1f3puYAERjIcT6vfg6RitmKX8nD0");
        aes128Key_2 = base64D.decode
            ("01+yuQ2huPS1+Qv0LH+zaQ==");
        aes192Key_2 = base64D.decode
            ("IlfuS40LvStVU0Mj8ePrrGHVhAb48y++");
        aes256Key_2 = base64D.decode
            ("ZhZ4v3RlwTlCEOpIrHfLKVyJOBDtEJOOQDat/4xR1bA=");

    }
    private static String[] desEdeWrappedKey_1 = {
        "ZyJbVsjRM4MEsswwwHz57aUz1eMqZHuEIoEPGS47CcmLvhuCtlzWZ9S/WcVJZIpz",
        "gHMpx5iF7+KXtNHLasZrkcLHn8Ti4rxUjCIRK+IcgbQir6FUsQ/uxQ3o8enEMWq1"
    };
    private static String[] desEdeWrappedKey_2 = {
        "/PZvvn42E9dmMUZ8KCY6B5XtLaaIaG4X5YNDwgV5Vlo=",
        "HgVuHoXxBQWD9fvi0gt9TanywZ5lJokM/12fcMG6gRoMjsCPulH+4A=="
    };
    private static String[] aes128WrappedKey_1 = {
        "dV45TUpJbidb9iKa34xj1WVtTZ036cnqvym2TBJWR5c=",
        "rPnY/XoSGCbuwy7vpslf29rs9dbvSCmGFOjEs3LT6g/qyZjfDA+2fQ=="
    };
    private static String[] aes128WrappedKey_2 = {
        "GPl6bneL1jKl0/lGnf9gejlYHRI6XxFz"
    };
    private static String[] aes192WrappedKey_1 = {
        "IbjZH7Mq564oMybpvCHWYM/5ER3eFsAV",
        "19D633XVohP6UJvaVRAhJek+ahtM3gOiVs6nZyAasDEb+WCUQOcWZw=="
    };
    private static String[] aes192WrappedKey_2 = {
        "5+GpVUQNTAT3uY8pPedEg/PpftiX+fJsTCun+fgmIz0=",
        "iuZvvGBWScikHld9TtNIOz0Sm7Srg5AcxOBMA8qIvQY=",
        "PeDwjnCsg6xWzs3SmzUtc2nyUz28nGu7"
    };
    private static String[] aes256WrappedKey_1 = {
        "4AAgyi3M7xNdBimbQZKdGJLn3/cS4Yv8QKuA01+gUnY=",
        "tPCC89jQShB+WDINCdRfKgf8wTlAx8xRXD73RmEHPBfix8zS1N82KQ==",
        "bsL63D0hPN6EOyzdgfEmKsAAvoJiGM+Wp9a9KZM92IKdl7s3YSntRg=="
    };
    private static String[] aes256WrappedKey_2 = {
        "IbnoS1cvuIFIGB46jj1V1FGftc92irrCwcC7BoBvxwQ=",
        "ic+Om6/3ZKcThVN3iv9lUEankNkDv3Et",
        "jOvQe4SxDqEMvAHcmb3Z+/Uedj23pvL6BRQsl2sjJlQ=",
        "IMwdsyg89IZ4Txf1SYYZNKUOKuYdDoIi/zEKXCjj4j9PM6BdkZligA=="
    };

    public static void testKeyWrap(String cAlg, byte[] cKeyVal,
        String cKeyAlg, String[] base64Wrapped) throws Exception {
        System.out.println("Testing " + cAlg + " Cipher with " +
            8*cKeyVal.length + "-bit key");
        Cipher c = Cipher.getInstance(cAlg, "SunJCE");
        SecretKey cKey = new SecretKeySpec(cKeyVal, cKeyAlg);
        c.init(Cipher.UNWRAP_MODE, cKey);
        Key[] key = new SecretKey[base64Wrapped.length];
        IvParameterSpec[] params =
            new IvParameterSpec[base64Wrapped.length];
        // first test UNWRAP with known values
        for (int i = 0; i < base64Wrapped.length; i++) {
            byte[] wrappedKey = base64D.decode(base64Wrapped[i]);
            key[i] = c.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
            if (c.getIV() != null) {
                params[i] = new IvParameterSpec(c.getIV());
            }
        }
        // then test WRAP and compare with the known values
        for (int i = 0; i < key.length; i++) {
            c.init(Cipher.WRAP_MODE, cKey, params[i]);
            byte[] wrapped2 = c.wrap(key[i]);
            String out = base64E.encodeToString(wrapped2);
            if (!out.equalsIgnoreCase(base64Wrapped[i])) {
                throw new Exception("Wrap failed; got " + out + ", expect " +
                                   base64Wrapped[i]);
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        String wrapAlg = "DESedeWrap";
        String keyAlg = "DESede";
        testKeyWrap(wrapAlg, desEdeKey_1, keyAlg, desEdeWrappedKey_1);
        testKeyWrap(wrapAlg, desEdeKey_2, keyAlg, desEdeWrappedKey_2);

        wrapAlg = "AESWrap";
        keyAlg = "AES";
        testKeyWrap(wrapAlg, aes128Key_1, keyAlg, aes128WrappedKey_1);
        testKeyWrap(wrapAlg, aes128Key_2, keyAlg, aes128WrappedKey_2);
        // only run the tests on longer key lengths if unlimited version
        // of JCE jurisdiction policy files are installed
        if (Cipher.getMaxAllowedKeyLength(keyAlg) == Integer.MAX_VALUE) {
            testKeyWrap(wrapAlg, aes192Key_1, keyAlg, aes192WrappedKey_1);
            testKeyWrap(wrapAlg, aes192Key_2, keyAlg, aes192WrappedKey_2);
            testKeyWrap(wrapAlg, aes256Key_1, keyAlg, aes256WrappedKey_1);
            testKeyWrap(wrapAlg, aes256Key_2, keyAlg, aes256WrappedKey_2);
        }
        System.out.println("All Tests Passed");
    }
}
