/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6689000
 * @summary Changes in 6675606 causing regression test failures on windows-i586
 */

import com.sun.security.auth.login.*;
import java.io.*;
import java.net.URL;

public class IllegalURL {
    public static void main(String[] args) throws Exception {
        FileOutputStream fos = new FileOutputStream("x.conf");
        fos.close();
        use("file:" + System.getProperty("user.dir") + "/x.conf");
        use("file:x.conf");
        System.out.println("Test passed");
    }

    static void use(String f) throws Exception {
        System.out.println("Testing " + f  + "...");
        System.setProperty("java.security.auth.login.config", f);
        try (FileInputStream fis =
                new FileInputStream(new URL(f).getFile().replace('/', File.separatorChar))) {
            // do nothing
        } catch (Exception e) {
            System.out.println("Even old implementation does not support it. Ignored.");
            return;
        }
        new ConfigFile();
    }
}
