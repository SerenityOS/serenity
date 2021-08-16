/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import java.security.ProviderException;

/**
 * class CK_MECHANISM_INFO provides information about a particular mechanism.
 * <p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_MECHANISM_INFO {&nbsp;&nbsp;
 *   CK_ULONG ulMinKeySize;&nbsp;&nbsp;
 *   CK_ULONG ulMaxKeySize;&nbsp;&nbsp;
 *   CK_FLAGS flags;&nbsp;&nbsp;
 * } CK_MECHANISM_INFO;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_MECHANISM_INFO {

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMinKeySize;
     * </PRE>
     */
    public long ulMinKeySize;

    // the integer version of ulMinKeySize for doing the actual range
    // check in SunPKCS11 provider, defaults to 0
    public final int iMinKeySize;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_ULONG ulMaxKeySize;
     * </PRE>
     */
    public long ulMaxKeySize;

    // the integer version of ulMaxKeySize for doing the actual range
    // check in SunPKCS11 provider, defaults to Integer.MAX_VALUE
    public final int iMaxKeySize;

    /**
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_FLAGS flags;
     * </PRE>
     */
    public long flags;

    public CK_MECHANISM_INFO(long minKeySize, long maxKeySize,
                             long flags) {
        this.ulMinKeySize = minKeySize;
        this.ulMaxKeySize = maxKeySize;
        this.iMinKeySize = ((minKeySize < Integer.MAX_VALUE && minKeySize > 0)?
                (int)minKeySize : 0);
        this.iMaxKeySize = ((maxKeySize < Integer.MAX_VALUE && maxKeySize > 0)?
                (int)maxKeySize : Integer.MAX_VALUE);
        this.flags = flags;
    }

    /**
     * Returns the string representation of CK_MECHANISM_INFO.
     *
     * @return the string representation of CK_MECHANISM_INFO
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("ulMinKeySize: ");
        sb.append(String.valueOf(ulMinKeySize));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("ulMaxKeySize: ");
        sb.append(String.valueOf(ulMaxKeySize));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("flags: ");
        sb.append(String.valueOf(flags));
        sb.append(" = ");
        sb.append(Functions.mechanismInfoFlagsToString(flags));
        //buffer.append(Constants.NEWLINE);

        return sb.toString() ;
    }
}
