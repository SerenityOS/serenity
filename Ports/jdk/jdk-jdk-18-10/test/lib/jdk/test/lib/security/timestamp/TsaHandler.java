/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.security.timestamp;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import sun.security.util.KnownOIDs;
import sun.security.util.SignatureUtil;
import sun.security.x509.AlgorithmId;

/**
 * A {@link HttpHandler} receiving time-stamping request and returning response.
 *
 * <p> It can be initialized by the given key store and key passphrase if any.
 * At runtime, the handler can accept the below parameters via {@code HTTP}
 * query string.
 *
 * <ul>
 *   <li><b>alias</b>: Certificate alias. The default value is the alias of the
 *   first found time stamping certificate in the key store.</li>
 *   <li><b>sigAlgo</b>: Signature algorithm. The default value is determined by
 *   the selected certificate.</li>
 *   <li><b>version</b>: The time-stamping request/token version.</li>
 *   <li><b>status</b>: The time-stamping status.</li>
 *   <li><b>policyId</b>: The policy ID.</li>
 *   <li><b>digestAlgo</b>: The digest algorithm used by the hashed message.</li>
 *   <li><b>hashedMessage</b>: The hashed message.</li>
 *   <li><b>serialNumber</b>: The serial number.</li>
 *   <li><b>genTime</b>: The time at which the time-stamp token has been created.
 *   The format is {@code yyyy-MM-dd}.</li>
 *   <li><b>nonce</b>: The nonce.</li>
 *   <li><b>certReq</b>: Indicate if require {@code TSA} server certificate.</li>
 * </ul>
 *
 * <p> All the parameters are optional.
 *
 * <p> The {@code HTTP} query string format looks like: <br>
 * {@literal alias=<certificate alias>&sigAlgo=<signature algorithm>&date=<yyyy-MM-dd>&status=<status number>}
 */
public class TsaHandler implements HttpHandler {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public final KeyStore keyStore;
    public final char[] passphrase;

    /**
     * Create a handler with a given key store and private key passphrase.
     *
     * @param keyStore a key store
     * @param passphrase the private key passphrase
     */
    public TsaHandler(KeyStore keyStore, String passphrase) {
        this.keyStore = keyStore;
        this.passphrase = passphrase == null
                          ? null
                          : passphrase.toCharArray();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            byte[] responseData = createSigner(exchange).sign();
            exchange.getResponseHeaders().set(
                    "Content-Type", "application/timestamp-reply");
            exchange.sendResponseHeaders(200, responseData.length);
            exchange.getResponseBody().write(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
        } finally {
            exchange.close();
        }
    }

    /**
     * Create a {@link TsaSigner} instance with the time-stamping request and
     * application parameters from a given {@link HttpExchange} instance.
     *
     * @param exchange a {@link HttpExchange} instance
     * @return a {@link TsaSigner} instance
     * @throws Exception the exception
     */
    protected TsaSigner createSigner(HttpExchange exchange) throws Exception {
        TsaParam param = getParam(exchange.getRequestURI());

        String alias = getSignerAlias(param.alias(), param.sigAlgo());
        SignerEntry signerEntry = createSignerEntry(alias);

        byte[] requestData = exchange.getRequestBody().readAllBytes();
        return new TsaSigner(signerEntry, requestData, param);
    }

    /**
     * Create a {@link SignerEntry} from a given key store alias.
     *
     * @param alias the keys tore alias
     * @return the {@link SignerEntry} instance
     * @throws Exception the exception
     */
    protected SignerEntry createSignerEntry(String alias) throws Exception {
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passphrase);
        Certificate[] certChain = keyStore.getCertificateChain(alias);
        X509Certificate[] x509CertChain = Arrays.copyOf(
                certChain, certChain.length, X509Certificate[].class);
        SignerEntry signerEntry = new SignerEntry(privateKey, x509CertChain);
        return signerEntry;
    }

    /**
     * Parse parameters from {@link URI}.
     *
     * @param uri the {@link URI} from {@code HTTP} request
     * @return a {@link TsaParam} instance wrapping application parameters
     */
    protected TsaParam getParam(URI uri) {
        String query = uri.getQuery();

        TsaParam param = TsaParam.newInstance();
        if (query != null) {
            for (String bufParam : query.split("&")) {
                String[] pair = bufParam.split("=");
                if ("alias".equalsIgnoreCase(pair[0])) {
                    param.alias(pair[1]);
                    System.out.println("alias: " + param.alias());
                } else if ("sigAlgo".equalsIgnoreCase(pair[0])) {
                    param.sigAlgo(pair[1]);
                    System.out.println("sigAlgo: " + param.sigAlgo());
                } else if ("version".equalsIgnoreCase(pair[0])) {
                    param.version(Integer.valueOf(pair[1]));
                    System.out.println("version: " + param.version());
                } else if ("status".equalsIgnoreCase(pair[0])) {
                    param.status(Integer.valueOf(pair[1]));
                    System.out.println("status: " + param.status());
                } else if ("policyId".equalsIgnoreCase(pair[0])) {
                    param.policyId(pair[1]);
                    System.out.println("policyId: " + param.policyId());
                } else if ("digestAlgo".equalsIgnoreCase(pair[0])) {
                    param.digestAlgo(pair[1]);
                    System.out.println("digestAlgo: " + param.digestAlgo());
                } else if ("serialNumber".equalsIgnoreCase(pair[0])) {
                    param.serialNumber(new BigInteger(pair[1]));
                    System.out.println("serialNumber: " + param.serialNumber());
                } else if ("nonce".equalsIgnoreCase(pair[0])) {
                    param.nonce(new BigInteger(pair[1]));
                    System.out.println("nonce: " + param.nonce());
                } else if ("date".equalsIgnoreCase(pair[0])) {
                    Date genTime = Date.from(LocalDate.parse(pair[1], FORMATTER)
                            .atStartOfDay().atZone(ZoneId.systemDefault())
                            .toInstant());
                    param.genTime(genTime);
                    System.out.println("genTime: " + param.genTime());
                } else if ("certReq".equalsIgnoreCase(pair[0])) {
                    param.certReq(Boolean.valueOf(pair[1]));
                    System.out.println("certReq: " + param.certReq());
                }
            }
        }

        return param;
    }

    /**
     * Determine the signer certificate alias.
     *
     * @param alias the specified alias
     * @param sigAlgo the specified signature algorithm
     * @return the signer alias
     * @throws Exception the exception
     */
    protected String getSignerAlias(String alias, String sigAlgo)
            throws Exception {
        if (alias == null) {
            String keyAlgo;
            if (sigAlgo == null) {
                keyAlgo = null;
            } else {
                String lower = sigAlgo.toLowerCase(Locale.ROOT);
                int pos = lower.indexOf("with");
                if (pos < 0) {
                    keyAlgo = sigAlgo;
                } else {
                    keyAlgo = sigAlgo.substring(pos + 4);
                }
            }
            Enumeration<String> aliases = keyStore.aliases();
            while(aliases.hasMoreElements()) {
                String bufAlias = aliases.nextElement();
                X509Certificate bufCert
                        = (X509Certificate) keyStore.getCertificate(bufAlias);
                // The certificate must have critical extended key usage time
                // stamping, and must match the key algorithm if any.
                List<String> eku = bufCert.getExtendedKeyUsage();
                if (eku != null && eku.contains(KnownOIDs.KP_TimeStamping.value())
                        && (keyAlgo == null || keyAlgo.equalsIgnoreCase(
                                bufCert.getPublicKey().getAlgorithm()))) {
                    return bufAlias;
                }
            }
        }

        return alias;
    }
}
