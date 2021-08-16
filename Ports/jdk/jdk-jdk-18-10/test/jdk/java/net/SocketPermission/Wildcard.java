/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021280
 * @summary SocketPermission should accept wildcards
 */

import java.net.SocketPermission;

public class Wildcard
{
    public static void main(String[] args) throws Exception {
        SocketPermission star_All =
                new SocketPermission("*.blabla.bla", "listen,accept,connect");
        SocketPermission www_All =
                new SocketPermission("bla.blabla.bla", "listen,accept,connect");

        if (!star_All.implies(www_All)) {
            throw new RuntimeException(
                   "Failed: " + star_All + " does not imply " + www_All);
        }
    }
}
