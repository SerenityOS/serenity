/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;
import sun.security.jgss.krb5.Krb5AcceptCredential;
import java.net.InetAddress;
import sun.security.util.*;
import java.io.IOException;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.security.krb5.internal.rcache.AuthTimeWithHash;

/**
 * This class encapsulates a KRB-AP-REQ that a client sends to a
 * server for authentication.
 */
public class KrbApReq {

    private byte[] obuf;
    private KerberosTime ctime;
    private int cusec;
    private Authenticator authenticator;
    private Credentials creds;
    private APReq apReqMessg;

    // Used by acceptor side
    private static ReplayCache rcache = ReplayCache.getInstance();
    private static boolean DEBUG = Krb5.DEBUG;
    private static final char[] hexConst = "0123456789ABCDEF".toCharArray();

    /**
     * Constructs an AP-REQ message to send to the peer.
     * @param tgsCred the <code>Credentials</code> to be used to construct the
     *          AP Request  protocol message.
     * @param mutualRequired Whether mutual authentication is required
     * @param useSubkey Whether the subkey is to be used to protect this
     *        specific application session. If this is not set then the
     *        session key from the ticket will be used.
     * @throws KrbException for any Kerberos protocol specific error
     * @throws IOException for any IO related errors
     *          (e.g. socket operations)
     */
     /*
     // Not Used
    public KrbApReq(Credentials tgsCred,
                    boolean mutualRequired,
                    boolean useSubKey,
                    boolean useSeqNumber) throws Asn1Exception,
                    KrbCryptoException, KrbException, IOException {

        this(tgsCred, mutualRequired, useSubKey, useSeqNumber, null);
    }
*/

    /**
     * Constructs an AP-REQ message to send to the peer.
     * @param tgsCred the <code>Credentials</code> to be used to construct the
     *          AP Request  protocol message.
     * @param mutualRequired Whether mutual authentication is required
     * @param useSubKey Whether the subkey is to be used to protect this
     *        specific application session. If this is not set then the
     *        session key from the ticket will be used.
     * @param cksum checksum of the application data that accompanies
     *        the KRB_AP_REQ.
     * @throws KrbException for any Kerberos protocol specific error
     * @throws IOException for any IO related errors
     *          (e.g. socket operations)
     */
     // Used in InitSecContextToken
    public KrbApReq(Credentials tgsCred,
                    boolean mutualRequired,
                    boolean useSubKey,
                    boolean useSeqNumber,
                    Checksum cksum) throws Asn1Exception,
                    KrbCryptoException, KrbException, IOException  {

        APOptions apOptions = (mutualRequired?
                               new APOptions(Krb5.AP_OPTS_MUTUAL_REQUIRED):
                               new APOptions());
        if (DEBUG)
            System.out.println(">>> KrbApReq: APOptions are " + apOptions);

        EncryptionKey subKey = (useSubKey?
                                new EncryptionKey(tgsCred.getSessionKey()):
                                null);

        SeqNumber seqNum = new LocalSeqNumber();

        init(apOptions,
             tgsCred,
             cksum,
             subKey,
             seqNum,
             null,   // AuthorizationData authzData
            KeyUsage.KU_AP_REQ_AUTHENTICATOR);

    }

    /**
     * Constructs an AP-REQ message from the bytes received from the
     * peer.
     * @param message The message received from the peer
     * @param cred <code>KrbAcceptCredential</code> containing keys to decrypt
     *    the message; key selected will depend on etype used to encrypt data
     * @throws KrbException for any Kerberos protocol specific error
     * @throws IOException for any IO related errors
     *          (e.g. socket operations)
     */
     // Used in InitSecContextToken (for AP_REQ and not TGS REQ)
    public KrbApReq(byte[] message,
                    Krb5AcceptCredential cred,
                    InetAddress initiator)
        throws KrbException, IOException {
        obuf = message;
        if (apReqMessg == null)
            decode();
        authenticate(cred, initiator);
    }

    /**
     * Constructs an AP-REQ message from the bytes received from the
     * peer.
     * @param value The <code>DerValue</code> that contains the
     *              DER enoded AP-REQ protocol message
     * @param keys <code>EncrtyptionKey</code>s to decrypt the message;
     *
     * @throws KrbException for any Kerberos protocol specific error
     * @throws IOException for any IO related errors
     *          (e.g. socket operations)
     */
     /*
    public KrbApReq(DerValue value, EncryptionKey[] key, InetAddress initiator)
        throws KrbException, IOException {
        obuf = value.toByteArray();
        if (apReqMessg == null)
            decode(value);
        authenticate(keys, initiator);
    }

    KrbApReq(APOptions options,
             Credentials tgs_creds,
             Checksum cksum,
             EncryptionKey subKey,
             SeqNumber seqNumber,
             AuthorizationData authorizationData)
        throws KrbException, IOException {
        init(options, tgs_creds, cksum, subKey, seqNumber, authorizationData);
    }
*/

