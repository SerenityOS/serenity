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
 * @bug 8210918 8210334 8209916
 * @summary Add test to exercise server-side client hello processing
 * @run main/othervm ClientHelloProcessing noPskNoKexModes
 * @run main/othervm ClientHelloProcessing noPskYesKexModes
 * @run main/othervm ClientHelloProcessing yesPskNoKexModes
 * @run main/othervm ClientHelloProcessing yesPskYesKexModes
 * @run main/othervm ClientHelloProcessing supGroupsSect163k1
 */

/*
 * SunJSSE does not support dynamic system properties, no way to re-use
 * system properties in samevm/agentvm mode.
 */

import java.io.FileInputStream;
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/*
 * If you wish to add test cases, the following must be done:
 * 1. Add a @run line with the parameter being a name for the test case
 * 2. Create the ClientHello as a byte[].  It should be a complete TLS
 *    record, but does not need upper layer headers like TCP, IP, Ethernet, etc.
 * 3. Create a new TestCase instance, see "noPskNoKexModes" as an example
 * 4. Create a mapping between the test case name in your @run line and the
 *    TestCase object you created in step #3.  Add this to TESTMAP.
 */

public class ClientHelloProcessing {

    private static final ByteBuffer SERVOUTBUF =
            ByteBuffer.wrap("Server Side".getBytes());

    private static final String pathToStores = "../etc";
    private static final String keyStoreFile = "keystore";
    private static final String trustStoreFile = "truststore";
    private static final String passwd = "passphrase";

    private static final String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static final String trustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + trustStoreFile;

    private static TrustManagerFactory trustMgrFac = null;
    private static KeyManagerFactory keyMgrFac = null;

    // Canned client hello messages
    // These were created from packet captures using openssl 1.1.1's
    // s_client utility.  The captured TLS record containing the client
    // hello was then manually edited to remove or add fields if the s_client
    // utility could not be used to generate a message with the desired
    // extensions.  When manually altering the hello messages, care must
    // be taken to change the lengths of the extensions themselves, the
    // extensions vector length, the handshake message length, and the TLS
    // record length.

    // Client Hello with the pre_shared_key and psk_key_exchange_modes
    // both absent.  Required manual removal of the psk_key_exchange_modes
    // extension.  Similarly formed Client Hello messages may be generated
    // by clients that don't support pre-shared keys.
    //
    //    TLSv1.3 Record Layer: Handshake Protocol: Client Hello
    //        Content Type: Handshake (22)
    //        Version: TLS 1.0 (0x0301)
    //        Length: 256
    //        Handshake Protocol: Client Hello
    //            Handshake Type: Client Hello (1)
    //            Length: 252
    //            Version: TLS 1.2 (0x0303)
    //            Random: 9b796ad0cbd559fb48fc4ba32da5bb8c1ef9a7da85231860...
    //            Session ID Length: 32
    //            Session ID: fe8411205bc99a506952f5c28569facb96ff0f37621be072...
    //            Cipher Suites Length: 8
    //            Cipher Suites (4 suites)
    //            Compression Methods Length: 1
    //            Compression Methods (1 method)
    //            Extensions Length: 171
    //            Extension: server_name (len=14)
    //            Extension: ec_point_formats (len=4)
    //            Extension: supported_groups (len=4)
    //            Extension: SessionTicket TLS (len=0)
    //            Extension: status_request (len=5)
    //            Extension: encrypt_then_mac (len=0)
    //            Extension: extended_master_secret (len=0)
    //            Extension: signature_algorithms (len=30)
    //            Extension: supported_versions (len=3)
    //            Extension: key_share (len=71)
    private static final byte[] CLIHELLO_NOPSK_NOPSKEXMODE = {
        22,    3,    1,    1,    0,    1,    0,    0,
        -4,    3,    3, -101,  121,  106,  -48,  -53,
       -43,   89,   -5,   72,   -4,   75,  -93,   45,
       -91,  -69, -116,   30,   -7,  -89,  -38, -123,
        35,   24,   96,   29,  -93,  -22,   10,  -97,
       -15,  -11,    3,   32,   -2, -124,   17,   32,
        91,  -55, -102,   80,  105,   82,  -11,  -62,
      -123,  105,   -6,  -53, -106,   -1,   15,   55,
        98,   27,  -32,  114, -126,  -13,   42, -104,
      -102,   37,  -65,   52,    0,    8,   19,    2,
        19,    3,   19,    1,    0,   -1,    1,    0,
         0,  -85,    0,    0,    0,   14,    0,   12,
         0,    0,    9,  108,  111,   99,   97,  108,
       104,  111,  115,  116,    0,   11,    0,    4,
         3,    0,    1,    2,    0,   10,    0,    4,
         0,    2,    0,   23,    0,   35,    0,    0,
         0,    5,    0,    5,    1,    0,    0,    0,
         0,    0,   22,    0,    0,    0,   23,    0,
         0,    0,   13,    0,   30,    0,   28,    4,
         3,    5,    3,    6,    3,    8,    7,    8,
         8,    8,    9,    8,   10,    8,   11,    8,
         4,    8,    5,    8,    6,    4,    1,    5,
         1,    6,    1,    0,   43,    0,    3,    2,
         3,    4,    0,   51,    0,   71,    0,   69,
         0,   23,    0,   65,    4,  125,  -92,  -50,
       -91,  -39,  -55, -114,    0,   22,    2,  -50,
       123, -126,    0,  -94,  100, -119, -106,  125,
       -81,  -24,   51,  -84,   25,   25, -115,   13,
       -17,  -20,   93,   68,  -97,  -79,  -98,   91,
        86,   91, -114,  123,  119,  -87,  -12,   32,
        63,  -41,   50,  126,  -70,   96,   33,   -6,
        94,   -7,  -68,   54,  -47,   53,    0,   88,
        40,  -48, -102,  -50,   88
    };

