/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.RuntimePermission;
import java.net.NetPermission;
import java.sql.SQLPermission;
import java.util.PropertyPermission;
import javax.net.ssl.SSLPermission;

/*
 * @test
 * @bug 7167056
 * @summary Check that BasicPermission subclasses don't throw exception if name
 *          contains wildcard character ("*") but does not signify a
 *          wildcard match
 * @modules java.sql
 */

public class Wildcard {

    public static void main(String[] args) throws Exception {
        wildcard("*java");
        wildcard("java*");
        wildcard("ja*va");
    }

    private static void wildcard(String wildcard) throws Exception {
        new RuntimePermission(wildcard);
        new NetPermission(wildcard);
        new SQLPermission(wildcard);
        new PropertyPermission(wildcard, "read");
        new SSLPermission(wildcard);
    }
}
