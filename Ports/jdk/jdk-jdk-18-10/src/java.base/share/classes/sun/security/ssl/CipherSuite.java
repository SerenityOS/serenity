/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.util.*;

import static sun.security.ssl.CipherSuite.HashAlg.*;
import static sun.security.ssl.CipherSuite.KeyExchange.*;
import static sun.security.ssl.CipherSuite.MacAlg.*;
import static sun.security.ssl.SSLCipher.*;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import static sun.security.ssl.NamedGroup.NamedGroupSpec.*;

/**
 * Enum for SSL/(D)TLS cipher suites.
 *
 * Please refer to the "TLS Cipher Suite Registry" section for more details
 * about each cipher suite:
 *     https://www.iana.org/assignments/tls-parameters/tls-parameters.xml
 */
enum CipherSuite {
    //
    // in preference order
    //

    // Definition of the CipherSuites that are enabled by default.
    //
    // They are listed in preference order, most preferred first, using
    // the following criteria:
    // 1. Prefer Suite B compliant cipher suites, see RFC6460 (To be
    //    changed later, see below).
    // 2. Prefer forward secrecy cipher suites.
    // 3. Prefer the stronger bulk cipher, in the order of AES_256(GCM),
    //    AES_128(GCM), AES_256, AES_128, 3DES-EDE.
    // 4. Prefer the stronger MAC algorithm, in the order of SHA384,
    //    SHA256, SHA, MD5.
    // 5. Prefer the better performance of key exchange and digital
    //    signature algorithm, in the order of ECDHE-ECDSA, ECDHE-RSA,
    //    DHE-RSA, DHE-DSS, ECDH-ECDSA, ECDH-RSA, RSA.

    // TLS 1.3 cipher suites.
    TLS_AES_256_GCM_SHA384(
            0x1302, true, "TLS_AES_256_GCM_SHA384",
            ProtocolVersion.PROTOCOLS_OF_13, B_AES_256_GCM_IV, H_SHA384),
    TLS_AES_128_GCM_SHA256(
            0x1301, true, "TLS_AES_128_GCM_SHA256",
            ProtocolVersion.PROTOCOLS_OF_13, B_AES_128_GCM_IV, H_SHA256),
    TLS_CHACHA20_POLY1305_SHA256(
            0x1303, true, "TLS_CHACHA20_POLY1305_SHA256",
            ProtocolVersion.PROTOCOLS_OF_13, B_CC20_P1305, H_SHA256),