    // Client Hello with the pre_shared_key extension absent but
    // containing the psk_key_exchange_modes extension asserted.  No
    // manual modification was necessary.
    //
    //    TLSv1.3 Record Layer: Handshake Protocol: Client Hello
    //        Content Type: Handshake (22)
    //        Version: TLS 1.0 (0x0301)
    //        Length: 262
    //        Handshake Protocol: Client Hello
    //            Handshake Type: Client Hello (1)
    //            Length: 258
    //            Version: TLS 1.2 (0x0303)
    //            Random: 9b796ad0cbd559fb48fc4ba32da5bb8c1ef9a7da85231860...
    //            Session ID Length: 32
    //            Session ID: fe8411205bc99a506952f5c28569facb96ff0f37621be072...
    //            Cipher Suites Length: 8
    //            Cipher Suites (4 suites)
    //            Compression Methods Length: 1
    //            Compression Methods (1 method)
    //            Extensions Length: 177
    //            Extension: server_name (len=14)
    //            Extension: ec_point_formats (len=4)
    //            Extension: supported_groups (len=4)
    //            Extension: SessionTicket TLS (len=0)
    //            Extension: status_request (len=5)
    //            Extension: encrypt_then_mac (len=0)
    //            Extension: extended_master_secret (len=0)
    //            Extension: signature_algorithms (len=30)
    //            Extension: supported_versions (len=3)
    //            Extension: psk_key_exchange_modes (len=2)
    //                Type: psk_key_exchange_modes (45)
    //                Length: 2
    //                PSK Key Exchange Modes Length: 1
    //                PSK Key Exchange Mode: PSK with (EC)DHE key establishment (psk_dhe_ke) (1)
    //            Extension: key_share (len=71)
    private static final byte[] CLIHELLO_NOPSK_YESPSKEXMODE = {
        22,    3,    1,    1,    6,    1,    0,    1,
         2,    3,    3, -101,  121,  106,  -48,  -53,
       -43,   89,   -5,   72,   -4,   75,  -93,   45,
       -91,  -69, -116,   30,   -7,  -89,  -38, -123,
        35,   24,   96,   29,  -93,  -22,   10,  -97,
       -15,  -11,    3,   32,   -2, -124,   17,   32,
        91,  -55, -102,   80,  105,   82,  -11,  -62,
      -123,  105,   -6,  -53, -106,   -1,   15,   55,
        98,   27,  -32,  114, -126,  -13,   42, -104,
      -102,   37,  -65,   52,    0,    8,   19,    2,
        19,    3,   19,    1,    0,   -1,    1,    0,
         0,  -79,    0,    0,    0,   14,    0,   12,
         0,    0,    9,  108,  111,   99,   97,  108,
       104,  111,  115,  116,    0,   11,    0,    4,
         3,    0,    1,    2,    0,   10,    0,    4,
         0,    2,    0,   23,    0,   35,    0,    0,
         0,    5,    0,    5,    1,    0,    0,    0,
         0,    0,   22,    0,    0,    0,   23,    0,
         0,    0,   13,    0,   30,    0,   28,    4,
         3,    5,    3,    6,    3,    8,    7,    8,
         8,    8,    9,    8,   10,    8,   11,    8,
         4,    8,    5,    8,    6,    4,    1,    5,
         1,    6,    1,    0,   43,    0,    3,    2,
         3,    4,    0,   45,    0,    2,    1,    1,
         0,   51,    0,   71,    0,   69,    0,   23,
         0,   65,    4,  125,  -92,  -50,  -91,  -39,
       -55, -114,    0,   22,    2,  -50,  123, -126,
         0,  -94,  100, -119, -106,  125,  -81,  -24,
        51,  -84,   25,   25, -115,   13,  -17,  -20,
        93,   68,  -97,  -79,  -98,   91,   86,   91,
      -114,  123,  119,  -87,  -12,   32,   63,  -41,
        50,  126,  -70,   96,   33,   -6,   94,   -7,
       -68,   54,  -47,   53,    0,   88,   40,  -48,
      -102,  -50,   88
    };

