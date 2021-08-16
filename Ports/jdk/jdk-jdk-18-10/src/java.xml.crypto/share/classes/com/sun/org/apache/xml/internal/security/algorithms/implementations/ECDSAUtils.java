/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.org.apache.xml.internal.security.algorithms.implementations;

import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ECDSAUtils {

    private ECDSAUtils() {
        // complete
    }

    /**
     * Converts an ASN.1 ECDSA value to a XML Signature ECDSA Value.
     * <p></p>
     * The JAVA JCE ECDSA Signature algorithm creates ASN.1 encoded (r, s) value
     * pairs; the XML Signature requires the core BigInteger values.
     *
     * @param asn1Bytes the ASN.1 encoded bytes
     * @param rawLen the intended length of decoded bytes for an integer.
     *               If -1, choose one automatically.
     * @return the decoded bytes
     * @throws IOException
     * @see <A HREF="http://www.w3.org/TR/xmldsig-core/#dsa-sha1">6.4.1 DSA</A>
     * @see <A HREF="ftp://ftp.rfc-editor.org/in-notes/rfc4050.txt">3.3. ECDSA Signatures</A>
     */
    public static byte[] convertASN1toXMLDSIG(byte[] asn1Bytes, int rawLen) throws IOException {
        if (asn1Bytes.length < 8 || asn1Bytes[0] != 48) {
            throw new IOException("Invalid ASN.1 format of ECDSA signature");
        }
        int offset;
        if (asn1Bytes[1] > 0) {
            offset = 2;
        } else if (asn1Bytes[1] == (byte) 0x81) {
            offset = 3;
        } else {
            throw new IOException("Invalid ASN.1 format of ECDSA signature");
        }

        byte rLength = asn1Bytes[offset + 1];
        int i;

        for (i = rLength; i > 0 && asn1Bytes[offset + 2 + rLength - i] == 0; i--); //NOPMD

        byte sLength = asn1Bytes[offset + 2 + rLength + 1];
        int j;

        for (j = sLength; j > 0 && asn1Bytes[offset + 2 + rLength + 2 + sLength - j] == 0; j--); //NOPMD

        int maxLen = Math.max(i, j);

        if (rawLen < 0) {
            rawLen = maxLen;
        } else if (rawLen < maxLen) {
            throw new IOException("Invalid signature length");
        }

        if ((asn1Bytes[offset - 1] & 0xff) != asn1Bytes.length - offset
                || (asn1Bytes[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
                || asn1Bytes[offset] != 2
                || asn1Bytes[offset + 2 + rLength] != 2) {
            throw new IOException("Invalid ASN.1 format of ECDSA signature");
        }
        byte[] xmldsigBytes = new byte[2 * rawLen];

        System.arraycopy(asn1Bytes, offset + 2 + rLength - i, xmldsigBytes, rawLen - i, i);
        System.arraycopy(asn1Bytes, offset + 2 + rLength + 2 + sLength - j, xmldsigBytes,
                2 * rawLen - j, j);

        return xmldsigBytes;
    }

    /**
     * Converts a XML Signature ECDSA Value to an ASN.1 DSA value.
     * <p></p>
     * The JAVA JCE ECDSA Signature algorithm creates ASN.1 encoded (r, s) value
     * pairs; the XML Signature requires the core BigInteger values.
     *
     * @param xmldsigBytes
     * @return the encoded ASN.1 bytes
     * @throws IOException
     * @see <A HREF="http://www.w3.org/TR/xmldsig-core/#dsa-sha1">6.4.1 DSA</A>
     * @see <A HREF="ftp://ftp.rfc-editor.org/in-notes/rfc4050.txt">3.3. ECDSA Signatures</A>
     */
    public static byte[] convertXMLDSIGtoASN1(byte[] xmldsigBytes) throws IOException {

        int rawLen = xmldsigBytes.length / 2;

        int i;

        for (i = rawLen; i > 0 && xmldsigBytes[rawLen - i] == 0; i--); //NOPMD

        int j = i;

        if (xmldsigBytes[rawLen - i] < 0) {
            j += 1;
        }

        int k;

        for (k = rawLen; k > 0 && xmldsigBytes[2 * rawLen - k] == 0; k--); //NOPMD

        int l = k;

        if (xmldsigBytes[2 * rawLen - k] < 0) {
            l += 1;
        }

        int len = 2 + j + 2 + l;
        if (len > 255) {
            throw new IOException("Invalid XMLDSIG format of ECDSA signature");
        }
        int offset;
        byte[] asn1Bytes;
        if (len < 128) {
            asn1Bytes = new byte[2 + 2 + j + 2 + l];
            offset = 1;
        } else {
            asn1Bytes = new byte[3 + 2 + j + 2 + l];
            asn1Bytes[1] = (byte) 0x81;
            offset = 2;
        }
        asn1Bytes[0] = 48;
        asn1Bytes[offset++] = (byte) len;
        asn1Bytes[offset++] = 2;
        asn1Bytes[offset++] = (byte) j;

        System.arraycopy(xmldsigBytes, rawLen - i, asn1Bytes, offset + j - i, i);

        offset += j;

        asn1Bytes[offset++] = 2;
        asn1Bytes[offset++] = (byte) l;

        System.arraycopy(xmldsigBytes, 2 * rawLen - k, asn1Bytes, offset + l - k, k);

        return asn1Bytes;
    }

    private static final List<ECCurveDefinition> ecCurveDefinitions = new ArrayList<>();

    static {
        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp112r1",
                        "1.3.132.0.6",
                        "db7c2abf62e35e668076bead208b",
                        "db7c2abf62e35e668076bead2088",
                        "659ef8ba043916eede8911702b22",
                        "09487239995a5ee76b55f9c2f098",
                        "a89ce5af8724c0a23e0e0ff77500",
                        "db7c2abf62e35e7628dfac6561c5",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp112r2",
                        "1.3.132.0.7",
                        "db7c2abf62e35e668076bead208b",
                        "6127c24c05f38a0aaaf65c0ef02c",
                        "51def1815db5ed74fcc34c85d709",
                        "4ba30ab5e892b4e1649dd0928643",
                        "adcd46f5882e3747def36e956e97",
                        "36df0aafd8b8d7597ca10520d04b",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp128r1",
                        "1.3.132.0.28",
                        "fffffffdffffffffffffffffffffffff",
                        "fffffffdfffffffffffffffffffffffc",
                        "e87579c11079f43dd824993c2cee5ed3",
                        "161ff7528b899b2d0c28607ca52c5b86",
                        "cf5ac8395bafeb13c02da292dded7a83",
                        "fffffffe0000000075a30d1b9038a115",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp128r2",
                        "1.3.132.0.29",
                        "fffffffdffffffffffffffffffffffff",
                        "d6031998d1b3bbfebf59cc9bbff9aee1",
                        "5eeefca380d02919dc2c6558bb6d8a5d",
                        "7b6aa5d85e572983e6fb32a7cdebc140",
                        "27b6916a894d3aee7106fe805fc34b44",
                        "3fffffff7fffffffbe0024720613b5a3",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp160k1",
                        "1.3.132.0.9",
                        "fffffffffffffffffffffffffffffffeffffac73",
                        "0000000000000000000000000000000000000000",
                        "0000000000000000000000000000000000000007",
                        "3b4c382ce37aa192a4019e763036f4f5dd4d7ebb",
                        "938cf935318fdced6bc28286531733c3f03c4fee",
                        "0100000000000000000001b8fa16dfab9aca16b6b3",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp160r1",
                        "1.3.132.0.8",
                        "ffffffffffffffffffffffffffffffff7fffffff",
                        "ffffffffffffffffffffffffffffffff7ffffffc",
                        "1c97befc54bd7a8b65acf89f81d4d4adc565fa45",
                        "4a96b5688ef573284664698968c38bb913cbfc82",
                        "23a628553168947d59dcc912042351377ac5fb32",
                        "0100000000000000000001f4c8f927aed3ca752257",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp160r2",
                        "1.3.132.0.30",
                        "fffffffffffffffffffffffffffffffeffffac73",
                        "fffffffffffffffffffffffffffffffeffffac70",
                        "b4e134d3fb59eb8bab57274904664d5af50388ba",
                        "52dcb034293a117e1f4ff11b30f7199d3144ce6d",
                        "feaffef2e331f296e071fa0df9982cfea7d43f2e",
                        "0100000000000000000000351ee786a818f3a1a16b",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp192k1",
                        "1.3.132.0.31",
                        "fffffffffffffffffffffffffffffffffffffffeffffee37",
                        "000000000000000000000000000000000000000000000000",
                        "000000000000000000000000000000000000000000000003",
                        "db4ff10ec057e9ae26b07d0280b7f4341da5d1b1eae06c7d",
                        "9b2f2f6d9c5628a7844163d015be86344082aa88d95e2f9d",
                        "fffffffffffffffffffffffe26f2fc170f69466a74defd8d",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp192r1 [NIST P-192, X9.62 prime192v1]",
                        "1.2.840.10045.3.1.1",
                        "fffffffffffffffffffffffffffffffeffffffffffffffff",
                        "fffffffffffffffffffffffffffffffefffffffffffffffc",
                        "64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1",
                        "188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012",
                        "07192b95ffc8da78631011ed6b24cdd573f977a11e794811",
                        "ffffffffffffffffffffffff99def836146bc9b1b4d22831",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp224k1",
                        "1.3.132.0.32",
                        "fffffffffffffffffffffffffffffffffffffffffffffffeffffe56d",
                        "00000000000000000000000000000000000000000000000000000000",
                        "00000000000000000000000000000000000000000000000000000005",
                        "a1455b334df099df30fc28a169a467e9e47075a90f7e650eb6b7a45c",
                        "7e089fed7fba344282cafbd6f7e319f7c0b0bd59e2ca4bdb556d61a5",
                        "010000000000000000000000000001dce8d2ec6184caf0a971769fb1f7",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp224r1 [NIST P-224]",
                        "1.3.132.0.33",
                        "ffffffffffffffffffffffffffffffff000000000000000000000001",
                        "fffffffffffffffffffffffffffffffefffffffffffffffffffffffe",
                        "b4050a850c04b3abf54132565044b0b7d7bfd8ba270b39432355ffb4",
                        "b70e0cbd6bb4bf7f321390b94a03c1d356c21122343280d6115c1d21",
                        "bd376388b5f723fb4c22dfe6cd4375a05a07476444d5819985007e34",
                        "ffffffffffffffffffffffffffff16a2e0b8f03e13dd29455c5c2a3d",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp256k1",
                        "1.3.132.0.10",
                        "fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000000000000000000000000000000000000000000007",
                        "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                        "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                        "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp256r1 [NIST P-256, X9.62 prime256v1]",
                        "1.2.840.10045.3.1.7",
                        "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff",
                        "ffffffff00000001000000000000000000000000fffffffffffffffffffffffc",
                        "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b",
                        "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296",
                        "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",
                        "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp384r1 [NIST P-384]",
                        "1.3.132.0.34",
                        "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff",
                        "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000fffffffc",
                        "b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef",
                        "aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760ab7",
                        "3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5f",
                        "ffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "secp521r1 [NIST P-521]",
                        "1.3.132.0.35",
                        "01ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                        "01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc",
                        "0051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00",
                        "00c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66",
                        "011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650",
                        "01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 prime192v2",
                        "1.2.840.10045.3.1.2",
                        "fffffffffffffffffffffffffffffffeffffffffffffffff",
                        "fffffffffffffffffffffffffffffffefffffffffffffffc",
                        "cc22d6dfb95c6b25e49c0d6364a4e5980c393aa21668d953",
                        "eea2bae7e1497842f2de7769cfe9c989c072ad696f48034a",
                        "6574d11d69b6ec7a672bb82a083df2f2b0847de970b2de15",
                        "fffffffffffffffffffffffe5fb1a724dc80418648d8dd31",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 prime192v3",
                        "1.2.840.10045.3.1.3",
                        "fffffffffffffffffffffffffffffffeffffffffffffffff",
                        "fffffffffffffffffffffffffffffffefffffffffffffffc",
                        "22123dc2395a05caa7423daeccc94760a7d462256bd56916",
                        "7d29778100c65a1da1783716588dce2b8b4aee8e228f1896",
                        "38a90f22637337334b49dcb66a6dc8f9978aca7648a943b0",
                        "ffffffffffffffffffffffff7a62d031c83f4294f640ec13",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 prime239v1",
                        "1.2.840.10045.3.1.4",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007fffffffffff",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc",
                        "6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a",
                        "0ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf",
                        "7debe8e4e90a5dae6e4054ca530ba04654b36818ce226b39fccb7b02f1ae",
                        "7fffffffffffffffffffffff7fffff9e5e9a9f5d9071fbd1522688909d0b",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 prime239v2",
                        "1.2.840.10045.3.1.5",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007fffffffffff",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc",
                        "617fab6832576cbbfed50d99f0249c3fee58b94ba0038c7ae84c8c832f2c",
                        "38af09d98727705120c921bb5e9e26296a3cdcf2f35757a0eafd87b830e7",
                        "5b0125e4dbea0ec7206da0fc01d9b081329fb555de6ef460237dff8be4ba",
                        "7fffffffffffffffffffffff800000cfa7e8594377d414c03821bc582063",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 prime239v3",
                        "1.2.840.10045.3.1.6",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007fffffffffff",
                        "7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc",
                        "255705fa2a306654b1f4cb03d6a750a30c250102d4988717d9ba15ab6d3e",
                        "6768ae8e18bb92cfcf005c949aa2c6d94853d0e660bbf854b1c9505fe95a",
                        "1607e6898f390c06bc1d552bad226f3b6fcfe48b6e818499af18e3ed6cf3",
                        "7fffffffffffffffffffffff7fffff975deb41b3a6057c3c432146526551",
                        1)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect113r1",
                        "1.3.132.0.4",
                        "020000000000000000000000000201",
                        "003088250ca6e7c7fe649ce85820f7",
                        "00e8bee4d3e2260744188be0e9c723",
                        "009d73616f35f4ab1407d73562c10f",
                        "00a52830277958ee84d1315ed31886",
                        "0100000000000000d9ccec8a39e56f",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect113r2",
                        "1.3.132.0.5",
                        "020000000000000000000000000201",
                        "00689918dbec7e5a0dd6dfc0aa55c7",
                        "0095e9a9ec9b297bd4bf36e059184f",
                        "01a57a6a7b26ca5ef52fcdb8164797",
                        "00b3adc94ed1fe674c06e695baba1d",
                        "010000000000000108789b2496af93",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect131r1",
                        "1.3.132.0.22",
                        "080000000000000000000000000000010d",
                        "07a11b09a76b562144418ff3ff8c2570b8",
                        "0217c05610884b63b9c6c7291678f9d341",
                        "0081baf91fdf9833c40f9c181343638399",
                        "078c6e7ea38c001f73c8134b1b4ef9e150",
                        "0400000000000000023123953a9464b54d",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect131r2",
                        "1.3.132.0.23",
                        "080000000000000000000000000000010d",
                        "03e5a88919d7cafcbf415f07c2176573b2",
                        "04b8266a46c55657ac734ce38f018f2192",
                        "0356dcd8f2f95031ad652d23951bb366a8",
                        "0648f06d867940a5366d9e265de9eb240f",
                        "0400000000000000016954a233049ba98f",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect163k1 [NIST K-163]",
                        "1.3.132.0.1",
                        "0800000000000000000000000000000000000000c9",
                        "000000000000000000000000000000000000000001",
                        "000000000000000000000000000000000000000001",
                        "02fe13c0537bbc11acaa07d793de4e6d5e5c94eee8",
                        "0289070fb05d38ff58321f2e800536d538ccdaa3d9",
                        "04000000000000000000020108a2e0cc0d99f8a5ef",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect163r1",
                        "1.3.132.0.2",
                        "0800000000000000000000000000000000000000c9",
                        "07b6882caaefa84f9554ff8428bd88e246d2782ae2",
                        "0713612dcddcb40aab946bda29ca91f73af958afd9",
                        "0369979697ab43897789566789567f787a7876a654",
                        "00435edb42efafb2989d51fefce3c80988f41ff883",
                        "03ffffffffffffffffffff48aab689c29ca710279b",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect163r2 [NIST B-163]",
                        "1.3.132.0.15",
                        "0800000000000000000000000000000000000000c9",
                        "000000000000000000000000000000000000000001",
                        "020a601907b8c953ca1481eb10512f78744a3205fd",
                        "03f0eba16286a2d57ea0991168d4994637e8343e36",
                        "00d51fbc6c71a0094fa2cdd545b11c5c0c797324f1",
                        "040000000000000000000292fe77e70c12a4234c33",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect193r1",
                        "1.3.132.0.24",
                        "02000000000000000000000000000000000000000000008001",
                        "0017858feb7a98975169e171f77b4087de098ac8a911df7b01",
                        "00fdfb49bfe6c3a89facadaa7a1e5bbc7cc1c2e5d831478814",
                        "01f481bc5f0ff84a74ad6cdf6fdef4bf6179625372d8c0c5e1",
                        "0025e399f2903712ccf3ea9e3a1ad17fb0b3201b6af7ce1b05",
                        "01000000000000000000000000c7f34a778f443acc920eba49",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect193r2",
                        "1.3.132.0.25",
                        "02000000000000000000000000000000000000000000008001",
                        "0163f35a5137c2ce3ea6ed8667190b0bc43ecd69977702709b",
                        "00c9bb9e8927d4d64c377e2ab2856a5b16e3efb7f61d4316ae",
                        "00d9b67d192e0367c803f39e1a7e82ca14a651350aae617e8f",
                        "01ce94335607c304ac29e7defbd9ca01f596f927224cdecf6c",
                        "010000000000000000000000015aab561b005413ccd4ee99d5",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect233k1 [NIST K-233]",
                        "1.3.132.0.26",
                        "020000000000000000000000000000000000000004000000000000000001",
                        "000000000000000000000000000000000000000000000000000000000000",
                        "000000000000000000000000000000000000000000000000000000000001",
                        "017232ba853a7e731af129f22ff4149563a419c26bf50a4c9d6eefad6126",
                        "01db537dece819b7f70f555a67c427a8cd9bf18aeb9b56e0c11056fae6a3",
                        "008000000000000000000000000000069d5bb915bcd46efb1ad5f173abdf",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect233r1 [NIST B-233]",
                        "1.3.132.0.27",
                        "020000000000000000000000000000000000000004000000000000000001",
                        "000000000000000000000000000000000000000000000000000000000001",
                        "0066647ede6c332c7f8c0923bb58213b333b20e9ce4281fe115f7d8f90ad",
                        "00fac9dfcbac8313bb2139f1bb755fef65bc391f8b36f8f8eb7371fd558b",
                        "01006a08a41903350678e58528bebf8a0beff867a7ca36716f7e01f81052",
                        "01000000000000000000000000000013e974e72f8a6922031d2603cfe0d7",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect239k1",
                        "1.3.132.0.3",
                        "800000000000000000004000000000000000000000000000000000000001",
                        "000000000000000000000000000000000000000000000000000000000000",
                        "000000000000000000000000000000000000000000000000000000000001",
                        "29a0b6a887a983e9730988a68727a8b2d126c44cc2cc7b2a6555193035dc",
                        "76310804f12e549bdb011c103089e73510acb275fc312a5dc6b76553f0ca",
                        "2000000000000000000000000000005a79fec67cb6e91f1c1da800e478a5",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect283k1 [NIST K-283]",
                        "1.3.132.0.16",
                        "0800000000000000000000000000000000000000000000000000000000000000000010a1",
                        "000000000000000000000000000000000000000000000000000000000000000000000000",
                        "000000000000000000000000000000000000000000000000000000000000000000000001",
                        "0503213f78ca44883f1a3b8162f188e553cd265f23c1567a16876913b0c2ac2458492836",
                        "01ccda380f1c9e318d90f95d07e5426fe87e45c0e8184698e45962364e34116177dd2259",
                        "01ffffffffffffffffffffffffffffffffffe9ae2ed07577265dff7f94451e061e163c61",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect283r1 [NIST B-283]",
                        "1.3.132.0.17",
                        "0800000000000000000000000000000000000000000000000000000000000000000010a1",
                        "000000000000000000000000000000000000000000000000000000000000000000000001",
                        "027b680ac8b8596da5a4af8a19a0303fca97fd7645309fa2a581485af6263e313b79a2f5",
                        "05f939258db7dd90e1934f8c70b0dfec2eed25b8557eac9c80e2e198f8cdbecd86b12053",
                        "03676854fe24141cb98fe6d4b20d02b4516ff702350eddb0826779c813f0df45be8112f4",
                        "03ffffffffffffffffffffffffffffffffffef90399660fc938a90165b042a7cefadb307",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect409k1 [NIST K-409]",
                        "1.3.132.0.36",
                        "02000000000000000000000000000000000000000000000000000000000000000000000000000000008000000000000000000001",
                        "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                        "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001",
                        "0060f05f658f49c1ad3ab1890f7184210efd0987e307c84c27accfb8f9f67cc2c460189eb5aaaa62ee222eb1b35540cfe9023746",
                        "01e369050b7c4e42acba1dacbf04299c3460782f918ea427e6325165e9ea10e3da5f6c42e9c55215aa9ca27a5863ec48d8e0286b",
                        "007ffffffffffffffffffffffffffffffffffffffffffffffffffe5f83b2d4ea20400ec4557d5ed3e3e7ca5b4b5c83b8e01e5fcf",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect409r1 [NIST B-409]",
                        "1.3.132.0.37",
                        "02000000000000000000000000000000000000000000000000000000000000000000000000000000008000000000000000000001",
                        "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001",
                        "0021a5c2c8ee9feb5c4b9a753b7b476b7fd6422ef1f3dd674761fa99d6ac27c8a9a197b272822f6cd57a55aa4f50ae317b13545f",
                        "015d4860d088ddb3496b0c6064756260441cde4af1771d4db01ffe5b34e59703dc255a868a1180515603aeab60794e54bb7996a7",
                        "0061b1cfab6be5f32bbfa78324ed106a7636b9c5a7bd198d0158aa4f5488d08f38514f1fdf4b4f40d2181b3681c364ba0273c706",
                        "010000000000000000000000000000000000000000000000000001e2aad6a612f33307be5fa47c3c9e052f838164cd37d9a21173",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect571k1 [NIST K-571]",
                        "1.3.132.0.38",
                        "080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000425",
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001",
                        "026eb7a859923fbc82189631f8103fe4ac9ca2970012d5d46024804801841ca44370958493b205e647da304db4ceb08cbbd1ba39494776fb988b47174dca88c7e2945283a01c8972",
                        "0349dc807f4fbf374f4aeade3bca95314dd58cec9f307a54ffc61efc006d8a2c9d4979c0ac44aea74fbebbb9f772aedcb620b01a7ba7af1b320430c8591984f601cd4c143ef1c7a3",
                        "020000000000000000000000000000000000000000000000000000000000000000000000131850e1f19a63e4b391a8db917f4138b630d84be5d639381e91deb45cfe778f637c1001",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "sect571r1 [NIST B-571]",
                        "1.3.132.0.39",
                        "080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000425",
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001",
                        "02f40e7e2221f295de297117b7f3d62f5c6a97ffcb8ceff1cd6ba8ce4a9a18ad84ffabbd8efa59332be7ad6756a66e294afd185a78ff12aa520e4de739baca0c7ffeff7f2955727a",
                        "0303001d34b856296c16c0d40d3cd7750a93d1d2955fa80aa5f40fc8db7b2abdbde53950f4c0d293cdd711a35b67fb1499ae60038614f1394abfa3b4c850d927e1e7769c8eec2d19",
                        "037bf27342da639b6dccfffeb73d69d78c6c27a6009cbbca1980f8533921e8a684423e43bab08a576291af8f461bb2a8b3531d2f0485c19b16e2f1516e23dd3c1a4827af1b8ac15b",
                        "03ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe661ce18ff55987308059b186823851ec7dd9ca1161de93d5174d66e8382e9bb2fe84e47",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb191v1",
                        "1.2.840.10045.3.0.5",
                        "800000000000000000000000000000000000000000000201",
                        "2866537b676752636a68f56554e12640276b649ef7526267",
                        "2e45ef571f00786f67b0081b9495a3d95462f5de0aa185ec",
                        "36b3daf8a23206f9c4f299d7b21a9c369137f2c84ae1aa0d",
                        "765be73433b3f95e332932e70ea245ca2418ea0ef98018fb",
                        "40000000000000000000000004a20e90c39067c893bbb9a5",
                        2)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb191v2",
                        "1.2.840.10045.3.0.6",
                        "800000000000000000000000000000000000000000000201",
                        "401028774d7777c7b7666d1366ea432071274f89ff01e718",
                        "0620048d28bcbd03b6249c99182b7c8cd19700c362c46a01",
                        "3809b2b7cc1b28cc5a87926aad83fd28789e81e2c9e3bf10",
                        "17434386626d14f3dbf01760d9213a3e1cf37aec437d668a",
                        "20000000000000000000000050508cb89f652824e06b8173",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb191v3",
                        "1.2.840.10045.3.0.7",
                        "800000000000000000000000000000000000000000000201",
                        "6c01074756099122221056911c77d77e77a777e7e7e77fcb",
                        "71fe1af926cf847989efef8db459f66394d90f32ad3f15e8",
                        "375d4ce24fde434489de8746e71786015009e66e38a926dd",
                        "545a39176196575d985999366e6ad34ce0a77cd7127b06be",
                        "155555555555555555555555610c0b196812bfb6288a3ea3",
                        6)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb239v1",
                        "1.2.840.10045.3.0.11",
                        "800000000000000000000000000000000000000000000000001000000001",
                        "32010857077c5431123a46b808906756f543423e8d27877578125778ac76",
                        "790408f2eedaf392b012edefb3392f30f4327c0ca3f31fc383c422aa8c16",
                        "57927098fa932e7c0a96d3fd5b706ef7e5f5c156e16b7e7c86038552e91d",
                        "61d8ee5077c33fecf6f1a16b268de469c3c7744ea9a971649fc7a9616305",
                        "2000000000000000000000000000000f4d42ffe1492a4993f1cad666e447",
                        4)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb239v2",
                        "1.2.840.10045.3.0.12",
                        "800000000000000000000000000000000000000000000000001000000001",
                        "4230017757a767fae42398569b746325d45313af0766266479b75654e65f",
                        "5037ea654196cff0cd82b2c14a2fcf2e3ff8775285b545722f03eacdb74b",
                        "28f9d04e900069c8dc47a08534fe76d2b900b7d7ef31f5709f200c4ca205",
                        "5667334c45aff3b5a03bad9dd75e2c71a99362567d5453f7fa6e227ec833",
                        "1555555555555555555555555555553c6f2885259c31e3fcdf154624522d",
                        6)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb239v3",
                        "1.2.840.10045.3.0.13",
                        "800000000000000000000000000000000000000000000000001000000001",
                        "01238774666a67766d6676f778e676b66999176666e687666d8766c66a9f",
                        "6a941977ba9f6a435199acfc51067ed587f519c5ecb541b8e44111de1d40",
                        "70f6e9d04d289c4e89913ce3530bfde903977d42b146d539bf1bde4e9c92",
                        "2e5a0eaf6e5e1305b9004dce5c0ed7fe59a35608f33837c816d80b79f461",
                        "0cccccccccccccccccccccccccccccac4912d2d9df903ef9888b8a0e4cff",
                        0xA)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb359v1",
                        "1.2.840.10045.3.0.18",
                        "800000000000000000000000000000000000000000000000000000000000000000000000100000000000000001",
                        "5667676a654b20754f356ea92017d946567c46675556f19556a04616b567d223a5e05656fb549016a96656a557",
                        "2472e2d0197c49363f1fe7f5b6db075d52b6947d135d8ca445805d39bc345626089687742b6329e70680231988",
                        "3c258ef3047767e7ede0f1fdaa79daee3841366a132e163aced4ed2401df9c6bdcde98e8e707c07a2239b1b097",
                        "53d7e08529547048121e9c95f3791dd804963948f34fae7bf44ea82365dc7868fe57e4ae2de211305a407104bd",
                        "01af286bca1af286bca1af286bca1af286bca1af286bc9fb8f6b85c556892c20a7eb964fe7719e74f490758d3b",
                        0x4C)
        );

        ecCurveDefinitions.add(
                new ECCurveDefinition(
                        "X9.62 c2tnb431r1",
                        "1.2.840.10045.3.0.20",
                        "800000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000001",
                        "1a827ef00dd6fc0e234caf046c6a5d8a85395b236cc4ad2cf32a0cadbdc9ddf620b0eb9906d0957f6c6feacd615468df104de296cd8f",
                        "10d9b4a3d9047d8b154359abfb1b7f5485b04ceb868237ddc9deda982a679a5a919b626d4e50a8dd731b107a9962381fb5d807bf2618",
                        "120fc05d3c67a99de161d2f4092622feca701be4f50f4758714e8a87bbf2a658ef8c21e7c5efe965361f6c2999c0c247b0dbd70ce6b7",
                        "20d0af8903a96f8d5fa2c255745d3c451b302c9346d9b7e485e7bce41f6b591f3e8f6addcbb0bc4c2f947a7de1a89b625d6a598b3760",
                        "0340340340340340340340340340340340340340340340340340340323c313fab50589703b5ec68d3587fec60d161cc149c1ad4a91",
                        0x2760)
        );
    }

    public static String getOIDFromPublicKey(ECPublicKey ecPublicKey) {
        ECParameterSpec ecParameterSpec = ecPublicKey.getParams();
        BigInteger order = ecParameterSpec.getOrder();
        BigInteger affineX = ecParameterSpec.getGenerator().getAffineX();
        BigInteger affineY = ecParameterSpec.getGenerator().getAffineY();
        BigInteger a = ecParameterSpec.getCurve().getA();
        BigInteger b = ecParameterSpec.getCurve().getB();
        int h = ecParameterSpec.getCofactor();
        ECField ecField = ecParameterSpec.getCurve().getField();
        BigInteger field;
        if (ecField instanceof ECFieldFp) {
            ECFieldFp ecFieldFp = (ECFieldFp) ecField;
            field = ecFieldFp.getP();
        } else {
            ECFieldF2m ecFieldF2m = (ECFieldF2m) ecField;
            field = ecFieldF2m.getReductionPolynomial();
        }

        Iterator<ECCurveDefinition> ecCurveDefinitionIterator = ecCurveDefinitions.iterator();
        while (ecCurveDefinitionIterator.hasNext()) {
            ECCurveDefinition ecCurveDefinition = ecCurveDefinitionIterator.next();
            String oid = ecCurveDefinition.equals(field, a, b, affineX, affineY, order, h);
            if (oid != null) {
                return oid;
            }
        }
        return null;
    }

    public static ECCurveDefinition getECCurveDefinition(String oid) {
        Iterator<ECCurveDefinition> ecCurveDefinitionIterator = ecCurveDefinitions.iterator();
        while (ecCurveDefinitionIterator.hasNext()) {
            ECCurveDefinition ecCurveDefinition = ecCurveDefinitionIterator.next();
            if (ecCurveDefinition.getOid().equals(oid)) {
                return ecCurveDefinition;
            }
        }
        return null;
    }

    public static class ECCurveDefinition {

        private final String name;
        private final String oid;
        private final String field;
        private final String a;
        private final String b;
        private final String x;
        private final String y;
        private final String n;
        private final int h;

        public ECCurveDefinition(String name, String oid, String field, String a, String b, String x, String y, String n, int h) {
            this.name = name;
            this.oid = oid;
            this.field = field;
            this.a = a;
            this.b = b;
            this.x = x;
            this.y = y;
            this.n = n;
            this.h = h;
        }

        /**
         * returns the ec oid if parameter are equal to this definition
         */
        public String equals(BigInteger field, BigInteger a, BigInteger b, BigInteger x, BigInteger y, BigInteger n, int h) {
            if (this.field.equals(field.toString(16))
                    && this.a.equals(a.toString(16))
                    && this.b.equals(b.toString(16))
                    && this.x.equals(x.toString(16))
                    && this.y.equals(y.toString(16))
                    && this.n.equals(n.toString(16))
                    && this.h == h) {
                return this.oid;
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getOid() {
            return oid;
        }

        public String getField() {
            return field;
        }

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getN() {
            return n;
        }

        public int getH() {
            return h;
        }
    }

    public static byte[] encodePoint(ECPoint ecPoint, EllipticCurve ellipticCurve) {
        int size = (ellipticCurve.getField().getFieldSize() + 7) / 8;
        byte[] affineXBytes = stripLeadingZeros(ecPoint.getAffineX().toByteArray());
        byte[] affineYBytes = stripLeadingZeros(ecPoint.getAffineY().toByteArray());
        byte[] encodedBytes = new byte[size * 2 + 1];
        encodedBytes[0] = 0x04; //uncompressed
        System.arraycopy(affineXBytes, 0, encodedBytes, size - affineXBytes.length + 1, affineXBytes.length);
        System.arraycopy(affineYBytes, 0, encodedBytes, encodedBytes.length - affineYBytes.length, affineYBytes.length);
        return encodedBytes;
    }

    public static ECPoint decodePoint(byte[] encodedBytes, EllipticCurve elliptiCcurve) {
        if (encodedBytes[0] != 0x04) {
            throw new IllegalArgumentException("Only uncompressed format is supported");
        }

        int size = (elliptiCcurve.getField().getFieldSize() + 7) / 8;
        byte[] affineXBytes = new byte[size];
        byte[] affineYBytes = new byte[size];
        System.arraycopy(encodedBytes, 1, affineXBytes, 0, size);
        System.arraycopy(encodedBytes, size + 1, affineYBytes, 0, size);
        return new ECPoint(new BigInteger(1, affineXBytes), new BigInteger(1, affineYBytes));
    }

    public static byte[] stripLeadingZeros(byte[] bytes) {
        int i;
        for (i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] != 0) {
                break;
            }
        }

        if (i == 0) {
            return bytes;
        } else {
            byte[] stripped = new byte[bytes.length - i];
            System.arraycopy(bytes, i, stripped, 0, stripped.length);
            return stripped;
        }
    }
}
