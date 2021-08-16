/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * SSL/TLS cipher suites.
 */
public enum CipherSuite {

    TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCAA, KeyExAlgorithm.DHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCA9, KeyExAlgorithm.ECDHE_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256(
            0xCCA8, KeyExAlgorithm.ECDHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384(
            0xC032, KeyExAlgorithm.ECDH_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256(
            0xC031, KeyExAlgorithm.ECDH_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384(
            0xC030, KeyExAlgorithm.ECDHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256(
            0xC02F, KeyExAlgorithm.ECDHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384(
            0xC02E, KeyExAlgorithm.ECDH_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256(
            0xC02D, KeyExAlgorithm.ECDH_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384(
            0xC02C, KeyExAlgorithm.ECDHE_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256(
            0xC02B, KeyExAlgorithm.ECDHE_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384(
            0xC02A, KeyExAlgorithm.ECDH_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256(
            0xC029, KeyExAlgorithm.ECDH_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384(
            0xC028, KeyExAlgorithm.ECDHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256(
            0xC027, KeyExAlgorithm.ECDHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384(
            0xC026, KeyExAlgorithm.ECDH_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA(
            0xC025, KeyExAlgorithm.ECDH_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256(
            0xC025, KeyExAlgorithm.ECDH_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384(
            0xC024, KeyExAlgorithm.ECDHE_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256(
            0xC023, KeyExAlgorithm.ECDHE_ECDSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_anon_WITH_AES_256_CBC_SHA(
            0xC019, KeyExAlgorithm.ECDH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_anon_WITH_AES_128_CBC_SHA(
            0xC018, KeyExAlgorithm.ECDH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA(
            0xC017, KeyExAlgorithm.ECDH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_anon_WITH_RC4_128_SHA(
            0xC016, KeyExAlgorithm.ECDH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_anon_WITH_NULL_SHA(
            0xC015, KeyExAlgorithm.ECDH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA(
            0xC014, KeyExAlgorithm.ECDHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA(
            0xC013, KeyExAlgorithm.ECDHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA(
            0xC012, KeyExAlgorithm.ECDHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_RC4_128_SHA(
            0xC011, KeyExAlgorithm.ECDHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_RSA_WITH_NULL_SHA(
            0xC010, KeyExAlgorithm.ECDHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_256_CBC_SHA(
            0xC00F, KeyExAlgorithm.ECDH_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_AES_128_CBC_SHA(
            0xC00E, KeyExAlgorithm.ECDH_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA(
            0xC00D, KeyExAlgorithm.ECDH_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_RC4_128_SHA(
            0xC00C, KeyExAlgorithm.ECDH_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_RSA_WITH_NULL_SHA(
            0xC00B, KeyExAlgorithm.ECDH_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA(
            0xC00A, KeyExAlgorithm.ECDHE_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA(
            0xC009, KeyExAlgorithm.ECDHE_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA(
            0xC008, KeyExAlgorithm.ECDHE_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_RC4_128_SHA(
            0xC007, KeyExAlgorithm.ECDHE_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDHE_ECDSA_WITH_NULL_SHA(
            0xC006, KeyExAlgorithm.ECDHE_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA(
            0xC003, KeyExAlgorithm.ECDH_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_RC4_128_SHA(
            0xC002, KeyExAlgorithm.ECDH_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_NULL_SHA(
            0xC001, KeyExAlgorithm.ECDH_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_EMPTY_RENEGOTIATION_INFO_SCSV(
            0x00FF, KeyExAlgorithm.SCSV, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_AES_256_GCM_SHA384(
            0x1302, null, Protocol.TLSV1_3, Protocol.TLSV1_3),
    TLS_AES_128_GCM_SHA256(
            0x1301, null, Protocol.TLSV1_3, Protocol.TLSV1_3),
    TLS_CHACHA20_POLY1305_SHA256(
            0x1303, null, Protocol.TLSV1_3, Protocol.TLSV1_3),
    TLS_DH_anon_WITH_AES_256_GCM_SHA384(
            0x00A7, KeyExAlgorithm.DH_ANON, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DH_anon_WITH_AES_128_GCM_SHA256(
            0x00A6, KeyExAlgorithm.DH_ANON, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_256_GCM_SHA384(
            0x00A3, KeyExAlgorithm.DHE_DSS, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_128_GCM_SHA256(
            0x00A2, KeyExAlgorithm.DHE_DSS, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_256_GCM_SHA384(
            0x009F, KeyExAlgorithm.DHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256(
            0x009E, KeyExAlgorithm.DHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_256_GCM_SHA384(
            0x009D, KeyExAlgorithm.RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_128_GCM_SHA256(
            0x009C, KeyExAlgorithm.RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DH_anon_WITH_AES_256_CBC_SHA256(
            0x006D, KeyExAlgorithm.DH_ANON, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DH_anon_WITH_AES_128_CBC_SHA256(
            0x006C, KeyExAlgorithm.DH_ANON, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256(
            0x006B, KeyExAlgorithm.DHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA256(
            0x006A, KeyExAlgorithm.DHE_DSS, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256(
            0x0067, KeyExAlgorithm.DHE_RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA(
            0x004C, KeyExAlgorithm.ECDH_ECDSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA256(
            0x0040, KeyExAlgorithm.DHE_DSS, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_256_CBC_SHA256(
            0x003D, KeyExAlgorithm.RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_128_CBC_SHA256(
            0x003C, KeyExAlgorithm.RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_RSA_WITH_NULL_SHA256(
            0x003B, KeyExAlgorithm.RSA, Protocol.TLSV1_2, Protocol.TLSV1_2),
    TLS_DH_anon_WITH_AES_256_CBC_SHA(
            0x003A, KeyExAlgorithm.DH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA(
            0x0039, KeyExAlgorithm.DHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA(
            0x0038, KeyExAlgorithm.DHE_DSS, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_256_CBC_SHA(
            0x0035, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DH_anon_WITH_AES_128_CBC_SHA(
            0x0034, KeyExAlgorithm.DH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA(
            0x0033, KeyExAlgorithm.DHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA(
            0x0032, KeyExAlgorithm.DHE_DSS, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_RSA_WITH_AES_128_CBC_SHA(
            0x002F, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_EXPORT_WITH_RC4_40_MD5(
            0x002B, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5(
            0x002A, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_EXPORT_WITH_RC4_40_SHA(
            0x0028, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5(
            0x0029, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA(
            0x0027, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA(
            0x0026, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_IDEA_CBC_MD5(
            0x0025, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_RC4_128_MD5(
            0x0024, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_WITH_3DES_EDE_CBC_MD5(
            0x0023, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_DES_CBC_MD5(
            0x0022, KeyExAlgorithm.KRB5,  Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_IDEA_CBC_SHA(
            0x0021, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_RC4_128_SHA(
            0x0020, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    TLS_KRB5_WITH_3DES_EDE_CBC_SHA(
            0x001F, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_1),
    TLS_KRB5_WITH_DES_CBC_SHA(
            0x001E, KeyExAlgorithm.KRB5, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_DH_anon_WITH_3DES_EDE_CBC_SHA(
            0x001B, KeyExAlgorithm.DH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_DH_anon_WITH_DES_CBC_SHA(
            0x001A, KeyExAlgorithm.DH_ANON, Protocol.SSLV3, Protocol.TLSV1_1),
    SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA(
            0x0019, KeyExAlgorithm.DH_ANON_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_DH_anon_WITH_RC4_128_MD5(
            0x0018, KeyExAlgorithm.DH_ANON, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_DH_anon_EXPORT_WITH_RC4_40_MD5(
            0x0017, KeyExAlgorithm.DH_ANON_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA(
            0x0016, KeyExAlgorithm.DHE_RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_DHE_RSA_WITH_DES_CBC_SHA(
            0x0015, KeyExAlgorithm.DHE_RSA, Protocol.SSLV3, Protocol.TLSV1_1),
    SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA(
            0x0014, KeyExAlgorithm.DHE_RSA_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA(
            0x0013, KeyExAlgorithm.DHE_DSS, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_DHE_DSS_WITH_DES_CBC_SHA(
            0x0012, KeyExAlgorithm.DHE_DSS, Protocol.SSLV3, Protocol.TLSV1_1),
    SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA(
            0x0011, KeyExAlgorithm.DHE_DSS_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_RSA_WITH_3DES_EDE_CBC_SHA(
            0x000A, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_RSA_WITH_DES_CBC_SHA(
            0x0009, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_1),
    SSL_RSA_EXPORT_WITH_DES40_CBC_SHA(
            0x0008, KeyExAlgorithm.RSA_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_RSA_WITH_RC4_128_SHA(
            0x0005, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_RSA_WITH_RC4_128_MD5(
            0x0004, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_RSA_EXPORT_WITH_RC4_40_MD5(
            0x0003, KeyExAlgorithm.RSA_EXPORT, Protocol.SSLV3, Protocol.TLSV1),
    SSL_RSA_WITH_NULL_SHA(
            0x0002, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2),
    SSL_RSA_WITH_NULL_MD5(
            0x0001, KeyExAlgorithm.RSA, Protocol.SSLV3, Protocol.TLSV1_2);

    public final int id;
    public final KeyExAlgorithm keyExAlgorithm;
    public final Protocol startProtocol;
    public final Protocol endProtocol;

    private CipherSuite(
            int id,
            KeyExAlgorithm keyExAlgorithm,
            Protocol startProtocol,
            Protocol endProtocol) {
        this.id = id;
        this.keyExAlgorithm = keyExAlgorithm;
        this.startProtocol = startProtocol;
        this.endProtocol = endProtocol;
    }

    public boolean supportedByProtocol(Protocol protocol) {
        return startProtocol.id <= protocol.id
                && protocol.id <= endProtocol.id;
    }

    public static CipherSuite cipherSuite(String name) {
        return CipherSuite.valueOf(CipherSuite.class, name);
    }
}
