/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;

import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * class CK_ATTRIBUTE includes the type, value and length of an attribute.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_ATTRIBUTE {&nbsp;&nbsp;
 *   CK_ATTRIBUTE_TYPE type;&nbsp;&nbsp;
 *   CK_VOID_PTR pValue;&nbsp;&nbsp;
 *   CK_ULONG ulValueLen;
 * } CK_ATTRIBUTE;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_ATTRIBUTE {

    // common attributes
    // NOTE that CK_ATTRIBUTE is a mutable classes but these attributes
    // *MUST NEVER* be modified, e.g. by using them in a
    // C_GetAttributeValue() call!

    public static final CK_ATTRIBUTE TOKEN_FALSE =
                                    new CK_ATTRIBUTE(CKA_TOKEN, false);

    public static final CK_ATTRIBUTE SENSITIVE_FALSE =
                                    new CK_ATTRIBUTE(CKA_SENSITIVE, false);

    public static final CK_ATTRIBUTE EXTRACTABLE_TRUE =
                                    new CK_ATTRIBUTE(CKA_EXTRACTABLE, true);

    public static final CK_ATTRIBUTE ENCRYPT_TRUE =
                                    new CK_ATTRIBUTE(CKA_ENCRYPT, true);

    public static final CK_ATTRIBUTE DECRYPT_TRUE =
                                    new CK_ATTRIBUTE(CKA_DECRYPT, true);

    public static final CK_ATTRIBUTE WRAP_TRUE =
                                    new CK_ATTRIBUTE(CKA_WRAP, true);

    public static final CK_ATTRIBUTE UNWRAP_TRUE =
                                    new CK_ATTRIBUTE(CKA_UNWRAP, true);

    public static final CK_ATTRIBUTE SIGN_TRUE =
                                    new CK_ATTRIBUTE(CKA_SIGN, true);

    public static final CK_ATTRIBUTE VERIFY_TRUE =
                                    new CK_ATTRIBUTE(CKA_VERIFY, true);

    public static final CK_ATTRIBUTE SIGN_RECOVER_TRUE =
                                    new CK_ATTRIBUTE(CKA_SIGN_RECOVER, true);

    public static final CK_ATTRIBUTE VERIFY_RECOVER_TRUE =
                                    new CK_ATTRIBUTE(CKA_VERIFY_RECOVER, true);

    public static final CK_ATTRIBUTE DERIVE_TRUE =
                                    new CK_ATTRIBUTE(CKA_DERIVE, true);

    public static final CK_ATTRIBUTE ENCRYPT_NULL =
                                    new CK_ATTRIBUTE(CKA_ENCRYPT);

    public static final CK_ATTRIBUTE DECRYPT_NULL =
                                    new CK_ATTRIBUTE(CKA_DECRYPT);

    public static final CK_ATTRIBUTE WRAP_NULL =
                                    new CK_ATTRIBUTE(CKA_WRAP);

    public static final CK_ATTRIBUTE UNWRAP_NULL =
                                    new CK_ATTRIBUTE(CKA_UNWRAP);

    public CK_ATTRIBUTE() {
        // empty
    }

    public CK_ATTRIBUTE(long type) {
        this.type = type;
    }

    public CK_ATTRIBUTE(long type, Object pValue) {
        this.type = type;
        this.pValue = pValue;
    }

    public CK_ATTRIBUTE(long type, boolean value) {
        this.type = type;
        this.pValue = Boolean.valueOf(value);
    }

    public CK_ATTRIBUTE(long type, long value) {
        this.type = type;
        this.pValue = Long.valueOf(value);
    }

    public CK_ATTRIBUTE(long type, BigInteger value) {
        this.type = type;
        this.pValue = sun.security.pkcs11.P11Util.getMagnitude(value);
    }

    public BigInteger getBigInteger() {
        if (pValue instanceof byte[] == false) {
            throw new RuntimeException("Not a byte[]");
        }
        return new BigInteger(1, (byte[])pValue);
    }

    public boolean getBoolean() {
        if (pValue instanceof Boolean == false) {
            throw new RuntimeException
                ("Not a Boolean: " + pValue.getClass().getName());
        }
        return ((Boolean)pValue).booleanValue();
    }

    public char[] getCharArray() {
        if (pValue instanceof char[] == false) {
            throw new RuntimeException("Not a char[]");
        }
        return (char[])pValue;
    }

    public byte[] getByteArray() {
        if (pValue instanceof byte[] == false) {
            throw new RuntimeException("Not a byte[]");
        }
        return (byte[])pValue;
    }

    public long getLong() {
        if (pValue instanceof Long == false) {
            throw new RuntimeException
                ("Not a Long: " + pValue.getClass().getName());
        }
        return ((Long)pValue).longValue();
    }

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ATTRIBUTE_TYPE type;
     * </PRE>
     */
    public long type;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VOID_PTR pValue;
     *   CK_ULONG ulValueLen;
     * </PRE>
     */
    public Object pValue;

    /**
     * Returns the string representation of CK_ATTRIBUTE.
     *
     * @return the string representation of CK_ATTRIBUTE
     */
    public String toString() {
        String prefix = Functions.getAttributeName(type) + " = ";
        if (type == CKA_CLASS) {
            return prefix + Functions.getObjectClassName(getLong());
        } else if (type == CKA_KEY_TYPE) {
            return prefix + Functions.getKeyName(getLong());
        } else {
            String s;
            if (pValue instanceof char[]) {
                s = new String((char[])pValue);
            } else if (pValue instanceof byte[]) {
                s = Functions.toHexString((byte[])pValue);
            } else {
                s = String.valueOf(pValue);
            }
            return prefix + s;
        }
    }

}
