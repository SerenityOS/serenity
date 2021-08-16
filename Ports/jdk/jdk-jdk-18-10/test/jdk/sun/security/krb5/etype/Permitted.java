/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262389
 * @modules java.security.jgss/sun.security.krb5
 * @library /test/lib
 * @summary Use permitted_enctypes if default_tkt_enctypes or default_tgs_enctypes is not present
 */

import jdk.test.lib.Asserts;
import sun.security.krb5.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Permitted {
    public static void main(String[] args) throws Exception {

        System.setProperty("java.security.krb5.conf", "permitted.conf");

        Files.write(Path.of("permitted.conf"), List.of("[libdefaults]",
                "permitted_enctypes = aes128-cts"));
        Config.refresh();
        Asserts.assertEQ(Config.getInstance().defaultEtype("default_tkt_enctypes").length, 1);
        Asserts.assertEQ(Config.getInstance().defaultEtype("default_tgs_enctypes").length, 1);

        Files.write(Path.of("permitted.conf"), List.of("[libdefaults]",
                "default_tkt_enctypes = aes128-cts aes256-cts",
                "default_tgs_enctypes = aes128-cts aes256-cts aes256-sha2",
                "permitted_enctypes = aes128-cts"));
        Config.refresh();
        Asserts.assertEQ(Config.getInstance().defaultEtype("default_tkt_enctypes").length, 2);
        Asserts.assertEQ(Config.getInstance().defaultEtype("default_tgs_enctypes").length, 3);
    }
}
