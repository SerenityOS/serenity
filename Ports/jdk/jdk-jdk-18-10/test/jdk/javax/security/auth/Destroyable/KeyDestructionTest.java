/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6263419
 * @summary No way to clean the memory for a java.security.Key
 */

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.security.auth.Destroyable;
import javax.security.auth.DestroyFailedException;

public class KeyDestructionTest {
    public static void main(String[] args) throws Exception {
        KeyPair keypair = generateKeyPair("RSA", 1024);

        // Check keys that support and have implemented key destruction
        testKeyDestruction(new MyDestroyableSecretKey());
        testKeyDestruction(new MyDestroyablePrivateKey());

        // Check keys that support but have not implemented key destruction
        testNoKeyDestruction(generateSecretKey("AES", 128));
        testNoKeyDestruction(keypair.getPrivate());

        // Check keys that do not support key destruction
        try {
            testKeyDestruction(keypair.getPublic());
        } catch (UnsupportedOperationException uoe) {
            // not an error
            System.out.println(keypair.getPublic().getClass().getName() +
                " keys do not support key destruction");
        }

        System.out.println("PASSED.");
    }

    // Check the behaviour of a key that implements key destruction
    private static void testKeyDestruction(Key key) throws Exception {
        String klass = key.getClass().getName();
        boolean hasUsable = key instanceof Usable;

        try {
            key.getAlgorithm();
            key.getFormat();
            if (allZero(key.getEncoded())) {
                throw new Exception("error: key destroyed prematurely");
            }
        } catch (IllegalStateException ise) {
            throw new Exception("error: unexpected ISE", ise);
        }

        if (hasUsable) {
            ((Usable) key).useKey();
        }

        destroyKey(key);

        try {
            if (hasUsable) {
                ((Usable) key).useKey();
            }
        } catch (IllegalStateException ise) {
            // not an error
        }

        try {
            key.getAlgorithm();
            key.getFormat();
            if (!allZero(key.getEncoded())) {
                throw new Exception("error: key destroyed incorrectly");
            }
        } catch (IllegalStateException ise) {
            // not an error
        }

        System.out.println("A " + klass +
            " key has been successfully destroyed");
    }

    // Check the behaviour of a key that does not implement key destruction
    private static void testNoKeyDestruction(Destroyable key)
        throws Exception {
        String klass = key.getClass().getName();

        if (key.isDestroyed()) {
            throw new Exception("error: a " + klass +
                " key has been unexpectedly destroyed");
        }
        try {
            key.destroy();
        } catch (DestroyFailedException dfe) {
            // not an error

            if (key.isDestroyed()) {
                throw new Exception("error: a " + klass +
                    " key has been unexpectedly destroyed");
            }
            System.out.println(klass + " keys are not destroyable");
            return;
        }
        throw new Exception("error: key may been unexpectedly destroyed");
    }

    private static KeyPair generateKeyPair(String algorithm, int size)
        throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
        generator.initialize(size);
        return generator.genKeyPair();
    }

    private static SecretKey generateSecretKey(String algorithm, int size)
        throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(algorithm);
        generator.init(size);
        return generator.generateKey();
    }

    private static void destroyKey(Key key) throws Exception {
        String klass = key.getClass().getName();

        if (!(key instanceof Destroyable)) {
            throw new UnsupportedOperationException();
        }

        Destroyable dKey = (Destroyable) key;
        if (dKey.isDestroyed()) {
            throw new Exception("error: a " + klass +
                " key has already been destroyed");
        }
        dKey.destroy();
        if (!dKey.isDestroyed()) {
            throw new Exception("error: a " + klass +
                " key has NOT been destroyed");
        }
    }

    private static boolean allZero(byte[] bytes) {
        int count = 0;
        for (byte b : bytes) {
            if (b == 0x00) {
                count++;
            }
        }
        return (bytes.length == count);
    }
}

interface Usable {
    public void useKey();
}

class MyDestroyableSecretKey implements SecretKey, Usable {
    private byte[] encoded = new byte[]{0x0F, 0x1F, 0x2F, 0x3F}; // non-zero
    private boolean isDestroyed = false;

    @Override
    public void useKey() {
        if (isDestroyed) {
            throw new IllegalStateException();
        }
    }

    @Override
    public String getAlgorithm() {
        return "MyDestroyableSecretKey algorithm";
    }

    @Override
    public String getFormat() {
        return "MyDestroyableSecretKey format";
    }

    @Override
    public byte[] getEncoded() {
        return this.encoded;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.isDestroyed) {
            Arrays.fill(encoded, (byte) 0);
            this.isDestroyed = true;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.isDestroyed;
    }
}

class MyDestroyablePrivateKey implements PrivateKey, Usable {
    private byte[] encoded = new byte[]{0x4F, 0x5F, 0x6F, 0x7F}; // non-zero
    private boolean isDestroyed = false;

    @Override
    public void useKey() {
        if (isDestroyed) {
            throw new IllegalStateException();
        }
    }

    @Override
    public String getAlgorithm() {
        return "MyDestroyablePrivateKey algorithm";
    }

    @Override
    public String getFormat() {
        return "MyDestroyablePrivateKey format";
    }

    @Override
    public byte[] getEncoded() {
        return this.encoded;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.isDestroyed) {
            Arrays.fill(encoded, (byte) 0);
            this.isDestroyed = true;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.isDestroyed;
    }
}
