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

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

/**
 * This interceptor defines some extensions for generating time-stamping
 * response.
 */
public interface RespInterceptor {

    /**
     * Return an alternative signer certificate chain if necessary.
     * In default case, the returned certificate chain is empty if the server
     * certificate chain is not required.
     *
     * @param signerCertChain the original signer certificate chain
     * @param certReq indicate if require {@code TSA} server certificate
     * @return the signer certificate chain
     * @throws Exception the exception
     */
    default X509Certificate[] getSignerCertChain(
            X509Certificate[] signerCertChain, boolean certReq)
            throws Exception {
        return certReq ? signerCertChain : new X509Certificate[0];
    }

    /**
     * Return an alternative signature algorithm if necessary.
     *
     * @param sigAlgo the original signature algorithm
     * @return the signature algorithm
     * @throws Exception the exception
     */
    default public String getSigAlgo(String sigAlgo) throws Exception {
        return sigAlgo;
    }

    /**
     * Return the TSA response parameters.
     *
     * @param reqParam the TSA request parameters.
     * @return the TSA response parameters.
     */
    default public TsaParam getRespParam(TsaParam reqParam) {
        TsaParam respParam = TsaParam.newInstance();
        respParam.version(1);
        respParam.status(0);
        respParam.policyId(reqParam.policyId() == null
                ? "2.3.4" : reqParam.policyId());
        respParam.digestAlgo(reqParam.digestAlgo());
        respParam.hashedMessage(reqParam.hashedMessage());
        respParam.serialNumber(BigInteger.ONE);
        respParam.genTime(Date.from(Instant.now()));
        respParam.nonce(reqParam.nonce());
        return respParam;
    }
}