    // Client Hello with pre_shared_key asserted and psk_key_exchange_modes
    // absent.  This is a violation of RFC 8446.  This required manual
    // removal of the psk_key_exchange_modes extension.
    //
    //    TLSv1.3 Record Layer: Handshake Protocol: Client Hello
    //        Content Type: Handshake (22)
    //        Version: TLS 1.0 (0x0301)
    //        Length: 318
    //        Handshake Protocol: Client Hello
    //            Handshake Type: Client Hello (1)
    //            Length: 314
    //            Version: TLS 1.2 (0x0303)
    //            Random: e730e42336a19ed9fdb42919c65769132e9e779a797f188c...
    //            Session ID Length: 32
    //            Session ID: 6c6ed31408042fabd0c47fdeee6d19de2d6795e37590f00e...
    //            Cipher Suites Length: 8
    //            Cipher Suites (4 suites)
    //            Compression Methods Length: 1
    //            Compression Methods (1 method)
    //            Extensions Length: 233
    //            Extension: server_name (len=14)
    //            Extension: ec_point_formats (len=4)
    //            Extension: supported_groups (len=4)
    //            Extension: SessionTicket TLS (len=0)
    //            Extension: status_request (len=5)
    //            Extension: encrypt_then_mac (len=0)
    //            Extension: extended_master_secret (len=0)
    //            Extension: signature_algorithms (len=30)
    //            Extension: supported_versions (len=3)
    //            Extension: key_share (len=71)
    //            Extension: pre_shared_key (len=58)
    //                Type: pre_shared_key (41)
    //                Length: 58
    //                Pre-Shared Key extension
    //                    Identities Length: 21
    //                    PSK Identity (length: 15)
    //                        Identity Length: 15
    //                        Identity: 436c69656e745f6964656e74697479
    //                        Obfuscated Ticket Age: 0
    //                    PSK Binders length: 33
    //                    PSK Binders
    private static final byte[] CLIHELLO_YESPSK_NOPSKEXMODE = {
        22,    3,    1,    1,   62,    1,    0,    1,
        58,    3,    3,  -25,   48,  -28,   35,   54,
       -95,  -98,  -39,   -3,  -76,   41,   25,  -58,
        87,  105,   19,   46,  -98,  119, -102,  121,
       127,   24, -116,   -9,  -99,   22,  116,  -97,
        90,   73,  -18,   32,  108,  110,  -45,   20,
         8,    4,   47,  -85,  -48,  -60,  127,  -34,
       -18,  109,   25,  -34,   45,  103, -107,  -29,
       117, -112,  -16,   14,   -5,  -24,   24,   61,
        -9,   28, -119,  -73,    0,    8,   19,    2,
        19,    3,   19,    1,    0,   -1,    1,    0,
         0,  -23,    0,    0,    0,   14,    0,   12,
         0,    0,    9,  108,  111,   99,   97,  108,
       104,  111,  115,  116,    0,   11,    0,    4,
         3,    0,    1,    2,    0,   10,    0,    4,
         0,    2,    0,   23,    0,   35,    0,    0,
         0,    5,    0,    5,    1,    0,    0,    0,
         0,    0,   22,    0,    0,    0,   23,    0,
         0,    0,   13,    0,   30,    0,   28,    4,
         3,    5,    3,    6,    3,    8,    7,    8,
         8,    8,    9,    8,   10,    8,   11,    8,
         4,    8,    5,    8,    6,    4,    1,    5,
         1,    6,    1,    0,   43,    0,    3,    2,
         3,    4,    0,   51,    0,   71,    0,   69,
         0,   23,    0,   65,    4,   -6,  101,  105,
        -2,   -6,   85,  -99,  -37,  112,   90,   44,
      -123, -107,    4,  -12,  -64,   92,   40,  100,
        22,  -53, -124,   54,   56,  102,   25,   76,
       -86,   -1,    6,  110,   95,   92,  -86,  -35,
      -101,  115,   85,   99,   19,    6,  -43,  105,
       -37,  -92,   53,  -97,   84,   -1,  -53,   87,
       -53, -107,  -13,  -14,   32,  101,  -35,   39,
       102,  -17, -119,  -25,  -51,    0,   41,    0,
        58,    0,   21,    0,   15,   67,  108,  105,
       101,  110,  116,   95,  105,  100,  101,  110,
       116,  105,  116,  121,    0,    0,    0,    0,
         0,   33,   32, -113,  -27,  -44,  -71,  -68,
       -26,  -47,   57,  -82,  -29,  -13,  -61,   77,
        52,  -60,   27,   74, -120, -104,  102,   21,
       121,    0,   48,   43,  -40,  -19,  -67,   57,
       -20,   97,   23
    };

