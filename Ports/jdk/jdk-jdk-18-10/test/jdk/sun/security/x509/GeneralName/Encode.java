/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import sun.security.x509.*;
import sun.security.util.*;

/**
 * @test
 * @author Sean Mullan
 * @bug 4716972
 * @summary Check that GeneralName.encode() encodes an X500Name with
 *      an explicit tag
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */
public class Encode {

    public static void main(String[] args) throws Exception {

        GeneralName gn = new GeneralName(new X500Name("cn=john"));
        DerOutputStream dos = new DerOutputStream();
        gn.encode(dos);
        DerValue dv = new DerValue(dos.toByteArray());
        short tag = (byte)(dv.tag & 0x1f);
        if (tag != GeneralNameInterface.NAME_DIRECTORY) {
            throw new Exception("Invalid tag for Directory name");
        }
        if (!dv.isContextSpecific() || !dv.isConstructed()) {
            throw new Exception("Invalid encoding of Directory name");
        }
        DerInputStream data = dv.getData();
        DerValue[] seq = data.getSequence(5);
    }
}
