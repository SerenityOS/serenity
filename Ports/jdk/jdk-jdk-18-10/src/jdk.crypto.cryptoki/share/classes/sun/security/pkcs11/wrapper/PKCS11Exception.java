/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 */

/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

package sun.security.pkcs11.wrapper;

import java.util.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * This is the superclass of all checked exceptions used by this package. An
 * exception of this class indicates that a function call to the underlying
 * PKCS#11 module returned a value not equal to CKR_OK. The application can get
 * the returned value by calling getErrorCode(). A return value not equal to
 * CKR_OK is the only reason for such an exception to be thrown.
 * PKCS#11 defines the meaning of an error-code, which may depend on the
 * context in which the error occurs.
 *
 * @author <a href="mailto:Karl.Scheibelhofer@iaik.at"> Karl Scheibelhofer </a>
 * @invariants
 */
public class PKCS11Exception extends Exception {
    private static final long serialVersionUID = 4077027363729192L;

    /**
     * The code of the error which was the reason for this exception.
     */
    protected long errorCode;

    protected String errorMsg;

    public static final long CKR_GENERAL_ERROR = RV.CKR_GENERAL_ERROR.value;
    public static final long CKR_ATTRIBUTE_TYPE_INVALID =
            RV.CKR_ATTRIBUTE_TYPE_INVALID.value;
    public static final long CKR_DATA_LEN_RANGE = RV.CKR_DATA_LEN_RANGE.value;
    public static final long CKR_ENCRYPTED_DATA_INVALID =
            RV.CKR_ENCRYPTED_DATA_INVALID.value;
    public static final long CKR_ENCRYPTED_DATA_LEN_RANGE =
            RV.CKR_ENCRYPTED_DATA_LEN_RANGE.value;
    public static final long CKR_MECHANISM_INVALID =
            RV.CKR_MECHANISM_INVALID.value;
    public static final long CKR_MECHANISM_PARAM_INVALID =
            RV.CKR_MECHANISM_PARAM_INVALID.value;
    public static final long CKR_OPERATION_NOT_INITIALIZED =
            RV.CKR_OPERATION_NOT_INITIALIZED.value;
    public static final long CKR_PIN_INCORRECT =
            RV.CKR_PIN_INCORRECT.value;
    public static final long CKR_SIGNATURE_INVALID =
            RV.CKR_SIGNATURE_INVALID.value;
    public static final long CKR_SIGNATURE_LEN_RANGE =
            RV.CKR_SIGNATURE_LEN_RANGE.value;
    public static final long CKR_USER_ALREADY_LOGGED_IN =
            RV.CKR_USER_ALREADY_LOGGED_IN.value;
    public static final long CKR_USER_NOT_LOGGED_IN =
            RV.CKR_USER_NOT_LOGGED_IN.value;
    public static final long CKR_BUFFER_TOO_SMALL =
            RV.CKR_BUFFER_TOO_SMALL.value;
    public static final long CKR_CRYPTOKI_ALREADY_INITIALIZED =
            RV.CKR_CRYPTOKI_ALREADY_INITIALIZED.value;

