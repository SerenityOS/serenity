/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4475278
 * @summary     X500Principal, X509Certificate and X509CRL
 *              unnecessarily throw checked exception
 */

import java.io.*;
import javax.security.auth.x500.X500Principal;

public class InvalidConstructorInput {

    public static void main(String[] args) {

        try {
            byte[] bytes = { 'a' };
            X500Principal p = new X500Principal(bytes);
            throw new SecurityException("test failed: #1");
        } catch (RuntimeException re) {
        }

        try {
            String dir = System.getProperty("test.src");
            if (dir == null)
                dir = ".";

            FileInputStream fis = new FileInputStream
                                (dir + "/InvalidConstructorInput.java");
            X500Principal p = new X500Principal(fis);
            throw new SecurityException("test failed: #2.1");
        } catch (FileNotFoundException fnfe) {
            throw new SecurityException("test failed: #2.2");
        } catch (RuntimeException re) {
        }

        System.out.println("Test passed");
    }
}
