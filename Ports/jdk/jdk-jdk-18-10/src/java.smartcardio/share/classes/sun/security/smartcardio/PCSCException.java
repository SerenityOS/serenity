/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import static sun.security.smartcardio.PCSC.*;

/**
 * Exception for PC/SC errors. The native code portion checks the return value
 * of the SCard* functions. If it indicates an error, the native code constructs
 * an instance of this exception, throws it, and returns to Java.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class PCSCException extends Exception {

    private static final long serialVersionUID = 4181137171979130432L;

    final int code;

    PCSCException(int code) {
        super(toErrorString(code));
        this.code = code;
    }

    private static String toErrorString(int code) {
        switch (code) {
        case SCARD_S_SUCCESS             : return "SCARD_S_SUCCESS";
        case SCARD_E_CANCELLED           : return "SCARD_E_CANCELLED";
        case SCARD_E_CANT_DISPOSE        : return "SCARD_E_CANT_DISPOSE";
        case SCARD_E_INSUFFICIENT_BUFFER : return "SCARD_E_INSUFFICIENT_BUFFER";
        case SCARD_E_INVALID_ATR         : return "SCARD_E_INVALID_ATR";
        case SCARD_E_INVALID_HANDLE      : return "SCARD_E_INVALID_HANDLE";
        case SCARD_E_INVALID_PARAMETER   : return "SCARD_E_INVALID_PARAMETER";
        case SCARD_E_INVALID_TARGET      : return "SCARD_E_INVALID_TARGET";
        case SCARD_E_INVALID_VALUE       : return "SCARD_E_INVALID_VALUE";
        case SCARD_E_NO_MEMORY           : return "SCARD_E_NO_MEMORY";
        case SCARD_F_COMM_ERROR          : return "SCARD_F_COMM_ERROR";
        case SCARD_F_INTERNAL_ERROR      : return "SCARD_F_INTERNAL_ERROR";
        case SCARD_F_UNKNOWN_ERROR       : return "SCARD_F_UNKNOWN_ERROR";
        case SCARD_F_WAITED_TOO_LONG     : return "SCARD_F_WAITED_TOO_LONG";
        case SCARD_E_UNKNOWN_READER      : return "SCARD_E_UNKNOWN_READER";
        case SCARD_E_TIMEOUT             : return "SCARD_E_TIMEOUT";
        case SCARD_E_SHARING_VIOLATION   : return "SCARD_E_SHARING_VIOLATION";
        case SCARD_E_NO_SMARTCARD        : return "SCARD_E_NO_SMARTCARD";
        case SCARD_E_UNKNOWN_CARD        : return "SCARD_E_UNKNOWN_CARD";
        case SCARD_E_PROTO_MISMATCH      : return "SCARD_E_PROTO_MISMATCH";
        case SCARD_E_NOT_READY           : return "SCARD_E_NOT_READY";
        case SCARD_E_SYSTEM_CANCELLED    : return "SCARD_E_SYSTEM_CANCELLED";
        case SCARD_E_NOT_TRANSACTED      : return "SCARD_E_NOT_TRANSACTED";
        case SCARD_E_READER_UNAVAILABLE  : return "SCARD_E_READER_UNAVAILABLE";

        case SCARD_W_UNSUPPORTED_CARD    : return "SCARD_W_UNSUPPORTED_CARD";
        case SCARD_W_UNRESPONSIVE_CARD   : return "SCARD_W_UNRESPONSIVE_CARD";
        case SCARD_W_UNPOWERED_CARD      : return "SCARD_W_UNPOWERED_CARD";
        case SCARD_W_RESET_CARD          : return "SCARD_W_RESET_CARD";
        case SCARD_W_REMOVED_CARD        : return "SCARD_W_REMOVED_CARD";
        case SCARD_W_INSERTED_CARD       : return "SCARD_W_INSERTED_CARD";

        case SCARD_E_UNSUPPORTED_FEATURE : return "SCARD_E_UNSUPPORTED_FEATURE";
        case SCARD_E_PCI_TOO_SMALL       : return "SCARD_E_PCI_TOO_SMALL";
        case SCARD_E_READER_UNSUPPORTED  : return "SCARD_E_READER_UNSUPPORTED";
        case SCARD_E_DUPLICATE_READER    : return "SCARD_E_DUPLICATE_READER";
        case SCARD_E_CARD_UNSUPPORTED    : return "SCARD_E_CARD_UNSUPPORTED";
        case SCARD_E_NO_SERVICE          : return "SCARD_E_NO_SERVICE";
        case SCARD_E_SERVICE_STOPPED     : return "SCARD_E_SERVICE_STOPPED";

        case SCARD_E_NO_READERS_AVAILABLE: return "SCARD_E_NO_READERS_AVAILABLE";
        case WINDOWS_ERROR_INVALID_HANDLE: return "WINDOWS_ERROR_INVALID_HANDLE";
        case WINDOWS_ERROR_INVALID_PARAMETER: return "WINDOWS_ERROR_INVALID_PARAMETER";

        default: return "Unknown error 0x" + Integer.toHexString(code);
        }
    }
}
