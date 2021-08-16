/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import org.ietf.jgss.*;
import javax.security.auth.kerberos.DelegationPermission;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import sun.security.krb5.*;
import sun.security.krb5.internal.Krb5;
import sun.security.jgss.krb5.internal.TlsChannelBindingImpl;

abstract class InitialToken extends Krb5Token {

    private static final int CHECKSUM_TYPE = 0x8003;

    private static final int CHECKSUM_LENGTH_SIZE     = 4;
    private static final int CHECKSUM_BINDINGS_SIZE   = 16;
    private static final int CHECKSUM_FLAGS_SIZE      = 4;
    private static final int CHECKSUM_DELEG_OPT_SIZE  = 2;
    private static final int CHECKSUM_DELEG_LGTH_SIZE = 2;

    private static final int CHECKSUM_DELEG_FLAG    = 1;
    private static final int CHECKSUM_MUTUAL_FLAG   = 2;
    private static final int CHECKSUM_REPLAY_FLAG   = 4;
    private static final int CHECKSUM_SEQUENCE_FLAG = 8;
    private static final int CHECKSUM_CONF_FLAG     = 16;
    private static final int CHECKSUM_INTEG_FLAG    = 32;

    private final byte[] CHECKSUM_FIRST_BYTES =
    {(byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00};

    private static final int CHANNEL_BINDING_AF_UNSPEC = 0;
    private static final int CHANNEL_BINDING_AF_INET = 2;
    private static final int CHANNEL_BINDING_AF_INET6 = 24;
    private static final int CHANNEL_BINDING_AF_NULL_ADDR = 255;

    private static final int Inet4_ADDRSZ = 4;
    private static final int Inet6_ADDRSZ = 16;

    protected class OverloadedChecksum {

        private byte[] checksumBytes = null;
        private Credentials delegCreds = null;
        private int flags = 0;

        /**
         * Called on the initiator side when creating the
         * InitSecContextToken.
         */
        public OverloadedChecksum(Krb5Context context,
                                  Credentials tgt,
                                  Credentials serviceTicket)
            throws KrbException, IOException, GSSException {

            byte[] krbCredMessage = null;
            int pos = 0;
            int size = CHECKSUM_LENGTH_SIZE + CHECKSUM_BINDINGS_SIZE +
                CHECKSUM_FLAGS_SIZE;

            if (!tgt.isForwardable()) {
                context.setCredDelegState(false);
                context.setDelegPolicyState(false);
            } else if (context.getCredDelegState()) {
                if (context.getDelegPolicyState()) {
                    if (!serviceTicket.checkDelegate()) {
                        // delegation not permitted by server policy, mark it
                        context.setDelegPolicyState(false);
                    }
                }
            } else if (context.getDelegPolicyState()) {
                if (serviceTicket.checkDelegate()) {
                    context.setCredDelegState(true);
                } else {
                    context.setDelegPolicyState(false);
                }
            }

            if (context.getCredDelegState()) {
                KrbCred krbCred = null;
                CipherHelper cipherHelper =
                    context.getCipherHelper(serviceTicket.getSessionKey());
                if (useNullKey(cipherHelper)) {
                    krbCred = new KrbCred(tgt, serviceTicket,
                                              EncryptionKey.NULL_KEY);
                } else {
                    krbCred = new KrbCred(tgt, serviceTicket,
                                    serviceTicket.getSessionKey());
                }
                krbCredMessage = krbCred.getMessage();
                size += CHECKSUM_DELEG_OPT_SIZE +
                        CHECKSUM_DELEG_LGTH_SIZE +
                        krbCredMessage.length;
            }

            checksumBytes = new byte[size];

            checksumBytes[pos++] = CHECKSUM_FIRST_BYTES[0];
            checksumBytes[pos++] = CHECKSUM_FIRST_BYTES[1];
            checksumBytes[pos++] = CHECKSUM_FIRST_BYTES[2];
            checksumBytes[pos++] = CHECKSUM_FIRST_BYTES[3];

            ChannelBinding localBindings = context.getChannelBinding();
            if (localBindings != null) {
                byte[] localBindingsBytes =
                    computeChannelBinding(context.getChannelBinding());
                System.arraycopy(localBindingsBytes, 0,
                             checksumBytes, pos, localBindingsBytes.length);
                //              System.out.println("ChannelBinding hash: "
                //         + getHexBytes(localBindingsBytes));
            }

            pos += CHECKSUM_BINDINGS_SIZE;

            if (context.getCredDelegState())
                flags |= CHECKSUM_DELEG_FLAG;
            if (context.getMutualAuthState())
                flags |= CHECKSUM_MUTUAL_FLAG;
            if (context.getReplayDetState())
                flags |= CHECKSUM_REPLAY_FLAG;
            if (context.getSequenceDetState())
                flags |= CHECKSUM_SEQUENCE_FLAG;
            if (context.getIntegState())
                flags |= CHECKSUM_INTEG_FLAG;
            if (context.getConfState())
                flags |= CHECKSUM_CONF_FLAG;

            byte[] temp = new byte[4];
            writeLittleEndian(flags, temp);
            checksumBytes[pos++] = temp[0];
            checksumBytes[pos++] = temp[1];
            checksumBytes[pos++] = temp[2];
            checksumBytes[pos++] = temp[3];

            if (context.getCredDelegState()) {

                PrincipalName delegateTo =
                    serviceTicket.getServer();
                // Cannot use '\"' instead of "\"" in constructor because
                // it is interpreted as suggested length!
                StringBuilder sb = new StringBuilder("\"");
                sb.append(delegateTo.getName()).append('\"');
                String realm = delegateTo.getRealmAsString();
                sb.append(" \"krbtgt/").append(realm).append('@');
                sb.append(realm).append('\"');
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    DelegationPermission perm =
                        new DelegationPermission(sb.toString());
                    sm.checkPermission(perm);
                }


                /*
                 * Write 1 in little endian but in two bytes
                 * for DlgOpt
                 */

                checksumBytes[pos++] = (byte)0x01;
                checksumBytes[pos++] = (byte)0x00;

                /*
                 * Write the length of the delegated credential in little
                 * endian but in two bytes for Dlgth
                 */

                if (krbCredMessage.length > 0x0000ffff)
                    throw new GSSException(GSSException.FAILURE, -1,
                        "Incorrect message length");

                writeLittleEndian(krbCredMessage.length, temp);
                checksumBytes[pos++] = temp[0];
                checksumBytes[pos++] = temp[1];
                System.arraycopy(krbCredMessage, 0,
                                 checksumBytes, pos, krbCredMessage.length);
            }

        }

        /**
         * Called on the acceptor side when reading an InitSecContextToken.
         */
        // XXX Passing in Checksum is not required. byte[] can
        // be passed in if this checksum type denotes a
        // raw_checksum. In that case, make Checksum class krb5
        // internal.
        public OverloadedChecksum(Krb5Context context, Checksum checksum,
                                  EncryptionKey key, EncryptionKey subKey)
            throws GSSException, KrbException, IOException {

            int pos = 0;

            if (checksum == null) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                        "No cksum in AP_REQ's authenticator");
                ge.initCause(new KrbException(Krb5.KRB_AP_ERR_INAPP_CKSUM));
                throw ge;
            }
            checksumBytes = checksum.getBytes();

