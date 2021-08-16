/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234744
 * @summary KeyStore.store can write wrong type of file
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public class WrongStoreType {
    public static void main(String ... args) throws Exception {

        char[] password = "changeit".toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");

        ks.load(null, null);
        System.out.println(ks.getType());

        Files.createFile(Path.of("emptyfile"));
        try (InputStream in = new FileInputStream("emptyfile")) {
            ks.load(in, password);
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println(ks.getType());
        try (OutputStream out = new FileOutputStream("newfile")) {
            ks.store(out, password);
        }

        ks = KeyStore.getInstance(new File("newfile"), password);
        String type = ks.getType();
        if (!type.equalsIgnoreCase("PKCS12")) {
            throw new Exception("see storetype " + type);
        }
    }
}
