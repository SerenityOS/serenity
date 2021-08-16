/*
 * Copyright (c) 2021, Red Hat, Inc.
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
 * @bug 8258833
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki/sun.security.pkcs11:open
 * @run main/othervm CancelMultipart
 */

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.Provider;
import java.security.ProviderException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

public class CancelMultipart extends PKCS11Test {

    private static Provider provider;
    private static Key key;

    static {
        key = new SecretKeySpec(new byte[16], "AES");
    }

    private static class SessionLeaker {
        private LeakOperation op;
        private LeakInputType type;

        SessionLeaker(LeakOperation op, LeakInputType type) {
            this.op = op;
            this.type = type;
        }

        private void leakAndTry() throws Exception {
            Cipher cipher = op.getCipher();
            try {
                type.doOperation(cipher,
                        (op instanceof LeakDecrypt ?
                                LeakInputType.DECRYPT_MODE :
                                null));
                throw new Exception("PKCS11Exception expected, invalid block"
                        + "size");
            } catch (ProviderException | IllegalBlockSizeException e) {
                // Exception expected - session returned to the SessionManager
                // should be cancelled. That's what will be tested now.
            }

            tryCipherInit();
        }
    }

    private static interface LeakOperation {
        Cipher getCipher() throws Exception;
    }

    private static interface LeakInputType {
        static int DECRYPT_MODE = 1;
        void doOperation(Cipher cipher, int mode) throws Exception;
    }

    private static class LeakDecrypt implements LeakOperation {
        public Cipher getCipher() throws Exception {
            Cipher cipher = Cipher.getInstance(
                    "AES/ECB/PKCS5Padding", provider);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher;
        }
    }

    private static class LeakByteBuffer implements LeakInputType {
        public void doOperation(Cipher cipher, int mode) throws Exception {
            if (mode == DECRYPT_MODE) {
                cipher.update(ByteBuffer.allocate(1), ByteBuffer.allocate(1));
                cipher.doFinal(ByteBuffer.allocate(0), ByteBuffer.allocate(1));
            }
        }
    }

    private static class LeakByteArray implements LeakInputType {
        public void doOperation(Cipher cipher, int mode) throws Exception {
            if (mode == DECRYPT_MODE) {
                cipher.update(new byte[1]);
                cipher.doFinal(new byte[1], 0, 0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        main(new CancelMultipart(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        init(p);

        // Try multiple paths:

        executeTest(new SessionLeaker(new LeakDecrypt(), new LeakByteArray()),
                "P11Cipher::implDoFinal(byte[], int, int)");

        executeTest(new SessionLeaker(new LeakDecrypt(), new LeakByteBuffer()),
                "P11Cipher::implDoFinal(ByteBuffer)");

        System.out.println("TEST PASS - OK");
    }

    private static void executeTest(SessionLeaker sl, String testName)
            throws Exception {
        try {
            sl.leakAndTry();
            System.out.println(testName +  ": OK");
        } catch (Exception e) {
            System.out.println(testName +  ": FAILED");
            throw e;
        }
    }

    private static void init(Provider p) throws Exception {
        provider = p;

        // The max number of sessions is 2 because, in addition to the
        // operation (i.e. PKCS11::getNativeKeyInfo), a session to hold
        // the P11Key object is needed.
        setMaxSessions(2);
    }

    /*
     * This method is intended to generate pression on the number of sessions
     * to be used from the NSS Software Token, so sessions with (potentially)
     * active operations are reused.
     */
    private static void setMaxSessions(int maxSessions) throws Exception {
        Field tokenField = Class.forName("sun.security.pkcs11.SunPKCS11")
                .getDeclaredField("token");
        tokenField.setAccessible(true);
        Field sessionManagerField = Class.forName("sun.security.pkcs11.Token")
                .getDeclaredField("sessionManager");
        sessionManagerField.setAccessible(true);
        Field maxSessionsField = Class.forName("sun.security.pkcs11.SessionManager")
                .getDeclaredField("maxSessions");
        maxSessionsField.setAccessible(true);
        Object sessionManagerObj = sessionManagerField.get(
                tokenField.get(provider));
        maxSessionsField.setInt(sessionManagerObj, maxSessions);
    }

    private static void tryCipherInit() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding", provider);

        // A CKR_OPERATION_ACTIVE error may be thrown if a session was
        // returned to the Session Manager with an active operation, and
        // we try to initialize the Cipher using it.
        //
        // Given that the maximum number of sessions was forced to 2, we know
        // that the session to be used here was already used in a previous
        // (failed) operation. Thus, the test asserts that the operation was
        // properly cancelled.
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // If initialization passes, finish gracefully so other paths can
        // be tested under the current maximum number of sessions.
        cipher.doFinal(new byte[16], 0, 0);
    }
}
