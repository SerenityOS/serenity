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
 * class CK_VERSION describes the version of a Cryptoki interface, a Cryptoki
 * library, or an SSL implementation, or the hardware or firmware version of a
 * slot or token.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_VERSION {&nbsp;&nbsp;
 *   CK_BYTE major;&nbsp;&nbsp;
 *   CK_BYTE minor;&nbsp;&nbsp;
 * } CK_VERSION;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_VERSION {

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_BYTE major;
     * </PRE>
     */
    public byte major;  /* integer portion of version number */

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_BYTE minor;
     * </PRE>
     */
    public byte minor;  /* 1/100ths portion of version number */

    public CK_VERSION(int major, int minor) {
        this.major = (byte)major;
        this.minor = (byte)minor;
    }

    /**
     * Returns the string representation of CK_VERSION.
     *
     * @return the string representation of CK_VERSION
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(major & 0xff);
        buffer.append('.');
        int m = minor & 0xff;
        if (m < 10) {
            buffer.append('0');
        }
        buffer.append(m);

        return buffer.toString();
    }

}