    // Client Hello containing both pre_shared_key and psk_key_exchange_modes
    // extensions.  Generation of this hello was done by adding
    // "-psk a1b2c3d4" to the s_client command.
    //
    //    TLSv1.3 Record Layer: Handshake Protocol: Client Hello
    //        Content Type: Handshake (22)
    //        Version: TLS 1.0 (0x0301)
    //        Length: 324
    //        Handshake Protocol: Client Hello
    //            Handshake Type: Client Hello (1)
    //            Length: 320
    //            Version: TLS 1.2 (0x0303)
    //            Random: e730e42336a19ed9fdb42919c65769132e9e779a797f188c...
    //            Session ID Length: 32
    //            Session ID: 6c6ed31408042fabd0c47fdeee6d19de2d6795e37590f00e...
    //            Cipher Suites Length: 8
    //            Cipher Suites (4 suites)
    //            Compression Methods Length: 1
    //            Compression Methods (1 method)
    //            Extensions Length: 239
    //            Extension: server_name (len=14)
    //            Extension: ec_point_formats (len=4)
    //            Extension: supported_groups (len=4)
    //            Extension: SessionTicket TLS (len=0)
    //            Extension: status_request (len=5)
    //            Extension: encrypt_then_mac (len=0)
    //            Extension: extended_master_secret (len=0)
    //            Extension: signature_algorithms (len=30)
    //            Extension: supported_versions (len=3)
    //            Extension: psk_key_exchange_modes (len=2)
    //                Type: psk_key_exchange_modes (45)
    //                Length: 2
    //                PSK Key Exchange Modes Length: 1
    //                PSK Key Exchange Mode: PSK with (EC)DHE key establishment (psk_dhe_ke) (1)
    //            Extension: key_share (len=71)
    //            Extension: pre_shared_key (len=58)
    //                Type: pre_shared_key (41)
    //                Length: 58
    //                Pre-Shared Key extension
    //                    Identities Length: 21
    //                    PSK Identity (length: 15)
    //                        Identity Length: 15
    //                        Identity: 436c69656e745f6964656e74697479
    //                        Obfuscated Ticket Age: 0
    //                    PSK Binders length: 33
    //                    PSK Binders
    private static final byte[] CLIHELLO_YESPSK_YESPSKEXMODE = {
        22,    3,    1,    1,   68,    1,    0,    1,
        64,    3,    3,  -25,   48,  -28,   35,   54,
       -95,  -98,  -39,   -3,  -76,   41,   25,  -58,
        87,  105,   19,   46,  -98,  119, -102,  121,
       127,   24, -116,   -9,  -99,   22,  116,  -97,
        90,   73,  -18,   32,  108,  110,  -45,   20,
         8,    4,   47,  -85,  -48,  -60,  127,  -34,
       -18,  109,   25,  -34,   45,  103, -107,  -29,
       117, -112,  -16,   14,   -5,  -24,   24,   61,
        -9,   28, -119,  -73,    0,    8,   19,    2,
        19,    3,   19,    1,    0,   -1,    1,    0,
         0,  -17,    0,    0,    0,   14,    0,   12,
         0,    0,    9,  108,  111,   99,   97,  108,
       104,  111,  115,  116,    0,   11,    0,    4,
         3,    0,    1,    2,    0,   10,    0,    4,
         0,    2,    0,   23,    0,   35,    0,    0,
         0,    5,    0,    5,    1,    0,    0,    0,
         0,    0,   22,    0,    0,    0,   23,    0,
         0,    0,   13,    0,   30,    0,   28,    4,
         3,    5,    3,    6,    3,    8,    7,    8,
         8,    8,    9,    8,   10,    8,   11,    8,
         4,    8,    5,    8,    6,    4,    1,    5,
         1,    6,    1,    0,   43,    0,    3,    2,
         3,    4,    0,   45,    0,    2,    1,    1,
         0,   51,    0,   71,    0,   69,    0,   23,
         0,   65,    4,   -6,  101,  105,   -2,   -6,
        85,  -99,  -37,  112,   90,   44, -123, -107,
         4,  -12,  -64,   92,   40,  100,   22,  -53,
      -124,   54,   56,  102,   25,   76,  -86,   -1,
         6,  110,   95,   92,  -86,  -35, -101,  115,
        85,   99,   19,    6,  -43,  105,  -37,  -92,
        53,  -97,   84,   -1,  -53,   87,  -53, -107,
       -13,  -14,   32,  101,  -35,   39,  102,  -17,
      -119,  -25,  -51,    0,   41,    0,   58,    0,
        21,    0,   15,   67,  108,  105,  101,  110,
       116,   95,  105,  100,  101,  110,  116,  105,
       116,  121,    0,    0,    0,    0,    0,   33,
        32, -113,  -27,  -44,  -71,  -68,  -26,  -47,
        57,  -82,  -29,  -13,  -61,   77,   52,  -60,
        27,   74, -120, -104,  102,   21,  121,    0,
        48,   43,  -40,  -19,  -67,   57,  -20,   97,
        23
    };

