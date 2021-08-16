/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.security.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.spec.DHParameterSpec;

/**
 * Predefined default DH ephemeral parameters.
 */
final class PredefinedDHParameterSpecs {

    //
    // Default DH ephemeral parameters
    //
    private static final BigInteger p512 = new BigInteger(       // generated
            "D87780E15FF50B4ABBE89870188B049406B5BEA98AB23A02" +
            "41D88EA75B7755E669C08093D3F0CA7FC3A5A25CF067DCB9" +
            "A43DD89D1D90921C6328884461E0B6D3", 16);
    private static final BigInteger p768 = new BigInteger(       // RFC 2409
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF", 16);

    private static final BigInteger p1024 = new BigInteger(      // RFC 2409
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381" +
            "FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p1536 = new BigInteger(      // RFC 3526
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
            "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
            "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p2048 = new BigInteger(      // TLS FFDHE
            "FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1" +
            "D8B9C583CE2D3695A9E13641146433FBCC939DCE249B3EF9" +
            "7D2FE363630C75D8F681B202AEC4617AD3DF1ED5D5FD6561" +
            "2433F51F5F066ED0856365553DED1AF3B557135E7F57C935" +
            "984F0C70E0E68B77E2A689DAF3EFE8721DF158A136ADE735" +
            "30ACCA4F483A797ABC0AB182B324FB61D108A94BB2C8E3FB" +
            "B96ADAB760D7F4681D4F42A3DE394DF4AE56EDE76372BB19" +
            "0B07A7C8EE0A6D709E02FCE1CDF7E2ECC03404CD28342F61" +
            "9172FE9CE98583FF8E4F1232EEF28183C3FE3B1B4C6FAD73" +
            "3BB5FCBC2EC22005C58EF1837D1683B2C6F34A26C1B2EFFA" +
            "886B423861285C97FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p3072 = new BigInteger(      // TLS FFDHE
            "FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1" +
            "D8B9C583CE2D3695A9E13641146433FBCC939DCE249B3EF9" +
            "7D2FE363630C75D8F681B202AEC4617AD3DF1ED5D5FD6561" +
            "2433F51F5F066ED0856365553DED1AF3B557135E7F57C935" +
            "984F0C70E0E68B77E2A689DAF3EFE8721DF158A136ADE735" +
            "30ACCA4F483A797ABC0AB182B324FB61D108A94BB2C8E3FB" +
            "B96ADAB760D7F4681D4F42A3DE394DF4AE56EDE76372BB19" +
            "0B07A7C8EE0A6D709E02FCE1CDF7E2ECC03404CD28342F61" +
            "9172FE9CE98583FF8E4F1232EEF28183C3FE3B1B4C6FAD73" +
            "3BB5FCBC2EC22005C58EF1837D1683B2C6F34A26C1B2EFFA" +
            "886B4238611FCFDCDE355B3B6519035BBC34F4DEF99C0238" +
            "61B46FC9D6E6C9077AD91D2691F7F7EE598CB0FAC186D91C" +
            "AEFE130985139270B4130C93BC437944F4FD4452E2D74DD3" +
            "64F2E21E71F54BFF5CAE82AB9C9DF69EE86D2BC522363A0D" +
            "ABC521979B0DEADA1DBF9A42D5C4484E0ABCD06BFA53DDEF" +
            "3C1B20EE3FD59D7C25E41D2B66C62E37FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p4096 = new BigInteger(      // TLS FFDHE
            "FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1" +
            "D8B9C583CE2D3695A9E13641146433FBCC939DCE249B3EF9" +
            "7D2FE363630C75D8F681B202AEC4617AD3DF1ED5D5FD6561" +
            "2433F51F5F066ED0856365553DED1AF3B557135E7F57C935" +
            "984F0C70E0E68B77E2A689DAF3EFE8721DF158A136ADE735" +
            "30ACCA4F483A797ABC0AB182B324FB61D108A94BB2C8E3FB" +
            "B96ADAB760D7F4681D4F42A3DE394DF4AE56EDE76372BB19" +
            "0B07A7C8EE0A6D709E02FCE1CDF7E2ECC03404CD28342F61" +
            "9172FE9CE98583FF8E4F1232EEF28183C3FE3B1B4C6FAD73" +
            "3BB5FCBC2EC22005C58EF1837D1683B2C6F34A26C1B2EFFA" +
            "886B4238611FCFDCDE355B3B6519035BBC34F4DEF99C0238" +
            "61B46FC9D6E6C9077AD91D2691F7F7EE598CB0FAC186D91C" +
            "AEFE130985139270B4130C93BC437944F4FD4452E2D74DD3" +
            "64F2E21E71F54BFF5CAE82AB9C9DF69EE86D2BC522363A0D" +
            "ABC521979B0DEADA1DBF9A42D5C4484E0ABCD06BFA53DDEF" +
            "3C1B20EE3FD59D7C25E41D2B669E1EF16E6F52C3164DF4FB" +
            "7930E9E4E58857B6AC7D5F42D69F6D187763CF1D55034004" +
            "87F55BA57E31CC7A7135C886EFB4318AED6A1E012D9E6832" +
            "A907600A918130C46DC778F971AD0038092999A333CB8B7A" +
            "1A1DB93D7140003C2A4ECEA9F98D0ACC0A8291CDCEC97DCF" +
            "8EC9B55A7F88A46B4DB5A851F44182E1C68A007E5E655F6A" +
            "FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p6144 = new BigInteger(      // TLS FFDHE
            "FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1" +
            "D8B9C583CE2D3695A9E13641146433FBCC939DCE249B3EF9" +
            "7D2FE363630C75D8F681B202AEC4617AD3DF1ED5D5FD6561" +
            "2433F51F5F066ED0856365553DED1AF3B557135E7F57C935" +
            "984F0C70E0E68B77E2A689DAF3EFE8721DF158A136ADE735" +
            "30ACCA4F483A797ABC0AB182B324FB61D108A94BB2C8E3FB" +
            "B96ADAB760D7F4681D4F42A3DE394DF4AE56EDE76372BB19" +
            "0B07A7C8EE0A6D709E02FCE1CDF7E2ECC03404CD28342F61" +
            "9172FE9CE98583FF8E4F1232EEF28183C3FE3B1B4C6FAD73" +
            "3BB5FCBC2EC22005C58EF1837D1683B2C6F34A26C1B2EFFA" +
            "886B4238611FCFDCDE355B3B6519035BBC34F4DEF99C0238" +
            "61B46FC9D6E6C9077AD91D2691F7F7EE598CB0FAC186D91C" +
            "AEFE130985139270B4130C93BC437944F4FD4452E2D74DD3" +
            "64F2E21E71F54BFF5CAE82AB9C9DF69EE86D2BC522363A0D" +
            "ABC521979B0DEADA1DBF9A42D5C4484E0ABCD06BFA53DDEF" +
            "3C1B20EE3FD59D7C25E41D2B669E1EF16E6F52C3164DF4FB" +
            "7930E9E4E58857B6AC7D5F42D69F6D187763CF1D55034004" +
            "87F55BA57E31CC7A7135C886EFB4318AED6A1E012D9E6832" +
            "A907600A918130C46DC778F971AD0038092999A333CB8B7A" +
            "1A1DB93D7140003C2A4ECEA9F98D0ACC0A8291CDCEC97DCF" +
            "8EC9B55A7F88A46B4DB5A851F44182E1C68A007E5E0DD902" +
            "0BFD64B645036C7A4E677D2C38532A3A23BA4442CAF53EA6" +
            "3BB454329B7624C8917BDD64B1C0FD4CB38E8C334C701C3A" +
            "CDAD0657FCCFEC719B1F5C3E4E46041F388147FB4CFDB477" +
            "A52471F7A9A96910B855322EDB6340D8A00EF092350511E3" +
            "0ABEC1FFF9E3A26E7FB29F8C183023C3587E38DA0077D9B4" +
            "763E4E4B94B2BBC194C6651E77CAF992EEAAC0232A281BF6" +
            "B3A739C1226116820AE8DB5847A67CBEF9C9091B462D538C" +
            "D72B03746AE77F5E62292C311562A846505DC82DB854338A" +
            "E49F5235C95B91178CCF2DD5CACEF403EC9D1810C6272B04" +
            "5B3B71F9DC6B80D63FDD4A8E9ADB1E6962A69526D43161C1" +
            "A41D570D7938DAD4A40E329CD0E40E65FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger p8192 = new BigInteger(      // TLS FFDHE
            "FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1" +
            "D8B9C583CE2D3695A9E13641146433FBCC939DCE249B3EF9" +
            "7D2FE363630C75D8F681B202AEC4617AD3DF1ED5D5FD6561" +
            "2433F51F5F066ED0856365553DED1AF3B557135E7F57C935" +
            "984F0C70E0E68B77E2A689DAF3EFE8721DF158A136ADE735" +
            "30ACCA4F483A797ABC0AB182B324FB61D108A94BB2C8E3FB" +
            "B96ADAB760D7F4681D4F42A3DE394DF4AE56EDE76372BB19" +
            "0B07A7C8EE0A6D709E02FCE1CDF7E2ECC03404CD28342F61" +
            "9172FE9CE98583FF8E4F1232EEF28183C3FE3B1B4C6FAD73" +
            "3BB5FCBC2EC22005C58EF1837D1683B2C6F34A26C1B2EFFA" +
            "886B4238611FCFDCDE355B3B6519035BBC34F4DEF99C0238" +
            "61B46FC9D6E6C9077AD91D2691F7F7EE598CB0FAC186D91C" +
            "AEFE130985139270B4130C93BC437944F4FD4452E2D74DD3" +
            "64F2E21E71F54BFF5CAE82AB9C9DF69EE86D2BC522363A0D" +
            "ABC521979B0DEADA1DBF9A42D5C4484E0ABCD06BFA53DDEF" +
            "3C1B20EE3FD59D7C25E41D2B669E1EF16E6F52C3164DF4FB" +
            "7930E9E4E58857B6AC7D5F42D69F6D187763CF1D55034004" +
            "87F55BA57E31CC7A7135C886EFB4318AED6A1E012D9E6832" +
            "A907600A918130C46DC778F971AD0038092999A333CB8B7A" +
            "1A1DB93D7140003C2A4ECEA9F98D0ACC0A8291CDCEC97DCF" +
            "8EC9B55A7F88A46B4DB5A851F44182E1C68A007E5E0DD902" +
            "0BFD64B645036C7A4E677D2C38532A3A23BA4442CAF53EA6" +
            "3BB454329B7624C8917BDD64B1C0FD4CB38E8C334C701C3A" +
            "CDAD0657FCCFEC719B1F5C3E4E46041F388147FB4CFDB477" +
            "A52471F7A9A96910B855322EDB6340D8A00EF092350511E3" +
            "0ABEC1FFF9E3A26E7FB29F8C183023C3587E38DA0077D9B4" +
            "763E4E4B94B2BBC194C6651E77CAF992EEAAC0232A281BF6" +
            "B3A739C1226116820AE8DB5847A67CBEF9C9091B462D538C" +
            "D72B03746AE77F5E62292C311562A846505DC82DB854338A" +
            "E49F5235C95B91178CCF2DD5CACEF403EC9D1810C6272B04" +
            "5B3B71F9DC6B80D63FDD4A8E9ADB1E6962A69526D43161C1" +
            "A41D570D7938DAD4A40E329CCFF46AAA36AD004CF600C838" +
            "1E425A31D951AE64FDB23FCEC9509D43687FEB69EDD1CC5E" +
            "0B8CC3BDF64B10EF86B63142A3AB8829555B2F747C932665" +
            "CB2C0F1CC01BD70229388839D2AF05E454504AC78B758282" +
            "2846C0BA35C35F5C59160CC046FD8251541FC68C9C86B022" +
            "BB7099876A460E7451A8A93109703FEE1C217E6C3826E52C" +
            "51AA691E0E423CFC99E9E31650C1217B624816CDAD9A95F9" +
            "D5B8019488D9C0A0A1FE3075A577E23183F81D4A3F2FA457" +
            "1EFC8CE0BA8A4FE8B6855DFE72B0A66EDED2FBABFBE58A30" +
            "FAFABE1C5D71A87E2F741EF8C1FE86FEA6BBFDE530677F0D" +
            "97D11D49F7A8443D0822E506A9F4614E011E2A94838FF88C" +
            "D68C8BB7C5C6424CFFFFFFFFFFFFFFFF", 16);

    private static final BigInteger[] supportedPrimes = {
            p512, p768, p1024, p1536, p2048, p3072, p4096, p6144, p8192};

    private static final BigInteger[] ffdhePrimes = {
            p2048, p3072, p4096, p6144, p8192};

    // a measure of the uncertainty that prime modulus p is not a prime
    //
    // see BigInteger.isProbablePrime(int certainty)
    private static final int PRIME_CERTAINTY = 120;

    // the known security property, jdk.tls.server.defaultDHEParameters
    private static final String PROPERTY_NAME =
            "jdk.tls.server.defaultDHEParameters";

    private static final Pattern spacesPattern = Pattern.compile("\\s+");

    private static final Pattern syntaxPattern = Pattern.compile(
            "(\\{[0-9A-Fa-f]+,[0-9A-Fa-f]+\\})" +
            "(,\\{[0-9A-Fa-f]+,[0-9A-Fa-f]+\\})*");

    private static final Pattern paramsPattern = Pattern.compile(
            "\\{([0-9A-Fa-f]+),([0-9A-Fa-f]+)\\}");

    // cache of predefined default DH ephemeral parameters
    static final Map<Integer, DHParameterSpec> definedParams;

    // cache of Finite Field DH Ephemeral parameters (RFC 7919/FFDHE)
    static final Map<Integer, DHParameterSpec> ffdheParams;

    static {
        @SuppressWarnings("removal")
        String property = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return Security.getProperty(PROPERTY_NAME);
                }
            });

        if (property != null && !property.isEmpty()) {
            // remove double quote marks from beginning/end of the property
            if (property.length() >= 2 && property.charAt(0) == '"' &&
                    property.charAt(property.length() - 1) == '"') {
                property = property.substring(1, property.length() - 1);
            }

            property = property.trim();
        }

        if (property != null && !property.isEmpty()) {
            Matcher spacesMatcher = spacesPattern.matcher(property);
            property = spacesMatcher.replaceAll("");

            if (SSLLogger.isOn && SSLLogger.isOn("sslctx")) {
                SSLLogger.fine(
                        "The Security Property " +
                        PROPERTY_NAME + ": " + property);
            }
        }

        Map<Integer,DHParameterSpec> defaultParams = new HashMap<>();
        if (property != null && !property.isEmpty()) {
            Matcher syntaxMatcher = syntaxPattern.matcher(property);
            if (syntaxMatcher.matches()) {
                Matcher paramsFinder = paramsPattern.matcher(property);
                while(paramsFinder.find()) {
                    String primeModulus = paramsFinder.group(1);
                    BigInteger p = new BigInteger(primeModulus, 16);
                    if (!p.isProbablePrime(PRIME_CERTAINTY)) {
                        if (SSLLogger.isOn && SSLLogger.isOn("sslctx")) {
                            SSLLogger.fine(
                                "Prime modulus p in Security Property, " +
                                PROPERTY_NAME + ", is not a prime: " +
                                primeModulus);
                        }

                        continue;
                    }

                    String baseGenerator = paramsFinder.group(2);
                    BigInteger g = new BigInteger(baseGenerator, 16);

                    DHParameterSpec spec = new DHParameterSpec(p, g);
                    int primeLen = p.bitLength();
                    defaultParams.put(primeLen, spec);
                }
            } else if (SSLLogger.isOn && SSLLogger.isOn("sslctx")) {
                SSLLogger.fine("Invalid Security Property, " +
                        PROPERTY_NAME + ", definition");
            }
        }

        Map<Integer,DHParameterSpec> tempFFDHEs = new HashMap<>();
        for (BigInteger p : ffdhePrimes) {
            int primeLen = p.bitLength();
            DHParameterSpec dhps = new DHParameterSpec(p, BigInteger.TWO);
            tempFFDHEs.put(primeLen, dhps);
            defaultParams.putIfAbsent(primeLen, dhps);
        }

        for (BigInteger p : supportedPrimes) {
            int primeLen = p.bitLength();
            if (defaultParams.get(primeLen) == null) {
                defaultParams.put(primeLen,
                    new DHParameterSpec(p, BigInteger.TWO));
            }
        }

        ffdheParams =
            Collections.<Integer,DHParameterSpec>unmodifiableMap(tempFFDHEs);
        definedParams =
            Collections.<Integer,DHParameterSpec>unmodifiableMap(defaultParams);
    }
}
