/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @author Weijun Wang
 * @bug 4654195
 * @summary BIT STRING types with named bits must remove trailing 0 bits
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */

import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.DNSName;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.KeyUsageExtension;
import sun.security.x509.NetscapeCertTypeExtension;
import sun.security.x509.ReasonFlags;

public class NamedBitList {
    public static void main(String[] args) throws Exception {

        boolean[] bb = (new boolean[] {true, false, true, false, false, false});
        GeneralNames gns = new GeneralNames();
        gns.add(new GeneralName(new DNSName("dns")));
        DerOutputStream out;

        // length should be 5 since only {T,F,T} should be encoded
        KeyUsageExtension x1 = new KeyUsageExtension(bb);
        check(new DerValue(x1.getExtensionValue()).getUnalignedBitString().length(), 3);

        NetscapeCertTypeExtension x2 = new NetscapeCertTypeExtension(bb);
        check(new DerValue(x2.getExtensionValue()).getUnalignedBitString().length(), 3);

        ReasonFlags r = new ReasonFlags(bb);
        out = new DerOutputStream();
        r.encode(out);
        check(new DerValue(out.toByteArray()).getUnalignedBitString().length(), 3);

        // Read sun.security.x509.DistributionPoint for ASN.1 definition
        DistributionPoint dp = new DistributionPoint(gns, bb, gns);
        out = new DerOutputStream();
        dp.encode(out);
        DerValue v = new DerValue(out.toByteArray());
        // skip distributionPoint
        v.data.getDerValue();
        // read reasons
        DerValue v2 = v.data.getDerValue();
        // reset to BitString since it's context-specfic[1] encoded
        v2.resetTag(DerValue.tag_BitString);
        // length should be 5 since only {T,F,T} should be encoded
        check(v2.getUnalignedBitString().length(), 3);

        BitArray ba;
        ba = new BitArray(new boolean[] {false, false, false});
        check(ba.length(), 3);
        ba = ba.truncate();
        check(ba.length(), 1);

        ba = new BitArray(new boolean[] {
            true, true, true, true, true, true, true, true,
            false, false});
        check(ba.length(), 10);
        check(ba.toByteArray().length, 2);
        ba = ba.truncate();
        check(ba.length(), 8);
        check(ba.toByteArray().length, 1);

        ba = new BitArray(new boolean[] {
            true, true, true, true, true, true, true, true,
            true, false});
        check(ba.length(), 10);
        check(ba.toByteArray().length, 2);
        ba = ba.truncate();
        check(ba.length(), 9);
        check(ba.toByteArray().length, 2);
    }

    static void check(int la, int lb) throws Exception {
        if (la != lb) {
            System.err.println("Length is " + la + ", should be " + lb);
            throw new Exception("Encoding Error");
        } else {
            System.err.println("Correct, which is " + lb);
        }
    }
}