    // Client Hello with sect163k1 and secp256r1 as supported groups.  This
    // test covers an error condition where a known, supported curve that is
    // not in the default enabled set of curves would cause failures.
    // Generation of this hello was done using "-curves sect163k1:prime256v1"
    // as an option to s_client.
    //
    //    TLSv1.2 Record Layer: Handshake Protocol: Client Hello
    //        Content Type: Handshake (22)
    //        Version: TLS 1.0 (0x0301)
    //        Length: 210
    //        Handshake Protocol: Client Hello
    //            Handshake Type: Client Hello (1)
    //            Length: 206
    //            Version: TLS 1.2 (0x0303)
    //            Random: 05cbae9b834851d856355b72601cb67b7cd4eb51f29ed50b...
    //            Session ID Length: 0
    //            Cipher Suites Length: 56
    //            Cipher Suites (28 suites)
    //            Compression Methods Length: 1
    //            Compression Methods (1 method)
    //            Extensions Length: 109
    //            Extension: server_name (len=14)
    //            Extension: ec_point_formats (len=4)
    //            Extension: supported_groups (len=6)
    //                Type: supported_groups (10)
    //                Length: 6
    //                Supported Groups List Length: 4
    //                Supported Groups (2 groups)
    //                    Supported Group: sect163k1 (0x0001)
    //                    Supported Group: secp256r1 (0x0017)
    //            Extension: SessionTicket TLS (len=0)
    //            Extension: status_request (len=5)
    //            Extension: encrypt_then_mac (len=0)
    //            Extension: extended_master_secret (len=0)
    //            Extension: signature_algorithms (len=48)
    private static final byte[] CLIHELLO_SUPGRP_SECT163K1 = {
        22,    3,    1,    0,  -46,    1,    0,    0,
       -50,    3,    3,    5,  -53,  -82, -101, -125,
        72,   81,  -40,   86,   53,   91,  114,   96,
        28,  -74,  123,  124,  -44,  -21,   81,  -14,
       -98,  -43,   11,   90,  -87, -106,   13,   63,
       -62,  100,  111,    0,    0,   56,  -64,   44,
       -64,   48,    0,  -97,  -52,  -87,  -52,  -88,
       -52,  -86,  -64,   43,  -64,   47,    0,  -98,
       -64,   36,  -64,   40,    0,  107,  -64,   35,
       -64,   39,    0,  103,  -64,   10,  -64,   20,
         0,   57,  -64,    9,  -64,   19,    0,   51,
         0,  -99,    0, -100,    0,   61,    0,   60,
         0,   53,    0,   47,    0,   -1,    1,    0,
         0,  109,    0,    0,    0,   14,    0,   12,
         0,    0,    9,  108,  111,   99,   97,  108,
       104,  111,  115,  116,    0,   11,    0,    4,
         3,    0,    1,    2,    0,   10,    0,    6,
         0,    4,    0,    1,    0,   23,    0,   35,
         0,    0,    0,    5,    0,    5,    1,    0,
         0,    0,    0,    0,   22,    0,    0,    0,
        23,    0,    0,    0,   13,    0,   48,    0,
        46,    4,    3,    5,    3,    6,    3,    8,
         7,    8,    8,    8,    9,    8,   10,    8,
        11,    8,    4,    8,    5,    8,    6,    4,
         1,    5,    1,    6,    1,    3,    3,    2,
         3,    3,    1,    2,    1,    3,    2,    2,
         2,    4,    2,    5,    2,    6,    2
    };

