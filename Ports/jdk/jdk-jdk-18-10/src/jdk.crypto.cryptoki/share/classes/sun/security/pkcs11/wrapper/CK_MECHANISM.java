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

import java.math.BigInteger;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * class CK_MECHANISM specifies a particular mechanism and any parameters it
 * requires.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 *  typedef struct CK_MECHANISM {&nbsp;&nbsp;
 *    CK_MECHANISM_TYPE mechanism;&nbsp;&nbsp;
 *    CK_VOID_PTR pParameter;&nbsp;&nbsp;
 *    CK_ULONG ulParameterLen;&nbsp;&nbsp;
 *  } CK_MECHANISM;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_MECHANISM {

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_MECHANISM_TYPE mechanism;
     * </PRE>
     */
    public long mechanism;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VOID_PTR pParameter;
     *   CK_ULONG ulParameterLen;
     * </PRE>
     */
    public Object pParameter = null;

    // pointer to native CK_MECHANISM structure
    // For mechanisms which have only mechanism id, the native structure
    // can be freed right after init and this field will not be used. However,
    // for mechanisms which have both mechanism id and parameters, it can
    // only be freed after operation is finished. Thus, the native pointer
    // will be stored here and then be explicitly freed by caller.
    private long pHandle = 0L;

    public CK_MECHANISM(long mechanism) {
        this.mechanism = mechanism;
    }

    // We don't have a (long,Object) constructor to force type checking.
    // This makes sure we don't accidentally pass a class that the native
    // code cannot handle.
    public CK_MECHANISM(long mechanism, byte[] pParameter) {
        init(mechanism, pParameter);
    }

    public CK_MECHANISM(long mechanism, BigInteger b) {
        init(mechanism, sun.security.pkcs11.P11Util.getMagnitude(b));
    }

    public CK_MECHANISM(long mechanism, CK_VERSION version) {
        init(mechanism, version);
    }

    public CK_MECHANISM(long mechanism, CK_SSL3_MASTER_KEY_DERIVE_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_TLS12_MASTER_KEY_DERIVE_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_SSL3_KEY_MAT_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_TLS12_KEY_MAT_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_TLS_PRF_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_TLS_MAC_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_ECDH1_DERIVE_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, Long params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_AES_CTR_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_GCM_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism, CK_CCM_PARAMS params) {
        init(mechanism, params);
    }

    public CK_MECHANISM(long mechanism,
            CK_SALSA20_CHACHA20_POLY1305_PARAMS params) {
        init(mechanism, params);
    }

    // For PSS. the parameter may be set multiple times, use the
    // CK_MECHANISM(long) constructor and setParameter(CK_RSA_PKCS_PSS_PARAMS)
    // methods instead of creating yet another constructor
    public void setParameter(CK_RSA_PKCS_PSS_PARAMS params) {
        assert(this.mechanism == CKM_RSA_PKCS_PSS);
        assert(params != null);
        if (this.pParameter != null && this.pParameter.equals(params)) {
            return;
        }
        freeHandle();
        this.pParameter = params;
    }

    public void freeHandle() {
        if (this.pHandle != 0L) {
            this.pHandle = PKCS11.freeMechanism(pHandle);
        }
    }

    private void init(long mechanism, Object pParameter) {
        this.mechanism = mechanism;
        this.pParameter = pParameter;
    }

    /**
     * Returns the string representation of CK_MECHANISM.
     *
     * @return the string representation of CK_MECHANISM
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("mechanism: ");
        sb.append(mechanism);
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("pParameter: ");
        sb.append(pParameter.toString());
        sb.append(Constants.NEWLINE);

        /*
        sb.append(Constants.INDENT);
        sb.append("ulParameterLen: ??");
        sb.append(Constants.NEWLINE);
        */
        if (pHandle != 0L) {
            sb.append(Constants.INDENT);
            sb.append("pHandle: ");
            sb.append(pHandle);
            sb.append(Constants.NEWLINE);
        }
        return sb.toString() ;
    }
}
