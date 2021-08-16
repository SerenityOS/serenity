/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

/*
 * @test
 * @bug 8075286
 * @summary Test the SHAwithDSA signature algorithm OIDs in JDK.
 *          OID and algorithm transformation string should match.
 *          Both could be able to be used to generate the algorithm instance.
 * @compile ../../TestSignatureOidHelper.java
 * @run main TestSHAwithDSASignatureOids
 */
public class TestSHAwithDSASignatureOids {

    private static final List<OidAlgorithmPair> DATA = Arrays.asList(
            new OidAlgorithmPair("2.16.840.1.101.3.4.3.1", "SHA224withDSA"),
            new OidAlgorithmPair("2.16.840.1.101.3.4.3.2", "SHA256withDSA"));

    public static void main(String[] args) throws Exception {
        TestSignatureOidHelper helper = new TestSignatureOidHelper("DSA",
                "SUN", 1024, DATA);
        helper.execute();
    }
}