            if ((checksumBytes[0] != CHECKSUM_FIRST_BYTES[0]) ||
                (checksumBytes[1] != CHECKSUM_FIRST_BYTES[1]) ||
                (checksumBytes[2] != CHECKSUM_FIRST_BYTES[2]) ||
                (checksumBytes[3] != CHECKSUM_FIRST_BYTES[3])) {
                throw new GSSException(GSSException.FAILURE, -1,
                        "Incorrect checksum");
            }

            ChannelBinding localBindings = context.getChannelBinding();

            // Ignore remote channel binding info when not requested at
            // local side (RFC 4121 4.1.1.2: the acceptor MAY ignore...).
            //
            // All major krb5 implementors implement this "MAY",
            // and some applications depend on it as a workaround
            // for not having a way to negotiate the use of channel
            // binding -- the initiator application always uses CB
            // and hopes the acceptor will ignore the CB if the
            // acceptor doesn't support CB.
            if (localBindings != null) {
                byte[] remoteBindingBytes = new byte[CHECKSUM_BINDINGS_SIZE];
                System.arraycopy(checksumBytes, 4, remoteBindingBytes, 0,
                                 CHECKSUM_BINDINGS_SIZE);

                byte[] noBindings = new byte[CHECKSUM_BINDINGS_SIZE];
                if (!Arrays.equals(noBindings, remoteBindingBytes)) {
                    byte[] localBindingsBytes =
                        computeChannelBinding(localBindings);
                    if (!Arrays.equals(localBindingsBytes,
                                                remoteBindingBytes)) {
                        throw new GSSException(GSSException.BAD_BINDINGS, -1,
                                               "Bytes mismatch!");
                    }
                } else {
                    throw new GSSException(GSSException.BAD_BINDINGS, -1,
                                           "Token missing ChannelBinding!");
                }
            }

