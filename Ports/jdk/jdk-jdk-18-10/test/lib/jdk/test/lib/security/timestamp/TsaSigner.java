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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

import jdk.test.lib.hexdump.HexPrinter;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.*;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

/**
 * Process time-stamping request and generate signed data.
 */
public class TsaSigner {

    private static final boolean DEBUG = Boolean.getBoolean("test.debug");
    private static final HexPrinter HEX_PRINTER = HexPrinter.simple();

    protected final SignerEntry signerEntry;
    protected final byte[] requestData;
    private final RespInterceptor interceptor;

    /**
     * Initialization.
     *
     * @param signerEntry a {@link SignerEntry} instance
     * @param requestData the time-stamping request data
     * @param interceptor the interceptor for customizing signing
     */
    public TsaSigner(SignerEntry signerEntry,
            byte[] requestData, RespInterceptor interceptor) {
        Objects.requireNonNull(signerEntry);
        Objects.requireNonNull(interceptor);

        this.signerEntry = signerEntry;
        this.requestData = requestData;
        this.interceptor = interceptor;
    }

    /**
     * Initialization.
     *
     * @param signerEntry a {@link SignerEntry} instance
     * @param requestData the time-stamping request
     * @param param the application parameters
     */
    public TsaSigner(SignerEntry signerEntry, byte[] requestData,
            TsaParam param) {
        this(signerEntry, requestData,
                new DefaultRespInterceptor<TsaParam>(param));
    }

    /**
     * Sign data.
     *
     * @returns the time-stamping response data
     */
    public byte[] sign() throws Exception {
        TsaParam requestParam = parseRequestParam();
        byte[] responseSeqData = createResponse(requestParam);
        return responseSeqData;
    }

     // Parse the parameters from the time-stamping request data.
    private TsaParam parseRequestParam() throws Exception {
        TsaParam param = TsaParam.newInstance();

        if (requestData == null) {
            return param;
        }

        System.out.println("===== Request ====");
        debug("Request", requestData);

        DerValue request = new DerValue(requestData);

        param.version(request.data.getInteger());
        print("reqVersion", param.version());

        DerValue messageImprintValue = request.data.getDerValue();
        debug("messageImprintValue", messageImprintValue.toByteArray());

        DerValue digestAlgoValue = messageImprintValue.data.getDerValue();
        debug("digestAlgoValue", digestAlgoValue.toByteArray());

        param.digestAlgo(AlgorithmId.parse(digestAlgoValue).getName());
        print("reqDigestAlgo", param.digestAlgo());

        param.hashedMessage(messageImprintValue.data.getOctetString());
        debug("reqHashedMessage", param.hashedMessage());

        while (request.data.available() > 0) {
            DerValue value = request.data.getDerValue();
            if (value.tag == DerValue.tag_Integer) {
                param.nonce(value.getBigInteger());
                print("reqNonce", param.nonce());
            } else if (value.tag == DerValue.tag_Boolean) {
                param.certReq(value.getBoolean());
                print("certReq", param.certReq());
            } else if (value.tag == DerValue.tag_ObjectId) {
                param.policyId(value.getOID().toString());
                print("reqPolicyId", param.policyId());
            }
        }

        return param;
    }

