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

package sun.security.krb5.internal.ccache;

import sun.security.krb5.*;
import java.io.ByteArrayOutputStream;

/**
 * tag field introduced in KRB5_FCC_FVNO_4
 *
 * @author Yanni Zhang
 */
public class Tag{
    int length;
    int tag;
    int tagLen;
    Integer time_offset;
    Integer usec_offset;

    public Tag(int len, int new_tag, Integer new_time, Integer new_usec) {
        tag = new_tag;
        tagLen = 8;
        time_offset = new_time;
        usec_offset = new_usec;
        length =  4 + tagLen;
    }
    public Tag(int new_tag) {
        tag = new_tag;
        tagLen = 0;
        length = 4 + tagLen;
    }
    public byte[] toByteArray() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(length);
        os.write(tag);
        os.write(tagLen);
        if (time_offset != null) {
            os.write(time_offset.intValue());
        }
        if (usec_offset != null) {
            os.write(usec_offset.intValue());
        }
        return os.toByteArray();
    }
}