    public static interface TestCase {
        void execTest() throws Exception;
    }

    private static final Map<String, TestCase> TESTMAP = new HashMap<>();

    public static void main(String[] args) throws Exception {
        boolean allGood = true;
        System.setProperty("javax.net.debug", "ssl:handshake");
        trustMgrFac = makeTrustManagerFactory(trustFilename, passwd);
        keyMgrFac = makeKeyManagerFactory(keyFilename, passwd);

        // Populate the test map
        TESTMAP.put("noPskNoKexModes", noPskNoKexModes);
        TESTMAP.put("noPskYesKexModes", noPskYesKexModes);
        TESTMAP.put("yesPskNoKexModes", yesPskNoKexModes);
        TESTMAP.put("yesPskYesKexModes", yesPskYesKexModes);
        TESTMAP.put("supGroupsSect163k1", supGroupsSect163k1);

        if (args == null || args.length < 1) {
            throw new Exception("FAIL: Test @run line is missing a test label");
        }

        // Pull the test to run from the test map.
        TestCase test = Objects.requireNonNull(TESTMAP.get(args[0]),
                "No TestCase found for test label " + args[0]);
        test.execTest();
    }

    /**
     * Test case to cover hellos with no pre_shared_key nor
     * psk_key_exchange_modes extensions.  Clients not supporting PSK at all
     * may send hellos like this.
     */
    private static final TestCase noPskNoKexModes = new TestCase() {
        @Override
        public void execTest() throws Exception {
            System.out.println("\nTest: PSK = No, PSKEX = No");
            processClientHello("TLS", CLIHELLO_NOPSK_NOPSKEXMODE);
            System.out.println("PASS");
        }
    };

