/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4173993
 * @summary Make sure "null" is returned if property does not exist (or has
 * wrong numeric format) and no default has been specified.
 * @modules java.base/sun.security.action
 */

import sun.security.action.*;

public class ReturnNullIfNoDefault {

    public static void main(String[] args) throws Exception {
        long larg = 1234567890L;

        GetLongAction ac = new GetLongAction("test");
        if (ac.run() != null)
            throw new Exception("Returned value is not null");

        ac = new GetLongAction("test", larg);
        long ret = ((Long)ac.run()).longValue();
        if (ret != larg)
            throw new Exception("Returned value differs from default");

        System.setProperty("test", Long.toString(larg));
        ac = new GetLongAction("test");
        ret = ((Long)ac.run()).longValue();
        if (ret != larg)
            throw new Exception("Returned value differs from property");
    }
}