    // Suite B compliant cipher suites, see RFC 6460.
    //
    // Note that, at present this provider is not Suite B compliant. The
    // preference order of the GCM cipher suites does not follow the spec
    // of RFC 6460.  In this section, only two cipher suites are listed
    // so that applications can make use of Suite-B compliant cipher
    // suite firstly.
    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384(
            0xC02C, true, "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_ECDSA, B_AES_256_GCM, M_NULL, H_SHA384),
    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256(
            0xC02B, true, "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_ECDSA, B_AES_128_GCM, M_NULL, H_SHA256),

    // Not suite B, but we want it to position the suite early in the list
    // of 1.2 suites.
    TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCA9, true, "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_ECDSA, B_CC20_P1305, M_NULL, H_SHA256),

    //
    // Forward secrecy cipher suites.
    //

    // AES_256(GCM) - ECDHE
    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384(
            0xC030, true, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_RSA, B_AES_256_GCM, M_NULL, H_SHA384),
    TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCA8, true, "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_RSA, B_CC20_P1305, M_NULL, H_SHA256),

    // AES_128(GCM) - ECDHE
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256(
            0xC02F, true, "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_RSA, B_AES_128_GCM, M_NULL, H_SHA256),

    // AES_256(GCM) - DHE
    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384(
            0x009F, true, "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_RSA, B_AES_256_GCM, M_NULL, H_SHA384),
    TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCAA, true, "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_RSA, B_CC20_P1305, M_NULL, H_SHA256),
    TLS_DHE_DSS_WITH_AES_256_GCM_SHA384(
            0x00A3, true, "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_DSS, B_AES_256_GCM, M_NULL, H_SHA384),

    // AES_128(GCM) - DHE
    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256(
            0x009E, true, "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_RSA, B_AES_128_GCM, M_NULL, H_SHA256),
    TLS_DHE_DSS_WITH_AES_128_GCM_SHA256(
            0x00A2, true, "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_DSS, B_AES_128_GCM, M_NULL, H_SHA256),

    // AES_256(CBC) - ECDHE
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384(
            0xC024, true, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_ECDSA, B_AES_256, M_SHA384, H_SHA384),
    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384(
            0xC028, true, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_RSA, B_AES_256, M_SHA384, H_SHA384),

    // AES_128(CBC) - ECDHE
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256(
            0xC023, true, "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_ECDSA, B_AES_128, M_SHA256, H_SHA256),
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256(
            0xC027, true, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDHE_RSA, B_AES_128, M_SHA256, H_SHA256),

    // AES_256(CBC) - DHE
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256(
            0x006B, true, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_RSA, B_AES_256, M_SHA256, H_SHA256),
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA256(
            0x006A, true, "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_DSS, B_AES_256, M_SHA256, H_SHA256),

    // AES_128(CBC) - DHE
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256(
            0x0067, true, "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_RSA, B_AES_128, M_SHA256, H_SHA256),
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA256(
            0x0040, true, "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DHE_DSS, B_AES_128, M_SHA256, H_SHA256),

    //
    // not forward secret cipher suites.
    //

    // AES_256(GCM)
    TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384(
            0xC02E, true, "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_ECDSA, B_AES_256_GCM, M_NULL, H_SHA384),
    TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384(
            0xC032, true, "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_RSA, B_AES_256_GCM, M_NULL, H_SHA384),

    // AES_128(GCM)
    TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256(
            0xC02D, true, "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_ECDSA, B_AES_128_GCM, M_NULL, H_SHA256),
    TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256(
            0xC031, true, "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_RSA, B_AES_128_GCM, M_NULL, H_SHA256),

    // AES_256(CBC)
    TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384(
            0xC026, true, "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_ECDSA, B_AES_256, M_SHA384, H_SHA384),
    TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384(
            0xC02A, true, "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_RSA, B_AES_256, M_SHA384, H_SHA384),

    // AES_128(CBC)
    TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256(
            0xC025, true, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_ECDSA, B_AES_128, M_SHA256, H_SHA256),
    TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256(
            0xC029, true, "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_ECDH_RSA, B_AES_128, M_SHA256, H_SHA256),

    //
    // Legacy, used for compatibility
    //

    // AES_256(CBC) - ECDHE - Using SHA
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA(
            0xC00A, true, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_ECDSA, B_AES_256, M_SHA, H_SHA256),
    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA(
            0xC014, true, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_RSA, B_AES_256, M_SHA, H_SHA256),

    // AES_128(CBC) - ECDHE - using SHA
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA(
            0xC009, true, "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_ECDSA, B_AES_128, M_SHA, H_SHA256),
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA(
            0xC013, true, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_RSA, B_AES_128, M_SHA, H_SHA256),

    // AES_256(CBC) - DHE - Using SHA
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA(
            0x0039, true, "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_RSA, B_AES_256, M_SHA, H_SHA256),
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA(
            0x0038, true, "TLS_DHE_DSS_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_DSS, B_AES_256, M_SHA, H_SHA256),

    // AES_128(CBC) - DHE - using SHA
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA(
            0x0033, true, "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_RSA, B_AES_128, M_SHA, H_SHA256),
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA(
            0x0032, true, "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_DSS, B_AES_128, M_SHA, H_SHA256),

    // AES_256(CBC) - using SHA, not forward secrecy
    TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA(
            0xC005, true, "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ECDSA, B_AES_256, M_SHA, H_SHA256),
    TLS_ECDH_RSA_WITH_AES_256_CBC_SHA(
            0xC00F, true, "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_RSA, B_AES_256, M_SHA, H_SHA256),

    // AES_128(CBC) - using SHA, not forward secrecy
    TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA(
            0xC004, true, "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ECDSA, B_AES_128, M_SHA, H_SHA256),
    TLS_ECDH_RSA_WITH_AES_128_CBC_SHA(
            0xC00E, true, "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_RSA, B_AES_128, M_SHA, H_SHA256),

    //
    // deprecated, used for compatibility
    //

    // RSA, AES_256(GCM)
    TLS_RSA_WITH_AES_256_GCM_SHA384(
            0x009D, true, "TLS_RSA_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_RSA, B_AES_256_GCM, M_NULL, H_SHA384),

    // RSA, AES_128(GCM)
    TLS_RSA_WITH_AES_128_GCM_SHA256(
            0x009C, true, "TLS_RSA_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_RSA, B_AES_128_GCM, M_NULL, H_SHA256),

    // RSA, AES_256(CBC)
    TLS_RSA_WITH_AES_256_CBC_SHA256(
            0x003D, true, "TLS_RSA_WITH_AES_256_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_RSA, B_AES_256, M_SHA256, H_SHA256),

    // RSA, AES_128(CBC)
    TLS_RSA_WITH_AES_128_CBC_SHA256(
            0x003C, true, "TLS_RSA_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_RSA, B_AES_128, M_SHA256, H_SHA256),

    // RSA, AES_256(CBC) - using SHA, not forward secrecy
    TLS_RSA_WITH_AES_256_CBC_SHA(
            0x0035, true, "TLS_RSA_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_RSA, B_AES_256, M_SHA, H_SHA256),

    // RSA, AES_128(CBC) - using SHA, not forward secrecy
    TLS_RSA_WITH_AES_128_CBC_SHA(
            0x002F, true, "TLS_RSA_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_RSA, B_AES_128, M_SHA, H_SHA256),

    // 3DES_EDE, forward secrecy.
    TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA(
            0xC008, true, "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_ECDSA, B_3DES, M_SHA, H_SHA256),
    TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA(
            0xC012, true, "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_RSA, B_3DES, M_SHA, H_SHA256),
    SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA(
            0x0016, true, "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                          "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_RSA, B_3DES, M_SHA, H_SHA256),
    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA(
            0x0013, true, "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                          "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DHE_DSS, B_3DES, M_SHA, H_SHA256),

    // 3DES_EDE, not forward secrecy.
    TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA(
            0xC003, true, "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ECDSA, B_3DES, M_SHA, H_SHA256),
    TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA(
            0xC00D, true, "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_RSA, B_3DES, M_SHA, H_SHA256),
    SSL_RSA_WITH_3DES_EDE_CBC_SHA(
            0x000A, true, "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                          "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_RSA, B_3DES, M_SHA, H_SHA256),

    // Renegotiation protection request Signalling Cipher Suite Value (SCSV).
    TLS_EMPTY_RENEGOTIATION_INFO_SCSV(        //  RFC 5746, TLS 1.2 and prior
            0x00FF, true, "TLS_EMPTY_RENEGOTIATION_INFO_SCSV", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_SCSV, B_NULL, M_NULL, H_NONE),

    // Definition of the CipherSuites that are supported but not enabled
    // by default.
    // They are listed in preference order, preferred first, using the
    // following criteria:
    // 1. If a cipher suite has been obsoleted, we put it at the end of
    //    the list.
    // 2. Prefer the stronger bulk cipher, in the order of AES_256,
    //    AES_128, 3DES-EDE, RC-4, DES, DES40, RC4_40, NULL.
    // 3. Prefer the stronger MAC algorithm, in the order of SHA384,
    //    SHA256, SHA, MD5.
    // 4. Prefer the better performance of key exchange and digital
    //    signature algorithm, in the order of ECDHE-ECDSA, ECDHE-RSA,
    //    RSA, ECDH-ECDSA, ECDH-RSA, DHE-RSA, DHE-DSS, anonymous.
    TLS_DH_anon_WITH_AES_256_GCM_SHA384(
            0x00A7, false, "TLS_DH_anon_WITH_AES_256_GCM_SHA384", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DH_ANON, B_AES_256_GCM, M_NULL, H_SHA384),
    TLS_DH_anon_WITH_AES_128_GCM_SHA256(
            0x00A6, false, "TLS_DH_anon_WITH_AES_128_GCM_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DH_ANON, B_AES_128_GCM, M_NULL, H_SHA256),
    TLS_DH_anon_WITH_AES_256_CBC_SHA256(
            0x006D, false, "TLS_DH_anon_WITH_AES_256_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DH_ANON, B_AES_256, M_SHA256, H_SHA256),
    TLS_ECDH_anon_WITH_AES_256_CBC_SHA(
            0xC019, false, "TLS_ECDH_anon_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ANON, B_AES_256, M_SHA, H_SHA256),
    TLS_DH_anon_WITH_AES_256_CBC_SHA(
            0x003A, false, "TLS_DH_anon_WITH_AES_256_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DH_ANON, B_AES_256, M_SHA, H_SHA256),
    TLS_DH_anon_WITH_AES_128_CBC_SHA256(
            0x006C, false, "TLS_DH_anon_WITH_AES_128_CBC_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_DH_ANON, B_AES_128, M_SHA256, H_SHA256),
    TLS_ECDH_anon_WITH_AES_128_CBC_SHA(
            0xC018, false, "TLS_ECDH_anon_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ANON, B_AES_128, M_SHA, H_SHA256),
    TLS_DH_anon_WITH_AES_128_CBC_SHA(
            0x0034, false, "TLS_DH_anon_WITH_AES_128_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DH_ANON, B_AES_128, M_SHA, H_SHA256),
    TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA(
            0xC017, false, "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ANON, B_3DES, M_SHA, H_SHA256),
    SSL_DH_anon_WITH_3DES_EDE_CBC_SHA(
            0x001B, false, "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                           "TLS_DH_anon_WITH_3DES_EDE_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_DH_ANON, B_3DES, M_SHA, H_SHA256),

    // RC4
    TLS_ECDHE_ECDSA_WITH_RC4_128_SHA(
            0xC007, false, "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_ECDHE_ECDSA, B_RC4_128, M_SHA, H_SHA256),
    TLS_ECDHE_RSA_WITH_RC4_128_SHA(
            0xC011, false, "TLS_ECDHE_RSA_WITH_RC4_128_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_ECDHE_RSA, B_RC4_128, M_SHA, H_SHA256),
    SSL_RSA_WITH_RC4_128_SHA(
            0x0005, false, "SSL_RSA_WITH_RC4_128_SHA",
                           "TLS_RSA_WITH_RC4_128_SHA",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_RSA, B_RC4_128, M_SHA, H_SHA256),
    TLS_ECDH_ECDSA_WITH_RC4_128_SHA(
            0xC002, false, "TLS_ECDH_ECDSA_WITH_RC4_128_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_ECDH_ECDSA, B_RC4_128, M_SHA, H_SHA256),
    TLS_ECDH_RSA_WITH_RC4_128_SHA(
            0xC00C, false, "TLS_ECDH_RSA_WITH_RC4_128_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_ECDH_RSA, B_RC4_128, M_SHA, H_SHA256),
    SSL_RSA_WITH_RC4_128_MD5(
            0x0004, false, "SSL_RSA_WITH_RC4_128_MD5",
                           "TLS_RSA_WITH_RC4_128_MD5",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_RSA, B_RC4_128, M_MD5, H_SHA256),
    TLS_ECDH_anon_WITH_RC4_128_SHA(
            0xC016, false, "TLS_ECDH_anon_WITH_RC4_128_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_ECDH_ANON, B_RC4_128, M_SHA, H_SHA256),
    SSL_DH_anon_WITH_RC4_128_MD5(
            0x0018, false, "SSL_DH_anon_WITH_RC4_128_MD5",
                           "TLS_DH_anon_WITH_RC4_128_MD5",
            ProtocolVersion.PROTOCOLS_TO_TLS12,
            K_DH_ANON, B_RC4_128, M_MD5, H_SHA256),

    // Weak cipher suites obsoleted in TLS 1.2 [RFC 5246]
    SSL_RSA_WITH_DES_CBC_SHA(
            0x0009, false, "SSL_RSA_WITH_DES_CBC_SHA",
                           "TLS_RSA_WITH_DES_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_11,
            K_RSA, B_DES, M_SHA, H_NONE),
    SSL_DHE_RSA_WITH_DES_CBC_SHA(
            0x0015, false, "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                           "TLS_DHE_RSA_WITH_DES_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_11,
            K_DHE_RSA, B_DES, M_SHA, H_NONE),
    SSL_DHE_DSS_WITH_DES_CBC_SHA(
            0x0012, false, "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                           "TLS_DHE_DSS_WITH_DES_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_11,
            K_DHE_DSS, B_DES, M_SHA, H_NONE),
    SSL_DH_anon_WITH_DES_CBC_SHA(
            0x001A, false, "SSL_DH_anon_WITH_DES_CBC_SHA",
                           "TLS_DH_anon_WITH_DES_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_11,
            K_DH_ANON, B_DES, M_SHA, H_NONE),

    // Weak cipher suites obsoleted in TLS 1.1  [RFC 4346]
    SSL_RSA_EXPORT_WITH_DES40_CBC_SHA(
            0x0008, false, "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                           "TLS_RSA_EXPORT_WITH_DES40_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_RSA_EXPORT, B_DES_40, M_SHA, H_NONE),
    SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA(
            0x0014, false, "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                           "TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_DHE_RSA_EXPORT, B_DES_40, M_SHA, H_NONE),
    SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA(
            0x0011, false, "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                           "TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_DHE_DSS_EXPORT, B_DES_40, M_SHA, H_NONE),
    SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA(
            0x0019, false, "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                           "TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_DH_ANON_EXPORT, B_DES_40, M_SHA, H_NONE),
    SSL_RSA_EXPORT_WITH_RC4_40_MD5(
            0x0003, false, "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                           "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_RSA_EXPORT, B_RC4_40, M_MD5, H_NONE),
    SSL_DH_anon_EXPORT_WITH_RC4_40_MD5(
            0x0017, false, "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                           "TLS_DH_anon_EXPORT_WITH_RC4_40_MD5",
            ProtocolVersion.PROTOCOLS_TO_10,
            K_DH_ANON, B_RC4_40, M_MD5, H_NONE),

    // No traffic encryption cipher suites
    TLS_RSA_WITH_NULL_SHA256(
            0x003B, false, "TLS_RSA_WITH_NULL_SHA256", "",
            ProtocolVersion.PROTOCOLS_OF_12,
            K_RSA, B_NULL, M_SHA256, H_SHA256),
    TLS_ECDHE_ECDSA_WITH_NULL_SHA(
            0xC006, false, "TLS_ECDHE_ECDSA_WITH_NULL_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_ECDSA, B_NULL, M_SHA, H_SHA256),
    TLS_ECDHE_RSA_WITH_NULL_SHA(
            0xC010, false, "TLS_ECDHE_RSA_WITH_NULL_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDHE_RSA, B_NULL, M_SHA, H_SHA256),
    SSL_RSA_WITH_NULL_SHA(
            0x0002, false, "SSL_RSA_WITH_NULL_SHA",
                           "TLS_RSA_WITH_NULL_SHA",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_RSA, B_NULL, M_SHA, H_SHA256),
    TLS_ECDH_ECDSA_WITH_NULL_SHA(
            0xC001, false, "TLS_ECDH_ECDSA_WITH_NULL_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ECDSA, B_NULL, M_SHA, H_SHA256),
    TLS_ECDH_RSA_WITH_NULL_SHA(
            0xC00B, false, "TLS_ECDH_RSA_WITH_NULL_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_RSA, B_NULL, M_SHA, H_SHA256),
    TLS_ECDH_anon_WITH_NULL_SHA(
            0xC015, false, "TLS_ECDH_anon_WITH_NULL_SHA", "",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_ECDH_ANON, B_NULL, M_SHA, H_SHA256),
    SSL_RSA_WITH_NULL_MD5(
            0x0001, false, "SSL_RSA_WITH_NULL_MD5",
                           "TLS_RSA_WITH_NULL_MD5",
            ProtocolVersion.PROTOCOLS_TO_12,
            K_RSA, B_NULL, M_MD5, H_SHA256),

    // Definition of the cipher suites that are not supported but the names
    // are known.
    TLS_AES_128_CCM_SHA256(                          // TLS 1.3
            "TLS_AES_128_CCM_SHA256", 0x1304),
    TLS_AES_128_CCM_8_SHA256(                        // TLS 1.3
            "TLS_AES_128_CCM_8_SHA256", 0x1305),

    // Remaining unsupported cipher suites defined in RFC2246.
    CS_0006("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5",           0x0006),
    CS_0007("SSL_RSA_WITH_IDEA_CBC_SHA",                    0x0007),
    CS_000B("SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",         0x000b),
    CS_000C("SSL_DH_DSS_WITH_DES_CBC_SHA",                  0x000c),
    CS_000D("SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",             0x000d),
    CS_000E("SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",         0x000e),
    CS_000F("SSL_DH_RSA_WITH_DES_CBC_SHA",                  0x000f),
    CS_0010("SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",             0x0010),

    // SSL 3.0 Fortezza cipher suites
    CS_001C("SSL_FORTEZZA_DMS_WITH_NULL_SHA",               0x001c),
    CS_001D("SSL_FORTEZZA_DMS_WITH_FORTEZZA_CBC_SHA",       0x001d),

    // 1024/56 bit exportable cipher suites from expired internet draft
    CS_0062("SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA",          0x0062),
    CS_0063("SSL_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA",      0x0063),
    CS_0064("SSL_RSA_EXPORT1024_WITH_RC4_56_SHA",           0x0064),
    CS_0065("SSL_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA",       0x0065),
    CS_0066("SSL_DHE_DSS_WITH_RC4_128_SHA",                 0x0066),

    // Netscape old and new SSL 3.0 FIPS cipher suites
    // see http://www.mozilla.org/projects/security/pki/nss/ssl/fips-ssl-ciphersuites.html
    CS_FFE0("NETSCAPE_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",      0xffe0),
    CS_FFE1("NETSCAPE_RSA_FIPS_WITH_DES_CBC_SHA",           0xffe1),
    CS_FEFE("SSL_RSA_FIPS_WITH_DES_CBC_SHA",                0xfefe),
    CS_FEFF("SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",           0xfeff),

    // Unsupported Kerberos cipher suites from RFC 2712
    CS_001E("TLS_KRB5_WITH_DES_CBC_SHA",                    0x001E),
    CS_001F("TLS_KRB5_WITH_3DES_EDE_CBC_SHA",               0x001F),
    CS_0020("TLS_KRB5_WITH_RC4_128_SHA",                    0x0020),
    CS_0021("TLS_KRB5_WITH_IDEA_CBC_SHA",                   0x0021),
    CS_0022("TLS_KRB5_WITH_DES_CBC_MD5",                    0x0022),
    CS_0023("TLS_KRB5_WITH_3DES_EDE_CBC_MD5",               0x0023),
    CS_0024("TLS_KRB5_WITH_RC4_128_MD5",                    0x0024),
    CS_0025("TLS_KRB5_WITH_IDEA_CBC_MD5",                   0x0025),
    CS_0026("TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",          0x0026),
    CS_0027("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA",          0x0027),
    CS_0028("TLS_KRB5_EXPORT_WITH_RC4_40_SHA",              0x0028),
    CS_0029("TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",          0x0029),
    CS_002A("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5",          0x002a),
    CS_002B("TLS_KRB5_EXPORT_WITH_RC4_40_MD5",              0x002B),

    // Unsupported cipher suites from RFC 4162
    CS_0096("TLS_RSA_WITH_SEED_CBC_SHA",                    0x0096),
    CS_0097("TLS_DH_DSS_WITH_SEED_CBC_SHA",                 0x0097),
    CS_0098("TLS_DH_RSA_WITH_SEED_CBC_SHA",                 0x0098),
    CS_0099("TLS_DHE_DSS_WITH_SEED_CBC_SHA",                0x0099),
    CS_009A("TLS_DHE_RSA_WITH_SEED_CBC_SHA",                0x009a),
    CS_009B("TLS_DH_anon_WITH_SEED_CBC_SHA",                0x009b),

    // Unsupported cipher suites from RFC 4279
    CS_008A("TLS_PSK_WITH_RC4_128_SHA",                     0x008a),
    CS_008B("TLS_PSK_WITH_3DES_EDE_CBC_SHA",                0x008b),
    CS_008C("TLS_PSK_WITH_AES_128_CBC_SHA",                 0x008c),
    CS_008D("TLS_PSK_WITH_AES_256_CBC_SHA",                 0x008d),
    CS_008E("TLS_DHE_PSK_WITH_RC4_128_SHA",                 0x008e),
    CS_008F("TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA",            0x008f),
    CS_0090("TLS_DHE_PSK_WITH_AES_128_CBC_SHA",             0x0090),
    CS_0091("TLS_DHE_PSK_WITH_AES_256_CBC_SHA",             0x0091),
    CS_0092("TLS_RSA_PSK_WITH_RC4_128_SHA",                 0x0092),
    CS_0093("TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA",            0x0093),
    CS_0094("TLS_RSA_PSK_WITH_AES_128_CBC_SHA",             0x0094),
    CS_0095("TLS_RSA_PSK_WITH_AES_256_CBC_SHA",             0x0095),

    // Unsupported cipher suites from RFC 4785
    CS_002C("TLS_PSK_WITH_NULL_SHA",                        0x002c),
    CS_002D("TLS_DHE_PSK_WITH_NULL_SHA",                    0x002d),
    CS_002E("TLS_RSA_PSK_WITH_NULL_SHA",                    0x002e),

    // Unsupported cipher suites from RFC 5246
    CS_0030("TLS_DH_DSS_WITH_AES_128_CBC_SHA",              0x0030),
    CS_0031("TLS_DH_RSA_WITH_AES_128_CBC_SHA",              0x0031),
    CS_0036("TLS_DH_DSS_WITH_AES_256_CBC_SHA",              0x0036),
    CS_0037("TLS_DH_RSA_WITH_AES_256_CBC_SHA",              0x0037),
    CS_003E("TLS_DH_DSS_WITH_AES_128_CBC_SHA256",           0x003e),
    CS_003F("TLS_DH_RSA_WITH_AES_128_CBC_SHA256",           0x003f),
    CS_0068("TLS_DH_DSS_WITH_AES_256_CBC_SHA256",           0x0068),
    CS_0069("TLS_DH_RSA_WITH_AES_256_CBC_SHA256",           0x0069),

    // Unsupported cipher suites from RFC 5288
    CS_00A0("TLS_DH_RSA_WITH_AES_128_GCM_SHA256",           0x00a0),
    CS_00A1("TLS_DH_RSA_WITH_AES_256_GCM_SHA384",           0x00a1),
    CS_00A4("TLS_DH_DSS_WITH_AES_128_GCM_SHA256",           0x00a4),
    CS_00A5("TLS_DH_DSS_WITH_AES_256_GCM_SHA384",           0x00a5),

    // Unsupported cipher suites from RFC 5487
    CS_00A8("TLS_PSK_WITH_AES_128_GCM_SHA256",              0x00a8),
    CS_00A9("TLS_PSK_WITH_AES_256_GCM_SHA384",              0x00a9),
    CS_00AA("TLS_DHE_PSK_WITH_AES_128_GCM_SHA256",          0x00aa),
    CS_00AB("TLS_DHE_PSK_WITH_AES_256_GCM_SHA384",          0x00ab),
    CS_00AC("TLS_RSA_PSK_WITH_AES_128_GCM_SHA256",          0x00ac),
    CS_00AD("TLS_RSA_PSK_WITH_AES_256_GCM_SHA384",          0x00ad),
    CS_00AE("TLS_PSK_WITH_AES_128_CBC_SHA256",              0x00ae),
    CS_00AF("TLS_PSK_WITH_AES_256_CBC_SHA384",              0x00af),
    CS_00B0("TLS_PSK_WITH_NULL_SHA256",                     0x00b0),
    CS_00B1("TLS_PSK_WITH_NULL_SHA384",                     0x00b1),
    CS_00B2("TLS_DHE_PSK_WITH_AES_128_CBC_SHA256",          0x00b2),
    CS_00B3("TLS_DHE_PSK_WITH_AES_256_CBC_SHA384",          0x00b3),
    CS_00B4("TLS_DHE_PSK_WITH_NULL_SHA256",                 0x00b4),
    CS_00B5("TLS_DHE_PSK_WITH_NULL_SHA384",                 0x00b5),
    CS_00B6("TLS_RSA_PSK_WITH_AES_128_CBC_SHA256",          0x00b6),
    CS_00B7("TLS_RSA_PSK_WITH_AES_256_CBC_SHA384",          0x00b7),
    CS_00B8("TLS_RSA_PSK_WITH_NULL_SHA256",                 0x00b8),
    CS_00B9("TLS_RSA_PSK_WITH_NULL_SHA384",                 0x00b9),

    // Unsupported cipher suites from RFC 5932
    CS_0041("TLS_RSA_WITH_CAMELLIA_128_CBC_SHA",            0x0041),
    CS_0042("TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA",         0x0042),
    CS_0043("TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA",         0x0043),
    CS_0044("TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA",        0x0044),
    CS_0045("TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA",        0x0045),
    CS_0046("TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA",        0x0046),
    CS_0084("TLS_RSA_WITH_CAMELLIA_256_CBC_SHA",            0x0084),
    CS_0085("TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA",         0x0085),
    CS_0086("TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA",         0x0086),
    CS_0087("TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA",        0x0087),
    CS_0088("TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA",        0x0088),
    CS_0089("TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA",        0x0089),
    CS_00BA("TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256",         0x00ba),
    CS_00BB("TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256",      0x00bb),
    CS_00BC("TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA256",      0x00bc),
    CS_00BD("TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256",     0x00bd),
    CS_00BE("TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256",     0x00be),
    CS_00BF("TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256",     0x00bf),
    CS_00C0("TLS_RSA_WITH_CAMELLIA_256_CBC_SHA256",         0x00c0),
    CS_00C1("TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256",      0x00c1),
    CS_00C2("TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256",      0x00c2),
    CS_00C3("TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256",     0x00c3),
    CS_00C4("TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256",     0x00c4),
    CS_00C5("TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256",     0x00c5),

    // TLS Fallback Signaling Cipher Suite Value (SCSV) RFC 7507
    CS_5600("TLS_FALLBACK_SCSV",                            0x5600),

    // Unsupported cipher suites from RFC 5054
    CS_C01A("TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA",            0xc01a),
    CS_C01B("TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA",        0xc01b),
    CS_C01C("TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA",        0xc01c),
    CS_C01D("TLS_SRP_SHA_WITH_AES_128_CBC_SHA",             0xc01d),
    CS_C01E("TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA",         0xc01e),
    CS_C01F("TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA",         0xc01f),
    CS_C020("TLS_SRP_SHA_WITH_AES_256_CBC_SHA",             0xc020),
    CS_C021("TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA",         0xc021),
    CS_C022("TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA",         0xc022),

    // Unsupported cipher suites from RFC 5489
    CS_C033("TLS_ECDHE_PSK_WITH_RC4_128_SHA",               0xc033),
    CS_C034("TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA",          0xc034),
    CS_C035("TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA",           0xc035),
    CS_C036("TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA",           0xc036),
    CS_C037("TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256",        0xc037),
    CS_C038("TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384",        0xc038),
    CS_C039("TLS_ECDHE_PSK_WITH_NULL_SHA",                  0xc039),
    CS_C03A("TLS_ECDHE_PSK_WITH_NULL_SHA256",               0xc03a),
    CS_C03B("TLS_ECDHE_PSK_WITH_NULL_SHA384",               0xc03b),

    // Unsupported cipher suites from RFC 6209
    CS_C03C("TLS_RSA_WITH_ARIA_128_CBC_SHA256",             0xc03c),
    CS_C03D("TLS_RSA_WITH_ARIA_256_CBC_SHA384",             0xc03d),
    CS_C03E("TLS_DH_DSS_WITH_ARIA_128_CBC_SHA256",          0xc03e),
    CS_C03F("TLS_DH_DSS_WITH_ARIA_256_CBC_SHA384",          0xc03f),
    CS_C040("TLS_DH_RSA_WITH_ARIA_128_CBC_SHA256",          0xc040),
    CS_C041("TLS_DH_RSA_WITH_ARIA_256_CBC_SHA384",          0xc041),
    CS_C042("TLS_DHE_DSS_WITH_ARIA_128_CBC_SHA256",         0xc042),
    CS_C043("TLS_DHE_DSS_WITH_ARIA_256_CBC_SHA384",         0xc043),
    CS_C044("TLS_DHE_RSA_WITH_ARIA_128_CBC_SHA256",         0xc044),
    CS_C045("TLS_DHE_RSA_WITH_ARIA_256_CBC_SHA384",         0xc045),
    CS_C046("TLS_DH_anon_WITH_ARIA_128_CBC_SHA256",         0xc046),
    CS_C047("TLS_DH_anon_WITH_ARIA_256_CBC_SHA384",         0xc047),
    CS_C048("TLS_ECDHE_ECDSA_WITH_ARIA_128_CBC_SHA256",     0xc048),
    CS_C049("TLS_ECDHE_ECDSA_WITH_ARIA_256_CBC_SHA384",     0xc049),
    CS_C04A("TLS_ECDH_ECDSA_WITH_ARIA_128_CBC_SHA256",      0xc04a),
    CS_C04B("TLS_ECDH_ECDSA_WITH_ARIA_256_CBC_SHA384",      0xc04b),
    CS_C04C("TLS_ECDHE_RSA_WITH_ARIA_128_CBC_SHA256",       0xc04c),
    CS_C04D("TLS_ECDHE_RSA_WITH_ARIA_256_CBC_SHA384",       0xc04d),
    CS_C04E("TLS_ECDH_RSA_WITH_ARIA_128_CBC_SHA256",        0xc04e),
    CS_C04F("TLS_ECDH_RSA_WITH_ARIA_256_CBC_SHA384",        0xc04f),
    CS_C050("TLS_RSA_WITH_ARIA_128_GCM_SHA256",             0xc050),
    CS_C051("TLS_RSA_WITH_ARIA_256_GCM_SHA384",             0xc051),
    CS_C052("TLS_DHE_RSA_WITH_ARIA_128_GCM_SHA256",         0xc052),
    CS_C053("TLS_DHE_RSA_WITH_ARIA_256_GCM_SHA384",         0xc053),
    CS_C054("TLS_DH_RSA_WITH_ARIA_128_GCM_SHA256",          0xc054),
    CS_C055("TLS_DH_RSA_WITH_ARIA_256_GCM_SHA384",          0xc055),
    CS_C056("TLS_DHE_DSS_WITH_ARIA_128_GCM_SHA256",         0xc056),
    CS_C057("TLS_DHE_DSS_WITH_ARIA_256_GCM_SHA384",         0xc057),
    CS_C058("TLS_DH_DSS_WITH_ARIA_128_GCM_SHA256",          0xc058),
    CS_C059("TLS_DH_DSS_WITH_ARIA_256_GCM_SHA384",          0xc059),
    CS_C05A("TLS_DH_anon_WITH_ARIA_128_GCM_SHA256",         0xc05a),
    CS_C05B("TLS_DH_anon_WITH_ARIA_256_GCM_SHA384",         0xc05b),
    CS_C05C("TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256",     0xc05c),
    CS_C05D("TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384",     0xc05d),
    CS_C05E("TLS_ECDH_ECDSA_WITH_ARIA_128_GCM_SHA256",      0xc05e),
    CS_C05F("TLS_ECDH_ECDSA_WITH_ARIA_256_GCM_SHA384",      0xc05f),
    CS_C060("TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256",       0xc060),
    CS_C061("TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384",       0xc061),
    CS_C062("TLS_ECDH_RSA_WITH_ARIA_128_GCM_SHA256",        0xc062),
    CS_C063("TLS_ECDH_RSA_WITH_ARIA_256_GCM_SHA384",        0xc063),
    CS_C064("TLS_PSK_WITH_ARIA_128_CBC_SHA256",             0xc064),
    CS_C065("TLS_PSK_WITH_ARIA_256_CBC_SHA384",             0xc065),
    CS_C066("TLS_DHE_PSK_WITH_ARIA_128_CBC_SHA256",         0xc066),
    CS_C067("TLS_DHE_PSK_WITH_ARIA_256_CBC_SHA384",         0xc067),
    CS_C068("TLS_RSA_PSK_WITH_ARIA_128_CBC_SHA256",         0xc068),
    CS_C069("TLS_RSA_PSK_WITH_ARIA_256_CBC_SHA384",         0xc069),
    CS_C06A("TLS_PSK_WITH_ARIA_128_GCM_SHA256",             0xc06a),
    CS_C06B("TLS_PSK_WITH_ARIA_256_GCM_SHA384",             0xc06b),
    CS_C06C("TLS_DHE_PSK_WITH_ARIA_128_GCM_SHA256",         0xc06c),
    CS_C06D("TLS_DHE_PSK_WITH_ARIA_256_GCM_SHA384",         0xc06d),
    CS_C06E("TLS_RSA_PSK_WITH_ARIA_128_GCM_SHA256",         0xc06e),
    CS_C06F("TLS_RSA_PSK_WITH_ARIA_256_GCM_SHA384",         0xc06f),
    CS_C070("TLS_ECDHE_PSK_WITH_ARIA_128_CBC_SHA256",       0xc070),
    CS_C071("TLS_ECDHE_PSK_WITH_ARIA_256_CBC_SHA384",       0xc071),

    // Unsupported cipher suites from RFC 6367
    CS_C072("TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256", 0xc072),
    CS_C073("TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384", 0xc073),
    CS_C074("TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256",  0xc074),
    CS_C075("TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384",  0xc075),
    CS_C076("TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256",   0xc076),
    CS_C077("TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384",   0xc077),
    CS_C078("TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256",    0xc078),
    CS_C079("TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384",    0xc079),
    CS_C07A("TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256",         0xc07a),
    CS_C07B("TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384",         0xc07b),
    CS_C07C("TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256",     0xc07c),
    CS_C07D("TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384",     0xc07d),
    CS_C07E("TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256",      0xc07e),
    CS_C07F("TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384",      0xc07f),
    CS_C080("TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256",     0xc080),
    CS_C081("TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384",     0xc081),
    CS_C082("TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256",      0xc082),
    CS_C083("TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384",      0xc083),
    CS_C084("TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256",     0xc084),
    CS_C085("TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384",     0xc085),
    CS_C086("TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256", 0xc086),
    CS_C087("TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384", 0xc087),
    CS_C088("TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256",  0xc088),
    CS_C089("TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384",  0xc089),
    CS_C08A("TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256",   0xc08a),
    CS_C08B("TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384",   0xc08b),
    CS_C08C("TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256",    0xc08c),
    CS_C08D("TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384",    0xc08d),
    CS_C08E("TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256",         0xc08e),
    CS_C08F("TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384",         0xc08f),
    CS_C090("TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256",     0xc090),
    CS_C091("TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384",     0xc091),
    CS_C092("TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256",     0xc092),
    CS_C093("TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384",     0xc093),
    CS_C094("TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256",         0xc094),
    CS_C095("TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384",         0xc095),
    CS_C096("TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256",     0xc096),
    CS_C097("TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384",     0xc097),
    CS_C098("TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256",     0xc098),
    CS_C099("TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384",     0xc099),
    CS_C09A("TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256",   0xc09a),
    CS_C09B("TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384",   0xc09b),

    // Unsupported cipher suites from RFC 6655
    CS_C09C("TLS_RSA_WITH_AES_128_CCM",                     0xc09c),
    CS_C09D("TLS_RSA_WITH_AES_256_CCM",                     0xc09d),
    CS_C09E("TLS_DHE_RSA_WITH_AES_128_CCM",                 0xc09e),
    CS_C09F("TLS_DHE_RSA_WITH_AES_256_CCM",                 0xc09f),
    CS_C0A0("TLS_RSA_WITH_AES_128_CCM_8",                   0xc0A0),
    CS_C0A1("TLS_RSA_WITH_AES_256_CCM_8",                   0xc0A1),
    CS_C0A2("TLS_DHE_RSA_WITH_AES_128_CCM_8",               0xc0A2),
    CS_C0A3("TLS_DHE_RSA_WITH_AES_256_CCM_8",               0xc0A3),
    CS_C0A4("TLS_PSK_WITH_AES_128_CCM",                     0xc0A4),
    CS_C0A5("TLS_PSK_WITH_AES_256_CCM",                     0xc0A5),
    CS_C0A6("TLS_DHE_PSK_WITH_AES_128_CCM",                 0xc0A6),
    CS_C0A7("TLS_DHE_PSK_WITH_AES_256_CCM",                 0xc0A7),
    CS_C0A8("TLS_PSK_WITH_AES_128_CCM_8",                   0xc0A8),
    CS_C0A9("TLS_PSK_WITH_AES_256_CCM_8",                   0xc0A9),
    CS_C0AA("TLS_PSK_DHE_WITH_AES_128_CCM_8",               0xc0Aa),
    CS_C0AB("TLS_PSK_DHE_WITH_AES_256_CCM_8",               0xc0Ab),

    // Unsupported cipher suites from RFC 7251
    CS_C0AC("TLS_ECDHE_ECDSA_WITH_AES_128_CCM",             0xc0Ac),
    CS_C0AD("TLS_ECDHE_ECDSA_WITH_AES_256_CCM",             0xc0Ad),
    CS_C0AE("TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8",           0xc0Ae),
    CS_C0AF("TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8",           0xc0Af),

    C_NULL("SSL_NULL_WITH_NULL_NULL", 0x0000);

    final int id;
    final boolean isDefaultEnabled;
    final String name;
    final List<String> aliases;
    final List<ProtocolVersion> supportedProtocols;
    final KeyExchange keyExchange;
    final SSLCipher bulkCipher;
    final MacAlg macAlg;
    final HashAlg hashAlg;

    final boolean exportable;

    private static final Map<Integer, CipherSuite> cipherSuiteIds;
    private static final Map<String, CipherSuite> cipherSuiteNames;
    private static final List<CipherSuite> allowedCipherSuites;
    private static final List<CipherSuite> defaultCipherSuites;

    static {
        Map<Integer, CipherSuite> ids = new HashMap<>();
        Map<String, CipherSuite> names = new HashMap<>();
        List<CipherSuite> allowedCS = new ArrayList<>();
        List<CipherSuite> defaultCS = new ArrayList<>();

        for(CipherSuite cs : CipherSuite.values()) {
            ids.put(cs.id, cs);
            names.put(cs.name, cs);
            for (String alias : cs.aliases) {
                names.put(alias, cs);
            }

            if (!cs.supportedProtocols.isEmpty()) {
                allowedCS.add(cs);
            }

            if (cs.isDefaultEnabled) {
                defaultCS.add(cs);
            }
        }

        cipherSuiteIds = Map.copyOf(ids);
        cipherSuiteNames = Map.copyOf(names);
        allowedCipherSuites = List.copyOf(allowedCS);
        defaultCipherSuites = List.copyOf(defaultCS);
    }

    // known but unsupported cipher suite
    private CipherSuite(String name, int id) {
        this(id, false, name, "",
                ProtocolVersion.PROTOCOLS_EMPTY, null, null, null, null);
    }

    // TLS 1.3 cipher suite
    private CipherSuite(int id, boolean isDefaultEnabled,
            String name, ProtocolVersion[] supportedProtocols,
            SSLCipher bulkCipher, HashAlg hashAlg) {
        this(id, isDefaultEnabled, name, "",
                supportedProtocols, null, bulkCipher, M_NULL, hashAlg);
    }

    private CipherSuite(int id, boolean isDefaultEnabled,
            String name, String aliases,
            ProtocolVersion[] supportedProtocols,
            KeyExchange keyExchange, SSLCipher cipher,
            MacAlg macAlg, HashAlg hashAlg) {
        this.id = id;
        this.isDefaultEnabled = isDefaultEnabled;
        this.name = name;
        if (!aliases.isEmpty()) {
            this.aliases = Arrays.asList(aliases.split(","));
        } else {
            this.aliases = Collections.emptyList();
        }
        this.supportedProtocols = Arrays.asList(supportedProtocols);
        this.keyExchange = keyExchange;
        this.bulkCipher = cipher;
        this.macAlg = macAlg;
        this.hashAlg = hashAlg;

        this.exportable = (cipher != null && cipher.exportable);
    }

    static CipherSuite nameOf(String ciperSuiteName) {
        return cipherSuiteNames.get(ciperSuiteName);
    }

    static CipherSuite valueOf(int id) {
        return cipherSuiteIds.get(id);
    }

    static String nameOf(int id) {
        CipherSuite cs = cipherSuiteIds.get(id);

        if (cs != null) {
            return cs.name;
        }

        return "UNKNOWN-CIPHER-SUITE(" + Utilities.byte16HexString(id) + ")";
    }

    static Collection<CipherSuite> allowedCipherSuites() {
        return allowedCipherSuites;
    }

    static Collection<CipherSuite> defaultCipherSuites() {
        return defaultCipherSuites;
    }

    /**
     * Validates and converts an array of cipher suite names.
     *
     * @throws IllegalArgumentException when one or more of the ciphers named
     *         by the parameter is not supported, or when the parameter is null.
     */
    static List<CipherSuite> validValuesOf(String[] names) {
        if (names == null) {
            throw new IllegalArgumentException("CipherSuites cannot be null");
        }

        List<CipherSuite> cipherSuites = new ArrayList<>(names.length);
        for (String name : names) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException(
                        "The specified CipherSuites array contains " +
                        "invalid null or empty string elements");
            }

            boolean found = false;
            CipherSuite cs;
            if ((cs = cipherSuiteNames.get(name)) != null
                    && !cs.supportedProtocols.isEmpty()) {
                cipherSuites.add(cs);
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "Unsupported CipherSuite: "  + name);
            }
        }

        return Collections.unmodifiableList(cipherSuites);
    }

    static String[] namesOf(List<CipherSuite> cipherSuites) {
        String[] names = new String[cipherSuites.size()];
        int i = 0;
        for (CipherSuite cipherSuite : cipherSuites) {
            names[i++] = cipherSuite.name;
        }

        return names;
    }

    boolean isAvailable() {
        // Note: keyExchange is null for TLS 1.3 CipherSuites.
        return !supportedProtocols.isEmpty() &&
                (keyExchange == null || keyExchange.isAvailable()) &&
                bulkCipher != null && bulkCipher.isAvailable();
    }

    public boolean supports(ProtocolVersion protocolVersion) {
        return supportedProtocols.contains(protocolVersion);
    }

    boolean isNegotiable() {
        return this != TLS_EMPTY_RENEGOTIATION_INFO_SCSV && isAvailable();
    }

    boolean isAnonymous() {
        return (keyExchange != null && keyExchange.isAnonymous);
    }

    // See also SSLWriteCipher.calculatePacketSize().
    int calculatePacketSize(int fragmentSize,
            ProtocolVersion protocolVersion, boolean isDTLS) {
        int packetSize = fragmentSize;
        if (bulkCipher != null && bulkCipher != B_NULL) {
            int blockSize = bulkCipher.ivSize;
            switch (bulkCipher.cipherType) {
                case BLOCK_CIPHER:
                    packetSize += macAlg.size;
                    packetSize += 1;        // 1 byte padding length field
                    packetSize +=           // use the minimal padding
                            (blockSize - (packetSize % blockSize)) % blockSize;
                    if (protocolVersion.useTLS11PlusSpec()) {
                        packetSize += blockSize;        // explicit IV
                    }

                    break;
                case AEAD_CIPHER:
                    if (protocolVersion == ProtocolVersion.TLS12 ||
                            protocolVersion == ProtocolVersion.DTLS12) {
                        packetSize +=
                                bulkCipher.ivSize - bulkCipher.fixedIvSize;
                    }
                    packetSize += bulkCipher.tagSize;

                    break;
                default:    // NULL_CIPHER or STREAM_CIPHER
                    packetSize += macAlg.size;
            }
        }

        return packetSize +
            (isDTLS ? DTLSRecord.headerSize : SSLRecord.headerSize);
    }

    // See also CipherBox.calculateFragmentSize().
    int calculateFragSize(int packetLimit,
            ProtocolVersion protocolVersion, boolean isDTLS) {
        int fragSize = packetLimit -
                (isDTLS ? DTLSRecord.headerSize : SSLRecord.headerSize);
        if (bulkCipher != null && bulkCipher != B_NULL) {
            int blockSize = bulkCipher.ivSize;
            switch (bulkCipher.cipherType) {
                case BLOCK_CIPHER:
                    if (protocolVersion.useTLS11PlusSpec()) {
                        fragSize -= blockSize;          // explicit IV
                    }
                    fragSize -= (fragSize % blockSize); // cannot hold a block
                    // No padding for a maximum fragment.
                    fragSize -= 1;        // 1 byte padding length field: 0x00
                    fragSize -= macAlg.size;

                    break;
                case AEAD_CIPHER:
                    fragSize -= bulkCipher.tagSize;
                    fragSize -= bulkCipher.ivSize - bulkCipher.fixedIvSize;

                    break;
                default:    // NULL_CIPHER or STREAM_CIPHER
                    fragSize -= macAlg.size;
            }
        }

        return fragSize;
    }

    /**
     * An SSL/TLS key exchange algorithm.
     */
    static enum KeyExchange {
        K_NULL          ("NULL",           false, true,   NAMED_GROUP_NONE),
        K_RSA           ("RSA",            true,  false,  NAMED_GROUP_NONE),
        K_RSA_EXPORT    ("RSA_EXPORT",     true,  false,  NAMED_GROUP_NONE),
        K_DH_RSA        ("DH_RSA",         false, false,  NAMED_GROUP_NONE),
        K_DH_DSS        ("DH_DSS",         false, false,  NAMED_GROUP_NONE),
        K_DHE_DSS       ("DHE_DSS",        true,  false,  NAMED_GROUP_FFDHE),
        K_DHE_DSS_EXPORT("DHE_DSS_EXPORT", true,  false,  NAMED_GROUP_NONE),
        K_DHE_RSA       ("DHE_RSA",        true,  false,  NAMED_GROUP_FFDHE),
        K_DHE_RSA_EXPORT("DHE_RSA_EXPORT", true,  false,  NAMED_GROUP_NONE),
        K_DH_ANON       ("DH_anon",        true,  true,   NAMED_GROUP_FFDHE),
        K_DH_ANON_EXPORT("DH_anon_EXPORT", true,  true,   NAMED_GROUP_NONE),

        // These KeyExchanges can use either ECDHE/XDH, so we'll use a
        // varargs here.
        K_ECDH_ECDSA    ("ECDH_ECDSA",     JsseJce.ALLOW_ECC,  false,
                NAMED_GROUP_ECDHE, NAMED_GROUP_XDH),
        K_ECDH_RSA      ("ECDH_RSA",       JsseJce.ALLOW_ECC,  false,
            NAMED_GROUP_ECDHE, NAMED_GROUP_XDH),
        K_ECDHE_ECDSA   ("ECDHE_ECDSA",    JsseJce.ALLOW_ECC,  false,
            NAMED_GROUP_ECDHE, NAMED_GROUP_XDH),
        K_ECDHE_RSA     ("ECDHE_RSA",      JsseJce.ALLOW_ECC,  false,
            NAMED_GROUP_ECDHE, NAMED_GROUP_XDH),
        K_ECDH_ANON     ("ECDH_anon",      JsseJce.ALLOW_ECC,  true,
            NAMED_GROUP_ECDHE, NAMED_GROUP_XDH),

        // renegotiation protection request signaling cipher suite
        K_SCSV          ("SCSV",           true,  true,   NAMED_GROUP_NONE);

        // name of the key exchange algorithm, e.g. DHE_DSS
        final String name;
        final boolean allowed;
        final NamedGroupSpec[] groupTypes;
        private final boolean alwaysAvailable;
        private final boolean isAnonymous;

        KeyExchange(String name, boolean allowed,
                boolean isAnonymous, NamedGroupSpec... groupTypes) {
            this.name = name;
            this.groupTypes = groupTypes;
            this.allowed = allowed;

            this.alwaysAvailable = allowed && (!name.startsWith("EC"));
            this.isAnonymous = isAnonymous;
        }

        boolean isAvailable() {
            if (alwaysAvailable) {
                return true;
            }

            if (NamedGroupSpec.arrayContains(groupTypes,
                    NamedGroupSpec.NAMED_GROUP_ECDHE)) {
                return (allowed && JsseJce.isEcAvailable());
            } else {
                return allowed;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * An SSL/TLS key MAC algorithm.
     *
     * Also contains a factory method to obtain an initialized MAC
     * for this algorithm.
     */
    static enum MacAlg {
        M_NULL      ("NULL",     0,   0,   0),
        M_MD5       ("MD5",     16,  64,   9),
        M_SHA       ("SHA",     20,  64,   9),
        M_SHA256    ("SHA256",  32,  64,   9),
        M_SHA384    ("SHA384",  48, 128,  17);

        // descriptive name, e.g. MD5
        final String name;

        // size of the MAC value (and MAC key) in bytes
        final int size;

        // block size of the underlying hash algorithm
        final int hashBlockSize;

        // minimal padding size of the underlying hash algorithm
        final int minimalPaddingSize;

        MacAlg(String name, int size,
                int hashBlockSize, int minimalPaddingSize) {
            this.name = name;
            this.size = size;
            this.hashBlockSize = hashBlockSize;
            this.minimalPaddingSize = minimalPaddingSize;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * The hash algorithms used for PRF (PseudoRandom Function) or HKDF.
     *
     * Note that TLS 1.1- uses a single MD5/SHA1-based PRF algorithm for
     * generating the necessary material.
     */
    static enum HashAlg {
        H_NONE      ("NONE",    0,    0),
        H_SHA256    ("SHA-256", 32,  64),
        H_SHA384    ("SHA-384", 48, 128);

        final String name;
        final int hashLength;
        final int blockSize;

        HashAlg(String hashAlg, int hashLength, int blockSize) {
            this.name = hashAlg;
            this.hashLength = hashLength;
            this.blockSize = blockSize;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
