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
 * @bug 8044500
 * @summary Add kinit options and krb5.conf flags that allow users to
 *          obtain renewable tickets and specify ticket lifetimes
 * @modules java.security.jgss/sun.security.krb5
 * @compile -XDignore.symbol.file Duration.java
 * @run main Duration
 */
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;

public class Duration {
    public static void main(String[] args) throws Exception {
        check("123", 123);
        check("1:1", 3660);
        check("1:1:1", 3661);
        check("1d", 86400);
        check("1h", 3600);
        check("1h1m", 3660);
        check("1h 1m", 3660);
        check("1d 1h 1m 1s", 90061);
        check("1d1h1m1s", 90061);

        check("", -1);
        check("abc", -1);
        check("1ms", -1);
        check("1d1d", -1);
        check("1h1d", -1);
        check("x1h", -1);
        check("1h x 1m", -1);
        check(":", -1);
        check("1:60", -1);
        check("1:1:1:1", -1);
        check("1:1:1:", -1);
    }

    static void check(String s, int ex) throws Exception {
        System.out.print("\u001b[1;37;41m" +s + " " + ex);
        System.out.print("\u001b[m\n");
        try {
            int result = Config.duration(s);
            if (result != ex) throw new Exception("for " + s + " is " + result);
        } catch (KrbException ke) {
            ke.printStackTrace();
            if (ex != -1) throw new Exception();
        }
    }
}
