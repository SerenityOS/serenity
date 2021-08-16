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

/*
 * @test
 * @bug 8136410
 * @summary AlgorithmDecomposer is not parsing padding correctly
 * @modules java.base/sun.security.util
 */

import sun.security.util.AlgorithmDecomposer;
import java.util.Set;

public class DecomposeAlgorithms {

    public static void main(String[] args) throws Exception {
        AlgorithmDecomposer decomposer = new AlgorithmDecomposer();

        check(decomposer, "AES/CBC/NoPadding", new String[] {
                "AES", "CBC", "NoPadding"});
        check(decomposer, "DES/CBC/PKCS5Padding", new String[] {
                "DES", "CBC", "PKCS5Padding"});
        check(decomposer, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", new String[] {
                "RSA", "ECB", "OAEP", "SHA1", "SHA-1", "MGF1Padding"});
        check(decomposer, "OAEPWithSHA-512AndMGF1Padding", new String[] {
                "OAEP", "SHA512", "SHA-512", "MGF1Padding"});
        check(decomposer, "OAEPWithSHA-512AndMGF1Padding", new String[] {
                "OAEP", "SHA512", "SHA-512", "MGF1Padding"});
        check(decomposer, "PBEWithSHA1AndRC2_40", new String[] {
                "PBE", "SHA1", "SHA-1", "RC2_40"});
        check(decomposer, "PBEWithHmacSHA224AndAES_128", new String[] {
                "PBE", "HmacSHA224", "AES_128"});
    }

    private static void check(AlgorithmDecomposer parser,
            String fullAlgName, String[] components) throws Exception {

        Set<String> parsed = parser.decompose(fullAlgName);
        if (parsed.size() != components.length) {
            throw new Exception("Not expected components number: " + parsed);
        }

        for (String component : components) {
            if (!parsed.contains(component)) {
                throw new Exception("Not a expected component: " + component);
            }
        }

        System.out.println("OK: " + fullAlgName);
    }
}