    /**
     * Test case to cover hellos with no pre_shared_key but have the
     * psk_key_exchange_modes extension.  This kind of hello is seen from
     * some popular browsers and test clients.
     */
    private static final TestCase noPskYesKexModes = new TestCase() {
        @Override
        public void execTest() throws Exception {
            System.out.println("\nTest: PSK = No, PSKEX = Yes");
            processClientHello("TLS", CLIHELLO_NOPSK_YESPSKEXMODE);
            System.out.println("PASS");
        }
    };

    /**
     * Test case using a client hello with the pre_shared_key extension but
     * no psk_key_exchange_modes extension present.  This is a violation of
     * 8446 and should cause an exception when unwrapped and processed by
     * SSLEngine.
     */
    private static final TestCase yesPskNoKexModes = new TestCase() {
        @Override
        public void execTest() throws Exception {
            try {
                System.out.println("\nTest: PSK = Yes, PSKEX = No");
                processClientHello("TLS", CLIHELLO_YESPSK_NOPSKEXMODE);
                throw new Exception(
                    "FAIL: Client Hello processed without expected error");
            } catch (SSLHandshakeException sslhe) {
                System.out.println("PASS: Caught expected exception: " + sslhe);
            }
        }
    };

    /**
     * Test case using a client hello asserting the pre_shared_key and
     * psk_key_exchange_modes extensions.
     */
    private static final TestCase yesPskYesKexModes = new TestCase() {
        @Override
        public void execTest() throws Exception {
            System.out.println("\nTest: PSK = Yes, PSKEX = Yes");
            processClientHello("TLS", CLIHELLO_YESPSK_YESPSKEXMODE);
            System.out.println("PASS");
        }
    };

    /**
     * Test case with a client hello asserting two named curves in the
     * supported_groups extension: sect163k1 and secp256r1.
     */
    private static final TestCase supGroupsSect163k1 = new TestCase() {
        @Override
        public void execTest() throws Exception {
            System.out.println("\nTest: Use of non-default-enabled " +
                    "Supported Group (sect163k1)");
            processClientHello("TLS", CLIHELLO_SUPGRP_SECT163K1);
            System.out.println("PASS");
        }
    };

    /**
     * Send a ClientHello message to an SSLEngine instance configured as a
     * server.
     *
     * @param proto the protocol used to create the SSLContext.  This will
     *      default to "TLS" if null is passed in.
     * @param message the ClientHello as a complete TLS record.
     *
     * @throws Exception if any processing errors occur.  The caller (TestCase)
     *      is expected to deal with the exception in whatever way appropriate
     *      for the test.
     */
    private static void processClientHello(String proto, byte[] message)
            throws Exception {
        SSLEngine serverEng = makeServerEngine(proto, keyMgrFac, trustMgrFac);
        ByteBuffer sTOc = makePacketBuf(serverEng);
        SSLEngineResult serverResult;

        ByteBuffer cTOs = ByteBuffer.wrap(message);
        System.out.println("CLIENT-TO-SERVER\n" +
                dumpHexBytes(cTOs, 16, "\n", " "));
        serverResult = serverEng.unwrap(cTOs, SERVOUTBUF);
        printResult("server unwrap: ", serverResult);
        runDelegatedTasks(serverResult, serverEng);
        serverEng.wrap(SERVOUTBUF, sTOc);
    }

    /**
     * Create a TrustManagerFactory from a given keystore.
     *
     * @param tsPath the path to the trust store file.
     * @param pass the password for the trust store.
     *
     * @return a new TrustManagerFactory built from the trust store provided.
     *
     * @throws GeneralSecurityException if any processing errors occur
     *      with the Keystore instantiation or TrustManagerFactory creation.
     * @throws IOException if any loading error with the trust store occurs.
     */
    private static TrustManagerFactory makeTrustManagerFactory(String tsPath,
            String pass) throws GeneralSecurityException, IOException {
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] passphrase = pass.toCharArray();

        ts.load(new FileInputStream(tsPath), passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
        return tmf;
    }

