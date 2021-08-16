/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7031343
 * @summary Provide API changes to support GCM AEAD ciphers
 * @author Brad Wetmore
 */

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.ByteBuffer;

/*
 * At this point in time, we can't really do any testing since only the API
 * is available, the underlying implementation doesn't exist yet.  Test
 * what we can...
 */
public class GCMAPI {

    // 16 elements
    private static byte[] bytes = new byte[] {
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f };

    private static int failed = 0;
    private static Cipher c;

    public static void main(String[] args) throws Exception {
        c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[16], "AES"));

        updateAADFail((byte[]) null);
        updateAADPass(bytes);

        updateAADFail(null, 2, 4);
        updateAADFail(bytes, -2, 4);
        updateAADFail(bytes, 2, -4);
        updateAADFail(bytes, 2, 15);  // one too many

        updateAADPass(bytes, 2, 14);  // ok.
        updateAADPass(bytes, 4, 4);
        updateAADPass(bytes, 0, 0);

        ByteBuffer bb = ByteBuffer.wrap(bytes);

        updateAADFail((ByteBuffer) null);
        updateAADPass(bb);

        if (failed != 0) {
            throw new Exception("Test(s) failed");
        }
    }

    private static void updateAADPass(byte[] src) {
        try {
            c.updateAAD(src);
        } catch (UnsupportedOperationException e) {
            // swallow
        } catch (IllegalStateException ise) {
            // swallow
        }catch (Exception e) {
            e.printStackTrace();
            failed++;
        }
    }

    private static void updateAADFail(byte[] src) {
        try {
            c.updateAAD(src);
            new Exception("Didn't Fail as Expected").printStackTrace();
            failed++;
        } catch (IllegalArgumentException e) {
            // swallow
        }
    }

    private static void updateAADPass(byte[] src, int offset, int len) {
        try {
            c.updateAAD(src, offset, len);
        } catch (UnsupportedOperationException e) {
            // swallow
        } catch (IllegalStateException ise) {
            // swallow
        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }
    }

    private static void updateAADFail(byte[] src, int offset, int len) {
        try {
            c.updateAAD(src, offset, len);
            new Exception("Didn't Fail as Expected").printStackTrace();
            failed++;
        } catch (IllegalArgumentException e) {
            // swallow
        }
    }

    private static void updateAADPass(ByteBuffer src) {
        try {
            c.updateAAD(src);
        } catch (UnsupportedOperationException e) {
            // swallow
        } catch (IllegalStateException ise) {
            // swallow
        }catch (Exception e) {
            e.printStackTrace();
            failed++;
        }
    }

    private static void updateAADFail(ByteBuffer src) {
        try {
            c.updateAAD(src);
            new Exception("Didn't Fail as Expected").printStackTrace();
            failed++;
        } catch (IllegalArgumentException e) {
            // swallow
        }
    }
}
