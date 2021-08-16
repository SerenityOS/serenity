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
import java.util.Objects;

/**
 * A default implementation for {@link RespInterceptor}.
 */
public class DefaultRespInterceptor<T extends TsaParam>
        implements RespInterceptor {

    private final T param;

    public DefaultRespInterceptor(T param) {
        Objects.requireNonNull(param);

        this.param = param;
    }

    @Override
    public X509Certificate[] getSignerCertChain(
            X509Certificate[] signerCertChain, boolean certReq)
            throws Exception {
        X509Certificate[] certChain = RespInterceptor.super.getSignerCertChain(
                signerCertChain, certReq);
        if (param.certReq() == null) {
            return certChain;
        } else if (param.certReq()) {
            return signerCertChain;
        } else {
            return new X509Certificate[0];
        }
    }

    @Override
    public String getSigAlgo(String sigAlgo) throws Exception {
        return param.sigAlgo() == null ? sigAlgo : param.sigAlgo();
    }

    @Override
    public TsaParam getRespParam(TsaParam reqParam) {
        TsaParam respParam = RespInterceptor.super.getRespParam(reqParam);
        respParam.version(param.version() == null
                ? respParam.version() : param.version());
        respParam.status(param.status() == null
                ? respParam.status() : param.status());
        respParam.policyId(param.policyId() == null
                ? respParam.policyId() : param.policyId());
        respParam.digestAlgo(param.digestAlgo() == null
                ? respParam.digestAlgo() : param.digestAlgo());
        respParam.hashedMessage(param.hashedMessage() == null
                ? respParam.hashedMessage() : param.hashedMessage());
        respParam.genTime(param.genTime() == null
                ? respParam.genTime() : param.genTime());
        return respParam;
    }
}
