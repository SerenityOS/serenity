/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8159488
 * @summary Basic tests for the various getInstance() methods of
 * XMLSignatureFactory, TransformService, and KeyInfoFactory classes
 * @run main GetInstanceTests
 */
import java.security.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;


public class GetInstanceTests {

    public static void main(String[] argv) throws Exception {
        TestTransformService(CanonicalizationMethod.INCLUSIVE, "DOM");
        TestTransformService(CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS, "DOM");
        TestTransformService(Transform.BASE64, "DOM");
        TestTransformService(Transform.XPATH2, "DOM");
        TestXMLSignatureFactory();
        TestKeyInfoFactory();
    }

    private static void TestTransformService(String algo,
        String mechType) throws Exception {
        TransformService ts = TransformService.getInstance(algo, mechType);
        Provider p = ts.getProvider();
        try {
            ts = TransformService.getInstance(algo, mechType, p);
            ts = TransformService.getInstance(algo, mechType, p.getName());
        } catch (Exception ex) {
            throw new RuntimeException("Error: Unexpected exception", ex);
        }
    }

    private static void TestXMLSignatureFactory() throws Exception {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance();
        Provider p = fac.getProvider();
        String mechType = fac.getMechanismType();
        Provider p2;
        try {
            fac = XMLSignatureFactory.getInstance(mechType);
            p2 = fac.getProvider();
            fac = XMLSignatureFactory.getInstance(mechType, p);
            fac = XMLSignatureFactory.getInstance(mechType, p.getName());
        } catch (Exception ex) {
            throw new RuntimeException("Error: Unexpected exception", ex);
        }
        if (p2.getName() != p.getName()) {
            throw new RuntimeException("Error: Provider equality check failed");
        }
        if (p2.getName() != p.getName()) {
            throw new RuntimeException("Error: Provider equality check failed");
        }
    }

    private static void TestKeyInfoFactory() throws Exception {
        KeyInfoFactory fac = KeyInfoFactory.getInstance();
        Provider p = fac.getProvider();
        String mechType = fac.getMechanismType();
        Provider p2;
        try {
            fac = KeyInfoFactory.getInstance(mechType);
            p2 = fac.getProvider();
            fac = KeyInfoFactory.getInstance(mechType, p);
            fac = KeyInfoFactory.getInstance(mechType, p.getName());
        } catch (Exception ex) {
            throw new RuntimeException("Error: Unexpected exception", ex);
        }
        if (p2.getName() != p.getName()) {
            throw new RuntimeException("Error: Provider equality check failed");
        }
    }
}
