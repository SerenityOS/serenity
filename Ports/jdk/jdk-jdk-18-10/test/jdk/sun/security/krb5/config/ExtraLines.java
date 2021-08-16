/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8036971
 * @modules java.security.jgss/sun.security.krb5
 * @compile -XDignore.symbol.file ExtraLines.java
 * @run main/othervm ExtraLines
 * @summary krb5.conf does not accept directive lines before the first section
 */

import sun.security.krb5.Config;
import java.nio.file.*;
import java.util.Objects;

public class ExtraLines {
    public static void main(String[] args) throws Exception {
        Path base = Paths.get("krb5.conf");
        Path include = Paths.get("included.conf");
        String baseConf = "include " + include.toAbsolutePath().toString()
                + "\n[x]\na = b\n";
        String includeConf = "[y]\nc = d\n";
        Files.write(include, includeConf.getBytes());
        Files.write(base, baseConf.getBytes());

        System.setProperty("java.security.krb5.conf", base.toString());
        Config.refresh();

        if (!Objects.equals(Config.getInstance().get("x", "a"), "b")) {
            throw new Exception("Failed");
        }
    }
}