    // enum for all PKCS#11 return value
    static enum RV {
        CKR_OK(0x00000000L),
        CKR_CANCEL(0x00000001L),
        CKR_HOST_MEMORY(0x00000002L),
        CKR_SLOT_ID_INVALID(0x00000003L),
        CKR_GENERAL_ERROR(0x00000005L),
        CKR_FUNCTION_FAILED(0x00000006L),
        CKR_ARGUMENTS_BAD(0x00000007L),
        CKR_NO_EVENT(0x00000008L),
        CKR_NEED_TO_CREATE_THREADS(0x00000009L),
        CKR_CANT_LOCK(0x0000000AL),
        CKR_ATTRIBUTE_READ_ONLY(0x00000010L),
        CKR_ATTRIBUTE_SENSITIVE(0x00000011L),
        CKR_ATTRIBUTE_TYPE_INVALID(0x00000012L),
        CKR_ATTRIBUTE_VALUE_INVALID(0x00000013L),
        CKR_ACTION_PROHIBITED(0x0000001BL),
        CKR_DATA_INVALID(0x00000020L),
        CKR_DATA_LEN_RANGE(0x00000021L),
        CKR_DEVICE_ERROR(0x00000030L),
        CKR_DEVICE_MEMORY(0x00000031L),
        CKR_DEVICE_REMOVED(0x00000032L),
        CKR_ENCRYPTED_DATA_INVALID(0x00000040L),
        CKR_ENCRYPTED_DATA_LEN_RANGE(0x00000041L),
        CKR_AEAD_DECRYPT_FAILED(0x00000042L),
        CKR_FUNCTION_CANCELED(0x00000050L),
        CKR_FUNCTION_NOT_PARALLEL(0x00000051L),
        CKR_FUNCTION_NOT_SUPPORTED(0x00000054L),
        CKR_KEY_HANDLE_INVALID(0x00000060L),
        CKR_KEY_SIZE_RANGE(0x00000062L),
        CKR_KEY_TYPE_INCONSISTENT(0x00000063L),
        CKR_KEY_NOT_NEEDED(0x00000064L),
        CKR_KEY_CHANGED(0x00000065L),
        CKR_KEY_NEEDED(0x00000066L),
        CKR_KEY_INDIGESTIBLE(0x00000067L),
        CKR_KEY_FUNCTION_NOT_PERMITTED(0x00000068L),
        CKR_KEY_NOT_WRAPPABLE(0x00000069L),
        CKR_KEY_UNEXTRACTABLE(0x0000006AL),
        CKR_MECHANISM_INVALID(0x00000070L),
        CKR_MECHANISM_PARAM_INVALID(0x00000071L),
        CKR_OBJECT_HANDLE_INVALID(0x00000082L),
        CKR_OPERATION_ACTIVE(0x00000090L),
        CKR_OPERATION_NOT_INITIALIZED(0x00000091L),
        CKR_PIN_INCORRECT(0x000000A0L),
        CKR_PIN_INVALID(0x000000A1L),
        CKR_PIN_LEN_RANGE(0x000000A2L),
        CKR_PIN_EXPIRED(0x000000A3L),
        CKR_PIN_LOCKED(0x000000A4L),
        CKR_SESSION_CLOSED(0x000000B0L),
        CKR_SESSION_COUNT(0x000000B1L),
        CKR_SESSION_HANDLE_INVALID(0x000000B3L),
        CKR_SESSION_PARALLEL_NOT_SUPPORTED(0x000000B4L),
        CKR_SESSION_READ_ONLY(0x000000B5L),
        CKR_SESSION_EXISTS(0x000000B6L),
        CKR_SESSION_READ_ONLY_EXISTS(0x000000B7L),
        CKR_SESSION_READ_WRITE_SO_EXISTS(0x000000B8L),
        CKR_SIGNATURE_INVALID(0x000000C0L),
        CKR_SIGNATURE_LEN_RANGE(0x000000C1L),
        CKR_TEMPLATE_INCOMPLETE(0x000000D0L),
        CKR_TEMPLATE_INCONSISTENT(0x000000D1L),
        CKR_TOKEN_NOT_PRESENT(0x000000E0L),
        CKR_TOKEN_NOT_RECOGNIZED(0x000000E1L),
        CKR_TOKEN_WRITE_PROTECTED(0x000000E2L),
        CKR_UNWRAPPING_KEY_HANDLE_INVALID(0x000000F0L),
        CKR_UNWRAPPING_KEY_SIZE_RANGE(0x000000F1L),
        CKR_UNWRAPPING_KEY_TYPE_INCONSISTENT(0x000000F2L),
        CKR_USER_ALREADY_LOGGED_IN(0x00000100L),
        CKR_USER_NOT_LOGGED_IN(0x00000101L),
        CKR_USER_PIN_NOT_INITIALIZED(0x00000102L),
        CKR_USER_TYPE_INVALID(0x00000103L),
        CKR_USER_ANOTHER_ALREADY_LOGGED_IN(0x00000104L),
        CKR_USER_TOO_MANY_TYPES(0x00000105L),
        CKR_WRAPPED_KEY_INVALID(0x00000110L),
        CKR_WRAPPED_KEY_LEN_RANGE(0x00000112L),
        CKR_WRAPPING_KEY_HANDLE_INVALID(0x00000113L),
        CKR_WRAPPING_KEY_SIZE_RANGE(0x00000114L),
        CKR_WRAPPING_KEY_TYPE_INCONSISTENT(0x00000115L),
        CKR_RANDOM_SEED_NOT_SUPPORTED(0x00000120L),
        CKR_RANDOM_NO_RNG(0x00000121L),
        CKR_DOMAIN_PARAMS_INVALID(0x00000130L),
        CKR_CURVE_NOT_SUPPORTED(0x00000140L),
        CKR_BUFFER_TOO_SMALL(0x00000150L),
        CKR_SAVED_STATE_INVALID(0x00000160L),
        CKR_INFORMATION_SENSITIVE(0x00000170L),
        CKR_STATE_UNSAVEABLE(0x00000180L),
        CKR_CRYPTOKI_NOT_INITIALIZED(0x00000190L),
        CKR_CRYPTOKI_ALREADY_INITIALIZED(0x00000191L),
        CKR_MUTEX_BAD(0x000001A0L),
        CKR_MUTEX_NOT_LOCKED(0x000001A1L),
        CKR_NEW_PIN_MODE(0x000001B0L),
        CKR_NEXT_OTP(0x000001B1L),
        CKR_EXCEEDED_MAX_ITERATIONS(0x000001B5L),
        CKR_FIPS_SELF_TEST_FAILED(0x000001B6L),
        CKR_LIBRARY_LOAD_FAILED(0x000001B7L),
        CKR_PIN_TOO_WEAK(0x000001B8L),
        CKR_PUBLIC_KEY_INVALID(0x000001B9L),
        CKR_FUNCTION_REJECTED(0x00000200L),
        CKR_TOKEN_RESOURCE_EXCEEDED(0x00000201L),
        CKR_OPERATION_CANCEL_FAILED(0x00000202L),
        CKR_VENDOR_DEFINED(0x80000000L);

        private final long value;

        RV(long value) {
            this.value = value;
        }
    };

    private static String lookup(long errorCode) {
        for (RV r : RV.values()) {
            if (r.value == errorCode) {
                return r.name();
            }
        }
        // for unknown PKCS11 return values, just use hex as its string
        return "0x" + Functions.toFullHexString((int)errorCode);
    }

    /**
     * Constructor taking the error code (the CKR_* constants in PKCS#11) and
     * extra info for error message.
     */
    public PKCS11Exception(long errorCode, String extraInfo) {
        this.errorCode = errorCode;
        this.errorMsg = lookup(errorCode);
        if (extraInfo != null) {
            this.errorMsg += extraInfo;
        }
    }

    /**
     * This method gets the corresponding text error message from
     * a property file. If this file is not available, it returns the error
     * code as a hex-string.
     *
     * @return The message or the error code; e.g. "CKR_DEVICE_ERROR" or
     *         "0x00000030".
     * @preconditions
     * @postconditions (result <> null)
     */
    public String getMessage() {
        return errorMsg;
    }

    /**
     * Returns the PKCS#11 error code.
     *
     * @return The error code; e.g. 0x00000030.
     * @preconditions
     * @postconditions
     */
    public long getErrorCode() {
        return errorCode;
    }
}
