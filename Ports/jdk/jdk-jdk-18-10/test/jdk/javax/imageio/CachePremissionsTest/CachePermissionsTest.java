/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6684104
 * @summary Test verifies that ImageIO checks all permissions required for
 *           the file cache usage:
 *
 *          no policy file: No security restrictions.
 *             Expected result: ImageIO creates file-cached stream.
 *
 *          w.policy: the case when we have read and write permissions
 *              for java.io.temp directory but have only write permission
 *              for a temp file.
 *             Expected result: ImageIO create a memory-cached stream
 *              image output stream.
 *
 *          rw.policy: the case when we  have read and write permissions
 *              for java.io.temp directory but have only read and write
 *              permission for a temp cache file.
 *             Expected result: ImageIO creates a memory-cached stream
 *              because temporary cache file can not be deleted.
 *
 *          rwd.policy: the case when we  have read and write permissions
 *              for java.io.temp directory and have all required permissions
 *             (read, write, and delete) for a temporary cache file.
 *             Expected result: ImageIO creates file-cached stream.
 *
 *           -Djava.security.debug=access can be used to verify file permissions.
 *
 * @run     main CachePermissionsTest true
 * @run     main/othervm -Djava.security.manager=allow CachePermissionsTest false w.policy
 * @run     main/othervm -Djava.security.manager=allow CachePermissionsTest false rw.policy
 * @run     main/othervm -Djava.security.manager=allow CachePermissionsTest true rwd.policy
 */

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.imageio.stream.ImageOutputStream;

import javax.imageio.ImageIO;


public class CachePermissionsTest {
    public static void main(String[] args) {
        boolean isFileCacheExpected =
            Boolean.valueOf(args[0]).booleanValue();
        System.out.println("Is file cache expected: " + isFileCacheExpected);

        ImageIO.setUseCache(true);

        System.out.println("java.io.tmpdir is " + System.getProperty("java.io.tmpdir"));

        if (args.length > 1) {
            String testsrc = System.getProperty("test.src", ".");
            String policy = testsrc + File.separator + args[1];

            System.out.println("Policy file: " + policy);
            System.setProperty("java.security.policy", policy);

            System.out.println("Install security manager...");
            System.setSecurityManager(new SecurityManager());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

            boolean isFileCache = ios.isCachedFile();
            System.out.println("Is file cache used: " + isFileCache);

            if (isFileCache !=isFileCacheExpected) {
                System.out.println("WARNING: file chace usage is not as expected!");
            }

            System.out.println("Verify data writing...");
            for (int i = 0; i < 8192; i++) {
                ios.writeInt(i);
            }

            System.out.println("Verify data reading...");
            ios.seek(0L);

            for (int i = 0; i < 8192; i++) {
                int j = ios.readInt();
                if (i != j) {
                    throw new RuntimeException("Wrong data in the stream " + j + " instead of " + i);
                }
            }

            System.out.println("Verify stream closing...");
            ios.close();
        } catch (IOException e) {
            /*
             * Something went wrong?
             */
            throw new RuntimeException("Test FAILED.", e);
        } catch (SecurityException e) {
            /*
             * We do not expect security execptions here:
             * we there are any security restrition, ImageIO
             * should swith to memory-cached streams, instead
             * of using file cache.
             */
            throw new RuntimeException("Test FAILED.", e);
        }
    }
}
