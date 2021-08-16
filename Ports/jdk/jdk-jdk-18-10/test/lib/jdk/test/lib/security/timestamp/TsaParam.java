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

import jdk.test.lib.Utils;

import java.math.BigInteger;
import java.util.Date;

/**
 * The application parameters. They are used for generating {@code TSA}
 * response.
 */
public class TsaParam {

    /********** Signing internals **********/

    // The signer certificate alias in the key store
    private String alias;

    // The signature algorithm used by time-stamping
    private String sigAlgo;

    /********** TSA fields **********/

    // The time-stamping token version
    private Integer version;

    // The time-stamping response status
    private Integer status;

    // The policy ID
    private String policyId;

    // The digest algorithm in messageImprint
    private String digestAlgo;

    // The hashed message in messageImprint
    private byte[] hashedMessage;

    // The serial number
    private BigInteger serialNumber;

    // The time-stamping token generation time
    private Date genTime;

    // The nonce
    private BigInteger nonce;

    // Indicate if request TSA server certificate
    private Boolean certReq;

    public static TsaParam newInstance() {
        return new TsaParam();
    }

    public String alias() {
        return alias;
    }

    public TsaParam alias(String alias) {
        this.alias = alias;
        return this;
    }

    public String sigAlgo() {
        return sigAlgo;
    }

    public TsaParam sigAlgo(String sigAlgo) {
        this.sigAlgo = sigAlgo;
        return this;
    }

    public Integer version() {
        return version;
    }

    public TsaParam version(Integer version) {
        this.version = version;
        return this;
    }

    public Integer status() {
        return status;
    }

    public TsaParam status(Integer status) {
        this.status = status;
        return this;
    }

    public String policyId() {
        return policyId;
    }

    public TsaParam policyId(String policyId) {
        this.policyId = policyId;
        return this;
    }

    public String digestAlgo() {
        return digestAlgo;
    }

    public TsaParam digestAlgo(String digestAlgo) {
        this.digestAlgo = digestAlgo;
        return this;
    }

    public Date genTime() {
        return genTime;
    }

    public TsaParam genTime(Date genTime) {
        this.genTime = genTime;
        return this;
    }

    public byte[] hashedMessage() {
        return hashedMessage;
    }

    public TsaParam hashedMessage(byte[] hashedMessage) {
        this.hashedMessage = hashedMessage;
        return this;
    }

    public TsaParam hashedMessage(String hashedMessageHex) {
        return hashedMessage(Utils.toByteArray(hashedMessageHex));
    }

    public BigInteger serialNumber() {
        return serialNumber;
    }

    public TsaParam serialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public BigInteger nonce() {
        return nonce;
    }

    public TsaParam nonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    public Boolean certReq() {
        return certReq;
    }

    public TsaParam certReq(Boolean certReq) {
        this.certReq = certReq;
        return this;
    }
}