     /** used by KrbTgsReq **/
    KrbApReq(APOptions apOptions,
             Ticket ticket,
             EncryptionKey key,
             PrincipalName cname,
             Checksum cksum,
             KerberosTime ctime,
             EncryptionKey subKey,
             SeqNumber seqNumber,
        AuthorizationData authorizationData)
        throws Asn1Exception, IOException,
               KdcErrException, KrbCryptoException {

        init(apOptions, ticket, key, cname,
             cksum, ctime, subKey, seqNumber, authorizationData,
            KeyUsage.KU_PA_TGS_REQ_AUTHENTICATOR);

    }

    private void init(APOptions options,
                      Credentials tgs_creds,
                      Checksum cksum,
                      EncryptionKey subKey,
                      SeqNumber seqNumber,
                      AuthorizationData authorizationData,
        int usage)
        throws KrbException, IOException {

        ctime = KerberosTime.now();
        init(options,
             tgs_creds.ticket,
             tgs_creds.key,
             tgs_creds.client,
             cksum,
             ctime,
             subKey,
             seqNumber,
             authorizationData,
            usage);
    }

    private void init(APOptions apOptions,
                      Ticket ticket,
                      EncryptionKey key,
                      PrincipalName cname,
                      Checksum cksum,
                      KerberosTime ctime,
                      EncryptionKey subKey,
                      SeqNumber seqNumber,
                      AuthorizationData authorizationData,
        int usage)
        throws Asn1Exception, IOException,
               KdcErrException, KrbCryptoException {

        createMessage(apOptions, ticket, key, cname,
                      cksum, ctime, subKey, seqNumber, authorizationData,
            usage);
        obuf = apReqMessg.asn1Encode();
    }


    void decode() throws KrbException, IOException {
        DerValue encoding = new DerValue(obuf);
        decode(encoding);
    }

    void decode(DerValue encoding) throws KrbException, IOException {
        apReqMessg = null;
        try {
            apReqMessg = new APReq(encoding);
        } catch (Asn1Exception e) {
            apReqMessg = null;
            KRBError err = new KRBError(encoding);
            String errStr = err.getErrorString();
            String eText;
            if (errStr.charAt(errStr.length() - 1) == 0)
                eText = errStr.substring(0, errStr.length() - 1);
            else
                eText = errStr;
            KrbException ke = new KrbException(err.getErrorCode(), eText);
            ke.initCause(e);
            throw ke;
        }
    }

    private void authenticate(Krb5AcceptCredential cred, InetAddress initiator)
        throws KrbException, IOException {
        int encPartKeyType = apReqMessg.ticket.encPart.getEType();
        Integer kvno = apReqMessg.ticket.encPart.getKeyVersionNumber();
        EncryptionKey[] keys = cred.getKrb5EncryptionKeys(apReqMessg.ticket.sname);
        EncryptionKey dkey = EncryptionKey.findKey(encPartKeyType, kvno, keys);

        if (dkey == null) {
            throw new KrbException(Krb5.API_INVALID_ARG,
                "Cannot find key of appropriate type to decrypt AP-REQ - " +
                                   EType.toString(encPartKeyType));
        }

        byte[] bytes = apReqMessg.ticket.encPart.decrypt(dkey,
            KeyUsage.KU_TICKET);
        byte[] temp = apReqMessg.ticket.encPart.reset(bytes);
        EncTicketPart enc_ticketPart = new EncTicketPart(temp);

        checkPermittedEType(enc_ticketPart.key.getEType());

        byte[] bytes2 = apReqMessg.authenticator.decrypt(enc_ticketPart.key,
            KeyUsage.KU_AP_REQ_AUTHENTICATOR);
        byte[] temp2 = apReqMessg.authenticator.reset(bytes2);
        authenticator = new Authenticator(temp2);
        ctime = authenticator.ctime;
        cusec = authenticator.cusec;
        authenticator.ctime =
                authenticator.ctime.withMicroSeconds(authenticator.cusec);

        if (!authenticator.cname.equals(enc_ticketPart.cname)) {
            throw new KrbApErrException(Krb5.KRB_AP_ERR_BADMATCH);
        }

        if (!authenticator.ctime.inClockSkew())
            throw new KrbApErrException(Krb5.KRB_AP_ERR_SKEW);

        String alg = AuthTimeWithHash.DEFAULT_HASH_ALG;
        byte[] hash;
        try {
            hash = MessageDigest.getInstance(AuthTimeWithHash.realAlg(alg))
                    .digest(apReqMessg.authenticator.cipher);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError("Impossible " + alg);
        }

        char[] h = new char[hash.length * 2];
        for (int i=0; i<hash.length; i++) {
            h[2*i] = hexConst[(hash[i]&0xff)>>4];
            h[2*i+1] = hexConst[hash[i]&0xf];
        }
        AuthTimeWithHash time = new AuthTimeWithHash(
                authenticator.cname.toString(),
                apReqMessg.ticket.sname.toString(),
                authenticator.ctime.getSeconds(),
                authenticator.cusec,
                alg,
                new String(h));
        rcache.checkAndStore(KerberosTime.now(), time);

        if (initiator != null) {
            // sender host address
            HostAddress sender = new HostAddress(initiator);
            if (enc_ticketPart.caddr != null
                    && !enc_ticketPart.caddr.inList(sender)) {
                if (DEBUG) {
                    System.out.println(">>> KrbApReq: initiator is "
                            + sender.getInetAddress()
                            + ", but caddr is "
                            + Arrays.toString(
                                enc_ticketPart.caddr.getInetAddresses()));
                }
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADADDR);
            }
        }

