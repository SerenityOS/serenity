/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.StringJoiner;

public enum PBEAlgorithm {
    MD5_DES("PBEWithMD5ANDdes", "", "", AbstractPBEWrapper.DEFAULT),
    MD5_DES_CBC_PKCS5("PBEWithMD5AndDES", "CBC", "PKCS5Padding",
            AbstractPBEWrapper.DEFAULT),
    MD5_TRIPLEDES("PBEWithMD5ANDtripledes", "", "", AbstractPBEWrapper.DEFAULT),
    MD5_TRIPLEDES_CBC_PKCS5("PBEWithMD5AndTRIPLEDES", "CBC", "PKCS5Padding",
            AbstractPBEWrapper.DEFAULT),
    SHA1_DESEDE("PBEwithSHA1AndDESede", "", "", AbstractPBEWrapper.DEFAULT),
    SHA1_DESEDE_CBC_PKCS5("PBEwithSHA1AndDESede", "CBC", "PKCS5Padding",
            AbstractPBEWrapper.DEFAULT),
    SHA1_RC2_40("PBEwithSHA1AndRC2_40", "", "", AbstractPBEWrapper.DEFAULT),
    SHA1_RC2_40_PKCS5("PBEwithSHA1Andrc2_40", "CBC", "PKCS5Padding",
            AbstractPBEWrapper.DEFAULT),
    SHA1_RC2_128("PBEWithSHA1AndRC2_128", "", "", AbstractPBEWrapper.DEFAULT),
    SHA1_RC2_128_PKCS5("PBEWithSHA1andRC2_128", "CBC", "PKCS5Padding",
            AbstractPBEWrapper.DEFAULT),
    SHA1_RC4_40("PBEWithSHA1AndRC4_40", "", "", AbstractPBEWrapper.DEFAULT),
    SHA1_RC4_40_ECB_NOPADDING("PBEWithsha1AndRC4_40", "ECB", "NoPadding",
            AbstractPBEWrapper.DEFAULT),
    SHA1_RC4_128("PBEWithSHA1AndRC4_128", "", "", AbstractPBEWrapper.DEFAULT),
    SHA1_RC4_128_ECB_NOPADDING("pbeWithSHA1AndRC4_128", "ECB", "NoPadding",
            AbstractPBEWrapper.DEFAULT),
    HMAC_SHA1_AES_128("PBEWithHmacSHA1AndAES_128", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA224_AES_128("PBEWithHmacSHA224AndAES_128", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA256_AES_128("PBEWithHmacSHA256AndAES_128", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA384_AES_128("PBEWithHmacSHA384AndAES_128", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA512_AES_128("PBEWithHmacSHA512AndAES_128", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA1_AES_256("PBEWithHmacSHA1AndAES_256", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA224_AES_256("PBEWithHmacSHA224AndAES_256", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA256_AES_256("PBEWithHmacSHA256AndAES_256", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA384_AES_256("PBEWithHmacSHA384AndAES_256", "", "", AbstractPBEWrapper.AES),
    HMAC_SHA512_AES_256("PBEWithHmacSHA512AndAES_256", "", "", AbstractPBEWrapper.AES),
    PBKDF_HMAC_SHA1("PBKDF2WithHmacSHA1", "", "", AbstractPBEWrapper.PBKDF2),
    PBKDF_HMAC_SHA224("PBKDF2WithHmacSHA224", "", "", AbstractPBEWrapper.PBKDF2),
    PBKDF_HMAC_SHA256("PBKDF2WithHmacSHA256", "", "", AbstractPBEWrapper.PBKDF2),
    PBKDF_HMAC_SHA384("PBKDF2WithHmacSHA384", "", "", AbstractPBEWrapper.PBKDF2),
    PBKDF_HMAC_SHA512("PBKDF2WithHmacSHA512", "", "", AbstractPBEWrapper.PBKDF2);
    final String baseAlgo;
    final String mode;
    final String padding;
    final String type;

    PBEAlgorithm(String alg, String mode, String padding, String type) {
        this.baseAlgo = alg;
        this.mode = mode;
        this.padding = padding;
        this.type = type;
    }

    public String getTransformation() {
        StringJoiner sj = new StringJoiner("/");
        sj.add(baseAlgo);
        if (!mode.equals("")) {
            sj.add(this.mode);
        }
        if (!padding.equals("")) {
            sj.add(this.padding);
        }
        return sj.toString();
    }
}