     // Create the time-stamping response data with the given the time-stamping
     // request parameters.
    private byte[] createResponse(TsaParam requestParam) throws Exception {
        System.out.println("===== Response ====");
        TsaParam respParam = interceptor.getRespParam(requestParam);

        DerOutputStream statusInfoOut = new DerOutputStream();
        int status = respParam.status();
        print("Status", status);
        statusInfoOut.putInteger(status);

        DerOutputStream responseOut = new DerOutputStream();
        responseOut.write(DerValue.tag_Sequence, statusInfoOut);
        debug("Status info", statusInfoOut.toByteArray());
        System.out.println("Generated status info");

        // Here, when the status is either 0 or 1, the response will contains
        // the signed data. Note that even though the signed data is not
        // generated, no failure info will be sent.
        if (status == 0 || status == 1) {
            System.out.println("Signer: "
                    + signerEntry.cert.getSubjectX500Principal().getName());
            String issuerName = signerEntry.cert.getIssuerX500Principal().getName();
            print("Issuer", issuerName);

            DerOutputStream tstInfoOut = new DerOutputStream();

            int version = respParam.version();
            print("version", version);
            tstInfoOut.putInteger(version);

            String policyId = respParam.policyId();
            print("policyId", policyId);
            tstInfoOut.putOID(ObjectIdentifier.of(policyId));

            String digestAlgo = respParam.digestAlgo();
            print("digestAlgo", digestAlgo);
            DerOutputStream digestAlgoOut = new DerOutputStream();
            AlgorithmId digestAlgoId = AlgorithmId.get(digestAlgo);
            digestAlgoId.encode(digestAlgoOut);
            byte[] hashedMessage = respParam.hashedMessage();
            debug("hashedMessage", hashedMessage);
            digestAlgoOut.putOctetString(hashedMessage);
            tstInfoOut.write(DerValue.tag_Sequence, digestAlgoOut);

            BigInteger serialNumber = respParam.serialNumber();
            print("serialNumber", serialNumber);
            tstInfoOut.putInteger(serialNumber);

            Date genTime = respParam.genTime();
            print("genTime", genTime);
            tstInfoOut.putGeneralizedTime(genTime);

            BigInteger nonce = respParam.nonce();
            if (nonce != null) {
                tstInfoOut.putInteger(nonce);
            }

            DerOutputStream tstInfoSeqOut = new DerOutputStream();
            tstInfoSeqOut.write(DerValue.tag_Sequence, tstInfoOut);
            byte[] tstInfoSeqData = tstInfoSeqOut.toByteArray();
            debug("TST Info", tstInfoSeqData);

            DerOutputStream eContentOut = new DerOutputStream();
            eContentOut.putOctetString(tstInfoSeqData);

            ContentInfo eContentInfo = new ContentInfo(
                    ObjectIdentifier.of(KnownOIDs.TimeStampTokenInfo),
                    new DerValue(eContentOut.toByteArray()));

            String defaultSigAlgo =  SignatureUtil.getDefaultSigAlgForKey(
                    signerEntry.privateKey);
            String sigAlgo = interceptor.getSigAlgo(defaultSigAlgo);
            Signature signature = Signature.getInstance(sigAlgo);
            System.out.println(
                    "Signature algorithm: " + signature.getAlgorithm());
            signature.initSign(signerEntry.privateKey);
            signature.update(tstInfoSeqData);

            SignerInfo signerInfo = new SignerInfo(
                    new X500Name(issuerName),
                    signerEntry.cert.getSerialNumber(),
                    SignatureUtil.getDigestAlgInPkcs7SignerInfo(
                            signature, sigAlgo, signerEntry.privateKey, false),
                    AlgorithmId.get(sigAlgo),
                    signature.sign());

            X509Certificate[] signerCertChain = interceptor.getSignerCertChain(
                    signerEntry.certChain, requestParam.certReq());
            PKCS7 p7 = new PKCS7(new AlgorithmId[] { digestAlgoId },
                    eContentInfo, signerCertChain,
                    new SignerInfo[] { signerInfo });
            ByteArrayOutputStream signedDataOut = new ByteArrayOutputStream();
            p7.encodeSignedData(signedDataOut);
            byte[] signedData = signedDataOut.toByteArray();
            debug("Signed data", signedData);
            responseOut.putDerValue(new DerValue(signedData));
            System.out.println("Generated signed data");
        }

        DerOutputStream responseSeqOut = new DerOutputStream();
        responseSeqOut.write(DerValue.tag_Sequence, responseOut);
        byte[] responseSeqData = responseSeqOut.toByteArray();
        debug("Response", responseSeqData);
        System.out.println("Generated response");
        return responseSeqData;
    }

    private static void print(String name, Object value) {
        System.out.println(name + ": " + value);
    }

    private static void debug(String name, byte[] bytes) {
        if (DEBUG) {
            System.out.println(name + ":");
            HEX_PRINTER.format(bytes);
        }
    }
}