        // XXX check for repeated authenticator
        // if found
        //    throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);
        // else
        //    save authenticator to check for later

        KerberosTime now = KerberosTime.now();

        if ((enc_ticketPart.starttime != null &&
             enc_ticketPart.starttime.greaterThanWRTClockSkew(now)) ||
            enc_ticketPart.flags.get(Krb5.TKT_OPTS_INVALID))
            throw new KrbApErrException(Krb5.KRB_AP_ERR_TKT_NYV);

        // if the current time is later than end time by more
        // than the allowable clock skew, throws ticket expired exception.
        if (enc_ticketPart.endtime != null &&
            now.greaterThanWRTClockSkew(enc_ticketPart.endtime)) {
            throw new KrbApErrException(Krb5.KRB_AP_ERR_TKT_EXPIRED);
        }

        creds = new Credentials(
                                apReqMessg.ticket,
                                authenticator.cname,
                                null,
                                apReqMessg.ticket.sname,
                                null,
                                enc_ticketPart.key,
                                enc_ticketPart.flags,
                                enc_ticketPart.authtime,
                                enc_ticketPart.starttime,
                                enc_ticketPart.endtime,
                                enc_ticketPart.renewTill,
                                enc_ticketPart.caddr,
                                enc_ticketPart.authorizationData);
        if (DEBUG) {
            System.out.println(">>> KrbApReq: authenticate succeed.");
        }
    }

    /**
     * Returns the credentials that are contained in the ticket that
     * is part of this AP-REQ.
     */
    public Credentials getCreds() {
        return creds;
    }

    KerberosTime getCtime() {
        if (ctime != null)
            return ctime;
        return authenticator.ctime;
    }

    int cusec() {
        return cusec;
    }

    APOptions getAPOptions() throws KrbException, IOException {
        if (apReqMessg == null)
            decode();
        if (apReqMessg != null)
            return apReqMessg.apOptions;
        return null;
    }

    /**
     * Returns true if mutual authentication is required and hence an
     * AP-REP will need to be generated.
     * @throws KrbException
     * @throws IOException
     */
    public boolean getMutualAuthRequired() throws KrbException, IOException {
        if (apReqMessg == null)
            decode();
        if (apReqMessg != null)
            return apReqMessg.apOptions.get(Krb5.AP_OPTS_MUTUAL_REQUIRED);
        return false;
    }

    boolean useSessionKey() throws KrbException, IOException {
        if (apReqMessg == null)
            decode();
        if (apReqMessg != null)
            return apReqMessg.apOptions.get(Krb5.AP_OPTS_USE_SESSION_KEY);
        return false;
    }

    /**
     * Returns the optional subkey stored in the Authenticator for
     * this message. Returns null if none is stored.
     */
    public EncryptionKey getSubKey() {
        // XXX Can authenticator be null
        return authenticator.getSubKey();
    }

    /**
     * Returns the optional sequence number stored in the
     * Authenticator for this message. Returns null if none is
     * stored.
     */
    public Integer getSeqNumber() {
        // XXX Can authenticator be null
        return authenticator.getSeqNumber();
    }

    /**
     * Returns the optional Checksum stored in the
     * Authenticator for this message. Returns null if none is
     * stored.
     */
    public Checksum getChecksum() {
        return authenticator.getChecksum();
    }

    /**
     * Returns the ASN.1 encoding that should be sent to the peer.
     */
    public byte[] getMessage() {
        return obuf;
    }

    /**
     * Returns the principal name of the client that generated this
     * message.
     */
    public PrincipalName getClient() {
        return creds.getClient();
    }

    private void createMessage(APOptions apOptions,
                               Ticket ticket,
                               EncryptionKey key,
                               PrincipalName cname,
                               Checksum cksum,
                               KerberosTime ctime,
                               EncryptionKey subKey,
                               SeqNumber seqNumber,
                               AuthorizationData authorizationData,
        int usage)
        throws Asn1Exception, IOException,
               KdcErrException, KrbCryptoException {

        Integer seqno = null;

        if (seqNumber != null)
            seqno = seqNumber.current();

        authenticator =
            new Authenticator(cname,
                              cksum,
                              ctime.getMicroSeconds(),
                              ctime,
                              subKey,
                              seqno,
                              authorizationData);

        byte[] temp = authenticator.asn1Encode();

        EncryptedData encAuthenticator =
            new EncryptedData(key, temp, usage);

        apReqMessg =
            new APReq(apOptions, ticket, encAuthenticator);
    }

     // Check that key is one of the permitted types
     private static void checkPermittedEType(int target) throws KrbException {
        int[] etypes = EType.getDefaults("permitted_enctypes");
        if (!EType.isSupported(target, etypes)) {
            throw new KrbException(EType.toString(target) +
                " encryption type not in permitted_enctypes list");
        }
     }
}