    /**
     * Create a KeyManagerFactory from a given keystore.
     *
     * @param ksPath the path to the keystore file.
     * @param pass the password for the keystore.
     *
     * @return a new TrustManagerFactory built from the keystore provided.
     *
     * @throws GeneralSecurityException if any processing errors occur
     *      with the Keystore instantiation or KeyManagerFactory creation.
     * @throws IOException if any loading error with the keystore occurs
     */
    private static KeyManagerFactory makeKeyManagerFactory(String ksPath,
            String pass) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] passphrase = pass.toCharArray();

        ks.load(new FileInputStream(ksPath), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        return kmf;
    }

    /**
     * Create an SSLEngine instance from a given protocol specifier,
     * KeyManagerFactory and TrustManagerFactory.
     *
     * @param proto the protocol specifier for the SSLContext.  This will
     *      default to "TLS" if null is provided.
     * @param kmf an initialized KeyManagerFactory.  May be null.
     * @param tmf an initialized TrustManagerFactory.  May be null.
     *
     * @return an SSLEngine instance configured as a server and with client
     *      authentication disabled.
     *
     * @throws GeneralSecurityException if any errors occur during the
     *      creation of the SSLEngine.
     */
    private static SSLEngine makeServerEngine(String proto,
            KeyManagerFactory kmf, TrustManagerFactory tmf)
            throws GeneralSecurityException {
        SSLContext ctx = SSLContext.getInstance(proto != null ? proto : "TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLEngine ssle = ctx.createSSLEngine();
        ssle.setUseClientMode(false);
        ssle.setNeedClientAuth(false);
        return ssle;
    }

    /**
     * Make a ByteBuffer sized for TLS records that can be used by an SSLEngine.
     *
     * @param engine the SSLEngine used to determine the packet buffer size.
     *
     * @return a ByteBuffer sized for TLS packets.
     */
    private static ByteBuffer makePacketBuf(SSLEngine engine) {
        SSLSession sess = engine.getSession();
        ByteBuffer packetBuf = ByteBuffer.allocate(sess.getPacketBufferSize());
        return packetBuf;
    }

    /**
     * Runs any delegated tasks after unwrapping TLS records.
     *
     * @param result the most recent result from an unwrap operation on
     *      an SSLEngine.
     * @param engine the SSLEngine used to unwrap the data.
     *
     * @throws Exception if any errors occur while running the delegated
     *      tasks.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        if (hsStatus == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                System.out.println("\trunning delegated task...");
                runnable.run();
            }
            hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            System.out.println("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    /**
     * Display the results of a wrap or unwrap operation from an SSLEngine.
     *
     * @param str a label to be prefixed to the result display.
     * @param result the result returned from the wrap/unwrap operation.
     */
    private static void printResult(String str, SSLEngineResult result) {
        System.out.println("The format of the SSLEngineResult is: \n" +
            "\t\"getStatus() / getHandshakeStatus()\" +\n" +
            "\t\"bytesConsumed() / bytesProduced()\"\n");
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        System.out.println(str + result.getStatus() + "/" + hsStatus + ", " +
            result.bytesConsumed() + "/" + result.bytesProduced() + " bytes");
        if (hsStatus == HandshakeStatus.FINISHED) {
            System.out.println("\t...ready for application data");
        }
    }

    /**
     * Dump the hex bytes of a buffer into string form.
     *
     * @param data The array of bytes to dump to stdout.
     * @param itemsPerLine The number of bytes to display per line
     *      if the {@code lineDelim} character is blank then all bytes
     *      will be printed on a single line.
     * @param lineDelim The delimiter between lines
     * @param itemDelim The delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(byte[] data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        return dumpHexBytes(ByteBuffer.wrap(data), itemsPerLine, lineDelim,
                itemDelim);
    }

    /**
     * Dump the hex bytes of a buffer into string form.
     *
     * @param data The ByteBuffer to dump to stdout.
     * @param itemsPerLine The number of bytes to display per line
     *      if the {@code lineDelim} character is blank then all bytes
     *      will be printed on a single line.
     * @param lineDelim The delimiter between lines
     * @param itemDelim The delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(ByteBuffer data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            data.mark();
            int i = 0;
            while (data.remaining() > 0) {
                if (i % itemsPerLine == 0 && i != 0) {
                    sb.append(lineDelim);
                }
                sb.append(String.format("%02X", data.get())).append(itemDelim);
                i++;
            }
            data.reset();
        }

        return sb.toString();
    }
}
