/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * class CK_TOKEN_INFO provides information about a token.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_TOKEN_INFO {&nbsp;&nbsp;
 *   CK_UTF8CHAR label[32];&nbsp;&nbsp;
 *   CK_UTF8CHAR manufacturerID[32];&nbsp;&nbsp;
 *   CK_UTF8CHAR model[16];&nbsp;&nbsp;
 *   CK_CHAR serialNumber[16];&nbsp;&nbsp;
 *   CK_FLAGS flags;&nbsp;&nbsp;
 *   CK_ULONG ulMaxSessionCount;&nbsp;&nbsp;
 *   CK_ULONG ulSessionCount;&nbsp;&nbsp;
 *   CK_ULONG ulMaxRwSessionCount;&nbsp;&nbsp;
 *   CK_ULONG ulRwSessionCount;&nbsp;&nbsp;
 *   CK_ULONG ulMaxPinLen;&nbsp;&nbsp;
 *   CK_ULONG ulMinPinLen;&nbsp;&nbsp;
 *   CK_ULONG ulTotalPublicMemory;&nbsp;&nbsp;
 *   CK_ULONG ulFreePublicMemory;&nbsp;&nbsp;
 *   CK_ULONG ulTotalPrivateMemory;&nbsp;&nbsp;
 *   CK_ULONG ulFreePrivateMemory;&nbsp;&nbsp;
 *   CK_VERSION hardwareVersion;&nbsp;&nbsp;
 *   CK_VERSION firmwareVersion;&nbsp;&nbsp;
 *   CK_CHAR utcTime[16];&nbsp;&nbsp;
 * } CK_TOKEN_INFO;
 * &nbsp;&nbsp;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_TOKEN_INFO {

    /* label, manufacturerID, and model have been changed from
     * CK_CHAR to CK_UTF8CHAR for v2.11. */
    /**
     * must be blank padded and only the first 32 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_UTF8CHAR label[32];
     * </PRE>
     */
    public char[] label;           /* blank padded */

    /**
     * must be blank padded and only the first 32 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_UTF8CHAR manufacturerID[32];
     * </PRE>
     */
    public char[] manufacturerID;  /* blank padded */

    /**
     * must be blank padded and only the first 16 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_UTF8CHAR model[16];
     * </PRE>
     */
    public char[] model;           /* blank padded */

    /**
     * must be blank padded and only the first 16 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_CHAR serialNumber[16];
     * </PRE>
     */
    public char[] serialNumber;    /* blank padded */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_FLAGS flags;
     * </PRE>
     */
    public long flags;               /* see below */

    /* ulMaxSessionCount, ulSessionCount, ulMaxRwSessionCount,
     * ulRwSessionCount, ulMaxPinLen, and ulMinPinLen have all been
     * changed from CK_USHORT to CK_ULONG for v2.0 */
    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMaxSessionCount;
     * </PRE>
     */
    public long ulMaxSessionCount;     /* max open sessions */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulSessionCount;
     * </PRE>
     */
    public long ulSessionCount;        /* sess. now open */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMaxRwSessionCount;
     * </PRE>
     */
    public long ulMaxRwSessionCount;   /* max R/W sessions */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulRwSessionCount;
     * </PRE>
     */
    public long ulRwSessionCount;      /* R/W sess. now open */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMaxPinLen;
     * </PRE>
     */
    public long ulMaxPinLen;           /* in bytes */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMinPinLen;
     * </PRE>
     */
    public long ulMinPinLen;           /* in bytes */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulTotalPublicMemory;
     * </PRE>
     */
    public long ulTotalPublicMemory;   /* in bytes */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulFreePublicMemory;
     * </PRE>
     */
    public long ulFreePublicMemory;    /* in bytes */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulTotalPrivateMemory;
     * </PRE>
     */
    public long ulTotalPrivateMemory;  /* in bytes */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulFreePrivateMemory;
     * </PRE>
     */
    public long ulFreePrivateMemory;   /* in bytes */

    /* hardwareVersion, firmwareVersion, and time are new for
     * v2.0 */
    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VERSION hardwareVersion;
     * </PRE>
     */
    public CK_VERSION    hardwareVersion;       /* version of hardware */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VERSION firmwareVersion;
     * </PRE>
     */
    public CK_VERSION    firmwareVersion;       /* version of firmware */

    /**
     * only the first 16 chars will be used
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_CHAR utcTime[16];
     * </PRE>
     */
    public char[] utcTime;           /* time */

    public CK_TOKEN_INFO(char[] label, char[] vendor, char[] model,
                         char[] serialNo, long flags,
                         long sessionMax, long session,
                         long rwSessionMax, long rwSession,
                         long pinLenMax, long pinLenMin,
                         long totalPubMem, long freePubMem,
                         long totalPrivMem, long freePrivMem,
                         CK_VERSION hwVer, CK_VERSION fwVer, char[] utcTime) {
        this.label = label;
        this.manufacturerID = vendor;
        this.model = model;
        this.serialNumber = serialNo;
        this.flags = flags;
        this.ulMaxSessionCount = sessionMax;
        this.ulSessionCount = session;
        this.ulMaxRwSessionCount = rwSessionMax;
        this.ulRwSessionCount = rwSession;
        this.ulMaxPinLen = pinLenMax;
        this.ulMinPinLen = pinLenMin;
        this.ulTotalPublicMemory = totalPubMem;
        this.ulFreePublicMemory = freePubMem;
        this.ulTotalPrivateMemory = totalPrivMem;
        this.ulFreePrivateMemory = freePrivMem;
        this.hardwareVersion = hwVer;
        this.firmwareVersion = fwVer;
        this.utcTime = utcTime;
    }

    /**
     * Returns the string representation of CK_TOKEN_INFO.
     *
     * @return the string representation of CK_TOKEN_INFO
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("label: ");
        sb.append(new String(label));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("manufacturerID: ");
        sb.append(new String(manufacturerID));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("model: ");
        sb.append(new String(model));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("serialNumber: ");
        sb.append(new String(serialNumber));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("flags: ");
        sb.append(Functions.tokenInfoFlagsToString(flags));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulMaxSessionCount: ");
        sb.append((ulMaxSessionCount == PKCS11Constants.CK_EFFECTIVELY_INFINITE)
                  ? "CK_EFFECTIVELY_INFINITE"
                  : (ulMaxSessionCount == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                    ? "CK_UNAVAILABLE_INFORMATION"
                    : String.valueOf(ulMaxSessionCount));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulSessionCount: ");
        sb.append((ulSessionCount == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulSessionCount));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulMaxRwSessionCount: ");
        sb.append((ulMaxRwSessionCount == PKCS11Constants.CK_EFFECTIVELY_INFINITE)
                  ? "CK_EFFECTIVELY_INFINITE"
                  : (ulMaxRwSessionCount == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                    ? "CK_UNAVAILABLE_INFORMATION"
                    : String.valueOf(ulMaxRwSessionCount));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulRwSessionCount: ");
        sb.append((ulRwSessionCount == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulRwSessionCount));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulMaxPinLen: ");
        sb.append(String.valueOf(ulMaxPinLen));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulMinPinLen: ");
        sb.append(String.valueOf(ulMinPinLen));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulTotalPublicMemory: ");
        sb.append((ulTotalPublicMemory == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulTotalPublicMemory));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulFreePublicMemory: ");
        sb.append((ulFreePublicMemory == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulFreePublicMemory));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulTotalPrivateMemory: ");
        sb.append((ulTotalPrivateMemory == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulTotalPrivateMemory));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulFreePrivateMemory: ");
        sb.append((ulFreePrivateMemory == PKCS11Constants.CK_UNAVAILABLE_INFORMATION)
                  ? "CK_UNAVAILABLE_INFORMATION"
                  : String.valueOf(ulFreePrivateMemory));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("hardwareVersion: ");
        sb.append(hardwareVersion.toString());
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("firmwareVersion: ");
        sb.append(firmwareVersion.toString());
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("utcTime: ");
        sb.append(new String(utcTime));
        //buffer.append(Constants.NEWLINE);

        return sb.toString() ;
    }

}
