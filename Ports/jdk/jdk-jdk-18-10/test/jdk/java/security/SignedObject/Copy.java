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

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignedObject;

/*
 * @test
 * @bug 8050374
 * @summary Checks if a signed object is a copy of an original object
 */
public class Copy {

    private static final String DSA = "DSA";
    private static final int KEY_SIZE = 512;
    private static final int MAGIC = 123;

    public static void main(String args[]) throws Exception {
        KeyPairGenerator kg = KeyPairGenerator.getInstance(DSA);
        kg.initialize(KEY_SIZE);
        KeyPair kp = kg.genKeyPair();

        Signature signature = Signature.getInstance(DSA);
        Test original = new Test();
        SignedObject so = new SignedObject(original, kp.getPrivate(),
                signature);
        System.out.println("Signature algorithm: " + so.getAlgorithm());

        signature = Signature.getInstance(DSA, "SUN");
        if (!so.verify(kp.getPublic(), signature)) {
            throw new RuntimeException("Verification failed");
        }

        kg = KeyPairGenerator.getInstance(DSA);
        kg.initialize(KEY_SIZE);
        kp = kg.genKeyPair();

        if (so.verify(kp.getPublic(), signature)) {
            throw new RuntimeException("Unexpected success");
        }

        Object copy = so.getObject();
        if (!original.equals(copy)) {
            throw new RuntimeException("Signed object is not equal "
                    + "to original one: " + copy);
        }

        /*
         * The signed object is a copy of an original one.
         * Once the copy is made, further manipulation
         * of the original object shouldn't has any effect on the copy.
         */
        original.set(MAGIC - 1);
        copy = so.getObject();
        if (original.equals(copy)) {
            throw new RuntimeException("Signed object is not a copy "
                    + "of original one: " + copy);
        }

        System.out.println("Test passed");
    }

    private static class Test implements Serializable {
        private int number = MAGIC;

        public int get() {
            return number;
        }

        public void set(int magic) {
            this.number = magic;
        }

        @Override
        public int hashCode() {
            return number;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof Test)) {
                return false;
            }

            Test other = (Test) obj;
            return number == other.number;
        }

        @Override
        public String toString() {
            return "" + number;
        }
    }
}