            flags = readLittleEndian(checksumBytes, 20, 4);

            if ((flags & CHECKSUM_DELEG_FLAG) > 0) {

                /*
                 * XXX
                 * if ((checksumBytes[24] != (byte)0x01) &&
                 * (checksumBytes[25] != (byte)0x00))
                 */

                int credLen = readLittleEndian(checksumBytes, 26, 2);
                byte[] credBytes = new byte[credLen];
                System.arraycopy(checksumBytes, 28, credBytes, 0, credLen);

                KrbCred cred;
                try {
                    cred = new KrbCred(credBytes, key);
                } catch (KrbException ke) {
                    if (subKey != null) {
                        cred = new KrbCred(credBytes, subKey);
                    } else {
                        throw ke;
                    }
                }
                delegCreds = cred.getDelegatedCreds()[0];
            }
        }

        // check if KRB-CRED message should use NULL_KEY for encryption
        private boolean useNullKey(CipherHelper ch) {
            boolean flag = true;
            // for "newer" etypes and RC4-HMAC do not use NULL KEY
            if ((ch.getProto() == 1) || ch.isArcFour()) {
                flag = false;
            }
            return flag;
        }

        public Checksum getChecksum() throws KrbException {
            return new Checksum(checksumBytes, CHECKSUM_TYPE);
        }

        public Credentials getDelegatedCreds() {
            return delegCreds;
        }

        // Only called by acceptor
        public void setContextFlags(Krb5Context context) {
                // default for cred delegation is false
            if ((flags & CHECKSUM_DELEG_FLAG) > 0)
                context.setCredDelegState(true);
                // default for the following are true
            if ((flags & CHECKSUM_MUTUAL_FLAG) == 0) {
                context.setMutualAuthState(false);
            }
            if ((flags & CHECKSUM_REPLAY_FLAG) == 0) {
                context.setReplayDetState(false);
            }
            if ((flags & CHECKSUM_SEQUENCE_FLAG) == 0) {
                context.setSequenceDetState(false);
            }
            if ((flags & CHECKSUM_CONF_FLAG) == 0) {
                context.setConfState(false);
            }
            if ((flags & CHECKSUM_INTEG_FLAG) == 0) {
                context.setIntegState(false);
            }
        }
    }

    private int getAddrType(InetAddress addr, int defValue) {
        int addressType = defValue;

        if (addr instanceof Inet4Address)
            addressType = CHANNEL_BINDING_AF_INET;
        else if (addr instanceof Inet6Address)
            addressType = CHANNEL_BINDING_AF_INET6;
        return (addressType);
    }

    private byte[] getAddrBytes(InetAddress addr) throws GSSException {
        int addressType = getAddrType(addr, CHANNEL_BINDING_AF_NULL_ADDR);
        byte[] addressBytes = addr.getAddress();
        if (addressBytes != null) {
            switch (addressType) {
                case CHANNEL_BINDING_AF_INET:
                    if (addressBytes.length != Inet4_ADDRSZ) {
                        throw new GSSException(GSSException.FAILURE, -1,
                        "Incorrect AF-INET address length in ChannelBinding.");
                    }
                    return (addressBytes);
                case CHANNEL_BINDING_AF_INET6:
                    if (addressBytes.length != Inet6_ADDRSZ) {
                        throw new GSSException(GSSException.FAILURE, -1,
                        "Incorrect AF-INET6 address length in ChannelBinding.");
                    }
                    return (addressBytes);
                default:
                    throw new GSSException(GSSException.FAILURE, -1,
                    "Cannot handle non AF-INET addresses in ChannelBinding.");
            }
        }
        return null;
    }

    private byte[] computeChannelBinding(ChannelBinding channelBinding)
        throws GSSException {

        InetAddress initiatorAddress = channelBinding.getInitiatorAddress();
        InetAddress acceptorAddress = channelBinding.getAcceptorAddress();
        int size = 5*4;

        // LDAP TLS Channel Binding requires CHANNEL_BINDING_AF_UNSPEC address type
        // for unspecified initiator and acceptor addresses.
        // CHANNEL_BINDING_AF_NULL_ADDR value should be used for unspecified address
        // in all other cases.
        int initiatorAddressType = getAddrType(initiatorAddress,
                (channelBinding instanceof TlsChannelBindingImpl) ?
                        CHANNEL_BINDING_AF_UNSPEC : CHANNEL_BINDING_AF_NULL_ADDR);
        int acceptorAddressType = getAddrType(acceptorAddress,
                (channelBinding instanceof TlsChannelBindingImpl) ?
                        CHANNEL_BINDING_AF_UNSPEC : CHANNEL_BINDING_AF_NULL_ADDR);

        byte[] initiatorAddressBytes = null;
        if (initiatorAddress != null) {
            initiatorAddressBytes = getAddrBytes(initiatorAddress);
            size += initiatorAddressBytes.length;
        }

        byte[] acceptorAddressBytes = null;
        if (acceptorAddress != null) {
            acceptorAddressBytes = getAddrBytes(acceptorAddress);
            size += acceptorAddressBytes.length;
        }

        byte[] appDataBytes = channelBinding.getApplicationData();
        if (appDataBytes != null) {
            size += appDataBytes.length;
        }

        byte[] data = new byte[size];

        int pos = 0;

        writeLittleEndian(initiatorAddressType, data, pos);
        pos += 4;

        if (initiatorAddressBytes != null) {
            writeLittleEndian(initiatorAddressBytes.length, data, pos);
            pos += 4;
            System.arraycopy(initiatorAddressBytes, 0,
                             data, pos, initiatorAddressBytes.length);
            pos += initiatorAddressBytes.length;
        } else {
            // Write length 0
            pos += 4;
        }

        writeLittleEndian(acceptorAddressType, data, pos);
        pos += 4;

        if (acceptorAddressBytes != null) {
            writeLittleEndian(acceptorAddressBytes.length, data, pos);
            pos += 4;
            System.arraycopy(acceptorAddressBytes, 0,
                             data, pos, acceptorAddressBytes.length);
            pos += acceptorAddressBytes.length;
        } else {
            // Write length 0
            pos += 4;
        }

        if (appDataBytes != null) {
            writeLittleEndian(appDataBytes.length, data, pos);
            pos += 4;
            System.arraycopy(appDataBytes, 0, data, pos,
                             appDataBytes.length);
            pos += appDataBytes.length;
        } else {
            // Write 0
            pos += 4;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(data);
        } catch (NoSuchAlgorithmException e) {
                throw new GSSException(GSSException.FAILURE, -1,
                                       "Could not get MD5 Message Digest - "
                                       + e.getMessage());
        }
    }

    public abstract byte[] encode() throws IOException;

}
