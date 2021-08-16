/*
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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.crypto;

import sun.security.krb5.internal.*;
import sun.security.krb5.Checksum;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.KrbCryptoException;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;

public final class DesCbcMd5EType extends DesCbcEType {

    public DesCbcMd5EType() {
    }

    public int eType() {
        return EncryptedData.ETYPE_DES_CBC_MD5;
    }

    public int minimumPadSize() {
        return 0;
    }

    public int confounderSize() {
        return 8;
    }

    public int checksumType() {
        return Checksum.CKSUMTYPE_RSA_MD5;
    }

    public int checksumSize() {
        return 16;
    }

    /**
     * Calculates checksum using MD5.
     * @param data the input data.
     * @param size the length of data.
     * @return the checksum.
     *
     * @modified by Yanni Zhang, 12/06/99.
     */
    protected byte[] calculateChecksum(byte[] data, int size)
         throws KrbCryptoException {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new KrbCryptoException("JCE provider may not be installed. " + e.getMessage());
        }
        try {
            md5.update(data);
            return(md5.digest());
        } catch (Exception e) {
            throw new KrbCryptoException(e.getMessage());
        }
    }
}
