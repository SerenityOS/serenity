/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246077
 * @summary Make sure that digest spi and the resulting digest impl are
 * consistent in the impl of Cloneable interface
 * @run testng TestCloneable
 */
import java.security.*;
import java.util.Objects;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

public class TestCloneable {

    private static final Class<CloneNotSupportedException> CNSE =
            CloneNotSupportedException.class;

    @DataProvider
    public Object[][] testData() {
        return new Object[][] {
            { "MD2", "SUN" }, { "MD5", "SUN" }, { "SHA-1", "SUN" },
            { "SHA-224", "SUN" }, { "SHA-256", "SUN" },
            { "SHA-384", "SUN" }, { "SHA-512", "SUN" },
            { "SHA3-224", "SUN" }, { "SHA3-256", "SUN" },
            { "SHA3-384", "SUN" }, { "SHA3-512", "SUN" }
        };
    }

    @Test(dataProvider = "testData")
    public void test(String algo, String provName)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            CloneNotSupportedException {
        System.out.print("Testing " + algo + " impl from " + provName);
        Provider p = Security.getProvider(provName);
        Provider.Service s = p.getService("MessageDigest", algo);
        Objects.requireNonNull(s);
        MessageDigestSpi spi = (MessageDigestSpi) s.newInstance(null);
        MessageDigest md = MessageDigest.getInstance(algo, provName);
        if (spi instanceof Cloneable) {
            System.out.println(": Cloneable");
            Assert.assertTrue(md instanceof Cloneable);
            MessageDigest md2 = (MessageDigest) md.clone();
            Assert.assertEquals(md2.getAlgorithm(), algo);
            Assert.assertEquals(md2.getProvider().getName(), provName);
            Assert.assertTrue(md2 instanceof Cloneable);
        } else {
            System.out.println(": NOT Cloneable");
            Assert.assertThrows(CNSE, ()->md.clone());
        }
        System.out.println("Test Passed");
    }
}
