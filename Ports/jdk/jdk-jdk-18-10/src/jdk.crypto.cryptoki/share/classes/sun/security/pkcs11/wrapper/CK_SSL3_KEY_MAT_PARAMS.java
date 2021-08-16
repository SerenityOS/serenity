/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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



/**
 * class CK_SSL3_KEY_MAT_PARAMS provides the parameters to the
 * CKM_SSL3_KEY_AND_MAC_DERIVE mechanism.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_SSL3_KEY_MAT_PARAMS {
 *   CK_ULONG ulMacSizeInBits;
 *   CK_ULONG ulKeySizeInBits;
 *   CK_ULONG ulIVSizeInBits;
 *   CK_BBOOL bIsExport;
 *   CK_SSL3_RANDOM_DATA RandomInfo;
 *   CK_SSL3_KEY_MAT_OUT_PTR pReturnedKeyMaterial;
 * } CK_SSL3_KEY_MAT_PARAMS;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_SSL3_KEY_MAT_PARAMS{

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMacSizeInBits;
     * </PRE>
     */
    public long ulMacSizeInBits;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulKeySizeInBits;
     * </PRE>
     */
    public long ulKeySizeInBits;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulIVSizeInBits;
     * </PRE>
     */
    public long ulIVSizeInBits;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_BBOOL bIsExport;
     * </PRE>
     */
    public boolean bIsExport;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_SSL3_RANDOM_DATA RandomInfo;
     * </PRE>
     */
    public CK_SSL3_RANDOM_DATA RandomInfo;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_SSL3_KEY_MAT_OUT_PTR pReturnedKeyMaterial;
     * </PRE>
     */
    public CK_SSL3_KEY_MAT_OUT pReturnedKeyMaterial;

    public CK_SSL3_KEY_MAT_PARAMS(int macSize, int keySize, int ivSize, boolean export, CK_SSL3_RANDOM_DATA random) {
        ulMacSizeInBits = macSize;
        ulKeySizeInBits = keySize;
        ulIVSizeInBits = ivSize;
        bIsExport = export;
        RandomInfo = random;
        pReturnedKeyMaterial = new CK_SSL3_KEY_MAT_OUT();
        if (ivSize != 0) {
            int n = ivSize >> 3;
            pReturnedKeyMaterial.pIVClient = new byte[n];
            pReturnedKeyMaterial.pIVServer = new byte[n];
        }
    }

    /**
     * Returns the string representation of CK_SSL3_KEY_MAT_PARAMS.
     *
     * @return the string representation of CK_SSL3_KEY_MAT_PARAMS
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(Constants.INDENT);
        buffer.append("ulMacSizeInBits: ");
        buffer.append(ulMacSizeInBits);
        buffer.append(Constants.NEWLINE);

        buffer.append(Constants.INDENT);
        buffer.append("ulKeySizeInBits: ");
        buffer.append(ulKeySizeInBits);
        buffer.append(Constants.NEWLINE);

        buffer.append(Constants.INDENT);
        buffer.append("ulIVSizeInBits: ");
        buffer.append(ulIVSizeInBits);
        buffer.append(Constants.NEWLINE);

        buffer.append(Constants.INDENT);
        buffer.append("bIsExport: ");
        buffer.append(bIsExport);
        buffer.append(Constants.NEWLINE);

        buffer.append(Constants.INDENT);
        buffer.append("RandomInfo: ");
        buffer.append(RandomInfo);
        buffer.append(Constants.NEWLINE);

        buffer.append(Constants.INDENT);
        buffer.append("pReturnedKeyMaterial: ");
        buffer.append(pReturnedKeyMaterial);
        //buffer.append(Constants.NEWLINE);

        return buffer.toString();
    }

}
