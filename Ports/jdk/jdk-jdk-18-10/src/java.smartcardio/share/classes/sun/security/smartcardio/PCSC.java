/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.smartcardio;

import java.security.AccessController;

/**
 * Access to native PC/SC functions and definition of PC/SC constants.
 * Initialization and platform specific PC/SC constants are handled in
 * the platform specific superclass.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class PCSC extends PlatformPCSC {

    private PCSC() {
        // no instantiation
    }

    static void checkAvailable() throws RuntimeException {
        if (initException != null) {
            throw new UnsupportedOperationException
                    ("PC/SC not available on this platform", initException);
        }
    }

    // returns SCARDCONTEXT (contextId)
    static native long SCardEstablishContext
            (int scope)
            throws PCSCException;

    static native String[] SCardListReaders
            (long contextId)
            throws PCSCException;

    // returns SCARDHANDLE (cardId)
    static native long SCardConnect
            (long contextId, String readerName, int shareMode, int preferredProtocols)
            throws PCSCException;

    static native byte[] SCardTransmit
            (long cardId, int protocol, byte[] buf, int ofs, int len)
            throws PCSCException;

    // returns the ATR of the card, updates status[] with reader state and protocol
    static native byte[] SCardStatus
            (long cardId, byte[] status)
            throws PCSCException;

    static native void SCardDisconnect
            (long cardId, int disposition)
            throws PCSCException;

    // returns dwEventState[] of the same size and order as readerNames[]
    static native int[] SCardGetStatusChange
            (long contextId, long timeout, int[] currentState, String[] readerNames)
            throws PCSCException;

    static native void SCardBeginTransaction
            (long cardId)
            throws PCSCException;

    static native void SCardEndTransaction
            (long cardId, int disposition)
            throws PCSCException;

    static native byte[] SCardControl
            (long cardId, int controlCode, byte[] sendBuffer)
            throws PCSCException;

    // PCSC success/error/failure/warning codes
    final static int SCARD_S_SUCCESS             = 0x00000000;
    final static int SCARD_E_CANCELLED           = 0x80100002;
    final static int SCARD_E_CANT_DISPOSE        = 0x8010000E;
    final static int SCARD_E_INSUFFICIENT_BUFFER = 0x80100008;
    final static int SCARD_E_INVALID_ATR         = 0x80100015;
    final static int SCARD_E_INVALID_HANDLE      = 0x80100003;
    final static int SCARD_E_INVALID_PARAMETER   = 0x80100004;
    final static int SCARD_E_INVALID_TARGET      = 0x80100005;
    final static int SCARD_E_INVALID_VALUE       = 0x80100011;
    final static int SCARD_E_NO_MEMORY           = 0x80100006;
    final static int SCARD_F_COMM_ERROR          = 0x80100013;
    final static int SCARD_F_INTERNAL_ERROR      = 0x80100001;
    final static int SCARD_F_UNKNOWN_ERROR       = 0x80100014;
    final static int SCARD_F_WAITED_TOO_LONG     = 0x80100007;
    final static int SCARD_E_UNKNOWN_READER      = 0x80100009;
    final static int SCARD_E_TIMEOUT             = 0x8010000A;
    final static int SCARD_E_SHARING_VIOLATION   = 0x8010000B;
    final static int SCARD_E_NO_SMARTCARD        = 0x8010000C;
    final static int SCARD_E_UNKNOWN_CARD        = 0x8010000D;
    final static int SCARD_E_PROTO_MISMATCH      = 0x8010000F;
    final static int SCARD_E_NOT_READY           = 0x80100010;
    final static int SCARD_E_SYSTEM_CANCELLED    = 0x80100012;
    final static int SCARD_E_NOT_TRANSACTED      = 0x80100016;
    final static int SCARD_E_READER_UNAVAILABLE  = 0x80100017;

    final static int SCARD_W_UNSUPPORTED_CARD    = 0x80100065;
    final static int SCARD_W_UNRESPONSIVE_CARD   = 0x80100066;
    final static int SCARD_W_UNPOWERED_CARD      = 0x80100067;
    final static int SCARD_W_RESET_CARD          = 0x80100068;
    final static int SCARD_W_REMOVED_CARD        = 0x80100069;
    final static int SCARD_W_INSERTED_CARD       = 0x8010006A;

    final static int SCARD_E_UNSUPPORTED_FEATURE = 0x8010001F;
    final static int SCARD_E_PCI_TOO_SMALL       = 0x80100019;
    final static int SCARD_E_READER_UNSUPPORTED  = 0x8010001A;
    final static int SCARD_E_DUPLICATE_READER    = 0x8010001B;
    final static int SCARD_E_CARD_UNSUPPORTED    = 0x8010001C;
    final static int SCARD_E_NO_SERVICE          = 0x8010001D;
    final static int SCARD_E_SERVICE_STOPPED     = 0x8010001E;

    // MS undocumented
    final static int SCARD_E_NO_READERS_AVAILABLE = 0x8010002E;
    // std. Windows invalid handle return code, used instead of SCARD code
    final static int WINDOWS_ERROR_INVALID_HANDLE = 6;
    final static int WINDOWS_ERROR_INVALID_PARAMETER = 87;

    //
    final static int SCARD_SCOPE_USER      =  0x0000;
    final static int SCARD_SCOPE_TERMINAL  =  0x0001;
    final static int SCARD_SCOPE_SYSTEM    =  0x0002;
    final static int SCARD_SCOPE_GLOBAL    =  0x0003;

    final static int SCARD_SHARE_EXCLUSIVE =  0x0001;
    final static int SCARD_SHARE_SHARED    =  0x0002;
    final static int SCARD_SHARE_DIRECT    =  0x0003;

    final static int SCARD_LEAVE_CARD      =  0x0000;
    final static int SCARD_RESET_CARD      =  0x0001;
    final static int SCARD_UNPOWER_CARD    =  0x0002;
    final static int SCARD_EJECT_CARD      =  0x0003;

    final static int SCARD_STATE_UNAWARE     = 0x0000;
    final static int SCARD_STATE_IGNORE      = 0x0001;
    final static int SCARD_STATE_CHANGED     = 0x0002;
    final static int SCARD_STATE_UNKNOWN     = 0x0004;
    final static int SCARD_STATE_UNAVAILABLE = 0x0008;
    final static int SCARD_STATE_EMPTY       = 0x0010;
    final static int SCARD_STATE_PRESENT     = 0x0020;
    final static int SCARD_STATE_ATRMATCH    = 0x0040;
    final static int SCARD_STATE_EXCLUSIVE   = 0x0080;
    final static int SCARD_STATE_INUSE       = 0x0100;
    final static int SCARD_STATE_MUTE        = 0x0200;
    final static int SCARD_STATE_UNPOWERED   = 0x0400;

    final static int TIMEOUT_INFINITE = 0xffffffff;

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

}
