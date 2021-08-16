/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7144530
 * @summary KeyTab.getInstance(String) no longer handles keyTabNames with "file:" prefix
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ktab
 */
import java.io.File;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ktab.KeyTab;

public class FileKeyTab {
    public static void main(String[] args) throws Exception {
        String name = "ktab";
        KeyTab kt = KeyTab.create(name);
        kt.addEntry(new PrincipalName("a@A"), "x".toCharArray(), 1, true);
        kt.save();
        check(name);
        check("FILE:" + name);

        name = new File(name).getAbsolutePath().toString();

        check(name);
        check("FILE:" + name);

        // The bug reporter uses this style, should only work for
        // absolute path
        check("FILE:/" + name);
    }

    static void check(String file) throws Exception {
        System.out.println("Checking for " + file + "...");
        KeyTab kt2 = KeyTab.getInstance(file);
        if (kt2.isMissing()) {
            throw new Exception("FILE:ktab cannot be loaded");
        }
    }
}
