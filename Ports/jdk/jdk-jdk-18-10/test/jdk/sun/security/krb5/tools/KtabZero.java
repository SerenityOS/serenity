/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabConstants;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jdk.test.lib.SecurityTools.klist;

/*
 * @test
 * @bug 8014196 7002036 7043737
 * @summary ktab creates a file with zero kt_vno
 * @requires os.family == "windows"
 * @library /test/lib
 * @modules java.security.jgss/sun.security.krb5.internal.ktab:+open
 */
public class KtabZero {

    static final String NAME = "k.tab";

    public static void main(String[] args) throws Exception {

        // 0. Non-existing keytab
        Files.deleteIfExists(Paths.get(NAME));
        ktab("-l").shouldNotHaveExitValue(0);
        klist("-k " + NAME).shouldNotHaveExitValue(0);
        check(true);

        // 1. Create with KeyTab
        Files.deleteIfExists(Paths.get(NAME));
        KeyTab.getInstance(NAME).save();
        check(false);

        // 2. Create with the tool
        Files.deleteIfExists(Paths.get(NAME));
        ktab("-a me@HERE pass").shouldHaveExitValue(0);
        ktab("-l").shouldHaveExitValue(0);

        // 7002036: ktab return code changes on a error case
        ktab("-hello").shouldNotHaveExitValue(0);
        ktab("").shouldNotHaveExitValue(0);
        check(false);

        // 3. Invalid keytab
        Files.write(Path.of(NAME), "garbage".getBytes());
        ktab("-l").shouldNotHaveExitValue(0);
        ktab("-a me@HERE pass").shouldNotHaveExitValue(0);
        klist("-k " + NAME).shouldNotHaveExitValue(0);
    }

    static OutputAnalyzer ktab(String s) throws Exception {
        s = ("-k " + NAME + " " + s).trim();
        return SecurityTools.ktab(s);
    }

    // Checks existence as well as kt-vno
    static void check(boolean showBeMissing) throws Exception {
        KeyTab kt = KeyTab.getInstance(NAME);
        if (kt.isMissing() != showBeMissing) {
            throw new Exception("isMissing is not " + showBeMissing);
        }
        Field f = KeyTab.class.getDeclaredField("kt_vno");
        f.setAccessible(true);
        if (f.getInt(kt) != KeyTabConstants.KRB5_KT_VNO) {
            throw new Exception("kt_vno is " + f.getInt(kt));
        }
    }
}
